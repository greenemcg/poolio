package nm.poolio.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import javax.annotation.Nullable;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    @Nullable
    User findByUserName(String userName);

    @Nullable
    User findByName(String name);

    Optional<UserName> findUserNameById(Long id);
}
