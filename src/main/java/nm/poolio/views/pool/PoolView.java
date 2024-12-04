package nm.poolio.views.pool;

import static nm.poolio.utils.VaddinUtils.LEAGUE_ICON;
import static nm.poolio.utils.VaddinUtils.PAY_AS_YOU_GO;
import static nm.poolio.utils.VaddinUtils.PLAYERS_ICON;
import static nm.poolio.utils.VaddinUtils.POOL_ICON;
import static nm.poolio.utils.VaddinUtils.STATUS_ICON;
import static nm.poolio.utils.VaddinUtils.WEEK_ICON;
import static nm.poolio.utils.VaddinUtils.decorateIncludeThursdayCheckbox;
import static nm.poolio.utils.VaddinUtils.decorateNameField;
import static nm.poolio.utils.VaddinUtils.decoratePoolAmountField;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.cache.CacheConfig;
import nm.poolio.cache.CacheConfig.CacheName;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.enitities.pool.PoolService;
import nm.poolio.enitities.ticket.Ticket;
import nm.poolio.enitities.ticket.TicketService;
import nm.poolio.model.NflGame;
import nm.poolio.model.enums.League;
import nm.poolio.model.enums.NflWeek;
import nm.poolio.model.enums.PoolStatus;
import nm.poolio.model.enums.Season;
import nm.poolio.services.NflGameService;
import nm.poolio.services.PoolWeekCloser;
import nm.poolio.services.TicketScorerService;
import nm.poolio.services.UserService;
import nm.poolio.vaadin.PoolioDialog;
import nm.poolio.vaadin.PoolioNotification;
import nm.poolio.vaadin.UserComboBox;
import nm.poolio.views.MainLayout;
import org.springframework.cache.Cache;
import org.springframework.util.CollectionUtils;

@PageTitle("Pools \uD83C\uDFC8")
@Route(value = "pool", layout = MainLayout.class)
@RolesAllowed("ADMIN")
@Slf4j
public class PoolView extends VerticalLayout implements PoolGrid, PoolioDialog, PoolioNotification {
  private final PoolService service;
  private final UserService userService;
  private final TicketService ticketService;
  private final NflGameService nflGameService;
  private final CacheConfig cacheConfig;
  private final TicketScorerService ticketScorerService;
  private final PoolWeekCloser poolWeekCloser;

  TextField name = new TextField("Name");
  ComboBox<League> league = new ComboBox<>("League");
  Checkbox includeThursday = new Checkbox();
  IntegerField amount = new IntegerField("Amount");
  ComboBox<User> payAsYouGoUser = new ComboBox<>();
  ComboBox<NflWeek> week = new ComboBox<>("Week");
  ComboBox<PoolStatus> status = new ComboBox<>("Status");
  Binder<Pool> binder = new Binder<>(Pool.class);

  Dialog poolDialog = new Dialog();
  Dialog adminDialog = new Dialog();
  Dialog playersDialog = new Dialog();
  IntegerField maxPlayersPerWeek = new IntegerField("Max Per Week");

  @Getter Grid<Pool> grid = createGrid(Pool.class);

  UserComboBox userComboBox = new UserComboBox();
  MultiSelectComboBox<User> adminComboBox;
  MultiSelectComboBox<User> playersComboBox;

  PoolStatus originalPoolStatus;

  public PoolView(
      PoolService poolService,
      UserService userService,
      TicketService ticketService,
      NflGameService nflGameService,
      CacheConfig cacheConfig,
      TicketScorerService ticketScorerService,
      PoolWeekCloser poolWeekCloser) {
    this.service = poolService;
    this.userService = userService;
    this.cacheConfig = cacheConfig;
    this.ticketScorerService = ticketScorerService;
    this.poolWeekCloser = poolWeekCloser;
    setHeight("100%");

    binder.bindInstanceFields(this);

    createDialog(adminDialog, e -> onSaveAdmins(binder.getBean()), createAdminComboBox());
    createDialog(playersDialog, e -> onSavePlayers(binder.getBean()), createPlayersComboBox());
    createDialog(poolDialog, e -> onSavePool(binder.getBean()), createPoolDialogLayout());

    Button newPoolButton =
        new Button("New Pool", POOL_ICON.create(), e -> openPoolDialog(new Pool()));
    add(newPoolButton);

    decorateGrid();

    // decoratePoolDialog();
    // decorateAdminDialog();
    //  decoratePlayersDialog();

    add(grid);
    this.ticketService = ticketService;
    this.nflGameService = nflGameService;
  }

  private MultiSelectComboBox<User> createAdminComboBox() {
    adminComboBox = new MultiSelectComboBox<>("Admins");

    adminComboBox.setItems(userService.findAdmins());
    // adminComboBox.select();
    adminComboBox.setItemLabelGenerator(User::getName);

    return adminComboBox;
  }

