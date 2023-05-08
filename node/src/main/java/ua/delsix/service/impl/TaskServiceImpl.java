package ua.delsix.service.impl;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.delsix.entity.Task;
import ua.delsix.repository.TaskRepository;
import ua.delsix.service.TaskService;
import ua.delsix.utils.UserUtils;

import java.time.LocalDate;

@Service
public class TaskServiceImpl implements TaskService {
    private final UserUtils userUtils;
    private final TaskRepository taskRepository;

    public TaskServiceImpl(UserUtils userUtils, TaskRepository taskRepository) {
        this.userUtils = userUtils;
        this.taskRepository = taskRepository;
    }

    @Override
    public String processCreateTask(Update update) {
        // creating a new task
        Task newTask = Task.builder()
                .userId(userUtils.getUserByTag(update).getId())
                .name("Unnamed " + taskRepository.count()+1)
                .state("CREATING_NAME")
                .createdAt(LocalDate.now())
                .build();

        // saving the task to the table
        taskRepository.save(newTask);

        // sending a message back to MainService
        return "Alright, a new task. To proceed, let's choose a name for the task first.";
    }

    @Override
    public String processTask(Update update) {

        return null;
    }
}
