package ua.delsix.utils;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ua.delsix.entity.Task;
import ua.delsix.entity.User;
import ua.delsix.manager.LanguageManager;
import ua.delsix.repository.TaskRepository;

import java.time.LocalDate;
import java.util.*;

@Log4j
@Component
public class MarkupUtils {
    private final UserUtils userUtils;
    private final TaskRepository taskRepository;
    private final LanguageManager languageManager;

    public MarkupUtils(UserUtils userUtils, TaskRepository taskRepository, LanguageManager languageManager) {
        this.userUtils = userUtils;
        this.taskRepository = taskRepository;
        this.languageManager = languageManager;
    }

    private KeyboardRow getCancelSkipFinishKeyboard(Update update) {
        String language = userUtils.getUserByUpdate(update).getLanguage();

        KeyboardRow row = new KeyboardRow();
        row.add(languageManager.getMessage(
                String.format("keyboard.create.cancel.%s", language),
                language));
        row.add(languageManager.getMessage(
                String.format("keyboard.create.skip.%s", language),
                language));
        row.add(languageManager.getMessage(
                String.format("keyboard.create.finish.%s", language),
                language));

        return row;
    }

    public ReplyKeyboardMarkup getCancelSkipFinishMarkup(Update update) {
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = getCancelSkipFinishKeyboard(update);
        keyboard.add(row);

        return new ReplyKeyboardMarkup(keyboard);
    }

    public ReplyKeyboardMarkup getDateMarkup(Update update) {
        String language = userUtils.getUserByUpdate(update).getLanguage();
        LocalDate today = LocalDate.now();

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = getCancelSkipFinishKeyboard(update);
        KeyboardRow row2 = new KeyboardRow();
        KeyboardRow row3 = new KeyboardRow();

        row2.add(languageManager.getMessage(
                String.format("keyboard.date.today.%s", language),
                language));
        row2.add(languageManager.getMessage(
                String.format("keyboard.date.tomorrow.%s", language),
                language));
        row3.add(String.valueOf(today.plusDays(2)));
        row3.add(String.valueOf(today.plusDays(3)));
        row3.add(String.valueOf(today.plusDays(4)));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        return new ReplyKeyboardMarkup(keyboard);
    }

    public ReplyKeyboardMarkup getDateMarkupWithoutSkipCancelFinish(Update update) {
        String language = userUtils.getUserByUpdate(update).getLanguage();
        LocalDate today = LocalDate.now();

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();

        row1.add(languageManager.getMessage(
                String.format("keyboard.date.today.%s", language),
                language));
        row1.add(languageManager.getMessage(
                String.format("keyboard.date.tomorrow.%s", language),
                language));
        row2.add(String.valueOf(today.plusDays(2)));
        row2.add(String.valueOf(today.plusDays(3)));
        row2.add(String.valueOf(today.plusDays(4)));

        keyboard.add(row1);
        keyboard.add(row2);

        return new ReplyKeyboardMarkup(keyboard);
    }

    public ReplyKeyboardMarkup getTagsReplyMarkup(Update update) {
        List<Map.Entry<String, Integer>> tags = getTags(update);
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow navKeyboard = getCancelSkipFinishKeyboard(update);
        KeyboardRow mainTagsKeyboard = new KeyboardRow();
        KeyboardRow secTagsKeyboard = new KeyboardRow();

        byte x = 0;
        for (Map.Entry<String, Integer> entry : tags) {
            String tag = entry.getKey();
            if (x < 5) {
                mainTagsKeyboard.add(tag);
            } else {
                secTagsKeyboard.add(tag);
            }

            x++;

            if (x == 10) {
                break;
            }
        }

        keyboard.add(navKeyboard);
        keyboard.add(mainTagsKeyboard);
        keyboard.add(secTagsKeyboard);

        return new ReplyKeyboardMarkup(keyboard);
    }

    public ReplyKeyboardMarkup getTagsReplyMarkupWithoutCancelSkipFinish(Update update) {
        List<Map.Entry<String, Integer>> tags = getTags(update);
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow mainTagsKeyboard = new KeyboardRow();
        KeyboardRow secTagsKeyboard = new KeyboardRow();

        byte x = 0;
        for (Map.Entry<String, Integer> entry : tags) {
            String tag = entry.getKey();
            if (x < 5) {
                mainTagsKeyboard.add(tag);
            } else {
                secTagsKeyboard.add(tag);
            }

            x++;

            if (x == 10) {
                break;
            }
        }

        keyboard.add(mainTagsKeyboard);
        keyboard.add(secTagsKeyboard);

        return new ReplyKeyboardMarkup(keyboard);
    }

