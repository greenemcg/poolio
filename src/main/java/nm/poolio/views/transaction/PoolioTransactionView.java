package nm.poolio.views.transaction;

import static nm.poolio.utils.VaddinUtils.AMOUNT_ICON;
import static nm.poolio.utils.VaddinUtils.MONEY_TYPE_ICON;
import static nm.poolio.utils.VaddinUtils.NOTES_ICON;
import static nm.poolio.utils.VaddinUtils.TRANSACTION_ICON;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.data.User;
import nm.poolio.enitities.transaction.NoteCreator;
import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import nm.poolio.enitities.transaction.PoolioTransactionType;
import nm.poolio.model.JsonbNote;
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

  Binder<PoolioTransaction> binder = new Binder<>(PoolioTransaction.class);
  Dialog transactionDialog = new Dialog();

  @Getter Grid<PoolioTransaction> grid = createGrid(PoolioTransaction.class);

  IntegerField amount = new IntegerField("Amount");

  List<PoolioTransaction> allPoolioTransactions;

  ComboBox<User> targetUser = new ComboBox<>();
  ComboBox<PoolioTransactionType> type = new ComboBox<>("Type", PoolioTransactionType.cashTypes());

  TextArea transactionNotes = new TextArea();

  public PoolioTransactionView(
      AuthenticatedUser authenticatedUser,
      PoolioTransactionService service,
      UserService userService) {
    this.authenticatedUser = authenticatedUser;
    this.service = service;
    this.userService = userService;
    setHeight("100%");
    binder.bindInstanceFields(this);

    createDialog(
        transactionDialog, e -> onSaveTransaction(binder.getBean()), createTransactionDialog());

    Button newItemButton =
        new Button(
            "NewTransaction",
            TRANSACTION_ICON.create(),
            e -> openPoolioTransactionDialog(new PoolioTransaction()));
    add(newItemButton);

    decorateGrid();

    allPoolioTransactions = service.findAllPoolioTransactions();

    add(grid);
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

  private void decorateGrid() {
    decorateTransactionGrid();

    grid.setItems(service.findAllPoolioTransactions());
  }

  private void openPoolioTransactionDialog(PoolioTransaction poolioTransaction) {
    binder.setBean(poolioTransaction);
    transactionDialog.open();
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

  private void onSaveTransaction(PoolioTransaction t) {
    transactionDialog.close();
    handleType(t);
    processNote(t);
    service.save(t);
    grid.setItems(service.findAllPoolioTransactions());
  }

  private void processNote(PoolioTransaction t) {
    if (StringUtils.isNotEmpty(transactionNotes.getValue())) {
      JsonbNote note = buildNote(transactionNotes.getValue());
      t.setNotes(List.of(note));
    }
  }
}
