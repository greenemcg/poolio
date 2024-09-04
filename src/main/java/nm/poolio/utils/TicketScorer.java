package nm.poolio.utils;

import java.util.List;
import lombok.RequiredArgsConstructor;
import nm.poolio.enitities.ticket.Ticket;
import nm.poolio.model.NflGame;

@RequiredArgsConstructor
public class TicketScorer {
  private final List<NflGame> games;
  int score = 0;
  int fullScore = 0;

  private void reset() {
    score = 0;
    fullScore = 0;
  }

  public void score(Ticket ticket) {
    reset();

    ticket
        .getSheet()
        .getGamePicks()
        .forEach(
            (gameKey, teamPicked) -> {
              var nflGame =
                  games.stream().filter(g -> g.getId().equals(gameKey)).findAny().orElse(null);
              if (nflGame != null && teamPicked != null) {
                var winner = nflGame.getWinner();

                if (teamPicked.equals(winner)) {
                  score += 10;
                  fullScore += 100000;
                }
              }
            });

    ticket.setScore(score);

    var optional = games.getLast().getScore();

    if (optional.isPresent()) {
      var diff = optional.get() - ticket.getTieBreaker();
      fullScore -= Math.abs(diff);
    }

    ticket.setFullScore(fullScore);
  }
}
