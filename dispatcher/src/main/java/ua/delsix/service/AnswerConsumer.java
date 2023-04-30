package ua.delsix.service;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface AnswerConsumer {
    void consume(Update update);
}
