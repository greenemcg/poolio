package nm.poolio.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NflBetCleaner {


    @Scheduled(cron = "0 */5 * * * *")
    public void cleanBets() {
        log.info("Cleaning up bets");
    }

}
