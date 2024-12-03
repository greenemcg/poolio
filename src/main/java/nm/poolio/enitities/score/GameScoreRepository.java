package nm.poolio.enitities.score;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface GameScoreRepository
        extends JpaRepository<GameScore, Long>, JpaSpecificationExecutor<GameScore> {

    Optional<GameScore> findByGameId(String gameId);
}
