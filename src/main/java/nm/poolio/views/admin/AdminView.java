package nm.poolio.views.admin;

import static nm.poolio.utils.VaddinUtils.EDIT_ICON;
import static nm.poolio.utils.VaddinUtils.PLAYER_ICON;
import static nm.poolio.utils.VaddinUtils.WEEK_ICON;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import lombok.Getter;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.enitities.pool.PoolService;
import nm.poolio.enitities.ticket.Ticket;
import nm.poolio.enitities.ticket.TicketService;
import nm.poolio.model.JsonbNote;
import nm.poolio.model.NflGame;
import nm.poolio.model.enums.NflTeam;
import nm.poolio.model.enums.NflWeek;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.services.NflGameService;
import nm.poolio.services.TicketUiService;
import nm.poolio.services.TicketUiService.Status;
import nm.poolio.utils.VaddinUtils;
import nm.poolio.vaadin.PoolioDialog;
import nm.poolio.vaadin.PoolioNotification;
import nm.poolio.vaadin.UserComboBox;
import nm.poolio.views.MainLayout;
import nm.poolio.views.ticket.TicketEditUi;

@PageTitle("Admin \uD83D\uDC68\u200D\uD83D\uDCBB")
@Route(value = "admin", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class AdminView extends VerticalLayout
    implements TicketEditUi, PoolioNotification, PoolioDialog {

  @Getter private final PoolService poolService;
  private final TicketUiService ticketUiService;
  @Getter private final TicketService ticketService;
  private final NflGameService nflGameService;
  @Getter private final AuthenticatedUser authenticatedUser;

  ComboBox<Pool> poolComboBox = new ComboBox<>("Pool");
  ComboBox<User> playerComboBox = new ComboBox<>("Player");
  ComboBox<NflWeek> week = new ComboBox<>("Week");
  Button editTicketButton;

  Dialog ticketDialog = new Dialog();

  Pool pool;
  User player;
  Ticket ticket;

  public AdminView(
      PoolService poolService,
      TicketUiService ticketUiService,
      TicketService ticketService,
      NflGameService nflGameService,
      AuthenticatedUser authenticatedUser) {
    this.poolService = poolService;
    this.ticketUiService = ticketUiService;
    this.ticketService = ticketService;
    this.nflGameService = nflGameService;
    this.authenticatedUser = authenticatedUser;
    var layout = createTicketSelectBox();

    add(new H3("Admin Ticket Editor"));
    add(layout);
  }

  private Component createTicketDialogLayout() {
    var layout = new VerticalLayout();
    layout.setPadding(false);
    layout.setSpacing(false);

    var games = nflGameService.getWeeklyGamesForPool(pool);
    games.forEach(g -> layout.add(createGameRadioButton(g)));
    layout.add(createTieBreakerField(ticket));

    return layout;
  }

  private Component createGameRadioButton(NflGame g) {
    RadioButtonGroup<NflTeam> radioGroup = new RadioButtonGroup<>();
    radioGroup.setItems(g.getAwayTeam(), g.getHomeTeam());
    radioGroup.setRequired(true);

    radioGroup.addValueChangeListener(c -> setTicketGameValue(ticket, c.getValue(), g.getId()));

    var value = ticket.getSheet().getGamePicks().get(g.getId());

    if (value != null) radioGroup.setValue(value);

    return radioGroup;
  }

  private void onSaveTicket() {
    var savedTicket = ticketService.save(ticket);
    boolean ticketComplete = ticketService.isTicketComplete(savedTicket);

    if (ticketComplete) {
      add(createSucessNotification(new Span("Successfully saved Completed Ticket")));
      ticketDialog.close();

      ticketDialog.remove();
      ticketDialog = new Dialog();

    } else {
      add(createErrorNotification(new Span("Ticket (Saved) is not complete")));
      ticket = savedTicket;
    }
  }

  private HorizontalLayout createTicketSelectBox() {
    HorizontalLayout horizontalLayout = new HorizontalLayout();
    horizontalLayout.setPadding(true);
    horizontalLayout.setSpacing(true);

    horizontalLayout.getElement().getStyle().set("border", "1px solid blue");

    poolComboBox.setPrefixComponent(VaddinUtils.POOL_ICON.create());
    poolComboBox.setItemLabelGenerator(Pool::getName);
    poolComboBox.setItems(poolService.findAll());
    poolComboBox.addValueChangeListener(event -> onChangePool(event.getValue()));

    week.setItems(NflWeek.values());
    week.setRequired(true);
    week.setPrefixComponent(WEEK_ICON.create());
    week.setEnabled(false);

    UserComboBox userComboBox = new UserComboBox();
    userComboBox.decorate(playerComboBox, Collections.emptyList(), "Players");
    playerComboBox.addValueChangeListener(event -> editTicketButton.setEnabled(true));
    playerComboBox.setEnabled(false);
    playerComboBox.setPrefixComponent(PLAYER_ICON.create());

    editTicketButton = new Button("Edit/Create Ticket");
    editTicketButton.setPrefixComponent(EDIT_ICON.create());
    editTicketButton.addThemeVariants(ButtonVariant.LUMO_LARGE);
    editTicketButton.addClickListener(event -> openDialog());
    editTicketButton.setEnabled(false);

    horizontalLayout.add(poolComboBox, playerComboBox, week, editTicketButton);

    horizontalLayout.setVerticalComponentAlignment(
        Alignment.BASELINE, poolComboBox, playerComboBox, week, editTicketButton);
    return horizontalLayout;
  }

  private void openDialog() {
    ticketDialog.remove();
    ticketDialog = new Dialog();

    pool = poolComboBox.getValue();
    player = playerComboBox.getValue();

    var optional = ticketService.findTicketForUserWithWeek(player, pool, week.getValue());

    if (optional.isPresent()) ticket = optional.get();
    else {
      if (ticketUiService.checkFunds(pool, player) == Status.PLAYER_NOT_ENOUGH_FUNDS)
        createErrorNotification(
                new Div("Player " + player.getName() + " does not have enough funds"))
            .open();
      else ticket = buildNewTicket(week.getValue());
    }

    createDialog(ticketDialog, e -> onSaveTicket(), createTicketDialogLayout());
    ticketDialog.open();
  }

  private Ticket buildNewTicket(NflWeek week) {
    String userName = "unknown";
    if (authenticatedUser.get().isPresent()) {
      userName = authenticatedUser.get().get().getName();
    }

    String note =
        "Admin: %s created ticket for user: %s pool: %s-%s"
            .formatted(userName, player.getName(), pool.getName(), pool.getWeek());
    var jsonbNote = JsonbNote.builder().note(note).created(Instant.now()).user(userName).build();

    return ticketUiService.createTicket(createTicket(pool, player, week), pool, player, jsonbNote);
  }

  private void onChangePool(Pool p) {
    playerComboBox.setItems(
        p.getPlayers().stream().sorted(Comparator.comparing(User::getName)).toList());
    playerComboBox.setEnabled(true);

    week.setEnabled(true);
    week.setValue(p.getWeek());
  }

  @Override
  public void setErrorFound(boolean errorFound) {}

  @Override
  public HasComponents getDialogHasComponents() {
    return this;
  }
}
