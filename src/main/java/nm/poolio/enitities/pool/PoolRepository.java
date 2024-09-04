package nm.poolio.enitities.pool;

import java.util.List;
import java.util.Optional;
import nm.poolio.data.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PoolRepository extends JpaRepository<Pool, Long>, JpaSpecificationExecutor<Pool> {

  List<Pool> findByPlayersAndInactiveDateIsNull(User player);

  Optional<Pool> findByPlayersAndIdAndInactiveDateIsNull(User player, Long id);

  List<PoolIdName> findIdAndNameByPlayersAndInactiveDateIsNull(User player);
}
