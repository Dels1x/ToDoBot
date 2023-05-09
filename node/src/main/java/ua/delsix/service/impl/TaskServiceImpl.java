package ua.delsix.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ua.delsix.entity.Task;
import ua.delsix.repository.TaskRepository;
import ua.delsix.service.TaskService;
import ua.delsix.service.enums.ServiceCommand;
import ua.delsix.utils.TaskUtils;
import ua.delsix.utils.UserUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
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
    public SendMessage processCreateTask(Update update, SendMessage answerMessage) {
        // creating a new task
        Task newTask = Task.builder()
                .userId(userUtils.getUserByTag(update).getId())
                .name("Unnamed " + (taskRepository.count()+1))
                .state("CREATING_NAME")
                .createdAt(LocalDate.now())
                .build();

        // saving the task to the table
        taskRepository.save(newTask);

        ReplyKeyboardMarkup markup = getCancelSkipFinishMarkup();
        answerMessage.setReplyMarkup(markup);
        answerMessage.setText("Alright, a new task. To proceed, let's choose a name for the task first.");

        // sending answerMessage back to MainService
        return answerMessage;
    }

    @Override
    public SendMessage processCreatingTask(Update update, SendMessage answerMessage) {
        Optional<Task> taskOptional = taskRepository.findTopByUserIdOrderByIdDesc(userUtils.getUserByTag(update).getId());
        if(taskOptional.isEmpty()) {
            log.error("User does not have any tasks");
            answerMessage.setText("Seems like there is an issue with the bot.\n\n Please, come back later");
            return answerMessage;
        }
        Message userMessage = update.getMessage();
        ServiceCommand userCommand = ServiceCommand.fromValue(userMessage.getText());
        Task task = taskOptional.get();
        String taskState = task.getState();

        if(userCommand.equals(ServiceCommand.CANCEL)) {
            taskRepository.deleteById(task.getId());
            answerMessage.setText("Creation of the task was successfully cancelled.");
            return answerMessage;
        } else if(userCommand.equals(ServiceCommand.SKIP)) {
            int stateId = TaskUtils.states.indexOf(taskState);
            taskState = TaskUtils.states.get(stateId + 1);
            task.setState(taskState);
        } else if(userCommand.equals(ServiceCommand.FINISH)) {
            return completedTaskAnswer(answerMessage, task);
        }

        // handling different task's states
        switch (taskState) {
            case "CREATING_NAME" -> {
                // checking if user's message has text, since he can send a picture of document
                if(!userMessage.hasText()) {
                    answerMessage.setText("Please, send a text for the name of your task");
                }
                task.setName(userMessage.getText());
                task.setState("CREATING_DESCRIPTION");
                taskRepository.save(task);
                ReplyKeyboardMarkup markup = getCancelSkipFinishMarkup();
                answerMessage.setReplyMarkup(markup);
                answerMessage.setText("""
                        Now let's create a description for your task, if you want to.""");


            }
            case "CREATING_DESCRIPTION" -> {
                // checking if user's message has text, since he can send a picture of document
                if(!userMessage.hasText()) {

                    answerMessage.setText("Please, send a text for the description of your task");
                }

                task.setDescription(userMessage.getText());
                task.setState("CREATING_PRIORITY");
                taskRepository.save(task);
                ReplyKeyboardMarkup markup = getCancelSkipFinishMarkup();
                answerMessage.setReplyMarkup(markup);
                answerMessage.setText("""
                        We can also set a priority for your task - a number in range of 1-6.

                        If you don't want task to have a priority - press the skip button or type in "0".""");
            }
            case "CREATING_PRIORITY" -> {
                // checking if user's message has text, since he can send a picture of document
                if(!userMessage.hasText()) {
                    answerMessage.setText("Please, send a number for the priority of your task");
                }

                int number;

                // Verifying that user's response is a number
                try {
                    number = Integer.parseInt(userMessage.getText());
                } catch (NumberFormatException e ) {
                    answerMessage.setText("Please, write a number from 1 to 6 for the priority.");
                    ReplyKeyboardMarkup markup = getCancelSkipFinishMarkup();
                    answerMessage.setReplyMarkup(markup);
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
                    ReplyKeyboardMarkup markup = getCancelSkipFinishMarkup();
                    answerMessage.setReplyMarkup(markup);
                    return answerMessage;
                }

                task.setState("CREATING_DATE");
                taskRepository.save(task);
                ReplyKeyboardMarkup markup = getCancelSkipFinishMarkup();
                answerMessage.setReplyMarkup(markup);
                answerMessage.setText("""
                        You can set a target completion date or a deadline for your task using the format "dd.MM.yyyy" (e.g. "30.04.2023")""");
            }
            case "CREATING_DATE" -> {
                // checking if user's message has text, since he can send a picture of document
                if(!userMessage.hasText()) {
                    answerMessage.setText("Please enter the task target completion date in the format \"dd.MM.yyyy\", e.g. \"30.04.2023\".");
                }
                LocalDate date;
                // checking if user's message is a date. if true - parsing to LocalDate, if false - returning a corresponding message back to user
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    date = LocalDate.parse(userMessage.getText(), formatter);
                } catch (DateTimeParseException e) {
                    answerMessage.setText("Please enter the task target completion date in the format \"dd.MM.yyyy\", e.g. \"30.04.2023\"");
                    ReplyKeyboardMarkup markup = getCancelSkipFinishMarkup();
                    answerMessage.setReplyMarkup(markup);
                    return answerMessage;
                }

                task.setTargetDate(date);
                task.setState("CREATING_DIFFICULTY");
                taskRepository.save(task);
                ReplyKeyboardMarkup markup = getCancelSkipFinishMarkup();
                answerMessage.setReplyMarkup(markup);
                answerMessage.setText("""
                        If you want to set a specific difficulty for your task - we can also do that.
                        a number in range of 0-7. (0 - No difficulty; 1 - Very easy; 2 - Easy; 3 - Moderate; 4 - Challenging; 5 - Difficult; 6 - Very Difficult; 7 - Extremely difficult)""");
            }
            case "CREATING_DIFFICULTY" -> {
                // checking if user's message has text, since he can send a picture of document
                if(!userMessage.hasText()) {
                    answerMessage.setText("Please, send a number in range of 0 to 7 for the difficulty.");
                }

                int number;

                // Verifying that user's response is a number
                try {
                    number = Integer.parseInt(userMessage.getText());
                } catch (NumberFormatException e ) {
                    answerMessage.setText("Please, send a number in range of 0 to 7 for the difficulty.");
                    ReplyKeyboardMarkup markup = getCancelSkipFinishMarkup();
                    answerMessage.setReplyMarkup(markup);
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
                    ReplyKeyboardMarkup markup = getCancelSkipFinishMarkup();
                    answerMessage.setReplyMarkup(markup);
                    return answerMessage;
                }

                task.setState("CREATING_TAG");
                taskRepository.save(task);
                ReplyKeyboardMarkup markup = getCancelSkipFinishMarkup();
                answerMessage.setReplyMarkup(markup);
                answerMessage.setText("""
                        If you want to - you can set a specific tag for the task. It could be something like: (Goals, Programming, Chores etc.).""");
            }
            case "CREATING_TAG" -> {
                // checking if user's message has text, since he can send a picture of document
                if(!userMessage.hasText()) {
                    answerMessage.setText("Please, send a text for the tag of your task");
                }
                task.setTag(userMessage.getText());
                return completedTaskAnswer(answerMessage, task);
            }
        }

        return answerMessage;
    }

    @Override
    public SendMessage processEditingTask(Update update, SendMessage answerMessage) {

        return null;
    }

    private SendMessage completedTaskAnswer(SendMessage answerMessage, Task task) {
        task.setState(TaskUtils.states.get(TaskUtils.states.size() - 1));
        taskRepository.save(task);

        ReplyKeyboardMarkup markup = getCancelSkipFinishMarkup();
        answerMessage.setReplyMarkup(markup);
        answerMessage.setText(String.format("""
                        Your task is successfully created!
                        
                        If there is need, you can also create subtasks for this task using this task's id (%d)
                        
                        This is how your task looks like:
                        #%d - %s
                         %s
                        Tag: %s | Priority: %d | Difficulty: %d | Due %s
                        Current state: %s""",
                task.getId(),
                task.getId(), task.getName(),
                task.getDescription(),
                task.getTag(), task.getPriority(), task.getDifficulty(), task.getTargetDate().toString(),
                task.getStatus()));
        //TODO use TaskUtils taskToString() for creating answer message text

        return answerMessage;
    }

    private ReplyKeyboardMarkup getCancelSkipFinishMarkup() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Skip");
        row1.add("Cancel");
        row1.add("Finish");
        keyboard.add(row1);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private ReplyKeyboardMarkup getCancelFinishMarkup() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Cancel");
        row1.add("Finish");
        keyboard.add(row1);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
