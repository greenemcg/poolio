package nm.poolio.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class CacheConfig {

  private static CacheManager cacheManager;

  @Bean
  public Caffeine<Object, Object> caffeineConfig() {
    return Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS);
  }

  @Bean
  public CacheManager cacheManager(Caffeine caffeine) {
    CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();

    var names = Arrays.stream(CacheName.values()).map(Enum::name).toList();

    caffeineCacheManager.setCacheNames(names);

    caffeineCacheManager.setCaffeine(caffeine);

    cacheManager = caffeineCacheManager;
    return caffeineCacheManager;
  }

  public Cache getCache(CacheName cacheName) {
    return cacheManager.getCache(cacheName.name());
  }

  public enum CacheName {
    SCORED_TICKETS,
    SCORED_WEEKLY_POOL_NFL_GAMES,
    SCORED_NFL_GAMES,
    USER_NAME,
    POOLS,
    IMAGE_BYTES
  }
}
