package ua.delsix.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
    public void ProduceAnswer(SendMessage answer) {
        rabbitTemplate.convertAndSend(RabbitQueue.ANSWER_UPDATE, answer);
        log.debug("NODE: answer message sent to RabbitMQ");
    }
}