  private MultiSelectComboBox<User> createPlayersComboBox() {
    playersComboBox = new MultiSelectComboBox<>("Players");

    playersComboBox.setItems(userService.findPlayers());
    // playersComboBox.select();
    playersComboBox.setItemLabelGenerator(User::getName);

    return playersComboBox;
  }

  private void decorateGrid() {
    decoratePoolGrid();
    grid.addItemClickListener(item -> openPoolDialog(item.getItem()));
    grid.setItems(service.findAll());
  }

  private void onSaveAdmins(Pool pool) {
    pool.setAdmins(adminComboBox.getValue());
    adminDialog.close();
    onSaveAndRefresh(pool);
  }

  private void onSavePlayers(Pool pool) {
    pool.setPlayers(playersComboBox.getValue());
    playersDialog.close();

    onSaveAndRefresh(pool);
  }

  private void onSavePool(Pool pool) {
    pool.setPayAsYouGoUser(userComboBox.getSelected());
    pool.setSeason(Season.S_2024);

    if (binder.validate().isOk()) {

      if (pool.getStatus() == PoolStatus.PAID) {
        createInfoNotification(new Span("Checking if " + pool.getWeek().name() + " can be closed"));
        var winners = ticketService.findWinners(pool, pool.getWeek());

        if (CollectionUtils.isEmpty(winners)) {
          AtomicBoolean haveError = new AtomicBoolean(false);
          var weeklyGames = nflGameService.getGamesForPool(pool, pool.getWeek());
          weeklyGames.forEach(
              g -> {
                if (!isGameScored(g)) {
                  createErrorNotification(new Span("Game Not scored"));
                  haveError.set(true);
                }
              });

          if (!haveError.get()) {
            // clear for safety
            Cache scoredTicketCache = cacheConfig.getCache(CacheName.SCORED_TICKETS);
            scoredTicketCache.evict(List.of(pool.getId(), pool.getWeek().getWeekNum()));
            List<Ticket> scoredTickets =
                ticketScorerService.findAndScoreTickets(pool, pool.getWeek());
            createInfoNotification(new Span("Processing tickets"));
            poolWeekCloser.close(scoredTickets);
            poolDialog.close();
            onSaveAndRefresh(pool);
            createInfoNotification(new Span("Closed Week"));
          }

        } else {
          createErrorNotification(new Span("Cannot close wee we have already paid winners"));
        }

      } else {
        poolDialog.close();
        onSaveAndRefresh(pool);
      }

    } else {
      log.error("Form is bad");
    }
  }

  private boolean isGameScored(NflGame g) {
    return g.getHomeScore() != null || g.getAwayScore() != null;
  }

  private void onSaveAndRefresh(Pool pool) {
    service.update(pool);
    grid.setItems(service.findAll());
  }

  public void openPoolDialog(Pool pool) {
    poolDialog.getHeader().removeAll();

    String headerTitle = (pool.getId() == null) ? "New" : "Edit";

    originalPoolStatus = pool.getStatus();

    binder.setBean(pool);
    poolDialog.open();
  }

  @Override
  public void openAdminDialog(Pool pool) {
    adminComboBox.select(pool.getAdmins());
    binder.setBean(pool);
    adminDialog.open();
  }

  @Override
  public void openPlayersDialog(Pool pool) {
    playersComboBox.select(pool.getPlayers());
    binder.setBean(pool);
    playersDialog.open();
  }

  private Component[] createPoolDialogLayout() {
    userComboBox.decorate(payAsYouGoUser, userService.findUsersWithNoRoles(), "Pay as You Go");
    payAsYouGoUser.setPrefixComponent(PAY_AS_YOU_GO.create());

    league.setItems(League.values());
    league.setRequired(true);
    league.setPrefixComponent(LEAGUE_ICON.create());

    week.setItems(NflWeek.values());
    week.setRequired(true);
    week.setPrefixComponent(WEEK_ICON.create());

    status.setItems(PoolStatus.values());
    status.setRequired(true);
    status.setPrefixComponent(STATUS_ICON.create());

    maxPlayersPerWeek.setRequired(true);
    maxPlayersPerWeek.setPrefixComponent(PLAYERS_ICON.create());
    maxPlayersPerWeek.setMax(250);
    maxPlayersPerWeek.setMin(2);

    decorateNameField(name);
    decoratePoolAmountField(amount);
    decorateIncludeThursdayCheckbox(includeThursday);

    return new Component[] {name, league, week, status, amount, payAsYouGoUser, maxPlayersPerWeek};
  }
}
