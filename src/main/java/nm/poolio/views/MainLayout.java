package nm.poolio.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.theme.lumo.LumoUtility;
import nm.poolio.data.User;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.vaadin.PoolioAvatar;
import nm.poolio.vaadin.UserTheme;
import nm.poolio.views.admin.AdminView;
import nm.poolio.views.bet.BetView;
import nm.poolio.views.home.HomeView;
import nm.poolio.views.nfl_game.NflGameView;
import nm.poolio.views.pool.PoolView;
import nm.poolio.views.profile.ProfileView;
import nm.poolio.views.result.ResultsView;
import nm.poolio.views.standings.StandingsView;
import nm.poolio.views.ticket.TicketView;
import nm.poolio.views.transaction.PoolioTransactionView;
import nm.poolio.views.user.UserView;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.TimeZone;

import static nm.poolio.utils.VaddinUtils.*;
import static org.vaadin.lineawesome.LineAwesomeIcon.STORE_ALT_SOLID;
import static org.vaadin.lineawesome.LineAwesomeIcon.USER_EDIT_SOLID;

/**
 * The main view is a top-level placeholder for other views.
 */
@CssImport(value = "./themes/styles.css")
public class MainLayout extends AppLayout implements PoolioAvatar, UserTheme {
    private final AuthenticatedUser authenticatedUser;
    private final AccessAnnotationChecker accessChecker;
    private H1 viewTitle;
    private User user;
    private final UI ui;

    public MainLayout(
            AuthenticatedUser authenticatedUser,
            AccessAnnotationChecker accessChecker) {
        this.authenticatedUser = authenticatedUser;
        this.accessChecker = accessChecker;

        Optional<User> maybeUser = authenticatedUser.get();
        maybeUser.ifPresent(value -> user = value);

        ui = UI.getCurrent();
        ui.addBeforeEnterListener(this::handleBeforeEvent);

        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void handleBeforeEvent(BeforeEnterEvent event) {
        setTheme(ui, authenticatedUser);
        setTimeZone(ui);

        System.out.println(ZonedDateTime.now().format(DateTimeFormatter.ISO_TIME)
                + " - View: "
                + event.getNavigationTarget().getSimpleName()
                + " - For User: "
                + (user == null ? null : user.getUserName()));
    }


    public static TimeZone getTimeZone() {
        UI ui = UI.getCurrent();
        String timeZoneId = (ui == null || ui.getSession().getAttribute("tz") == null)
                ? "America/New_York"
                : ui.getSession().getAttribute("tz").toString();
        return TimeZone.getTimeZone(timeZoneId);
    }

    private void setTimeZone(UI ui) {
        if (ui.getSession().getAttribute("tz") == null)
            ui.getPage()
                    .retrieveExtendedClientDetails(
                            details ->
                                    ui.getSession().setAttribute("tz", details.getTimeZoneId())
                    );
    }


    private void addDrawerContent() {
        Span appNameSpan = new Span("");
        appNameSpan.add(LineAwesomeIcon.FOOTBALL_BALL_SOLID.create());

        String title = createTitle();

        appNameSpan.add(title);
        appNameSpan.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
        Header header = new Header(appNameSpan);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H1();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        addToNavbar(true, toggle, viewTitle);
    }


    private String createTitle() {
        return "Poolio";
    }

    private SideNav createNavigation() {
        SideNav sideNav = new SideNav();

        addNavItem(sideNav, "Home", HomeView.class, STORE_ALT_SOLID);
        addNavItem(sideNav, "Profile", ProfileView.class, USER_EDIT_SOLID);
        addNavItem(sideNav, "Tickets", TicketView.class, TICKET_ICON);
        addNavItem(sideNav, "Pools", PoolView.class, POOL_ICON);
        addNavItem(sideNav, "Bets", BetView.class, BET_ICON);
        addNavItem(sideNav, "Users", UserView.class, USERS_ICON);
        addNavItem(sideNav, "Admin", AdminView.class, ADMIN_ICON);
        addNavItem(sideNav, "Games", NflGameView.class, GAMES_ICON);
        addNavItem(sideNav, "Results", ResultsView.class, RESULTS_ICON);
        addNavItem(sideNav, "Standings", StandingsView.class, TROPHY_ICON);
        addNavItem(sideNav, "Transactions", PoolioTransactionView.class, TRANSACTION_ICON);

        return sideNav;
    }

    private void addNavItem(SideNav sideNav, String label, Class<? extends Component> viewClass, LineAwesomeIcon icon) {
        if (accessChecker.hasAccess(viewClass)) sideNav.addItem(new SideNavItem(label, viewClass, icon.create()));
    }

    private Footer createFooter() {
        Footer layout = new Footer();

        if (user != null) layout.add(createUserMenu());
        else layout.add(new Anchor("login", "Sign in"));

        return layout;
    }

    private MenuBar createUserMenu() {
        Avatar avatar = createUserAvatar(user, AvatarVariant.LUMO_SMALL);
        avatar.getElement().setAttribute("tabindex", "-1");

        MenuBar userMenu = new MenuBar();
        userMenu.setThemeName("tertiary-inline contrast");

        MenuItem userName = userMenu.addItem("");
        Div div = new Div(avatar, new Text(user.getName()), new Icon("lumo", "dropdown"));
        div.getStyle().set("display", "flex")
                .set("align-items", "center")
                .set("gap", "var(--lumo-space-s)");
        userName.add(div);

        userName.getSubMenu().addItem("Sign out", e -> authenticatedUser.logout());

        return userMenu;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }
}
