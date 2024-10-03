package nm.poolio.views.bet;

import static nm.poolio.utils.VaddinUtils.*;

import com.vaadin.flow.data.renderer.ComponentRenderer;
import nm.poolio.enitities.bet.GameBet;
import nm.poolio.vaadin.PoolioGrid;
import org.vaadin.lineawesome.LineAwesomeIcon;

public interface NflBetGrid extends PoolioGrid<GameBet> {
  default void decorateTransactionGrid() {
    getGrid()
        .addColumn(
            new ComponentRenderer<>(transaction -> createUserComponent(transaction.getProposer())))
        .setHeader(createIconSpan(USER_ICON, "Credit", LineAwesomeIcon.MINUS_SOLID))
        .setAutoWidth(true)
        .setComparator(t -> t.getProposer().getName());

    createColumn(GameBet::getAmount, createIconSpan(AMOUNT_ICON, "Amt"))
        .setComparator(GameBet::getAmount);

    createColumn(GameBet::getSpread, createIconSpan(SPREAD_ICON, "Spread"))
        .setComparator(GameBet::getSpread);
  }
}
