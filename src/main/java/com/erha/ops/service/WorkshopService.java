package com.erha.ops.service;

import com.erha.ops.entity.*;
import com.erha.ops.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkshopService {

    private static final Logger logger = LoggerFactory.getLogger(WorkshopService.class);

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    @Autowired
    private JobAssignmentRepository jobAssignmentRepository;

    public Map<String, List<JobKanbanDTO>> getKanbanBoard() {
        logger.info("Building Kanban board");
        Map<String, List<JobKanbanDTO>> kanban = new LinkedHashMap<>();
        for (WorkshopStatus status : WorkshopStatus.values()) {
            kanban.put(status.name(), new ArrayList<>());
        }
        List<Job> jobs = jobRepository.findAll();
        for (Job job : jobs) {
            WorkshopStatus status = job.getWorkshopStatus();
            if (status == null) status = WorkshopStatus.NEW;
            kanban.get(status.name()).add(toKanbanDTO(job));
        }
        return kanban;
    }

    public List<JobKanbanDTO> getJobsByWorkshopStatus(WorkshopStatus status) {
        return jobRepository.findAll().stream()
                .filter(j -> status.equals(j.getWorkshopStatus()))
                .map(this::toKanbanDTO)
                .collect(Collectors.toList());
    }

    public Job advanceJobStatus(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        WorkshopStatus current = job.getWorkshopStatus();
        if (current == null) current = WorkshopStatus.NEW;
        WorkshopStatus next = getNextStatus(current);
        if (next != null) {
            job.setWorkshopStatus(next);
            job = jobRepository.save(job);
        }
        return job;
    }

    public Job setJobWorkshopStatus(Long jobId, WorkshopStatus status) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        job.setWorkshopStatus(status);
        return jobRepository.save(job);
    }

    public Job completeJob(Long jobId, Long qcInspectorId, String qcInspectorName, 
                          Long shopManagerId, String shopManagerName, String notes) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        
        // Validate job is in QC_IN_PROGRESS or READY_FOR_DELIVERY
        if (job.getWorkshopStatus() != WorkshopStatus.QC_IN_PROGRESS && 
            job.getWorkshopStatus() != WorkshopStatus.READY_FOR_DELIVERY) {
            throw new RuntimeException("Job must be in QC Check status to complete");
        }
        
        // Record QC Inspector sign-off (employee)
        job.setEmployeeSignedBy(qcInspectorId);
        job.setEmployeeSignedAt(LocalDateTime.now());
        
        // Record Shop Manager sign-off (supervisor)
        job.setSupervisorSignedBy(shopManagerId);
        job.setSupervisorSignedAt(LocalDateTime.now());
        
        // Record QC completion
        job.setQcCompletedBy(qcInspectorId);
        job.setQcCompletedAt(LocalDateTime.now());
        
        // Advance to READY_FOR_DELIVERY
        job.setWorkshopStatus(WorkshopStatus.READY_FOR_DELIVERY);
        
        logger.info("Job {} completed - QC: {}, Manager: {}", job.getJobNumber(), qcInspectorName, shopManagerName);
        
        return jobRepository.save(job);
    }

    public Job initializeJobForWorkshop(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        if (job.getWorkshopStatus() == null) {
            job.setWorkshopStatus(WorkshopStatus.NEW);
        }
        return jobRepository.save(job);
    }

    public WorkshopStatistics getWorkshopStatistics() {
        List<Job> jobs = jobRepository.findAll();
        WorkshopStatistics stats = new WorkshopStatistics();
        stats.totalJobs = jobs.size();
        for (Job job : jobs) {
            WorkshopStatus status = job.getWorkshopStatus();
            if (status == null) status = WorkshopStatus.NEW;
            switch (status) {
                case NEW: stats.newJobs++; break;
                case ASSIGNED: stats.assignedJobs++; break;
                case IN_PROGRESS: stats.inProgressJobs++; break;
                case QC_IN_PROGRESS: stats.qcInProgressJobs++; break;
                case READY_FOR_DELIVERY: stats.readyForDeliveryJobs++; break;
                case DELIVERED: stats.deliveredJobs++; break;
                case INVOICED: stats.invoicedJobs++; break;
            }
        }
        return stats;
    }

    private WorkshopStatus getNextStatus(WorkshopStatus current) {
        switch (current) {
            case NEW: return WorkshopStatus.ASSIGNED;
            case ASSIGNED: return WorkshopStatus.IN_PROGRESS;
            case IN_PROGRESS: return WorkshopStatus.QC_IN_PROGRESS;
            case QC_IN_PROGRESS: return WorkshopStatus.READY_FOR_DELIVERY;
            case READY_FOR_DELIVERY: return WorkshopStatus.DELIVERED;
            case DELIVERED: return WorkshopStatus.INVOICED;
            default: return null;
        }
    }

    private JobKanbanDTO toKanbanDTO(Job job) {
        JobKanbanDTO dto = new JobKanbanDTO();
        dto.jobId = job.getJobId();
        dto.jobNumber = job.getJobNumber();
        dto.description = job.getDescription();
        dto.workshopStatus = job.getWorkshopStatus() != null ? job.getWorkshopStatus().name() : "NEW";
        dto.priority = job.getPriority() != null ? job.getPriority().name() : "MEDIUM";
        dto.clientId = job.getClientId();

        dto.orderNumber = job.getOrderNumber();
        dto.expectedDeliveryDate = job.getExpectedDeliveryDate();

        if (job.getClientId() != null) {
            clientRepository.findById(job.getClientId())
                    .ifPresent(client -> dto.clientName = client.getCompanyName());
        }

        if (job.getQuoteId() != null) {
            quoteRepository.findById(job.getQuoteId())
                    .ifPresent(quote -> dto.quoteNumber = quote.getQuoteNumber());
        }

        try {
            List<JobAssignment> assignments = jobAssignmentRepository.findByJobJobId(job.getJobId());
            dto.workerCount = assignments != null ? assignments.size() : 0;
        } catch (Exception e) {
            dto.workerCount = 0;
        }

        try {
            List<TimeEntry> timeEntries = timeEntryRepository.findByJobJobId(job.getJobId());
            BigDecimal totalHours = BigDecimal.ZERO;
            for (TimeEntry entry : timeEntries) {
                if (entry.getNormalHours() != null) totalHours = totalHours.add(entry.getNormalHours());
                if (entry.getOvertimeHours() != null) totalHours = totalHours.add(entry.getOvertimeHours());
            }
            dto.totalHoursLogged = totalHours.doubleValue();
        } catch (Exception e) {
            dto.totalHoursLogged = 0.0;
        }

        dto.qcProgress = 0;

        return dto;
    }

    public static class JobKanbanDTO {
        public Long jobId;
        public String jobNumber;
        public String description;
        public String workshopStatus;
        public String priority;
        public Long clientId;
        public String clientName;
        public String orderNumber;
        public String quoteNumber;
        public LocalDate expectedDeliveryDate;
        public int workerCount;
        public int qcProgress;
        public Double totalHoursLogged;
    }

    public static class WorkshopStatistics {
        public int totalJobs;
        public int newJobs;
        public int assignedJobs;
        public int inProgressJobs;
        public int qcInProgressJobs;
        public int readyForDeliveryJobs;
        public int deliveredJobs;
        public int invoicedJobs;
    }
}