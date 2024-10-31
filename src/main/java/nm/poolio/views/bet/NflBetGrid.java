package nm.poolio.views.bet;

import static nm.poolio.utils.VaddinUtils.*;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import nm.poolio.data.User;
import nm.poolio.enitities.bet.GameBet;
import nm.poolio.model.enums.NflTeam;
import nm.poolio.services.NflGameService;
import nm.poolio.vaadin.PoolioGrid;

public interface NflBetGrid extends PoolioGrid<GameBet>, BetUtils {

  NflGameService getNflGameService();

  User getPlayer();

  private Component createOtherTeamPickedSpan(GameBet gameBet) {
    var t = getNflTeamNotPicked(gameBet, gameBet.getGame());

    var winningTeam = gameBet.getGame().findWinnerSpread(gameBet.getSpread());

    if (winningTeam == NflTeam.TBD) {
      return new Span(t.toString());
    }

    if (winningTeam == NflTeam.TIE) {
      Span span = new Span();
      span.getStyle().set("font-style", "italic");
      span.add(t.toString() + "-TIE");
      return span;
    }

    Span span = new Span();
    if (winningTeam == t) {
      span.getStyle().set("font-weight", "bold");
      span.add(t.toString() + "-WIN");
    } else {
      span.getStyle().set("text-decoration", "line-through");
      span.add(t.toString());
    }
    return span;
  }

  private Component createGameSpan(GameBet gameBet) {
    var game = gameBet.getGame();
    Span span = new Span();

    span.add(createGameWithSpreadString(gameBet, game));

    return span;
  }

  private Component createPlayersComponent(GameBet gameBet) {
    Div div = new Div();
    createPlayersDiv(div, gameBet);
    return div;
  }

  private Component amountAvailSpan(GameBet gameBet) {
    return new Span(gameBet.getBetCanBeSplit() ? createAmountAvailableString(gameBet) : "N/A");
  }

  default void decorateTransactionGrid(boolean isProposal) {

    getGrid().setDetailsVisibleOnClick(false);

    if (!isProposal)
      getGrid()
          .addColumn(
              new ComponentRenderer<>(
                  transaction -> createUserComponent(transaction.getProposer())))
          .setHeader(createIconSpan(BET_ICON, "Proposer"))
          .setAutoWidth(true)
          .setFrozen(true)
          .setFlexGrow(0);

    if (isProposal)
      getGrid()
          .addColumn(new ComponentRenderer<>(gameBet -> new Span("$" + gameBet.getAmount())))
          .setHeader(createIconSpan(AMOUNT_ICON, "$ AMT"))
          .setAutoWidth(true);
    else
      getGrid()
          .addColumn(new ComponentRenderer<>(gameBet -> new Span("$" + getPlayerAmount(gameBet))))
          .setHeader(createIconSpan(AMOUNT_ICON, "$ AMT"))
          .setAutoWidth(true);

    getGrid()
        .addColumn(new ComponentRenderer<>(this::createGameSpan))
        .setHeader(createIconSpan(GAMES_ICON, "Game"))
        .setAutoWidth(true)
        .setComparator(GameBet::getWeek);

    if (isProposal) createColumn(GameBet::getTeamPicked, createIconSpan(POOLIO_ICON, "Pick"));
    else
      getGrid()
          .addColumn(new ComponentRenderer<>(this::createOtherTeamPickedSpan))
          .setHeader(createIconSpan(POOLIO_ICON, "Pick"))
          .setAutoWidth(true);

    createColumn(GameBet::getStatus, createIconSpan(STATUS_ICON, "Status"));

    getGrid()
        .addColumn(new ComponentRenderer<>(this::createPlayersComponent))
        .setHeader(createIconSpan(PLAYERS_ICON, "Player(s)"))
        .setAutoWidth(true)
        .setComparator(GameBet::getWeek);

    createColumn(GameBet::getBetCanBeSplit, createIconSpan(SPLIT_ICON, "Split"));

    if (isProposal)
      getGrid()
          .addColumn(new ComponentRenderer<>(this::amountAvailSpan))
          .setHeader(createIconSpan(GAMES_ICON, "$ Avail"))
          .setAutoWidth(true);
  }

  private Integer getPlayerAmount(GameBet gameBet) {

    var o =
        gameBet.getAcceptorTransactions().stream()
            .filter(t -> t.getCreditUser().equals(getPlayer()))
            .findFirst()
            .map(t -> t.getAmount());

    return o.orElse(-1);
  }
}
