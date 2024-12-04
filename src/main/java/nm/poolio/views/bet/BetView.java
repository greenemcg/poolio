package nm.poolio.views.bet;

import static nm.poolio.utils.VaddinUtils.AMOUNT_ICON;
import static nm.poolio.utils.VaddinUtils.BET_ICON;
import static nm.poolio.utils.VaddinUtils.GAMES_ICON;
import static nm.poolio.utils.VaddinUtils.POOLIO_ICON;
import static nm.poolio.utils.VaddinUtils.SPLIT_ICON;
import static nm.poolio.utils.VaddinUtils.SPREAD_ICON;
import static nm.poolio.utils.VaddinUtils.createIconSpan;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.data.User;
import nm.poolio.enitities.bet.GameBet;
import nm.poolio.enitities.bet.GameBetService;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.enitities.pool.PoolService;
import nm.poolio.enitities.pool.UserPoolFinder;
import nm.poolio.enitities.transaction.NoteCreator;
import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import nm.poolio.enitities.transaction.PoolioTransactionType;
import nm.poolio.model.NflGame;
import nm.poolio.model.enums.NflTeam;
import nm.poolio.model.enums.Season;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.services.NflGameService;
import nm.poolio.vaadin.PoolioAvatar;
import nm.poolio.vaadin.PoolioDialog;
import nm.poolio.views.MainLayout;
import org.springframework.util.CollectionUtils;

