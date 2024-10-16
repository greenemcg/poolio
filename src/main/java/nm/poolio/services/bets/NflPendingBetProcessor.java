package nm.poolio.services.bets;

import java.math.BigDecimal;

import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.enitities.bet.GameBetCommon;
import nm.poolio.enitities.bet.GameBetService;
import nm.poolio.enitities.transaction.NoteCreator;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import nm.poolio.enitities.transaction.PoolioTransactionType;
import nm.poolio.model.enums.NflTeam;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.services.NflGameService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class NflPendingBetProcessor implements GameBetCommon, NoteCreator {
  private final GameBetService gameBetService;
  private final PoolioTransactionService poolioTransactionService;
  @Getter private final AuthenticatedUser authenticatedUser;

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
              var t = refund(b, poolioTransactionService, PoolioTransactionType.TIE_REFUND);
              b.getResultTransactions().add(t);




              log.info("Game {} has no winner yet", game.getId());
            return;
          }

          if (winner == NflTeam.TIE) {

            log.info("Game {} is a tie", game.getId());
            return;
          }

          if (winner.equals(b.getTeamPicked())) {
            log.info("Proposer {} Won", game.getId());
            return;
          } else {
            log.info("Acceptor(s) {} Won", game.getId());
          }
        });
  }
}
