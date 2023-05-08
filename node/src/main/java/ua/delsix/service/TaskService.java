package ua.delsix.service;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface TaskService {
    String processCreateTask(Update update);
    String processTask(Update update);
}
