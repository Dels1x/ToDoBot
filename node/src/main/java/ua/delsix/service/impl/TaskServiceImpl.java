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
import ua.delsix.utils.UserUtils;

import java.time.LocalDate;
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
        Task task = taskOptional.get();
        String taskState = task.getState();

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
                        Now let's create a description for your task, if you want to.

                         If you don't need one - simply press the skip button
                         
                         Once you done with creating the task - just press finish button""");


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
                        Now let's set a priority for your task - a number in range of 1-6.

                         If you don't want task to have a priority - press the skip button or type in 0.
                         
                         Once you done with creating the task - just press finish button""");
            }
            case "CREATING_PRIORITY" -> {
                //TODO

                // checking if user's message has text, since he can send a picture of document
                if(!userMessage.hasText()) {
                    answerMessage.setText("Please, send a number for the priority of your task");
                }

                task.setDescription(userMessage.getText());
                task.setState("CREATING_DATE");
                taskRepository.save(task);
                ReplyKeyboardMarkup markup = getCancelSkipFinishMarkup();
                answerMessage.setReplyMarkup(markup);
                answerMessage.setText("""
                        Now let's set a priority for your task - a number in range of 1-6.

                         If you don't want task to have a priority - press the skip button or type in 0.
                         
                         Once you done with creating the task - just press finish button""");
            }
            case "CREATING_DATE" -> {
                //TODO

                // checking if user's message has text, since he can send a picture of document
                if(!userMessage.hasText()) {
                    answerMessage.setText("Please, send a text for the description of your task");
                }

                task.setDescription(userMessage.getText());
                task.setState("CREATING_DIFFICULTY");
                taskRepository.save(task);
                ReplyKeyboardMarkup markup = getCancelSkipFinishMarkup();
                answerMessage.setReplyMarkup(markup);
                answerMessage.setText("""
                        Now let's set a priority for your task - a number in range of 1-6.

                         If you don't want task to have a priority - press the skip button or type in 0.
                         
                         Once you done with creating the task - just press finish button""");
            }
            case "CREATING_DIFFICULTY" -> {
                //TODO

                // checking if user's message has text, since he can send a picture of document
                if(!userMessage.hasText()) {
                    answerMessage.setText("Please, send a text for the description of your task");
                }

                task.setDescription(userMessage.getText());
                task.setState("CREATING_TAG");
                taskRepository.save(task);
                ReplyKeyboardMarkup markup = getCancelSkipFinishMarkup();
                answerMessage.setReplyMarkup(markup);
                answerMessage.setText("""
                        Now let's set a priority for your task - a number in range of 1-6.

                         If you don't want task to have a priority - press the skip button or type in 0.
                         
                         Once you done with creating the task - just press finish button""");
            }
            case "CREATING_TAG" -> {
                //TODO

                // checking if user's message has text, since he can send a picture of document
                if(!userMessage.hasText()) {
                    answerMessage.setText("Please, send a text for the description of your task");
                }

                task.setDescription(userMessage.getText());
                task.setState("CREATING_SUBTASK");
                taskRepository.save(task);
                ReplyKeyboardMarkup markup = getCancelSkipFinishMarkup();
                answerMessage.setReplyMarkup(markup);
                answerMessage.setText("""
                        Now let's set a priority for your task - a number in range of 1-6.

                         If you don't want task to have a priority - press the skip button or type in 0.
                         
                         Once you done with creating the task - just press finish button""");
            }
            case "CREATING_SUBTASK" -> {
                //TODO

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
                        Now let's set a priority for your task - a number in range of 1-6.

                         If you don't want task to have a priority - press the skip button or type in 0.
                         
                         Once you done with creating the task - just press finish button""");
            }
        }

        return answerMessage;
    }

    @Override
    public SendMessage processEditingTask(Update update, SendMessage answerMessage) {

        return null;
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