    public InlineKeyboardMarkup getTagsInlineMarkup(Update update, int pageIndex) {
        List<Map.Entry<String, Integer>> tags = getTags(update);
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> navKeyboard = new ArrayList<>();
        List<InlineKeyboardButton> mainTagsKeyboard = new ArrayList<>();
        List<InlineKeyboardButton> secTagsKeyboard = new ArrayList<>();
        List<InlineKeyboardButton> thirdTagsKeyboard = new ArrayList<>();

        int pageCount = (int) Math.ceil(tags.size() / 9.0) - 1;

        byte x = 0;
        byte y = 0;
        for (Map.Entry<String, Integer> entry : tags) {
            String tag = entry.getKey();
            InlineKeyboardButton button = new InlineKeyboardButton("#".concat(tag));
            button.setCallbackData(String.format("GET_BY_TAG|%s/NEXT/-1/0", tag));

            if (y == pageIndex) {
                if (x < 3) {
                    mainTagsKeyboard.add(button);
                } else if (x < 6) {
                    secTagsKeyboard.add(button);
                } else {
                    thirdTagsKeyboard.add(button);
                }
            }

            x++;

            if (x == 9) {
                x = 0;
                y++;
            }
        }

        InlineKeyboardButton prevButton = new InlineKeyboardButton("<");
        InlineKeyboardButton nextButton = new InlineKeyboardButton(">");

        prevButton.setCallbackData(String.format("GET_TAGS/PREV/%d", pageIndex));
        nextButton.setCallbackData(String.format("GET_TAGS/NEXT/%d", pageIndex));

        // setting up row with prev next buttons
        if (pageIndex > 0 && pageIndex < pageCount) {
            navKeyboard.add(prevButton);
            navKeyboard.add(nextButton);
        } else if (pageIndex == 0 && pageIndex < pageCount) {
            navKeyboard.add(nextButton);
        } else if (pageIndex > 0 && pageIndex == pageCount) {
            navKeyboard.add(prevButton);
        }

        keyboard.add(navKeyboard);
        keyboard.add(mainTagsKeyboard);
        keyboard.add(secTagsKeyboard);
        keyboard.add(thirdTagsKeyboard);

        return new InlineKeyboardMarkup(keyboard);
    }

    public List<Map.Entry<String, Integer>> getTags(Update update) {
        User user = userUtils.getUserByUpdate(update);
        List<Task> tasks = taskRepository.findAllSortedOnlyByTags(user.getId());
        Map<String, Integer> tagOccurrences = new HashMap<>();

        for (Task task : tasks) {
            String tag = task.getTag();

            if (tag == null) {
                tagOccurrences.put("Untagged", tagOccurrences.getOrDefault("Untagged", 0) + 1);
            } else {
                tagOccurrences.put(tag, tagOccurrences.getOrDefault(tag, 0) + 1);
            }
        }

        List<Map.Entry<String, Integer>> tags = new ArrayList<>(tagOccurrences.entrySet());
        tags.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        return tags;
    }

