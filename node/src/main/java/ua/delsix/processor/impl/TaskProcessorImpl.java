package ua.delsix.processor.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ua.delsix.entity.Task;
import ua.delsix.entity.User;
import ua.delsix.language.LanguageManager;
import ua.delsix.repository.TaskRepository;
import ua.delsix.repository.UserRepository;
import ua.delsix.service.ProducerService;
import ua.delsix.processor.TaskProcessor;
import ua.delsix.enums.ServiceCommand;
import ua.delsix.utils.MarkupUtils;
import ua.delsix.utils.MessageUtils;
import ua.delsix.utils.TaskUtils;
import ua.delsix.utils.UserUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@Log4j
public class TaskProcessorImpl implements TaskProcessor {
    private final MessageUtils messageUtils;
    private final MarkupUtils markupUtils;
    private final UserUtils userUtils;
    private final TaskUtils taskUtils;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final LanguageManager languageManager;
    private final ProducerService producerService;
    private static final int TASK_PER_PAGE = 4;

    public TaskProcessorImpl(MessageUtils messageUtils, MarkupUtils markupUtils, UserUtils userUtils, TaskRepository taskRepository, UserRepository userRepository, TaskUtils taskUtils, LanguageManager languageManager, ProducerService producerService) {
        this.messageUtils = messageUtils;
        this.markupUtils = markupUtils;
        this.userUtils = userUtils;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskUtils = taskUtils;
        this.languageManager = languageManager;
        this.producerService = producerService;
    }

    // create methods

    @Override
    public SendMessage processCreateTask(Update update) {
        // creating a new task
        Task newTask = Task.builder()
                .userId(userUtils.getUserByUpdate(update).getId())
                .name("Unnamed " + (taskRepository.count() + 1))
                .state("CREATING_NAME")
                .status("Uncompleted")
                .createdAt(LocalDate.now())
                .build();

        // get user's preferred language
        String languageCode = userUtils.getUserByUpdate(update).getLanguage();

        // saving the task to the table
        taskRepository.save(newTask);

        // sending answerMessage to MainService
        return messageUtils.sendMessageGenerator(
                update,
                languageManager.getMessage(
                        String.format("create.start.%s", languageCode),
                        languageCode),
                markupUtils.getCancelSkipFinishMarkup(update));
    }

    @Override
    public SendMessage processCreatingTask(Update update) {
        SendMessage answerMessage = messageUtils.sendMessageGenerator(update, "");
        User user = userUtils.getUserByUpdate(update);
        // get user's preferred language
        String languageCode = user.getLanguage();

        Optional<Task> taskOptional = taskRepository.findTopByUserIdOrderByIdDesc(user.getId());
        if (taskOptional.isEmpty()) {
            log.error("User does not have any tasks");
            answerMessage.setText(
                    languageManager.getMessage(
                            String.format("bot.error.%s", languageCode),
                            languageCode));
            return answerMessage;
        }

        Message userMessage = update.getMessage();
        ServiceCommand userCommand = ServiceCommand.fromValue(userMessage.getText());
        Task task = taskOptional.get();
        String taskState = task.getState();

        //set text according to task's state
        answerMessage.setText(taskUtils.responseForEachState(task, user));
        ReplyKeyboardMarkup markup = markupUtils.getCancelSkipFinishMarkup(update);
        answerMessage.setReplyMarkup(markup);
        String answerText;

        if (userCommand.equals(ServiceCommand.CANCEL)) {
            // Delete task from tasks table
            taskRepository.deleteById(task.getId());
            answerMessage.setText(
                    languageManager.getMessage(
                            String.format("create.cancel.%s", languageCode),
                            languageCode));
            answerMessage.setReplyMarkup(null);
            userRepository.save(user);

            return answerMessage;
        } else if (userCommand.equals(ServiceCommand.SKIP)) {
            int stateId = TaskUtils.states.indexOf(taskState);
            taskState = TaskUtils.states.get(stateId + 1);
            task.setState(taskState);
            taskRepository.save(task);

            switch (taskState) {
                case "CREATING_DATE" -> answerMessage.setReplyMarkup(markupUtils.getDateMarkup(update));
                case "CREATING_PRIORITY" -> answerMessage.setReplyMarkup(markupUtils.getPriorityMarkup(update));
                case "CREATING_DIFFICULTY" -> answerMessage.setReplyMarkup(markupUtils.getDifficultyMarkup(update));
                case "CREATING_TAG" -> answerMessage.setReplyMarkup(markupUtils.getTagsReplyMarkup(update));
                case "COMPLETED" -> answerMessage.setReplyMarkup(null);
            }

            return answerMessage;
        } else if (userCommand.equals(ServiceCommand.FINISH)) {
            // set task's state to COMPLETED
            task.setState("COMPLETED");
            taskRepository.save(task);

            // increment user's task count by 1
            user.setTaskCompleted(user.getTaskCompleted() + 1);
            userRepository.save(user);

            // get completed task answer and set its reply markup to null and return it
            var newAnswerMessage = completedTaskAnswer(answerMessage, task, user);
            newAnswerMessage.setReplyMarkup(null);
            return newAnswerMessage;
        }

        // handling different task's states
        switch (taskState) {
            case "CREATING_NAME" -> {
                answerText = setTaskName(update, task);

                if (answerText != null) {
                    answerMessage.setText(answerText);
                } else {
                    task.setState("CREATING_DESCRIPTION");
                }
            }
            case "CREATING_DESCRIPTION" -> {
                answerText = setTaskDescription(update, task);

                if (answerText != null) {
                    answerMessage.setText(answerText);
                } else {
                    answerMessage.setReplyMarkup(markupUtils.getDateMarkup(update));
                    task.setState("CREATING_DATE");
                }
            }
            case "CREATING_DATE" -> {
                answerText = setTaskDate(update, task);

                if (answerText != null) {
                    answerMessage.setText(answerText);
                } else {
                    answerMessage.setReplyMarkup(markupUtils.getPriorityMarkup(update));
                    task.setState("CREATING_PRIORITY");
                }
            }
            case "CREATING_PRIORITY" -> {
                answerText = setTaskPriority(update, task);

                if (answerText != null) {
                    answerMessage.setText(answerText);
                } else {
                    answerMessage.setReplyMarkup(markupUtils.getDifficultyMarkup(update));
                    task.setState("CREATING_DIFFICULTY");
                }
            }
            case "CREATING_DIFFICULTY" -> {
                answerText = setTaskDifficulty(update, task);

                if (answerText != null) {
                    answerMessage.setText(answerText);
                } else {
                    answerMessage.setReplyMarkup(markupUtils.getTagsReplyMarkup(update));
                    task.setState("CREATING_TAG");
                }
            }
            case "CREATING_TAG" -> {
                answerText = setTaskTag(update, task);

                if (answerText != null) {
                    answerMessage.setText(answerText);
                } else {
                    // set task's state to COMPLETED and save since we will return message
                    task.setState("COMPLETED");
                    taskRepository.save(task);

                    // increment user's task count by 1
                    user.setTaskCompleted(user.getTaskCompleted() + 1);
                    userRepository.save(user);

                    var message = completedTaskAnswer(answerMessage, task, user);
                    message.setReplyMarkup(null);
                    return message;
                }
            }
        }

        taskRepository.save(task);
        return answerMessage;
    }

