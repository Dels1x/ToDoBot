package ua.delsix.utils;

import org.springframework.stereotype.Component;
import ua.delsix.entity.Task;

import java.util.Arrays;
import java.util.List;

@Component
public class TaskUtils {

    public static final List<String> states = Arrays.asList("CREATING_NAME", "CREATING_DESCRIPTION", "CREATING_PRIORITY", "CREATING_DATE",
            "CREATING_DIFFICULTY", "CREATING_TAG", "COMPLETED");

    public String getDifficultyDescription(Integer diff) {
        if(diff == null) {
            return "Not specified";
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
            default -> "Not specified";
        };
    }

    public String getPriorityDescription(Integer priority) {
        if(priority == null) {
            return "Not specified";
        }

        return switch (priority) {
            case 1 -> "Not important";
            case 2 -> "Low";
            case 3 -> "Medium";
            case 4 -> "High";
            case 5 -> "Very high";
            case 6 -> "Urgent";
            default -> "Not specified";
        };
    }

    public String taskToString(Task task) {
        //TODO
        return "";
    }

    public String taskToStringInDetail(Task task) {
        System.out.println(task.toString());
        StringBuilder sb = new StringBuilder("Task:\n\n");
        sb.append(String.format("""
                Name: %s
                Description: %s
                Date: %s
                Priority: %s
                Difficulty: %s
                Tag: %s
                Status: %s
                Created at: %s
                """,
                task.getName() == null ? "Unnamed" : task.getName(),
                task.getDescription() == null ? "No description" : task.getDescription(),
                task.getTargetDate() == null ? "No date specified" : task.getTargetDate().toString(),
                getPriorityDescription(task.getPriority()),
                getDifficultyDescription(task.getDifficulty()),
                task.getTag() == null ? "Untagged" :  task.getTag(),
                task.getStatus() == null ? "Uncompleted" : task.getStatus(),
                task.getCreatedAt().toString()));

        if (task.getCompletionDate() != null) {
            sb.append(String.format("\nCompletion date: %s", task.getCompletionDate()));
        }

        return sb.toString();
    }

    public String responseForEachState(Task task) {
        String state = task.getState();

        return switch (state) {
            case "CREATING_NAME" -> "Now let's create a description for your task, if you want to.";
            case "CREATING_DESCRIPTION" -> """
                    We can also set a priority for your task - a number in range of 1-6.

                    If you don't want task to have a priority - press the skip button or type in "0".""";
            case "CREATING_PRIORITY" ->
                    "You can set a target completion date or a deadline for your task using the format \"dd.MM.yyyy\" (e.g. \"30.04.2023\")";
            case "CREATING_DATE" -> """
                    If you want to set a specific difficulty for your task - we can also do that.
                    a number in range of 0-7. (0 - No difficulty; 1 - Very easy; 2 - Easy; 3 - Moderate; 4 - Challenging; 5 - Difficult; 6 - Very Difficult; 7 - Extremely difficult)""";
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
