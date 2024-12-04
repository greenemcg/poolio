package nm.poolio.enitities.pool;

import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nm.poolio.cache.CacheConfig;
import nm.poolio.cache.CacheConfig.CacheName;
import nm.poolio.data.AvatarImageBytes;
import nm.poolio.data.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PoolService {
  private final PoolRepository repository;
  private final CacheConfig cacheConfig;
  @Getter private final BuildProperties buildProperties;

  @Value("${app.allowBets}")
  @Getter
  private Boolean allowBets;

  public List<Pool> findAll() {
    return repository.findAll();
  }

  @Cacheable(cacheNames = "POOLS", key = "{#id}")
  public Optional<Pool> get(Long id) {
    return repository.findById(id);
  }

  @CacheEvict(cacheNames = "POOLS", key = "{#p.id}")
  public Pool update(Pool p) {

    List.of(CacheName.USER_NAME, CacheName.POOLS).forEach(c -> cacheConfig.getCache(c).clear());

    return repository.save(p);
  }

  public Optional<AvatarImageBytes> getImageBytes(Long id) {
    var op = get(id);

    if (op.isPresent()) return Optional.of(op.get());
    else return Optional.empty();
  }

  //  public List<Pool> findPoolsForUser(User user) {
  //    return repository.findByPlayersAndInactiveDateIsNull(user);
  //  }

  public List<PoolIdName> findPoolIdNames(User user) {
    return repository.findIdAndNameByPlayersAndInactiveDateIsNull(user);
  }
}
