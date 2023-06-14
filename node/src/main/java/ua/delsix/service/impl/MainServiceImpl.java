package ua.delsix.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
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

import java.util.Arrays;
import java.util.List;
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
    public void processUpdate(Update update) {
        SendMessage answerMessage = null;
        if (update.hasMessage()) {
            answerMessage = processMessage(update);
        } else if (update.hasCallbackQuery()) {
            processCallbackQuery(update);
            return;
        } else {
            log.error("Update has neither message nor callback query");
        }

        producerService.produceAnswer(answerMessage);
    }

    private SendMessage processMessage(Update update) {
        String messageText = update.getMessage().getText();
        ServiceCommand userCommand = ServiceCommand.fromValue(messageText);

        return processUserMessage(update, userCommand);
    }

    private void processCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");
        EditMessageText answer = MessageUtils.editMessageGenerator(update, "Unknown error. Please contact developer using his telegram (@dels1x).");
        String operation = callbackData[0];

        log.trace("CallbackData: "+ Arrays.toString(callbackData));

        // get answer based on callbackQuery
        if (operation.startsWith("GET")) {
            switch (callbackData[1]) {
                case "NEXT" -> answer = taskService.processGetAllTasksNext(update, operation);
                case "PREV" -> answer = taskService.processGetAllTasksPrev(update, operation);
                case "TASK" -> answer = taskService.processGetTaskInDetail(update, operation);
            }
        } else if (operation.startsWith("DELETE_ALL_COMPLETED")) {
            if (callbackData[1].equals("CONFIRM")) {
                answer = taskService.processDeleteAllCompletedTasks(update);
            }
        }

        if(answer == null) {
            log.error("answer is null");
            return;
        }

        producerService.produceAnswer(answer);
    }

    private SendMessage processUserMessage(Update update, ServiceCommand userCommand) {
        String answerText = "";
        SendMessage answerMessage = MessageUtils.sendMessageGenerator(update, "");
        User user = userUtils.getUserByTag(update);

        if (userCommand == null) {
            userCommand = ServiceCommand.NON_COMMAND;
        }

        log.debug("User command: " + userCommand);
        switch (userCommand) {
            case HELP -> {
                answerText = """
                        Available commands:

                        _/tasks_ - see all of your tasks (and you can edit/remove/complete them using this command)
                        _/create_ - Create task
                        _/uncompleted _ - see all of your uncompleted tasks
                        _/completed _ - see all of your completed tasks
                        _/today _ - see all of your tasks dated for today
                        _/clearCompleted_ - delete all your completed tasks (W.I.P)
                        _/clearAll_ - delete all your tasks (W.I.P)""";
                answerMessage.setText(answerText);
            }
            case START -> {
                answerText = """
                        Welcome to the delsix's Task Manager Bot!
                                                
                        Type "help" to see all available commands.""";
                answerMessage.setText(answerText);
            }
            case CREATE_TASK -> answerMessage = taskService.processCreateTask(update);
            case TASKS -> answerMessage = taskService.processGetAllTasks(update, "GET_ALL_TASKS");
            case TODAY_TASKS -> answerMessage = taskService.processGetAllTasks(update, "GET_TODAY_TASKS");
            case COMPLETED_TASKS -> answerMessage = taskService.processGetAllTasks(update, "GET_COMPLETED_TASKS");
            case UNCOMPLETED_TASKS -> answerMessage = taskService.processGetAllTasks(update, "GET_UNCOMPLETED_TASKS");
            case DELETE_COMPLETED_TASKS -> answerMessage = taskService.processDeleteAllCompletedTasksConfirmation(update);
            default -> {
                //TODO handle editing
                answerMessage.setText(answerText);
                // get task to determine what user tries to achieve by user's last task's state
                Optional<Task> lastTask = taskRepository.findTopByUserIdOrderByIdDesc(user.getId());
                if(lastTask.isPresent()) {
                    String taskState = lastTask.get().getState();
                    log.trace("User's last task state: "+taskState);

                    // process further creation/editing of a task in TaskService
                    if(taskState.startsWith("CREAT")) {
                        answerMessage = taskService.processCreatingTask(update);
                    } else {
                        List<Task> tasks = taskRepository.findAll(user.getId());

                        Optional<Task> editTaskOptional = tasks.stream()
                                .filter(task -> task.getState().startsWith("EDIT"))
                                .findFirst();

                        if(editTaskOptional.isPresent()) {
                            answerMessage = taskService.editTask(update, editTaskOptional.get());
                        } else {
                            answerText = "Unknown command";
                            answerMessage.setText(answerText);
                        }
                    }
                } else {
                    log.trace("User doesn't have any tasks");
                    answerText = "Unknown command";
                    answerMessage.setText(answerText);
                }
            }
        }

        return answerMessage;
    }
}
