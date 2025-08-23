package nm.poolio.views.standings;

import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import nm.poolio.vaadin.PoolioGrid;

import static nm.poolio.utils.VaddinUtils.*;

public interface StandingsGrid extends PoolioGrid<PlayerStandings> {
    default void decorateGrid() {
        this.getGrid()
                .addColumn(new ComponentRenderer<>(ticket -> createUserAvatar(ticket.getPlayer(), AvatarVariant.LUMO_XSMALL)))
                .setHeader("ICO")
                .setAutoWidth(true)
                .setFrozen(true);

        createColumn(PlayerStandings::findPlayerName, createIconSpan(PLAYER_ICON, "Player"));

        this.getGrid()
                .addColumn(new ComponentRenderer<>(ticket -> new Span(createCorrectString(ticket))))
                .setHeader(createIconSpan(GAMES_ICON, "Games"))
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.CENTER)
                .setComparator(PlayerStandings::getTotalCorrect);

        createColumn(PlayerStandings::getWeekPlayed, createIconSpan(WEEK_ICON, "Weeks"))
                .setComparator(PlayerStandings::getWeekPlayed);


        this.getGrid()
                .addColumn(new ComponentRenderer<>(ticket -> new Span(createTrophyString(ticket))))
                .setHeader(createIconSpan(TROPHY_ICON, "Wins"))
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.CENTER)
                .setComparator(PlayerStandings::getWins);

        createColumn(PlayerStandings::getWinnngsString, createIconSpan(MONEY_TYPE_ICON, "Winnings"))
                .setComparator(PlayerStandings::getTotalWinnings);
    }

    default String createTrophyString(PlayerStandings ticket) {
        return "\uD83C\uDFC6".repeat(ticket.getWins());
    }

    default String createCorrectString(PlayerStandings ticket) {
        return String.format("%d/%d (%s%%)", ticket.getTotalCorrect(), ticket.getTotalGames(), ticket.getWinPercentage());
    }
}