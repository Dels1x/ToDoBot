package ua.delsix.service;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface CallbackQueryProcessor {
    void processCallbackQuery(Update update);
}
