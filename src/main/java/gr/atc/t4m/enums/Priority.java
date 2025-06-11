package gr.atc.t4m.enums;

/*
 * Enum for Priority
 */
public enum Priority {
    LOW("Low"),
    MID("Mid"),
    HIGH("High");

    private final String priority;

    Priority(final String priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return priority;
    }

}