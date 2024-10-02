package nm.poolio.enitities.bet;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.model.enums.BetStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameBetService implements Serializable, GameBetCommon {
  @Serial private static final long serialVersionUID = 1011954453227284372L;
  private final GameBetRepository repository;

  public List<GameBet> findAvailableBets() {
    log.debug("Now: {}", Instant.now());

    var list =
        repository
            .findByStatusAndExpiryDateAfterAndAcceptanceDateIsNullOrderByCreatedDate(
                BetStatus.OPEN, Instant.now())
            .stream()
            .filter(this::isProposalOpen)
            .toList();

    return list;
  }

  public List<GameBet> findOpenBets() {
    return repository.findByStatus(BetStatus.OPEN);
  }

  public GameBet save(GameBet gameBet) {
    return repository.save(gameBet);
  }
}
