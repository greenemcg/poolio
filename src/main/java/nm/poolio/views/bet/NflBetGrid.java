package nm.poolio.views.bet;

import static nm.poolio.utils.VaddinUtils.*;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import nm.poolio.enitities.bet.GameBet;
import nm.poolio.model.NflGame;
import nm.poolio.services.NflGameService;
import nm.poolio.vaadin.PoolioGrid;
import org.vaadin.lineawesome.LineAwesomeIcon;

public interface NflBetGrid extends PoolioGrid<GameBet>, BetUtils {

  NflGameService getNflGameService();

  default Component createGameSpan(GameBet gameBet) {
    NflGame game = getNflGameService().findGameById(gameBet.getGameId());
    return new Span(createGameWithSpreadString(gameBet, game));
  }

  default void decorateTransactionGrid() {
    getGrid()
        .addColumn(
            new ComponentRenderer<>(transaction -> createUserComponent(transaction.getProposer())))
        .setHeader(createIconSpan(BET_ICON, "Proposer"))
        .setWidth("100px");

    createColumn(GameBet::getAmount, createIconSpan(AMOUNT_ICON, "Amt"))
        .setWidth("80px")
        .setComparator(GameBet::getAmount);

    getGrid()
        .addColumn(new ComponentRenderer<>(this::createGameSpan))
        .setHeader(createIconSpan(GAMES_ICON, "Game", LineAwesomeIcon.MINUS_SOLID))
        .setComparator(GameBet::getWeek);

    createColumn(GameBet::getWeek, createIconSpan(WEEK_ICON, "Week"));

    createColumn(GameBet::getTeamPicked, createIconSpan(POOLIO_ICON, "Pick"));

    createColumn(GameBet::getStatus, createIconSpan(STATUS_ICON, "Status"));
  }
}
