package nm.poolio.services.bets;

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
import nm.poolio.model.enums.NflTeam;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.services.NflGameService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class NflPendingBetProcessor implements GameBetCommon, NoteCreator {
    private final GameBetService gameBetService;
    private final PoolioTransactionService poolioTransactionService;
    @Getter
    private final AuthenticatedUser authenticatedUser;
    private final NflGameService nflGameService;

    @Transactional
    public void process() {
        log.info("Processing game bets");
        var pendingBets = gameBetService.findPendingBets();
        log.debug("Found {} pending bets", pendingBets.size());

        pendingBets.forEach(
                b -> {
                    var game = nflGameService.findGameById(b.getGameId());
                    BigDecimal spread = b.getSpread();
                    var winner = game.findWinnerSpread(spread);

                    if (winner == NflTeam.TBD) {
                        log.info("Game {} has no winner yet", game.getId());
                        return;
                    }

                    if (winner == NflTeam.TIE) {
                        var t = refund(b, poolioTransactionService, PoolioTransactionType.TIE_REFUND);
                        b.getResultTransactions().addAll(t);
                        log.info("Game {} is a tie", game.getId());
                    } else if (winner.equals(b.getTeamPicked())) {
                        var t = createWinnerProposer(b, winner);
                        b.getResultTransactions().add(t);
                        log.info("Proposer {} Won", game.getId());
                    } else {
                        var t = createWinnerAcceptors(b, winner);
                        b.getResultTransactions().addAll(t);
                        log.info("Acceptor(s) {} Won", game.getId());
                    }


                    if (b.getBetCanBeSplit()
                            && !CollectionUtils.isEmpty(b.getAcceptorTransactions())
                            && b.getAmount() > findTaken(b)) {

                        var taken = findTaken(b);

                        var refundAmt = b.getAmount() - taken;
//              String message = StringConcatFactory.makeConcatWithConstants(
//                      "Refunded ", refundAmt, "$, Due to only " , taken", , " years old."
//              );

                        var message = "Refunded because bet was split and not enough was taken ";

                        var u = authenticatedUser.get().orElse(null);
                        PoolioTransaction refundTransaction =
                                refundGameBet(refundAmt, b, message, u == null ? "System" : u.getUserName());
                        log.info("{} {}", message, refundAmt);
                    }

                    b.setStatus(BetStatus.PAID);
                    gameBetService.save(b);
                });
    }

    private List<PoolioTransaction> createWinnerAcceptors(GameBet b, NflTeam winningTeam) {
        return b.getAcceptorTransactions().stream()
                .map(t -> createWinnerAcceptor(t, b, winningTeam))
                .toList();
    }

    private PoolioTransaction createWinnerAcceptor(
            PoolioTransaction poolioTransaction, GameBet b, NflTeam winningTeam) {
        var winner = new PoolioTransaction();
        winner.setAmount(poolioTransaction.getAmount() * 2);
        winner.setCreditUser(poolioTransaction.getDebitUser());
        winner.setDebitUser(poolioTransaction.getCreditUser());
        winner.setType(PoolioTransactionType.GAME_BET_WINNER);
        if (CollectionUtils.isEmpty(winner.getNotes())) winner.setNotes(new ArrayList<>());
        return poolioTransactionService.save(winner);
    }

    private PoolioTransaction createWinnerProposer(GameBet b, NflTeam winningTeam) {
        var betTaken = findTaken(b);
        var winner = new PoolioTransaction();
        winner.setAmount(betTaken * 2);
        winner.setCreditUser(b.getProposerTransaction().getDebitUser());
        winner.setDebitUser(b.getProposerTransaction().getCreditUser());
        winner.setType(PoolioTransactionType.GAME_BET_WINNER);
        if (CollectionUtils.isEmpty(winner.getNotes())) winner.setNotes(new ArrayList<>());
        return poolioTransactionService.save(winner);
    }
}
