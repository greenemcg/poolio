package nm.poolio.services.bets;

import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.enitities.bet.GameBetCommon;
import nm.poolio.enitities.bet.GameBetService;
import nm.poolio.enitities.transaction.NoteCreator;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import nm.poolio.enitities.transaction.PoolioTransactionType;
import nm.poolio.model.enums.BetStatus;
import nm.poolio.security.AuthenticatedUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Component
@Slf4j
@RequiredArgsConstructor
class NflOpenBetProcessor implements GameBetCommon, NoteCreator {
  private final GameBetService gameBetService;
  private final PoolioTransactionService poolioTransactionService;
  @Getter private final AuthenticatedUser authenticatedUser;

  @Transactional
 public void process() {
    var openBets = gameBetService.findOpenBets();
    log.debug("Found {} open bets", openBets.size());

    openBets.forEach(
        b -> {
          boolean isProposalOpen = isProposalOpen(b);
          boolean isExpired = b.getExpiryDate().isBefore(Instant.now());

          if (isProposalOpen && !isExpired) return;

          if (isProposalOpen) {
            var t = refund(b, poolioTransactionService, PoolioTransactionType.GAME_BET_REFUND);
            b.getResultTransactions().add(t);
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
}
