package ua.delsix.service.impl;


import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.delsix.language.LanguageManager;
import ua.delsix.service.ProducerService;
import ua.delsix.service.SettingsService;
import ua.delsix.utils.MarkupUtils;
import ua.delsix.utils.MessageUtils;
import ua.delsix.utils.UserUtils;

@Service
public class SettingsServiceImpl implements SettingsService {
    private final UserUtils userUtils;
    private final MarkupUtils markupUtils;
    private final MessageUtils messageUtils;
    private final LanguageManager languageManager;
    private final ProducerService producerService;

    public SettingsServiceImpl(UserUtils userUtils, MarkupUtils markupUtils, MessageUtils messageUtils, LanguageManager languageManager, ProducerService producerService) {
        this.userUtils = userUtils;
        this.markupUtils = markupUtils;
        this.messageUtils = messageUtils;
        this.languageManager = languageManager;
        this.producerService = producerService;
    }

    @Override
    public SendMessage getSettings(Update update) {
        String language = userUtils.getUserByUpdate(update).getLanguage();

        return messageUtils.sendMessageGenerator(
                update,
                languageManager.getMessage(
                        String.format("settings.name.%s", language),
                        language),
                markupUtils.getSettingsMainMarkup(update)
        );
    }

    @Override
    public EditMessageText processLanguage(Update update) {
        String language = userUtils.getUserByUpdate(update).getLanguage();

        return messageUtils.editMessageGenerator(
                update,
                languageManager.getMessage(
                        String.format("keyboard.settings.language.%s", language),
                        language),
                markupUtils.getSettingsLanguageMarkup()
        );
    }

    @Override
    public void setLanguage(Update update) {
        String selectedLanguage = update.getCallbackQuery().getData().split("/")[3];
        String languageCode;

        switch (selectedLanguage) {
            case "RUSSIAN" -> languageCode = "ru";
            case "UKRAINIAN" -> languageCode = "ua";
            default -> languageCode = "en";
        }

        userUtils.setLanguage(update, languageCode);

        producerService.produceAnswer(
                messageUtils.sendMessageGenerator(
                update,
                languageManager.getMessage(
                        String.format("keyboard.settings.set.%s", languageCode),
                        languageCode)),
                update);
    }
}
