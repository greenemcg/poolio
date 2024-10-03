package nm.poolio.data;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public class AuditorAwareImpl implements AuditorAware<String> {
  @Override
  public Optional<String> getCurrentAuditor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String name = (authentication != null) ? authentication.getName() : "System";
    return Optional.of(name);
  }
}
