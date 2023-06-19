package ua.delsix.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.delsix.RabbitQueue;
import ua.delsix.service.ProducerService;
import ua.delsix.utils.MarkupUtils;

@Service
@Log4j
public class ProducerServiceImpl implements ProducerService {
    private final RabbitTemplate rabbitTemplate;
    private final MarkupUtils markupUtils;

    public ProducerServiceImpl(RabbitTemplate rabbitTemplate, MarkupUtils markupUtils) {
        this.rabbitTemplate = rabbitTemplate;
        this.markupUtils = markupUtils;
    }


    @Override
    public void produceAnswer(SendMessage answer, Update update) {
        if(answer.getReplyMarkup() == null) {
            answer.setReplyMarkup(markupUtils.getDefaultMarkup(update));
        }

        rabbitTemplate.convertAndSend(RabbitQueue.ANSWER_UPDATE, answer);
        log.debug("NODE: answer message sent to RabbitMQ");
    }

    @Override
    public void produceAnswer(EditMessageText editAnswer) {
        rabbitTemplate.convertAndSend(RabbitQueue.EDIT_ANSWER_UPDATE, editAnswer);
        log.debug("NODE: answer message sent to RabbitMQ");
    }
}
