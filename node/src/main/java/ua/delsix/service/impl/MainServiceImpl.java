package ua.delsix.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.delsix.entity.Task;
import ua.delsix.entity.User;
import ua.delsix.repository.TaskRepository;
import ua.delsix.service.MainService;
import ua.delsix.service.ProducerService;
import ua.delsix.service.TaskService;
import ua.delsix.service.enums.ServiceCommand;
import ua.delsix.utils.MessageUtils;
import ua.delsix.utils.UserUtils;

import java.util.Optional;

@Service
@Log4j
public class MainServiceImpl implements MainService {

    private final ProducerService producerService;
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final UserUtils userUtils;

    public MainServiceImpl(ProducerService producerService, TaskService taskService, TaskRepository taskRepository, UserUtils userUtils) {
        this.producerService = producerService;
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.userUtils = userUtils;
    }

    @Override
    public void processMessage(Update update) {
        String messageText = update.getMessage().getText();
        ServiceCommand userCommand = ServiceCommand.fromValue(messageText);
        String answerText = "";

        log.debug("User command: " + userCommand);

        User user = userUtils.getUserByTag(update);
        log.trace("User: " + user.toString());

        if (userCommand == null) {
            userCommand = ServiceCommand.nonCommand;
        }

        switch (userCommand) {
            case help -> {
                answerText = """
                        Available commands:

                        - not yet""";
            }
            case start -> {
                answerText = """
                        Welcome to the delsix's Task Manager Bot!
                                                
                        Type \"/help\" to see all available commands.""";
            }
            case createTask -> {
                answerText = taskService.processCreateTask(update);
            }
            case cancel -> {
                //TODO provide logic for cancelling different command, you need to check last user's task's state
            }
            default -> {
                // get task to determine what user tries to achieve by user's last task's state
                Optional<Task> lastTask = taskRepository.findTopByUserIdOrderByIdDesc(userUtils.getUserByTag(update).getId());
                if(lastTask.isPresent()) {
                    String taskState = lastTask.get().getState();
                    log.trace("User's last task state: "+taskState);

                    // process further creation/editing of a task in TaskService
                    if(taskState.startsWith("CREAT")) {
                        answerText = taskService.processCreatingTask(update);
                    } else if(taskState.startsWith("EDIT")) {
                        answerText = taskService.processEditingTask(update);
                    } else {
                        answerText = "Unknown command";
                    }
                } else {
                    log.trace("User doesn't have any tasks");
                    answerText = "Unknown command";
                }
            }
        }

        SendMessage answerMessage = MessageUtils.sendMessageGenerator(update, answerText);
        producerService.ProduceAnswer(answerMessage);
    }
}
