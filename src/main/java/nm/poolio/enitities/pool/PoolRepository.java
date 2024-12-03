package nm.poolio.enitities.pool;

import nm.poolio.data.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface PoolRepository extends JpaRepository<Pool, Long>, JpaSpecificationExecutor<Pool> {

    List<Pool> findByPlayersAndInactiveDateIsNull(User player);

    Optional<Pool> findByPlayersAndIdAndInactiveDateIsNull(User player, Long id);

    List<PoolIdName> findIdAndNameByPlayersAndInactiveDateIsNull(User player);
}
