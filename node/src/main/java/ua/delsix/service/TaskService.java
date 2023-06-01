package ua.delsix.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface TaskService {
    SendMessage processCreateTask(Update update, SendMessage answerMessage);
    SendMessage processCreatingTask(Update update, SendMessage answerMessage);
    SendMessage processGetAllTasks(Update update, SendMessage answerMessage);
    EditMessageText processTasksEdit(Update update);
    EditMessageText processTasksDelete(Update update);
    EditMessageText processGetAllTasksNext(Update update);
    EditMessageText processGetAllTasksPrev(Update update);
    EditMessageText processGetTaskInDetail(Update update);
}
