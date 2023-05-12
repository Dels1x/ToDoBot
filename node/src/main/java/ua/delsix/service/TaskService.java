package ua.delsix.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface TaskService {
    SendMessage processCreateTask(Update update, SendMessage answerMessage);
    SendMessage processCreatingTask(Update update, SendMessage answerMessage);
    SendMessage processEditingTask(Update update, SendMessage answerMessage);
    SendMessage processGetAllTasks(Update update, SendMessage answerMessage);
    SendMessage processDeleteTask(Update update, SendMessage answerMessage);
}
