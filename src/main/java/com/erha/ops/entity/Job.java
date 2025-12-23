package com.erha.ops.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs")
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "job_number", unique = true, nullable = false, length = 50)
    private String jobNumber;

    @Column(name = "rfq_id")
    private Long rfqId;

    @Column(name = "quote_id")
    private Long quoteId;
    
    // ========================================
    // PARENT/CHILD JOB HIERARCHY
    // ========================================
    
    @Column(name = "parent_job_id")
    private Long parentJobId;
    
    @Column(name = "job_sequence", length = 10)
    private String jobSequence;
    
    @Column(name = "is_parent_job")
    private Boolean isParentJob = false;
    
    @Column(name = "billing_type", length = 20)
    private String billingType = "INDIVIDUAL";
    
    @Column(name = "creation_source", length = 20)
    private String creationSource = "RFQ";

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type")
    private JobType jobType = JobType.NORMAL;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private JobLocation location = JobLocation.SHOP;

    @Column(name = "client_id")
    private Long clientId;

    private String department;

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "order_number", length = 100)
    private String orderNumber;

    @Column(name = "order_received_date")
    private LocalDate orderReceivedDate;

    @Column(name = "order_value_excl", precision = 15, scale = 2)
    private BigDecimal orderValueExcl;

    @Column(name = "order_value_incl", precision = 15, scale = 2)
    private BigDecimal orderValueIncl;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.NEW;

    @Enumerated(EnumType.STRING)
    private JobPriority priority = JobPriority.MEDIUM;

    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    // ========================================
    // PHASE 2: WORKSHOP STATUS (KANBAN)
    // ========================================
    
    @Enumerated(EnumType.STRING)
    @Column(name = "workshop_status")
    private WorkshopStatus workshopStatus = WorkshopStatus.NEW;
    
    @Column(name = "compiled_by", length = 100)
    private String compiledBy;  // Zoey (Contract) or Jeanic (Quoted)
    
    // ========================================
    // PHASE 2: ACTIONS REQUIRED (Job Card Checkboxes)
    // ========================================
    
    @Column(name = "action_manufacture")
    private Boolean actionManufacture = false;
    
    @Column(name = "action_service")
    private Boolean actionService = false;
    
    @Column(name = "action_repair")
    private Boolean actionRepair = false;
    
    @Column(name = "action_modify")
    private Boolean actionModify = false;
    
    @Column(name = "action_sandblast")
    private Boolean actionSandblast = false;
    
    @Column(name = "action_paint")
    private Boolean actionPaint = false;
    
    @Column(name = "action_installation")
    private Boolean actionInstallation = false;
    
    @Column(name = "action_prepare_material")
    private Boolean actionPrepareMaterial = false;
    
    @Column(name = "action_cut")
    private Boolean actionCut = false;
    
    @Column(name = "action_other")
    private Boolean actionOther = false;
    
    @Column(name = "action_other_description", length = 255)
    private String actionOtherDescription;
    
    // ========================================
    // PHASE 2: ATTACHED DOCUMENTS FLAGS
    // ========================================
    
    @Column(name = "has_service_schedule")
    private Boolean hasServiceSchedule = false;
    
    @Column(name = "has_drawing")
    private Boolean hasDrawing = false;
    
    @Column(name = "has_internal_order")
    private Boolean hasInternalOrder = false;
    
    @Column(name = "has_qcp")
    private Boolean hasQcp = false;
    
    @Column(name = "has_info_for_quote")
    private Boolean hasInfoForQuote = false;
    
    @Column(name = "has_list_as_quoted")
    private Boolean hasListAsQuoted = false;
    
    // ========================================
    // PHASE 2: SUPERVISOR PLANNING
    // ========================================
    
    @Column(name = "date_received")
    private LocalDate dateReceived;
    
    @Column(name = "material_ordered_date")
    private LocalDate materialOrderedDate;
    
    @Column(name = "completion_date")
    private LocalDate completionDate;  // At least 2 days before delivery
    
    // ========================================
    // PHASE 2: DIGITAL SIGNATURES
    // ========================================
    
    @Column(name = "supervisor_signed_by")
    private Long supervisorSignedBy;
    
    @Column(name = "supervisor_signed_at")
    private LocalDateTime supervisorSignedAt;
    
    @Column(name = "employee_signed_by")
    private Long employeeSignedBy;
    
    @Column(name = "employee_signed_at")
    private LocalDateTime employeeSignedAt;
    
    // ========================================
    // PHASE 2: QC COMPLETION
    // ========================================
    
    @Column(name = "qc_completed_at")
    private LocalDateTime qcCompletedAt;
    
    @Column(name = "qc_completed_by")
    private Long qcCompletedBy;
    
    // ========================================
    // PHASE 2: RELATIONSHIPS
    // ========================================
    
    @JsonIgnore
    @OneToMany(mappedBy = "job", fetch = FetchType.LAZY)
    private List<JobAssignment> assignments = new ArrayList<>();
    
    @JsonIgnore
    @OneToMany(mappedBy = "job", fetch = FetchType.LAZY)
    private List<JobHoldingPointSignoff> holdingPointSignoffs = new ArrayList<>();
    
    @JsonIgnore
    @OneToMany(mappedBy = "job", fetch = FetchType.LAZY)
    private List<TimeEntry> timeEntries = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        lastModifiedDate = LocalDateTime.now();
        if (workshopStatus == null) {
            workshopStatus = WorkshopStatus.NEW;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
    }

    // Enums
    public enum JobType {
        NORMAL, SHUTDOWN, CONTRACT, INTERNAL
    }

    public enum JobLocation {
        SHOP, SITE, BOTH
    }

    public enum JobStatus {
        NEW, IN_PROGRESS, QUALITY_CHECK, READY, DELIVERED, INVOICED, COMPLETE
    }

    public enum JobPriority {
        LOW, MEDIUM, HIGH, URGENT
    }

    // ========================================
    // PHASE 1 GETTERS/SETTERS
    // ========================================
    
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getJobNumber() { return jobNumber; }
    public void setJobNumber(String jobNumber) { this.jobNumber = jobNumber; }
    public Long getRfqId() { return rfqId; }
    public void setRfqId(Long rfqId) { this.rfqId = rfqId; }
    public Long getQuoteId() { return quoteId; }
    public void setQuoteId(Long quoteId) { this.quoteId = quoteId; }
    public JobType getJobType() { return jobType; }
    public void setJobType(JobType jobType) { this.jobType = jobType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public JobLocation getLocation() { return location; }
    public void setLocation(JobLocation location) { this.location = location; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public LocalDate getOrderReceivedDate() { return orderReceivedDate; }
    public void setOrderReceivedDate(LocalDate orderReceivedDate) { this.orderReceivedDate = orderReceivedDate; }
    public BigDecimal getOrderValueExcl() { return orderValueExcl; }
    public void setOrderValueExcl(BigDecimal orderValueExcl) { this.orderValueExcl = orderValueExcl; }
    public BigDecimal getOrderValueIncl() { return orderValueIncl; }
    public void setOrderValueIncl(BigDecimal orderValueIncl) { this.orderValueIncl = orderValueIncl; }
    public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) { this.expectedDeliveryDate = expectedDeliveryDate; }
    public LocalDate getActualDeliveryDate() { return actualDeliveryDate; }
    public void setActualDeliveryDate(LocalDate actualDeliveryDate) { this.actualDeliveryDate = actualDeliveryDate; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public JobPriority getPriority() { return priority; }
    public void setPriority(JobPriority priority) { this.priority = priority; }
    public Integer getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public String getLastModifiedBy() { return lastModifiedBy; }
    public void setLastModifiedBy(String lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }
    public LocalDateTime getLastModifiedDate() { return lastModifiedDate; }
    public void setLastModifiedDate(LocalDateTime lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }

    // Parent/Child
    public Long getParentJobId() { return parentJobId; }
    public void setParentJobId(Long parentJobId) { this.parentJobId = parentJobId; }
    public String getJobSequence() { return jobSequence; }
    public void setJobSequence(String jobSequence) { this.jobSequence = jobSequence; }
    public Boolean getIsParentJob() { return isParentJob; }
    public void setIsParentJob(Boolean isParentJob) { this.isParentJob = isParentJob; }
    public String getBillingType() { return billingType; }
    public void setBillingType(String billingType) { this.billingType = billingType; }
    public String getCreationSource() { return creationSource; }
    public void setCreationSource(String creationSource) { this.creationSource = creationSource; }

    // ========================================
    // PHASE 2 GETTERS/SETTERS
    // ========================================
    
    // Workshop Status
    public WorkshopStatus getWorkshopStatus() { return workshopStatus; }
    public void setWorkshopStatus(WorkshopStatus workshopStatus) { this.workshopStatus = workshopStatus; }
    public String getCompiledBy() { return compiledBy; }
    public void setCompiledBy(String compiledBy) { this.compiledBy = compiledBy; }
    
    // Actions
    public Boolean getActionManufacture() { return actionManufacture; }
    public void setActionManufacture(Boolean actionManufacture) { this.actionManufacture = actionManufacture; }
    public Boolean getActionService() { return actionService; }
    public void setActionService(Boolean actionService) { this.actionService = actionService; }
    public Boolean getActionRepair() { return actionRepair; }
    public void setActionRepair(Boolean actionRepair) { this.actionRepair = actionRepair; }
    public Boolean getActionModify() { return actionModify; }
    public void setActionModify(Boolean actionModify) { this.actionModify = actionModify; }
    public Boolean getActionSandblast() { return actionSandblast; }
    public void setActionSandblast(Boolean actionSandblast) { this.actionSandblast = actionSandblast; }
    public Boolean getActionPaint() { return actionPaint; }
    public void setActionPaint(Boolean actionPaint) { this.actionPaint = actionPaint; }
    public Boolean getActionInstallation() { return actionInstallation; }
    public void setActionInstallation(Boolean actionInstallation) { this.actionInstallation = actionInstallation; }
    public Boolean getActionPrepareMaterial() { return actionPrepareMaterial; }
    public void setActionPrepareMaterial(Boolean actionPrepareMaterial) { this.actionPrepareMaterial = actionPrepareMaterial; }
    public Boolean getActionCut() { return actionCut; }
    public void setActionCut(Boolean actionCut) { this.actionCut = actionCut; }
    public Boolean getActionOther() { return actionOther; }
    public void setActionOther(Boolean actionOther) { this.actionOther = actionOther; }
    public String getActionOtherDescription() { return actionOtherDescription; }
    public void setActionOtherDescription(String actionOtherDescription) { this.actionOtherDescription = actionOtherDescription; }
    
    // Document Flags
    public Boolean getHasServiceSchedule() { return hasServiceSchedule; }
    public void setHasServiceSchedule(Boolean hasServiceSchedule) { this.hasServiceSchedule = hasServiceSchedule; }
    public Boolean getHasDrawing() { return hasDrawing; }
    public void setHasDrawing(Boolean hasDrawing) { this.hasDrawing = hasDrawing; }
    public Boolean getHasInternalOrder() { return hasInternalOrder; }
    public void setHasInternalOrder(Boolean hasInternalOrder) { this.hasInternalOrder = hasInternalOrder; }
    public Boolean getHasQcp() { return hasQcp; }
    public void setHasQcp(Boolean hasQcp) { this.hasQcp = hasQcp; }
    public Boolean getHasInfoForQuote() { return hasInfoForQuote; }
    public void setHasInfoForQuote(Boolean hasInfoForQuote) { this.hasInfoForQuote = hasInfoForQuote; }
    public Boolean getHasListAsQuoted() { return hasListAsQuoted; }
    public void setHasListAsQuoted(Boolean hasListAsQuoted) { this.hasListAsQuoted = hasListAsQuoted; }
    
    // Planning
    public LocalDate getDateReceived() { return dateReceived; }
    public void setDateReceived(LocalDate dateReceived) { this.dateReceived = dateReceived; }
    public LocalDate getMaterialOrderedDate() { return materialOrderedDate; }
    public void setMaterialOrderedDate(LocalDate materialOrderedDate) { this.materialOrderedDate = materialOrderedDate; }
    public LocalDate getCompletionDate() { return completionDate; }
    public void setCompletionDate(LocalDate completionDate) { this.completionDate = completionDate; }
    
    // Signatures
    public Long getSupervisorSignedBy() { return supervisorSignedBy; }
    public void setSupervisorSignedBy(Long supervisorSignedBy) { this.supervisorSignedBy = supervisorSignedBy; }
    public LocalDateTime getSupervisorSignedAt() { return supervisorSignedAt; }
    public void setSupervisorSignedAt(LocalDateTime supervisorSignedAt) { this.supervisorSignedAt = supervisorSignedAt; }
    public Long getEmployeeSignedBy() { return employeeSignedBy; }
    public void setEmployeeSignedBy(Long employeeSignedBy) { this.employeeSignedBy = employeeSignedBy; }
    public LocalDateTime getEmployeeSignedAt() { return employeeSignedAt; }
    public void setEmployeeSignedAt(LocalDateTime employeeSignedAt) { this.employeeSignedAt = employeeSignedAt; }
    
    // QC
    public LocalDateTime getQcCompletedAt() { return qcCompletedAt; }
    public void setQcCompletedAt(LocalDateTime qcCompletedAt) { this.qcCompletedAt = qcCompletedAt; }
    public Long getQcCompletedBy() { return qcCompletedBy; }
    public void setQcCompletedBy(Long qcCompletedBy) { this.qcCompletedBy = qcCompletedBy; }
    
    // Relationships
    public List<JobAssignment> getAssignments() { return assignments; }
    public void setAssignments(List<JobAssignment> assignments) { this.assignments = assignments; }
    public List<JobHoldingPointSignoff> getHoldingPointSignoffs() { return holdingPointSignoffs; }
    public void setHoldingPointSignoffs(List<JobHoldingPointSignoff> holdingPointSignoffs) { this.holdingPointSignoffs = holdingPointSignoffs; }
    public List<TimeEntry> getTimeEntries() { return timeEntries; }
    public void setTimeEntries(List<TimeEntry> timeEntries) { this.timeEntries = timeEntries; }

    // ========================================
    // HELPER METHODS
    // ========================================
    
    public String getFullJobNumber() {
        if (parentJobId != null && jobSequence != null) {
            return jobNumber;
        }
        return jobNumber;
    }
    
    public boolean isChildJob() {
        return parentJobId != null;
    }
    
    public boolean hasChildren() {
        return isParentJob != null && isParentJob;
    }
    
    /**
     * Check if job is ready for workshop (has all required info)
     */
    public boolean isReadyForWorkshop() {
        return workshopStatus != null && 
               description != null && 
               !description.isEmpty();
    }
    
    /**
     * Get list of selected actions as strings
     */
    public List<String> getSelectedActions() {
        List<String> actions = new ArrayList<>();
        if (Boolean.TRUE.equals(actionManufacture)) actions.add("Manufacture");
        if (Boolean.TRUE.equals(actionService)) actions.add("Service");
        if (Boolean.TRUE.equals(actionRepair)) actions.add("Repair");
        if (Boolean.TRUE.equals(actionModify)) actions.add("Modify");
        if (Boolean.TRUE.equals(actionSandblast)) actions.add("Sandblast");
        if (Boolean.TRUE.equals(actionPaint)) actions.add("Paint");
        if (Boolean.TRUE.equals(actionInstallation)) actions.add("Installation");
        if (Boolean.TRUE.equals(actionPrepareMaterial)) actions.add("Prepare Material");
        if (Boolean.TRUE.equals(actionCut)) actions.add("Cut");
        if (Boolean.TRUE.equals(actionOther)) actions.add(actionOtherDescription != null ? actionOtherDescription : "Other");
        return actions;
    }

    // Delivery fields
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "delivered_by", length = 100)
    private String deliveredBy;

    @Column(name = "delivery_vehicle", length = 50)
    private String deliveryVehicle;

    @Column(name = "delivery_note_number", length = 50)
    private String deliveryNoteNumber;

    @Column(name = "received_by", length = 100)
    private String receivedBy;

    @Column(name = "delivery_signature", columnDefinition = "LONGTEXT")
    private String deliverySignature;

    @Column(name = "delivery_notes", columnDefinition = "TEXT")
    private String deliveryNotes;

    // Delivery getters and setters
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public String getDeliveredBy() { return deliveredBy; }
    public void setDeliveredBy(String deliveredBy) { this.deliveredBy = deliveredBy; }

    public String getDeliveryVehicle() { return deliveryVehicle; }
    public void setDeliveryVehicle(String deliveryVehicle) { this.deliveryVehicle = deliveryVehicle; }

    public String getDeliveryNoteNumber() { return deliveryNoteNumber; }
    public void setDeliveryNoteNumber(String deliveryNoteNumber) { this.deliveryNoteNumber = deliveryNoteNumber; }

    public String getReceivedBy() { return receivedBy; }
    public void setReceivedBy(String receivedBy) { this.receivedBy = receivedBy; }

    public String getDeliverySignature() { return deliverySignature; }
    public void setDeliverySignature(String deliverySignature) { this.deliverySignature = deliverySignature; }

    public String getDeliveryNotes() { return deliveryNotes; }
    public void setDeliveryNotes(String deliveryNotes) { this.deliveryNotes = deliveryNotes; }
}