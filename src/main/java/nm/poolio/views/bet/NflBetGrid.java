package nm.poolio.views.bet;

import static nm.poolio.utils.VaddinUtils.*;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import java.time.format.DateTimeFormatter;
import nm.poolio.data.AbstractEntity;
import nm.poolio.enitities.bet.GameBet;
import nm.poolio.model.NflGame;
import nm.poolio.services.NflGameService;
import nm.poolio.vaadin.PoolioGrid;
import org.vaadin.lineawesome.LineAwesomeIcon;

public interface NflBetGrid extends PoolioGrid<GameBet>, BetUtils {

  NflGameService getNflGameService();

  private Component createOtherTeamPickedSpan(GameBet gameBet) {
    NflGame game = getNflGameService().findGameById(gameBet.getGameId());
    return new Span(getNflTeamNotPicked(gameBet, game).toString());
  }

  private Component createGameSpan(GameBet gameBet) {
    NflGame game = getNflGameService().findGameById(gameBet.getGameId());
    String message =
        createGameWithSpreadString(gameBet, game)
            + " - "
            + DateTimeFormatter.ofPattern("MMM d, h:mm a").format(game.getLocalDateTime());

    return new Span(message);
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

    if (!isProposal)
      getGrid()
          .addColumn(
              new ComponentRenderer<>(
                  transaction -> createUserComponent(transaction.getProposer())))
          .setHeader(createIconSpan(BET_ICON, "Proposer"))
          .setAutoWidth(true);

    if (isProposal)
      getGrid()
          .addColumn(
              new ComponentRenderer<>(transaction -> new Span("$" + transaction.getAmount())))
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
          .setHeader(createIconSpan(POOLIO_ICON, "Your Pick"))
          .setAutoWidth(true);

    createColumn(GameBet::getWeek, createIconSpan(WEEK_ICON, "Week"));
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

    createColumn(
        GameBet::getExpirationString, createIconSpan(LineAwesomeIcon.CLOCK_SOLID, "Expires (EST)"));

    getGrid()
        .addColumn(
            new LocalDateTimeRenderer<>(
                GameBet::getCreatedLocalDateTime,
                () -> DateTimeFormatter.ofPattern("MMM d, h:mm a")))
        .setHeader(createIconSpan(CREATED_ICON, "Created (EST)"))
        .setAutoWidth(true)
        .setComparator(AbstractEntity::getCreatedDate);
  }
}
