package com.erha.ops.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "jarison_monthly_hours")
public class JarisonMonthlyHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_id", nullable = false)
    private JarisonBatchImport batchImport;

    @Column(name = "jarison_code", nullable = false, length = 20)
    private String jarisonCode;

    @Column(name = "employee_name", length = 100)
    private String employeeName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private Worker worker;

    @Column(name = "total_hours", precision = 10, scale = 2)
    private BigDecimal totalHours;

    @Column(name = "normal_hours", precision = 10, scale = 2)
    private BigDecimal normalHours;

    @Column(name = "ot_hours_1_5", precision = 10, scale = 2)
    private BigDecimal otHours15;

    @Column(name = "ot_hours_2_0", precision = 10, scale = 2)
    private BigDecimal otHours20;

    @Column(name = "erha_job_hours", precision = 10, scale = 2)
    private BigDecimal erhaJobHours = BigDecimal.ZERO;

    @Column(name = "variance_hours", precision = 10, scale = 2)
    private BigDecimal varianceHours = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status")
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.PENDING;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ReconciliationStatus {
        PENDING, MATCHED, VARIANCE, UNMATCHED
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public JarisonBatchImport getBatchImport() { return batchImport; }
    public void setBatchImport(JarisonBatchImport batchImport) { this.batchImport = batchImport; }

    public String getJarisonCode() { return jarisonCode; }
    public void setJarisonCode(String jarisonCode) { this.jarisonCode = jarisonCode; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public Worker getWorker() { return worker; }
    public void setWorker(Worker worker) { this.worker = worker; }

    public BigDecimal getTotalHours() { return totalHours; }
    public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }

    public BigDecimal getNormalHours() { return normalHours; }
    public void setNormalHours(BigDecimal normalHours) { this.normalHours = normalHours; }

    public BigDecimal getOtHours15() { return otHours15; }
    public void setOtHours15(BigDecimal otHours15) { this.otHours15 = otHours15; }

    public BigDecimal getOtHours20() { return otHours20; }
    public void setOtHours20(BigDecimal otHours20) { this.otHours20 = otHours20; }

    public BigDecimal getErhaJobHours() { return erhaJobHours; }
    public void setErhaJobHours(BigDecimal erhaJobHours) { this.erhaJobHours = erhaJobHours; }

    public BigDecimal getVarianceHours() { return varianceHours; }
    public void setVarianceHours(BigDecimal varianceHours) { this.varianceHours = varianceHours; }

    public ReconciliationStatus getReconciliationStatus() { return reconciliationStatus; }
    public void setReconciliationStatus(ReconciliationStatus reconciliationStatus) { this.reconciliationStatus = reconciliationStatus; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}