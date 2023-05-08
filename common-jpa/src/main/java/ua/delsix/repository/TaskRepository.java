package ua.delsix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.delsix.entity.Task;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Optional<Task> findTopByUserIdOrderByIdDesc(Long userId);
}
