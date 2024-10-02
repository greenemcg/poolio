package nm.poolio.enitities.bet;

import nm.poolio.enitities.transaction.PoolioTransaction;

public interface GameBetCommon {
  default boolean isProposalOpen(GameBet gameBet) {
    return gameBet.getBetCanBeSplit() ? findTaken(gameBet) < gameBet.getAmount() : gameBet.getAcceptorTransactions().isEmpty();
  }

  default int findTaken(GameBet gameBet) {
    return gameBet.getAcceptorTransactions().stream().mapToInt(PoolioTransaction::getAmount).sum();
  }
}
