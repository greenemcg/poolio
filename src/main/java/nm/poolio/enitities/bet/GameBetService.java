package nm.poolio.enitities.bet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.data.User;
import nm.poolio.model.enums.BetStatus;
import nm.poolio.model.enums.Season;
import org.springframework.stereotype.Service;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameBetService implements Serializable, GameBetCommon {
    @Serial
    private static final long serialVersionUID = 1011954453227284372L;
    private final GameBetRepository repository;

    public List<GameBet> findAvailableBets() {
        log.debug("Now: {}", Instant.now());

        return repository
                .findBySeasonAndStatusAndExpiryDateAfterAndAcceptanceDateIsNullOrderByCreatedDate(
                        Season.getCurrent(), BetStatus.OPEN, Instant.now())
                .stream()
                .filter(this::isProposalOpen)
                .toList();
    }

    public List<GameBet> findOpenBets() {
        return repository.findByStatusAndSeason(BetStatus.OPEN, Season.getCurrent());
    }

    public List<GameBet> findPendingBets() {
        return repository.findByStatusAndSeason(BetStatus.PENDING, Season.getCurrent());
    }

    public List<GameBet> findBetProposals(User player) {
        return repository.findBySeasonAndProposerTransactionCreditUserOrderByCreatedDateDesc(
                Season.getCurrent(), player);
    }

    public List<GameBet> findBetsForPlayer(User player) {
        return repository.findBySeasonAndAcceptorTransactionId(Season.getCurrent(), player.getId());
    }

    public GameBet save(GameBet gameBet) {
        return repository.save(gameBet);
    }
}
