package ua.delsix.utils;

import org.springframework.stereotype.Component;
import ua.delsix.entity.Task;
import ua.delsix.entity.User;
import ua.delsix.manager.LanguageManager;

import java.util.Arrays;
import java.util.List;

@Component
public class TaskUtils {

    private final LanguageManager languageManager;
    public static final List<String> states = Arrays.asList("CREATING_NAME", "CREATING_DESCRIPTION", "CREATING_DATE", "CREATING_PRIORITY", "CREATING_DIFFICULTY", "CREATING_TAG", "COMPLETED");

    public TaskUtils(LanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    public String getDifficultyDescription(User user, Integer diff) {
        if (diff == null) {
            return "❌";
        }
        String language = user.getLanguage();

        return switch (diff) {
            case 0 -> languageManager.getMessage(
                    String.format("keyboard.difficulty.no-difficulty.%s", language),
                    language);
            case 1 -> languageManager.getMessage(
                    String.format("keyboard.difficulty.very-easy.%s", language),
                    language);
            case 2 -> languageManager.getMessage(
                    String.format("keyboard.difficulty.easy.%s", language),
                    language);
            case 3 -> languageManager.getMessage(
                    String.format("keyboard.difficulty.moderate.%s", language),
                    language);
            case 4 -> languageManager.getMessage(
                    String.format("keyboard.difficulty.challenging.%s", language),
                    language);
            case 5 -> languageManager.getMessage(
                    String.format("keyboard.difficulty.difficult.%s", language),
                    language);
            case 6 -> languageManager.getMessage(
                    String.format("keyboard.difficulty.very-difficult.%s", language),
                    language);
            case 7 -> languageManager.getMessage(
                    String.format("keyboard.difficulty.extremely-difficult.%s", language),
                    language);
            default -> "❌";
        };
    }

    public String getPriorityDescription(User user, Integer priority) {
        if (priority == null) {
            return "❌";
        }
        String language = user.getLanguage();

        return switch (priority) {
            case 1 -> languageManager.getMessage(
                    String.format("keyboard.priority.not-important.%s", language),
                    language);
            case 2 -> languageManager.getMessage(
                    String.format("keyboard.priority.low.%s", language),
                    language);
            case 3 -> languageManager.getMessage(
                    String.format("keyboard.priority.medium.%s", language),
                    language);
            case 4 -> languageManager.getMessage(
                    String.format("keyboard.priority.high.%s", language),
                    language);
            case 5 -> languageManager.getMessage(
                    String.format("keyboard.priority.very-high.%s", language),
                    language);
            case 6 -> languageManager.getMessage(
                    String.format("keyboard.priority.extremely-high.%s", language),
                    language);
            default -> "❌";
        };
    }

    public String taskToString(User user, Task task) {
        return String.format("""
                        ✏️ *%s*
                        💬 - _%s_
                                        
                        _%s%s%s 🏷️#%s_""",
                task.getName() == null ? "" :
                        task.getName(),
                task.getDescription() == null ? "" :
                        task.getDescription(),
                task.getTargetDate() == null ? "" :
                        String.format("📅 %s ", task.getTargetDate()),
                getPriorityDescription(user, task.getPriority()).equals("❌") ? "" :
                        String.format("⭐️️ %s ", getPriorityDescription(user, task.getPriority())),
                getDifficultyDescription(user, task.getDifficulty()).equals("❌") ? "" :
                        String.format("⚡️ %s ", getDifficultyDescription(user, task.getDifficulty())),
                task.getTag() == null ? "Untagged" :
                        task.getTag());
    }

    public String taskToStringInDetail(Task task, User user) {
        String language = user.getLanguage();
        return String.format(
                languageManager.getMessage(
                        String.format("task.detail.main.%s", user.getLanguage()),
                        language),
                task.getName() == null ? "❌" : task.getName(),
                task.getDescription() == null ? "❌" : task.getDescription(),
                task.getTargetDate() == null ? "❌" : task.getTargetDate().toString(),
                getPriorityDescription(user, task.getPriority()),
                getDifficultyDescription(user, task.getDifficulty()),
                task.getTag() == null ? "Untagged" : task.getTag(),
                task.getCreatedAt().toString(),
                task.getStatus() == null ? "❌" :
                        task.getStatus().equals("Completed") ? "✅" :
                                task.getStatus().equals("Failed") ? "\uD83D\uDC94" : "❌",
                task.getCompletionDate() == null ? "" :
                       languageManager.getMessage(
                               String.format("task.detail.completed-at.%s", language),
                                       language)
                               .concat(task.getCompletionDate().toString()));
    }

    public String responseForEachState(Task task, User user) {
        String state = task.getState();
        String language = user.getLanguage();
        String step;

        switch (state) {
            case "CREATING_NAME" -> step = "description";
            case "CREATING_DESCRIPTION" -> step = "date";
            case "CREATING_DATE" -> step = "priority";
            case "CREATING_PRIORITY" -> step = "difficulty";
            case "CREATING_DIFFICULTY" -> step = "tag";
            case "CREATING_TAG", "COMPLETED" -> step = "completed";
            default -> {
                return languageManager.getMessage(
                        String.format("bot.error.%s", language),
                        language);
            }
        }

        if(step.equals("completed")) {
            return String.format(
                    languageManager.getMessage(
                    String.format("create.%s.%s", step, language),
                            language),
                    taskToStringInDetail(task, user));
        } else {
            return languageManager.getMessage(
                    String.format("create.%s.%s", step, language),
                    language);
        }
    }
}
