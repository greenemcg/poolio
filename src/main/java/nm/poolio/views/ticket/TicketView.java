package nm.poolio.views.ticket;

import static nm.poolio.utils.VaddinUtils.EDIT_ICON;
import static nm.poolio.utils.VaddinUtils.NEW_ICON;
import static nm.poolio.utils.VaddinUtils.VIEW_ICON;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.enitities.pool.PoolService;
import nm.poolio.enitities.ticket.Ticket;
import nm.poolio.enitities.ticket.TicketService;
import nm.poolio.model.enums.Season;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.services.TicketUiService;
import nm.poolio.services.TicketUiService.Status;
import nm.poolio.vaadin.PoolioNotification;
import nm.poolio.views.MainLayout;

@PageTitle("Tickets \uD83C\uDF9F\uFE0F")
@Route(value = "ticket", layout = MainLayout.class)
@RolesAllowed("USER")
@Slf4j
public class TicketView extends VerticalLayout implements PoolioNotification, TicketGrid {
  private final TicketService service;
  private final User player;
  private final PoolService poolService;
  private final TicketUiService ticketUiService;

  @Getter Grid<Ticket> grid = createGrid(Ticket.class);
  List<Ticket> allTickets = new ArrayList<>();

  public TicketView(
      TicketService service,
      AuthenticatedUser authenticatedUser,
      PoolService poolService,
      TicketUiService ticketUiService) {
    this.service = service;
    player = authenticatedUser.get().orElseThrow();
    this.poolService = poolService;
    this.ticketUiService = ticketUiService;

    var userPools =
        player.getPoolIdNames().stream()
            .map(idName -> poolService.get(idName.getId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();

    // List<Pool> userPools = poolService.findPoolsForUser(player);

    if (userPools.isEmpty()) {
      Div text =
          new Div(
              new Text("Your Account is not linked to any Poolio pools."),
              new HtmlComponent("br"),
              new Text("Contact your admin to join a pool."));

      Notification notification = createErrorNotification(text);

      notification.addDetachListener(
          openedChangeEvent -> {
            UI.getCurrent().navigate("home");
          });

      add(notification);
      notification.open();
    } else {
      HorizontalLayout layout = new HorizontalLayout();
      layout.setVerticalComponentAlignment(Alignment.CENTER);

      add(new H4("Active Pool Tickets:"));

      layout.setPadding(true);
      layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

      userPools.forEach(
          p -> {
            var optional = service.findTicketForUserCurrentWeek(p, player);

            if (optional.isPresent()) {
              var ticket = optional.get();
              switch (p.getStatus()) {
                case OPEN -> layout.add(createEditButton(p, ticket));
                case CLOSED, PENDING, PAID -> layout.add(createViewButton(p, ticket));
              }
            } else {
              if (p.getPlayers().size() > p.getMaxPlayersPerWeek()) {
                Span pending =
                    new Span(
                        "Cannot create new Ticket. Pool %sWeek :%d is Full with %d Players"
                            .formatted(
                                p.getName(), p.getWeek().getWeekNum(), p.getMaxPlayersPerWeek()));
                pending.getElement().getThemeList().add("badge");
                layout.add(pending);
              } else {
                switch (p.getStatus()) {
                  case OPEN -> layout.add(createNewButton(p));

                  case PAID, PENDING, CLOSED -> {
                    Span pending =
                        new Span(
                            "Cannot create new Ticket. Pool %sWeek :%d is %s"
                                .formatted(p.getName(), p.getWeek().getWeekNum(), p.getStatus()));
                    pending.getElement().getThemeList().add("badge");
                    layout.add(pending);
                  }
                }
              }
            }
          });

      decorateGrid();
      add(layout);
    }

    add(new H3("Tickets:"));
    add(grid);
  }

  private static Button createEditButton(Pool p, Ticket t) {
    return new Button(
        "Edit " + p.getName() + "-" + p.getWeek(),
        EDIT_ICON.create(),
        buttonClickEvent ->
            UI.getCurrent().navigate("ticketEdit?poolId=" + p.getId() + "&ticketId=" + t.getId()));
  }

  private Component createNewButton(Pool pool) {
    var status = ticketUiService.checkStatus(pool, player);
    if (status == Status.OK)
      return new Button(
          "New " + pool.getName() + "-" + pool.getWeek(),
          NEW_ICON.create(),
          buttonClickEvent -> UI.getCurrent().navigate("ticketEdit?poolId=" + pool.getId()));
    else return new Span("Cannot create ticket issue: %s".formatted(status));
  }

  private Component createViewButton(Pool pool, Ticket ticket) {
    return new Button(
        "View " + pool.getName() + "-" + pool.getWeek(),
        VIEW_ICON.create(),
        buttonClickEvent -> UI.getCurrent().navigate("result"));
    //  .navigate("ticketView?poolId=" + pool.getId() + "&ticketId=" + ticket.getId()));
  }

  private void decorateGrid() {
    decorateTicketGrid();
    getAllTickets();
  }

  private void getAllTickets() {
    allTickets = service.getAllTickets(player, Season.getCurrent());
    grid.setItems(allTickets);
  }

  public void viewTicket(Long ticketId) {
    UI.getCurrent().navigate("ticketShow?ticketId=" + ticketId);
  }
}
