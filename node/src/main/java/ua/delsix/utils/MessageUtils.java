package ua.delsix.utils;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

@Component
public class MessageUtils {
    public static SendMessage sendMessageGenerator(Update update, String text) {
        Message message;

        if(update.hasCallbackQuery()) {
            message = update.getCallbackQuery().getMessage();
        } else {
            message = update.getMessage();
        }

        long chatId = message.getChatId();

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        sendMessage.enableMarkdown(true);

        return sendMessage;
    }

    public static SendMessage sendMessageGenerator(Update update, String text, ReplyKeyboard markup) {
        Message message;

        if(update.hasCallbackQuery()) {
            message = update.getCallbackQuery().getMessage();
        } else {
            message = update.getMessage();
        }

        long chatId = message.getChatId();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        sendMessage.setReplyMarkup(markup);
        sendMessage.enableMarkdown(true);

        return sendMessage;
    }

    public static EditMessageText editMessageGenerator(Update update, String text) {
        var message = update.getCallbackQuery().getMessage();
        EditMessageText editMessage = new EditMessageText();
        editMessage.setMessageId(message.getMessageId());
        editMessage.setChatId(message.getChatId());
        editMessage.setText(text);
        editMessage.enableMarkdown(true);

        return editMessage;
    }

    public static EditMessageText editMessageGenerator(Update update, String text, InlineKeyboardMarkup markup) {
        var message = update.getCallbackQuery().getMessage();
        EditMessageText editMessage = new EditMessageText();
        editMessage.setMessageId(message.getMessageId());
        editMessage.setChatId(message.getChatId());
        editMessage.setText(text);
        editMessage.setReplyMarkup(markup);
        editMessage.enableMarkdown(true);

        return editMessage;
    }
}
