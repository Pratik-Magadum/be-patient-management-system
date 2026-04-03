package com.eyehospital.pms.common.enums;

/**
 * Appointment status progression: {@code REGISTERED → IN_PROGRESS → COMPLETED}.
 */
public enum AppointmentStatus {

    REGISTERED,
    IN_PROGRESS,
    COMPLETED;

    /**
     * Returns the next valid status in the workflow, or {@code null} if already terminal.
     */
    public AppointmentStatus next() {
        return switch (this) {
            case REGISTERED  -> IN_PROGRESS;
            case IN_PROGRESS -> COMPLETED;
            case COMPLETED   -> null;
        };
    }

    /**
     * Checks whether transitioning from this status to {@code target} is valid.
     */
    public boolean canTransitionTo(AppointmentStatus target) {
        return target != null && target.equals(next());
    }
}
