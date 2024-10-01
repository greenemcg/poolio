package nm.poolio.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.enitities.score.GameScoreService;
import nm.poolio.model.NflGame;
import nm.poolio.model.enums.NflWeek;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NflGameScorerService {
    private final NflGameService service;
    private final GameScoreService gameScoreService;

    @Cacheable(cacheNames = "SCORED_WEEKLY_POOL_NFL_GAMES", key = "{#pool.id, #week.weekNum}")
    public List<NflGame> getWeeklyGamesForPool(Pool pool, NflWeek week) {
        var weeklyGames = service.getGamesForPool(pool, week);
        weeklyGames.forEach(this::addScore);

        return weeklyGames;
    }

    @Cacheable(cacheNames = "SCORED_NFL_GAMES")
    public List<NflGame> getAllGames() {
        service.gameList.parallelStream().forEach(this::addScore);
        return service.gameList;
    }

    private void addScore(NflGame g) {
        service.addScores(g);
    }
}
