package ua.delsix.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface ProducerService {

    void ProduceAnswer(SendMessage answer);

}
