package nm.poolio.views.user;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.Nullable;
import jakarta.annotation.security.RolesAllowed;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.PoolService;
import nm.poolio.enitities.transaction.NoteCreator;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.services.UserService;
import nm.poolio.vaadin.PoolioNotification;
import nm.poolio.views.MainLayout;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static nm.poolio.utils.VaddinUtils.*;
import static org.vaadin.lineawesome.LineAwesomeIcon.SAVE_SOLID;
import static org.vaadin.lineawesome.LineAwesomeIcon.WINDOW_CLOSE_SOLID;

@PageTitle("Users \uD83D\uDC64")
@Route(value = "user", layout = MainLayout.class)
@RolesAllowed("ADMIN")
@Slf4j
public class UserView extends VerticalLayout implements UserGrid, NoteCreator, PoolioNotification {

    @Getter
    private final PoolioTransactionService poolioTransactionService;
    @Getter
    private final AuthenticatedUser authenticatedUser;
    private final PoolService poolService;
    private final UserService userService;

    private final TextField userName = new TextField("User name");
    private final TextField name = new TextField("Name");
    private final PasswordField tempPassword = new PasswordField();
    private final EmailField email = new EmailField("Email");
    private final TextField phone = new TextField("Phone");
    private final Checkbox admin = new Checkbox();
    private final Checkbox payAsYouGo = new Checkbox();
    private final ComboBox<User> jumpToUserComboBox = new ComboBox<>("Jump to User");
    private final Dialog dialog = new Dialog();
    private final Binder<User> binder = new Binder<>(User.class);
    @Getter
    private final Grid<User> grid = createGrid(User.class);
    private final Button saveButton = createSaveButton();
    private String dialogButtonLabel = "Add";

    public UserView(PoolioTransactionService poolioTransactionService, UserService userService, PoolService poolService, AuthenticatedUser authenticatedUser) {
        this.poolioTransactionService = poolioTransactionService;
        this.authenticatedUser = authenticatedUser;
        this.poolService = poolService;
        this.userService = userService;
        setHeight("100%");

        HorizontalLayout gridActionHeader = new HorizontalLayout();
        gridActionHeader.setAlignItems(FlexComponent.Alignment.BASELINE);

        Button newUserButton = new Button("New User", USER_ICON.create(), e -> openDialog(new User()));
        gridActionHeader.add(newUserButton);

        jumpToUserComboBox.getStyle().set("--vaadin-combo-box-overlay-width", "16em");
        jumpToUserComboBox.addValueChangeListener(e -> jumpToUser(e.getValue()));
        jumpToUserComboBox.setItemLabelGenerator(User::getName);
        jumpToUserComboBox.setClearButtonVisible(true);
        gridActionHeader.add(jumpToUserComboBox);
        add( gridActionHeader);

        binder.bindInstanceFields(this);
        createDialog();
        decorateGrid();
        grid.addItemClickListener(item -> openDialog(item.getItem()));
        var users = userService.findAll();

        grid.setItems(users);

        List<User> sortedUsers = users.stream()
                .sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()))
                .toList();
        jumpToUserComboBox.setItems(sortedUsers);

        setPadding(true);
        add(grid);
    }

    private void jumpToUser(User value) {
        if (value != null) {
            grid.select(value);
            grid.scrollToItem(value);
        }
    }

    private void createDialog() {
        VerticalLayout dialogLayout = createDialogLayout();
        dialogLayout.setPadding(false);
        dialog.add(dialogLayout);

        Button cancelButton = new Button("Cancel", WINDOW_CLOSE_SOLID.create(), e -> dialog.close());
        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(saveButton);
    }

    private void decorateGrid() {
        decoratePoolGrid();
    }

    private void onSaveUser(@NotNull User user) {
        if (StringUtils.isNotEmpty(user.getTempPassword())) {
            user.setHashedPassword(new BCryptPasswordEncoder().encode(user.getTempPassword()));
        }

        if (binder.validate().isOk()) {
            user.setUserName(user.getUserName().toLowerCase());

            if (!checkUser(user)) {
                return;
            }

            var newUser = userService.update(user);
            poolService.get(5L).ifPresent(bobsPool -> {
                bobsPool.getPlayers().add(newUser);
                poolService.update(bobsPool);
            });

            dialog.close();
            grid.setItems(userService.findAll());
        } else {
            log.error("FIX me");
        }
    }

    private boolean checkUser(User user) {
        boolean success = true;

        if (!userService.checkUserName(user.getUserName(), user.getId())) {
            createErrorNotification(new Span("User (login) username already in use: " + user.getUserName()));
            success = false;
        }

        if (!userService.checkName(user.getName(), user.getId())) {
            createErrorNotification(new Span("User name already in use: " + user.getName()));
            success = false;
        }

        return success;
    }

    private Button createSaveButton() {
        Button button = new Button(dialogButtonLabel, SAVE_SOLID.create(), e -> onSaveUser(binder.getBean()));
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return button;
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

        VerticalLayout layout = new VerticalLayout(userName, name, tempPassword, admin, email, phone, payAsYouGo);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);
        layout.getStyle().set("width", "18rem").set("max-width", "100%");
        return layout;
    }
}
