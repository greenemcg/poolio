package nm.poolio.views.result;

import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import nm.poolio.enitities.ticket.Ticket;

@RequiredArgsConstructor
public class TicketRanker {
  private final List<Ticket> ticketsSortedByFullScore;

  private int rank = 1;
  private boolean tie = false;
  private int tieCount = 0;

  public void rank() {

    if (ticketsSortedByFullScore.size() < 2) {
      return;
    }

    IntStream.range(0, ticketsSortedByFullScore.size())
        .forEach(
            index -> {
              int score = ticketsSortedByFullScore.get(index).getFullScore();

              if (index + 1 < ticketsSortedByFullScore.size()) process(index, score);
              else processLast(index, score);
            });
  }

  private void process(int index, int score) {
    int nextScore = ticketsSortedByFullScore.get(index + 1).getFullScore();

    if (score == nextScore) {
      tie = true;
      setRankInTicket(index);
      tieCount++;
    } else {
      if (!tie && tieCount > 0) {
        rank += tieCount;
        tieCount = 0;
      }

      setRankInTicket(index);
      rank++;
      tie = false;
    }
  }

  private void processLast(int index, int score) {
    if (!tie && tieCount > 0) {
      rank += tieCount;
    }

    int prevScore = ticketsSortedByFullScore.get(index - 1).getFullScore();
    tie = score == prevScore;
    setRankInTicket(index);
  }

  private void setRankInTicket(int index) {
    ticketsSortedByFullScore.get(index).setRank(rank);
    ticketsSortedByFullScore.get(index).setRankString((tie ? "T" : "") + rank);
  }
}
