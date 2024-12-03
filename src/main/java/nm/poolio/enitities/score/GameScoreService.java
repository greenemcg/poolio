package nm.poolio.enitities.score;

import lombok.RequiredArgsConstructor;
import nm.poolio.cache.CacheConfig;
import nm.poolio.cache.CacheConfig.CacheName;
import nm.poolio.model.NflGame;
import org.springframework.stereotype.Service;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameScoreService implements Serializable {
    @Serial
    private static final long serialVersionUID = 1154614513034023197L;
    private final GameScoreRepository repository;
    private final CacheConfig cacheConfig;

    public Optional<GameScore> findScore(String gameId) {
        return repository.findByGameId(gameId);
    }

    public Map<String, GameScore> getScores(List<NflGame> games) {
        return games.stream()
                .map(g -> findScore(g.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(GameScore::getGameId, Function.identity()));
    }

    public GameScore save(GameScore score) {
        var result = repository.save(score);

        List.of(
                        CacheName.SCORED_WEEKLY_POOL_NFL_GAMES,
                        CacheName.SCORED_NFL_GAMES,
                        CacheName.SCORED_TICKETS)
                .forEach(c -> cacheConfig.getCache(c).clear());

        return result;
    }
}
