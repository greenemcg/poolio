package nm.poolio.views.standings;

import static nm.poolio.utils.VaddinUtils.*;

import com.vaadin.flow.data.renderer.ComponentRenderer;
import nm.poolio.vaadin.PoolioGrid;

public interface StandingsGrid extends PoolioGrid<PlayerStandings> {
  default void decorateGrid() {

    getGrid()
        .addColumn(new ComponentRenderer<>(ticket -> createUserComponent(ticket.getPlayer())))
        .setHeader(createIconSpan(PLAYER_ICON, "Player"))
        .setAutoWidth(true)
        .setComparator(t -> t.getPlayer().getName());
    createColumn(PlayerStandings::getTotalCorrect, createIconSpan(WIN_ICON, "Correct"))
        .setComparator(PlayerStandings::getTotalCorrect);
    createColumn(PlayerStandings::getTotalGames, createIconSpan(GAMES_ICON, "G. Played"))
        .setComparator(PlayerStandings::getTotalGames);
    createColumn(PlayerStandings::getWinPercentage, createIconSpan(PERCENT_ICON, "Correct"))
        .setComparator(PlayerStandings::getWinPercentage);
    createColumn(PlayerStandings::getWeekPlayed, createIconSpan(WEEK_ICON, "W. Played"))
        .setComparator(PlayerStandings::getWeekPlayed);
    createColumn(PlayerStandings::getWins, createIconSpan(TROPHY_ICON, "Wins"))
        .setComparator(PlayerStandings::getWins);
    createColumn(PlayerStandings::getTotalWinnings, createIconSpan(MONEY_TYPE_ICON, "Winnings"))
        .setComparator(PlayerStandings::getTotalWinnings);
  }
}
