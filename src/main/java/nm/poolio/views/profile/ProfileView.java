package nm.poolio.views.profile;


import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.Theme;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.services.UserService;
import nm.poolio.vaadin.PoolioAvatar;
import nm.poolio.views.MainLayout;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@PageTitle("Profile ⚙\uFE0F.")
@Route(value = "profile", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "USER"})
@Slf4j
public class ProfileView extends VerticalLayout implements PoolioAvatar {
    private final AuthenticatedUser authenticatedUser;
    private final UserService userService;

    PasswordField newPasswordField;
    PasswordField confirmPasswordField;
    Dialog dialog;
    Avatar avatar;

    Button cancelButtonAvatar;
    Button saveButtonAvatar;

    byte[] avatarBytes;
    Span avatarSpan;

    Upload upload;

    public ProfileView(AuthenticatedUser authenticatedUser, UserService userService) {
        this.authenticatedUser = authenticatedUser;
        this.userService = userService;

        dialog = new Dialog();

        newPasswordField = new PasswordField("New Password");
        confirmPasswordField = new PasswordField("Confirm New Password");

        setUpPasswordField(newPasswordField);
        setUpPasswordField(confirmPasswordField);
        newPasswordField.clear();
        confirmPasswordField.clear();

        themeForm();

        add(new Hr());

        changePasswordDialog();

        add(new Hr());

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        var user = authenticatedUser.get().orElseThrow();
        avatar = createUserAvatar(user, AvatarVariant.LUMO_LARGE);
        avatarSpan = new Span(" Current User Avatar");
        horizontalLayout.add(avatar, avatarSpan);
        add(horizontalLayout);
        uploadFileSize();
        add(new Hr());
    }

    private void setUpPasswordField(PasswordField field) {
        field.setRequiredIndicatorVisible(true);
        field.setMinLength(6);
        field.setMaxLength(12);

        field.setI18n(new PasswordField.PasswordFieldI18n()
                .setRequiredErrorMessage("Field is required")
                .setMinLengthErrorMessage("Minimum length is 6 characters")
                .setMaxLengthErrorMessage("Maximum length is 12 characters")
        );
    }


    private void changePasswordDialog() {
        dialog.getHeader().add(new H2("Change Password"));
        dialog.add(createPasswordFormLayout());
        createFooter();

        Button button = new Button("Change Password", e -> dialog.open());
        add(dialog, button);
    }

    private void createFooter() {
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        Button saveButton = new Button("Change Password", e -> doStuff());

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(saveButton);
    }


    private void doStuff() {
        String newPassword = newPasswordField.getValue();
        String confirmPassword = confirmPasswordField.getValue();

        // Basic validation example
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Notification.show("New password and confirmation cannot be empty.", 3000, Notification.Position.MIDDLE);
        } else if (!newPassword.equals(confirmPassword)) {
            Notification.show("New password and confirmation do not match.", 3000, Notification.Position.MIDDLE);
        } else {
            changeUserPassword(newPassword);

            Notification.show("Password changed successfully", 3000, Notification.Position.MIDDLE);
            newPasswordField.clear();
            confirmPasswordField.clear();
            dialog.close();
        }
    }

    private void changeUserPassword(String newPassword) {
        var user = authenticatedUser.get().orElseThrow();
        var fresh = userService.get(user.getId()).orElseThrow();
        fresh.setHashedPassword(new BCryptPasswordEncoder().encode(newPassword));
        userService.update(fresh);
    }

    @NotNull
    private FormLayout createPasswordFormLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.add(newPasswordField, confirmPasswordField);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1)
        );
        return formLayout;
    }

    private void themeForm() {
        var user = authenticatedUser.get().orElseThrow();

        Select<String> select = new Select<>();
        select.setLabel("Set UI Theme");
        select.setItems(Theme.LIGHT.name(),
                Theme.DARK.name());
        select.setValue(user.getTheme() == null ? Theme.LIGHT.name() : Theme.DARK.name());

        select.addValueChangeListener(event -> {
            log.info("Select UI Theme value: {}", event.getValue());

            Theme theme = Theme.valueOf(event.getValue());
            ThemeList themeList = UI.getCurrent().getElement().getThemeList();
            MainLayout.setTheme(theme, themeList);

            var fresh = userService.get(user.getId()).orElseThrow();

            if (Theme.DARK == theme)
                fresh.setTheme(Theme.DARK);
            else
                fresh.setTheme(null);

            userService.update(fresh);
        });

        add(select);
    }

    public void uploadFileSize() {

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        cancelButtonAvatar = new Button("Revert Image", e -> resetUserAvatar());
        saveButtonAvatar = new Button("Save Image", e -> saveUserAvatar());
        enableFileButtons(false);
        saveButtonAvatar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        horizontalLayout.add(cancelButtonAvatar, saveButtonAvatar);


        UploadHandler inMemoryUploadHandler = UploadHandler.inMemory(
                (uploadMetadata, bytes) -> {
                    log.info("b : {}", bytes.length);
                    setAvatar(uploadMetadata, bytes);
                    enableFileButtons(true);
                    avatarSpan.setText(" Uploaded Avatar Image Preview");
                });
        upload = new Upload(inMemoryUploadHandler);
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/gif");

        int maxFileSizeInBytes = 2 * 1024 * 1024; // 2mb
        upload.setMaxFileSize(maxFileSizeInBytes);

        upload.addFileRejectedListener(event -> {
            String errorMessage = event.getErrorMessage();

            Notification notification = Notification.show(errorMessage, 5000,
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        UploadExamplesI18N i18n = new UploadExamplesI18N();
        i18n.getError().setFileIsTooBig(
                "The file exceeds the maximum allowed size of 2MB.");
        upload.setI18n(i18n);

        H4 title = new H4("Upload Avatar User Image 2mb Max Size");
        title.getStyle().set("margin-top", "0");


        add(title, upload, horizontalLayout);
    }

    private void enableFileButtons(boolean enabled) {
        cancelButtonAvatar.setEnabled(enabled);
        saveButtonAvatar.setEnabled(enabled);
    }

    private void saveUserAvatar() {
        enableFileButtons(false);
        var user = authenticatedUser.get().orElseThrow();
        var fresh = userService.get(user.getId()).orElseThrow();
        fresh.setProfilePicture(avatarBytes);
        userService.update(fresh);
        decorateAvatar(fresh, avatar, AvatarVariant.LUMO_LARGE);
        avatarBytes = null;
        upload.clearFileList();
    }

    private void resetUserAvatar() {
        enableFileButtons(false);
        var user = authenticatedUser.get().orElseThrow();
        decorateAvatar(user, avatar, AvatarVariant.LUMO_LARGE);
        avatarBytes = null;
        upload.clearFileList();
    }

    private void setAvatar(UploadMetadata uploadMetadata, byte[] bytes) {
        avatarBytes = bytes;

        DownloadHandler imageHandler = DownloadHandler.fromInputStream(event -> {
            InputStream inputStream = new ByteArrayInputStream(avatarBytes);

            return new DownloadResponse(
                    inputStream,
                    uploadMetadata.fileName(),
                    uploadMetadata.contentType(),
                    avatarBytes.length
            );
        });

        avatar.setImageHandler(imageHandler);


    }
}


