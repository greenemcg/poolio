package nm.poolio.enitities.bet;

import nm.poolio.data.User;
import nm.poolio.model.enums.BetStatus;
import nm.poolio.model.enums.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface GameBetRepository
        extends JpaRepository<GameBet, Long>, JpaSpecificationExecutor<GameBet> {

    List<GameBet> findBySeasonAndStatusAndExpiryDateAfterAndAcceptanceDateIsNullOrderByCreatedDate(
            Season season, BetStatus status, Instant now);

    List<GameBet> findByStatusAndSeason(BetStatus status, Season season);

    List<GameBet> findBySeasonAndProposerTransactionCreditUserOrderByCreatedDateDesc(
            Season season, User player);

    @Query(
            "SELECT gb FROM GameBet gb JOIN gb.acceptorTransactions at WHERE  gb.season = :season AND at.creditUser.id = :transactionId")
    List<GameBet> findBySeasonAndAcceptorTransactionId(
            @Param("season") Season season, @Param("transactionId") Long transactionId);
}
