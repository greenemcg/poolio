package nm.poolio.views.bet;

import static nm.poolio.utils.VaddinUtils.*;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
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
import nm.poolio.model.enums.BetStatus;
import nm.poolio.model.enums.NflTeam;
import nm.poolio.model.enums.Season;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.services.bets.NflBetService;
import nm.poolio.services.NflGameService;
import nm.poolio.services.UserService;
import nm.poolio.vaadin.PoolioAvatar;
import nm.poolio.vaadin.PoolioDialog;
import nm.poolio.vaadin.PoolioNotification;
import nm.poolio.views.MainLayout;
import org.springframework.util.CollectionUtils;

@PageTitle("Bets \uD83C\uDFB0")
@Route(value = "bet", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "USER"})
@Slf4j
public class BetView extends VerticalLayout
    implements UserPoolFinder, PoolioDialog, NoteCreator, PoolioAvatar, PoolioNotification {

  private final GameBetService service;
  @Getter private final NflGameService nflGameService;
  @Getter private final PoolioTransactionService poolioTransactionService;
  @Getter private final AuthenticatedUser authenticatedUser;
  private final GameBetService gameBetService;
  private final UserService userService;
  private final NflBetService nflBetService;
  private final PoolService poolService;
  private final User player;
  private final AmountDialog amountDialog; // used to get current week
  Binder<GameBet> binder = new Binder<>(GameBet.class);
  Dialog betDialog = new Dialog();
  ComboBox<NflGame> game = new ComboBox<>();
  ComboBox<NflTeam> teamPicked = new ComboBox<>();
  NumberField spreadDouble = new NumberField("Home Team Spread");
  IntegerField amount = new IntegerField("Amount");
  Checkbox betCanBeSplit = new Checkbox();
  DateTimePicker expiryDate = new DateTimePicker();
  BetProposalRenderer betProposalRenderer;
  VirtualList<GameBet> gameBetVirtualList = new VirtualList<>();
  //  @Getter Grid<GameBet> proposalBetGrid = new Grid<>();
  //  @Getter Grid<GameBet> playerBetGrid = new Grid<>();
  private Pool pool;

  public BetView(
      GameBetService service,
      AuthenticatedUser authenticatedUser,
      PoolService poolService,
      NflGameService nflGameService,
      PoolioTransactionService poolioTransactionService,
      UserService userService,
      NflBetService nflBetService,
      GameBetService gameBetService) {
    this.service = service;
    this.nflGameService = nflGameService;
    this.poolioTransactionService = poolioTransactionService;
    this.authenticatedUser = authenticatedUser;
    player = authenticatedUser.get().orElseThrow();
    this.nflBetService = nflBetService;
    this.userService = userService;
    this.poolService = poolService;
    // decorateTransactionGrid();

    setHeight("100%");

    TabSheet tabSheet = new TabSheet();
    tabSheet.add("Open Bets \uD83D\uDCD6", new LazyComponent(this::createOpenBetsTab));
    tabSheet.add("Your Proposals \uD83D\uDCB0", new LazyComponent(this::createProposalBetGrid));
    tabSheet.add("Your Game Bets \uD83C\uDFC8", new LazyComponent(this::createPlayerBetGrid));
    add(tabSheet);
    this.gameBetService = gameBetService;

    amountDialog = new AmountDialog();
  }

  private VerticalLayout createPlayerBetGrid() {
    var p = new Bets();
    return p.render();
  }

  private VerticalLayout createProposalBetGrid() {
    var p = new Proposals();
    return p.render();
  }

  private VerticalLayout createOpenBetsTab() {
    VerticalLayout layout = new VerticalLayout();
    layout.setWidth("100%");

    betProposalRenderer =
        new BetProposalRenderer(
            player, this, nflGameService, poolioTransactionService, nflBetService, amountDialog);

    ComponentRenderer<Component, GameBet> personCardRenderer =
        new ComponentRenderer<>(betProposalRenderer::render);

    var userPools = findPoolsForUser(player.getPoolIdNames(), poolService);

    var funds = poolioTransactionService.getFunds(player);

    if (funds < 1) {
      layout.add(createNoFundsNotification());
    } else if (userPools.isEmpty()) {
      layout.add(createNoPoolNotification());
    } else {
      binder.bindInstanceFields(this);
      // binder.setBean(new GameBet());
      binder
          .forField(game)
          .withValidator(game -> game != null && game.getId() != null, "Game is required")
          .bind(GameBet::getGame, GameBet::setGame);

      binder
          .forField(teamPicked)
          .withValidator(Objects::nonNull, "Team is required")
          .bind(GameBet::getTeamPicked, GameBet::setTeamPicked);

      binder
          .forField(amount)
          .withValidator(this::validateAmount, "Amount is invalid")
          .bind(GameBet::getAmount, GameBet::setAmount);

      binder
          .forField(spreadDouble)
          .withValidator(this::validateSpread, "Spread is invalid")
          .bind(GameBet::getSpreadDouble, GameBet::setSpreadDouble);

      binder
          .forField(expiryDate)
          .withValidator(this::validateExpiryLocalDateTime, "Expire Date is invalid")
          .bind(GameBet::getExpiryLocalDateTime, GameBet::setExpiryLocalDateTime);

      pool = userPools.getFirst();
      var games = nflGameService.getWeeklyGamesNotStarted(pool.getWeek());
      var openBets = service.findAvailableBets();
      log.info("Open Bets: {}", openBets.size());

      if (CollectionUtils.isEmpty(games)) {
        Span noGamesAvailableToBet = new Span("No Games available to Bet");
        noGamesAvailableToBet.getElement().getThemeList().add("badge contrast primary");
        layout.add(noGamesAvailableToBet);
      } else {
        createDialog(betDialog, e -> onSaveBet(binder.getBean()), createGameBetDialogLayout(games));

        Button proposeNewBetButton =
            new Button(
                "Propose New Bet", BET_ICON.create(), e -> openBetProposalDialog(new GameBet()));
        layout.add(proposeNewBetButton);
      }

      HorizontalLayout horizontalLayout = new HorizontalLayout();
      horizontalLayout.setWidth("100%");
      horizontalLayout.add(new H3("Open Game bet Proposals"));
      Span pending = new Span("Your Funds: $" + funds);
      pending.getElement().getThemeList().add("badge success");
      horizontalLayout.add(pending);

      layout.add(horizontalLayout);

      gameBetVirtualList.getElement().getStyle().set("background-color", "rgba(0, 0, 0, 0.1)");
      gameBetVirtualList.setWidth(600, Unit.PIXELS);
      gameBetVirtualList.setHeight(900, Unit.PIXELS);

      gameBetVirtualList.setItems(openBets);
      gameBetVirtualList.setRenderer(personCardRenderer);
      layout.add(gameBetVirtualList);
    }

    return layout;
  }

  private boolean validateExpiryLocalDateTime(LocalDateTime expiryLocalDateTime) {
    if (expiryLocalDateTime == null) {
      return false;
    }

    var bean = binder.getBean();

    if (bean != null && bean.getGame() != null) {
      if (expiryLocalDateTime.isAfter(bean.getGame().getLocalDateTime())) {
        createWarningNotification(new Span("Expire Date must be before Game Start"));
        return false;
      }
    }

    if (expiryLocalDateTime.isBefore(LocalDateTime.now())) {
      createWarningNotification(new Span("Expire Date must be in the future"));
      return false;
    }

    return true;
  }

  private boolean validateSpread(Double doubleValue) {
    if (doubleValue == null) {
      return false;
    }

    int intValue = doubleValue.intValue();

    if (intValue > GameBet.MAX_SPREAD) {
      createWarningNotification(new Span("Max Bet Spread is: %d".formatted(GameBet.MAX_SPREAD)));
      return false;
    }

    if (intValue < GameBet.MIN_SPREAD) {
      createWarningNotification(new Span("Min Bet Spread is: %d".formatted(GameBet.MIN_SPREAD)));
      return false;
    }

    BigDecimal bigDecimal = new BigDecimal(doubleValue);
    bigDecimal = bigDecimal.setScale(1, RoundingMode.UNNECESSARY);

    BigDecimal fractionalPart = bigDecimal.remainder(BigDecimal.ONE);
    log.info("Fractional Part: {}", fractionalPart);

    if (!fractionalPart.equals(BigDecimal.ZERO) && !fractionalPart.toString().endsWith("0.5")) {
      createWarningNotification(new Span("Hook must be .5"));
      return false;
    }

    binder.getBean().setSpread(bigDecimal);

    return true;
  }

  private boolean validateAmount(Integer amount) {
    var funds = poolioTransactionService.getFunds(player);

    if (amount == null || amount < 1) {
      return false;
    }

    if (amount > GameBet.MAX_AMOUNT) {
      createWarningNotification(new Span("Max Bet Amount is: %d".formatted(GameBet.MAX_AMOUNT)));
      return false;
    }

    if (amount < GameBet.MIN_AMOUNT) {
      createWarningNotification(new Span("Min Bet Amount is: %d".formatted(GameBet.MIN_AMOUNT)));
      return false;
    }

    if (amount > funds) {
      createWarningNotification(
          new Span("Your funds: %d is not enough to cover bet: %d".formatted(funds, amount)));
      return false;
    }

    return true;
  }

  private void onSaveBet(GameBet gameBet) {
    if (binder.validate().isOk()) {
      var funds = poolioTransactionService.getFunds(player);

      if (gameBet.getAmount() > funds) {
        createErrorNotification(
            new Span(
                "Your funds: %d is not enough to cover bet: %d"
                    .formatted(funds, gameBet.getAmount())));
      } else {
        NflGame nflGame = game.getValue();
        gameBet.setSeason(Season.S_2024);
        gameBet.setWeek(pool.getWeek());
        gameBet.setGameId(nflGame.getId());
        gameBet.setProposerCanEditTeam(false);
        gameBet.setExpiryDate(
            expiryDate.getValue().atZone(ZoneId.of("America/New_York")).toInstant());

        var banker = userService.findByUserName(nflBetService.getBetBanker());

        PoolioTransaction poolioTransaction = new PoolioTransaction();
        poolioTransaction.setCreditUser(player);
        poolioTransaction.setDebitUser(banker); // add bet banker
        poolioTransaction.setAmount(gameBet.getAmount());
        poolioTransaction.setType(PoolioTransactionType.GAME_BET_PROPOSAL);
        poolioTransaction.setNotes(
            List.of(
                buildNote(
                    "%s at %s - Amount: $%d Spread: %s Pick: %s"
                        .formatted(
                            nflGame.getAwayTeam().name(),
                            nflGame.getHomeTeam().name(),
                            gameBet.getAmount(),
                            gameBet.getSpread().toString(),
                            gameBet.getTeamPicked()))));

        gameBet.setProposerTransaction(poolioTransaction);

        showConfirmDialog(gameBet);
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
            gameBetVirtualList.setItems(service.findAvailableBets());
          } catch (Exception e) {
            log.error("Cannot save to DB", e);
            createErrorNotification(new Span(e.getMessage()));
          }
        });

    add(layout);
    dialog.open();
  }

  private void saveToDb(GameBet bet) {
    bet.setStatus(BetStatus.OPEN);
    var saved = service.save(bet);
    createSucessNotification(new Span("Successfully created new proposed Bet"));
    log.info("Saved Bet {}", saved.getGameId());
  }

  public void openBetProposalDialog(GameBet bet) {
    betDialog.getHeader().removeAll();
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
                new Span(nflGame.getAwayTeam().name() + " at " + nflGame.getHomeTeam().name())));
    game.addValueChangeListener(
        e -> {
          NflGame game = e.getValue();

          if (game == null) {
            return;
          }

          var dateTime = game.getLocalDateTime();
          expiryDate.setValue(dateTime);
          expiryDate.setEnabled(true);

          teamPicked.setItems(List.of(game.getHomeTeam(), game.getAwayTeam()));
          teamPicked.setEnabled(true);

          binder.getBean().setGame(game);
        });

    teamPicked.setLabel("Team");
    teamPicked.getStyle().set("--vaadin-combo-box-overlay-width", "16em");
    teamPicked.setPrefixComponent(POOLIO_ICON.create());
    teamPicked.setRequired(true);
    teamPicked.setClearButtonVisible(true);
    teamPicked.setEnabled(false);

    spreadDouble.setRequired(true);
    spreadDouble.setWidth("80%");
    spreadDouble.setPrefixComponent(SPREAD_ICON.create());

    amount.setPrefixComponent(AMOUNT_ICON.create());

    expiryDate.setLabel("Expire Date " + "if No Takers (EST)");
    expiryDate.setStep(Duration.ofMinutes(10));
    ZoneId zone = ZoneId.of("America/New_York");

    expiryDate.setMin(LocalDateTime.ofInstant(Instant.now(), zone));
    expiryDate.setEnabled(false);

    betCanBeSplit.setLabelComponent(createIconSpan(SPLIT_ICON, "Bet Can Be Split"));

    var h4 = new H4("Propose Bet");
    return new Component[] {h4, game, teamPicked, amount, spreadDouble, betCanBeSplit, expiryDate};
  }

  //  @Override
  //  public Grid<GameBet> getGrid() {
  //    return proposalBetGrid;
  //  }

  public static class LazyComponent extends Div {
    public LazyComponent(SerializableSupplier<? extends Component> supplier) {
      addAttachListener(
          e -> {
            if (getElement().getChildCount() == 0) {
              add(supplier.get());
            }
          });
    }
  }

  class AmountDialog extends Dialog {
    public AmountDialog() {
      super();
    }

    public void close() {
      super.close();
      gameBetVirtualList.setItems(service.findAvailableBets());
    }
  }

  @Getter
  class Proposals implements NflBetProposalGrid {
    Grid<GameBet> proposalBetGrid = new Grid<>();

    @Override
    public NflGameService getNflGameService() {
      return nflGameService;
    }

    VerticalLayout render() {
      VerticalLayout layout = new VerticalLayout();
      layout.setWidth(1600, Unit.PIXELS);
      layout.add(proposalBetGrid);
      decorateGrid();
      var list = gameBetService.findBetProposals(player);
      proposalBetGrid.setItems(list);
      proposalBetGrid.setWidth("100%");

      return layout;
    }
  }

  @Getter
  class Bets implements NflBetPlayersGrid {
    Grid<GameBet> playerBetGrid = new Grid<>();

    @Override
    public NflGameService getNflGameService() {
      return nflGameService;
    }

    VerticalLayout render() {
      VerticalLayout layout = new VerticalLayout();
      layout.setWidth(1600, Unit.PIXELS);
      layout.add(playerBetGrid);
      decorateGrid();

      var list = gameBetService.findBetsForPlayer(player);
      playerBetGrid.setItems(list);
      playerBetGrid.setWidth("100%");

      return layout;
    }
  }
}
