package nm.poolio.views.ticket;

import static nm.poolio.utils.VaddinUtils.*;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import java.time.format.DateTimeFormatter;
import nm.poolio.data.AbstractEntity;
import nm.poolio.enitities.ticket.Ticket;
import nm.poolio.vaadin.PoolioGrid;
import org.apache.commons.lang3.ArrayUtils;

public interface TicketGrid extends PoolioGrid<Ticket> {

  private static String getPoolTemplateExpression() {
    return """
                <vaadin-horizontal-layout style="align-items: center;" theme="spacing">
                <vaadin-avatar img="${item.pictureUrl}" name="${item.fullName}" alt="User avatar"></vaadin-avatar>
                  <vaadin-vertical-layout style="line-height: var(--lumo-line-height-m);">
                    <span> ${item.name} </span>
                    <span style="font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);">
                      ${item.season}
                    </span>
                  </vaadin-vertical-layout>
                </vaadin-horizontal-layout>
                """;
  }

  void viewTicket(Long ticketId);

  private Renderer<Ticket> createUserRenderer() {
    return LitRenderer.<Ticket>of(getUserTemplateExpression())
        .withProperty("pictureUrl", pojo -> createUserPictureUrl(pojo.getPlayer()))
        .withProperty("fullName", t -> t.getPlayer().getName());
  }

  default Renderer<Ticket> createTicketPoolRenderer() {
    return LitRenderer.<Ticket>of(getPoolTemplateExpression())
        .withProperty(
            "pictureUrl",
            pojo ->
                (ArrayUtils.isEmpty(pojo.getPool().getImageResource()))
                    ? ""
                    : "/image?type=pool&id=" + pojo.getPool().getId())
        .withProperty("name", pojo -> (pojo.getPool().getName()))
        .withProperty("season", pojo -> (pojo.getPool().getSeason().getDisplay()));
  }

  default void decorateTicketGrid() {
    getGrid()
        .addColumn(
            new ComponentRenderer<>(
                ticket -> {
                  Button button = new Button(new Icon(VaadinIcon.EYE));
                  button.addThemeVariants(ButtonVariant.LUMO_ICON);
                  button.addClickListener(event -> viewTicket(ticket.getId()));
                  button.setAriaLabel("View Ticket");
                  return button;
                }))
        .setHeader(createIconSpan(VIEW_ICON, "View"))
        .setTextAlign(ColumnTextAlign.CENTER)
        .setAutoWidth(true);

    getGrid()
        .addColumn(createTicketPoolRenderer())
        .setHeader(createIconSpan(POOL_ICON, "Pool"))
        .setAutoWidth(true)
        .setComparator(t -> t.getPool().getName());

    getGrid()
        .addColumn(new ComponentRenderer<>(ticket -> createUserComponent(ticket.getPlayer())))
        .setHeader(createIconSpan(PLAYER_ICON, "PLayer"))
        .setAutoWidth(true)
        .setComparator(t -> t.getPlayer().getName());

    getGrid()
        .addColumn(getRenderer())
        .setHeader(createIconSpan(CREATED_ICON, "Created (EST)"))
        .setAutoWidth(true)
        .setComparator(AbstractEntity::getCreatedDate);

    createColumn(Ticket::getWeek, createIconSpan(WEEK_ICON, "Week"))
        .setComparator(t -> t.getWeek().getWeekNum());

    createColumn(Ticket::getTieBreaker, createIconSpan(TIE_BREAKER_ICON, "Tie Break"))
        .setComparator(Ticket::getTieBreaker);
    createColumn(Ticket::getPicksString, createIconSpan(TICKET_ICON, "Picks"))
        .setAutoWidth(true)
        // .setTooltipGenerator(Ticket::getPicksString)
        .setTextAlign(ColumnTextAlign.START);
  }

  private LocalDateTimeRenderer<Ticket> getRenderer() {
    return new LocalDateTimeRenderer<>(
        Ticket::getCreatedLocalDateTime, () -> DateTimeFormatter.ofPattern("MMM d, h:mm a"));
  }
}
