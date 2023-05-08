package ua.delsix.service;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface TaskService {
    String processCreateTask(Update update);
    String processCreatingTask(Update update);
    String processEditingTask(Update update);
}
