package nm.poolio.views;

import static nm.poolio.utils.VaddinUtils.*;
import static org.vaadin.lineawesome.LineAwesomeIcon.STORE_ALT_SOLID;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Optional;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.PoolService;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.vaadin.PoolioAvatar;
import nm.poolio.views.admin.AdminView;
import nm.poolio.views.bet.BetView;
import nm.poolio.views.home.HomeView;
import nm.poolio.views.nfl_game.NflGameView;
import nm.poolio.views.pool.PoolView;
import nm.poolio.views.result.ResultsView;
import nm.poolio.views.standings.StandingsView;
import nm.poolio.views.ticket.TicketView;
import nm.poolio.views.transaction.PoolioTransactionView;
import nm.poolio.views.user.UserView;
import org.vaadin.lineawesome.LineAwesomeIcon;

/** The main view is a top-level placeholder for other views. */
public class MainLayout extends AppLayout implements PoolioAvatar {
  private final AuthenticatedUser authenticatedUser;
  private final AccessAnnotationChecker accessChecker;
  private final PoolService poolService;

  private H1 viewTitle;
  private User user;

  public MainLayout(
      AuthenticatedUser authenticatedUser,
      AccessAnnotationChecker accessChecker,
      PoolService poolService) {
    this.authenticatedUser = authenticatedUser;
    this.accessChecker = accessChecker;
    this.poolService = poolService;

    Optional<User> maybeUser = authenticatedUser.get();
    maybeUser.ifPresent(value -> user = value);

    setPrimarySection(Section.DRAWER);
    addDrawerContent();
    addHeaderContent();
  }

  private void addHeaderContent() {
    DrawerToggle toggle = new DrawerToggle();
    toggle.setAriaLabel("Menu toggle");

    viewTitle = new H1();
    viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

    addToNavbar(true, toggle, viewTitle);
  }

  private void addDrawerContent() {
    Span appNameSpan = new Span("");
    appNameSpan.add(LineAwesomeIcon.FOOTBALL_BALL_SOLID.create());

    String title = createTitle(poolService.getAllowBets());

    appNameSpan.add(title);
    appNameSpan.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
    Header header = new Header(appNameSpan);

    Scroller scroller = new Scroller(createNavigation());

    addToDrawer(header, scroller, createFooter());
  }

  private String createTitle(Boolean allowBets) {
    String title = "Poolio";

    if (allowBets) return title + " \uD83E\uDDEADEMO\uD83E\uDDEA";
    else return title;
  }

  private SideNav createNavigation() {
    SideNav nav = new SideNav();

    if (accessChecker.hasAccess(HomeView.class))
      nav.addItem(new SideNavItem("Home", HomeView.class, STORE_ALT_SOLID.create()));

    if (accessChecker.hasAccess(TicketView.class))
      nav.addItem(new SideNavItem("Tickets", TicketView.class, TICKET_ICON.create()));

    if (accessChecker.hasAccess(PoolView.class))
      nav.addItem(new SideNavItem("Pools", PoolView.class, POOL_ICON.create()));

    if (poolService.getAllowBets() && accessChecker.hasAccess(BetView.class))
      nav.addItem(new SideNavItem("Bets", BetView.class, BET_ICON.create()));

    if (accessChecker.hasAccess(UserView.class))
      nav.addItem(new SideNavItem("Users", UserView.class, USERS_ICON.create()));

    if (accessChecker.hasAccess(AdminView.class))
      nav.addItem(new SideNavItem("Admin", AdminView.class, ADMIN_ICON.create()));

    nav.addItem(new SideNavItem("Games", NflGameView.class, GAMES_ICON.create()));

    if (accessChecker.hasAccess(ResultsView.class))
      nav.addItem(new SideNavItem("Results", ResultsView.class, RESULTS_ICON.create()));

    nav.addItem(new SideNavItem("Standings", StandingsView.class, TROPHY_ICON.create()));

    if (accessChecker.hasAccess(PoolioTransactionView.class))
      nav.addItem(
          new SideNavItem("Transactions", PoolioTransactionView.class, TRANSACTION_ICON.create()));

    return nav;
  }

  private Footer createFooter() {
    Footer layout = new Footer();

    if (user != null) {
      Avatar avatar = createUserAvatar(user, AvatarVariant.LUMO_SMALL);
      avatar.getElement().setAttribute("tabindex", "-1");

      MenuBar userMenu = new MenuBar();
      userMenu.setThemeName("tertiary-inline contrast");

      MenuItem userName = userMenu.addItem("");
      Div div = new Div();
      div.add(avatar);
      div.add(user.getName());
      div.add(new Icon("lumo", "dropdown"));
      div.getElement().getStyle().set("display", "flex");
      div.getElement().getStyle().set("align-items", "center");
      div.getElement().getStyle().set("gap", "var(--lumo-space-s)");
      userName.add(div);
      userName
          .getSubMenu()
          .addItem(
              "Sign out",
              e -> {
                authenticatedUser.logout();
              });

      layout.add(userMenu);
    } else {
      Anchor loginLink = new Anchor("login", "Sign in");
      layout.add(loginLink);
    }

    return layout;
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
