package ua.delsix.language;

import org.springframework.context.MessageSource;
import java.util.Locale;

public class LanguageManager {
    private final MessageSource messageSource;

    public LanguageManager(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String getMessage(String key, String languageCode) {
        Locale language = new Locale.Builder().setLanguageTag(languageCode).build();
        return messageSource.getMessage(key, null, language);
    }
}
