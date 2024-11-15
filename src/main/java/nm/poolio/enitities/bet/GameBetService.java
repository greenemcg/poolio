package nm.poolio.enitities.bet;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameBetService implements Serializable {
  @Serial private static final long serialVersionUID = 1011954453227284372L;
  private final GameBetRepository repository;

  public List<GameBet> findOpenBets() {
    return repository.findByExpiryDateAfterAndAcceptanceDateIsNullOrderByCreatedDate(Instant.now());
  }

  public GameBet save(GameBet gameBet) {
    return repository.save(gameBet);
  }
}
