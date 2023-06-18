package ua.delsix.service.enums;

public enum ServiceCommand {
    NON_COMMAND(""),
    START("/start"),
    HELP("/help"),
    CANCEL("Cancel"),
    SKIP("Skip"),
    FINISH("Finish"),
    TASKS("/tasks"),
    UNCOMPLETED_TASKS("/uncompleted"),
    COMPLETED_TASKS("/completed"),
    TODAY_TASKS("/today"),
    TAGS("/tags"),
    CREATE_TASK("/create"),
    DELETE_COMPLETED_TASKS("/clearCompleted"),
    DELETE_ALL_TASKS("/clearAll");
    private final String value;

    ServiceCommand(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static ServiceCommand fromValue(String value) {
        if (value == null) {
            return NON_COMMAND;
        }

        for(ServiceCommand command: ServiceCommand.values()) {
            if(command.toString().equals(value))
                return command;
        }

        return switch (value) {
            case "Tasks" -> TASKS;
            case "Tags" -> TAGS;
            case "Create task" -> CREATE_TASK;
            case "Completed tasks" -> COMPLETED_TASKS;
            case "Uncompleted tasks" -> UNCOMPLETED_TASKS;
            case "Today tasks" -> TODAY_TASKS;
            default -> NON_COMMAND;
        };
    }
}
