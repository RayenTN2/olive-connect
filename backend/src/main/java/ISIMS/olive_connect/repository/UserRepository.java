package ISIMS.olive_connect.repository;

import ISIMS.olive_connect.entity.AccountStatus;
import ISIMS.olive_connect.entity.Role;
import ISIMS.olive_connect.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // US-02: Fetch farmers who verified their email and are awaiting admin approval
    List<User> findByRoleAndEmailVerifiedTrueAndStatus(Role role, AccountStatus status);

    // US-05: Filter cooperative users by role and/or status
    List<User> findByRole(Role role);
    List<User> findByStatus(AccountStatus status);
    List<User> findByRoleAndStatus(Role role, AccountStatus status);
}