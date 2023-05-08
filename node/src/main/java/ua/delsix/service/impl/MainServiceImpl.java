package ua.delsix.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.delsix.entity.User;
import ua.delsix.service.MainService;
import ua.delsix.service.ProducerService;
import ua.delsix.service.TaskService;
import ua.delsix.service.enums.ServiceCommand;
import ua.delsix.utils.MessageUtils;
import ua.delsix.utils.UserUtils;

@Service
@Log4j
public class MainServiceImpl implements MainService {

    private final ProducerService producerService;
    private final TaskService taskService;
    private final UserUtils userUtils;

    public MainServiceImpl(ProducerService producerService, TaskService taskService, UserUtils userUtils) {
        this.producerService = producerService;
        this.taskService = taskService;
        this.userUtils = userUtils;
    }

    @Override
    public void processMessage(Update update) {
        String messageText = update.getMessage().getText();
        ServiceCommand userCommand = ServiceCommand.fromValue(messageText);
        String answerText = "";

        log.debug("User command: " + userCommand);

        User user = userUtils.getUserByTag(update);
        log.trace("User: " + user.toString());

        if (userCommand == null) {
            userCommand = ServiceCommand.nonCommand;
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
                                                
                        Type \"/help\" to see all available commands.""";
            }
            case createTask -> {
                answerText = taskService.processCreateTask(update);
            }
            default -> {
                answerText = "Unknown command";
            }

            //TODO handle different commands
        }

        SendMessage answerMessage = MessageUtils.sendMessageGenerator(update, answerText);
        producerService.ProduceAnswer(answerMessage);
    }
}
