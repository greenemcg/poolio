package nm.poolio.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@EnableCaching
@Configuration
public class CacheConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = 1011954453227284372L;

    private static CacheManager cacheManager;

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS);
    }

    @Bean
    public CacheManager cacheManager(Caffeine caffeine) {
        CaffeineCacheManager caffeineCacheManager = getCaffeineCacheManager(caffeine);

        cacheManager = caffeineCacheManager;
        return cacheManager;
    }

    private CaffeineCacheManager getCaffeineCacheManager(Caffeine caffeine) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();

        var names = Arrays.stream(CacheName.values()).map(Enum::name).toList();

        caffeineCacheManager.setCacheNames(names);

        caffeineCacheManager.setCaffeine(caffeine);
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
