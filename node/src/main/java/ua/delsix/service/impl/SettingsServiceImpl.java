package ua.delsix.service.impl;


import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.delsix.controller.LanguageController;
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
    private final LanguageController languageController;
    private final ProducerService producerService;

    public SettingsServiceImpl(UserUtils userUtils, MarkupUtils markupUtils, MessageUtils messageUtils, LanguageController languageController, ProducerService producerService) {
        this.userUtils = userUtils;
        this.markupUtils = markupUtils;
        this.messageUtils = messageUtils;
        this.languageController = languageController;
        this.producerService = producerService;
    }

    @Override
    public SendMessage getSettings(Update update) {
        String language = userUtils.getUserByUpdate(update).getLanguage();

        return messageUtils.sendMessageGenerator(
                update,
                languageController.getMessage(
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
                languageController.getMessage(
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
                languageController.getMessage(
                        String.format("keyboard.settings.set.%s", languageCode),
                        languageCode)),
                update);
    }
}
