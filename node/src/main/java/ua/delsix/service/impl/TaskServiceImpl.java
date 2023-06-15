package ua.delsix.service.impl;

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
import ua.delsix.repository.TaskRepository;
import ua.delsix.repository.UserRepository;
import ua.delsix.service.ProducerService;
import ua.delsix.service.TaskService;
import ua.delsix.service.enums.ServiceCommand;
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
public class TaskServiceImpl implements TaskService {
    private final UserUtils userUtils;
    private final TaskUtils taskUtils;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProducerService producerService;
    private static final int TASK_PER_PAGE = 4;

    public TaskServiceImpl(UserUtils userUtils, TaskRepository taskRepository, UserRepository userRepository, TaskUtils taskUtils, ProducerService producerService) {
        this.userUtils = userUtils;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskUtils = taskUtils;
        this.producerService = producerService;
    }

    // create methods

    @Override
    public SendMessage processCreateTask(Update update) {
        // creating a new task
        Task newTask = Task.builder()
                .userId(userUtils.getUserByTag(update).getId())
                .name("Unnamed " + (taskRepository.count() + 1))
                .state("CREATING_NAME")
                .status("Uncompleted")
                .createdAt(LocalDate.now())
                .build();

        // add one to user's task count
        User user = userUtils.getUserByTag(update);
        userRepository.save(user);

        // saving the task to the table
        taskRepository.save(newTask);

        // sending answerMessage to MainService
        return MessageUtils.sendMessageGenerator(
                update,
                "Alright, a new task. To proceed, let's choose a name for the task first.",
                MarkupUtils.getCancelSkipFinishMarkup() );
    }

    @Override
    public SendMessage processCreatingTask(Update update) {
        SendMessage answerMessage = MessageUtils.sendMessageGenerator(update, "");
        Optional<Task> taskOptional = taskRepository.findTopByUserIdOrderByIdDesc(userUtils.getUserByTag(update).getId());
        if (taskOptional.isEmpty()) {
            log.error("User does not have any tasks");
            answerMessage.setText("Seems like there is an issue with the bot.\n\n Please, come back later");
            return answerMessage;
        }
        Message userMessage = update.getMessage();
        ServiceCommand userCommand = ServiceCommand.fromValue(userMessage.getText());
        Task task = taskOptional.get();
        String taskState = task.getState();

        User user = userUtils.getUserByTag(update);

        //set text according to task's state
        answerMessage.setText(taskUtils.responseForEachState(task));
        ReplyKeyboardMarkup markup = MarkupUtils.getCancelSkipFinishMarkup();
        answerMessage.setReplyMarkup(markup);
        String answerText;

        if (userCommand.equals(ServiceCommand.CANCEL)) {
            // Delete task from tasks table
            taskRepository.deleteById(task.getId());
            answerMessage.setText("Creation of the task was successfully cancelled.");
            answerMessage.setReplyMarkup(null);
            userRepository.save(user);

            return answerMessage;
        } else if (userCommand.equals(ServiceCommand.SKIP)) {
            int stateId = TaskUtils.states.indexOf(taskState);
            taskState = TaskUtils.states.get(stateId + 1);
            task.setState(taskState);
            taskRepository.save(task);

            if(taskState.equals("CREATING_DATE")) {
                answerMessage.setReplyMarkup(MarkupUtils.getDateMarkup());
            }

            return answerMessage;
        } else if (userCommand.equals(ServiceCommand.FINISH)) {
            // set task's state to COMPLETED
            //TODO move this block into a separate method
            task.setState("COMPLETED");
            taskRepository.save(task);

            // add one to user's task completed count
            user.setTaskCompleted(user.getTaskCompleted() + 1);
            userRepository.save(user);

            // get completed task answer and set its reply markup to null and return it
            var newAnswerMessage = completedTaskAnswer(answerMessage, task);
            newAnswerMessage.setReplyMarkup(null);
            return newAnswerMessage;
        }

        // handling different task's states
        switch (taskState) {
            case "CREATING_NAME" -> {
                answerText = setTaskName(userMessage, task);

                if (answerText != null) {
                    answerMessage.setText(answerText);
                } else {
                    task.setState("CREATING_DESCRIPTION");
                }
            }
            case "CREATING_DESCRIPTION" -> {
                answerText = setTaskDescription(userMessage, task);

                if (answerText != null) {
                    answerMessage.setText(answerText);
                } else {
                    answerMessage.setReplyMarkup(MarkupUtils.getDateMarkup());
                    task.setState("CREATING_DATE");
                }
            }
            case "CREATING_DATE" -> {
                answerText = setTaskDate(userMessage, task);

                if (answerText != null) {
                    answerMessage.setText(answerText);
                } else {
                    task.setState("CREATING_PRIORITY");
                }
            }
            case "CREATING_PRIORITY" -> {
                answerText = setTaskPriority(userMessage, task);

                if (answerText != null) {
                    answerMessage.setText(answerText);
                } else {
                    task.setState("CREATING_DIFFICULTY");
                }
            }
            case "CREATING_DIFFICULTY" -> {
                answerText = setTaskDifficulty(userMessage, task);

                if (answerText != null) {
                    answerMessage.setText(answerText);
                } else {
                    task.setState("CREATING_TAG");
                }
            }
            case "CREATING_TAG" -> {
                answerText = setTaskTag(userMessage, task);

                if (answerText != null) {
                    answerMessage.setText(answerText);
                } else {
                    // set task's state to COMPLETED and save since we will return message
                    task.setState("COMPLETED");
                    taskRepository.save(task);

                    // add one to user's task completed count
                    user.setTaskCompleted(user.getTaskCompleted() + 1);
                    userRepository.save(user);

                    var message = completedTaskAnswer(answerMessage, task);
                    message.setReplyMarkup(null);
                    return message;
                }
            }
        }

        taskRepository.save(task);
        return answerMessage;
    }

