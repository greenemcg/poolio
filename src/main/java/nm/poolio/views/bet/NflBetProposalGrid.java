package nm.poolio.views.bet;

import com.vaadin.flow.component.grid.Grid;
import nm.poolio.enitities.bet.GameBet;

public interface NflBetProposalGrid extends NflBetGrid {
    Grid<GameBet> getProposalBetGrid();

    @Override
    default Grid<GameBet> getGrid() {
        return getProposalBetGrid();
    }


    default void decorateGrid() {
        decorateTransactionGrid(true);
    }
}
