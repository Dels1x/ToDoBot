package ua.delsix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ua.delsix.entity.Task;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Optional<Task> findTopByUserIdOrderByIdDesc(Long userId);
    @Query("SELECT t FROM Task t WHERE t.userId = :userId ORDER BY t.status DESC, t.targetDate, t.priority ASC, t.id ASC")
    List<Task> findAll(@Param("userId") Long userId);
    @Query("SELECT t FROM Task t WHERE t.userId = :userId AND (t.tag = :tag OR (:tag IS NULL AND t.tag IS NULL)) ORDER BY t.status DESC, t.targetDate, t.priority ASC, t.id ASC")
    List<Task> findAllByTag(@Param("userId") Long userId, String tag);
    @Query("SELECT t FROM Task t WHERE t.userId = :userId ORDER BY t.tag ASC")
    List<Task> findAllSortedOnlyByTags(@Param("userId") Long userId);
    @Query("SELECT t FROM Task t WHERE t.userId = :userId AND t.targetDate = :today ORDER BY t.status DESC, t.priority ASC, t.id ASC")
    List<Task> findAllTasksDatedForToday(@Param("userId") Long userId,
                                         @Param("today") LocalDate today);
    @Query("SELECT t FROM Task t WHERE t.userId = :userId AND t.status = 'Completed' ORDER BY t.targetDate ASC, t.priority ASC, t.id ASC")
    List<Task> findAllCompletedTasks(@Param("userId") Long userId);
    @Query("SELECT t FROM Task t WHERE t.userId = :userId AND t.status = 'Uncompleted' ORDER BY t.targetDate ASC, t.priority ASC, t.id ASC")
    List<Task> findAllUncompletedTasks(@Param("userId") Long userId);
    @Modifying
    @Transactional
    @Query("DELETE FROM Task t WHERE t.userId = :userId AND t.status = 'Completed'")
    void deleteAllCompletedTasks(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Task t WHERE t.userId = :userId")
    void deleteAllTasks(@Param("userId") Long userId);
}
