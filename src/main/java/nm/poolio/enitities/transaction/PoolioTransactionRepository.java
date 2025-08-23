package nm.poolio.enitities.transaction;

import nm.poolio.data.User;
import nm.poolio.model.enums.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface PoolioTransactionRepository
        extends JpaRepository<PoolioTransaction, Long>, JpaSpecificationExecutor<PoolioTransaction> {
    @Query("select sum(amount) from PoolioTransaction where creditUser = ?1 and season = ?2")
    Integer calculateCreditAmount(User user, Season season);

    @Query("select sum(amount) from PoolioTransaction where debitUser = ?1 and season = ?2")
    Integer calculateDebitAmount(User user, Season season);

    List<PoolioTransaction> findByDebitUserOrCreditUserOrPayAsYouGoUserAndSeason(
            User credituser, User debituser, User ayAsYouGoUser, Season season);

    @Query(
            "select t from PoolioTransaction t where (t.creditUser=?1 or t.debitUser=?2 or t.payAsYouGoUser=?3) and season=?4")
    List<PoolioTransaction> findForUser(
            User credituser, User debituser, User ayAsYouGoUser, Season season);

    List<PoolioTransaction> findBySeason(Season season);
}
