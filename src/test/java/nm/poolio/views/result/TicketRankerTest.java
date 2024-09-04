package nm.poolio.views.result;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.IntStream;
import nm.poolio.enitities.ticket.Ticket;
import org.junit.jupiter.api.Test;

class TicketRankerTest {
  @Test
  void rankNoTies() {
    var tickets = List.of(createTicket(100), createTicket(50), createTicket(10), createTicket(0));

    new TicketRanker(tickets).rank();

    IntStream.range(0, tickets.size())
        .forEach(
            index -> {
              assertEquals("" + (index + 1), tickets.get(index).getRankString());
              assertEquals((index + 1), tickets.get(index).getRank());
            });
  }

  @Test
  void rankNoScores() {
    var tickets = List.of(createTicket(0), createTicket(0), createTicket(0));

    new TicketRanker(tickets).rank();

    tickets.forEach(t -> assertEquals("T1", t.getRankString()));
    tickets.forEach(t -> assertEquals(1, t.getRank()));
  }

  @Test
  void rankTiesStart() {
    var tickets =
        List.of(
            createTicket(100),
            createTicket(100),
            createTicket(50),
            createTicket(0),
            createTicket(0));

    new TicketRanker(tickets).rank();

    assertEquals("T1", tickets.get(0).getRankString());
    assertEquals("T1", tickets.get(1).getRankString());
    assertEquals("3", tickets.get(2).getRankString());
    assertEquals("T4", tickets.get(3).getRankString());
    assertEquals("T4", tickets.get(4).getRankString());

    assertEquals(1, tickets.get(0).getRank());
    assertEquals(1, tickets.get(1).getRank());
    assertEquals(3, tickets.get(2).getRank());
    assertEquals(4, tickets.get(3).getRank());
    assertEquals(4, tickets.get(4).getRank());
  }

  Ticket createTicket(int fullScore) {
    var t = new Ticket();
    t.setFullScore(fullScore);
    return t;
  }
}
