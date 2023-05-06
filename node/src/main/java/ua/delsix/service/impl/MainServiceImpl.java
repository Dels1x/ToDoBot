package ua.delsix.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.delsix.service.MainService;
import ua.delsix.service.ProducerService;
import ua.delsix.service.enums.ServiceCommand;
import ua.delsix.utils.MessageUtils;

@Service
@Log4j
public class MainServiceImpl implements MainService {

    private final ProducerService producerService;

    public MainServiceImpl(ProducerService producerService) {
        this.producerService = producerService;
    }

    @Override
    public void processMessage(Update update) {
        log.debug("5");
        String messageText = update.getMessage().getText();
        ServiceCommand userCommand = ServiceCommand.fromValue(messageText);
        String answerText = "";

        log.debug("User command: "+userCommand);

        if(userCommand == null) {
            log.trace("user commmand is null");
            //TODO handle if command is null

            return;
        }

        switch (userCommand) {
            case help -> {
                answerText = """
                        Available commands:

                        - not yet""";
            }
            case start -> {
                answerText = """
                        Welcome to the delsix's Task Manager Bot!
                        
                        Type \"help\" to see all available commands.""";
            }

            //TODO handle different commands
        }

        SendMessage answerMessage = MessageUtils.sendMessageGenerator(update, answerText);
        producerService.ProduceAnswer(answerMessage);
    }
}
