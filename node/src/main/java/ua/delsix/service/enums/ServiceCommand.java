package ua.delsix.service.enums;

public enum ServiceCommand {
    start("/start"),
    help("/help");

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
