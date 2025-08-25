package nm.poolio.views.ticket;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.RolesAllowed;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.enitities.pool.PoolService;
import nm.poolio.enitities.ticket.Ticket;
import nm.poolio.enitities.ticket.TicketService;
import nm.poolio.enitities.transaction.NoteCreator;
import nm.poolio.model.JsonbNote;
import nm.poolio.model.NflGame;
import nm.poolio.model.enums.NflTeam;
import nm.poolio.model.enums.OverUnder;
import nm.poolio.model.enums.PoolStatus;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.services.NflGameService;
import nm.poolio.services.TicketUiService;
import nm.poolio.vaadin.PoolioAvatar;
import nm.poolio.vaadin.PoolioNotification;
import nm.poolio.views.MainLayout;
import org.springframework.util.CollectionUtils;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import static nm.poolio.utils.VaddinUtils.TIE_BREAKER_ICON;

@PageTitle("Edit Ticket \uFE0F")
@Route(value = "ticketEdit", layout = MainLayout.class)
@RolesAllowed("USER")
@Slf4j
public class TicketEditView extends VerticalLayout
        implements HasUrlParameter<String>,
        PoolioNotification,
        PoolioAvatar,
        NoteCreator,
        TicketEditUi {

    @Getter
    private final AuthenticatedUser authenticatedUser;
    private final User player;
    @Getter
    private final PoolService poolService;
    private final NflGameService nflGameService;
    @Getter
    private final TicketService ticketService;
    private final TicketUiService ticketUiService;
    private final TimeZone timeZone;
    IntegerField tieBreakerFiled;
    Pool pool;
    Ticket ticket;
    @Getter
    boolean errorFound = false;

    public TicketEditView(
            AuthenticatedUser authenticatedUser,
            PoolService poolService,
            NflGameService nflGameService,
            TicketService ticketService,
            TicketUiService ticketUiService) {
        this.poolService = poolService;
        this.nflGameService = nflGameService;
        player = authenticatedUser.get().orElseThrow();
        this.ticketService = ticketService;
        this.authenticatedUser = authenticatedUser;
        this.ticketUiService = ticketUiService;

        timeZone = MainLayout.getTimeZone();
    }

    @Override
    public void setErrorFound(boolean errorFound) {
    }

    public HasComponents getDialogHasComponents() {
        return this;
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String s) {
        Location location = event.getLocation();
        var optionalPoolId = location.getQueryParameters().getSingleParameter("poolId");

        if (optionalPoolId.isPresent()) processPoolIdParameter(optionalPoolId.get());
        else createNullPoolIdDialogAndGoHome();

        var optionalTicketId = location.getQueryParameters().getSingleParameter("ticketId");

        if (errorFound) return;

        optionalTicketId.ifPresent(this::processTicketIdParameter);

        if (errorFound) return;

        if (ticket == null) buildNewTicket();

        createTicketUI();
    }

    private void processPoolIdParameter(String poolIdParameter) {
        pool = findPoolWithQueryParam(poolIdParameter, player);
    }

    private void createNullPoolIdDialogAndGoHome() {
        errorFound = true;
        add(createErrorNotification(new Span("Cannot find pool with supplied poolId.")));
    }

    private void processTicketIdParameter(String ticketIdParameter) {
        ticket = processTicketIdParameter(ticketIdParameter, player);
    }

    private void buildNewTicket() {
        String userName = authenticatedUser.get().map(User::getName).orElse("unknown");
        String note =
                "User: %s Created ticket for pool: %s-%s"
                        .formatted(userName, pool.getName(), pool.getWeek());
        var jsonbNote = JsonbNote.builder().note(note).created(Instant.now()).user(userName).build();

        ticket =
                ticketUiService.createTicket(
                        createTicket(pool, player), pool, player, jsonbNote, pool.getWeek());
    }

    private void createTicketUI() {
        add(createHeaderBadgesTop(pool, ticket, timeZone));

        add(new Paragraph(
                "If a game has a point spread and a team listed as a favorite, the winner will be determined by this point spread. "
                        + "The final point spread will be the point spread at game time. We will use the score as our reference. "
                        + " https://www.thescore.com/nfl/events/"));

        add(new Paragraph("You can edit your game picks before the game starts or before the early the early (Noon) Sunday games begin." +
                " Once the noon games begin all games are locked \uD83D\uDD10 amd no changes can be made."));

        var games = nflGameService.getWeeklyGamesForPool(pool);

        var formLayout = new FormLayout();
        formLayout.setMaxWidth("900px");
        formLayout.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("200px", 2),
                new ResponsiveStep("400px", 3),
                new ResponsiveStep("6000px", 4));

        games.forEach(g -> formLayout.add(createGamePick(g)));

        add(formLayout);

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setPadding(true);
        buttonLayout.setAlignItems(Alignment.BASELINE);

        tieBreakerFiled = createTieBreakerField(ticket);
        buttonLayout.add(tieBreakerFiled);
        buttonLayout.add(createSubmitButton(e -> saveTicket()));

        AtomicBoolean showScoring = new AtomicBoolean(false);

        games.forEach(
                g -> {
                    if (!CollectionUtils.isEmpty(g.getSillies()) || g.getOverUnder() != null) {
                        showScoring.set(true);
                    }
                });

        if (showScoring.get()) {
            add(new Hr());
            add(createScoringBlurb());
            add(new Hr());
        }

        add(buttonLayout);
        add(new Hr());
        add(new Hr());

        HorizontalLayout spacerLayout = new HorizontalLayout();
        spacerLayout.setPadding(true);
        spacerLayout.setMargin(true);
        spacerLayout.add(new Hr());
        spacerLayout.setMinHeight(50, Unit.PIXELS);
        add(spacerLayout);
    }

    private Component createGamePick(NflGame nflGame) {
        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.getElement().getStyle().set("border", "1px solid black");

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.add(createGameRadioButton(nflGame));

        verticalLayout.add(horizontalLayout);

        if (nflGame.getOverUnder() != null) {
            var overUnder = createOverUnderField(nflGame, ticket);
            overUnder.addValueChangeListener(
                    c -> setOverUnderValue(ticket, c.getValue(), nflGame.getId()));
            verticalLayout.add(overUnder);
        }

        if (!CollectionUtils.isEmpty(nflGame.getSillies())) {
            var sillies =
                    nflGame.getSillies().stream().map(s -> createSillyRadio(s, ticket, nflGame)).toList();
            verticalLayout.add(sillies);
        }

        return verticalLayout;
    }

    private void saveTicket() {
        try {
            ticket.getSheet().setTieBreaker(tieBreakerFiled.getValue());

            var savedTicket = ticketService.save(ticket);
            log.debug("Saved ticket id: {}", savedTicket.getId());
            boolean ticketComplete = ticketService.isTicketComplete(savedTicket);

            if (ticketComplete) UI.getCurrent().navigate("ticketShow?ticketId=" + savedTicket.getId());
            else {
                ticket = savedTicket;
                VerticalLayout verticalLayout = new VerticalLayout();

                var missingPicks =
                        ticket.getSheet().getGamePicks().entrySet().stream()
                                .filter(e -> Objects.isNull(e.getValue()))
                                .map(e -> createMissingGamePick(e.getKey()))
                                .toList();

                if (missingPicks.isEmpty()) log.debug("No Picks missing");
                else if (missingPicks.size() > 3)
                    verticalLayout.add(new Div("Missing picks on " + missingPicks.size() + " games"));
                else {
                    verticalLayout.add(new Div("Missing picks:"));
                    verticalLayout.add(missingPicks);
                }

                if (tieBreakerFiled.getValue() == null) {
                    var tieBreaker = new Div(TIE_BREAKER_ICON.create());
                    tieBreaker.add(new Span(" Missing tie breaker"));
                    verticalLayout.add(tieBreaker);
                }

                var d = new Div("Ticket is Saved but it is not complete - Please Complete ASAP!");
                d.add(verticalLayout);
                add(createWarningNotification(d));
            }
        } catch (Exception e) {
            add(createErrorNotification(new Span("Ticket Not saved ERROR." + e.getMessage())));
        }
    }

    private RadioButtonGroup<NflTeam> createGameRadioButton(NflGame game) {

        boolean canUserChangeGame = canChangeGame(game);

        RadioButtonGroup<NflTeam> radioGroup = new RadioButtonGroup<>();

        radioGroup.setHelperComponent(createHelperSpread(game, canUserChangeGame));
        game.setTimeZone(timeZone);


        var t = DateTimeFormatter.ofPattern("E, h:mm").format(game.getLocalDateTimeWithZone());
        radioGroup.setLabel(game.getAwayTeam() + " v " + game.getHomeTeam() + " at " + t);
        radioGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        radioGroup.setRequired(true);
        radioGroup.setItems(game.getAwayTeam(), game.getHomeTeam());

        if (!canUserChangeGame) {
            radioGroup.setReadOnly(true);
        }

        var value = ticket.getSheet().getGamePicks().get(game.getId());
        if (value != null) radioGroup.setValue(value);

        radioGroup.addValueChangeListener(c -> setTicketGameValue(ticket, c.getValue(), game.getId()));

        radioGroup.setRenderer(
                new ComponentRenderer<>(
                        nflTeam -> {
                            var layout =
                                    new HorizontalLayout(
                                            createNflTeamAvatar(nflTeam, AvatarVariant.LUMO_SMALL),
                                            createGameSpan(nflTeam, game));
                            layout.setAlignItems(Alignment.CENTER);
                            return layout;
                        }));

        return radioGroup;
    }

    private void setOverUnderValue(Ticket ticket, OverUnder value, String id) {
        ticket.getSheet().getOverUnderPicks().put(id, value);
    }

    private Component createMissingGamePick(String gameKey) {
        var game = nflGameService.findGameById(gameKey);

        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(Alignment.BASELINE);

        layout.add(new Span(createNflTeamAvatar(game.getAwayTeam(), AvatarVariant.LUMO_XSMALL)));
        layout.add(new Span(" " + game.getAwayTeam().name() + " vs "));
        layout.add(new Span(createNflTeamAvatar(game.getHomeTeam(), AvatarVariant.LUMO_XSMALL)));
        layout.add(new Span(" " + game.getHomeTeam().name() + " "));

        return layout;
    }

    private Component createHelperSpread(NflGame g, boolean canUserChangeGame) {
        String locked = canUserChangeGame ? "" : " \uD83D\uDD10";
        if (g.getSpread() == null) return new Span(new Text(locked + "PICK EM"));

        String text = g.getSpread() < 0.0
                ? createSpreadSpan(g.getHomeTeam(), g.getSpread())
                : createSpreadSpan(g.getAwayTeam(), g.getSpread());

        return new Span(locked + text);
    }

    private boolean canChangeGame(NflGame g) {
        if (pool.getStatus() != PoolStatus.OPEN || g.getGameTime().isBefore(Instant.now())) {
            return false;
        }

        var gamesNotStarted = nflGameService.getWeeklyGamesNotStarted(pool.getWeek());
        if (gamesNotStarted.isEmpty()) {
            return false;
        }

        var firstGameNotStarted = gamesNotStarted.getFirst();
        var dayOfWeek = firstGameNotStarted.getLocalDateTime().getDayOfWeek();

        if (dayOfWeek == DayOfWeek.MONDAY) {
            return false;
        }

        return dayOfWeek != DayOfWeek.SUNDAY || firstGameNotStarted.getLocalDateTime().getHour() > 11;
    }


    private Component createGameSpan(NflTeam nflTeam, NflGame g) {
        if (g.getSpread() == null) return new Span(new Text(nflTeam.getFullName()));

        boolean isHomeTeam = g.getHomeTeam() == nflTeam;

        if (!isHomeTeam && g.getSpread() > 0.0) {
            return new Span(new Text(nflTeam.getFullName() + "  " + (-1 * g.getSpread())));
        }

        if (isHomeTeam && g.getSpread() <= 0.0) {
            return new Span(new Text(nflTeam.getFullName() + "  " + (g.getSpread())));
        }

        return new Span(new Text(nflTeam.getFullName()));
    }

    private String createSpreadSpan(NflTeam team, double spread) {
        return team.name() + " favored by " + Math.abs(spread) + " points.";
        //     return new Span(new Text(team.name() + " favored by " + Math.abs(spread) + " points."));
    }
}
