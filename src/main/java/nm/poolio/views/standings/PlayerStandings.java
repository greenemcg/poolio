package nm.poolio.views.standings;

import java.text.DecimalFormat;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import nm.poolio.data.User;
import nm.poolio.enitities.ticket.Ticket;

@Data
@RequiredArgsConstructor
public class PlayerStandings {
  private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("#.00");
  private final List<Ticket> tickets;
  private final User player;
  int totalCorrect;
  int totalGames;
  int totalWinnings;
  int wins;

  public int getWeekPlayed() {
    return tickets.size();
  }

  public void calculateTotals() {

    tickets.forEach(
        ticket -> {
          totalCorrect += ticket.getCorrect();
          totalGames += ticket.getSheet().getGamePicks().size();

          if (ticket.getWinningTransaction() != null) {
            totalWinnings += ticket.getWinningTransaction().getAmount();
            wins++;
          }
        });
  }

  public String getWinPercentage() {
    return PERCENT_FORMAT.format((double) totalCorrect / totalGames * 100);
  }

  public String getWinnngsString() {
    return (totalWinnings == 0) ? "" : "$" + totalWinnings;
  }

  public String findPlayerName() {
    return player != null ? player.getName() : "";
  }
}
