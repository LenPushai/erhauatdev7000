package com.erha.ops.rfq.entity;

import com.erha.ops.rfq.enums.RfqStatus;
import com.erha.ops.rfq.enums.Priority;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "rfqs", indexes = {
        @Index(name = "idx_rfq_status", columnList = "status"),
        @Index(name = "idx_rfq_job_no", columnList = "job_no"),
        @Index(name = "idx_rfq_client", columnList = "client_id"),
        @Index(name = "idx_rfq_operating_entity", columnList = "operating_entity"),
        @Index(name = "idx_rfq_quote_number", columnList = "quote_number"),
        @Index(name = "idx_rfq_order_number", columnList = "order_number"),
        @Index(name = "idx_rfq_invoice_number", columnList = "invoice_number")
})
public class RFQ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_no", unique = true, nullable = false, length = 20)
    private String jobNo;

    // ============================================
    // CLIENT INFORMATION
    // ============================================
    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "contact_person", nullable = false, length = 100)
    private String contactPerson;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "department", length = 100)
    private String department;

    // ============================================
    // RFQ DETAILS
    // ============================================
    @Column(name = "operating_entity", nullable = false, length = 50)
    private String operatingEntity; // "ERHA FC" or "ERHA SS"

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "request_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate requestDate;

    @Column(name = "required_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate requiredDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "estimated_value", precision = 19, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "special_requirements", columnDefinition = "TEXT")
    private String specialRequirements;

    // ============================================
    // STATUS AND ASSIGNMENT
    // ============================================
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private RfqStatus status = RfqStatus.DRAFT;

    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(name = "assigned_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate assignedDate;

    @Column(name = "follow_up_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate followUpDate;

    // ============================================
    // QUOTE INFORMATION (from Pastel)
    // ============================================
    @Column(name = "quote_number", length = 50)
    private String quoteNumber;

    @Column(name = "quote_value_excl_vat", precision = 19, scale = 2)
    private BigDecimal quoteValueExclVat;

    @Column(name = "quote_value_incl_vat", precision = 19, scale = 2)
    private BigDecimal quoteValueInclVat;

    @Column(name = "quote_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate quoteDate;

    @Column(name = "quote_valid_until")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate quoteValidUntil;

    @Column(name = "quote_pdf_path", length = 500)
    private String quotePdfPath;

    @Column(name = "quote_status", length = 30)
    private String quoteStatus; // DRAFT, SENT, ACCEPTED, REJECTED

    // ============================================
    // DOCUSIGN INTEGRATION
    // ============================================
    @Column(name = "docusign_envelope_id", length = 100)
    private String docusignEnvelopeId;

    @Column(name = "docusign_status", length = 30)
    private String docusignStatus; // NOT_SENT, PENDING, SIGNED, DECLINED

    @Column(name = "signed_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate signedDate;

    // ============================================
    // ORDER INFORMATION
    // ============================================
    @Column(name = "order_number", length = 50)
    private String orderNumber;

    @Column(name = "order_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate orderDate;

    // ============================================
    // INVOICE INFORMATION (from Pastel)
    // ============================================
    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    @Column(name = "invoice_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate invoiceDate;

    @Column(name = "invoice_status", length = 30)
    private String invoiceStatus;
    // WORKFLOW STAGE 3: PDF Upload tracking
    @Column(name = "quote_pdf_upload_date")
    private LocalDateTime quotePdfUploadDate;

    // WORKFLOW STAGE 4: Customer signature tracking
    @Column(name = "signed_by", length = 200)
    private String signedBy;

    // WORKFLOW STAGE 5: Order value tracking
    @Column(name = "order_value", precision = 15, scale = 2)
    private BigDecimal orderValue;

    // WORKFLOW STAGE 6: Job linking
    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "job_number", length = 50)
    private String jobNumber;

    @Column(name = "job_created_date")
    private LocalDateTime jobCreatedDate;

    // WORKFLOW STAGE 7: Invoice value tracking
    @Column(name = "invoice_value", precision = 15, scale = 2)
    private BigDecimal invoiceValue;

    // WORKFLOW STAGE 8: Payment tracking
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "amount_paid", precision = 15, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod; // PENDING, SENT, PAID

    // ============================================
    // NOTES
    // ============================================
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // Internal notes

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks; // Client-facing remarks

    // ============================================
    // ENQ REPORT INFORMATION
    // ============================================
    @Column(name = "erha_department", length = 100)
    private String erhaDepartment;

    @Column(name = "assigned_quoter", length = 100)
    private String assignedQuoter;

    @Column(name = "media_received", length = 50)
    private String mediaReceived;

    @Column(name = "actions_required", length = 500)
    private String actionsRequired;

    @Column(name = "drawing_number", length = 200)
    private String drawingNumber;

    // ============================================
    // METADATA
    // ============================================
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // ============================================
    // CONSTRUCTORS
    // ============================================
    public RFQ() {}

    public RFQ(String jobNo, Long clientId, String contactPerson, String operatingEntity,
               String description, LocalDate requestDate, LocalDate requiredDate, Priority priority) {
        this.jobNo = jobNo;
        this.clientId = clientId;
        this.contactPerson = contactPerson;
        this.operatingEntity = operatingEntity;
        this.description = description;
        this.requestDate = requestDate;
        this.requiredDate = requiredDate;
        this.priority = priority;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
        if (status == null) {
            status = RfqStatus.DRAFT;
        }
        if (priority == null) {
            priority = Priority.MEDIUM;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ============================================
    // GETTERS AND SETTERS - Original Fields
    // ============================================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getJobNo() { return jobNo; }
    public void setJobNo(String jobNo) { this.jobNo = jobNo; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getOperatingEntity() { return operatingEntity; }
    public void setOperatingEntity(String operatingEntity) { this.operatingEntity = operatingEntity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDate requestDate) { this.requestDate = requestDate; }

    public LocalDate getRequiredDate() { return requiredDate; }
    public void setRequiredDate(LocalDate requiredDate) { this.requiredDate = requiredDate; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public BigDecimal getEstimatedValue() { return estimatedValue; }
    public void setEstimatedValue(BigDecimal estimatedValue) { this.estimatedValue = estimatedValue; }

    public String getSpecialRequirements() { return specialRequirements; }
    public void setSpecialRequirements(String specialRequirements) { this.specialRequirements = specialRequirements; }

    public RfqStatus getStatus() { return status; }
    public void setStatus(RfqStatus status) { this.status = status; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public LocalDate getAssignedDate() { return assignedDate; }
    public void setAssignedDate(LocalDate assignedDate) { this.assignedDate = assignedDate; }

    public LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(LocalDate followUpDate) { this.followUpDate = followUpDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ============================================
    // GETTERS AND SETTERS - Quote Fields
    // ============================================
    public String getQuoteNumber() { return quoteNumber; }
    public void setQuoteNumber(String quoteNumber) { this.quoteNumber = quoteNumber; }

    public BigDecimal getQuoteValueExclVat() { return quoteValueExclVat; }
    public void setQuoteValueExclVat(BigDecimal quoteValueExclVat) { this.quoteValueExclVat = quoteValueExclVat; }

    public BigDecimal getQuoteValueInclVat() { return quoteValueInclVat; }
    public void setQuoteValueInclVat(BigDecimal quoteValueInclVat) { this.quoteValueInclVat = quoteValueInclVat; }

    public LocalDate getQuoteDate() { return quoteDate; }
    public void setQuoteDate(LocalDate quoteDate) { this.quoteDate = quoteDate; }

    public LocalDate getQuoteValidUntil() { return quoteValidUntil; }
    public void setQuoteValidUntil(LocalDate quoteValidUntil) { this.quoteValidUntil = quoteValidUntil; }

    public String getQuotePdfPath() { return quotePdfPath; }
    public void setQuotePdfPath(String quotePdfPath) { this.quotePdfPath = quotePdfPath; }

    public String getQuoteStatus() { return quoteStatus; }
    public void setQuoteStatus(String quoteStatus) { this.quoteStatus = quoteStatus; }

    // ============================================
    // GETTERS AND SETTERS - DocuSign Fields
    // ============================================
    public String getDocusignEnvelopeId() { return docusignEnvelopeId; }
    public void setDocusignEnvelopeId(String docusignEnvelopeId) { this.docusignEnvelopeId = docusignEnvelopeId; }

    public String getDocusignStatus() { return docusignStatus; }
    public void setDocusignStatus(String docusignStatus) { this.docusignStatus = docusignStatus; }

    public LocalDate getSignedDate() { return signedDate; }
    public void setSignedDate(LocalDate signedDate) { this.signedDate = signedDate; }

    // ============================================
    // GETTERS AND SETTERS - Order Fields
    // ============================================
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }

    // ============================================
    // GETTERS AND SETTERS - Invoice Fields
    // ============================================
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }

    public String getInvoiceStatus() { return invoiceStatus; }
    public void setInvoiceStatus(String invoiceStatus) { this.invoiceStatus = invoiceStatus; }
    // ============================================
    // GETTERS AND SETTERS - ENQ Report Fields
    // ============================================
    public String getErhaDepartment() { return erhaDepartment; }
    public void setErhaDepartment(String erhaDepartment) { this.erhaDepartment = erhaDepartment; }

    public String getAssignedQuoter() { return assignedQuoter; }
    public void setAssignedQuoter(String assignedQuoter) { this.assignedQuoter = assignedQuoter; }

    public String getMediaReceived() { return mediaReceived; }
    public void setMediaReceived(String mediaReceived) { this.mediaReceived = mediaReceived; }

    public String getActionsRequired() { return actionsRequired; }
    public void setActionsRequired(String actionsRequired) { this.actionsRequired = actionsRequired; }

    public String getDrawingNumber() { return drawingNumber; }
    public void setDrawingNumber(String drawingNumber) { this.drawingNumber = drawingNumber; }


    // ============================================
    // EQUALS, HASHCODE, TOSTRING
    // ============================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RFQ rfq = (RFQ) o;
        return Objects.equals(id, rfq.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("RFQ{id=%d, jobNo='%s', entity='%s', client=%d, contact='%s', status=%s, priority=%s, quoteNumber='%s', orderNumber='%s', invoiceNumber='%s'}",
                id, jobNo, operatingEntity, clientId, contactPerson, status, priority, quoteNumber, orderNumber, invoiceNumber);
    }

    public LocalDateTime getQuotePdfUploadDate() { return quotePdfUploadDate; }
    public void setQuotePdfUploadDate(LocalDateTime quotePdfUploadDate) { this.quotePdfUploadDate = quotePdfUploadDate; }

    public String getSignedBy() { return signedBy; }
    public void setSignedBy(String signedBy) { this.signedBy = signedBy; }

    public BigDecimal getOrderValue() { return orderValue; }
    public void setOrderValue(BigDecimal orderValue) { this.orderValue = orderValue; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getJobNumber() { return jobNumber; }
    public void setJobNumber(String jobNumber) { this.jobNumber = jobNumber; }

    public LocalDateTime getJobCreatedDate() { return jobCreatedDate; }
    public void setJobCreatedDate(LocalDateTime jobCreatedDate) { this.jobCreatedDate = jobCreatedDate; }

    public BigDecimal getInvoiceValue() { return invoiceValue; }
    public void setInvoiceValue(BigDecimal invoiceValue) { this.invoiceValue = invoiceValue; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }}