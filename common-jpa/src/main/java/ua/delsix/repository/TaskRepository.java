package ua.delsix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ua.delsix.entity.Task;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Optional<Task> findTopByUserIdOrderByIdDesc(Long userId);
    @Query("SELECT t FROM Task t WHERE t.userId = :userId ORDER BY t.targetDate, t.id ASC")
    List<Task> findAllByUserIdSortedByTargetDateAndIdAsc(Long userId);
    @Query("SELECT t FROM Task t WHERE t.userId = :userId ORDER BY t.status DESC, t.targetDate, t.id ASC")
    List<Task> findAllByUserIdSortedByTargetStatusAndDateAndId(Long userId);
    @Query("SELECT t FROM Task t WHERE t.userId = :userId AND t.targetDate = :today")
    List<Task> findTasksDatedForToday(Long userId, LocalDate today);
}
