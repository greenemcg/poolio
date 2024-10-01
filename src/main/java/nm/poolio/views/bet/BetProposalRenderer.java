package nm.poolio.views.bet;

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
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import jakarta.validation.constraints.NotNull;
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
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static nm.poolio.utils.VaddinUtils.SPLIT_ICON;

@RequiredArgsConstructor
@Slf4j
public class BetProposalRenderer implements PoolioAvatar, PoolioDialog, PoolioNotification {
    private final User player;
    private final VerticalLayout rootLayout;
    private final NflGameService nflGameService;
    private final PoolioTransactionService poolioTransactionService;
    private final NflBetService nflBetService;

    Dialog amountDialog;
    Integer partialBet;

    private Component[] createAmountDialog(GameBet gameBet) {
        SplitAmounts amounts = computeSplitBetAmounts(gameBet);

        H4 h4 = new H4("Chose Amount To Bet");

        IntegerField integerField = new IntegerField();
        integerField.setLabel("Amount");
        integerField.setHelperText("Bet 1 to " + amounts.availableToBetAmount);
        integerField.setMin(1);
        integerField.setMax(amounts.availableToBetAmount);

        integerField.addBlurListener(e -> partialBet = integerField.getValue());

        return new Component[]{h4, integerField};
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

            if (partialBet > funds)
                createErrorNotification(
                        new Span(
                                "You do not have enough funds: $%d to place bet amount: $%d"
                                        .formatted(funds, partialBet)));
            else if (partialBet > amounts.availableToBetAmount)
                createErrorNotification(
                        new Span(
                                "Your bet amount: $%d is greater than ticket bet amount available: $%d"
                                        .formatted(partialBet, amounts.availableToBetAmount)));
            else {
                openAcceptBetConfirmDialog(gameBet, nflGame, partialBet);
            }
        } else {
            createErrorNotification(new Span("Did not select amount to bet"));
        }
    }

    SplitAmounts computeSplitBetAmounts(GameBet gameBet) {
        int totalBetsSum =
                gameBet.getAcceptorTransactions().stream().mapToInt(PoolioTransaction::getAmount).sum();

        return new SplitAmounts(totalBetsSum, gameBet.getAmount() - totalBetsSum);
    }

    NflTeam getNflTeamNotPicked(GameBet gameBet, NflGame nflGame) {
        return (nflGame.getAwayTeam() == gameBet.getTeamPicked())
                ? nflGame.getHomeTeam()
                : nflGame.getAwayTeam();
    }

    String createSpreadString(GameBet gameBet, NflTeam otherTeamNotPicked, NflGame nflGame) {
        return (otherTeamNotPicked == nflGame.getAwayTeam())
                ? getSpreadString(-1 * gameBet.getSpread())
                : getSpreadString(gameBet.getSpread());
    }

    String getSpreadString(Integer spread) {
        if (spread.equals(0)) return "PICK-EM";

        if (spread > 0) return "+" + spread;

        return spread.toString();
    }

    private Button createAcceptButton(
            GameBet gameBet, NflTeam otherTeamNotPicked, String spreadString, NflGame nflGame) {

        if (gameBet.getProposer().equals(player)) {
            var b = new Button("Your Proposal");
            b.setEnabled(false);
            return b;
        } else {

            var funds = poolioTransactionService.getFunds(player);

            if (Boolean.TRUE.equals(gameBet.getBetCanBeSplit())) {
                var amounts = computeSplitBetAmounts(gameBet);

                var amountUserCanBet = amounts.availableToBetAmount;

                if (funds < amountUserCanBet) {
                    amountUserCanBet = funds;
                }

                var b =
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
                    b = new Button("You do not have enough funds");
                    b.setPrefixComponent(LineAwesomeIcon.SAD_TEAR_SOLID.create());
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

        HorizontalLayout proposerLayout = new HorizontalLayout();
        proposerLayout.add(createProposerDiv(gameBet));
        verticalLayout.add(proposerLayout);

        HorizontalLayout gameLayout = new HorizontalLayout();
        gameLayout.add(createGamePickedDiv(gameBet, nflGame));
        gameLayout.add(createNflTeamAvatar(gameBet.getTeamPicked(), AvatarVariant.LUMO_SMALL));
        verticalLayout.add(gameLayout);

        HorizontalLayout amountGameTimeLayout = new HorizontalLayout();
        amountGameTimeLayout.add(createAmountGameTimeDiv(gameBet, nflGame));
        verticalLayout.add(amountGameTimeLayout);

        if (gameBet.getBetCanBeSplit()) {
            HorizontalLayout splitAmountLayout = new HorizontalLayout();
            splitAmountLayout.add(SPLIT_ICON.create());
            splitAmountLayout.add(createSplitDiv(gameBet));
            verticalLayout.add(splitAmountLayout);
        }

        Button acceptBetButton = buildAcceptButton(gameBet, nflGame);
        verticalLayout.add(acceptBetButton);

        cardLayout.add(userAvatar, verticalLayout);
        return cardLayout;
    }

    private Button buildAcceptButton(GameBet gameBet, NflGame nflGame) {
        NflTeam otherTeamNotPicked = getNflTeamNotPicked(gameBet, nflGame);

        String spreadString = createSpreadString(gameBet, otherTeamNotPicked, nflGame);

        Button acceptBetButton = createAcceptButton(gameBet, otherTeamNotPicked, spreadString, nflGame);

        if (acceptBetButton.isEnabled() && !acceptBetButton.getThemeNames().contains("error"))
            acceptBetButton.setSuffixComponent(
                    createNflTeamAvatar(otherTeamNotPicked, AvatarVariant.LUMO_SMALL));

        return acceptBetButton;
    }

    private Div createAmountGameTimeDiv(GameBet gameBet, NflGame nflGame) {
        Div amountGameTimeDiv = new Div();
        var amountDivElement = amountGameTimeDiv.getElement();

        var amountLabel = gameBet.getBetCanBeSplit() ? "Bet Total" : "Bet Amount";

        createNameValueElements(amountLabel, "$" + gameBet.getAmount(), amountDivElement);
        createSpacerElement(amountDivElement);
        createNameValueElements(
                "Game Time",
                DateTimeFormatter.ofPattern("E, MMM d, h:mm a").format(nflGame.getLocalDateTime()) + " EST",
                amountDivElement);
        return amountGameTimeDiv;
    }

    private Div createProposerDiv(GameBet gameBet) {
        Div div = new Div();
        createNameValueElements("Proposer", gameBet.getProposer().getName(), div.getElement());

        ZoneId zone = ZoneId.of("America/New_York");
        var localDateTime = LocalDateTime.ofInstant(gameBet.getExpiryDate(), zone);

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
        Div div = new Div();

        var sumTotalBets =
                gameBet.getAcceptorTransactions().stream().mapToInt(PoolioTransaction::getAmount).sum();
        createNameValueElements(
                "Amount Available", "$" + (gameBet.getAmount() - sumTotalBets), div.getElement());

        return div;
    }

    private String createGameWithSpreadString(GameBet gameBet, NflGame nflGame) {
        return nflGame.getAwayTeam()
                + " at "
                + nflGame.getHomeTeam()
                + " ("
                + getSpreadString(gameBet.getSpread())
                + ")";
    }

    private void openAcceptBetConfirmDialog(GameBet gameBet, NflGame nflGame, @NotNull Integer betAmount) {

        if (gameBet.getProposer().equals(player)) {
            createErrorNotification(new Span("Cannot place bet on your own proposal"));
        } else {

            HorizontalLayout layout = new HorizontalLayout();
            layout.setAlignItems(Alignment.CENTER);
            layout.setJustifyContentMode(JustifyContentMode.CENTER);

            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Accept Bet : Are you sure");
            NflTeam otherTeamNotPicked = getNflTeamNotPicked(gameBet, nflGame);
            String spreadString = createSpreadString(gameBet, otherTeamNotPicked, nflGame);

            String message =
                    "Accept this Bet for $%d taking %s (%s)"
                            .formatted(betAmount, otherTeamNotPicked, spreadString);

            dialog.setText(message);

            dialog.setCancelable(true);

            dialog.setConfirmText("Accept Bet from " + gameBet.getProposer().getName());
            dialog.setConfirmButtonTheme("primary");
            dialog.addConfirmListener(
                    event -> {
                        dialog.close();
                        saveToDb(gameBet, betAmount);
                    });

            rootLayout.add(layout);
            dialog.open();
        }
    }

    private void saveToDb(GameBet gameBet, @NotNull Integer betAmount) {

        var transaction = nflBetService.createAcceptProposalTransaction(player, gameBet, betAmount);
        // poolioTransactionService.save(transaction);
        amountDialog.close();

        createSucessNotification(new Span("Bet Accepted"));
        log.info("Accepted bet: {}", transaction);
    }

    void createNameValueElements(String name, String value, Element element) {
        element.appendChild(ElementFactory.createStrong(name + ": "));
        element.appendChild(ElementFactory.createLabel(value + " "));
    }

    void createSpacerElement(Element element) {
        element.appendChild(ElementFactory.createEmphasis(" • "));
    }

    record SplitAmounts(int totalBetsSum, int availableToBetAmount) {
    }
}
