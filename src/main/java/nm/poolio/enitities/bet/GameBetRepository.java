package nm.poolio.enitities.bet;

import nm.poolio.model.enums.BetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public interface GameBetRepository
        extends JpaRepository<GameBet, Long>, JpaSpecificationExecutor<GameBet> {

    List<GameBet> findByStatusAndExpiryDateAfterAndAcceptanceDateIsNullOrderByCreatedDate(BetStatus status, Instant now);

    List<GameBet>  findByStatus(BetStatus status);
}
