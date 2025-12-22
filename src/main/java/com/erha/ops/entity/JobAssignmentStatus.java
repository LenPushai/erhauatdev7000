package com.erha.ops.entity;

/**
 * Status for worker job assignments
 */
public enum JobAssignmentStatus {
    ASSIGNED("Assigned", "Worker assigned but not started"),
    STARTED("Started", "Worker actively working on job"),
    COMPLETED("Completed", "Worker finished their part"),
    REMOVED("Removed", "Assignment cancelled");

    private final String displayName;
    private final String description;

    JobAssignmentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
