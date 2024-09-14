package nm.poolio.data;

import java.util.Optional;
import javax.annotation.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
  @Nullable
  User findByUserName(String userName);

  @Nullable
  User findByName(String name);

  Optional<UserName> findUserNameById(Long id);
}
