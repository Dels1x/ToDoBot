package ua.delsix.utils;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ua.delsix.entity.Task;
import ua.delsix.entity.User;
import ua.delsix.repository.TaskRepository;

import java.time.LocalDate;
import java.util.*;

@Component
public class MarkupUtils {
    private final UserUtils userUtils;
    private final TaskRepository taskRepository;

    public MarkupUtils(UserUtils userUtils, TaskRepository taskRepository) {
        this.userUtils = userUtils;
        this.taskRepository = taskRepository;
    }

    public ReplyKeyboardMarkup getCancelSkipFinishMarkup() {
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Cancel");
        row1.add("Skip");
        row1.add("Finish");
        keyboard.add(row1);

        return new ReplyKeyboardMarkup(keyboard);
    }

    public ReplyKeyboardMarkup getDateMarkup() {
        LocalDate today = LocalDate.now();

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();
        KeyboardRow row3 = new KeyboardRow();

        row1.add("Cancel");
        row1.add("Skip");
        row1.add("Finish");
        row2.add("Today");
        row2.add("Tomorrow");
        row3.add(String.valueOf(today.plusDays(2)));
        row3.add(String.valueOf(today.plusDays(3)));
        row3.add(String.valueOf(today.plusDays(4)));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        return new ReplyKeyboardMarkup(keyboard);
    }

    public ReplyKeyboardMarkup getDateMarkupWithoutSkipCancelFinish() {
        LocalDate today = LocalDate.now();

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

        return new ReplyKeyboardMarkup(keyboard);
    }

    public ReplyKeyboardMarkup getTagsReplyMarkup(Update update) {
        List<Map.Entry<String, Integer>> tags = getTags(update);
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow navKeyboard = new KeyboardRow();
        KeyboardRow mainTagsKeyboard = new KeyboardRow();
        KeyboardRow secTagsKeyboard = new KeyboardRow();

        navKeyboard.add("Cancel");
        navKeyboard.add("Skip");
        navKeyboard.add("Finish");

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

        int pageCount = (int) Math.ceil(tags.size() / 9.0);

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
        User user = userUtils.getUserByTag(update);
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

    public InlineKeyboardMarkup getEditMarkup(String[] callbackData, String operation) {
        int pageIndex = Integer.parseInt(callbackData[2]);
        int pageTaskIndex = Integer.parseInt(callbackData[3]);

        // setting up the keyboard
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> firstRow = new ArrayList<>();
        List<InlineKeyboardButton> secondRow = new ArrayList<>();

        // setting up buttons
        InlineKeyboardButton nameButton = new InlineKeyboardButton("Edit name");
        InlineKeyboardButton descButton = new InlineKeyboardButton("Edit description");
        InlineKeyboardButton dateButton = new InlineKeyboardButton("Edit date");
        InlineKeyboardButton priorityButton = new InlineKeyboardButton("Edit priority");
        InlineKeyboardButton difficultyButton = new InlineKeyboardButton("Edit difficulty");
        InlineKeyboardButton tagButton = new InlineKeyboardButton("Edit tag");
        InlineKeyboardButton cancelButton = new InlineKeyboardButton("Cancel");

        // setting callback data and adding buttons to keyboard
        nameButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/NAME", operation, pageIndex, pageTaskIndex));
        descButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/DESC", operation, pageIndex, pageTaskIndex));
        priorityButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/PRIOR", operation, pageIndex, pageTaskIndex));
        difficultyButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/DIFF", operation, pageIndex, pageTaskIndex));
        tagButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/TAG", operation, pageIndex, pageTaskIndex));
        dateButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/DATE", operation, pageIndex, pageTaskIndex));
        cancelButton.setCallbackData(String.format("%s/TASK/%d/%d/EDIT/CANCEL", operation, pageIndex, pageTaskIndex));

        firstRow.add(nameButton);
        firstRow.add(descButton);
        firstRow.add(dateButton);
        secondRow.add(priorityButton);
        secondRow.add(difficultyButton);
        secondRow.add(tagButton);
        secondRow.add(cancelButton);

        keyboard.add(firstRow);
        keyboard.add(secondRow);

        return new InlineKeyboardMarkup(keyboard);
    }
}
