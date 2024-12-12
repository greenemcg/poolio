package nm.poolio.views.result;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupItem;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox.AutoExpandMode;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.*;
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
import nm.poolio.model.enums.NflWeek;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.services.NflGameScorerService;
import nm.poolio.services.NflGameService;
import nm.poolio.services.TicketScorerService;
import nm.poolio.vaadin.PoolioAvatar;
import nm.poolio.vaadin.PoolioBadge;
import nm.poolio.vaadin.PoolioNotification;
import nm.poolio.views.MainLayout;
import nm.poolio.views.ticket.TicketShowGrid;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@PageTitle("Results \uD83D\uDCC3")
@Route(value = "result", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "USER"})
@Slf4j
public class ResultsView extends VerticalLayout
    implements ResultsGrid,
        PoolioAvatar,
        PoolioBadge,
        PoolioNotification,
        UserPoolFinder,
        HasUrlParameter<String> {
  private static final String POOL_INFO_TEMPLATE = "%s • %d Players • $%d";
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
  ComboBox<NflWeek> comboBox = new ComboBox<>("Change Week");
  MultiSelectComboBox<User> playerComboBox = new MultiSelectComboBox<>("Select Players");
  Span poolInfoSpan = new Span("Pool Value: $");
  Span poolDuplicateSpan = new Span("No Duplicates");
  Details details = new Details();
  private NflWeek week;

  public ResultsView(
      AuthenticatedUser authenticatedUser,
      PoolService poolService,
      TicketService ticketService,
      NflGameService nflGameService,
      GameScoreService gameScoreService,
      NflGameScorerService nflGameScorerService,
      TicketScorerService ticketScorerService,
      PoolioTransactionService poolioTransactionService) {
    this.poolService = poolService;
    this.ticketService = ticketService;
    this.nflGameService = nflGameService;
    this.gameScoreService = gameScoreService;
    this.nflGameScorerService = nflGameScorerService;
    this.ticketScorerService = ticketScorerService;
    this.poolioTransactionService = poolioTransactionService;

    setHeight("100%");
    player = authenticatedUser.get().orElseThrow();
    poolInfoSpan.getElement().getThemeList().add("badge success");
    poolDuplicateSpan.getElement().getThemeList().add("badge contrast");

    Span span = new Span("Duplicates");
    span.getElement().getThemeList().add("badge contrast");
    details.setSummary(span);
  }

  private void createUI() {
    var pools = findPoolsForUser(player.getPoolIdNames(), poolService);

    if (pools.isEmpty()) {
      add(createNoPoolNotification());
    } else {
      pool = pools.getFirst();

      weeklyGames = nflGameScorerService.getWeeklyGamesForPool(pool, week);

      if (!player.getAdmin() && Instant.now().isBefore(weeklyGames.getFirst().getGameTime())) {
        add(new H3("Games not started yet"));
        add(new Div("You can view all pics once the first game begins..."));

        var tickets = ticketService.findTickets(pool, week);
        HorizontalLayout usersLayout = new HorizontalLayout();
        usersLayout.add(new Span(tickets.size() + " Players: "));
        AvatarGroup avatarGroup = new AvatarGroup();
        avatarGroup.setMaxItemsVisible(25);

        var counter = new AtomicInteger(); // todo fix dup

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
        ticketsList = ticketScorerService.findAndScoreTickets(pool, week);

        //      ticketsList = ticketService.findTickets(pool, pool.getWeek());
        //      TicketScorer scorer = new TicketScorer(weeklyGames);
        //      ticketsList.forEach(scorer::score);
        //      ticketsList.sort(Comparator.comparing(Ticket::getFullScore).reversed());
        //      new TicketRanker(ticketsList).rank();

        HorizontalLayout badgesHorizontalLayout = new HorizontalLayout();
        badgesHorizontalLayout.setPadding(false);
        badgesHorizontalLayout.setSpacing(true);
        badgesHorizontalLayout.add(poolInfoSpan);
        badgesHorizontalLayout.add(poolDuplicateSpan);
        badgesHorizontalLayout.add(details);
        add(badgesHorizontalLayout);

        processDuplicates();

        //       var duplicates = findDups();
        //        badgesHorizontalLayout.add(poolDuplicateSpan);
        //        badgesHorizontalLayout.add(details);
        //
        //         poolDuplicateSpan.setVisible(duplicates.isEmpty());
        //        details.setVisible(!duplicates.isEmpty());
        //        details.removeAll();
        //        duplicates.forEach(
        //            d -> {
        //              var span = new Span(d);
        //              span.getElement().getThemeList().add("badge");
        //              details.add(span);
        //            });

        HorizontalLayout comboBoxesHorizontalLayout = new HorizontalLayout();
        comboBox.setItems(computeWeekValue(NflWeek.values(), pool.getWeek()));
        comboBox.getStyle().set("--vaadin-combo-box-overlay-width", "16em");
        comboBox.setValue(week);
        comboBox.addValueChangeListener(e -> changeWeek(e.getValue()));
        comboBoxesHorizontalLayout.add(comboBox);

        var players =
            ticketsList.stream()
                .map(t -> t.getPlayer())
                .sorted(Comparator.comparing(User::getName))
                .toList();
        playerComboBox.setItemLabelGenerator(User::getName);
        playerComboBox.setItems(players);
        playerComboBox.select(players);
        playerComboBox.setClearButtonVisible(true);
        playerComboBox.setAutoExpand(AutoExpandMode.HORIZONTAL);

        playerComboBox.addValueChangeListener(
            e -> {
              var usersPicked = e.getValue().stream().toList();
              var usersPickedTickets =
                  ticketsList.stream().filter(t -> usersPicked.contains(t.getPlayer())).toList();
              resultsGrid.setItems(usersPickedTickets);
            });

        comboBoxesHorizontalLayout.add(playerComboBox);
        add(comboBoxesHorizontalLayout);

        decorateGrid();

        poolInfoSpan.setText(
            POOL_INFO_TEMPLATE.formatted(
                pool.getName(), players.size(), pool.getAmount() * players.size()));

        resultsGrid.setItems(ticketsList);

        add(resultsGrid);
      }
    }
  }

  private List<String> findDups() {
    Map<Integer, List<String>> map = processDups(ticketsList);

    return map.values().stream().filter(v -> v.size() > 1).map(this::joinNames).toList();
  }

  private String joinNames(List<String> v) {
    return String.join(", ", v);
  }

  private Map<Integer, List<String>> processDups(List<Ticket> ticketsList) {
    Map<Integer, List<String>> map = new HashMap<>();
    ticketsList.forEach(
        t -> {
          if (!CollectionUtils.isEmpty(t.getSheet().getGamePicks()))
            map.computeIfAbsent(t.getSheet().getGamePicks().hashCode(), k -> new ArrayList<>())
                .add(t.getPlayer().getName());
        });

    return map;
  }

  private void changeWeek(NflWeek weekIn) {

    if (weekIn != null && (pool.getWeek().getWeekNum() < weekIn.getWeekNum())) {
      createErrorNotification(new Span("Week not started"));
      comboBox.setValue(week);
    } else {
      if (weekIn == null) {
        var pools = findPoolsForUser(player.getPoolIdNames(), poolService);
        week = pools.getFirst().getWeek();
      } else week = weekIn;

      resultsGrid.removeAllColumns();
      weeklyGames = nflGameScorerService.getWeeklyGamesForPool(pool, week);
      ticketsList = ticketScorerService.findAndScoreTickets(pool, week);

      var players =
          ticketsList.stream()
              .map(Ticket::getPlayer)
              .sorted(Comparator.comparing(User::getName))
              .toList();

      playerComboBox.setItems(players);
      playerComboBox.select(players);

      poolInfoSpan.setText(
          POOL_INFO_TEMPLATE.formatted(
              pool.getName(), players.size(), pool.getAmount() * players.size()));

      processDuplicates();

      resultsGrid.setItems(ticketsList);
      decorateGrid();
    }
  }

  private void processDuplicates() {
    var duplicates = findDups();

    poolDuplicateSpan.setVisible(duplicates.isEmpty());
    details.setVisible(!duplicates.isEmpty());
    details.removeAll();

    if (!duplicates.isEmpty()) {
      VerticalLayout content = new VerticalLayout();
      content.setSpacing(false);
      content.setPadding(false);
      AtomicInteger count = new AtomicInteger();

      duplicates.forEach(
          d -> {
            count.addAndGet(StringUtils.countOccurrencesOf(d, ",") + 1);

            var span = new Div("(" + d + ")");
            span.getElement().getThemeList().add("badge");
            content.add(span);
          });

      details.getSummary().getElement().setText(count.get() + " Duplicates");
      details.add(content);
    }
  }

  private List<NflWeek> computeWeekValue(NflWeek[] values, @NotNull NflWeek week) {
    return Arrays.stream(values)
        .filter(w -> w.getWeekNum() > 0 && w.getWeekNum() <= week.getWeekNum())
        .toList();
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
    var games = nflGameService.getWeeklyGamesForPool(pool, week);

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

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String _ignored) {
    Location location = event.getLocation();

    var optionalWeekParameter = location.getQueryParameters().getSingleParameter("week");

    if (optionalWeekParameter.isPresent()) week = NflWeek.valueOf(optionalWeekParameter.get());
    else {
      if (CollectionUtils.isEmpty(player.getPoolIdNames())) {
        add(createErrorNotificationAndGoHome("Cannot find pool with supplied poolId."));
      } else {
        var pools = findPoolsForUser(player.getPoolIdNames(), poolService);
        week = pools.getFirst().getWeek();
      }
    }

    createUI();
  }

  class GameGrid implements TicketShowGrid {
    @Getter Grid<NflGame> ticketGrid = createGrid(NflGame.class);

    public GameGrid(TicketService ticketService) {}
  }
}
