package ua.delsix.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDate;
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

    public static ReplyKeyboardMarkup getDateMarkup() {
        LocalDate today = LocalDate.now();

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();
        KeyboardRow row3 = new KeyboardRow();

        row1.add("Skip");
        row1.add("Cancel");
        row1.add("Finish");
        row2.add("Today");
        row2.add("Tomorrow");
        row3.add(String.valueOf(today.plusDays(2)));
        row3.add(String.valueOf(today.plusDays(3)));
        row3.add(String.valueOf(today.plusDays(4)));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        markup.setKeyboard(keyboard);
        return markup;
    }

    public static ReplyKeyboardMarkup getDateMarkupWithoutSkipCancelFinish() {
        LocalDate today = LocalDate.now();

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();

        row1.add("Today");
        row1.add("Tomorrow");
        row2.add(String.valueOf(today.plusDays(2)));
        row2.add(String.valueOf(today.plusDays(3)));
        row2.add(String.valueOf(today.plusDays(4)));

        keyboard.add(row1);
        keyboard.add(row2);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
