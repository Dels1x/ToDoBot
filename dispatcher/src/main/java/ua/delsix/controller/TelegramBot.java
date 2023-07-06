package ua.delsix.controller;

import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;

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

    @PostConstruct
    private void setup() {
        updateController.registerBot(this);
    }

    @Override
    public String getBotUsername() {
        return name;
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.debug("New update received: " + update);
        updateController.processUpdate(update);
    }

    protected void executeMessage(BotApiMethod<?> message) {
        if (!isMessageValid(message)) {
            return;
        }

        try {
            execute(message);
            log.debug("Successfully sent the message back");
        } catch (TelegramApiException e) {
            log.error("TelegramApiException thrown when attempted to edit a message: " + e.getMessage());
        }
    }

    private boolean isMessageValid(BotApiMethod<?> message) {
        if (message == null) {
            log.warn("Attempted to perform an operation with a null message");
            return false;
        }
        return true;
    }
}
