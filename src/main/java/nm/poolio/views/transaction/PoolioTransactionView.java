package nm.poolio.views.transaction;

import static nm.poolio.utils.VaddinUtils.*;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.data.User;
import nm.poolio.enitities.transaction.NoteCreator;
import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import nm.poolio.enitities.transaction.PoolioTransactionType;
import nm.poolio.model.JsonbNote;
import nm.poolio.model.enums.Season;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.services.UserService;
import nm.poolio.vaadin.PoolioDialog;
import nm.poolio.vaadin.UserComboBox;
import nm.poolio.views.MainLayout;
import org.apache.commons.lang3.StringUtils;

@PageTitle("Transactions \uD83D\uDCB8")
@Route(value = "transactions", layout = MainLayout.class)
@RolesAllowed("ADMIN")
@Slf4j
public class PoolioTransactionView extends VerticalLayout
    implements PoolioTransactionGrid, PoolioDialog, NoteCreator {
  @Getter private final AuthenticatedUser authenticatedUser;
  private final PoolioTransactionService service;
  private final UserService userService;
    private final TimeZone timeZone;

  @Setter Column<PoolioTransaction> temporalAmountColumn;
  @Setter Column<PoolioTransaction> sequenceColumn;
  @Setter Column<PoolioTransaction> payAsYouGoColumn;

  Binder<PoolioTransaction> binder = new Binder<>(PoolioTransaction.class);
  Dialog transactionDialog = new Dialog();

  @Getter Grid<PoolioTransaction> grid = createGrid(PoolioTransaction.class);

  IntegerField amount = new IntegerField("Amount");

  List<PoolioTransaction> allPoolioTransactions;

  ComboBox<User> targetUser = new ComboBox<>();
  ComboBox<PoolioTransactionType> type = new ComboBox<>("Type", PoolioTransactionType.cashTypes());

  ComboBox<User> comboBox = new ComboBox<>("View User");

  TextArea transactionNotes = new TextArea();

  public PoolioTransactionView(
      AuthenticatedUser authenticatedUser,
      PoolioTransactionService service,
      UserService userService) {
    this.authenticatedUser = authenticatedUser;
    this.service = service;
    this.userService = userService;

    timeZone = MainLayout.getTimeZone();

    setHeight("100%");
    binder.bindInstanceFields(this);

    createDialog(
        transactionDialog, e -> onSaveTransaction(binder.getBean()), createTransactionDialog());

    HorizontalLayout comboBoxesHorizontalLayout = new HorizontalLayout();
    comboBoxesHorizontalLayout.setAlignItems(Alignment.BASELINE);

    Button newItemButton =
        new Button(
            "New Transaction",
            TRANSACTION_ICON.create(),
            e -> openPoolioTransactionDialog(new PoolioTransaction()));
    comboBoxesHorizontalLayout.add(newItemButton);

    decorateGrid();

    allPoolioTransactions = service.findAllPoolioTransactions();

    Set<User> userSet = new HashSet<>();
    allPoolioTransactions.forEach(
        t -> {
          userSet.add(t.getDebitUser());
          userSet.add(t.getCreditUser());
        });

    List<User> users = userSet.stream().sorted(Comparator.comparing(User::getName)).toList();

    comboBox.setItems(users);
    comboBox.getStyle().set("--vaadin-combo-box-overlay-width", "16em");
    comboBox.addValueChangeListener(e -> inspectUser(e.getValue()));
    comboBox.setItemLabelGenerator(User::getName);
    comboBox.setClearButtonVisible(true);

    comboBoxesHorizontalLayout.add(comboBox);

    add(comboBoxesHorizontalLayout);

    add(grid);
  }

  private void onSaveTransaction(PoolioTransaction t) {
    t.setSeason(Season.getCurrent());
    transactionDialog.close();
    handleType(t);
    processNote(t);
    service.save(t);
    grid.setItems(service.findAllPoolioTransactions());
  }

  private Component[] createTransactionDialog() {
    amount.setPlaceholder("Transfer Amount");
    amount.setLabel("Amount");
    amount.setPrefixComponent(AMOUNT_ICON.create());
    amount.setMin(1);
    amount.setMax(1000);
    amount.setRequired(true);
    UserComboBox userComboBox = new UserComboBox();
    userComboBox.decorate(targetUser, userService.findAll(), "User");
    targetUser.setRequired(true);

    type.setPrefixComponent(MONEY_TYPE_ICON.create());
    type.setRequired(true);

    transactionNotes.setLabel("Notes");
    transactionNotes.setPlaceholder("Transaction Notes");
    transactionNotes.setTooltipText("Add Notes to help track or explain this transaction");
    transactionNotes.setClearButtonVisible(true);
    transactionNotes.setPrefixComponent(NOTES_ICON.create());

    return new Component[] {amount, targetUser, type, transactionNotes};
  }

  private void openPoolioTransactionDialog(PoolioTransaction poolioTransaction) {
    binder.setBean(poolioTransaction);
    transactionDialog.open();
  }

  private void decorateGrid() {
    decorateTransactionGrid(timeZone);

    grid.setItems(service.findAllPoolioTransactions());
  }

  private void inspectUser(User user) {
    if (user == null) {
      temporalAmountColumn.setVisible(false);
      sequenceColumn.setVisible(false);
      grid.setItems(allPoolioTransactions);
    } else createUserInGrid(user);
  }

  private void handleType(PoolioTransaction t) {
    User cashUser = userService.getCashUser();

    switch (t.getType()) {
      case CASH_WITHDRAWAL -> {
        t.setDebitUser(cashUser);
        t.setCreditUser(targetUser.getValue());
      }
      case CASH_DEPOSIT -> {
        t.setDebitUser(targetUser.getValue());
        t.setCreditUser(cashUser);
      }
      default -> throw new IllegalArgumentException("Not handling" + t.getType());
    }
  }

  private void processNote(PoolioTransaction t) {
    if (StringUtils.isNotEmpty(transactionNotes.getValue())) {
      JsonbNote note = buildNote(transactionNotes.getValue());
      t.setNotes(List.of(note));
    }
  }

  private void createUserInGrid(User user) {
    temporalAmountColumn.setVisible(true);
    sequenceColumn.setVisible(true);

    var transactions =
        allPoolioTransactions.stream()
            .filter(t -> hasUser(t, user))
            .sorted(Comparator.comparing(PoolioTransaction::getCreatedDate))
            .toList();

    AtomicInteger temporalAmount = new AtomicInteger();
    AtomicInteger sequence = new AtomicInteger(1);

    AtomicBoolean havePayAsYouGo = new AtomicBoolean(false);

    transactions.forEach(
        t -> {
          if (t.getCreditUser().equals(user)) temporalAmount.addAndGet(-t.getAmount());
          else if (t.getDebitUser().equals(user)) temporalAmount.addAndGet(t.getAmount());

          t.setSequence(sequence.getAndIncrement());

          t.setTemporalAmount(temporalAmount.get());

          if (t.getPayAsYouGoUser() != null) havePayAsYouGo.set(true);
        });

    if (payAsYouGoColumn != null) {
      payAsYouGoColumn.setVisible(havePayAsYouGo.get());
    }

    grid.setItems(transactions);
  }

  private boolean hasUser(PoolioTransaction t, User user) {
    return t.getCreditUser().equals(user) || t.getDebitUser().equals(user);
  }
}
