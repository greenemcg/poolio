package nm.poolio.views.result;

import static nm.poolio.utils.VaddinUtils.PLAYER_ICON;
import static nm.poolio.utils.VaddinUtils.RANK_ICON;
import static nm.poolio.utils.VaddinUtils.SCORE_ICON;
import static nm.poolio.utils.VaddinUtils.TIE_BREAKER_ICON;
import static nm.poolio.utils.VaddinUtils.createIconSpan;

import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import nm.poolio.enitities.ticket.Ticket;
import nm.poolio.vaadin.PoolioAvatar;
import nm.poolio.vaadin.PoolioGrid;

public interface ResultsGrid extends PoolioGrid<Ticket>, PoolioAvatar {
  Grid<Ticket> getResultsGrid();

  @Override
  default Grid<Ticket> getGrid() {
    return getResultsGrid();
  }

  private Renderer<Ticket> createUserRendererOld() {
    return LitRenderer.<Ticket>of(getUserTemplateExpression())
        .withProperty("pictureUrl", pojo -> createUserPictureUrl(pojo.getPlayer()))
        .withProperty("fullName", t -> t.getPlayer().getName())
        .withProperty("extraData", this::getWinningsString);
  }

  private String getWinningsString(Ticket t) {
    return "Winnings: 0";
  }

  default String createTieBreakerString(Ticket ticket) {
    return "" + ticket.getSheet().getTieBreaker();
  }

  default void decoratePoolGrid() {
    createColumn(Ticket::getRankString, createIconSpan(RANK_ICON, "Rank"));

    this.getResultsGrid()
        .addColumn(new ComponentRenderer<>(ticket -> createUserComponent(ticket.getPlayer())))
        .setHeader(createIconSpan(PLAYER_ICON, "PLayer"))
        .setAutoWidth(true)
        .setComparator(t -> t.getPlayer().getName());

    createColumn(Ticket::getScore, createIconSpan(SCORE_ICON, "Pts"))
        .setComparator(Ticket::getFullScore);

    //    createColumn(Ticket::getFullScore, createIconSpan(SCORE_ICON, "Full"))  // For Debug
    //            .setComparator(Ticket::getFullScore);

    this.getResultsGrid()
        .addColumn(new ComponentRenderer<>(ticket -> new Span(createTieBreakerString(ticket))))
        .setHeader(createIconSpan(TIE_BREAKER_ICON, "TB"))
        .setAutoWidth(true)
        .setTextAlign(ColumnTextAlign.CENTER)
        .setComparator(Ticket::getTieBreaker);
  }
}
