package ua.delsix.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.delsix.service.MainService;
import ua.delsix.service.enums.ServiceCommand;

@Service
@Log4j
public class MainServiceImpl implements MainService {

    @Override
    public void processMessage(Update update) {
        String messageText = update.getMessage().getText();
        ServiceCommand userCommand = ServiceCommand.fromValue(messageText);

        log.debug("User command: "+userCommand);

        if(userCommand == null) {
            //TODO handle if command is null
        }

        switch(userCommand) {
            //TODO handle different commands
        }
    }
}
