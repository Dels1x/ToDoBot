package ua.delsix.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.delsix.entity.User;
import ua.delsix.repository.UserRepository;
import ua.delsix.service.MainService;
import ua.delsix.service.ProducerService;
import ua.delsix.service.enums.ServiceCommand;
import ua.delsix.utils.MessageUtils;

import java.time.LocalDate;
import java.util.Optional;

@Service
@Log4j
public class MainServiceImpl implements MainService {

    private final ProducerService producerService;
    private final UserRepository userRepository;

    public MainServiceImpl(ProducerService producerService, UserRepository userRepository) {
        this.producerService = producerService;
        this.userRepository = userRepository;
    }

    @Override
    public void processMessage(Update update) {
        String messageText = update.getMessage().getText();
        ServiceCommand userCommand = ServiceCommand.fromValue(messageText);
        String answerText = "";

        log.debug("User command: "+userCommand);

        User user = getUserByTag(update);
        log.trace("User: "+user.toString());

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

    private User getUserByTag(Update update) {
        org.telegram.telegrambots.meta.api.objects.User tgUser = update.getMessage().getFrom();
        String userTag = tgUser.getUserName();
        Optional<User> user = userRepository.findByTag(userTag);

        if(user.isPresent()) {
            return user.get();
        } else {
            User newUser = User.builder()
                    .name(tgUser.getFirstName())
                    .taskCount(0)
                    .taskCompleted(0)
                    .createdAt(LocalDate.now())
                    .tag(userTag)
                    .build();
            return userRepository.save(newUser);
        }
    }
}
