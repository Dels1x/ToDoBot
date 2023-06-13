package ua.delsix.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.delsix.entity.Task;

public interface TaskService {
    SendMessage processCreateTask(Update update);
    SendMessage processCreatingTask(Update update);
    SendMessage editTask(Update update, Task task);
    SendMessage processGetAllTasks(Update update, String operation);
    EditMessageText processTasksEdit(Update update, String operation);
    EditMessageText processTasksDelete(Update update, String operation);
    EditMessageText processGetAllTasksNext(Update update, String operation);
    EditMessageText processGetAllTasksPrev(Update update, String operation);
    EditMessageText processGetTaskInDetail(Update update, String operation);
}
