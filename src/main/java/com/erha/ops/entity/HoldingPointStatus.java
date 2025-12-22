package com.erha.ops.entity;

/**
 * Status for individual QC holding point sign-offs
 */
public enum HoldingPointStatus {
    PENDING("Pending", "Not yet inspected"),
    PASSED("Passed", "Inspection passed"),
    FAILED("Failed", "Inspection failed - requires rework"),
    NOT_APPLICABLE("N/A", "Not applicable to this job");

    private final String displayName;
    private final String description;

    HoldingPointStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
