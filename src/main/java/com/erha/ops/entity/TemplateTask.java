package com.erha.ops.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "template_tasks")
public class TemplateTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_task_id")
    private Long templateTaskId;

    @ManyToOne
    @JoinColumn(name = "template_id", nullable = false)
    @JsonIgnore
    private TaskTemplate template;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "estimated_hours", precision = 5, scale = 2)
    private BigDecimal estimatedHours;

    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public TemplateTask() {}

    public Long getTemplateTaskId() { return templateTaskId; }
    public void setTemplateTaskId(Long templateTaskId) { this.templateTaskId = templateTaskId; }
    public TaskTemplate getTemplate() { return template; }
    public void setTemplate(TaskTemplate template) { this.template = template; }
    public Integer getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Integer sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(BigDecimal estimatedHours) { this.estimatedHours = estimatedHours; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
