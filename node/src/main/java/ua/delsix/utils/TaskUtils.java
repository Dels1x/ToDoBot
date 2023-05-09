package ua.delsix.utils;

import ua.delsix.entity.Task;

import java.util.Arrays;
import java.util.List;

public class TaskUtils {

    public static final List<String> states = Arrays.asList("CREATING_NAME", "CREATING_DESCRIPTION", "CREATING_PRIORITY", "CREATING_DATE",
            "CREATING_DIFFICULTY", "CREATING_TAG", "COMPLETED");

    public String taskToString(Task task) {
        //TODO
        return "";
    }
}
