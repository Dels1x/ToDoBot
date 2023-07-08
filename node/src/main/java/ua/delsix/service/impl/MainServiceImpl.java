package ua.delsix.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.delsix.entity.Task;
import ua.delsix.entity.User;
import ua.delsix.manager.LanguageManager;
import ua.delsix.repository.TaskRepository;
import ua.delsix.service.MainService;
import ua.delsix.service.ProducerService;
import ua.delsix.service.TaskService;
import ua.delsix.enums.ServiceCommand;
import ua.delsix.service.SettingsService;
import ua.delsix.utils.CallbackQueryUtils;
import ua.delsix.utils.MessageUtils;
import ua.delsix.utils.UserUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Log4j
public class MainServiceImpl implements MainService {
    private final LanguageManager languageManager;

    private final ProducerService producerService;
    private final SettingsService settingsService;
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final MessageUtils messageUtils;
    private final UserUtils userUtils;

    public MainServiceImpl(LanguageManager languageManager, ProducerService producerService, SettingsService settingsService, TaskService taskService, TaskRepository taskRepository, MessageUtils messageUtils, UserUtils userUtils) {
        this.languageManager = languageManager;
        this.producerService = producerService;
        this.settingsService = settingsService;
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.messageUtils = messageUtils;
        this.userUtils = userUtils;
    }

    @Override
    public void processUpdate(Update update) {
        if (update.hasMessage()) {
            SendMessage answerMessage = processMessage(update);
            producerService.produceAnswer(answerMessage, update);
        } else if (update.hasCallbackQuery()) {
            processCallbackQuery(update);
        } else {
            log.error("Update has neither message nor callback query");
            throw new IllegalStateException("Update has neither message nor callback query");
        }
    }

    private SendMessage processMessage(Update update) {
        String messageText = update.getMessage().getText();
        ServiceCommand userCommand = ServiceCommand.fromValue(messageText);

        return processUserMessage(update, userCommand);
    }

    private void processCallbackQuery(Update update) {
        String[] callbackData = CallbackQueryUtils.getCallbackData(update);
        String operation = callbackData[0];
        EditMessageText answer = messageUtils.generateEditMessage(update, "Unknown error");

        log.trace("CallbackData: " + Arrays.toString(callbackData));

        // get answer based on callbackQuery
        if (operation.startsWith("GET")) {
            answer = processGetOperation(update);
        } else if (operation.startsWith("DELETE")) {
            answer = processDeleteOperation(update);
        } else if (operation.startsWith("SETTINGS")) {
            answer = processSettingsOperation(update);
        }

        if (answer == null) {
            log.error("answer is null");
            answer = messageUtils.generateErrorEditMessage(update);
        }

        producerService.produceAnswer(answer);
    }

    private EditMessageText processSettingsOperation(Update update) {
        String[] callbackData = CallbackQueryUtils.getCallbackData(update);
        String operation = callbackData[0];

        if (callbackData[1].equals("LANGUAGE")) {
            if (callbackData.length == 2) {
                return settingsService.processLanguage(update);
            } else {
                if (callbackData[2].equals("SET")) {
                    settingsService.setLanguage(update);
                }
            }
        }

        log.error("Unexpected value: " + operation);
        return messageUtils.generateErrorEditMessage(update);
    }

    private EditMessageText processDeleteOperation(Update update) {
        String[] callbackData = CallbackQueryUtils.getCallbackData(update);
        String operation = callbackData[0];

        if (operation.startsWith("DELETE_ALL_COMPLETED")) {
            if (callbackData[1].equals("CONFIRM")) {
                return taskService.processDeleteAllCompletedTasks(update);
            }
        } else if (operation.startsWith("DELETE_ALL")) {
            if (callbackData[1].equals("CONFIRM")) {
                return taskService.processDeleteAllTasks(update);
            }
        }

        log.error("Unexpected value: " + operation);
        return messageUtils.generateErrorEditMessage(update);
    }

