package nm.poolio.enitities.bet;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GameBetRepository
    extends JpaRepository<GameBet, Long>, JpaSpecificationExecutor<GameBet> {

  List<GameBet> findByExpiryDateAfterAndAcceptanceDateIsNullOrderByCreatedDate(Instant now);
}
