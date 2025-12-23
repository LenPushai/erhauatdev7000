package com.erha.ops.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_notes")
public class DeliveryNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_note_number", unique = true, nullable = false, length = 20)
    private String deliveryNoteNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeliveryNoteStatus status = DeliveryNoteStatus.GENERATED;

    @Column(name = "delivered_by", length = 100)
    private String deliveredBy;

    @Column(name = "received_by", length = 100)
    private String receivedBy;

    @Column(name = "vehicle_info", length = 100)
    private String vehicleInfo;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "customer_signature", columnDefinition = "TEXT")
    private String customerSignature;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeliveryNoteNumber() { return deliveryNoteNumber; }
    public void setDeliveryNoteNumber(String deliveryNoteNumber) { this.deliveryNoteNumber = deliveryNoteNumber; }
    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }
    public DeliveryNoteStatus getStatus() { return status; }
    public void setStatus(DeliveryNoteStatus status) { this.status = status; }
    public String getDeliveredBy() { return deliveredBy; }
    public void setDeliveredBy(String deliveredBy) { this.deliveredBy = deliveredBy; }
    public String getReceivedBy() { return receivedBy; }
    public void setReceivedBy(String receivedBy) { this.receivedBy = receivedBy; }
    public String getVehicleInfo() { return vehicleInfo; }
    public void setVehicleInfo(String vehicleInfo) { this.vehicleInfo = vehicleInfo; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCustomerSignature() { return customerSignature; }
    public void setCustomerSignature(String customerSignature) { this.customerSignature = customerSignature; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(LocalDateTime dispatchedAt) { this.dispatchedAt = dispatchedAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public LocalDateTime getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDateTime signedAt) { this.signedAt = signedAt; }
}
