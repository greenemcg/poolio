package nm.poolio.enitities.bet;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import nm.poolio.enitities.transaction.*;
import nm.poolio.model.enums.Season;
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

  default PoolioTransaction refundGameBet(int refundAmount, GameBet gameBet, String message, String userName) {
    var refund = new PoolioTransaction();
    refund.setAmount(refundAmount);
    refund.setNotes(List.of(JsonbNoteCreator.buildJsonbNote(message, userName)));

    refund.setDebitUser(gameBet.getProposer());
    refund.setCreditUser(gameBet.getProposerTransaction().getDebitUser());
    refund.setType(PoolioTransactionType.GAME_BET_REFUND);

    gameBet.getResultTransactions().add(refund);

    return refund;
  }

  default List<PoolioTransaction> refund(
      GameBet b,
      PoolioTransactionService poolioTransactionService,
      PoolioTransactionType transactionType) {
    int refundAmount = calculateRefund(b);
    if (refundAmount == 0) return null;

    var refund = new PoolioTransaction();
    refund.setSeason(Season.getCurrent());
    refund.setAmount(refundAmount);
    refund.setCreditUser(b.getProposerTransaction().getDebitUser());
    refund.setDebitUser(b.getProposerTransaction().getCreditUser());
    refund.setType(transactionType);
    if (CollectionUtils.isEmpty(refund.getNotes())) refund.setNotes(new ArrayList<>());

    if (CollectionUtils.isEmpty(b.getAcceptorTransactions()))
      return List.of(poolioTransactionService.save(refund));

    var transactions =
        b.getAcceptorTransactions().stream()
            .map(t -> refundAcceptor(t, b, poolioTransactionService, transactionType))
            .collect(Collectors.toList()); // this way creates a modifiable list

    transactions.add(poolioTransactionService.save(refund));
    return transactions;
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
    if (CollectionUtils.isEmpty(refund.getNotes())) refund.setNotes(new ArrayList<>());
    return poolioTransactionService.save(refund);
  }
}
