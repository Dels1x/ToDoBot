package ua.delsix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.delsix.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByTag(String tag);
}
