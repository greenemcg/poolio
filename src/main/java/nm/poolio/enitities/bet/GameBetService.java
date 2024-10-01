package nm.poolio.enitities.bet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.enitities.transaction.PoolioTransaction;
import org.springframework.stereotype.Service;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameBetService implements Serializable {
    @Serial
    private static final long serialVersionUID = 1011954453227284372L;
    private final GameBetRepository repository;

    public List<GameBet> findOpenBets() {
        return repository.findByBetOpenIsTrueAndExpiryDateAfterAndAcceptanceDateIsNullOrderByCreatedDate(Instant.now())
                .stream()
                .filter(this::isProposalOpen)
                .toList();
    }

    private boolean isProposalOpen(GameBet gameBet) {
        if (gameBet.getBetCanBeSplit()) {
            int totalTaken = gameBet.getAcceptorTransactions().stream()
                    .mapToInt(PoolioTransaction::getAmount)
                    .sum();
            return totalTaken < gameBet.getAmount();
        }
        return gameBet.getAcceptorTransactions().isEmpty();
    }

    public GameBet save(GameBet gameBet) {
        return repository.save(gameBet);
    }
}
