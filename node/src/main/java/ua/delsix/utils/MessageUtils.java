package ua.delsix.utils;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
public class MessageUtils {
    public static SendMessage sendMessageGenerator(Update update, String text) {
        long chatId = update.getMessage().getChatId();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);

        return sendMessage;
    }

    public static EditMessageText editMessageGenerator(Update update, String text) {
        var message = update.getCallbackQuery().getMessage();
        EditMessageText editMessage = new EditMessageText();
        editMessage.setMessageId(message.getMessageId());
        editMessage.setChatId(message.getChatId());
        editMessage.setText(text);

        return editMessage;
    }
}
