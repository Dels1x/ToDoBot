package ua.delsix.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.delsix.entity.Task;
import ua.delsix.entity.User;
import ua.delsix.controller.LanguageController;
import ua.delsix.repository.TaskRepository;
import ua.delsix.service.MainService;
import ua.delsix.service.ProducerService;
import ua.delsix.service.TaskService;
import ua.delsix.enums.ServiceCommand;
import ua.delsix.service.SettingsService;
import ua.delsix.utils.MessageUtils;
import ua.delsix.utils.UserUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Log4j
public class MainServiceImpl implements MainService {
    private final LanguageController languageController;

    private final ProducerService producerService;
    private final SettingsService settingsService;
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final MessageUtils messageUtils;
    private final UserUtils userUtils;

    public MainServiceImpl(LanguageController languageController, ProducerService producerService, SettingsService settingsService, TaskService taskService, TaskRepository taskRepository, MessageUtils messageUtils, UserUtils userUtils) {
        this.languageController = languageController;
        this.producerService = producerService;
        this.settingsService = settingsService;
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.messageUtils = messageUtils;
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

        producerService.produceAnswer(answerMessage, update);
    }

    private SendMessage processMessage(Update update) {
        String messageText = update.getMessage().getText();
        ServiceCommand userCommand = ServiceCommand.fromValue(messageText);

        return processUserMessage(update, userCommand);
    }

    private void processCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");
        EditMessageText answer = messageUtils.editMessageGenerator(update, "Unknown error. Please contact developer using his telegram (@dels1x).");
        String operation = callbackData[0];

        log.trace("CallbackData: "+ Arrays.toString(callbackData));

        // get answer based on callbackQuery
        if (operation.startsWith("GET")) {
            if (operation.equals("GET_TAGS")) {
                answer = taskService.processGetAllTagsUpdate(update, callbackData[1]);
            } else {
                switch (callbackData[1]) {
                    case "NEXT", "PREV" -> answer = taskService.processGetAllTasksUpdate(update, operation, callbackData[1]);
                    case "TASK" -> answer = taskService.processGetTaskInDetail(update, operation);
                }
            }
        } else if (operation.startsWith("DELETE_ALL_COMPLETED")) {
            if (callbackData[1].equals("CONFIRM")) {
                answer = taskService.processDeleteAllCompletedTasks(update);
            }
        } else if (operation.startsWith("DELETE_ALL")) {
            if (callbackData[1].equals("CONFIRM")) {
                answer = taskService.processDeleteAllTasks(update);
            }
        } else if (operation.startsWith("SETTINGS")) {
            if (callbackData[1].equals("LANGUAGE")) {
                if (callbackData.length == 2) {
                    answer = settingsService.processLanguage(update);
                } else {
                    if (callbackData[2].equals("SET")) {
                        settingsService.setLanguage(update);
                        return;
                    }
                }
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
        SendMessage answerMessage = messageUtils.sendMessageGenerator(update, "");
        User user = userUtils.getUserByUpdate(update);
        String language = user.getLanguage();

        log.trace(update.getMessage().getFrom().toString());

        if (userCommand == null) {
            userCommand = ServiceCommand.NON_COMMAND;
        }

        log.debug("User command: " + userCommand);
        switch (userCommand) {
            case HELP -> {
                answerText = languageController.getMessage(
                        String.format("help.%s", language),
                        language);

                answerMessage.setText(answerText);
            }
            case START -> {
                answerText = languageController.getMessage(
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
            case DELETE_COMPLETED_TASKS -> answerMessage = taskService.processDeleteAllCompletedTasksConfirmation(update);
            case DELETE_ALL_TASKS -> answerMessage = taskService.processDeleteAllTasksConfirmation(update);
            default -> {
                answerMessage.setText(answerText);
                // get task to determine what user tries to achieve by user's last task's state
                Optional<Task> lastTask = taskRepository.findTopByUserIdOrderByIdDesc(user.getId());
                if(lastTask.isPresent()) {
                    String taskState = lastTask.get().getState();

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
                            answerText = languageController.getMessage(String.format(
                                    "unknown-command.%s", language),
                                    language);
                            answerMessage.setText(answerText);
                        }
                    }
                } else {
                    answerText = languageController.getMessage(
                            String.format("unknown-command.%s", language),
                            language);
                    answerMessage.setText(answerText);
                }
            }
        }

        return answerMessage;
    }
}
