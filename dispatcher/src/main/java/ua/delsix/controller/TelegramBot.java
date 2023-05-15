package ua.delsix.controller;

import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Log4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${bot.name}")
    private String name;

    private final UpdateController updateController;
    public static final ReplyKeyboardMarkup REPLY_KEYBOARD_MARKUP = new ReplyKeyboardMarkup();

    static {
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();

        row1.add("Tasks");
        row1.add("Create task");
        row2.add("Remove task");
        row2.add("Edit task");
        row2.add("Cancel");

        keyboard.add(row1);
        keyboard.add(row2);

        REPLY_KEYBOARD_MARKUP.setKeyboard(keyboard);
    }

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

    public void sendMessage(SendMessage message) {
        if(message == null) {
            log.warn("Attempted to send a null message");
            return;
        }
        log.trace("SendMessage: "+message);

        try {
            if(message.getReplyMarkup() instanceof ReplyKeyboard) {
                log.debug("Message already has reply keyboard");
            } else {
                log.debug("Message doesn't have a markup keyboard");
                message.setReplyMarkup(REPLY_KEYBOARD_MARKUP);
            }

            execute(message);
            log.debug("Successfully sent the message back");
        } catch (TelegramApiException e) {
            log.error("TelegramApiException thrown when attempted to send a message: "+e.getMessage());
        }
    }
}
