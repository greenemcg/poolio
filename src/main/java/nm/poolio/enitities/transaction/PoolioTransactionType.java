package nm.poolio.enitities.transaction;

public enum PoolioTransactionType {
    CASH_DEPOSIT,
    CASH_WITHDRAWAL,
    POOL_PURCHASE,
    GAME_BET_PROPOSAL,
    GAME_BET_PURCHASE,
    GAME_BET_REFUND,
    GAME_BET_WINNER,
    TIE_REFUND,
    POOL_WINNER,
    CANCEL_WAGER,
    ACCEPT_WAGER;

    public static PoolioTransactionType[] cashTypes() {
        return new PoolioTransactionType[]{CASH_DEPOSIT, CASH_WITHDRAWAL};
    }
}
