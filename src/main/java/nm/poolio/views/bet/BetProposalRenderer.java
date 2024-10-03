package nm.poolio.views.bet;

import static nm.poolio.utils.VaddinUtils.SPLIT_ICON;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.data.User;
import nm.poolio.enitities.bet.GameBet;
import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import nm.poolio.model.NflGame;
import nm.poolio.model.enums.NflTeam;
import nm.poolio.services.NflBetService;
import nm.poolio.services.NflGameService;
import nm.poolio.vaadin.PoolioAvatar;
import nm.poolio.vaadin.PoolioDialog;
import nm.poolio.vaadin.PoolioNotification;
import org.springframework.util.CollectionUtils;
import org.vaadin.lineawesome.LineAwesomeIcon;

@RequiredArgsConstructor
@Slf4j
public class BetProposalRenderer
    implements BetUtils, PoolioAvatar, PoolioDialog, PoolioNotification {
  private final User player;
  private final VerticalLayout rootLayout;
  private final NflGameService nflGameService;
  private final PoolioTransactionService poolioTransactionService;
  private final NflBetService nflBetService;

  private Dialog amountDialog;
  private Integer partialBet;

  private Component[] createAmountDialog(GameBet gameBet) {
    SplitAmounts amounts = computeSplitBetAmounts(gameBet);

    IntegerField integerField = new IntegerField("Amount");
    integerField.setHelperText("Bet 1 to " + amounts.availableToBetAmount());
    integerField.setMin(1);
    integerField.setMax(amounts.availableToBetAmount());
    integerField.addBlurListener(e -> partialBet = integerField.getValue());

    return new Component[] {new H4("Choose Amount To Bet"), integerField};
  }

  void openChooseAmountDialog(GameBet gameBet, NflGame nflGame) {
    if (gameBet.getProposer().equals(player))
      createErrorNotification(new Span("Cannot place bet on your own proposal"));
    else {
      amountDialog = new Dialog();
      createDialog(amountDialog, e -> onSetAmount(gameBet, nflGame), createAmountDialog(gameBet));
      amountDialog.open();
    }
  }

  private void onSetAmount(GameBet gameBet, NflGame nflGame) {
    if (partialBet != null) {
      var amounts = computeSplitBetAmounts(gameBet);
      var funds = poolioTransactionService.getFunds(player);

      if (partialBet > funds) {
        createErrorNotification(
            new Span(
                "You do not have enough funds: $%d to place bet amount: $%d"
                    .formatted(funds, partialBet)));
      } else if (partialBet > amounts.availableToBetAmount()) {
        createErrorNotification(
            new Span(
                "Your bet amount: $%d is greater than ticket bet amount available: $%d"
                    .formatted(partialBet, amounts.availableToBetAmount())));
      } else {
        openAcceptBetConfirmDialog(gameBet, nflGame, partialBet);
      }
    } else {
      createErrorNotification(new Span("Did not select amount to bet"));
    }
  }



  NflTeam getNflTeamNotPicked(GameBet gameBet, NflGame nflGame) {
    return (nflGame.getAwayTeam() == gameBet.getTeamPicked())
        ? nflGame.getHomeTeam()
        : nflGame.getAwayTeam();
  }

  String createSpreadString(GameBet gameBet, NflTeam otherTeamNotPicked, NflGame nflGame) {
    return (otherTeamNotPicked == nflGame.getAwayTeam())
        ? getSpreadString(gameBet.getSpread().multiply(new BigDecimal(-1)))
        : getSpreadString(gameBet.getSpread());
  }


  private boolean playerAlreadyMadeBet(GameBet gameBet) {
    return gameBet.getAcceptorTransactions().stream()
        .anyMatch(t -> t.getCreditUser().equals(player));
  }

  private Button createAcceptButton(
      GameBet gameBet, NflTeam otherTeamNotPicked, String spreadString, NflGame nflGame) {
    if (playerAlreadyMadeBet(gameBet)) {
      Button b = new Button("You have already accepted this bet");
      b.setEnabled(false);
      return b;
    } else if (gameBet.getProposer().equals(player)) {
      Button b = new Button("Your Proposal");
      b.setEnabled(false);
      return b;
    } else {
      var funds = poolioTransactionService.getFunds(player);
      if (Boolean.TRUE.equals(gameBet.getBetCanBeSplit())) {
        var amounts = computeSplitBetAmounts(gameBet);
        var amountUserCanBet = Math.min(funds, amounts.availableToBetAmount());
        Button b =
            new Button(
                "Accept this Bet for $%d to $%d taking %s (%s)"
                    .formatted(1, amountUserCanBet, otherTeamNotPicked, spreadString),
                LineAwesomeIcon.VOTE_YEA_SOLID.create(),
                e -> openChooseAmountDialog(gameBet, nflGame));
        b.setThemeName("primary");
        return b;
      } else {
        partialBet = null;
        Button b;
        if (gameBet.getAmount() > funds) {
          b = new Button("You do not have enough funds", LineAwesomeIcon.SAD_TEAR_SOLID.create());
          b.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        } else {
          b =
              new Button(
                  "Accept this Bet for $%d taking %s (%s)"
                      .formatted(gameBet.getAmount(), otherTeamNotPicked, spreadString),
                  LineAwesomeIcon.VOTE_YEA_SOLID.create(),
                  e -> openAcceptBetConfirmDialog(gameBet, nflGame, gameBet.getAmount()));
          b.setThemeName("primary");
        }
        return b;
      }
    }
  }

  public HorizontalLayout render(GameBet gameBet) {
    HorizontalLayout cardLayout = new HorizontalLayout();
    cardLayout.setMargin(true);

    var nflGame = nflGameService.findGameById(gameBet.getGameId());
    Avatar userAvatar = createUserAvatar(gameBet.getProposer(), AvatarVariant.LUMO_SMALL);

    VerticalLayout verticalLayout = new VerticalLayout();
    verticalLayout.setSpacing(false);
    verticalLayout.setPadding(false);

    verticalLayout.add(new HorizontalLayout(createProposerDiv(gameBet)));
    verticalLayout.add(
        new HorizontalLayout(
            createGamePickedDiv(gameBet, nflGame),
            createNflTeamAvatar(gameBet.getTeamPicked(), AvatarVariant.LUMO_SMALL)));
    verticalLayout.add(new HorizontalLayout(createAmountGameTimeDiv(gameBet, nflGame)));

    if (gameBet.getBetCanBeSplit()) {
      verticalLayout.add(new HorizontalLayout(SPLIT_ICON.create(), createSplitDiv(gameBet)));
    }

    verticalLayout.add(buildAcceptButton(gameBet, nflGame));
    cardLayout.add(userAvatar, verticalLayout);
    return cardLayout;
  }

  private Button buildAcceptButton(GameBet gameBet, NflGame nflGame) {
    NflTeam otherTeamNotPicked = getNflTeamNotPicked(gameBet, nflGame);
    String spreadString = createSpreadString(gameBet, otherTeamNotPicked, nflGame);
    Button acceptBetButton = createAcceptButton(gameBet, otherTeamNotPicked, spreadString, nflGame);
    if (acceptBetButton.isEnabled() && !acceptBetButton.getThemeNames().contains("error")) {
      acceptBetButton.setSuffixComponent(
          createNflTeamAvatar(otherTeamNotPicked, AvatarVariant.LUMO_SMALL));
    }
    return acceptBetButton;
  }

  private Div createAmountGameTimeDiv(GameBet gameBet, NflGame nflGame) {
    Div amountGameTimeDiv = new Div();
    var amountLabel = gameBet.getBetCanBeSplit() ? "Bet Total" : "Bet Amount";
    createNameValueElements(amountLabel, "$" + gameBet.getAmount(), amountGameTimeDiv.getElement());
    createSpacerElement(amountGameTimeDiv.getElement());
    createNameValueElements(
        "Game Time",
        DateTimeFormatter.ofPattern("E, MMM d, h:mm a").format(nflGame.getLocalDateTime()) + " EST",
        amountGameTimeDiv.getElement());
    return amountGameTimeDiv;
  }

  private Div createProposerDiv(GameBet gameBet) {
    Div div = new Div();
    createNameValueElements("Proposer", gameBet.getProposer().getName(), div.getElement());
    createSpacerElement(div.getElement());
    var localDateTime =
        LocalDateTime.ofInstant(gameBet.getExpiryDate(), ZoneId.of("America/New_York"));
    createNameValueElements(
        "Proposal Expiration",
        DateTimeFormatter.ofPattern("E, MMM d, h:mm a").format(localDateTime),
        div.getElement());
    return div;
  }

  private Div createGamePickedDiv(GameBet gameBet, NflGame nflGame) {
    Div div = new Div();
    createNameValueElements("Game", createGameWithSpreadString(gameBet, nflGame), div.getElement());
    createSpacerElement(div.getElement());
    createNameValueElements(
        gameBet.getProposer().getName() + " Picked",
        gameBet.getTeamPicked().name(),
        div.getElement());
    return div;
  }

  private Div createSplitDiv(GameBet gameBet) {
    Div mainDiv = new Div();
    var sumTotalBets =
        gameBet.getAcceptorTransactions().stream().mapToInt(PoolioTransaction::getAmount).sum();
    createNameValueElements(
        "Amount Available", "$" + (gameBet.getAmount() - sumTotalBets), mainDiv.getElement());

    Div div = new Div();
    if (CollectionUtils.isEmpty(gameBet.getAcceptorTransactions())) {
      createNameValueElements("No Bets Placed", "", div.getElement());
    } else {
      createNameValueElements("Players: ", "", div.getElement());
      gameBet
          .getAcceptorTransactions()
          .forEach(
              t -> {
                createNameValueElements("", t.getCreditUser().getName(), div.getElement());
                createNameValueElements("", "$" + t.getAmount(), div.getElement());
                createSpacerElement(div.getElement());
              });
    }
    mainDiv.add(div);
    return mainDiv;
  }

  private String createGameWithSpreadString(GameBet gameBet, NflGame nflGame) {
    return nflGame.getAwayTeam()
        + " at "
        + nflGame.getHomeTeam()
        + " ("
        + getSpreadString(gameBet.getSpread())
        + ")";
  }

  private void openAcceptBetConfirmDialog(
      GameBet gameBet, NflGame nflGame, @NotNull Integer betAmount) {
    if (gameBet.getProposer().equals(player)) {
      createErrorNotification(new Span("Cannot place bet on your own proposal"));
    } else {
      ConfirmDialog dialog = new ConfirmDialog();
      dialog.setHeader("Accept Bet : Are you sure");
      NflTeam otherTeamNotPicked = getNflTeamNotPicked(gameBet, nflGame);
      String spreadString = createSpreadString(gameBet, otherTeamNotPicked, nflGame);
      dialog.setText(
          "Accept this Bet for $%d taking %s (%s)"
              .formatted(betAmount, otherTeamNotPicked, spreadString));
      dialog.setCancelable(true);
      dialog.setConfirmText("Accept Bet from " + gameBet.getProposer().getName());
      dialog.setConfirmButtonTheme("primary");
      dialog.addConfirmListener(
          event -> {
            dialog.close();
            saveToDb(gameBet, betAmount);
          });
      rootLayout.add(new HorizontalLayout());
      dialog.open();
    }
  }

  private void saveToDb(GameBet gameBet, @NotNull Integer betAmount) {
    var transaction = nflBetService.createAcceptProposalTransaction(player, gameBet, betAmount);

    if (amountDialog != null) amountDialog.close();

    createSucessNotification(new Span("Bet Accepted"));
    log.info("Accepted bet: {}", transaction);
  }

  void createNameValueElements(
      @NotNull String name, @NotNull String value, @NotNull Element element) {
    String delimiter = ": ";

    if (name.isEmpty() || value.isEmpty()) {
      delimiter = "";
    }

    element.appendChild(ElementFactory.createStrong(name + delimiter));
    element.appendChild(ElementFactory.createLabel(value + " "));
  }

  void createSpacerElement(Element element) {
    element.appendChild(ElementFactory.createEmphasis(" • "));
  }


}
