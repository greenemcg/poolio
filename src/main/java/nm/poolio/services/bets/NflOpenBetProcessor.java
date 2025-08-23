package nm.poolio.services.bets;

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

import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
class NflOpenBetProcessor implements GameBetCommon, NoteCreator {
    private final GameBetService gameBetService;
    private final PoolioTransactionService poolioTransactionService;
    @Getter
    private final AuthenticatedUser authenticatedUser;

    @Transactional
    public void process() {
        var openBets = gameBetService.findOpenBets();
        log.debug("Found {} open bets", openBets.size());

        openBets.forEach(
                b -> {

                    // make 300
                    if (b.getModifiedDate().isAfter(Instant.now().minusSeconds(30))) {
                        log.info("Ignored bet {} because it was modified less than 5 minutes ago", b.getId());
                        return;
                    }

                    boolean isProposalOpen = isProposalOpen(b);

                    if (b.getBetCanBeSplit() && !b.getAcceptorTransactions().isEmpty()) {
                        log.info(
                                "Ignored bet {} because it can be split and has acceptor transactions", b.getId());
                        isProposalOpen = false;
                    }

                    boolean isExpired = b.getExpiryDate().isBefore(Instant.now());

                    if (isProposalOpen && !isExpired) return;

                    if (isProposalOpen) {
                        var t = refund(b, poolioTransactionService, PoolioTransactionType.GAME_BET_REFUND);
                        b.getResultTransactions().addAll(t);
                        b.setStatus(
                                CollectionUtils.isEmpty(b.getAcceptorTransactions())
                                        ? BetStatus.CLOSED
                                        : BetStatus.PENDING);
                        log.info(
                                "Bet {} is expired and set to {} - {}",
                                b.getStatus(),
                                b.getId(),
                                b.getInfoString());
                    } else {
                        b.setStatus(BetStatus.PENDING);
                        log.info("Bet {} is expired and set to Pending - {}", b.getId(), b.getInfoString());
                    }

                    gameBetService.save(b);
                });
    }
}
