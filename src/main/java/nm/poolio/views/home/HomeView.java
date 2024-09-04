package nm.poolio.views.home;

import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.data.Role;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.PoolService;
import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.vaadin.PoolioAvatar;
import nm.poolio.views.MainLayout;
import nm.poolio.views.transaction.PoolioTransactionGrid;
import org.apache.commons.lang3.BooleanUtils;

@PageTitle("Home \uD83C\uDFE0")
@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "home", layout = MainLayout.class)
@AnonymousAllowed
@Slf4j
public class HomeView extends VerticalLayout implements PoolioAvatar, PoolioTransactionGrid {
  private final PoolService poolService;
  private final PoolioTransactionService poolioTransactionService;
  @Getter Grid<PoolioTransaction> grid = createGrid(PoolioTransaction.class);
  @Getter User user;

  public HomeView(
      AuthenticatedUser authenticatedUser,
      PoolioTransactionService poolioTransactionService,
      PoolService poolService,
      PoolioTransactionService poolioTransactionService1) {
    this.poolioTransactionService = poolioTransactionService1;
    setSpacing(false);
    setHeight("100%");

    Optional<User> maybeUser = authenticatedUser.get();
    if (maybeUser.isPresent()) {
      user = maybeUser.get();

      Boolean aBoolean = (Boolean) VaadinSession.getCurrent().getAttribute("loggedInitDetails");

      if (BooleanUtils.isNotTrue(aBoolean)) {
        VaadinSession.getCurrent().setAttribute("loggedInitDetails", Boolean.TRUE);

        String ipAddress = VaadinSession.getCurrent().getBrowser().getAddress();
        InetAddress inetAddress = null; // can be slow
        try {
          inetAddress = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
          log.warn("Cant get inetAddress", e);
        }
        String hostname = inetAddress.getHostName();
        String browserApplication = VaadinSession.getCurrent().getBrowser().getBrowserApplication();

        log.info(
            "User: userName: {} from: {} with browser: {}",
            user.getUserName(),
            hostname,
            browserApplication);
      }

      String username = user.getUserName();
      String name = user.getName();
      Set<Role> roles = user.getRoles();

      add(createUserAvatar(user, AvatarVariant.LUMO_XLARGE));

      H2 header = new H2("Welcome, " + name + "!");
      header.addClassNames(Margin.Top.XLARGE, Margin.Bottom.MEDIUM);
      add(header);

      var fundsSpan = new Span(createBoldSpan("Funds: $"), new Span(String.valueOf(user.getFunds())));

      var usernameSpan = new Span(createBoldSpan(" Username: "), new Span(username));
      add(new Paragraph(fundsSpan, usernameSpan));

      var pools = user.getPoolIdNames(); // poolService.findPoolsForUser(user);

      var poolSpan = new Span();
      poolSpan.add(createBoldSpan("Pools:"));

      if (pools.isEmpty()) poolSpan.add(new Span(" You are not a member of any pools"));
      else
        pools.forEach(
            pool -> {
              // poolSpan.add(createPoolAvatar(pool, AvatarVariant.LUMO_XSMALL));
              poolSpan.add(new Span(" " + pool.getName() + " ")); // Use join with commas
            });

      if (roles.contains(Role.ADMIN))
        add(new Paragraph(poolSpan, new Span(createBoldSpan("Roles: "), new Span("" + roles))));
      else add(new Paragraph(poolSpan));

      decorateGrid();
      add(new Hr());
      var h3 = new H3("Latest Transactions: ");
      h3.getElement().getStyle().set("text-align", "left !important");
      add(h3);
      add(grid);
    } else {
      Image img = new Image("images/rico.jpeg", "NFL Football");
      add(img);

      H2 header = new H2("Login");
      header.addClassNames(Margin.Top.XLARGE, Margin.Bottom.MEDIUM);
      add(header);
      add(
          new Paragraph(
              "How much you wanna make a bet I can throw a football over them mountains?... Yeah... Coach woulda put me in fourth quarter, we would've been state champions. No doubt. No doubt in my mind."));

      Anchor loginLink = new Anchor("login", "Sign in");
      add(loginLink);
    }

    setSizeFull();
    // setJustifyContentMode(JustifyContentMode.CENTER);
    setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    getStyle().set("text-align", "center");
    this.poolService = poolService;
  }

  private Span createBoldSpan(String text) {
    var s = new Span(text);
    s.getElement().getStyle().set("font-weight", "bold");
    return s;
  }

  private void decorateGrid() {
    decorateTransactionGrid();

    grid.setItems(poolioTransactionService.findAllPoolioTransactionsForUser(user));
  }
}
