package nm.poolio.enitities.bet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import nm.poolio.enitities.transaction.PoolioTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class GameBetServiceTest {
  @Mock private GameBetRepository repository;
  @InjectMocks private GameBetService gameBetService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void findOpenBets_returnsAvailableBets() {
    GameBet bet1 = mock(GameBet.class);
    GameBet bet2 = mock(GameBet.class);
    when(repository.findByStatusAndExpiryDateAfterAndAcceptanceDateIsNullOrderByCreatedDate(
            any(), any(Instant.class)))
        .thenReturn(List.of(bet1, bet2));
    when(bet1.getBetCanBeSplit()).thenReturn(false);
    when(bet1.getAcceptorTransactions()).thenReturn(Set.of());
    when(bet2.getBetCanBeSplit()).thenReturn(true);
    when(bet2.getAcceptorTransactions()).thenReturn(Set.of(mock(PoolioTransaction.class)));
    when(bet2.getAmount()).thenReturn(100);
    when(bet2.getAcceptorTransactions().stream().mapToInt(PoolioTransaction::getAmount).sum())
        .thenReturn(50);

    List<GameBet> openBets = gameBetService.findAvailableBets();

    assertEquals(2, openBets.size());
    assertTrue(openBets.contains(bet1));
    assertTrue(openBets.contains(bet2));
  }

  @Test
  void findAvailableBets_excludesClosedBets() {
    GameBet bet1 = mock(GameBet.class);
    GameBet bet2 = mock(GameBet.class);
    when(repository.findByStatusAndExpiryDateAfterAndAcceptanceDateIsNullOrderByCreatedDate(
            any(), any(Instant.class)))
        .thenReturn(List.of(bet1, bet2));
    when(bet1.getBetCanBeSplit()).thenReturn(false);
    when(bet1.getAcceptorTransactions()).thenReturn(Set.of(mock(PoolioTransaction.class)));
    when(bet2.getBetCanBeSplit()).thenReturn(true);
    when(bet2.getAcceptorTransactions()).thenReturn(Set.of(mock(PoolioTransaction.class)));
    when(bet2.getAmount()).thenReturn(100);
    when(bet2.getAcceptorTransactions().stream().mapToInt(PoolioTransaction::getAmount).sum())
        .thenReturn(100);

    List<GameBet> openBets = gameBetService.findAvailableBets();

    assertEquals(0, openBets.size());
  }

  @Test
  void save_persistsGameBet() {
    GameBet bet = mock(GameBet.class);
    when(repository.save(bet)).thenReturn(bet);

    GameBet savedBet = gameBetService.save(bet);

    assertEquals(bet, savedBet);
    verify(repository, times(1)).save(bet);
  }
}
