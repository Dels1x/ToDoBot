package ua.delsix.service;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface ConsumerService {

    void consumeMessageUpdate(Update update);

}
