package com.erha.ops.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "casual_labor_entries")
public class CasualLaborEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_number")
    private String requestNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "casual_worker_id")
    private CasualWorker casualWorker;

    @Column(name = "originator")
    private String originator;

    @Column(name = "place")
    private String place;

    @Column(name = "work_item")
    private String workItem;

    @Column(name = "required_dates")
    private String requiredDates;

    @Column(name = "date_received")
    private LocalDate dateReceived;

    @Enumerated(EnumType.STRING)
    @Column(name = "pay_method")
    private PayMethod payMethod;

    @Column(name = "pay_date")
    private LocalDate payDate;

    @Column(name = "payment_amount", precision = 10, scale = 2)
    private BigDecimal paymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_status")
    private WorkStatus workStatus = WorkStatus.ASSIGNED;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum PayMethod { EFT, CASH }
    public enum PaymentStatus { PENDING, PAID, CANCELLED }
    public enum WorkStatus { REQUESTED, ASSIGNED, WORKING, COMPLETED, NO_SHOW, CANCELLED }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRequestNumber() { return requestNumber; }
    public void setRequestNumber(String requestNumber) { this.requestNumber = requestNumber; }
    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }
    public CasualWorker getCasualWorker() { return casualWorker; }
    public void setCasualWorker(CasualWorker casualWorker) { this.casualWorker = casualWorker; }
    public String getOriginator() { return originator; }
    public void setOriginator(String originator) { this.originator = originator; }
    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }
    public String getWorkItem() { return workItem; }
    public void setWorkItem(String workItem) { this.workItem = workItem; }
    public String getRequiredDates() { return requiredDates; }
    public void setRequiredDates(String requiredDates) { this.requiredDates = requiredDates; }
    public LocalDate getDateReceived() { return dateReceived; }
    public void setDateReceived(LocalDate dateReceived) { this.dateReceived = dateReceived; }
    public PayMethod getPayMethod() { return payMethod; }
    public void setPayMethod(PayMethod payMethod) { this.payMethod = payMethod; }
    public LocalDate getPayDate() { return payDate; }
    public void setPayDate(LocalDate payDate) { this.payDate = payDate; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public WorkStatus getWorkStatus() { return workStatus; }
    public void setWorkStatus(WorkStatus workStatus) { this.workStatus = workStatus; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}