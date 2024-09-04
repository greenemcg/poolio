package nm.poolio.views.user;

import static nm.poolio.utils.VaddinUtils.PAY_AS_YOU_GO;
import static nm.poolio.utils.VaddinUtils.POOLIO_ICON;
import static nm.poolio.utils.VaddinUtils.USER_ICON;
import static nm.poolio.utils.VaddinUtils.createIconSpan;
import static nm.poolio.utils.VaddinUtils.decorateAdminCheckbox;
import static nm.poolio.utils.VaddinUtils.decorateEmailField;
import static nm.poolio.utils.VaddinUtils.decorateNameField;
import static nm.poolio.utils.VaddinUtils.decoratePasswordField;
import static nm.poolio.utils.VaddinUtils.decoratePhoneField;
import static nm.poolio.utils.VaddinUtils.decorateUserNameField;
import static org.vaadin.lineawesome.LineAwesomeIcon.SAVE_SOLID;
import static org.vaadin.lineawesome.LineAwesomeIcon.WINDOW_CLOSE_SOLID;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.Nullable;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.PoolService;
import nm.poolio.enitities.transaction.NoteCreator;
import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import nm.poolio.enitities.transaction.PoolioTransactionType;
import nm.poolio.model.JsonbNote;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.services.UserService;
import nm.poolio.vaadin.PoolioNotification;
import nm.poolio.views.MainLayout;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@PageTitle("Users \uD83D\uDC64")
@Route(value = "user", layout = MainLayout.class)
@RolesAllowed("ADMIN")
@Slf4j
public class UserView extends VerticalLayout implements UserGrid, NoteCreator, PoolioNotification {

  @Getter private final PoolioTransactionService poolioTransactionService;
  @Getter private final AuthenticatedUser authenticatedUser;
  private final PoolService poolService;
  private final UserService userService;

  TextField userName = new TextField("User name");
  TextField name = new TextField("Name");
  PasswordField tempPassword = new PasswordField();
  EmailField email = new EmailField("Email");
  TextField phone = new TextField("Phone");
  Checkbox admin = new Checkbox();
  Checkbox payAsYouGo = new Checkbox();

  Dialog dialog;

  Binder<User> binder;
  @Getter Grid<User> grid = createGrid(User.class);
  UserService service;

  Button saveButton;
  String dialogButtonLabel = "Add";

  public UserView(
      PoolioTransactionService poolioTransactionService,
      UserService userService,
      PoolService poolService,
      AuthenticatedUser authenticatedUser) {
    this.poolioTransactionService = poolioTransactionService;
    this.authenticatedUser = authenticatedUser;
    setHeight("100%");

    this.service = userService;
    Button newUserButton = new Button("New User", USER_ICON.create(), e -> openDialog(new User()));
    add(newUserButton);

    binder = new Binder<>(User.class);
    binder.bindInstanceFields(this);

    dialog = new Dialog();

    VerticalLayout dialogLayout = createDialogLayout();
    dialogLayout.setPadding(false);
    dialog.add(dialogLayout);

    saveButton = createSaveButton();
    Button cancelButton = new Button("Cancel", WINDOW_CLOSE_SOLID.create(), e -> dialog.close());
    dialog.getFooter().add(cancelButton);
    dialog.getFooter().add(saveButton);

    decorateGrid();
    grid.addItemClickListener(item -> openDialog(item.getItem()));

    var users = userService.findAll();

    grid.setItems(users);

    setPadding(true);
    add(grid);
    this.poolService = poolService;
    this.userService = userService;
  }

  private void decorateGrid() {
    decoratePoolGrid();
  }

  private void onSaveUser(User user) {
    if (StringUtils.isNotEmpty(user.getTempPassword())) {
      user.setHashedPassword(new BCryptPasswordEncoder().encode(user.getTempPassword()));
    }

    if (binder.validate().isOk()) {
      user.setUserName(user.getUserName().toLowerCase());

      if (!checkUser(user)) {return;}

      var newUser = service.update(user);

      var optional = poolService.get(5L);

      if( optional.isPresent()) {
        var bobsPool = optional.get();
        bobsPool.getPlayers().add(newUser);
        poolService.update(bobsPool);
      }

//      PoolioTransaction poolioTransaction = new PoolioTransaction();
//      poolioTransaction.setDebitUser(newUser);
//      poolioTransaction.setCreditUser(userService.getCashUser());
//      poolioTransaction.setAmount(1);
//      poolioTransaction.setType(PoolioTransactionType.CASH_DEPOSIT);
//
//      JsonbNote note = buildNote("Created User with One Dollar and Member of bobs pool");
//      poolioTransaction.setNotes(List.of(note));
//      poolioTransactionService.save(poolioTransaction);

      dialog.close();
      grid.setItems(service.findAll());
    } else {
      log.error("FIX me");
    }
  }

  private boolean checkUser(User user) {
    boolean sucess = true;

    if (!userService.checkUserName(user.getUserName(), user.getId())) {
      createErrorNotification(
          new Span("User (login) username already in use: " + user.getUserName()));
      sucess = false;
    }

    if (!userService.checkName(user.getName(), user.getId())) {
      createErrorNotification(new Span("User name already in use: " + user.getName()));
      sucess = false;
    }

    return sucess;
  }

  private Button createSaveButton() {
    saveButton =
        new Button(dialogButtonLabel, SAVE_SOLID.create(), e -> onSaveUser(binder.getBean()));
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    return saveButton;
  }

  private Span createDialogTitlespan(String action) {
    Span span = new Span();
    span.add(action + " ");
    span.add(POOLIO_ICON.create());
    span.add(" Poolio User");

    return span;
  }

  private void openDialog(@Nullable User user) {
    dialog.getHeader().removeAll();

    if (user != null && user.getId() == null) {
      dialogButtonLabel = "Add";
      dialog.getHeader().add(createDialogTitlespan("New"));
      tempPassword.setRequiredIndicatorVisible(true);
    } else {
      dialogButtonLabel = "Update";
      dialog.getHeader().add(createDialogTitlespan("Edit"));
      tempPassword.setRequiredIndicatorVisible(false);
    }

    saveButton.setText(dialogButtonLabel);

    binder.setBean(user);
    dialog.open();
  }

  private VerticalLayout createDialogLayout() {
    decoratePasswordField(tempPassword);
    decorateEmailField(email);
    decorateUserNameField(userName);
    decorateNameField(name);
    decoratePhoneField(phone);
    decorateAdminCheckbox(admin);

    payAsYouGo.setLabelComponent(createIconSpan(PAY_AS_YOU_GO, "Pay As You Go"));

    userName.getStyle().set("text-transform", "lowercase");

    var layout = new VerticalLayout(userName, name, tempPassword, admin, email, phone, payAsYouGo);
    layout.setPadding(false);
    layout.setSpacing(false);
    layout.setAlignItems(FlexComponent.Alignment.STRETCH);
    layout.getStyle().set("width", "18rem").set("max-width", "100%");

    return layout;
  }
}
