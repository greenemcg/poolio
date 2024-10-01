package nm.poolio.enitities.transaction;

import java.util.List;
import nm.poolio.data.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

interface PoolioTransactionRepository
    extends JpaRepository<PoolioTransaction, Long>, JpaSpecificationExecutor<PoolioTransaction> {
  @Query("select sum(amount) from PoolioTransaction where creditUser = ?1")
  Integer calculateCreditAmount(User user);

  @Query("select sum(amount) from PoolioTransaction where debitUser = ?1")
  Integer calculateDebitAmount(User user);

  List<PoolioTransaction> findByDebitUserOrCreditUserOrPayAsYouGoUser(
      User credituser, User debituser, User ayAsYouGoUser);
}
