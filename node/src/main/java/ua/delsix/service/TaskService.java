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
    SendMessage processGetAllTags(Update update);
    EditMessageText processGetAllTagsUpdate(Update update, String move);
    EditMessageText processTasksEdit(Update update, String operation);
    EditMessageText processTasksDelete(Update update, String operation);
    EditMessageText processGetAllTasksUpdate(Update update, String operation, String move);
    EditMessageText processGetTaskInDetail(Update update, String operation);
    SendMessage processDeleteAllCompletedTasksConfirmation(Update update);
    EditMessageText processDeleteAllCompletedTasks(Update update);
    SendMessage processDeleteAllTasksConfirmation(Update update);
    EditMessageText processDeleteAllTasks(Update update);
}
