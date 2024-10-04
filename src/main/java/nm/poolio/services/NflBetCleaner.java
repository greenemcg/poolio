package nm.poolio.services;

import java.time.Instant;
import java.util.ArrayList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.enitities.bet.GameBet;
import nm.poolio.enitities.bet.GameBetCommon;
import nm.poolio.enitities.bet.GameBetService;
import nm.poolio.enitities.transaction.NoteCreator;
import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import nm.poolio.enitities.transaction.PoolioTransactionType;
import nm.poolio.model.enums.BetStatus;
import nm.poolio.security.AuthenticatedUser;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@Slf4j
@RequiredArgsConstructor
public class NflBetCleaner implements GameBetCommon, NoteCreator {
  private final GameBetService gameBetService;
  private final PoolioTransactionService poolioTransactionService;
  @Getter private final AuthenticatedUser authenticatedUser;

  @Scheduled(cron = "0 */5 * * * *")
  public void cleanBets() {

    var openBets = gameBetService.findOpenBets();
      log.debug("Cleaning bets found {} open bets", openBets.size());
    openBets.forEach(
        b -> {
          boolean isProposalOpen = isProposalOpen(b);
          boolean isExpired = b.getExpiryDate().isBefore(Instant.now());

          if (isProposalOpen && !isExpired) return;

          if (isProposalOpen) {
            refund(b);
            b.setStatus(
                CollectionUtils.isEmpty(b.getAcceptorTransactions())
                    ? BetStatus.CLOSED
                    : BetStatus.PENDING);
          } else {
            b.setStatus(BetStatus.PENDING);
          }

          gameBetService.save(b);
        });
  }

  private void refund(GameBet b) {
    int refundAmount = calculateRefund(b);

    if (refundAmount == 0) {
      log.info("No refund needed for bet {}", b);
      return;
    }

    var refund = new PoolioTransaction();
    refund.setAmount(refundAmount);
    refund.setCreditUser(b.getProposerTransaction().getDebitUser());
    refund.setDebitUser(b.getProposerTransaction().getCreditUser());
    refund.setType(PoolioTransactionType.GAME_BET_REFUND);

    if (CollectionUtils.isEmpty(refund.getNotes())) {
      refund.setNotes(new ArrayList<>());
    }

    poolioTransactionService.save(refund);
  }

  private int calculateRefund(GameBet b) {
    return b.getBetCanBeSplit() ? b.getAmount() - findTaken(b) : b.getAmount();
  }
}
