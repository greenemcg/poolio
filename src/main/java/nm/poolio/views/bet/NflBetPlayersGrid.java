package nm.poolio.views.bet;

import com.vaadin.flow.component.grid.Grid;
import nm.poolio.enitities.bet.GameBet;

public interface NflBetPlayersGrid extends NflBetGrid {
    Grid<GameBet> getPlayerBetGrid();

    @Override
    default Grid<GameBet> getGrid() {
        return getPlayerBetGrid();
    }

    default void decorateGrid() {
        decorateTransactionGrid(false);
    }
}
