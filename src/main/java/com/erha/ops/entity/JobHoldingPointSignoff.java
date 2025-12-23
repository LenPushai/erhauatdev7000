package com.erha.ops.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_holding_point_signoffs")
public class JobHoldingPointSignoff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holding_point_id", nullable = false)
    private HoldingPoint holdingPoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SignoffStatus status = SignoffStatus.PENDING;

    @Column(name = "signed_by_id")
    private Long signedById;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }
    public HoldingPoint getHoldingPoint() { return holdingPoint; }
    public void setHoldingPoint(HoldingPoint holdingPoint) { this.holdingPoint = holdingPoint; }
    public SignoffStatus getStatus() { return status; }
    public void setStatus(SignoffStatus status) { this.status = status; }
    public Long getSignedById() { return signedById; }
    public void setSignedById(Long signedById) { this.signedById = signedById; }
    public LocalDateTime getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDateTime signedAt) { this.signedAt = signedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