    public InlineKeyboardMarkup getEditMarkup(String[] callbackData, String operation, Update update) {
        String language = userUtils.getUserByUpdate(update).getLanguage();
        int pageIndex = Integer.parseInt(callbackData[2]);
        int pageTaskIndex = Integer.parseInt(callbackData[3]);

        // setting up the keyboard
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> firstRow = new ArrayList<>();
        List<InlineKeyboardButton> secondRow = new ArrayList<>();
        List<InlineKeyboardButton> thirdRow = new ArrayList<>();
        List<InlineKeyboardButton> fourthRow = new ArrayList<>();

        // setting up buttons
        InlineKeyboardButton nameButton = new InlineKeyboardButton(
                languageManager.getMessage(
                        String.format("keyboard.tasks.edit.name.%s", language),
                        language)
        );
        InlineKeyboardButton descButton = new InlineKeyboardButton(
                languageManager.getMessage(
                        String.format("keyboard.tasks.edit.description.%s", language),
                        language)
        );
        InlineKeyboardButton dateButton = new InlineKeyboardButton(
                languageManager.getMessage(
                        String.format("keyboard.tasks.edit.date.%s", language),
                        language)
        );
        InlineKeyboardButton priorityButton = new InlineKeyboardButton(
                languageManager.getMessage(
                        String.format("keyboard.tasks.edit.priority.%s", language),
                        language)
        );
        InlineKeyboardButton difficultyButton = new InlineKeyboardButton(
                languageManager.getMessage(
                        String.format("keyboard.tasks.edit.difficulty.%s", language),
                        language)
        );
        InlineKeyboardButton tagButton = new InlineKeyboardButton(
                languageManager.getMessage(
                        String.format("keyboard.tasks.edit.tag.%s", language),
                        language)
        );
        InlineKeyboardButton backButton = new InlineKeyboardButton(
                languageManager.getMessage(
                        String.format("keyboard.tasks.edit.back.%s", language),
                        language)
        );

        // setting callback data and adding buttons to keyboard
        nameButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/NAME", operation, pageIndex, pageTaskIndex));
        descButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/DESC", operation, pageIndex, pageTaskIndex));
        priorityButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/PRIOR", operation, pageIndex, pageTaskIndex));
        difficultyButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/DIFF", operation, pageIndex, pageTaskIndex));
        tagButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/TAG", operation, pageIndex, pageTaskIndex));
        dateButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/DATE", operation, pageIndex, pageTaskIndex));
        backButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/CANCEL", operation, pageIndex, pageTaskIndex));

        firstRow.add(nameButton);
        firstRow.add(descButton);
        secondRow.add(dateButton);
        secondRow.add(priorityButton);
        thirdRow.add(difficultyButton);
        thirdRow.add(tagButton);
        fourthRow.add(backButton);

        keyboard.add(firstRow);
        keyboard.add(secondRow);
        keyboard.add(thirdRow);
        keyboard.add(fourthRow);

        return new InlineKeyboardMarkup(keyboard);
    }

    public ReplyKeyboardMarkup getDefaultMarkup(Update update) {
        String language = userUtils.getUserByUpdate(update).getLanguage();

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();

        row1.add(languageManager.getMessage(
                String.format("keyboard.default.tasks.%s", language),
                language));
        row1.add(languageManager.getMessage(
                String.format("keyboard.default.tags.%s", language),
                language));
        row1.add(languageManager.getMessage(
                String.format("keyboard.default.create.%s", language),
                language));
        row2.add(languageManager.getMessage(
                String.format("keyboard.default.uncompleted.%s", language),
                language));
        row2.add(languageManager.getMessage(
                String.format("keyboard.default.completed.%s", language),
                language));
        row2.add(languageManager.getMessage(
                String.format("keyboard.default.today.%s", language),
                language));
        keyboard.add(row1);
        keyboard.add(row2);
        return new ReplyKeyboardMarkup(keyboard);
    }

    public ReplyKeyboardMarkup getPriorityMarkup(Update update) {
        String language = userUtils.getUserByUpdate(update).getLanguage();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = getCancelSkipFinishKeyboard(update);
        KeyboardRow row2 = new KeyboardRow();
        KeyboardRow row3 = new KeyboardRow();

        row2.add(languageManager.getMessage(
                String.format("keyboard.priority.not-important.%s", language),
                language));
        row2.add(languageManager.getMessage(
                String.format("keyboard.priority.low.%s", language),
                language));
        row2.add(languageManager.getMessage(
                String.format("keyboard.priority.medium.%s", language),
                language));
        row3.add(languageManager.getMessage(
                String.format("keyboard.priority.high.%s", language),
                language));
        row3.add(languageManager.getMessage(
                String.format("keyboard.priority.very-high.%s", language),
                language));
        row3.add(languageManager.getMessage(
                String.format("keyboard.priority.extremely-high.%s", language),
                language));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        return new ReplyKeyboardMarkup(keyboard);
    }

    public ReplyKeyboardMarkup getPriorityMarkupWithoutSkipCancelFinish(Update update) {
        String language = userUtils.getUserByUpdate(update).getLanguage();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row2 = new KeyboardRow();
        KeyboardRow row3 = new KeyboardRow();

        row2.add(languageManager.getMessage(
                String.format("keyboard.priority.not-important.%s", language),
                language));
        row2.add(languageManager.getMessage(
                String.format("keyboard.priority.low.%s", language),
                language));
        row2.add(languageManager.getMessage(
                String.format("keyboard.priority.medium.%s", language),
                language));
        row3.add(languageManager.getMessage(
                String.format("keyboard.priority.high.%s", language),
                language));
        row3.add(languageManager.getMessage(
                String.format("keyboard.priority.very-high.%s", language),
                language));
        row3.add(languageManager.getMessage(
                String.format("keyboard.priority.extremely-high.%s", language),
                language));

        keyboard.add(row2);
        keyboard.add(row3);

        return new ReplyKeyboardMarkup(keyboard);
    }

