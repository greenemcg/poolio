package nm.poolio.enitities.transaction;

public enum PoolioTransactionType {
  CASH_DEPOSIT,
  CASH_WITHDRAWAL,
  POOL_PURCHASE,
  POOL_WINNER;

  public static PoolioTransactionType[] cashTypes() {
    return new PoolioTransactionType[] {CASH_DEPOSIT, CASH_WITHDRAWAL};
  }
}
