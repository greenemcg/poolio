package nm.poolio.services.bets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nm.poolio.enitities.bet.GameBet;
import nm.poolio.enitities.bet.GameBetService;
import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import nm.poolio.model.enums.BetStatus;
import nm.poolio.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class NflOpenBetProcessorTest {

  @Mock private GameBetService gameBetService;

  @Mock private PoolioTransactionService poolioTransactionService;

  @Mock private AuthenticatedUser authenticatedUser;

  @InjectMocks private NflOpenBetProcessor nflOpenBetProcessor;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void process_withOpenAndNotExpiredBet_doesNothing() {
    GameBet openBet = mock(GameBet.class);
    when(openBet.getBetCanBeSplit()).thenReturn(true);
    when(openBet.getAmount()).thenReturn(100);
    when(openBet.getAcceptorTransactions()).thenReturn(Set.of());
    when(openBet.getExpiryDate()).thenReturn(Instant.now().plusSeconds(3600));
    when(openBet.getModifiedDate()).thenReturn(Instant.now().minusSeconds(1000));
    when(gameBetService.findOpenBets()).thenReturn(List.of(openBet));

    nflOpenBetProcessor.process();

    verify(gameBetService, never()).save(any());
  }

  @Test
  void process_withOpenAndExpiredBet_refundsAndSetsStatus() {
    GameBet expiredBet = mock(GameBet.class);
    when(expiredBet.getBetCanBeSplit()).thenReturn(true);
    when(expiredBet.getAmount()).thenReturn(100);
    when(expiredBet.getAcceptorTransactions()).thenReturn(Set.of());
    when(expiredBet.getExpiryDate()).thenReturn(Instant.now().minusSeconds(3600));
    when(expiredBet.getModifiedDate()).thenReturn(Instant.now().minusSeconds(1000));

    when(expiredBet.getProposerTransaction()).thenReturn(mock(PoolioTransaction.class));

    Set<PoolioTransaction> resultTransactions = new HashSet<>();
    when(expiredBet.getResultTransactions()).thenReturn(resultTransactions);
    when(gameBetService.findOpenBets()).thenReturn(List.of(expiredBet));

    PoolioTransaction refundTransaction = new PoolioTransaction();
    when(poolioTransactionService.save(any())).thenReturn(refundTransaction);

    nflOpenBetProcessor.process();

    verify(expiredBet).setStatus(BetStatus.CLOSED);
    verify(gameBetService).save(expiredBet);
  }

  @Test
  void process_withAcceptedBet_setsStatusToPending() {
    GameBet acceptedBet = mock(GameBet.class);
    when(acceptedBet.getBetCanBeSplit()).thenReturn(false);
    when(acceptedBet.getAcceptorTransactions()).thenReturn(Set.of(mock(PoolioTransaction.class)));
    when(acceptedBet.getExpiryDate()).thenReturn(Instant.now().minusSeconds(3600));
    when(acceptedBet.getModifiedDate()).thenReturn(Instant.now().minusSeconds(1000));
    when(gameBetService.findOpenBets()).thenReturn(List.of(acceptedBet));

    nflOpenBetProcessor.process();

    verify(acceptedBet).setStatus(BetStatus.PENDING);
    verify(gameBetService).save(acceptedBet);
  }

  @Test
  void process_mike() {
    PoolioTransaction acceptorTransaction1 = mock(PoolioTransaction.class);
    when(acceptorTransaction1.getAmount()).thenReturn(50);


    PoolioTransaction acceptorTransaction2 = mock(PoolioTransaction.class);
    when(acceptorTransaction2.getAmount()).thenReturn(50);


    GameBet expiredBet = mock(GameBet.class);
    when(expiredBet.getBetCanBeSplit()).thenReturn(true);
    when(expiredBet.getAmount()).thenReturn(100);
    when(expiredBet.getAcceptorTransactions()).thenReturn(Set.of(acceptorTransaction1, acceptorTransaction2));
    when(expiredBet.getExpiryDate()).thenReturn(Instant.now().minusSeconds(3600));
    when(expiredBet.getModifiedDate()).thenReturn(Instant.now().minusSeconds(1000));
    when(expiredBet.getProposerTransaction()).thenReturn(mock(PoolioTransaction.class));

    Set<PoolioTransaction> resultTransactions = new HashSet<>();
    when(expiredBet.getResultTransactions()).thenReturn(resultTransactions);
    when(gameBetService.findOpenBets()).thenReturn(List.of(expiredBet));

    PoolioTransaction refundTransaction = new PoolioTransaction();
    when(poolioTransactionService.save(any())).thenReturn(refundTransaction);

    nflOpenBetProcessor.process();

    assertEquals(0, resultTransactions.size());

//    verify(expiredBet).setStatus(BetStatus.CLOSED);
//    verify(gameBetService).save(expiredBet);
  }


}
