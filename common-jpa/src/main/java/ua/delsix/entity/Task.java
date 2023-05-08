package ua.delsix.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "task")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private String status;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "difficulty")
    private Integer difficulty;

    @Column(name = "tag")
    private String tag;

    @Column(name="parent_task")
    private Integer parentTask;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(name = "state")
    private String state;
}
