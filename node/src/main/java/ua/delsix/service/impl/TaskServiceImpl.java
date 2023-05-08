package ua.delsix.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.delsix.entity.Task;
import ua.delsix.repository.TaskRepository;
import ua.delsix.service.TaskService;
import ua.delsix.utils.UserUtils;

import java.time.LocalDate;
import java.util.Optional;

@Service
@Log4j
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
                .name("Unnamed " + (taskRepository.count()+1))
                .state("CREATING_NAME")
                .createdAt(LocalDate.now())
                .build();

        // saving the task to the table
        taskRepository.save(newTask);

        // sending a message back to MainService
        return "Alright, a new task. To proceed, let's choose a name for the task first.";
    }

    @Override
    public String processCreatingTask(Update update) {
        Optional<Task> taskOptional = taskRepository.findTopByUserIdOrderByIdDesc(userUtils.getUserByTag(update).getId());
        if(taskOptional.isEmpty()) {
            log.error("User does not have any tasks");
            return "Seems like there is an issue with the bot.\n\n Please, come back later";
        }
        Message userMessage = update.getMessage();
        Task task = taskOptional.get();
        String taskState = task.getState();

        // handling different task's states
        switch (taskState) {
            case "CREATING_NAME" -> {
                // checking if user's message has text, since he can send a picture of document
                if(!userMessage.hasText()) {
                    return "Please, send a text for the name of your task";
                }
                task.setName(userMessage.getText());
                taskRepository.save(task);
                return """
                        Now let's create a description for your task, if you want to.

                         If you don't need one -simply type in "Cancel" or press the cancel button""";

                //TODO change SendMessage keyboard markup
            }
        }

        return null;
    }

    @Override
    public String processEditingTask(Update update) {

        return null;
    }
}
