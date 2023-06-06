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

    public TaskServiceImpl(UserUtils userUtils, TaskRepository taskRepository, UserRepository userRepository, TaskUtils taskUtils, ProducerService producerService) {
        this.userUtils = userUtils;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskUtils = taskUtils;
        this.producerService = producerService;
    }

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
        user.setTaskCount(user.getTaskCount() + 1);
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

        answerMessage.setText(taskUtils.responseForEachState(task));
        ReplyKeyboardMarkup markup = MarkupUtils.getCancelSkipFinishMarkup();
        answerMessage.setReplyMarkup(markup);

        log.trace("Task: " + task);

        if (userCommand.equals(ServiceCommand.CANCEL)) {
            // Delete task from tasks table
            taskRepository.deleteById(task.getId());

            answerMessage.setText("Creation of the task was successfully cancelled.");
            answerMessage.setReplyMarkup(null);

            // subtract one from user's tasks count and save it
            user.setTaskCount(user.getTaskCount() - 1);
            userRepository.save(user);

            return answerMessage;
        } else if (userCommand.equals(ServiceCommand.SKIP)) {
            int stateId = TaskUtils.states.indexOf(taskState);
            taskState = TaskUtils.states.get(stateId + 1);
            task.setState(taskState);
            taskRepository.save(task);

            if (taskState.equals("COMPLETED")) {
                answerMessage.setReplyMarkup(null);
            }

            return answerMessage;
        } else if (userCommand.equals(ServiceCommand.FINISH)) {
            // set task's state to COMPLETED
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
                task.setState("CREATING_DESCRIPTION");

                // checking if user's message has text, since he can send a picture of document
                if (!userMessage.hasText()) {
                    answerMessage.setText("Please, send a text for the name of your task");
                }
                task.setName(userMessage.getText());
                taskRepository.save(task);
            }
            case "CREATING_DESCRIPTION" -> {
                // checking if user's message has text, since he can send a picture of document
                if (!userMessage.hasText()) {

                    answerMessage.setText("Please, send a text for the description of your task");
                }

                task.setDescription(userMessage.getText());
                task.setState("CREATING_PRIORITY");
                taskRepository.save(task);
            }
            case "CREATING_PRIORITY" -> {
                // checking if user's message has text, since he can send a picture of document
                if (!userMessage.hasText()) {
                    answerMessage.setText("Please, send a number for the priority of your task");
                }

                int number;

                // Verifying that user's response is a number
                try {
                    number = Integer.parseInt(userMessage.getText());
                } catch (NumberFormatException e) {
                    answerMessage.setText("Please, write a number from 1 to 6 for the priority.");
                    return answerMessage;
                }

                // Check if the number is within the allowed range of 1 to 6
                if (number >= 1 && number <= 6) {
                    // Set the task priority to the user-provided number
                    task.setPriority(number);
                } else if (number == 0) {
                    // Do nothing, since the user provided 0 as the priority
                } else {
                    // Inform the user that the priority must be within the allowed range of 1 to 6
                    answerMessage.setText("Allowed range for priority is 1-6.");
                    // Set the reply markup to include the "Cancel", "Skip", and "Finish" buttons
                    return answerMessage;
                }

                task.setState("CREATING_DATE");
                taskRepository.save(task);
            }
            case "CREATING_DATE" -> {
                // checking if user's message has text, since he can send a picture of document
                if (!userMessage.hasText()) {
                    answerMessage.setText("Please enter the task target completion date in the format \"dd.MM.yyyy\", e.g. \"30.04.2023\".");
                }
                LocalDate date;
                // checking if user's message is a date. if true - parsing to LocalDate, if false - returning a corresponding message back to user
                try {
                    if(userMessage.getText().toLowerCase().contains("today")) {
                        date = LocalDate.now();
                    } else {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                        date = LocalDate.parse(userMessage.getText(), formatter);
                    }
                } catch (DateTimeParseException e) {
                    answerMessage.setText("Please enter the task target completion date in the format \"dd.MM.yyyy\", e.g. \"30.04.2023\"");
                    return answerMessage;
                }

                task.setTargetDate(date);
                task.setState("CREATING_DIFFICULTY");
                taskRepository.save(task);
            }
            case "CREATING_DIFFICULTY" -> {
                // checking if user's message has text, since he can send a picture of document
                if (!userMessage.hasText()) {
                    answerMessage.setText("Please, send a number in range of 0 to 7 for the difficulty.");
                }

                int number;

                // Verifying that user's response is a number
                try {
                    number = Integer.parseInt(userMessage.getText());
                } catch (NumberFormatException e) {
                    answerMessage.setText("Please, send a number in range of 0 to 7 for the difficulty.");
                    return answerMessage;
                }

                // Check if the number is within the allowed range of 0 to 7
                if (number >= 0 && number <= 7) {
                    // Set the task difficulty to the user-provided number
                    task.setDifficulty(number);
                } else {
                    // Inform the user that the priority must be within the allowed range of 0 to 7
                    answerMessage.setText("Allowed range for difficulty is 0-7.");
                    // Set the reply markup to include the "Cancel", "Skip", and "Finish" buttons
                    return answerMessage;
                }

                task.setState("CREATING_TAG");
                taskRepository.save(task);
            }
            case "CREATING_TAG" -> {
                // checking if user's message has text, since he can send a picture of document
                if (!userMessage.hasText()) {
                    answerMessage.setText("Please, send a text for the tag of your task");
                }
                task.setTag(userMessage.getText());

                // set task's state to COMPLETED
                task.setState("COMPLETED");
                taskRepository.save(task);

                // add 1 to user's completed tasks count
                user.setTaskCompleted(user.getTaskCompleted() + 1);
                userRepository.save(user);

                var message = completedTaskAnswer(answerMessage, task);
                message.setReplyMarkup(null);
                return message;
            }
        }

        return answerMessage;
    }

    // EDIT methods

    @Override
    public EditMessageText processTasksEdit(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");
        int pageIndex = Integer.parseInt(callbackData[2]);
        int pageTaskIndex = Integer.parseInt(callbackData[3]);

        log.trace("callbackData: " + Arrays.toString(callbackData));

        // get user from database to later get needed task using user's id
        User user = userUtils.getUserByTag(update);

        // get overall task index with pageIndex * tasksPerPageAmount + pageTaskIndex formula
        int taskIndex = pageIndex * 8 + pageTaskIndex;

        // get task to edit
        Task taskToEdit = taskRepository.findAllByUserIdSortedByTargetStatusAndDateAndId(user.getId()).get(taskIndex);

        // calling editTask method, if callbackData presses any edit button and return null to MainService
        if(callbackData.length == 6) {
            if(taskSwitchStateToEdit(taskToEdit, update)) {
               return processGetTaskInDetail(update);
            }

            return null;
        }

        return MessageUtils.editMessageGenerator(
                update,
                taskUtils.taskToStringInDetail(taskToEdit),
                getEditMarkup(callbackData));
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
            case "PRIOR" -> {
                task.setState("EDITING_PRIORITY");
                String text = String.format("Enter a new priority for task \"%s\"", task.getName());
                log.trace("Sending answer to ProducerService");
                producerService.produceAnswer(
                        MessageUtils.sendMessageGenerator(
                                update,
                                text));
            }
            case "DIFF" -> {
                task.setState("EDITING_DIFFICULTY");
                String text = String.format("Enter a new difficulty for task \"%s\"", task.getName());
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
            case "DATE" -> {
                task.setState("EDITING_DATE");
                String text = String.format("Enter a new date for task \"%s\"", task.getName());
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

        return MessageUtils.sendMessageGenerator(
                update,
                ""
        );
    }

    private InlineKeyboardMarkup getEditMarkup(String[] callbackData) {
        int pageIndex = Integer.parseInt(callbackData[2]);
        int pageTaskIndex = Integer.parseInt(callbackData[3]);

        // setting up the keyboard
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> firstRow = new ArrayList<>();
        List<InlineKeyboardButton> secondRow = new ArrayList<>();

        // setting up buttons
        InlineKeyboardButton nameButton = new InlineKeyboardButton("Edit name");
        InlineKeyboardButton descButton = new InlineKeyboardButton("Edit description");
        InlineKeyboardButton priorityButton = new InlineKeyboardButton("Edit priority");
        InlineKeyboardButton difficultyButton = new InlineKeyboardButton("Edit difficulty");
        InlineKeyboardButton tagButton = new InlineKeyboardButton("Edit tag");
        InlineKeyboardButton dateButton = new InlineKeyboardButton("Edit date");
        InlineKeyboardButton cancelButton = new InlineKeyboardButton("Cancel");

        // setting callback data and adding buttons to keyboard
        nameButton.setCallbackData(String.format("GET_ALL_TASKS/TASK/%d/%d/EDIT/NAME", pageIndex, pageTaskIndex));
        descButton.setCallbackData(String.format("GET_ALL_TASKS/TASK/%d/%d/EDIT/DESC", pageIndex, pageTaskIndex));
        priorityButton.setCallbackData(String.format("GET_ALL_TASKS/TASK/%d/%d/EDIT/PRIOR", pageIndex, pageTaskIndex));
        difficultyButton.setCallbackData(String.format("GET_ALL_TASKS/TASK/%d/%d/EDIT/DIFF", pageIndex, pageTaskIndex));
        tagButton.setCallbackData(String.format("GET_ALL_TASKS/TASK/%d/%d/EDIT/TAG", pageIndex, pageTaskIndex));
        dateButton.setCallbackData(String.format("GET_ALL_TASKS/TASK/%d/%d/EDIT/DATE", pageIndex, pageTaskIndex));
        cancelButton.setCallbackData(String.format("GET_ALL_TASKS/TASK/%d/%d/EDIT/CANCEL", pageIndex, pageTaskIndex));

        firstRow.add(nameButton);
        firstRow.add(descButton);
        firstRow.add(priorityButton);
        secondRow.add(difficultyButton);
        secondRow.add(tagButton);
        secondRow.add(dateButton);
        secondRow.add(cancelButton);

        keyboard.add(firstRow);
        keyboard.add(secondRow);

        return new InlineKeyboardMarkup(keyboard);
    }

    // DELETE methods

    @Override
    public EditMessageText processTasksDelete(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");
        int pageIndex = Integer.parseInt(callbackData[2]);
        int pageTaskIndex = Integer.parseInt(callbackData[3]);

        log.trace("callbackData: " + Arrays.toString(callbackData));

        // get user from database to later get needed task using user's id
        User user = userUtils.getUserByTag(update);

        // get overall task index with pageIndex * tasksPerPageAmount + pageTaskIndex formula
        int taskIndex = pageIndex * 8 + pageTaskIndex;

        Task taskToDelete = taskRepository.findAllByUserIdSortedByTargetStatusAndDateAndId(user.getId()).get(taskIndex);
        taskRepository.delete(taskToDelete);

        return returnToAllTasks(update);
    }

    private EditMessageText processTasksDeleteConfirm(Update update) {
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

        confirmButton.setCallbackData(String.format("GET_ALL_TASKS/TASK/%d/%d/DELETE", pageIndex, pageTaskIndex));
        cancelButton.setCallbackData(String.format("GET_ALL_TASKS/TASK/%d/%d", pageIndex, pageTaskIndex));

        buttons.add(confirmButton);
        buttons.add(cancelButton);

        keyboard.add(buttons);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboard);

        return MessageUtils.editMessageGenerator(update, text, markup);
    }

    // GET methods

    @Override
    public EditMessageText processGetAllTasksNext(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");
        int pageIndex = Integer.parseInt(callbackData[2]);

        log.trace("callbackData: " + Arrays.toString(callbackData));

        pageIndex++;

        Map<String[], InlineKeyboardMarkup> pagesAndMarkup = getTasksTextAndMarkup(update, pageIndex);

        assert pagesAndMarkup != null;
        String[] pages = pagesAndMarkup.keySet().iterator().next();
        InlineKeyboardMarkup markup = pagesAndMarkup.values().iterator().next();

        return MessageUtils.editMessageGenerator(update, pages[pageIndex], markup);
    }

    @Override
    public EditMessageText processGetAllTasksPrev(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");
        int pageIndex = Integer.parseInt(callbackData[2]);

        log.trace("callbackData: " + Arrays.toString(callbackData));

        pageIndex--;

        Map<String[], InlineKeyboardMarkup> pagesAndMarkup = getTasksTextAndMarkup(update, pageIndex);

        assert pagesAndMarkup != null;
        String[] pages = pagesAndMarkup.keySet().iterator().next();
        InlineKeyboardMarkup markup = pagesAndMarkup.values().iterator().next();

        return MessageUtils.editMessageGenerator(update, pages[pageIndex], markup);
    }

    @Override
    public EditMessageText processGetTaskInDetail(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");

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
        List<Task> tasks = taskRepository.findAllByUserIdSortedByTargetStatusAndDateAndId(user.getId());

        // handling different buttons
        switch (callbackData[4]) {
            case "NEXT" -> pageTaskIndex++;
            case "PREV" -> pageTaskIndex--;
            case "CANCEL" -> {
                return returnToAllTasks(update);
            }
            case "DELETE_CONFIRM" -> {
                return processTasksDeleteConfirm(update);
            }
            case "DELETE" -> {
                return processTasksDelete(update);
            }
            case "EDIT" -> {
                // doing checks, so that if 6th value is CANCEL, it doesn't go on never-ending loop
                if (callbackData.length == 5 ||
                        (callbackData.length == 6 && !callbackData[5].equals("CANCEL"))) {
                    return processTasksEdit(update);
                }
            }
        }

        if(pageTaskIndex > 8) {
            pageTaskIndex = 0;
            pageIndex++;
        }

        // get overall task index with pageIndex * tasksPerPageAmount + pageTaskIndex formula
        int taskIndex = pageIndex * 8 + pageTaskIndex;

        if (taskIndex < 0 || taskIndex >= tasks.size()) {
            return null;
        }
        // get needed task
        Task task = tasks.get(taskIndex);

        // handle a button to complete a task
        if (callbackData[4].equals("COMPLETE")) {
            task.setStatus("Completed");
            taskRepository.save(task);
        } else if (callbackData[4].equals("UNCOMPLETE")) {
            task.setStatus("Uncompleted");
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
            prevButton.setCallbackData(String.format("GET_ALL_TASKS/TASK/%d/%d/PREV", pageIndex, pageTaskIndex));
            prevNextButtons.add(prevButton);
        }
        if(taskIndex < tasks.size() - 1) {
            nextButton.setCallbackData(String.format("GET_ALL_TASKS/TASK/%d/%d/NEXT", pageIndex, pageTaskIndex));
            prevNextButtons.add(nextButton);
        }
        if(task.getStatus().equals("Completed")) {
            uncompleteButton.setCallbackData(String.format("GET_ALL_TASKS/TASK/%d/%d/UNCOMPLETE", pageIndex, pageTaskIndex));
            configurationButtons.add(uncompleteButton);
        } else {
            completeButton.setCallbackData(String.format("GET_ALL_TASKS/TASK/%d/%d/COMPLETE", pageIndex, pageTaskIndex));
            configurationButtons.add(completeButton);
        }
        editButton.setCallbackData(String.format("GET_ALL_TASKS/TASK/%d/%d/EDIT", pageIndex, pageTaskIndex));
        deleteButton.setCallbackData(String.format("GET_ALL_TASKS/TASK/%d/%d/DELETE_CONFIRM", pageIndex, pageTaskIndex));
        cancelButton.setCallbackData(String.format("GET_ALL_TASKS/TASK/%d/%d/CANCEL", pageIndex, pageTaskIndex));

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
    public SendMessage processGetAllTasks(Update update) {
        int pageIndex = 0;

        Map<String[], InlineKeyboardMarkup> pagesAndMarkup = getTasksTextAndMarkup(update, pageIndex);

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
        ReplyKeyboardMarkup markup = MarkupUtils.getCancelSkipFinishMarkup();
        answerMessage.setReplyMarkup(markup);
        answerMessage.setText(taskUtils.responseForEachState(task));

        return answerMessage;
    }

    // GetAllTasks methods

    private EditMessageText returnToAllTasks(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String[] callbackData = callbackQuery.getData().split("/");
        int pageIndex = Integer.parseInt(callbackData[2]);

        Map<String[], InlineKeyboardMarkup> pagesAndMarkup = getTasksTextAndMarkup(update, pageIndex);

        // handle case if user doesn't have any tasks yet (user can remove the task, so he could still have 0 tasks)
        if (pagesAndMarkup == null) {
            String text = "You don't have any tasks yet.\n\nYou can create tasks using appropriate buttons";
            return MessageUtils.editMessageGenerator(update, text);
        }

        String[] pages = pagesAndMarkup.keySet().iterator().next();
        InlineKeyboardMarkup markup = pagesAndMarkup.values().iterator().next();

        return MessageUtils.editMessageGenerator(update, pages[pageIndex], markup);
    }

    private Map<String[], InlineKeyboardMarkup> getTasksTextAndMarkup(Update update, int pageIndex) {
        // TODO maybe create a separate class to hold info, instead of Map
        User user = userUtils.getUserByTag(update);
        List<Task> tasks = taskRepository.findAllByUserIdSortedByTargetStatusAndDateAndId(user.getId());
        String[] pages = new String[(int) Math.ceil(tasks.size() / 8.0)];
        int pageTaskIndex = 0;

        if (tasks.size() == 0) {
            return null;
        }

        //keyboard setup
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // row with prev and next buttons
        List<InlineKeyboardButton> mainRow = new ArrayList<>();

        // rows with tasks
        List<InlineKeyboardButton> secondRow = new ArrayList<>();
        List<InlineKeyboardButton> thirdRow = new ArrayList<>();

        // creating list of pages full of tasks
        int pageNumber = -1;
        int taskNumber = 0;
        boolean noCompletedFlowYet = true;
        for (int i = 0; i < tasks.size(); i++) {
            if (i % 8 == 0) {
                pageNumber++;
                taskNumber = 0;
                pages[pageNumber] = "Tasks:\n\n";

                if(pageNumber == 0) {
                    pages[pageNumber] = pages[pageNumber].concat("❌ Uncompleted:\n\n");
                }
            }

            //Mark completed/uncompleted
            if(noCompletedFlowYet && tasks.get(i).getStatus().equals("Completed")) {
                noCompletedFlowYet = false;

                pages[pageNumber] = pages[pageNumber].concat("✅ Completed:\n\n");
            }
            // get current task
            Task task = tasks.get(i);

            // set up button for current task
            InlineKeyboardButton currentButton = new InlineKeyboardButton(task.getName());
            currentButton.setCallbackData(String.format("GET_ALL_TASKS/TASK/%d/%d/TASK", pageNumber, taskNumber));

            if (pageNumber == pageIndex) {
                if (taskNumber >= 4) {
                    thirdRow.add(currentButton);
                } else {
                    secondRow.add(currentButton);
                }
            }

            pages[pageNumber] = pages[pageNumber].concat(taskUtils.taskToString(task) + "\n\n");
            taskNumber++;
        }

        // setting up row with prev next buttons
        if (pageIndex > 0 && pageIndex < pageNumber) {
            InlineKeyboardButton prevButton = new InlineKeyboardButton("<");
            InlineKeyboardButton nextButton = new InlineKeyboardButton(">");

            prevButton.setCallbackData(String.format("GET_ALL_TASKS/PREV/%d/%d", pageIndex, pageTaskIndex));
            nextButton.setCallbackData(String.format("GET_ALL_TASKS/NEXT/%d/%d", pageIndex, pageTaskIndex));

            mainRow.add(prevButton);
            mainRow.add(nextButton);
        } else if (pageIndex == 0 && pageIndex < pageNumber) {
            InlineKeyboardButton nextButton = new InlineKeyboardButton(">");
            nextButton.setCallbackData(String.format("GET_ALL_TASKS/NEXT/%d/%d", pageIndex, pageTaskIndex));
            mainRow.add(nextButton);
        } else if (pageIndex > 0 && pageIndex == pageNumber) {
            InlineKeyboardButton prevButton = new InlineKeyboardButton("<");
            prevButton.setCallbackData(String.format("GET_ALL_TASKS/PREV/%d/%d", pageIndex, pageTaskIndex));
            mainRow.add(prevButton);
        }

        // finish setting up keyboard
        keyboard.add(mainRow);
        keyboard.add(secondRow);
        keyboard.add(thirdRow);
        markup.setKeyboard(keyboard);

        // creating map to return pages and markup at the same time
        Map<String[], InlineKeyboardMarkup> map = new HashMap<>();
        map.put(pages, markup);
        return map;
    }

}
