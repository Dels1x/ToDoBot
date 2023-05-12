package ua.delsix.utils;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class MarkupUtils {
    public static ReplyKeyboardMarkup getCancelSkipFinishMarkup() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Skip");
        row1.add("Cancel");
        row1.add("Finish");
        keyboard.add(row1);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
