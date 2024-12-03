package nm.poolio.views.bet;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import nm.poolio.enitities.bet.GameBet;
import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.model.NflGame;
import nm.poolio.model.enums.NflTeam;

public interface BetUtils {
  default String createAmountAvailableString(GameBet gameBet) {
    int sumTotalBets =
        gameBet.getAcceptorTransactions().stream().mapToInt(PoolioTransaction::getAmount).sum();
    return "$" + (gameBet.getAmount() - sumTotalBets);
  }

  private String convertBigDecimalSpread(BigDecimal bd) {
    BigDecimal stripped = bd.stripTrailingZeros();
    return stripped.scale() <= 0
        ? stripped.toPlainString()
        : bd.setScale(1, RoundingMode.UNNECESSARY).toString();
  }

  default String getSpreadString(BigDecimal spread) {
    String scaled = convertBigDecimalSpread(spread);
    return spread.equals(BigDecimal.ZERO)
        ? "PICK-EM"
        : (spread.doubleValue() > 0 ? "+" + scaled : scaled);
  }

  default SplitAmounts computeSplitBetAmounts(GameBet gameBet) {
    int totalBetsSum =
        gameBet.getAcceptorTransactions().stream().mapToInt(PoolioTransaction::getAmount).sum();
    return new SplitAmounts(totalBetsSum, gameBet.getAmount() - totalBetsSum);
  }

  default String createGameWithSpreadString(GameBet gameBet, NflGame nflGame) {
    return String.format(
        "%s at %s (%s)",
        getTeamAndScore(nflGame.getAwayTeam(), nflGame),
        getTeamAndScore(nflGame.getHomeTeam(), nflGame),
        getSpreadString(gameBet.getSpread()));
  }

  private Integer getScore(NflTeam team, NflGame game) {
    if (team.equals(game.getAwayTeam())) return game.getAwayScore();
    else return game.getHomeScore();
  }

  private String getTeamAndScore(NflTeam team, NflGame game) {
    if (game.getScore().isEmpty()) {
      return team.toString();
    } else {
      return "%s-%d".formatted(team, getScore(team, game));
    }
  }

  default void createNameValueElements(
      @NotNull String name, @NotNull String value, @NotNull Element element) {
    String delimiter = name.isEmpty() || value.isEmpty() ? "" : ": ";
    element.appendChild(ElementFactory.createStrong(name + delimiter));
    element.appendChild(ElementFactory.createLabel(value + " "));
  }

  default void createSpacerElement(Element element) {
    element.appendChild(ElementFactory.createEmphasis(" • "));
  }

  default void createPlayersDiv(Div div, GameBet gameBet) {
    boolean first = true;
    for (PoolioTransaction t : gameBet.getAcceptorTransactions()) {
      if (!first) {
        createSpacerElement(div.getElement());
      }
      first = false;
      createNameValueElements("", t.getCreditUser().getName(), div.getElement());
      createNameValueElements("", "$" + t.getAmount(), div.getElement());
    }
  }

  default void createPayOutsDiv(Div div, GameBet gameBet) {
    boolean first = true;
    for (PoolioTransaction t : gameBet.getResultTransactions()) {
      if (!first) {
        createSpacerElement(div.getElement());
      }
      first = false;
      createNameValueElements("", t.getDebitUser().getName(),  div.getElement());
      createNameValueElements("", "$" + t.getAmount(), div.getElement());
    }
  }

  default NflTeam getNflTeamNotPicked(GameBet gameBet, NflGame nflGame) {
    return (nflGame.getAwayTeam() == gameBet.getTeamPicked())
        ? nflGame.getHomeTeam()
        : nflGame.getAwayTeam();
  }

  default Component createBetPlayersString(GameBet gameBet) {
    Div div = new Div();
    createPlayersDiv(div, gameBet);
    return div;
  }

  default Component createBetPayOutsString(GameBet gameBet) {
    Div div = new Div();
    createPayOutsDiv(div, gameBet);
    return div;
  }

  record SplitAmounts(int totalBetsSum, int availableToBetAmount) {}
}
