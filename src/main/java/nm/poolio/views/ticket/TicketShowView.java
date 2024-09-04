package nm.poolio.views.ticket;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.PoolService;
import nm.poolio.enitities.score.GameScoreService;
import nm.poolio.enitities.ticket.Ticket;
import nm.poolio.enitities.ticket.TicketService;
import nm.poolio.exceptions.PoolioException;
import nm.poolio.model.NflGame;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.services.NflGameScorerService;
import nm.poolio.services.NflGameService;
import nm.poolio.services.TicketScorerService;
import nm.poolio.views.MainLayout;

@PageTitle("Ticket View \uD83D\uDC40")
@Route(value = "ticketShow", layout = MainLayout.class)
@RolesAllowed("USER")
@Slf4j
public class TicketShowView extends VerticalLayout
    implements HasUrlParameter<String>, TicketEditUi, TicketShowGrid {
  @Getter private final PoolService poolService;
  @Getter private final TicketService ticketService;
  private final User player;
  private final NflGameService nflGameService;
  private final AuthenticatedUser authenticatedUser;

  private final GameScoreService gameScoreService;
  private final NflGameScorerService nflGameScorerService;
  private final TicketScorerService ticketScorerService;

  Ticket ticket;
  @Getter boolean errorFound = false;

  @Getter Grid<NflGame> ticketGrid = createGrid(NflGame.class);

  public TicketShowView(
      PoolService poolService,
      TicketService ticketService,
      AuthenticatedUser authenticatedUser,
      NflGameService nflGameService,
      GameScoreService gameScoreService,
      NflGameScorerService nflGameScorerService,
      TicketScorerService ticketScorerService) {
    this.poolService = poolService;
    this.ticketService = ticketService;
    this.authenticatedUser = authenticatedUser;

    player = authenticatedUser.get().orElseThrow();
    this.gameScoreService = gameScoreService;
    this.nflGameService = nflGameService;
    this.nflGameScorerService = nflGameScorerService;
    this.ticketScorerService = ticketScorerService;
  }

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String _ignored) {
    Location location = event.getLocation();

    var optionalTicketParameter = location.getQueryParameters().getSingleParameter("ticketId");

    if (optionalTicketParameter.isPresent()) processTicketParameter(optionalTicketParameter.get());
    else {
      add(createErrorNotification(new Span("Cannot find pool with supplied poolId.")));
      return;
    }

    if (player.equals(ticket.getPlayer())) {
      createUIComponents();
    } else {
      add(createErrorNotification(new Span("Wrong player found")));
    }
  }

  private void createUIComponents() {
    setHeight("100%");

    add(createHeaderBadges(ticket.getPool(), ticket));
    add(createBadge(new Span("TieBreaker: " + ticket.getTieBreaker())));

    var scoredTickets = ticketScorerService.findAndScoreTickets(ticket.getPool(), ticket.getWeek());

    HorizontalLayout horizontalLayout = new HorizontalLayout();
    horizontalLayout.setWidthFull();
    horizontalLayout.add(new Span( scoredTickets.size() + " Players: "));
    AvatarGroup avatarGroup = new AvatarGroup();
    avatarGroup.setMaxItemsVisible(25);
    scoredTickets.forEach(
        t -> {
          AvatarGroupItem avatar = new AvatarGroupItem(t.getPlayer().getName());
          avatar.setColorIndex((int) (t.getPlayer().getId() % 8));
          avatarGroup.add(avatar);
        });
    horizontalLayout.add(avatarGroup);
    add(horizontalLayout);

     ticket =
        scoredTickets.stream()
            .filter(t -> t.getPlayer().equals(player))
            .findFirst()
            .orElseThrow(() -> new PoolioException("Cannot find ticket"));

    var games =
        nflGameService.getWeeklyGamesThursdayFiltered(
            ticket.getWeek(), ticket.getPool().isIncludeThursday());

    var scores = gameScoreService.getScores(games);

    decorateTicketGrid(ticket.getSheet().getGamePicks(), scores);
    ticketGrid.setItems(games);

    add(ticketGrid);
  }

  private void processTicketParameter(String ticketIdParameter) {
    ticket = processTicketIdParameter(ticketIdParameter, player);
  }

  @Override
  public void setErrorFound(boolean errorFound) {
    this.errorFound = errorFound;
  }

  @Override
  public HasComponents getDialogHasComponents() {
    return this;
  }
}
