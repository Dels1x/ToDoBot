package ua.delsix.utils;

import lombok.extern.log4j.Log4j;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Arrays;
import java.util.stream.Collectors;

@Log4j
public class CallbackQueryUtils {
    public static String[] getCallbackData(Update update) {
        return update.getCallbackQuery().getData().split("/");
    }

    public static CallbackQuery getTaskEditCancelCallBackQuery(Update update) {
        String[] callbackData = getCallbackData(update);
        callbackData[5] = "CANCEL";
        String callbackContent = Arrays.stream(callbackData)
                .map(Object::toString) // Convert each element to a string
                .collect(Collectors.joining("/")); // Join elements with a "/"
        log.trace("Callback content: " + callbackContent);

        CallbackQuery callbackQuery = update.getCallbackQuery();
        callbackQuery.setData(callbackContent);

        return callbackQuery;
    }
}
