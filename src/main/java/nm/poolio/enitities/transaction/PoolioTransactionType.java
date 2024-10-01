package nm.poolio.enitities.transaction;

public enum PoolioTransactionType {
    CASH_DEPOSIT,
    CASH_WITHDRAWAL,
    POOL_PURCHASE,
    GAME_BET_PROPOSAL,
    GAME_BET_PURCHASE,
    POOL_WINNER,
    ACCEPT_PROPOSAL;

    public static PoolioTransactionType[] cashTypes() {
        return new PoolioTransactionType[]{CASH_DEPOSIT, CASH_WITHDRAWAL};
    }
}
