package ua.delsix.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

public interface ProducerService {

    void produceAnswer(SendMessage answer);
    void produceAnswer(EditMessageText answer);

}
