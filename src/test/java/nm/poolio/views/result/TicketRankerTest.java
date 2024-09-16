package nm.poolio.views.result;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import nm.poolio.enitities.ticket.Ticket;
import org.junit.jupiter.api.Test;

class TicketRankerTest implements FileUtil {
  @Test
  void testWithJsonData() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    var jsonInput = readFromFileToString("tickets.json");

    List<Ticket> tickets =
        mapper.readValue(
            jsonInput, mapper.getTypeFactory().constructCollectionType(List.class, Ticket.class));

    new TicketRanker(tickets).rank();

    assertEquals("1", tickets.get(0).getRankString());

    IntStream.range(1, 6)
        .forEach(
            index -> {
              assertEquals("T2", tickets.get(index).getRankString());
            });

    IntStream.range(7, 12)
        .forEach(
            index -> {
              assertEquals("T8", tickets.get(index).getRankString());
            });

    IntStream.range(13, 18)
        .forEach(
            index -> {
              assertEquals("T14", tickets.get(index).getRankString());
            });

    assertEquals("19", tickets.get(18).getRankString());

    tickets.forEach(t -> System.out.println(t.getRankString()));
  }

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
