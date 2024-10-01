package nm.poolio.enitities.bet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;

public interface GameBetRepository
        extends JpaRepository<GameBet, Long>, JpaSpecificationExecutor<GameBet> {

    List<GameBet> findByBetOpenIsTrueAndExpiryDateAfterAndAcceptanceDateIsNullOrderByCreatedDate(Instant now);
}
