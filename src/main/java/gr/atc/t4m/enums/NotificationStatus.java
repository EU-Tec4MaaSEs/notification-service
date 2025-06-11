package gr.atc.t4m.enums;

/*
 * Enum for Notification Status
 */
public enum NotificationStatus {
    UNREAD("Unread"),
    READ("Read");

    private final String status;

    NotificationStatus(final String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return status;
    }
}