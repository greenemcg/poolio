package nm.poolio.enitities.score;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GameScoreRepository
    extends JpaRepository<GameScore, Long>, JpaSpecificationExecutor<GameScore> {

  Optional<GameScore> findByGameId(String gameId);
}