    private String setTaskName(Update update, Task task) {
        String language = userUtils.getUserByUpdate(update).getLanguage();
        Message userMessage = update.getMessage();
        // checking if user's message has text, since he can send a picture of document
        if (!userMessage.hasText()) {
            return languageManager.getMessage(
                    String.format("error.set.name.no-text.%s", language),
                    language);
        }
        task.setName(userMessage.getText());
        taskRepository.save(task);

        return null;
    }

    private String setTaskDescription(Update update, Task task) {
        String language = userUtils.getUserByUpdate(update).getLanguage();
        Message userMessage = update.getMessage();
        // checking if user's message has text, since he can send a picture of document
        if (!userMessage.hasText()) {

            return languageManager.getMessage(
                    String.format("error.set.description.no-text.%s", language),
                    language);
        }

        task.setDescription(userMessage.getText());
        taskRepository.save(task);
        return null;
    }

    private String setTaskDate(Update update, Task task) {
        String language = userUtils.getUserByUpdate(update).getLanguage();
        Message userMessage = update.getMessage();
        String errorMessage = languageManager.getMessage(
                String.format("error.set.date.no-text.%s", language),
                language);
        // checking if user's message has text, since he can send a picture of document
        if (!userMessage.hasText()) {
            return errorMessage;
        }
        LocalDate date;

        // checking if user's message is a date. if true - parsing to LocalDate, if false - returning a corresponding message back to user
        try {
            //TODO add ability to choose date by typing "in X days"
            if (languageManager.isInSection(userMessage.getText(), "keyboard.date.today", language)) {
                date = LocalDate.now();
            } else if (languageManager.isInSection(userMessage.getText(), "keyboard.date.tomorrow", language)) {
                date = LocalDate.now().plusDays(1);
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                date = LocalDate.parse(userMessage.getText(), formatter);
            }
        } catch (DateTimeParseException e) {
            return errorMessage;
        }

        task.setTargetDate(date);
        taskRepository.save(task);
        return null;
    }

