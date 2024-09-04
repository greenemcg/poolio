package nm.poolio.services;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.cache.CacheConfig;
import nm.poolio.cache.CacheConfig.CacheName;
import nm.poolio.data.Role;
import nm.poolio.data.User;
import nm.poolio.data.UserName;
import nm.poolio.data.UserRepository;
import nm.poolio.enitities.pool.PoolService;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
  private final UserRepository repository;
  private final PoolService poolService;
  private final CacheConfig cacheConfig;
  private final PoolioTransactionService poolioTransactionService;

  public Optional<User> get(Long id) {
    return repository.findById(id);
  }

  public User getCashUser() {
    return get(0L).orElseThrow();
  }

  public User update(User entity) {
    entity.setUserName(entity.getUserName().toLowerCase().trim());
    var result = repository.save(entity);

    cacheConfig.getCache(CacheName.USER_NAME).invalidate();

    return result;
  }

  public Page<User> list(Pageable pageable) {
    return repository.findAll(pageable);
  }

  public Page<User> list(Pageable pageable, Specification<User> filter) {
    return repository.findAll(filter, pageable);
  }

  @Cacheable(cacheNames = "USER_NAME", key = "{#userName}")
  @SneakyThrows
  public @Nullable User findByUserName(String userName) {

    User user = repository.findByUserName(userName.toLowerCase().trim());

    if (user != null) {
      if (user.getInactiveDate() != null) {
        log.warn("User: {} is inactive and attempting to login", user.getUserName());
        return null;
      } else {
        addPoolIdNames(user);
        addFunds(user);
      }
    } else log.info("User: {} Not found", userName);

    return user;
  }

  public User addPoolIdNames(User user) {
    user.setPoolIdNames(poolService.findPoolIdNames(user));
    return user;
  }

  public List<User> findAll() {
    return repository.findAll().stream()
        .filter(u -> u.getInactiveDate() == null)
        .map(this::addPoolIdNames)
        .map(this::addFunds)
        .sorted(Comparator.comparing(User::getModifiedSortTime).reversed())
        .toList();
  }

  private User addFunds(User u) {
    u.setFunds(poolioTransactionService.getFunds(u));
    return u;
  }

  public int count() {
    return (int) repository.count();
  }

  public List<User> findUsersWithNoRoles() {
    return findAll().stream()
        .filter(u -> u.getId() > 0)
        .filter(u -> u.getRoles().isEmpty())
        .sorted(Comparator.comparing(User::getName))
        .toList();
  }

  public List<User> findUsersWithRoleUser() {
    return findAll().stream().filter(u -> u.getRoles().contains(Role.USER)).toList();
  }

  private boolean isAdmin(User u) {
    return u.getRoles().contains(Role.ADMIN);
  }

  public List<User> findAdmins() {
    return findAll().stream().filter(this::isAdmin).toList();
  }

  public List<User> findPlayers() {
    return findAll().stream()
        .filter(u -> !u.getRoles().isEmpty())
        .filter(u -> u.getRoles().contains(Role.USER))
        .toList();
  }

  public boolean checkUserName(String userName, @Nullable Long id) {

    if (id == null) return repository.findByUserName(userName) == null;
    else {
      var user = repository.findByUserName(userName);

      if (user == null) return true;
      else return Objects.equals(user.getId(), id);
    }
  }

  public boolean checkName(String name, @Nullable Long id) {
    if (id == null) return repository.findByName(name) == null;
    else {
      return checkNameInDb(name, id);
    }
  }

  private boolean checkNameInDb(String name, Long id) {
    var user = repository.findByName(name);

    if (user == null) return true;
    else return Objects.equals(user.getId(), id);
  }

  public @Nullable String findUserName(Long id) {
    var optional = repository.findUserNameById(id);
    return optional.map(UserName::getUserName).orElse(null);
  }
}