    private String setTaskName(Message userMessage, Task task) {
        // checking if user's message has text, since he can send a picture of document
        if (!userMessage.hasText()) {
            return "Please, send a text for the name of your task";
        }
        task.setName(userMessage.getText());
        taskRepository.save(task);

        return null;
    }

    private String setTaskDescription(Message userMessage, Task task) {
        // checking if user's message has text, since he can send a picture of document
        if (!userMessage.hasText()) {

            return "Please, send a text for the description of your task";
        }

        task.setDescription(userMessage.getText());
        taskRepository.save(task);
        return null;
    }

    private String setTaskDate(Message userMessage, Task task) {
        String errorMessage = "Please enter the task target completion date in the format \"yyyy-MM-dd\", e.g. \"2023-04-30\"";
        // checking if user's message has text, since he can send a picture of document
        if (!userMessage.hasText()) {
            return errorMessage;
        }
        LocalDate date;
        // checking if user's message is a date. if true - parsing to LocalDate, if false - returning a corresponding message back to user
        try {
            //TODO add ability to choose date by typing "in X days"
            if(userMessage.getText().toLowerCase().contains("today")) {
                // Get the current date
                date = LocalDate.now();
            } else if (userMessage.getText().toLowerCase().contains("tomorrow")) {
                // Get the date of tomorrow
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

    private String setTaskPriority(Message userMessage, Task task) {
        // checking if user's message has text, since he can send a picture of document
        if (!userMessage.hasText()) {
            return "Please, send a number for the priority of your task";
        }

        int number;

        // Verifying that user's response is a number
        try {
            number = Integer.parseInt(userMessage.getText());
        } catch (NumberFormatException e) {
            return "Please, write a number from 1 to 6 for the priority.";
        }

        // Check if the number is within the allowed range of 1 to 6
        if (number >= 1 && number <= 6) {
            // Set the task priority to the user-provided number
            task.setPriority(number);
        } else if (number != 0) {
            // Inform the user that the priority must be within the allowed range of 1 to 6
            return "Allowed range for priority is 1-6.";
        }

        taskRepository.save(task);
        return null;
    }

    private String setTaskDifficulty(Message userMessage, Task task) {
        // checking if user's message has text, since he can send a picture of document
        if (!userMessage.hasText()) {
            return "Please, send a number in range of 0 to 7 for the difficulty.";
        }

        int number;

        // Verifying that user's response is a number
        try {
            number = Integer.parseInt(userMessage.getText());
        } catch (NumberFormatException e) {
            return "Please, send a number in range of 0 to 7 for the difficulty.";
        }

        // Check if the number is within the allowed range of 0 to 7
        if (number >= 0 && number <= 7) {
            // Set the task difficulty to the user-provided number
            task.setDifficulty(number);
        } else {
            // Inform the user that the priority must be within the allowed range of 0 to 7
            return "Allowed range for difficulty is 0-7.";
        }

        taskRepository.save(task);
        return null;
    }

    private String setTaskTag(Message userMessage, Task task) {
        // checking if user's message has text, since he can send a picture of document
        if (!userMessage.hasText()) {
            return "Please, send a text for the tag of your task";
        }
        task.setTag(userMessage.getText());

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
        User user = userUtils.getUserByTag(update);

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
        if(callbackData.length == 6) {
            if(taskSwitchStateToEdit(taskToEdit, update)) {
               return processGetTaskInDetail(update, operation);
            }

            return null;
        }

        return MessageUtils.editMessageGenerator(
                update,
                taskUtils.taskToStringInDetail(taskToEdit),
                getEditMarkup(callbackData, operation));
    }

    private boolean taskSwitchStateToEdit(Task task, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");

        switch (callbackData[5]) {
            case "NAME" -> {
                task.setState("EDITING_NAME");
                String text = String.format("Enter a new title for task \"%s\"", task.getName());
                log.trace("Sending answer to ProducerService");
                producerService.produceAnswer(
                        MessageUtils.sendMessageGenerator(
                                update,
                                text));
            }
            case "DESC" -> {
                task.setState("EDITING_DESCRIPTION");
                String text = String.format("Enter a new description for task \"%s\"", task.getName());
                log.trace("Sending answer to ProducerService");
                producerService.produceAnswer(
                        MessageUtils.sendMessageGenerator(
                                update,
                                text));
            }
            case "DATE" -> {
                task.setState("EDITING_DATE");
                String text = String.format("Enter a new date for task \"%s\"", task.getName());
                log.trace("Sending answer to ProducerService");
                producerService.produceAnswer(
                        MessageUtils.sendMessageGenerator(
                                update,
                                text,
                                MarkupUtils.getDateMarkupWithoutSkipCancelFinish()));
            }
            case "PRIOR" -> {
                task.setState("EDITING_PRIORITY");
                String text = String.format("""
                        Enter a new priority for task "%s"
                        
                        1 = Not important
                        2 = Low priority
                        3 = Medium priority
                        4 = High priority
                        5 = Very high priority
                        6 = Urgent priority""", task.getName());
                log.trace("Sending answer to ProducerService");
                producerService.produceAnswer(
                        MessageUtils.sendMessageGenerator(
                                update,
                                text));
            }
            case "DIFF" -> {
                task.setState("EDITING_DIFFICULTY");
                String text = String.format("""
                        Enter a new difficulty for task "%s"
                        
                        Enter a number in range of 0-7:
                        0 = No difficulty
                        1 = Very easy
                        2 = Easy
                        3 = Moderate
                        4 = Challenging
                        5 = Difficult
                        6 = Very Difficult
                        7 = Extremely difficult""", task.getName());
                log.trace("Sending answer to ProducerService");
                producerService.produceAnswer(
                        MessageUtils.sendMessageGenerator(
                                update,
                                text));
            }
            case "TAG" -> {
                task.setState("EDITING_TAG");
                String text = String.format("Enter a new tag for task \"%s\"", task.getName());
                log.trace("Sending answer to ProducerService");
                producerService.produceAnswer(
                        MessageUtils.sendMessageGenerator(
                                update,
                                text));
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
        //TODO
        log.trace(task.toString());
        Message msg = update.getMessage();
        String msgText = msg.getText();
        SendMessage answer = MessageUtils.sendMessageGenerator(update, "");
        String answerText;


        switch(task.getState()) {
            case "EDITING_NAME" -> {
                answerText = setTaskName(msg, task);

                if (answerText != null) {
                    answer.setText(answerText);
                } else {
                    answer.setText(String.format("Title of the task set to: \"%s\"", msgText));
                    task.setState("COMPLETED");
                }
            }
            case "EDITING_DESCRIPTION" -> {
                answerText = setTaskDescription(msg, task);

                if (answerText != null) {
                    answer.setText(answerText);
                } else {
                    answer.setText(String.format("Description of the task \"%s\" set to: \"%s\"",
                            task.getName(),
                            msgText));
                    task.setState("COMPLETED");
                }
            }
            case "EDITING_DATE" -> {
                answerText = setTaskDate(msg, task);

                if (answerText != null) {
                    answer.setText(answerText);
                } else {
                    answer.setText(String.format("Date of the task \"%s\" set to: \"%s\"",
                            task.getName(),
                            task.getTargetDate()));
                    task.setState("COMPLETED");
                }
            }
            case "EDITING_PRIORITY" -> {
                answerText = setTaskPriority(msg, task);

                if (answerText != null) {
                    answer.setText(answerText);
                } else {
                    answer.setText(String.format("Priority of the task \"%s\" set to: \"%s\"",
                            task.getName(),
                            taskUtils.getPriorityDescription(task.getPriority())));
                    task.setState("COMPLETED");
                }
            }
            case "EDITING_DIFFICULTY" -> {
                answerText = setTaskDifficulty(msg, task);

                if (answerText != null) {
                    answer.setText(answerText);
                } else {
                    answer.setText(String.format("Difficulty of the task \"%s\" set to: \"%s\"",
                            task.getName(),
                            taskUtils.getDifficultyDescription(task.getDifficulty())));
                    task.setState("COMPLETED");
                }
            }
            case "EDITING_TAG" -> {
                answerText = setTaskTag(msg, task);

                if (answerText != null) {
                    answer.setText(answerText);
                } else {
                    answer.setText(String.format("Tag of the task \"%s\" set to: \"%s\"",
                            task.getName(),
                            task.getTag()));
                    task.setState("COMPLETED");
                }
            }

        }

        log.trace(answer.getText());

        taskRepository.save(task);

        return answer;
    }

    private InlineKeyboardMarkup getEditMarkup(String[] callbackData, String operation) {
        int pageIndex = Integer.parseInt(callbackData[2]);
        int pageTaskIndex = Integer.parseInt(callbackData[3]);

        // setting up the keyboard
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> firstRow = new ArrayList<>();
        List<InlineKeyboardButton> secondRow = new ArrayList<>();

        // setting up buttons
        InlineKeyboardButton nameButton = new InlineKeyboardButton("Edit name");
        InlineKeyboardButton descButton = new InlineKeyboardButton("Edit description");
        InlineKeyboardButton dateButton = new InlineKeyboardButton("Edit date");
        InlineKeyboardButton priorityButton = new InlineKeyboardButton("Edit priority");
        InlineKeyboardButton difficultyButton = new InlineKeyboardButton("Edit difficulty");
        InlineKeyboardButton tagButton = new InlineKeyboardButton("Edit tag");
        InlineKeyboardButton cancelButton = new InlineKeyboardButton("Cancel");

        // setting callback data and adding buttons to keyboard
        nameButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/NAME", operation, pageIndex, pageTaskIndex));
        descButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/DESC", operation, pageIndex, pageTaskIndex));
        priorityButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/PRIOR", operation, pageIndex, pageTaskIndex));
        difficultyButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/DIFF", operation, pageIndex, pageTaskIndex));
        tagButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/TAG", operation, pageIndex, pageTaskIndex));
        dateButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/DATE", operation, pageIndex, pageTaskIndex));
        cancelButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/CANCEL", operation, pageIndex, pageTaskIndex));

        firstRow.add(nameButton);
        firstRow.add(descButton);
        firstRow.add(dateButton);
        secondRow.add(priorityButton);
        secondRow.add(difficultyButton);
        secondRow.add(tagButton);
        secondRow.add(cancelButton);

        keyboard.add(firstRow);
        keyboard.add(secondRow);

        return new InlineKeyboardMarkup(keyboard);
    }

    // DELETE methods



    @Override
    public SendMessage processDeleteAllCompletedTasksConfirmation(Update update) {
        // setting up keyboard
        List<InlineKeyboardButton> keyboard = new ArrayList<>();
        InlineKeyboardButton confirmButton = new InlineKeyboardButton("Confirm");
        confirmButton.setCallbackData("DELETE_ALL_COMPLETED/CONFIRM");
        keyboard.add(confirmButton);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(Collections.singletonList(keyboard));

        return MessageUtils.sendMessageGenerator(update,
                "Are you sure you want to do it? All your completed tasks will be deleted, hence this will also delete all statistics related to the tasks",
                markup);
    }

    @Override
    public EditMessageText processDeleteAllCompletedTasks(Update update) {
        User user = userUtils.getUserByTag(update);
        taskRepository.deleteAllCompletedTasks(user.getId());

        return MessageUtils.editMessageGenerator(update,
                "All completed tasks successfully deleted!");
    }

    @Override
    public SendMessage processDeleteAllTasksConfirmation(Update update) {
        // setting up keyboard
        List<InlineKeyboardButton> keyboard = new ArrayList<>();
        InlineKeyboardButton confirmButton = new InlineKeyboardButton("Confirm");
        confirmButton.setCallbackData("DELETE_ALL/CONFIRM");
        keyboard.add(confirmButton);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(Collections.singletonList(keyboard));

        return MessageUtils.sendMessageGenerator(update,
                "Are you sure you want to do it? ALL your tasks will be deleted, hence this will also delete all your statistics",
                markup);
    }

    @Override
    public EditMessageText processDeleteAllTasks(Update update) {
        User user = userUtils.getUserByTag(update);
        taskRepository.deleteAllTasks(user.getId());

        return MessageUtils.editMessageGenerator(update,
                "All your tasks now aresuccessfully deleted!");
    }

    @Override
    public EditMessageText processTasksDelete(Update update, String operation) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");
        int pageIndex = Integer.parseInt(callbackData[2]);
        int pageTaskIndex = Integer.parseInt(callbackData[3]);

        log.trace("callbackData: " + Arrays.toString(callbackData));

        // get user from database to later get needed task using user's id
        User user = userUtils.getUserByTag(update);

        // get overall task index with pageIndex * tasksPerPageAmount + pageTaskIndex formula
        int taskIndex = pageIndex * TASK_PER_PAGE + pageTaskIndex;

        // get list of tasks
        List<Task> tasks = getTasks(user, operation);

        if (tasks == null || tasks.size() == 0) {
            log.error("tasks is null");
            return null;
        }

        Task taskToDelete = tasks.get(taskIndex);
        taskRepository.delete(taskToDelete);

        return returnToAllTasks(update, operation);
    }

    private EditMessageText processTasksDeleteConfirm(Update update, String operation) {
        CallbackQuery query = update.getCallbackQuery();
        String[] callbackData = query.getData().split("/");
        int pageIndex = Integer.parseInt(callbackData[2]);
        int pageTaskIndex = Integer.parseInt(callbackData[3]);

        log.trace("callbackData: " + Arrays.toString(callbackData));

        String text = "Are you sure you wish to delete this task";

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

        return MessageUtils.editMessageGenerator(update, text, markup);
    }

    // GET methods

    @Override
    public EditMessageText processGetAllTasksNext(Update update, String operation) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");
        int pageIndex = Integer.parseInt(callbackData[2]);

        log.trace("callbackData: " + Arrays.toString(callbackData));

        pageIndex++;

        Map<String[], InlineKeyboardMarkup> pagesAndMarkup = getTasksTextAndMarkup(update, pageIndex, operation);

        assert pagesAndMarkup != null;
        String[] pages = pagesAndMarkup.keySet().iterator().next();
        InlineKeyboardMarkup markup = pagesAndMarkup.values().iterator().next();

        return MessageUtils.editMessageGenerator(update, pages[pageIndex], markup);
    }

    @Override
    public EditMessageText processGetAllTasksPrev(Update update, String operation) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");
        int pageIndex = Integer.parseInt(callbackData[2]);

        log.trace("callbackData: " + Arrays.toString(callbackData));

        pageIndex--;

        Map<String[], InlineKeyboardMarkup> pagesAndMarkup = getTasksTextAndMarkup(update, pageIndex, operation);

        assert pagesAndMarkup != null;
        String[] pages = pagesAndMarkup.keySet().iterator().next();
        InlineKeyboardMarkup markup = pagesAndMarkup.values().iterator().next();

        return MessageUtils.editMessageGenerator(update, pages[pageIndex], markup);
    }

    @Override
    public EditMessageText processGetTaskInDetail(Update update, String operation) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");

        if(callbackData.length < 5) {
            log.error("callbackData length must be 5 or more, but callbackData length is "+callbackData.length);
            return null;
        }

        log.trace("callbackData: " + Arrays.toString(callbackData));

        int pageIndex;
        int pageTaskIndex;

        try {
            pageIndex = Integer.parseInt(callbackData[2]);
            pageTaskIndex = Integer.parseInt(callbackData[3]);
        } catch (NumberFormatException e) {
            log.error("CallbackQuery error: "+e.getMessage());
            return null;
        }
        // get user from database to later get needed task using user's id
        User user = userUtils.getUserByTag(update);

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

        if(pageTaskIndex > TASK_PER_PAGE) {
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

        // setting up buttons
        InlineKeyboardButton prevButton = new InlineKeyboardButton("Previous");
        InlineKeyboardButton nextButton = new InlineKeyboardButton("Next");
        InlineKeyboardButton completeButton = new InlineKeyboardButton("Complete");
        InlineKeyboardButton uncompleteButton = new InlineKeyboardButton("Uncomplete");
        InlineKeyboardButton editButton = new InlineKeyboardButton("Edit");
        InlineKeyboardButton deleteButton = new InlineKeyboardButton("Delete");
        InlineKeyboardButton cancelButton = new InlineKeyboardButton("Cancel");

        // setting callback data and adding buttons to keyboard
        if(taskIndex > 0) {
            prevButton.setCallbackData(String.format("%s/TASK/%d/%d/PREV", operation, pageIndex, pageTaskIndex));
            prevNextButtons.add(prevButton);
        }
        if(taskIndex < tasks.size() - 1) {
            nextButton.setCallbackData(String.format("%s/TASK/%d/%d/NEXT", operation, pageIndex, pageTaskIndex));
            prevNextButtons.add(nextButton);
        }
        if(task.getStatus().equals("Completed")) {
            uncompleteButton.setCallbackData(String.format("%s/TASK/%d/%d/UNCOMPLETE", operation, pageIndex, pageTaskIndex));
            configurationButtons.add(uncompleteButton);
        } else {
            completeButton.setCallbackData(String.format("%s/TASK/%d/%d/COMPLETE", operation, pageIndex, pageTaskIndex));
            configurationButtons.add(completeButton);
        }
        editButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT", operation, pageIndex, pageTaskIndex));
        deleteButton.setCallbackData(String.format("%s/TASK/%d/%d/DELETE_CONFIRM", operation, pageIndex, pageTaskIndex));
        cancelButton.setCallbackData(String.format("%s/TASK/%d/%d/CANCEL", operation, pageIndex, pageTaskIndex));

        configurationButtons.add(editButton);
        configurationButtons.add(deleteButton);
        configurationButtons.add(cancelButton);

        keyboard.add(prevNextButtons);
        keyboard.add(configurationButtons);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return MessageUtils.editMessageGenerator(update, taskUtils.taskToStringInDetail(task), markup);
    }

    @Override
    public SendMessage processGetAllTasks(Update update, String operation) {
        int pageIndex = 0;

        Map<String[], InlineKeyboardMarkup> pagesAndMarkup = getTasksTextAndMarkup(update, pageIndex, operation);

        // handle case if user doesn't have any tasks yet
        if (pagesAndMarkup == null) {
            return MessageUtils.sendMessageGenerator(
                    update,
                    "You don't have any tasks yet.\n\nYou can create tasks using appropriate buttons"
            );
        }

        String[] pages = pagesAndMarkup.keySet().iterator().next();
        InlineKeyboardMarkup markup = pagesAndMarkup.values().iterator().next();

        return MessageUtils.sendMessageGenerator(update, pages[pageIndex], markup);
    }

    // CreateTask methods

    private SendMessage completedTaskAnswer(SendMessage answerMessage, Task task) {
        answerMessage.setReplyMarkup(null);
        answerMessage.setText(taskUtils.responseForEachState(task));

        return answerMessage;
    }

    // GetAllTasks methods

    private EditMessageText returnToAllTasks(Update update, String operation) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");
        int pageIndex = Integer.parseInt(callbackData[2]);

        Map<String[], InlineKeyboardMarkup> pagesAndMarkup = getTasksTextAndMarkup(update, pageIndex, operation);

        // handle case if user doesn't have any tasks yet (user can remove the task, so he could still have 0 tasks)
        if (pagesAndMarkup == null) {
            String text = "You don't have any tasks yet.\n\nYou can create tasks using appropriate buttons";
            return MessageUtils.editMessageGenerator(update, text);
        }

        String[] pages = pagesAndMarkup.keySet().iterator().next();
        InlineKeyboardMarkup markup = pagesAndMarkup.values().iterator().next();

        return MessageUtils.editMessageGenerator(update, pages[pageIndex], markup);
    }

    private Map<String[], InlineKeyboardMarkup> getTasksTextAndMarkup(Update update, int pageIndex, String operation) {
        // TODO maybe create a separate class to hold info, instead of Map
        User user = userUtils.getUserByTag(update);
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
        pages = new String[(int) Math.ceil( tasks.size() / (float) TASK_PER_PAGE)];
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

                if(pageNumber == 0 && hasUncompletedTasks) {
                    pages[pageNumber] = pages[pageNumber].concat("❌ *Uncompleted:*\n\n");
                }
            }

            //Mark completed/uncompleted
            if(hasCompletedTasks && noCompletedFlowYet && tasks.get(i).getStatus().equals("Completed")) {
                noCompletedFlowYet = false;

                pages[pageNumber] = pages[pageNumber].concat("*✅ Completed:*\n\n");
            }
            // get current task
            Task task = tasks.get(i);

            // get task name for a button, and limit it only to 24 characters
            String taskName = task.getName();
            if(taskName.length() > 64) {
                taskName =  taskName.substring(0, 61).concat("...");
            }

            // set up button for current task
            InlineKeyboardButton currentButton;

            if(task.getStatus().equals("Completed")) {
                currentButton = new InlineKeyboardButton("✅ ".concat(taskName));
            } else {
                currentButton = new InlineKeyboardButton("❌ ".concat(taskName));
            }
            currentButton.setCallbackData(String.format("%s/TASK/%d/%d/TASK", operation, pageNumber, taskNumber));

            if (pageNumber == pageIndex) {
                if (taskNumber <= TASK_PER_PAGE / 4 - 1 ) {
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
        if (pageIndex > 0 && pageIndex < pageNumber) {
            InlineKeyboardButton prevButton = new InlineKeyboardButton("<");
            InlineKeyboardButton nextButton = new InlineKeyboardButton(">");

            prevButton.setCallbackData(String.format("%s/PREV/%d/%d", operation, pageIndex, pageTaskIndex));
            nextButton.setCallbackData(String.format("%s/NEXT/%d/%d", operation, pageIndex, pageTaskIndex));

            mainRow.add(prevButton);
            mainRow.add(nextButton);
        } else if (pageIndex == 0 && pageIndex < pageNumber) {
            InlineKeyboardButton nextButton = new InlineKeyboardButton(">");
            nextButton.setCallbackData(String.format("%s/NEXT/%d/%d", operation, pageIndex, pageTaskIndex));
            mainRow.add(nextButton);
        } else if (pageIndex > 0 && pageIndex == pageNumber) {
            InlineKeyboardButton prevButton = new InlineKeyboardButton("<");
            prevButton.setCallbackData(String.format("%s/PREV/%d/%d", operation, pageIndex, pageTaskIndex));
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
                log.error("Unknown operation: " + operation);
                return null;
            }
        }
    }
}
