package ua.delsix.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;

@Getter
@Setter
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "tag", nullable = false)
    private String tag;

    @Column(name = "telegram_id", nullable = false)
    private Long telegramId;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @Column(name = "task_count", nullable = false)
    private Integer taskCount;

    @Column(name = "task_completed", nullable = false)
    private Integer taskCompleted;
}