    private EditMessageText processGetOperation(Update update) {
        String[] callbackData = CallbackQueryUtils.getCallbackData(update);
        String operation = callbackData[0];

        if (operation.equals("GET_TAGS")) {
            return processGetTagsOperation(update);
        } else if (operation.equals("GET_TASKS") || operation.startsWith("GET_BY_TAG")) {
            return processGetTasksOperation(update);
        } else {
            log.error("Unexpected value: " + operation);
            return messageUtils.generateErrorEditMessage(update);
        }
    }

    private EditMessageText processGetTasksOperation(Update update) {
        String[] callbackData = CallbackQueryUtils.getCallbackData(update);
        String operation = callbackData[0];
        String subOperation = callbackData[1];

        return switch (subOperation) {
            case "NEXT", "PREV" -> taskService.processGetAllTasksUpdate(update, operation, subOperation);
            case "TASK" -> taskService.processGetTaskInDetail(update, operation);
            default -> {
                log.error("Unexpected value: " + subOperation);
                yield messageUtils.generateErrorEditMessage(update);
            }
        };
    }

    private EditMessageText processGetTagsOperation(Update update) {
        String subOperation =  CallbackQueryUtils.getCallbackData(update)[1];
        return taskService.processGetAllTagsUpdate(update, subOperation);
    }


    private SendMessage processUserMessage(Update update, ServiceCommand userCommand) {
        String answerText = "";
        SendMessage answerMessage = messageUtils.generateSendMessage(update, "");
        User user = userUtils.getUserByUpdate(update);
        String language = user.getLanguage();

        log.trace(update.getMessage().getFrom().toString());

        if (userCommand == null) {
            userCommand = ServiceCommand.NON_COMMAND;
        }

        log.debug("User command: " + userCommand);
        switch (userCommand) {
            case HELP -> {
                answerText = languageManager.getMessage(
                        String.format("help.%s", language),
                        language);

                answerMessage.setText(answerText);
            }
            case START -> {
                answerText = languageManager.getMessage(
                        String.format("start.%s", language),
                        language);

                answerMessage.setText(answerText);
            }
            case SETTINGS -> answerMessage = settingsService.getSettings(update);
            case CREATE_TASK -> answerMessage = taskService.processCreateTask(update);
            case TASKS -> answerMessage = taskService.processGetAllTasks(update, "GET_ALL_TASKS");
            case TODAY_TASKS -> answerMessage = taskService.processGetAllTasks(update, "GET_TODAY_TASKS");
            case COMPLETED_TASKS -> answerMessage = taskService.processGetAllTasks(update, "GET_COMPLETED_TASKS");
            case UNCOMPLETED_TASKS -> answerMessage = taskService.processGetAllTasks(update, "GET_UNCOMPLETED_TASKS");
            case TAGS -> answerMessage = taskService.processGetAllTags(update);
            case DELETE_COMPLETED_TASKS ->
                    answerMessage = taskService.processDeleteAllCompletedTasksConfirmation(update);
            case DELETE_ALL_TASKS -> answerMessage = taskService.processDeleteAllTasksConfirmation(update);
            default -> {
                answerMessage.setText(answerText);
                // get task to determine what user tries to achieve by user's last task's state
                Optional<Task> lastTask = taskRepository.findTopByUserIdOrderByIdDesc(user.getId());
                if (lastTask.isPresent()) {
                    String taskState = lastTask.get().getState();

                    // process further creation/editing of a task in TaskService
                    if (taskState.startsWith("CREAT")) {
                        answerMessage = taskService.processCreatingTask(update);
                    } else {
                        List<Task> tasks = taskRepository.findAll(user.getId());

                        Optional<Task> editTaskOptional = tasks.stream()
                                .filter(task -> task.getState().startsWith("EDIT"))
                                .findFirst();

                        if (editTaskOptional.isPresent()) {
                            answerMessage = taskService.editTask(update, editTaskOptional.get());
                        } else {
                            answerText = languageManager.getMessage(String.format(
                                            "unknown-command.%s", language),
                                    language);
                            answerMessage.setText(answerText);
                        }
                    }
                } else {
                    answerText = languageManager.getMessage(
                            String.format("unknown-command.%s", language),
                            language);
                    answerMessage.setText(answerText);
                }
            }
        }

        return answerMessage;
    }
}
