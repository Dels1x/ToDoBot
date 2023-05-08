package ua.delsix.service.enums;

public enum ServiceCommand {
    nonCommand(""),
    start("/start"),
    help("/help"),
    cancel("Cancel"),
    tasks("Tasks"),
    createTask("Create task"),
    removeTask("Remove task"),
    editTask("Edit task");

    private final String value;

    ServiceCommand(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public boolean equals(String value) {
        return this.toString().equals(value);
    }

    public static ServiceCommand fromValue(String value) {
        for(ServiceCommand command: ServiceCommand.values()) {
            if(command.toString().equals(value))
                return command;
        }

        return null;
    }
}
