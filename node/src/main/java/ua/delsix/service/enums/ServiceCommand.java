package ua.delsix.service.enums;

public enum ServiceCommand {
    NON_COMMAND(""),
    START("/start"),
    HELP("/help"),
    CANCEL("Cancel"),
    SKIP("Skip"),
    FINISH("Finish"),
    TASKS("Tasks"),
    UNCOMPLETED_TASKS("/uncompleted"),
    COMPLETED_TASKS("/completed"),
    TODAY_TASKS("/today"),
    CREATE_TASK("Create task");
    private final String value;

    ServiceCommand(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static ServiceCommand fromValue(String value) {
        for(ServiceCommand command: ServiceCommand.values()) {
            if(command.toString().equals(value))
                return command;
        }

        return NON_COMMAND;
    }
}
