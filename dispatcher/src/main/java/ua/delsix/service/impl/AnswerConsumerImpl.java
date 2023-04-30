package ua.delsix.service.impl;


import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.delsix.service.AnswerConsumer;

@Service
public class AnswerConsumerImpl implements AnswerConsumer {
    @Override
    public void consume(Update update) {

    }
}
