package nm.poolio.views.result;

import static nm.poolio.utils.VaddinUtils.*;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
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
import com.vaadin.flow.router.*;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
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
import nm.poolio.enitities.silly.SillyAnswer;
import nm.poolio.enitities.ticket.Ticket;
import nm.poolio.enitities.ticket.TicketService;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import nm.poolio.model.NflGame;
import nm.poolio.model.enums.NflTeam;
import nm.poolio.model.enums.NflWeek;
import nm.poolio.model.enums.OverUnder;
import nm.poolio.model.enums.PoolStatus;
import nm.poolio.push.Broadcaster;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.services.NflGameScorerService;
import nm.poolio.services.NflGameService;
import nm.poolio.services.TicketScorerService;
import nm.poolio.utils.TicketScorer;
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
    @Getter
    private final PoolioTransactionService poolioTransactionService;
    List<Ticket> ticketsList;
    @Getter
    Grid<Ticket> resultsGrid = createGrid(Ticket.class);
    User player;
    Pool pool;
    List<NflGame> weeklyGames;
    ComboBox<NflWeek> comboBox = new ComboBox<>("Change Week");
    MultiSelectComboBox<User> playerComboBox = new MultiSelectComboBox<>("Select Players");
    Span poolInfoSpan = new Span("Pool Value: $");
    Span poolDuplicateSpan = new Span("No Duplicates");
    Details details = new Details();
    Registration broadcasterRegistration;
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

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        broadcasterRegistration =
                Broadcaster.register(
                        newMessage -> {
                            ui.access(
                                    () -> {
                                        createInfoNotification(new Span(newMessage));
                                        ui.push();
                                        createUI();
                                        createSucessNotification(new Span("Results updated"));
                                        ui.push();
                                    });
                        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void createUI() {
        removeAll();

        var pools = findPoolsForUser(player.getPoolIdNames(), poolService);

        if (pools.isEmpty()) {
            add(createNoPoolNotification());
        } else {
            pool = pools.getFirst();
            weeklyGames = nflGameScorerService.getWeeklyGamesForPool(pool, week);

            if (!player.getAdmin() && Instant.now().isBefore(weeklyGames.getFirst().getGameTime())) {
                add(new H3("Games not started yet"));
                add(new Div("You can view all pics once the first game begins..."));
                displayPlayers();
            } else {
                ticketsList = ticketScorerService.findAndScoreTickets(pool, week);
                displayResults();
            }
        }
    }

    private void displayPlayers() {
        var tickets = ticketService.findTickets(pool, week);
        HorizontalLayout usersLayout = new HorizontalLayout();
        usersLayout.add(new Span(tickets.size() + " Players: "));
        AvatarGroup avatarGroup = new AvatarGroup();
        avatarGroup.setMaxItemsVisible(25);

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

        tickets.stream()
                .filter(t -> t.getPlayer().equals(player))
                .findFirst()
                .ifPresent(ticket -> createTicketBadge(ticket, poolLayout));

        add(poolLayout);
    }

    private void displayResults() {
        HorizontalLayout badgesHorizontalLayout =
                new HorizontalLayout(poolInfoSpan, poolDuplicateSpan, details);
        badgesHorizontalLayout.setPadding(false);
        badgesHorizontalLayout.setSpacing(true);
        add(badgesHorizontalLayout);

        processDuplicates();

        HorizontalLayout comboBoxesHorizontalLayout = new HorizontalLayout();
        comboBox.setItems(computeWeekValue(NflWeek.values(), pool.getWeek()));
        comboBox.getStyle().set("--vaadin-combo-box-overlay-width", "16em");
        comboBox.setValue(week);
        comboBox.addValueChangeListener(e -> changeWeek(e.getValue()));
        comboBoxesHorizontalLayout.add(comboBox);

        var players =
                ticketsList.stream()
                        .map(Ticket::getPlayer)
                        .sorted(Comparator.comparing(User::getName))
                        .toList();
        playerComboBox.setItemLabelGenerator(User::getName);
        playerComboBox.setItems(players);
        playerComboBox.select(players);
        playerComboBox.setClearButtonVisible(true);
        playerComboBox.setAutoExpand(AutoExpandMode.HORIZONTAL);
        playerComboBox.addValueChangeListener(e -> filterResultsByPlayers(e.getValue()));
        comboBoxesHorizontalLayout.add(playerComboBox);
        add(comboBoxesHorizontalLayout);

        decorateGrid();

        poolInfoSpan.setText(
                String.format(
                        POOL_INFO_TEMPLATE, pool.getName(), players.size(), pool.getAmount() * players.size()));
        resultsGrid.setItems(ticketsList);
        add(resultsGrid);
    }

    private void filterResultsByPlayers(Set<User> usersPicked) {
        var usersPickedTickets =
                ticketsList.stream().filter(t -> usersPicked.contains(t.getPlayer())).toList();
        resultsGrid.setItems(usersPickedTickets);
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
            week =
                    weekIn == null
                            ? findPoolsForUser(player.getPoolIdNames(), poolService).getFirst().getWeek()
                            : weekIn;
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
                    String.format(
                            POOL_INFO_TEMPLATE,
                            pool.getName(),
                            players.size(),
                            pool.getAmount() * players.size()));
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
                                ticket -> {
                                    Div layout = new Div();
                                    var span = createGameSpan(game, ticket);
                                    layout.add(span);
                                    if (game.getOverUnder() != null) {
                                        var choice = ticket.getSheet().getOverUnderPicks().get(game.getId());

                                        if (choice != null) {
                                            layout.add(new Span(" - "));
                                            layout.add(new Span(createOverUndeSpan(game, ticket)));
                                        }
                                    }

                                    return layout;
                                }))
                .setHeader(createHeader(game))
                .setAutoWidth(true)
                .setTooltipGenerator(g -> getString(game))
                .setTextAlign(ColumnTextAlign.CENTER);

        if (!CollectionUtils.isEmpty(game.getSillies()))
            game.getSillies()
                    .forEach(
                            sillyQuestion -> {
                                resultsGrid
                                        .addColumn(
                                                new ComponentRenderer<>(
                                                        ticket -> {
                                                            SillyAnswer sillyAnswer =
                                                                    ticket.getSheet().getSillyPicks().get(sillyQuestion.getId());

                                                            Div div = new Div();

                                                            if (sillyAnswer != null) {
                                                                div.add(new Span(sillyAnswer.getAnswer()));

                                                                if (!CollectionUtils.isEmpty(game.getSillyAnswers())
                                                                        && game.getSillyAnswers().containsKey(sillyQuestion.getId())) {
                                                                    String correctAnswer =
                                                                            game.getSillyAnswers().get(sillyQuestion.getId());

                                                                    if (correctAnswer.equals(sillyAnswer.getAnswer())) {
                                                                        div.getStyle().set("font-weight", "bolder");
                                                                    } else {
                                                                        div.getStyle().set("text-decoration", "line-through");
                                                                    }
                                                                }
                                                            }
                                                            return div;
                                                        }))
                                        .setHeader(createIconSpan(SILLY_QUESTION, sillyQuestion.getQuestion()))
                                        .setWidth("125px")
                                        .setTextAlign(ColumnTextAlign.CENTER);
                            });
    }

    @NotNull
    private Span createOverUndeSpan(NflGame game, Ticket e) {
        String cssName = "font-weight";
        String cssValue = "normal";

        OverUnder playerOverUnder = e.getSheet().getOverUnderPicks().get(game.getId());

        if (playerOverUnder == null) {
            return new Span("");
        }

        String value = playerOverUnder.name();

        var optional = game.getScore();

        if (optional.isPresent()) {
            var gameScore = optional.get();

            var overUnderResult = TicketScorer.computeOverUnderValue(gameScore, game.getOverUnder());

            if (overUnderResult == null) {
                cssName = "font-style";
                cssValue = "oblique";
            } else if (playerOverUnder == overUnderResult) {
                cssValue = "bolder";
            } else {
                cssName = "text-decoration";
                cssValue = "line-through";
            }
        }

        var span = new Span(value);
        span.getStyle().set(cssName, cssValue);
        return span;
    }

    private boolean showTeam(NflGame game) {
        var gamesNotStarted = nflGameService.getWeeklyGamesNotStarted(pool.getWeek());

        if (gamesNotStarted.isEmpty()) {
            return true;
        }

        if (game.getGameTime().isBefore(Instant.now())) {
            return true;
        }


        var firstGameNotStarted = gamesNotStarted.getFirst();
        var dayOfWeek = firstGameNotStarted.getLocalDateTime().getDayOfWeek();

        if (dayOfWeek == DayOfWeek.MONDAY) {
            return true;
        }

        return dayOfWeek == DayOfWeek.SUNDAY && firstGameNotStarted.getLocalDateTime().getHour() <= 11;
    }


    @NotNull
    private Span createGameSpan(NflGame game, Ticket e) {// tod make class shared with over under

        if( !showTeam(game)) {
            return new Span("\uD83E\uDEE3");
        }

        var value = e.getSheet().getGamePicks().get(game.getId());
        var optional = gameScoreService.findScore(game.getId());
        String cssName = "font-weight";
        String cssValue = "normal";

        if (optional.isPresent()) {
            var nflGame = nflGameService.findGameById(game.getId());
            var gameScore = optional.get();
            nflGame.setHomeScore(gameScore.getHomeScore());
            nflGame.setAwayScore(gameScore.getAwayScore());

            var winner = nflGame.findWinner();

            if (winner.equals(value)) {
                cssValue = "bolder";
            } else if (winner.equals(NflTeam.TBD)) {
                cssValue = "normal";
            } else if (winner.equals(NflTeam.TIE)) {
                cssName = "font-style";
                cssValue = "oblique";
            } else {
                cssName = "text-decoration";
                cssValue = "line-through";
            }
        }

        var span = new Span(value != null ? value.name() : "");
        span.getStyle().set(cssName, cssValue);
        return span;
    }

    private String getString(NflGame game) {
        return (game.getAwayScore() == null || game.getHomeScore() == null)
                ? "No score recorded"
                : game.getAwayScore() + " v " + game.getHomeScore();
    }

    private Component createHeader(NflGame game) {

        Span span = new Span();

        if (game.getSpread() != null && game.getSpread() > 0) {
            Span boldTextSpan = new Span("" + (game.getSpread() * -1));
            boldTextSpan.getElement().getStyle().set("font-weight", "bold");
            span.add(boldTextSpan);
        }

        span.add(createNflTeamAvatar(game.getAwayTeam(), AvatarVariant.LUMO_XSMALL));
        span.add("v");
        span.add(createNflTeamAvatar(game.getHomeTeam(), AvatarVariant.LUMO_XSMALL));

        if (game.getSpread() != null && game.getSpread() < 0) {
            Span boldTextSpan = new Span("" + game.getSpread());
            boldTextSpan.getElement().getStyle().set("font-weight", "bold");
            span.add(boldTextSpan);
        }

        if (game.getOverUnder() != null) {
            span.add(new Span(" - " + game.getOverUnder()));
        }

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
        return optional
                .map(
                        score ->
                                ticket.getSheet().getTieBreaker() + "-" + Math.abs(score - ticket.getTieBreaker()))
                .orElseGet(() -> "" + ticket.getSheet().getTieBreaker());
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String _ignored) {
        Location location = event.getLocation();
        var optionalWeekParameter = location.getQueryParameters().getSingleParameter("week");

        week =
                optionalWeekParameter
                        .map(NflWeek::valueOf)
                        .orElseGet(
                                () -> findPoolsForUser(player.getPoolIdNames(), poolService).getFirst().getWeek());

        createUI();
    }

    @Getter
    class GameGrid implements TicketShowGrid {
        Grid<NflGame> ticketGrid = createGrid(NflGame.class);

        public GameGrid(TicketService ticketService) {
        }
    }
}
