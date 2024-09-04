package nm.poolio.enitities.ticket;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import nm.poolio.cache.CacheConfig;
import nm.poolio.cache.CacheConfig.CacheName;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.model.enums.NflTeam;
import nm.poolio.model.enums.NflWeek;
import nm.poolio.model.enums.Season;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketService {
  private final TicketRepository repository;
  private final CacheConfig cacheConfig;

  public List<Ticket> getAllTickets(User player, Season season) {
    return repository.findByPlayerAndSeason(player, season);
  }

  public Optional<Ticket> findTicketForUserCurrentWeek(Pool pool, User player) {
    return repository.findByPlayerAndPoolAndSeasonAndWeek(
        player, pool, pool.getSeason(), pool.getWeek());
  }

  public List<Ticket> findTickets(Pool pool, NflWeek week) {
    return repository.findByPoolAndSeasonAndWeek(pool, pool.getSeason(), week);
  }

  public Optional<Ticket> findTicketForUser(User player, Long ticketId) {
    return repository.findByPlayerAndId(player, ticketId);
  }

  public boolean ticketExistsForUser(User player, Long ticketId) {
    return repository.existsByPlayerAndId(player, ticketId);
  }

  public Optional<Ticket> findTicketForUserWithWeek(User player, Pool pool, NflWeek week) {
    var result =
        repository.findByPlayerAndPoolAndSeasonAndWeek(player, pool, pool.getSeason(), week);

    return result;
  }

  public List<Ticket> findWinners(Pool pool, NflWeek week) {
    return repository.findByPoolAndSeasonAndWeekAndWinningTransactionNotNull(
        pool, pool.getSeason(), week);
  }

  public Ticket save(Ticket ticket) {
    var result = repository.save(ticket);

    clearCachesAfterSave(ticket);

    return result;
  }

  private void clearCachesAfterSave(Ticket ticket) {
    Cache scoredTicketCache = cacheConfig.getCache(CacheName.SCORED_TICKETS);
    scoredTicketCache.evict(List.of(ticket.getPool().getId(), ticket.getWeek().getWeekNum()));

    Cache userNameCache = cacheConfig.getCache(CacheName.USER_NAME);
    userNameCache.evict(ticket.getTransaction().getDebitUser().getUserName());
    userNameCache.evict(ticket.getTransaction().getCreditUser().getUserName());

    if (ticket.getTransaction().getPayAsYouGoUser() != null)
      userNameCache.evict(ticket.getTransaction().getPayAsYouGoUser().getUserName());
  }

  public boolean isTicketComplete(Ticket ticket) {
    PoolSheet sheet = ticket.getSheet();

    return sheet.getTieBreaker() != null && allGamesPicked(sheet.getGamePicks());
  }

  private boolean allGamesPicked(Map<String, NflTeam> picks) {
    return picks.values().stream().noneMatch(Objects::isNull);
  }
}
