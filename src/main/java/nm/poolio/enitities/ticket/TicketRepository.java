package nm.poolio.enitities.ticket;

import java.util.List;
import java.util.Optional;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.model.enums.NflWeek;
import nm.poolio.model.enums.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
  List<Ticket> findByPlayerAndSeason(User player, Season season);

  Optional<Ticket> findByPlayerAndPoolAndSeasonAndWeek(
      User player, Pool pool, Season season, NflWeek week);

  List<Ticket> findByPoolAndSeasonAndWeek(Pool pool, Season season, NflWeek week);

  List<Ticket> findByPoolAndSeasonAndWeekAndWinningTransactionNotNull(
      Pool pool, Season season, NflWeek week);

  Optional<Ticket> findByPlayerAndId(User player, Long id);

  boolean existsByPlayerAndId(User player, Long id);
}
