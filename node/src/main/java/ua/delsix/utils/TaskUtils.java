package ua.delsix.utils;

import org.springframework.stereotype.Component;
import ua.delsix.entity.Task;
import ua.delsix.entity.User;
import ua.delsix.language.LanguageManager;

import java.util.Arrays;
import java.util.List;

@Component
public class TaskUtils {

    private final LanguageManager languageManager;
    public static final List<String> states = Arrays.asList("CREATING_NAME", "CREATING_DESCRIPTION", "CREATING_DATE", "CREATING_PRIORITY", "CREATING_DIFFICULTY", "CREATING_TAG", "COMPLETED");

    public TaskUtils(LanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    public String getDifficultyDescription(Integer diff) {
        if (diff == null) {
            return "❌";
        }

        return switch (diff) {
            case 0 -> "No difficulty";
            case 1 -> "Very easy";
            case 2 -> "Easy";
            case 3 -> "Moderate";
            case 4 -> "Challenging";
            case 5 -> "Difficult";
            case 6 -> "Very difficult";
            case 7 -> "Extremely difficult";
            default -> "❌";
        };
    }

    public String getPriorityDescription(Integer priority) {
        if (priority == null) {
            return "❌";
        }

        return switch (priority) {
            case 1 -> "Not important";
            case 2 -> "Low";
            case 3 -> "Medium";
            case 4 -> "High";
            case 5 -> "Very high";
            case 6 -> "Extremely high";
            default -> "❌";
        };
    }

    public String taskToString(Task task) {
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
                getPriorityDescription(task.getPriority()).equals("❌") ? "" :
                        String.format("⭐️️ %s ", getPriorityDescription(task.getPriority())),
                getDifficultyDescription(task.getDifficulty()).equals("❌") ? "" :
                        String.format("⚡️ %s ", getDifficultyDescription(task.getDifficulty())),
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
                getPriorityDescription(task.getPriority()),
                getDifficultyDescription(task.getDifficulty()),
                task.getTag() == null ? "Untagged" : task.getTag(),
                task.getCreatedAt().toString(),
                task.getStatus() == null ? "❌" :
                        task.getStatus().equals("Completed") ? "✅" : "❌",
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
