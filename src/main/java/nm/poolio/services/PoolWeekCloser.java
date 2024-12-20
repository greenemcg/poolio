package nm.poolio.services;

import jakarta.transaction.Transactional;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.enitities.ticket.Ticket;
import nm.poolio.enitities.ticket.TicketService;
import nm.poolio.enitities.transaction.NoteCreator;
import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import nm.poolio.enitities.transaction.PoolioTransactionType;
import nm.poolio.exceptions.PoolioException;
import nm.poolio.model.JsonbNote;
import nm.poolio.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class PoolWeekCloser implements NoteCreator {
  private final PoolioTransactionService poolioTransactionService;
  @Getter private final AuthenticatedUser authenticatedUser;
  private final TicketService ticketService;

  @Transactional
  public void close(List<Ticket> scoredTickets) {
    if (CollectionUtils.isEmpty(scoredTickets)) {
      throw new PoolioException("No Tickets");
    }

    Pool pool = scoredTickets.getFirst().getPool();

    var winners = scoredTickets.stream().filter(t -> t.getRank().equals(1)).toList();

    if (CollectionUtils.isEmpty(winners)) {
      throw new PoolioException("No Winners");
    }

    winners.forEach(
        ticket -> {
          PoolioTransaction poolioTransaction = new PoolioTransaction();

          var winningPlayer = ticket.getPlayer();

          if (winningPlayer.isPayAsYouGo()) {
            poolioTransaction.setDebitUser(ticket.getPool().getPayAsYouGoUser());
            poolioTransaction.setPayAsYouGoUser(winningPlayer);
          } else poolioTransaction.setDebitUser(winningPlayer);

          poolioTransaction.setCreditUser(pool.getBankUser());
          final var amount = (pool.getAmount() * scoredTickets.size()) / winners.size();
          poolioTransaction.setAmount(amount);
          poolioTransaction.setType(PoolioTransactionType.POOL_WINNER);

          JsonbNote note =
              buildNote(
                  createHalfWinnerString(winners.size())
                      + "Winner Pool: %s-%s ".formatted(pool.getName(), pool.getWeek())
                      + createPayAyYouGoString(winningPlayer, ticket.getPool()));

          poolioTransaction.setNotes(List.of(note));

          var saveTrans = poolioTransactionService.save(poolioTransaction);
          ticket.setWinningTransaction(saveTrans);
          ticketService.save(ticket);
        });
  }

  private String createHalfWinnerString(int size) {
    return (size < 2) ? "" : "1/" + size + " ";
  }

  private String createPayAyYouGoString(User winningPlayer, Pool pool) {
    return (winningPlayer.isPayAsYouGo())
        ? " - Paid PasAsYouGo User: %s ".formatted(pool.getPayAsYouGoUser().getName())
        : "";
  }
}
