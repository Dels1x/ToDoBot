package ua.delsix.enums;

public enum ServiceCommand {
    NON_COMMAND(""),
    START("/start"),
    HELP("/help"),
    SETTINGS("/settings"),
    CANCEL("Cancel"),
    SKIP("Skip"),
    FINISH("Finish"),
    TASKS("/tasks"),
    UNCOMPLETED_TASKS("/uncompleted"),
    COMPLETED_TASKS("/completed"),
    TODAY_TASKS("/today"),
    TAGS("/tags"),
    CREATE_TASK("/create"),
    DELETE_COMPLETED_TASKS("/clear_completed"),
    DELETE_ALL_TASKS("/clear_all");
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

        return switch (value.toLowerCase()) {
            case "cancel", "отменить", "скасувати" -> CANCEL;
            case "skip", "пропустить", "пропустити" -> SKIP;
            case "finish", "закончить", "закінчити" -> FINISH;
            case "tasks", "задачи", "завдання" -> TASKS;
            case "tags", "тэги", "теги" -> TAGS;
            case "create task", "создать задачу","створити завдання" -> CREATE_TASK;
            case "completed tasks", "выполненные задачи", "виконані завдання" -> COMPLETED_TASKS;
            case "uncompleted tasks", "незавершённые задачи", "незавершені завдання" -> UNCOMPLETED_TASKS;
            case "today tasks", "задачи на сегодня", "завдання на сьогодні" -> TODAY_TASKS;
            default -> NON_COMMAND;
        };
    }
}
