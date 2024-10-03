package nm.poolio.services;

import lombok.RequiredArgsConstructor;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.enitities.ticket.PoolSheet;
import nm.poolio.enitities.ticket.Ticket;
import nm.poolio.enitities.ticket.TicketService;
import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import nm.poolio.enitities.transaction.PoolioTransactionType;
import nm.poolio.model.JsonbNote;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketUiService {
  private final PoolioTransactionService poolioTransactionService;
  private final NflGameService nflGameService;
  private final TicketService service;

  public Status checkStatus(Pool pool, User player) {
    switch (pool.getStatus()) {
      case OPEN -> {
        return checkFunds(pool, player);
      }
      case CLOSED, PENDING, PAID -> {
        return Status.STATUS_NOT_OPEN;
      }
      default -> {
        return Status.ERROR;
      }
    }
  }

  public Status checkFunds(Pool pool, User player) {
    int funds = poolioTransactionService.getFunds(player);

    if (player.isPayAsYouGo()) {
      funds += poolioTransactionService.getFunds(pool.getPayAsYouGoUser());

      if (funds + pool.getPayAsYouGoUser().getCreditAmount() < pool.getAmount())
        return Status.PAY_AS_YOU_GO_USER_NOT_ENOUGH_FUNDS;
      else return Status.OK;

    } else {
      if (funds + player.getCreditAmount() < pool.getAmount())
        return Status.PLAYER_NOT_ENOUGH_FUNDS;
      else return Status.OK;
    }
  }

  public PoolioTransaction createTransaction(Pool pool, User player, @Nullable JsonbNote note) {
    PoolioTransaction poolioTransaction = new PoolioTransaction();
    poolioTransaction.setDebitUser(pool.getBankUser());

    if (player.isPayAsYouGo()) {
      poolioTransaction.setCreditUser(pool.getPayAsYouGoUser());
      poolioTransaction.setPayAsYouGoUser(player);
    } else poolioTransaction.setCreditUser(player);

    poolioTransaction.setAmount(pool.getAmount());
    poolioTransaction.setType(PoolioTransactionType.POOL_PURCHASE);

    if (note != null) poolioTransaction.setNotes(List.of(note));

    return poolioTransaction;
  }

  public Ticket createTicket(Ticket ticket, Pool pool, User player, JsonbNote note) {

    PoolSheet poolSheet = new PoolSheet();

    var games =
        nflGameService.getWeeklyGamesThursdayFiltered(pool.getWeek(), pool.isIncludeThursday());

    games.forEach(g -> poolSheet.getGamePicks().put(g.getId(), null));
    ticket.setSheet(poolSheet);

    // var jsonbNote = JsonbNote.builder().note("C").created(Instant.now()).user("Test").build();

    ticket.setTransaction(createTransaction(pool, player, note));

    return service.save(ticket);
  }

  public enum Status {
    STATUS_NOT_OPEN,
    ERROR,
    PLAYER_NOT_ENOUGH_FUNDS,
    PAY_AS_YOU_GO_USER_NOT_ENOUGH_FUNDS,
    OK
  }
}
