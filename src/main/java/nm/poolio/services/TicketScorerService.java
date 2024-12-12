package nm.poolio.services;

import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.enitities.ticket.Ticket;
import nm.poolio.enitities.ticket.TicketService;
import nm.poolio.model.NflGame;
import nm.poolio.model.enums.NflWeek;
import nm.poolio.utils.TicketScorer;
import nm.poolio.views.result.TicketRanker;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketScorerService {
  private final TicketService service;
  private final NflGameScorerService nflGameScorerService;

  @Cacheable(cacheNames = "SCORED_TICKETS", key = "{#pool.id, #week.weekNum}")
  public List<Ticket> findAndScoreTickets(Pool pool, NflWeek week) {
    List<NflGame> weeklyGames = nflGameScorerService.getWeeklyGamesForPool(pool, week);
    List<Ticket> ticketsList = service.findTickets(pool, week);

    TicketScorer scorer = new TicketScorer(weeklyGames);
    ticketsList.forEach(scorer::score);
    ticketsList.sort(Comparator.comparing(Ticket::getFullScore).reversed());

    new TicketRanker(ticketsList).rank();

    return ticketsList;
  }
}
