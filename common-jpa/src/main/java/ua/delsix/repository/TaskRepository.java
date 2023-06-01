package ua.delsix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ua.delsix.entity.Task;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Optional<Task> findTopByUserIdOrderByIdDesc(Long userId);
    List<Task> findAllByUserIdOrderByTargetDateDesc(Long userId);
    @Query("SELECT t FROM Task t ORDER BY t.targetDate, t.id ASC")
    List<Task> findAllByUserIdSortedByTargetDateAndIdAsc(Long userId);
}
