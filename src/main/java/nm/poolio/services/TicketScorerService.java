package nm.poolio.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.enitities.ticket.Ticket;
import nm.poolio.enitities.ticket.TicketService;
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
    var weeklyGames = nflGameScorerService.getWeeklyGamesForPool(pool, week);

    var ticketsList = service.findTickets(pool, week);

    TicketScorer scorer = new TicketScorer(weeklyGames);
    ticketsList.forEach(scorer::score);
    ticketsList.sort(Comparator.comparing(Ticket::getFullScore).reversed());
    // must sort before rank

    try {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
      var s = objectMapper.writeValueAsString(ticketsList);
      System.out.println(s.length());

    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }


    new TicketRanker(ticketsList).rank();

    log.info("Scored {} tickets for pool {} week {}", ticketsList.size(), pool.getName(), week);


    return ticketsList;
  }
}
