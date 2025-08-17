package nm.poolio.services.bets;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NflBetCronJob {
  private final NflOpenBetProcessor nflOpenBetProcessor;
  private final NflPendingBetProcessor nflPendingBetProcessor;

  @Scheduled(cron = "0 */1 * * * *")
  void cleanBets() {
    nflOpenBetProcessor.process();
    nflPendingBetProcessor.process();
  }
}

