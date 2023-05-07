package ua.delsix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.delsix.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
