package nm.poolio.views.bet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import nm.poolio.enitities.bet.GameBet;
import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.model.NflGame;

public interface BetUtils {

  private String convertBigDecimalSpread(BigDecimal bd) {
    return bd.stripTrailingZeros().scale() <= 0
        ? bd.stripTrailingZeros().toPlainString()
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
        nflGame.getAwayTeam(), nflGame.getHomeTeam(), getSpreadString(gameBet.getSpread()));
  }

  record SplitAmounts(int totalBetsSum, int availableToBetAmount) {}
}
