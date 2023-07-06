package ua.delsix.utils;

import org.telegram.telegrambots.meta.api.objects.Update;

public class CallbackQueryUtils {
    public static String[] getCallbackData(Update update) {
        return update.getCallbackQuery().getData().split("/");
    }
}