    public ReplyKeyboardMarkup getDifficultyMarkup(Update update) {
        String language = userUtils.getUserByUpdate(update).getLanguage();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = getCancelSkipFinishKeyboard(update);
        KeyboardRow row2 = new KeyboardRow();
        KeyboardRow row3 = new KeyboardRow();
        KeyboardRow row4 = new KeyboardRow();

        row2.add(languageManager.getMessage(
                String.format("keyboard.difficulty.no-difficulty.%s", language),
                language));
        row2.add(languageManager.getMessage(
                String.format("keyboard.difficulty.very-easy.%s", language),
                language));
        row2.add(languageManager.getMessage(
                String.format("keyboard.difficulty.easy.%s", language),
                language));
        row3.add(languageManager.getMessage(
                String.format("keyboard.difficulty.moderate.%s", language),
                language));
        row3.add(languageManager.getMessage(
                String.format("keyboard.difficulty.challenging.%s", language),
                language));
        row3.add(languageManager.getMessage(
                String.format("keyboard.difficulty.difficult.%s", language),
                language));
        row4.add(languageManager.getMessage(
                String.format("keyboard.difficulty.very-difficult.%s", language),
                language));
        row4.add(languageManager.getMessage(
                String.format("keyboard.difficulty.extremely-difficult.%s", language),
                language));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        return new ReplyKeyboardMarkup(keyboard);
    }

    public ReplyKeyboardMarkup getDifficultyMarkupWithoutSkipCancelFinish(Update update) {
        String language = userUtils.getUserByUpdate(update).getLanguage();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row2 = new KeyboardRow();
        KeyboardRow row3 = new KeyboardRow();
        KeyboardRow row4 = new KeyboardRow();
        KeyboardRow row5 = new KeyboardRow();

        row2.add(languageManager.getMessage(
                String.format("keyboard.difficulty.no-difficulty.%s", language),
                language));
        row2.add(languageManager.getMessage(
                String.format("keyboard.difficulty.very-easy.%s", language),
                language));
        row3.add(languageManager.getMessage(
                String.format("keyboard.difficulty.easy.%s", language),
                language));
        row3.add(languageManager.getMessage(
                String.format("keyboard.difficulty.moderate.%s", language),
                language));
        row4.add(languageManager.getMessage(
                String.format("keyboard.difficulty.challenging.%s", language),
                language));
        row4.add(languageManager.getMessage(
                String.format("keyboard.difficulty.difficult.%s", language),
                language));
        row5.add(languageManager.getMessage(
                String.format("keyboard.difficulty.very-difficult.%s", language),
                language));
        row5.add(languageManager.getMessage(
                String.format("keyboard.difficulty.extremely-difficult.%s", language),
                language));

        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);
        keyboard.add(row5);

        return new ReplyKeyboardMarkup(keyboard);
    }

    public InlineKeyboardMarkup getSettingsMainMarkup(Update update) {
        String language = userUtils.getUserByUpdate(update).getLanguage();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> buttonsRow = new ArrayList<>();

        InlineKeyboardButton languageButton = new InlineKeyboardButton(
                languageManager.getMessage(
                        String.format("keyboard.settings.language.%s", language),
                        language)
        );
        languageButton.setCallbackData("SETTINGS/LANGUAGE");

        buttonsRow.add(languageButton);
        keyboard.add(buttonsRow);

        return new InlineKeyboardMarkup(keyboard);
    }

    public InlineKeyboardMarkup getSettingsLanguageMarkup() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> buttonsRow1 = new ArrayList<>();
        List<InlineKeyboardButton> buttonsRow2 = new ArrayList<>();
        List<InlineKeyboardButton> buttonsRow3 = new ArrayList<>();

        InlineKeyboardButton englishButton = new InlineKeyboardButton("English");
        InlineKeyboardButton russianButton = new InlineKeyboardButton("Русский");
        InlineKeyboardButton ukrainianButton = new InlineKeyboardButton("Українська");
        englishButton.setCallbackData("SETTINGS/LANGUAGE/SET/ENGLISH");
        russianButton.setCallbackData("SETTINGS/LANGUAGE/SET/RUSSIAN");
        ukrainianButton.setCallbackData("SETTINGS/LANGUAGE/SET/UKRAINIAN");

        buttonsRow1.add(englishButton);
        buttonsRow2.add(russianButton);
        buttonsRow3.add(ukrainianButton);

        keyboard.add(buttonsRow1);
        keyboard.add(buttonsRow2);
        keyboard.add(buttonsRow3);

        return new InlineKeyboardMarkup(keyboard);
    }
}
