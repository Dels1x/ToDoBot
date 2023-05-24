package ua.delsix.service.impl;


import lombok.extern.log4j.Log4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import ua.delsix.RabbitQueue;
import ua.delsix.controller.UpdateController;
import ua.delsix.service.AnswerConsumer;

@Service
@Log4j
public class AnswerConsumerImpl implements AnswerConsumer {
    private final UpdateController updateController;

    public AnswerConsumerImpl(UpdateController updateController) {
        this.updateController = updateController;
    }

    @Override
    @RabbitListener(queues = RabbitQueue.ANSWER_UPDATE)
    public void consume(SendMessage answer) {
        log.debug("DISPATCHER: answer message received");
        updateController.setView(answer);
    }

    @Override
    @RabbitListener(queues = RabbitQueue.EDIT_ANSWER_UPDATE)
    public void consume(EditMessageText answer) {
        log.debug("DISPATCHER: edit answer message received");
        updateController.setView(answer);
    }
}
