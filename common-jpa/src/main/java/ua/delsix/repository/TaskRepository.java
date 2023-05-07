package ua.delsix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.delsix.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
