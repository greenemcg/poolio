package nm.poolio.views.nfl_game;

import static nm.poolio.utils.VaddinUtils.*;

import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import java.time.format.DateTimeFormatter;
import nm.poolio.model.NflGame;
import nm.poolio.model.enums.NflWeek;
import nm.poolio.vaadin.PoolioAvatar;
import nm.poolio.vaadin.PoolioGrid;

public interface NflGameGrid extends PoolioGrid<NflGame>, PoolioAvatar {

  private String findByWeekNum(int weekNum) {
    var result = NflWeek.findByWeekNum(weekNum);

    return result == null ? "" : result.name();
  }

  default void decoratePoolGrid() {
    getGrid()
        .addColumn(new ComponentRenderer<>(game -> createTeamComponent(game.getAwayTeam(), game)))
        .setHeader(createIconSpan(AWAY_ICON, "Away"))
        .setAutoWidth(true)
        .setTextAlign(ColumnTextAlign.CENTER);

    createColumn(NflGame::getAwayScore, createIconSpan(AWAY_ICON, "Score"));

    getGrid()
        .addColumn(new ComponentRenderer<>(game -> createTeamComponent(game.getHomeTeam(), game)))
        .setHeader(createIconSpan(HOME_ICON, "Home"))
        .setAutoWidth(true)
        .setTextAlign(ColumnTextAlign.CENTER);

    createColumn(NflGame::getHomeScore, createIconSpan(HOME_ICON, "Score"));

    getGrid()
        .addColumn(new ComponentRenderer<>(game -> new Span(findByWeekNum(game.getWeek()))))
        .setHeader(createIconSpan(WEEK_ICON, "Week"))
        .setAutoWidth(true)
        .setTextAlign(ColumnTextAlign.CENTER);

    // This date formatter will show the day of week
    getGrid()
        .addColumn(
            new LocalDateTimeRenderer<>(
                NflGame::getLocalDateTime, () -> DateTimeFormatter.ofPattern("E, MMM d, h:mm a")))
        .setHeader(createIconSpan(GAME_TIME_ICON, "Game Time (EST)"))
        .setAutoWidth(true);

    // createColumn(NflGame::getFullId, createIconSpan(ID_ICON, "Id"));
  }
}
