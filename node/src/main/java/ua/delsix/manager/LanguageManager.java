package ua.delsix.manager;

import org.springframework.context.MessageSource;

import java.util.ArrayList;
import java.util.List;
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

    public boolean isInSection(String value, String key, String languageCode) {
        List<String> values = new ArrayList<>();
        values.add(getMessage(key.concat(".en"), languageCode).toLowerCase());
        values.add(getMessage(key.concat(".ru"), languageCode).toLowerCase());
        values.add(getMessage(key.concat(".ua"), languageCode).toLowerCase());

        return values.stream().anyMatch(v -> v.equalsIgnoreCase(value));
    }
}