@PageTitle("Bets \uD83C\uDFB0")
@Route(value = "bet", layout = MainLayout.class)
@RolesAllowed("ADMIN")
@Slf4j
public class BetView extends VerticalLayout
    implements UserPoolFinder, PoolioDialog, NoteCreator, PoolioAvatar {

  private final GameBetService service;
  @Getter private final NflGameService nflGameService;
  @Getter private final PoolioTransactionService poolioTransactionService;
  @Getter private final AuthenticatedUser authenticatedUser;

  private final ComponentRenderer<Component, GameBet> personCardRenderer;

  Binder<GameBet> binder = new Binder<>(GameBet.class);
  Dialog betDialog = new Dialog();
  ComboBox<NflGame> game = new ComboBox<>();
  ComboBox<NflTeam> teamPicked = new ComboBox<>();
  IntegerField spread = new IntegerField("Home Team Spread");
  IntegerField amount = new IntegerField("Amount");
  Checkbox betCanBeSplit = new Checkbox();
  DateTimePicker dateTimePicker = new DateTimePicker();
  BetProposalRenderer betProposalRenderer;

  private User player = null;
  private Pool pool; // used to get current week

  public BetView(
      GameBetService service,
      AuthenticatedUser authenticatedUser,
      PoolService poolService,
      NflGameService nflGameService,
      PoolioTransactionService poolioTransactionService) {
    this.service = service;
    this.nflGameService = nflGameService;
    this.poolioTransactionService = poolioTransactionService;
    this.authenticatedUser = authenticatedUser;
    player = authenticatedUser.get().orElseThrow();
    betProposalRenderer =
        new BetProposalRenderer(player, this, nflGameService, poolioTransactionService);

    personCardRenderer = new ComponentRenderer<>(betProposalRenderer::render);
    setHeight("100%");

    var userPools = findPoolsForUser(player.getPoolIdNames(), poolService);

    var funds = poolioTransactionService.getFunds(player);

    if (funds < 1) {
      add(createNoFundsNotification());
    } else if (userPools.isEmpty()) {
      add(createNoPoolNotification());
    } else {
      binder.bindInstanceFields(this);
      pool = userPools.getFirst();
      var games = nflGameService.getWeeklyGamesNotStarted(pool.getWeek());
      var openBets = service.findOpenBets();
      log.info("Open Bets: {}", openBets.size());

      if (CollectionUtils.isEmpty(games)) {
        Span noGamesAvailableToBet = new Span("No Games available to Bet");
        noGamesAvailableToBet.getElement().getThemeList().add("badge contrast primary");
        add(noGamesAvailableToBet);
      } else {
        createDialog(betDialog, e -> onSaveBet(binder.getBean()), createGameBetDialogLayout(games));

        Button proposeNewBetButton =
            new Button(
                "Propose New Bet", BET_ICON.create(), e -> openBetProposalDialog(new GameBet()));
        add(proposeNewBetButton);
      }

      HorizontalLayout horizontalLayout = new HorizontalLayout();
      horizontalLayout.add(new H3("Open Game bet Proposals"));
      Span pending = new Span("Your Funds: $" + funds);
      pending.getElement().getThemeList().add("badge success");
      horizontalLayout.add(pending);

      add(horizontalLayout);
      VirtualList<GameBet> list = new VirtualList<>();
      list.getElement().getStyle().set("background-color", "rgba(0, 0, 0, 0.1)");
      // list.setMaxHeight("300px");
      list.setItems(openBets);
      list.setRenderer(personCardRenderer);
      add(list);
    }
  }

  private void onSaveBet(GameBet bean) {
    if (binder.validate().isOk()) {
      var funds = poolioTransactionService.getFunds(player);

      if (bean.getAmount() > funds) {
        createErrorNotification(
            new Span(
                "Your funds: %d is not enough to cover bet: %d"
                    .formatted(funds, bean.getAmount())));
      } else {
        NflGame nflGame = game.getValue();
        bean.setSeason(Season.S_2024);
        bean.setWeek(pool.getWeek());
        bean.setGameId(nflGame.getId());
        bean.setProposerCanEditTeam(false);
        bean.setExpiryDate(
            dateTimePicker.getValue().atZone(ZoneId.of("America/New_York")).toInstant());

        PoolioTransaction poolioTransaction = new PoolioTransaction();
        poolioTransaction.setDebitUser(player);
        poolioTransaction.setCreditUser(pool.getBankUser()); // add bet banker
        poolioTransaction.setAmount(bean.getAmount());
        poolioTransaction.setType(PoolioTransactionType.GAME_BET_PURCHASE);
        poolioTransaction.setNotes(
            List.of(
                buildNote(
                    "%s at %s - Amount: %d Spread: %d"
                        .formatted(
                            nflGame.getAwayTeam().name(),
                            nflGame.getHomeTeam().name(),
                            bean.getAmount(),
                            bean.getSpread()))));

        bean.setProposerTransaction(poolioTransaction);

        showConfirmDialog(bean);
      }
    }
  }

  void showConfirmDialog(GameBet bet) {
    HorizontalLayout layout = new HorizontalLayout();
    layout.setAlignItems(Alignment.CENTER);
    layout.setJustifyContentMode(JustifyContentMode.CENTER);

    ConfirmDialog dialog = new ConfirmDialog();
    dialog.setHeader("Bet Proposal: Are you sure");
    dialog.setText(bet.getHumanReadableString());

    dialog.setCancelable(true);
    // dialog.addCancelListener(event -> setStatus("Canceled"));

    dialog.setConfirmText("Propose Bet");
    dialog.setConfirmButtonTheme("primary");
    dialog.addConfirmListener(
        event -> {
          try {
            saveToDb(bet);
            betDialog.close();
          } catch (Exception e) {
            log.error("Cannot save to DB", e);
            createErrorNotification(new Span(e.getMessage()));
          }
        });

    add(layout);
    dialog.open();
  }

  private void saveToDb(GameBet bet) {
    var saved = service.save(bet);
    createSucessNotification(new Span("Successfully created new proposed Bet"));
    log.info("Saved Bet {}", saved.getGameId());
  }

  public void openBetProposalDialog(GameBet bet) {
    betDialog.getHeader().removeAll();

    // String headerTitle = (pool.getId() == null) ? "New" : "Edit";

    // originalPoolStatus = pool.getStatus();

    binder.setBean(bet);
    betDialog.open();
  }

  private Component[] createGameBetDialogLayout(List<NflGame> games) {
    game.setLabel("Game");
    game.setItems(games);
    game.getStyle().set("--vaadin-combo-box-overlay-width", "16em");
    game.setPrefixComponent(GAMES_ICON.create());
    game.setRequired(true);
    game.setClearButtonVisible(true);
    game.setItemLabelGenerator(NflGame::getGameString);
    game.setRenderer(
        new ComponentRenderer<>(
            nflGame ->
                new Span(nflGame.getHomeTeam().name() + " vs " + nflGame.getAwayTeam().name())));
    game.addValueChangeListener(
        e -> {
          NflGame game = e.getValue();
          var dateTime = game.getLocalDateTime();
          dateTimePicker.setMax(dateTime);
          dateTimePicker.setValue(dateTime);
          dateTimePicker.setEnabled(true);

          teamPicked.setItems(List.of(game.getHomeTeam(), game.getAwayTeam()));
          teamPicked.setEnabled(true);
        });

    teamPicked.setLabel("Team");
    teamPicked.getStyle().set("--vaadin-combo-box-overlay-width", "16em");
    teamPicked.setPrefixComponent(POOLIO_ICON.create());
    teamPicked.setRequired(true);
    teamPicked.setClearButtonVisible(true);
    teamPicked.setEnabled(false);

    spread.setRequired(true);
    spread.setPrefixComponent(SPREAD_ICON.create());
    spread.setMax(100);
    spread.setMin(-100);

    amount.setRequired(true);
    amount.setPrefixComponent(AMOUNT_ICON.create());
    amount.setMax(100);
    amount.setMin(0);

    dateTimePicker.setLabel("Expire Date if No Takers (EST)");
    dateTimePicker.setStep(Duration.ofMinutes(10));
    ZoneId zone = ZoneId.of("America/New_York");

    dateTimePicker.setMin(LocalDateTime.ofInstant(Instant.now(), zone));
    dateTimePicker.setEnabled(false);

    betCanBeSplit.setLabelComponent(createIconSpan(SPLIT_ICON, "Bet Can Be Split"));

    var h4 = new H4("Propose Bet");
    return new Component[] {h4, game, teamPicked, amount, spread, betCanBeSplit, dateTimePicker};
  }
}
