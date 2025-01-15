package nm.poolio.views.ticket;

import static nm.poolio.utils.VaddinUtils.RESULTS_ICON;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupItem;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.RolesAllowed;
import java.time.Instant;
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
import org.vaadin.lineawesome.LineAwesomeIcon;

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
    var scoredTickets = ticketScorerService.findAndScoreTickets(ticket.getPool(), ticket.getWeek());

    ticket =
        scoredTickets.stream()
            .filter(t -> t.getPlayer().equals(player))
            .findFirst()
            .orElseThrow(() -> new PoolioException("Cannot find ticket"));

    var weeklyGames =
        nflGameService.getWeeklyGamesThursdayFiltered(
            ticket.getWeek(), ticket.getPool().isIncludeThursday());

    add(createHeaderBadgesTop(ticket.getPool(), ticket));
    add(createHeaderBadgesBottom(ticket));

    if (Instant.now().isAfter(weeklyGames.getFirst().getGameTime())) {
      var button =
          new Button(
              "View Results Grid for " + ticket.getWeek(),
              e -> UI.getCurrent().getPage().open("/result?week=" + ticket.getWeek(), "_self"));
      button.setPrefixComponent(RESULTS_ICON.create());
      button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      add(button);
    } else {
      var button = new Button("Games have no started yet " + ticket.getWeek());
      button.addThemeVariants(ButtonVariant.LUMO_ERROR);
      button.setPrefixComponent(LineAwesomeIcon.CLOCK_SOLID.create());
      button.setEnabled(false);
      add(button);
    }

    HorizontalLayout horizontalLayout = new HorizontalLayout();
    horizontalLayout.setWidthFull();
    horizontalLayout.add(new Span(scoredTickets.size() + " Players: "));
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

    var scores = gameScoreService.getScores(weeklyGames);

    decorateTicketGrid(ticket, scores);
    ticketGrid.setItems(weeklyGames);

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
