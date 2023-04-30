package ua.delsix.controller;

import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Log4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${bot.name}")
    private String name;

    private final UpdateController updateController;

    public TelegramBot(@Value("${bot.token}") String botToken, UpdateController updateController) {
        super(botToken);
        this.updateController = updateController;
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.debug("New update received: "+update);
        updateController.processUpdate(update);
    }

    @Override
    public String getBotUsername() {
        return name;
    }
}