    private String setTaskPriority(Update update, Task task) {
        String language = userUtils.getUserByUpdate(update).getLanguage();
        Message userMessage = update.getMessage();
        // checking if user's message has text, since he can send a picture of document
        if (!userMessage.hasText()) {
            return languageManager.getMessage(
                    String.format("error.set.priority.no-text.%s", language),
                    language);
        }

        String text = userMessage.getText().toLowerCase();

        if (languageManager.isInSection(userMessage.getText(), "keyboard.priority.not-important", language)) {
            task.setPriority(1);
        } else if (languageManager.isInSection(userMessage.getText(), "keyboard.priority.low", language)) {
            task.setPriority(2);
        } else if (languageManager.isInSection(userMessage.getText(), "keyboard.priority.medium", language)) {
            task.setPriority(3);
        } else if (languageManager.isInSection(userMessage.getText(), "keyboard.priority.high", language)) {
            task.setPriority(4);
        } else if (languageManager.isInSection(userMessage.getText(), "keyboard.priority.very-high", language)) {
            task.setPriority(5);
        } else if (languageManager.isInSection(userMessage.getText(), "keyboard.priority.extremely-high", language)) {
            task.setPriority(6);
        } else {
            int number;

            // Verifying that user's response is a number
            try {
                number = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                return languageManager.getMessage(
                        String.format("error.set.priority.no-text.%s", language),
                        language);
            }

            // Check if the number is within the allowed range of 1 to 6
            if (number >= 1 && number <= 6) {
                // Set the task priority to the user-provided number
                task.setPriority(number);
            } else if (number != 0) {
                // Inform the user that the priority must be within the allowed range of 1 to 6
                return languageManager.getMessage(
                        String.format("error.set.priority.out-of-range.%s", language),
                        language);
            }
        }

        taskRepository.save(task);
        return null;
    }

    private String setTaskDifficulty(Update update, Task task) {
        String language = userUtils.getUserByUpdate(update).getLanguage();
        Message userMessage = update.getMessage();
        // checking if user's message has text, since he can send a picture of document
        if (!userMessage.hasText()) {
            return languageManager.getMessage(
                    String.format("error.set.difficulty.no-text.%s", language),
                    language);
        }

        // Verifying that user's response is a number
        int number;
        try {
            number = Integer.parseInt(userMessage.getText());
        } catch (NumberFormatException e) {
            if (languageManager.isInSection(userMessage.getText(), "keyboard.difficulty.no-difficulty", language)) {
                task.setDifficulty(0);
            } else if (languageManager.isInSection(userMessage.getText(), "keyboard.difficulty.very-easy", language)) {
                task.setDifficulty(1);
            } else if (languageManager.isInSection(userMessage.getText(), "keyboard.difficulty.easy", language)) {
                task.setDifficulty(2);
            } else if (languageManager.isInSection(userMessage.getText(), "keyboard.difficulty.moderate", language)) {
                task.setDifficulty(3);
            } else if (languageManager.isInSection(userMessage.getText(), "keyboard.difficulty.challenging", language)) {
                task.setDifficulty(4);
            } else if (languageManager.isInSection(userMessage.getText(), "keyboard.difficulty.difficult", language)) {
                task.setDifficulty(5);
            } else if (languageManager.isInSection(userMessage.getText(), "keyboard.difficulty.very-difficult", language)) {
                task.setDifficulty(6);
            } else if (languageManager.isInSection(userMessage.getText(), "keyboard.difficulty.extremely-difficult", language)) {
                task.setDifficulty(7);
            } else {
                return languageManager.getMessage(
                        String.format("error.set.difficulty.no-text.%s", language),
                        language);
            }

            return null;
        }

        // Check if the number is within the allowed range of 0 to 7
        if (number >= 0 && number <= 7) {
            // Set the task difficulty to the user-provided number
            task.setDifficulty(number);
        } else {
            // Inform the user that the priority must be within the allowed range of 0 to 7
            return languageManager.getMessage(
                    String.format("error.set.priority.out-of-range.%s", language),
                    language);
        }

        taskRepository.save(task);
        return null;
    }

    private String setTaskTag(Update update, Task task) {
        String language = userUtils.getUserByUpdate(update).getLanguage();
        Message userMessage = update.getMessage();
        // checking if user's message has text, since he can send a picture of document
        if (!userMessage.hasText()) {
            return languageManager.getMessage(
                    String.format("error.set.priority.no-text.%s", language),
                    language);
        }

        if (userMessage.getText().equals("Untagged")) {
            task.setTag(null);
        } else {
            task.setTag(userMessage.getText());
        }

        return null;
    }


    // EDIT methods

    @Override
    public EditMessageText processTasksEdit(Update update, String operation) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");
        int pageIndex = Integer.parseInt(callbackData[2]);
        int pageTaskIndex = Integer.parseInt(callbackData[3]);

        log.trace("callbackData: " + Arrays.toString(callbackData));

        // get user from database to later get needed task using user's id
        User user = userUtils.getUserByUpdate(update);

        // get overall task index with pageIndex * tasksPerPageAmount + pageTaskIndex formula
        int taskIndex = pageIndex * TASK_PER_PAGE + pageTaskIndex;

        // get list of tasks
        List<Task> tasks = getTasks(user, operation);

        if (tasks == null || tasks.size() == 0) {
            log.error("tasks is null");
            return null;
        }

        // get task to edit
        Task taskToEdit = tasks.get(taskIndex);

        // calling editTask method, if callbackData presses any edit button and return null to MainService
        if (callbackData.length == 6) {
            if (taskSwitchStateToEdit(taskToEdit, update)) {
                return processGetTaskInDetail(update, operation);
            }

            return null;
        }

