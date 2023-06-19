package ua.delsix.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface ProducerService {

    void produceAnswer(SendMessage answer, Update update);
    void produceAnswer(EditMessageText answer);

}
