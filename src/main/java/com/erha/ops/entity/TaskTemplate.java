package com.erha.ops.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "task_templates")
public class TaskTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "template_name", nullable = false, unique = true, length = 100)
    private String templateName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "department", length = 50)
    private String department;

    @Column(name = "estimated_total_hours", precision = 5, scale = 2)
    private BigDecimal estimatedTotalHours;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sequenceNumber ASC")
    private List<TemplateTask> tasks;

    public TaskTemplate() {
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
        this.isActive = true;
    }

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public BigDecimal getEstimatedTotalHours() { return estimatedTotalHours; }
    public void setEstimatedTotalHours(BigDecimal estimatedTotalHours) { this.estimatedTotalHours = estimatedTotalHours; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }
    public List<TemplateTask> getTasks() { return tasks; }
    public void setTasks(List<TemplateTask> tasks) { this.tasks = tasks; }

    @PreUpdate
    protected void onUpdate() { this.updatedDate = LocalDateTime.now(); }
}
