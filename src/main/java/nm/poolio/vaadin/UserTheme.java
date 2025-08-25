package nm.poolio.vaadin;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.theme.lumo.Lumo;
import nm.poolio.data.Theme;
import nm.poolio.data.User;
import nm.poolio.security.AuthenticatedUser;

public interface UserTheme {


    default void setTheme(UI ui,  AuthenticatedUser authenticatedUser) {
        ThemeList themeList = ui.getElement().getThemeList();

        WebStorage.getItem(WebStorage.Storage.LOCAL_STORAGE, "theme")
                .thenAccept(
                        value -> {
                            ui.access(
                                    () -> {
                                        processTheme(authenticatedUser, value, themeList);
                                    });
                        });
    }

    private void processTheme(
            AuthenticatedUser authenticatedUser, String value, ThemeList themeList) {
        if (value != null) setThemeSafely(value, themeList);
        else authenticatedUser.get().ifPresent(user -> setThemeFromUser(user, themeList));
    }

    private void setThemeFromUser(User user, ThemeList themeList) {
        if (user.getTheme() != null) setTheme(user.getTheme(), themeList);
    }

    private void setThemeSafely(String value, ThemeList themeList) {
        try {
            setTheme(Theme.valueOf(value), themeList);
        } catch (IllegalArgumentException e) {
            WebStorage.removeItem(WebStorage.Storage.LOCAL_STORAGE, "theme");
        }
    }

   default void setTheme(Theme theme, ThemeList themeList) {
        String themeToAdd = theme == Theme.DARK ? Lumo.DARK : Lumo.LIGHT;
        String themeToRemove = theme == Theme.DARK ? Lumo.LIGHT : Lumo.DARK;

        if (!themeList.contains(themeToAdd)) {
            themeList.remove(themeToRemove);
            themeList.add(themeToAdd);
            WebStorage.setItem(WebStorage.Storage.LOCAL_STORAGE, "theme", theme.name());
        }
    }


}
