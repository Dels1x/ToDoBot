package ua.delsix.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.delsix.RabbitQueue;
import ua.delsix.service.ConsumerService;
import ua.delsix.service.MainService;

@Service
@Log4j
public class ConsumerServiceImpl implements ConsumerService {
    private final MainService mainService;

    public ConsumerServiceImpl(MainService mainService) {
        this.mainService = mainService;
    }


    @RabbitListener(queues = RabbitQueue.MESSAGE_UPDATE)
    @Override
    public void consumeMessageUpdate(Update update) {
        log.debug("Node: MESSAGE_UPDATE received");
        mainService.processUpdate(update);
    }
}
