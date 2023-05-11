package ua.delsix.utils;

import org.springframework.stereotype.Component;
import ua.delsix.entity.Task;

import java.util.Arrays;
import java.util.List;

@Component
public class TaskUtils {

    public static final List<String> states = Arrays.asList("CREATING_NAME", "CREATING_DESCRIPTION", "CREATING_PRIORITY", "CREATING_DATE",
            "CREATING_DIFFICULTY", "CREATING_TAG", "COMPLETED");

    public String getDifficultyRate(int diff) {
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

    public String taskToString(Task task) {
        //TODO
        return "";
    }

    public String taskToStringInDetail(Task task) {
        if(task.getCompletionDate() != null) {
            return String.format("""
                    Task:
                                    
                    Name: %s
                    Description: %s
                    Date: %s
                    Priority: %d
                    Difficulty: %s
                    Tag: %s
                    Status: %s
                    
                    Completion date: %s""",
                    task.getName(),
                    task.getDescription(),
                    task.getTargetDate().toString(),
                    task.getPriority(),
                    getDifficultyRate(task.getDifficulty()),
                    task.getTag(),
                    task.getStatus(),
                    task.getCompletionDate().toString());
        } else {
            return String.format("""
                    Task:
                                    
                    Name: %s
                    Description: %s
                    Date: %s
                    Priority: %d
                    Difficulty: %s
                    Tag: %s
                    Status: %s""",
                    task.getName(),
                    task.getDescription(),
                    task.getTargetDate().toString(),
                    task.getPriority(),
                    getDifficultyRate(task.getDifficulty()),
                    task.getTag(),
                    task.getStatus());
        }
    }

    public String responseForEachState(Task task) {
        String[] statesArray = states.toArray(new String[0]);
        String state = task.getState();

        switch (state) {
            case "CREATING_NAME" -> {
                return "Now let's create a description for your task, if you want to.";
            }
            case "CREATING_DESCRIPTION" -> {
                return """
                        We can also set a priority for your task - a number in range of 1-6.

                        If you don't want task to have a priority - press the skip button or type in "0".""";
            }
            case "CREATING_PRIORITY" -> {
                return "You can set a target completion date or a deadline for your task using the format \"dd.MM.yyyy\" (e.g. \"30.04.2023\")";
            }
            case "CREATING_DATE" -> {
                return """
                        \n\nIf you want to set a specific difficulty for your task - we can also do that.
                        a number in range of 0-7. (0 - No difficulty; 1 - Very easy; 2 - Easy; 3 - Moderate; 4 - Challenging; 5 - Difficult; 6 - Very Difficult; 7 - Extremely difficult)""";
            }
            case "CREATING_DIFFICULTY" -> {
                return "\n\nIf you want to - you can set a specific tag for the task. It could be something like: (Goals, Programming, Chores etc.\"";
            }
            case "COMPLETED" -> {
                return String.format("""
                        Your task is successfully created!
                        
                        If there is need, you can also create subtasks for this task using this task's id (%d)
                        
                        This is how your task looks like:
                        %s""",
                        task.getId(),
                        taskToStringInDetail(task));
            }
            default -> {
                return "Unknown state error";
            }
        }
    }
}
