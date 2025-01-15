package nm.poolio.views.result;

import static nm.poolio.utils.VaddinUtils.*;

import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import nm.poolio.enitities.ticket.Ticket;
import nm.poolio.vaadin.PoolioAvatar;
import nm.poolio.vaadin.PoolioGrid;

public interface ResultsGrid extends PoolioGrid<Ticket>, PoolioAvatar {
  Grid<Ticket> getResultsGrid();

  @Override
  default Grid<Ticket> getGrid() {
    return getResultsGrid();
  }

  default String createTieBreakerString(Ticket ticket) {
    return String.valueOf(ticket.getSheet().getTieBreaker());
  }

  default void decoratePoolGrid() {
    getResultsGrid()
            .addColumn(new ComponentRenderer<>(ticket -> createUserAvatar(ticket.getPlayer(), AvatarVariant.LUMO_XSMALL)))
            .setHeader("ICO")
            .setAutoWidth(true)
            .setFrozen(true);

    createColumn(Ticket::getRankString, createIconSpan(RANK_ICON, "Rank"));
    createColumn(Ticket::findPlayerName, createIconSpan(PLAYER_ICON, "Player"));
    createColumn(Ticket::getScore, createIconSpan(SCORE_ICON, "Pts"))
            .setComparator(Ticket::getFullScore);

    getResultsGrid()
            .addColumn(new ComponentRenderer<>(ticket -> new Span(createTieBreakerString(ticket))))
            .setHeader(createIconSpan(TIE_BREAKER_ICON, "TB"))
            .setAutoWidth(true)
            .setTextAlign(ColumnTextAlign.CENTER)
            .setComparator(Ticket::getTieBreaker);
  }
}