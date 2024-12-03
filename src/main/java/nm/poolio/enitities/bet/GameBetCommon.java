package nm.poolio.enitities.bet;

import java.util.ArrayList;
import java.util.List;

import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import nm.poolio.enitities.transaction.PoolioTransactionType;
import org.springframework.util.CollectionUtils;

public interface GameBetCommon {
  default boolean isProposalOpen(GameBet gameBet) {
    return gameBet.getBetCanBeSplit()
        ? findTaken(gameBet) < gameBet.getAmount()
        : gameBet.getAcceptorTransactions().isEmpty();
  }

  default int calculateRefund(GameBet b) {
    return Boolean.TRUE.equals(b.getBetCanBeSplit()) ? b.getAmount() - findTaken(b) : b.getAmount();
  }

  default int findTaken(GameBet gameBet) {
    return gameBet.getAcceptorTransactions().stream().mapToInt(PoolioTransaction::getAmount).sum();
  }

  default List<PoolioTransaction> refund(
      GameBet b,
      PoolioTransactionService poolioTransactionService,
      PoolioTransactionType transactionType) {
    int refundAmount = calculateRefund(b);

    if (refundAmount == 0) {
      return null;
    }

    var refund = new PoolioTransaction();
    refund.setAmount(refundAmount);
    refund.setCreditUser(b.getProposerTransaction().getDebitUser());
    refund.setDebitUser(b.getProposerTransaction().getCreditUser());
    refund.setType(transactionType);

    if (CollectionUtils.isEmpty(refund.getNotes())) {
      refund.setNotes(new ArrayList<>());
    }

    if( CollectionUtils.isEmpty(b.getAcceptorTransactions()))
        return List.of(poolioTransactionService.save(refund));
    else {
      var transactions =
          b.getAcceptorTransactions().stream()
              .map(t -> refundAcceptor(t, b, poolioTransactionService, transactionType))
              .toList();
      transactions.add(poolioTransactionService.save(refund));
      return transactions;

    }

  }

  private PoolioTransaction refundAcceptor(
      PoolioTransaction t,
      GameBet gameBet,
      PoolioTransactionService poolioTransactionService,
      PoolioTransactionType transactionType) {

    var refund = new PoolioTransaction();
    refund.setAmount(t.getAmount());
    refund.setCreditUser(t.getDebitUser());
    refund.setDebitUser(t.getCreditUser());
    refund.setType(transactionType);

    if (CollectionUtils.isEmpty(refund.getNotes())) {
      refund.setNotes(new ArrayList<>());
    }

    return poolioTransactionService.save(refund);
  }
}
