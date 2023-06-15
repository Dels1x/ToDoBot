package ua.delsix.utils;

import org.springframework.stereotype.Component;
import ua.delsix.entity.Task;

import java.util.Arrays;
import java.util.List;

@Component
public class TaskUtils {

    public static final List<String> states = Arrays.asList("CREATING_NAME", "CREATING_DESCRIPTION", "CREATING_DATE", "CREATING_PRIORITY", "CREATING_DIFFICULTY", "CREATING_TAG", "COMPLETED");

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
            case 6 -> "Urgent";
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
                task.getDescription() == null ? "no description" :
                        task.getDescription(),
                task.getTargetDate() == null ? "No date specified" :
                        String.format("📅 %s ", task.getTargetDate()),
                getPriorityDescription(task.getPriority()).equals("❌") ? "" :
                        String.format("⭐️️ %s ", getPriorityDescription(task.getPriority())),
                getDifficultyDescription(task.getDifficulty()).equals("❌") ? "" :
                        String.format("⚡️ %s ", getDifficultyDescription(task.getDifficulty())),
                task.getTag() == null ? "Untagged" :
                        task.getTag());
    }

    public String taskToStringInDetail(Task task) {
        return "Task:\n\n" + String.format("""
                        ✏️ Name: *%s*
                        💬 Description: _%s_
                        📅 Date: _%s_
                        ⭐️ Priority: *%s*
                        ⚡️ Difficulty: *%s*
                        🏷️ Tag: #%s
                        🕒 Created at: _%s_
                        ❔ Completed: _%s_
                        %s
                        """,
                task.getName() == null ? "❌" : task.getName(),
                task.getDescription() == null ? "❌" : task.getDescription(),
                task.getTargetDate() == null ? "❌" : task.getTargetDate().toString(),
                getPriorityDescription(task.getPriority()),
                getDifficultyDescription(task.getDifficulty()),
                task.getTag() == null ? "❌" : task.getTag(),
                task.getCreatedAt().toString(),
                task.getStatus() == null ? "❌" :
                        task.getStatus().equals("Completed") ? "✅" : "❌",
                task.getCompletionDate() == null ? "" :
                        "⌛️ Completed at: ".concat(task.getCompletionDate().toString()));
    }

    public String responseForEachState(Task task) {
        String state = task.getState();

        return switch (state) {
            case "CREATING_NAME" -> "Now let's create a *description* for your task, if you want to.";
            case "CREATING_DESCRIPTION" -> """
                    You can set a target *completion date* or a *deadline* for your task using the format "yyyy-MM-dd" (e.g. "2023-04-30")
                        
                    *You can also use today/tomorrow*""\";
                    """;
            case "CREATING_DATE" -> """
                    You can also set a *priority* for your task - a number in range of 1-6:
                   
                    1 = Not important
                    2 = Low priority
                    3 = Medium priority
                    4 = High priority
                    5 = Very high priority
                    6 = Urgent priority
                    """;

            case "CREATING_PRIORITY" -> """
                    If you want to set a specific difficulty for your task - we can also do that.
                  
                    Enter a number in range of 0-7:
                    0 = No difficulty
                    1 = Very easy
                    2 = Easy
                    3 = Moderate
                    4 = Challenging
                    5 = Difficult
                    6 = Very Difficult
                    7 = Extremely difficult""";
            case "CREATING_DIFFICULTY" ->
                    "If you want to - you can set a specific tag for the task. It could be something like: (Goals, Programming, Chores etc.\"";
            case "CREATING_TAG", "COMPLETED" -> String.format("""
                            Your task is successfully created!
                                                    
                            If there is need, you can also create subtasks for this task using buttons or directly using this task's id: %d
                                                    
                            This is how your task looks like:
                            %s""",
                    task.getId(),
                    taskToStringInDetail(task));
            default -> "Unknown state error";
        };
    }
}
