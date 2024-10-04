package nm.poolio.services;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.data.User;
import nm.poolio.enitities.bet.GameBet;
import nm.poolio.enitities.bet.GameBetService;
import nm.poolio.enitities.transaction.NoteCreator;
import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import nm.poolio.enitities.transaction.PoolioTransactionType;
import nm.poolio.model.NflGame;
import nm.poolio.model.enums.NflTeam;
import nm.poolio.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Service
public class NflBetService implements NoteCreator {
  private final PoolioTransactionService poolioTransactionService;
  private final UserService userService;
  private final NflGameService nflGameService;
  private final GameBetService gameBetService;
  @Getter private final AuthenticatedUser authenticatedUser;

  @Getter
  @Value("${poolio.bet.banker:bet_banker}")
  private String betBanker;

  private NflTeam getOtherTeam(NflGame game, NflTeam pick) {
    if (game.getHomeTeam().equals(pick)) return game.getAwayTeam();
    else return game.getHomeTeam();
  }

  @Transactional
  public PoolioTransaction createAcceptProposalTransaction(
      @NotNull User player, @NotNull GameBet gameBet, @NotNull Integer betAmount) {

    var banker = userService.findByUserName(getBetBanker());

    PoolioTransaction poolioTransaction = new PoolioTransaction();
    poolioTransaction.setDebitUser(banker);
    poolioTransaction.setCreditUser(player);
    poolioTransaction.setAmount(betAmount);
    poolioTransaction.setType(PoolioTransactionType.ACCEPT_PROPOSAL);

    var nflGame = nflGameService.findGameById(gameBet.getGameId());
    var otherTeam = getOtherTeam(nflGame, gameBet.getTeamPicked());

    poolioTransaction.setNotes(
        List.of(
            buildNote(
                "%s at %s - Amount: %d Spread: %s Team:"
                    .formatted(
                        nflGame.getAwayTeam().name(),
                        nflGame.getHomeTeam().name(),
                        betAmount,
                        gameBet.getSpread(),
                        otherTeam.name()))));

    var saved = poolioTransactionService.save(poolioTransaction);

    gameBet.getAcceptorTransactions().add(saved);

    gameBetService.save(gameBet);
    log.info("Created transaction: {}", saved);

    return saved;
  }
}
