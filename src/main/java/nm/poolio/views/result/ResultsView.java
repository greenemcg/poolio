package nm.poolio.views.result;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupItem;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.enitities.pool.PoolService;
import nm.poolio.enitities.pool.UserPoolFinder;
import nm.poolio.enitities.score.GameScoreService;
import nm.poolio.enitities.ticket.Ticket;
import nm.poolio.enitities.ticket.TicketService;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import nm.poolio.model.NflGame;
import nm.poolio.model.enums.NflTeam;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.services.NflGameScorerService;
import nm.poolio.services.NflGameService;
import nm.poolio.services.TicketScorerService;
import nm.poolio.vaadin.PoolioAvatar;
import nm.poolio.vaadin.PoolioBadge;
import nm.poolio.vaadin.PoolioNotification;
import nm.poolio.views.MainLayout;
import nm.poolio.views.ticket.TicketShowGrid;

@PageTitle("Results \uD83D\uDCC3")
@Route(value = "result", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "USER"})
@Slf4j
public class ResultsView extends VerticalLayout
    implements ResultsGrid, PoolioAvatar, PoolioBadge, PoolioNotification, UserPoolFinder {
  private final AuthenticatedUser authenticatedUser;
  private final PoolService poolService;
  private final TicketService ticketService;
  private final NflGameService nflGameService;
  private final GameScoreService gameScoreService;

  private final NflGameScorerService nflGameScorerService;
  private final TicketScorerService ticketScorerService;

  @Getter private final PoolioTransactionService poolioTransactionService;

  List<Ticket> ticketsList;

  @Getter Grid<Ticket> resultsGrid = createGrid(Ticket.class);

  User player;
  Pool pool;
  List<NflGame> weeklyGames;

  public ResultsView(
      AuthenticatedUser authenticatedUser,
      PoolService poolService,
      TicketService ticketService,
      NflGameService nflGameService,
      GameScoreService gameScoreService,
      NflGameScorerService nflGameScorerService,
      TicketScorerService ticketScorerService,
      PoolioTransactionService poolioTransactionService) {
    this.authenticatedUser = authenticatedUser;
    this.poolService = poolService;
    this.ticketService = ticketService;
    this.nflGameService = nflGameService;
    this.gameScoreService = gameScoreService;
    this.nflGameScorerService = nflGameScorerService;
    this.ticketScorerService = ticketScorerService;
    this.poolioTransactionService = poolioTransactionService;

    setHeight("100%");

    player = authenticatedUser.get().orElseThrow();
    var pools = findPoolsForUser(player.getPoolIdNames(), poolService);

    if (pools.isEmpty()) {
      add(createNoPoolNotification());
    } else {
      pool = pools.getFirst();

      weeklyGames = nflGameScorerService.getWeeklyGamesForPool(pool, pool.getWeek());

      if (!player.getAdmin() && Instant.now().isBefore(weeklyGames.getFirst().getGameTime())) {
        add(new H3("Games not started yet"));
        add(new Div("You can view all pics once the first game begins..."));

        var tickets = ticketService.findTickets(pool, pool.getWeek());
        HorizontalLayout usersLayout = new HorizontalLayout();
        usersLayout.add(new Span(tickets.size() + " Players: "));
        AvatarGroup avatarGroup = new AvatarGroup();
        avatarGroup.setMaxItemsVisible(25);

        var counter = new AtomicInteger();

        tickets.forEach(
            t -> {
              AvatarGroupItem avatar = new AvatarGroupItem(t.getPlayer().getName());
              avatar.setColorIndex((int) (t.getPlayer().getId() % 8));
              avatarGroup.add(avatar);
            });

        usersLayout.add(avatarGroup);

        add(usersLayout);

        var poolLayout = new HorizontalLayout();
        poolLayout.add(createBadge(new Span("  Pot: " + tickets.size() * pool.getAmount())));
        createPoolBadge(pool, poolLayout);

        var optional = tickets.stream().filter(t -> t.getPlayer().equals(player)).findFirst();

        optional.ifPresent(ticket -> createTicketBadge(ticket, poolLayout));

        add(poolLayout);

        if (optional.isPresent()) {
          var ticket = optional.get();
          GameGrid grid = new GameGrid(ticketService);

          var games =
              nflGameService.getWeeklyGamesThursdayFiltered(
                  ticket.getWeek(), ticket.getPool().isIncludeThursday());

          grid.decorateTicketGrid(ticket.getSheet().getGamePicks(), Map.of());
          grid.ticketGrid.setItems(games);
          add(grid.ticketGrid);

        } else {
          add(new Div("No ticket for you "));
        }

      } else {
        ticketsList = ticketScorerService.findAndScoreTickets(pool, pool.getWeek());

        //      ticketsList = ticketService.findTickets(pool, pool.getWeek());
        //      TicketScorer scorer = new TicketScorer(weeklyGames);
        //      ticketsList.forEach(scorer::score);
        //      ticketsList.sort(Comparator.comparing(Ticket::getFullScore).reversed());
        //      new TicketRanker(ticketsList).rank();

        decorateGrid();

        resultsGrid.setItems(ticketsList);

        add(resultsGrid);
      }
    }
  }

  void addScore(NflGame g) {
    var optional = gameScoreService.findScore(g.getId());

    if (optional.isPresent()) {
      var gameScore = optional.get();
      g.setHomeScore(gameScore.getHomeScore());
      g.setAwayScore(gameScore.getAwayScore());
    } else {
      g.setHomeScore(null);
      g.setAwayScore(null);
    }
  }

  private void addItem(NflGame game) {
    resultsGrid
        .addColumn(
            new ComponentRenderer<>(
                e -> {
                  var value = e.getSheet().getGamePicks().get(game.getId());
                  var optional = gameScoreService.findScore(game.getId());
                  String cssName = "font-weight";
                  String cssValue = "normal";

                  if (optional.isPresent()) {
                    var nflGame = nflGameService.findGameById(game.getId());
                    var gameScore = optional.get();
                    nflGame.setHomeScore(gameScore.getHomeScore());
                    nflGame.setAwayScore(gameScore.getAwayScore());

                    var winner = nflGame.getWinner();

                    // todo add this to nfl game view and share code

                    if (winner.equals(value)) {
                      cssValue = "bolder";
                    } else if (winner.equals(NflTeam.TBD)) {
                      cssValue = "normal";
                    } else if (winner.equals(NflTeam.TIE)) {
                      cssValue = "lighter";
                    } else {
                      cssName = "text-decoration";
                      cssValue = "line-through";
                    }
                  }

                  String result = "";
                  if (value != null) {
                    result = value.name();
                  }

                  var span = new Span(result);
                  span.getStyle().set(cssName, cssValue);

                  return span;
                }))
        .setHeader(createHeader(game))
        .setAutoWidth(true)
        .setTooltipGenerator(g -> getString(game))
        .setTextAlign(ColumnTextAlign.CENTER);
  }

  private String getString(NflGame game) {
    return (game.getAwayScore() == null || game.getHomeScore() == null)
        ? "No score recorded"
        : game.getAwayScore() + " v " + game.getHomeScore();
  }

  private Component createHeader(NflGame game) {
    Span span = new Span();
    span.add(createNflTeamAvatar(game.getAwayTeam(), AvatarVariant.LUMO_XSMALL));
    span.add("v");
    span.add(createNflTeamAvatar(game.getHomeTeam(), AvatarVariant.LUMO_XSMALL));
    return span;
  }

  private void decorateGrid() {
    var games = nflGameService.getWeeklyGamesForPool(pool);

    decoratePoolGrid();

    games.forEach(this::addItem);
  }

  @Override
  public String createTieBreakerString(Ticket ticket) {
    var optional = weeklyGames.getLast().getScore();

    if (optional.isPresent()) {
      var diff = optional.get() - ticket.getTieBreaker();
      return ticket.getSheet().getTieBreaker() + "-" + Math.abs(diff);
    } else return "" + ticket.getSheet().getTieBreaker();
  }

  class GameGrid implements TicketShowGrid {
    @Getter Grid<NflGame> ticketGrid = createGrid(NflGame.class);

    public GameGrid(TicketService ticketService) {}
  }
}
