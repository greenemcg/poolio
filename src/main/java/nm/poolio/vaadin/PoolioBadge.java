package nm.poolio.vaadin;

import static nm.poolio.utils.VaddinUtils.WEEK_ICON;
import static nm.poolio.utils.VaddinUtils.createIconSpan;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasText.WhiteSpace;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.html.Span;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.enitities.ticket.Ticket;

public interface PoolioBadge extends PoolioAvatar {

  default Span createBadge(Span span) {
    span.getElement().getThemeList().add("badge");
    return span;
  }

  default void createPoolBadge(Pool pool, HasComponents layout) {
    layout.add(createPoolAvatar(pool, AvatarVariant.LUMO_SMALL));

    layout.add(
        createBadge(
            new Span(
                pool.getName() + " " + pool.getSeason().getDisplay() + " $" + pool.getAmount())));

    var weekSpan = createIconSpan(WEEK_ICON, " " + pool.getWeek());
    weekSpan.setWhiteSpace(WhiteSpace.PRE);
    layout.add(createBadge(weekSpan));
  }

  default void createTicketBadge(Ticket ticket, HasComponents layout) {
    layout.add(createUserAvatar(ticket.getPlayer(), AvatarVariant.LUMO_SMALL));
    // layout.add(createBadge(new Span(" " + ticket.getPlayer().getName())));
  }
}
