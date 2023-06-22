package ua.delsix.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface SettingsService {
    SendMessage getSettings(Update update);
    EditMessageText processLanguage(Update update);
    void setLanguage (Update update);
}
