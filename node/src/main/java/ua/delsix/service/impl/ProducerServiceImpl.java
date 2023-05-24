package ua.delsix.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import ua.delsix.RabbitQueue;
import ua.delsix.service.ProducerService;

@Service
@Log4j
public class ProducerServiceImpl implements ProducerService {
    private final RabbitTemplate rabbitTemplate;

    public ProducerServiceImpl(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }


    @Override
    public void produceAnswer(SendMessage answer) {
        rabbitTemplate.convertAndSend(RabbitQueue.ANSWER_UPDATE, answer);
        log.debug("NODE: answer message sent to RabbitMQ");
    }

    @Override
    public void produceAnswer(EditMessageText editAnswer) {
        rabbitTemplate.convertAndSend(RabbitQueue.EDIT_ANSWER_UPDATE, editAnswer);
        log.debug("NODE: answer message sent to RabbitMQ");
    }
}