        return messageUtils.editMessageGenerator(
                update,
                taskUtils.taskToStringInDetail(taskToEdit, user),
                markupUtils.getEditMarkup(callbackData, operation, update));
    }

    private boolean taskSwitchStateToEdit(Task task, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");

        String language = userUtils.getUserByUpdate(update).getLanguage();

        switch (callbackData[5]) {
            case "NAME" -> {
                task.setState("EDITING_NAME");
                String text = String.format(
                        languageManager.getMessage(
                                String.format("edit.name.%s", language),
                                language),
                        task.getName());
                log.trace("Sending answer to ProducerService");
                producerService.produceAnswer(
                        messageUtils.sendMessageGenerator(
                                update,
                                text),
                        update);
            }
            case "DESC" -> {
                task.setState("EDITING_DESCRIPTION");
                String text = String.format(
                        languageManager.getMessage(
                                String.format("edit.description.%s", language),
                                language),
                        task.getName());
                log.trace("Sending answer to ProducerService");
                producerService.produceAnswer(
                        messageUtils.sendMessageGenerator(
                                update,
                                text),
                        update);
            }
            case "DATE" -> {
                task.setState("EDITING_DATE");
                String text = String.format(
                        languageManager.getMessage(
                                String.format("edit.date.%s", language),
                                language),
                        task.getName());
                log.trace("Sending answer to ProducerService");
                producerService.produceAnswer(
                        messageUtils.sendMessageGenerator(
                                update,
                                text,
                                markupUtils.getDateMarkupWithoutSkipCancelFinish(update)),
                        update);
            }
            case "PRIOR" -> {
                task.setState("EDITING_PRIORITY");
                String text = String.format(
                        languageManager.getMessage(
                                String.format("edit.priority.%s", language),
                                language),
                        task.getName());
                log.trace("Sending answer to ProducerService");
                producerService.produceAnswer(
                        messageUtils.sendMessageGenerator(
                                update,
                                text,
                                markupUtils.getPriorityMarkupWithoutSkipCancelFinish(update)),
                        update);
            }
            case "DIFF" -> {
                task.setState("EDITING_DIFFICULTY");
                String text = String.format(
                        languageManager.getMessage(
                                String.format("edit.difficulty.%s", language),
                                language),
                        task.getName());
                log.trace("Sending answer to ProducerService");
                producerService.produceAnswer(
                        messageUtils.sendMessageGenerator(
                                update,
                                text,
                                markupUtils.getDifficultyMarkupWithoutSkipCancelFinish(update)),
                        update);
            }
            case "TAG" -> {
                task.setState("EDITING_TAG");
                String text = String.format(
                        languageManager.getMessage(
                                String.format("edit.tag.%s", language),
                                language),
                        task.getName());
                log.trace("Sending answer to ProducerService");
                producerService.produceAnswer(
                        messageUtils.sendMessageGenerator(
                                update,
                                text,
                                markupUtils.getTagsReplyMarkupWithoutCancelSkipFinish(update)),
                        update);
            }
            case "CANCEL" -> {
                task.setState("COMPLETED");
                taskRepository.save(task);
                return true;
            }
        }

        taskRepository.save(task);
        return false;
    }

    @Override
    public SendMessage editTask(Update update, Task task) {
        Message msg = update.getMessage();
        String msgText = msg.getText();
        SendMessage answer = messageUtils.sendMessageGenerator(update, "");
        String answerText;
        String language = userUtils.getUserByUpdate(update).getLanguage();

        switch (task.getState()) {
            case "EDITING_NAME" -> {
                answerText = setTaskName(update, task);

                if (answerText != null) {
                    answer.setText(answerText);
                } else {
                    answer.setText(String.format(languageManager.getMessage(
                            String.format("edit.finish.name.%s", language),
                            language), msgText));
                    task.setState("COMPLETED");
                }
            }
            case "EDITING_DESCRIPTION" -> {
                answerText = setTaskDescription(update, task);

                if (answerText != null) {
                    answer.setText(answerText);
                } else {
                    answer.setText(String.format(languageManager.getMessage(
                                    String.format("edit.finish.description.%s", language),
                                    language),
                            task.getName(),
                            msgText));
                    task.setState("COMPLETED");
                }
            }
            case "EDITING_DATE" -> {
                answerText = setTaskDate(update, task);

                if (answerText != null) {
                    answer.setText(answerText);
                } else {
                    answer.setText(String.format(languageManager.getMessage(
                                    String.format("edit.finish.date.%s", language),
                                    language),
                            task.getName(),
                            task.getTargetDate()));
                    task.setState("COMPLETED");
                }
            }
            case "EDITING_PRIORITY" -> {
                answerText = setTaskPriority(update, task);

                if (answerText != null) {
                    answer.setText(answerText);
                } else {
                    answer.setText(String.format(languageManager.getMessage(
                                    String.format("edit.finish.priority.%s", language),
                                    language),
                            task.getName(),
                            taskUtils.getPriorityDescription(task.getPriority())));
                    task.setState("COMPLETED");
                }
            }
            case "EDITING_DIFFICULTY" -> {
                answerText = setTaskDifficulty(update, task);

                if (answerText != null) {
                    answer.setText(answerText);
                } else {
                    answer.setText(String.format(languageManager.getMessage(
                                    String.format("edit.finish.difficulty.%s", language),
                                    language),
                            task.getName(),
                            taskUtils.getDifficultyDescription(task.getDifficulty())));
                    task.setState("COMPLETED");
                }
            }
            case "EDITING_TAG" -> {
                answerText = setTaskTag(update, task);

                if (answerText != null) {
                    answer.setText(answerText);
                } else {
                    answer.setText(String.format(languageManager.getMessage(
                                    String.format("edit.finish.tag.%s", language),
                                    language),
                            task.getName(),
                            task.getTag()));
                    task.setState("COMPLETED");
                }
            }
        }

        taskRepository.save(task);
        return answer;
    }

    // DELETE methods


    @Override
    public SendMessage processDeleteAllCompletedTasksConfirmation(Update update) {
        String language = userUtils.getUserByUpdate(update).getLanguage();
        // setting up keyboard
        List<InlineKeyboardButton> keyboard = new ArrayList<>();
        InlineKeyboardButton confirmButton = new InlineKeyboardButton("Confirm");
        confirmButton.setCallbackData("DELETE_ALL_COMPLETED/CONFIRM");
        keyboard.add(confirmButton);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(Collections.singletonList(keyboard));

        return messageUtils.sendMessageGenerator(update,
                languageManager.getMessage(
                        String.format("task.delete.all-completed-confirm.%s", language),
                        language),
                markup);
    }

    @Override
    public EditMessageText processDeleteAllCompletedTasks(Update update) {
        User user = userUtils.getUserByUpdate(update);
        String language = user.getLanguage();
        taskRepository.deleteAllCompletedTasks(user.getId());

        return messageUtils.editMessageGenerator(update,
                languageManager.getMessage(
                        String.format("task.delete.all-completed.%s", language),
                        language)
        );
    }

    @Override
    public SendMessage processDeleteAllTasksConfirmation(Update update) {
        String language = userUtils.getUserByUpdate(update).getLanguage();
        // setting up keyboard
        List<InlineKeyboardButton> keyboard = new ArrayList<>();
        InlineKeyboardButton confirmButton = new InlineKeyboardButton("Confirm");
        confirmButton.setCallbackData("DELETE_ALL/CONFIRM");
        keyboard.add(confirmButton);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(Collections.singletonList(keyboard));

        return messageUtils.sendMessageGenerator(update,
                languageManager.getMessage(
                        String.format("task.delete.all.%s", language),
                        language),
                markup);
    }

    @Override
    public EditMessageText processDeleteAllTasks(Update update) {
        User user = userUtils.getUserByUpdate(update);
        String language = user.getLanguage();
        taskRepository.deleteAllTasks(user.getId());

        return messageUtils.editMessageGenerator(update,
                languageManager.getMessage(
                        String.format("task.delete.all-confirm.%s", language),
                        language));
    }

    @Override
    public EditMessageText processTasksDelete(Update update, String operation) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");
        int pageIndex = Integer.parseInt(callbackData[2]);
        int pageTaskIndex = Integer.parseInt(callbackData[3]);

        log.trace("callbackData: " + Arrays.toString(callbackData));

        // get user from database to later get needed task using user's id
        User user = userUtils.getUserByUpdate(update);

        // get overall task index with pageIndex * tasksPerPageAmount + pageTaskIndex formula
        int taskIndex = pageIndex * TASK_PER_PAGE + pageTaskIndex;

        // get list of tasks
        List<Task> tasks = getTasks(user, operation);
        log.trace(tasks);

        if (tasks == null || tasks.size() == 0) {
            log.error("tasks is null");
            return null;
        }

        try {
            Task taskToDelete = tasks.get(taskIndex);
            taskRepository.delete(taskToDelete);
        } catch (IndexOutOfBoundsException e) {
            log.error(e.getMessage());
            return returnToAllTasks(update, operation);
        }

        return returnToAllTasks(update, operation);
    }

    private EditMessageText processTasksDeleteConfirm(Update update, String operation) {
        String language = userUtils.getUserByUpdate(update).getLanguage();
        CallbackQuery query = update.getCallbackQuery();
        String[] callbackData = query.getData().split("/");
        int pageIndex = Integer.parseInt(callbackData[2]);
        int pageTaskIndex = Integer.parseInt(callbackData[3]);

        log.trace("callbackData: " + Arrays.toString(callbackData));

        String text = languageManager.getMessage(
                String.format("keyboard.tasks.detail.complete.%s", language),
                language);

        //setting up keyboard
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> buttons = new ArrayList<>();

        InlineKeyboardButton confirmButton = new InlineKeyboardButton("Confirm");
        InlineKeyboardButton cancelButton = new InlineKeyboardButton("Cancel");

        confirmButton.setCallbackData(String.format("%s/TASK/%d/%d/DELETE", operation, pageIndex, pageTaskIndex));
        cancelButton.setCallbackData(String.format("%s/TASK/%d/%d/TASK", operation, pageIndex, pageTaskIndex));

        buttons.add(confirmButton);
        buttons.add(cancelButton);

        keyboard.add(buttons);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboard);

        return messageUtils.editMessageGenerator(update, text, markup);
    }

    // GET methods

    @Override
    public EditMessageText processGetTaskInDetail(Update update, String operation) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");

        if (callbackData.length < 5) {
            log.error("callbackData length must be 5 or more, but callbackData length is " + callbackData.length);
            return null;
        }

        log.trace("callbackData: " + Arrays.toString(callbackData));

        int pageIndex;
        int pageTaskIndex;

        try {
            pageIndex = Integer.parseInt(callbackData[2]);
            pageTaskIndex = Integer.parseInt(callbackData[3]);
        } catch (NumberFormatException e) {
            log.error("CallbackQuery error: " + e.getMessage());
            return null;
        }
        // get user from database to later get needed task using user's id
        User user = userUtils.getUserByUpdate(update);
        String language = user.getLanguage();

        List<Task> tasks = getTasks(user, operation);

        if (tasks == null || tasks.size() == 0) {
            log.error("tasks is null");
            return null;
        }

        // handling different buttons
        switch (callbackData[4]) {
            case "NEXT" -> pageTaskIndex++;
            case "PREV" -> pageTaskIndex--;
            case "CANCEL" -> {
                return returnToAllTasks(update, operation);
            }
            case "DELETE_CONFIRM" -> {
                return processTasksDeleteConfirm(update, operation);
            }
            case "DELETE" -> {
                return processTasksDelete(update, operation);
            }
            case "EDIT" -> {
                // doing checks, so that if 6th value is CANCEL, it doesn't go on never-ending loop
                if (callbackData.length == 5 ||
                        (callbackData.length == 6 && !callbackData[5].equals("CANCEL"))) {
                    return processTasksEdit(update, operation);
                }
            }
        }

        if (pageTaskIndex > TASK_PER_PAGE) {
            pageTaskIndex = 0;
            pageIndex++;
        }

        // get overall task index with pageIndex * tasksPerPageAmount + pageTaskIndex formula
        int taskIndex = pageIndex * TASK_PER_PAGE + pageTaskIndex;

        if (taskIndex < 0 || taskIndex >= tasks.size()) {
            log.error(String.format("Task Index = %d | task.size() = %d", taskIndex, tasks.size()));
            return null;
        }
        // get needed task
        Task task = tasks.get(taskIndex);

        // handle a button to complete a task
        if (callbackData[4].equals("COMPLETE")) {
            task.setStatus("Completed");
            task.setCompletionDate(LocalDate.now());
            taskRepository.save(task);
        } else if (callbackData[4].equals("UNCOMPLETE")) {
            task.setStatus("Uncompleted");
            task.setCompletionDate(null);
            taskRepository.save(task);
        }

        // setting up the keyboard
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> prevNextButtons = new ArrayList<>();
        List<InlineKeyboardButton> configurationButtons = new ArrayList<>();
        List<InlineKeyboardButton> configurationButtons2 = new ArrayList<>();

        // setting up buttons
        InlineKeyboardButton prevButton = new InlineKeyboardButton("<");
        InlineKeyboardButton nextButton = new InlineKeyboardButton(">");
        InlineKeyboardButton completeButton = new InlineKeyboardButton(
                languageManager.getMessage(
                        String.format("keyboard.tasks.detail.complete.%s", language),
                        language)
        );
        InlineKeyboardButton notCompletedButton = new InlineKeyboardButton(
                languageManager.getMessage(
                        String.format("keyboard.tasks.detail.uncomplete.%s", language),
                        language)
        );
        InlineKeyboardButton editButton = new InlineKeyboardButton(
                languageManager.getMessage(
                        String.format("keyboard.tasks.detail.edit.%s", language),
                        language)
        );
        InlineKeyboardButton deleteButton = new InlineKeyboardButton(
                languageManager.getMessage(
                        String.format("keyboard.tasks.detail.delete.%s", language),
                        language)
        );
        InlineKeyboardButton backButton = new InlineKeyboardButton(
                languageManager.getMessage(
                        String.format("keyboard.tasks.detail.back.%s", language),
                        language)
        );

        // setting callback data and adding buttons to keyboard
        if (taskIndex > 0) {
            prevButton.setCallbackData(String.format("%s/TASK/%d/%d/PREV", operation, pageIndex, pageTaskIndex));
            prevNextButtons.add(prevButton);
        }
        if (taskIndex < tasks.size() - 1) {
            nextButton.setCallbackData(String.format("%s/TASK/%d/%d/NEXT", operation, pageIndex, pageTaskIndex));
            prevNextButtons.add(nextButton);
        }
        if (task.getStatus().equals("Completed")) {
            notCompletedButton.setCallbackData(String.format("%s/TASK/%d/%d/UNCOMPLETE", operation, pageIndex, pageTaskIndex));
            configurationButtons.add(notCompletedButton);
        } else {
            completeButton.setCallbackData(String.format("%s/TASK/%d/%d/COMPLETE", operation, pageIndex, pageTaskIndex));
            configurationButtons.add(completeButton);
        }
        editButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT", operation, pageIndex, pageTaskIndex));
        deleteButton.setCallbackData(String.format("%s/TASK/%d/%d/DELETE_CONFIRM", operation, pageIndex, pageTaskIndex));
        backButton.setCallbackData(String.format("%s/TASK/%d/%d/CANCEL", operation, pageIndex, pageTaskIndex));

        configurationButtons.add(editButton);
        configurationButtons2.add(deleteButton);
        configurationButtons2.add(backButton);

        keyboard.add(prevNextButtons);
        keyboard.add(configurationButtons);
        keyboard.add(configurationButtons2);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return messageUtils.editMessageGenerator(update, taskUtils.taskToStringInDetail(task, user), markup);
    }

    @Override
    public EditMessageText processGetAllTasksUpdate(Update update, String operation, String move) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");
        int pageIndex = Integer.parseInt(callbackData[2]);

        if (move.equals("NEXT")) {
            pageIndex++;
        } else if (move.equals("PREV")) {
            pageIndex--;
        }

        Map<String[], InlineKeyboardMarkup> pagesAndMarkup = getTasksTextAndMarkup(update, pageIndex, operation);

        if (pagesAndMarkup == null) {
            log.error("pagesAndMarkup == null");
            return null;
        }

        String[] pages = pagesAndMarkup.keySet().iterator().next();
        InlineKeyboardMarkup markup = pagesAndMarkup.values().iterator().next();

        return messageUtils.editMessageGenerator(update, pages[pageIndex], markup);
    }

    @Override
    public SendMessage processGetAllTasks(Update update, String operation) {
        int pageIndex = 0;
        String language = userUtils.getUserByUpdate(update).getLanguage();
        Map<String[], InlineKeyboardMarkup> pagesAndMarkup = getTasksTextAndMarkup(update, pageIndex, operation);

        // handle case if user doesn't have any tasks yet
        if (pagesAndMarkup == null) {
            return messageUtils.sendMessageGenerator(
                    update,
                    languageManager.getMessage(
                            String.format("error.no-tasks.%s", language),
                            language)
            );
        }

        String[] pages = pagesAndMarkup.keySet().iterator().next();
        InlineKeyboardMarkup markup = pagesAndMarkup.values().iterator().next();

        return messageUtils.sendMessageGenerator(update, pages[pageIndex], markup);
    }

    @Override
    public SendMessage processGetAllTags(Update update) {
        User user = userUtils.getUserByUpdate(update);
        String language = user.getLanguage();
        return messageUtils.sendMessageGenerator(update,
                languageManager.getMessage(
                        String.format("task.other.tags.%s", language),
                        language),
                markupUtils.getTagsInlineMarkup(update, 0));
    }

    @Override
    public EditMessageText processGetAllTagsUpdate(Update update, String move) {
        String[] callbackData = update.getCallbackQuery().getData().split("/");

        int pageIndex = Integer.parseInt(callbackData[2]);
        if (move.equals("NEXT")) {
            pageIndex++;
        } else if (move.equals("PREV")) {
            pageIndex--;
        }

        User user = userUtils.getUserByUpdate(update);
        String language = user.getLanguage();

        return messageUtils.editMessageGenerator(update,
                languageManager.getMessage(
                        String.format("task.other.tags.%s", language),
                        language),
                markupUtils.getTagsInlineMarkup(update, pageIndex));
    }

    // CreateTask methods

    private SendMessage completedTaskAnswer(SendMessage answerMessage, Task task, User user) {
        answerMessage.setReplyMarkup(null);
        answerMessage.setText(taskUtils.responseForEachState(task, user));

        return answerMessage;
    }

    // GetAllTasks methods

    private EditMessageText returnToAllTasks(Update update, String operation) {
        String language = userUtils.getUserByUpdate(update).getLanguage();
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");
        int pageIndex = Integer.parseInt(callbackData[2]);

        Map<String[], InlineKeyboardMarkup> pagesAndMarkup = getTasksTextAndMarkup(update, pageIndex, operation);

        // handle case if user doesn't have any tasks yet (user can remove the task, so he could still have 0 tasks)
        if (pagesAndMarkup == null) {
            String text = languageManager.getMessage(
                    String.format("error.no-tasks.%s", language),
                    language);
            return messageUtils.editMessageGenerator(update, text);
        }

        String[] pages = pagesAndMarkup.keySet().iterator().next();
        InlineKeyboardMarkup markup = pagesAndMarkup.values().iterator().next();

        if (pages.length == pageIndex) {
            pageIndex = pages.length - 1;
        }

        return messageUtils.editMessageGenerator(update, pages[pageIndex], markup);
    }

    private Map<String[], InlineKeyboardMarkup> getTasksTextAndMarkup(Update update, int pageIndex, String operation) {
        User user = userUtils.getUserByUpdate(update);
        String language = user.getLanguage();
        String[] pages;

        int pageTaskIndex = 0;

        //keyboard setup
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // row with prev and next buttons
        List<InlineKeyboardButton> mainRow = new ArrayList<>();

        // rows with tasks
        List<InlineKeyboardButton> secondRow = new ArrayList<>();
        List<InlineKeyboardButton> thirdRow = new ArrayList<>();
        List<InlineKeyboardButton> fourthRow = new ArrayList<>();
        List<InlineKeyboardButton> fifthRow = new ArrayList<>();


        List<Task> tasks = getTasks(user, operation);

        if (tasks == null || tasks.size() == 0) {
            log.error("tasks is null");
            return null;
        }

        // creating list of pages full of tasks
        pages = new String[(int) Math.ceil(tasks.size() / (float) TASK_PER_PAGE)];
        int pageNumber = -1;
        int taskNumber = 0;
        boolean noCompletedFlowYet = true;
        boolean hasCompletedTasks = tasks.stream().anyMatch(task -> task.getStatus().equals("Completed"));
        boolean hasUncompletedTasks = tasks.stream().anyMatch(task -> task.getStatus().equals("Uncompleted"));

        for (int i = 0; i < tasks.size(); i++) {
            if (i % TASK_PER_PAGE == 0) {
                pageNumber++;
                taskNumber = 0;
                pages[pageNumber] = "";

                if (pageNumber == 0 && hasUncompletedTasks) {
                    pages[pageNumber] = pages[pageNumber].concat(
                                    languageManager.getMessage(String.format("task.other.uncompleted.%s", language),
                                            language))
                            .concat("\n\n");
                }
            }

            //Mark completed/uncompleted
            if (hasCompletedTasks && noCompletedFlowYet && tasks.get(i).getStatus().equals("Completed")) {
                noCompletedFlowYet = false;

                pages[pageNumber] = pages[pageNumber].concat(
                                languageManager.getMessage(String.format("task.other.completed.%s", language),
                                        language))
                        .concat("\n\n");
            }
            // get current task
            Task task = tasks.get(i);

            // get task name for a button, and limit it only to 24 characters
            String taskName = task.getName();
            if (taskName.length() > 64) {
                taskName = taskName.substring(0, 61).concat("...");
            }

            // set up button for current task
            InlineKeyboardButton currentButton;

            if (task.getStatus().equals("Completed")) {
                currentButton = new InlineKeyboardButton("✅ ".concat(taskName));
            } else {
                currentButton = new InlineKeyboardButton("❌ ".concat(taskName));
            }
            currentButton.setCallbackData(String.format("%s/TASK/%d/%d/TASK", operation, pageNumber, taskNumber));

            if (pageNumber == pageIndex) {
                if (taskNumber <= TASK_PER_PAGE / 4 - 1) {
                    secondRow.add(currentButton);
                } else if (taskNumber <= TASK_PER_PAGE / 4 * 2 - 1) {
                    thirdRow.add(currentButton);
                } else if (taskNumber <= TASK_PER_PAGE / 4 * 3 - 1) {
                    fourthRow.add(currentButton);
                } else {
                    fifthRow.add(currentButton);
                }
            }

            pages[pageNumber] = pages[pageNumber].concat(taskUtils.taskToString(task) + "\n\n");
            taskNumber++;
        }

        // setting up row with prev next buttons
        InlineKeyboardButton prevButton = new InlineKeyboardButton("<");
        InlineKeyboardButton nextButton = new InlineKeyboardButton(">");

        prevButton.setCallbackData(String.format("%s/PREV/%d/%d", operation, pageIndex, pageTaskIndex));
        nextButton.setCallbackData(String.format("%s/NEXT/%d/%d", operation, pageIndex, pageTaskIndex));

        if (pageIndex > 0 && pageIndex < pageNumber) {
            mainRow.add(prevButton);
            mainRow.add(nextButton);
        } else if (pageIndex == 0 && pageIndex < pageNumber) {
            mainRow.add(nextButton);
        } else if (pageIndex > 0 && pageIndex == pageNumber) {
            mainRow.add(prevButton);
        }

        // finish setting up keyboard
        keyboard.add(mainRow);
        keyboard.add(secondRow);
        keyboard.add(thirdRow);
        keyboard.add(fourthRow);
        keyboard.add(fifthRow);
        markup.setKeyboard(keyboard);

        // creating map to return pages and markup at the same time
        Map<String[], InlineKeyboardMarkup> map = new HashMap<>();
        map.put(pages, markup);
        return map;
    }

    private List<Task> getTasks(User user, String operation) {
        // get tasks for different operations
        switch (operation) {
            case "GET_ALL_TASKS" -> {
                return taskRepository.findAll(user.getId());
            }
            case "GET_TODAY_TASKS" -> {
                return taskRepository.findAllTasksDatedForToday(user.getId(), LocalDate.now());
            }
            case "GET_COMPLETED_TASKS" -> {
                return taskRepository.findAllCompletedTasks(user.getId());
            }
            case "GET_UNCOMPLETED_TASKS" -> {
                return taskRepository.findAllUncompletedTasks(user.getId());
            }
            default -> {
                if (operation.startsWith("GET_BY_TAG")) {
                    String tag = operation.split("\\|")[1];

                    if (tag.equals("Untagged")) {
                        return taskRepository.findAllByTag(user.getId(), null);
                    }

                    return taskRepository.findAllByTag(user.getId(), tag);
                }
                log.error("Unknown operation: " + operation);
                return null;
            }
        }
    }
}
