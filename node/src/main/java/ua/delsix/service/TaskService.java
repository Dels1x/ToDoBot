package ua.delsix.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.delsix.entity.Task;

public interface TaskService {
    SendMessage processCreateTask(Update update);
    SendMessage processCreatingTask(Update update);
    SendMessage processGetAllTasks(Update update);
    SendMessage editTask(Update update, Task task);
    EditMessageText processTasksEdit(Update update);
    EditMessageText processTasksDelete(Update update);
    EditMessageText processGetAllTasksNext(Update update);
    EditMessageText processGetAllTasksPrev(Update update);
    EditMessageText processGetTaskInDetail(Update update);
}
