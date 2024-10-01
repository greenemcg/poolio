package nm.poolio.views.ticket;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Div;
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
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.services.NflGameService;
import nm.poolio.services.TicketUiService;
import nm.poolio.vaadin.PoolioAvatar;
import nm.poolio.vaadin.PoolioNotification;
import nm.poolio.views.MainLayout;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

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
    }

    public HasComponents getDialogHasComponents() {
        return this;
    }

    private void createNullPoolIdDialogAndGoHome() {
        errorFound = true;
        add(createErrorNotification(new Span("Cannot find pool with supplied poolId.")));
    }

    @Override
    public void setErrorFound(boolean errorFound) {
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

    private void createTicketUI() {
        add(createHeaderBadgesTop(pool, ticket));

        var games = nflGameService.getWeeklyGamesForPool(pool);

        var formLayout = new FormLayout();
        formLayout.setMaxWidth("900px");
        formLayout.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("200px", 2),
                new ResponsiveStep("400px", 3),
                new ResponsiveStep("6000px", 4));

        games.forEach(g -> formLayout.add(createGameRadioButton(g)));

        add(formLayout);
        tieBreakerFiled = createTieBreakerField(ticket);
        add(tieBreakerFiled);
        add(createSubmitButton(e -> saveTicket()));
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

    private RadioButtonGroup<NflTeam> createGameRadioButton(NflGame g) {
        RadioButtonGroup<NflTeam> radioGroup = new RadioButtonGroup<>();
        var t = DateTimeFormatter.ofPattern("E, h:mm").format(g.getLocalDateTime());
        radioGroup.setLabel(g.getAwayTeam() + " v " + g.getHomeTeam() + " at " + t);
        radioGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        radioGroup.setRequired(true);
        radioGroup.setItems(g.getAwayTeam(), g.getHomeTeam());

        var value = ticket.getSheet().getGamePicks().get(g.getId());

        if (value != null) radioGroup.setValue(value);

        radioGroup.addValueChangeListener(c -> setTicketGameValue(ticket, c.getValue(), g.getId()));

        radioGroup.setRenderer(
                new ComponentRenderer<>(
                        nflTeam -> {
                            var layout =
                                    new HorizontalLayout(
                                            createNflTeamAvatar(nflTeam, AvatarVariant.LUMO_SMALL),
                                            new Span(new Text(nflTeam.getFullName())));
                            layout.setAlignItems(Alignment.CENTER);
                            return layout;
                        }));

        return radioGroup;
    }

    private void processTicketIdParameter(String ticketIdParameter) {
        ticket = processTicketIdParameter(ticketIdParameter, player);
    }

    private void processPoolIdParameter(String poolIdParameter) {
        pool = findPoolWithQueryParam(poolIdParameter, player);
    }

    private void buildNewTicket() {
        String userName = "unknown";
        if (authenticatedUser.get().isPresent()) {
            userName = authenticatedUser.get().get().getName();
        }

        String note =
                "User: %s Created ticket for pool: %s-%s"
                        .formatted(userName, pool.getName(), pool.getWeek());
        var jsonbNote = JsonbNote.builder().note(note).created(Instant.now()).user(userName).build();

        ticket = ticketUiService.createTicket(createTicket(pool, player), pool, player, jsonbNote);
    }
}
