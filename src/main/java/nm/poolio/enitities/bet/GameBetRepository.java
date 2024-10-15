package nm.poolio.enitities.bet;

import java.time.Instant;
import java.util.List;
import nm.poolio.data.User;
import nm.poolio.model.enums.BetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameBetRepository
        extends JpaRepository<GameBet, Long>, JpaSpecificationExecutor<GameBet> {

    List<GameBet> findByStatusAndExpiryDateAfterAndAcceptanceDateIsNullOrderByCreatedDate(BetStatus status, Instant now);

    List<GameBet>  findByStatus(BetStatus status);

    List<GameBet> findByProposerTransactionCreditUserOrderByCreatedDateDesc(User player);

    @Query("SELECT gb FROM GameBet gb JOIN gb.acceptorTransactions at WHERE at.creditUser.id = :transactionId")
    List<GameBet> findByAcceptorTransactionId(@Param("transactionId") Long transactionId);
}
