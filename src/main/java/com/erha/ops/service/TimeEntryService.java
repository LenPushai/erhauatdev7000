package com.erha.ops.service;

import com.erha.ops.entity.*;
import com.erha.ops.repository.JobRepository;
import com.erha.ops.repository.TimeEntryRepository;
import com.erha.ops.repository.WorkerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class TimeEntryService {

    private static final Logger logger = LoggerFactory.getLogger(TimeEntryService.class);

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private JobAssignmentService assignmentService;

    public TimeEntry logTime(Long jobId, Long workerId, LocalDate workDate, BigDecimal normalHours, BigDecimal overtimeHours, String notes) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        Worker worker = workerRepository.findById(workerId).orElseThrow(() -> new RuntimeException("Worker not found: " + workerId));

        // Auto-assign worker if not already assigned (for workshop flexibility)
        if (!assignmentService.isWorkerAssignedToJob(jobId, workerId)) {
            assignmentService.assignWorkerToJob(jobId, workerId, null, RoleOnJob.ARTISAN);
            logger.info("Auto-assigned worker {} to job {}", workerId, jobId);
        }

        BigDecimal nt = normalHours != null ? normalHours : BigDecimal.ZERO;
        BigDecimal ot = overtimeHours != null ? overtimeHours : BigDecimal.ZERO;
        if (nt.add(ot).compareTo(new BigDecimal("24")) > 0) {
            throw new RuntimeException("Total hours cannot exceed 24 per day");
        }
        TimeEntry entry = new TimeEntry();
        entry.setJob(job);
        entry.setWorker(worker);
        entry.setWorkDate(workDate);
        entry.setNormalHours(nt);
        entry.setOvertimeHours(ot);
        entry.setNotes(notes);
        entry.setStatus(TimeEntryStatus.DRAFT);
        entry.setCreatedAt(LocalDateTime.now());
        return timeEntryRepository.save(entry);
    }

    public TimeEntry logTimeToday(Long jobId, Long workerId, BigDecimal normalHours, BigDecimal overtimeHours, String notes) {
        return logTime(jobId, workerId, LocalDate.now(), normalHours, overtimeHours, notes);
    }

    public TimeEntry submitTimeEntry(Long entryId) {
        TimeEntry entry = timeEntryRepository.findById(entryId).orElseThrow(() -> new RuntimeException("Time entry not found"));
        if (entry.getStatus() != TimeEntryStatus.DRAFT) throw new RuntimeException("Only DRAFT entries can be submitted");
        entry.setStatus(TimeEntryStatus.SUBMITTED);
        entry.setSubmittedAt(LocalDateTime.now());
        return timeEntryRepository.save(entry);
    }

    public List<TimeEntry> submitAllDraftEntries(Long workerId, LocalDate startDate, LocalDate endDate) {
        List<TimeEntry> drafts = timeEntryRepository.findByWorkerIdAndStatusAndWorkDateBetween(workerId, TimeEntryStatus.DRAFT, startDate, endDate);
        for (TimeEntry entry : drafts) {
            entry.setStatus(TimeEntryStatus.SUBMITTED);
            entry.setSubmittedAt(LocalDateTime.now());
        }
        return timeEntryRepository.saveAll(drafts);
    }

    public TimeEntry approveTimeEntry(Long entryId, Long approvedById) {
        TimeEntry entry = timeEntryRepository.findById(entryId).orElseThrow(() -> new RuntimeException("Time entry not found"));
        if (entry.getStatus() != TimeEntryStatus.SUBMITTED) throw new RuntimeException("Only SUBMITTED entries can be approved");
        entry.setStatus(TimeEntryStatus.APPROVED);
        entry.setApprovedAt(LocalDateTime.now());
        entry.setApprovedById(approvedById);
        return timeEntryRepository.save(entry);
    }

    public TimeEntry rejectTimeEntry(Long entryId, Long rejectedById, String reason) {
        TimeEntry entry = timeEntryRepository.findById(entryId).orElseThrow(() -> new RuntimeException("Time entry not found"));
        if (entry.getStatus() != TimeEntryStatus.SUBMITTED) throw new RuntimeException("Only SUBMITTED entries can be rejected");
        entry.setStatus(TimeEntryStatus.REJECTED);
        entry.setRejectionReason(reason);
        return timeEntryRepository.save(entry);
    }

    public List<TimeEntry> getJobTimeEntries(Long jobId) { return timeEntryRepository.findByJobJobId(jobId); }
    public List<TimeEntry> getWorkerTimeEntries(Long workerId) { return timeEntryRepository.findByWorkerId(workerId); }
    public List<TimeEntry> getTodayEntriesForWorker(Long workerId) { return timeEntryRepository.findByWorkerIdAndWorkDate(workerId, LocalDate.now()); }
    public List<TimeEntry> getPendingApproval() { return timeEntryRepository.findByStatus(TimeEntryStatus.SUBMITTED); }
    public List<TimeEntry> getEntriesForDateRange(LocalDate startDate, LocalDate endDate) { return timeEntryRepository.findByWorkDateBetween(startDate, endDate); }

    public JobTimeSummary getJobTimeSummary(Long jobId) {
        List<TimeEntry> entries = timeEntryRepository.findByJobJobIdAndStatus(jobId, TimeEntryStatus.APPROVED);
        JobTimeSummary summary = new JobTimeSummary();
        summary.jobId = jobId;
        summary.totalNormalHours = BigDecimal.ZERO;
        summary.totalOvertimeHours = BigDecimal.ZERO;
        for (TimeEntry entry : entries) {
            summary.totalNormalHours = summary.totalNormalHours.add(entry.getNormalHours());
            summary.totalOvertimeHours = summary.totalOvertimeHours.add(entry.getOvertimeHours());
        }
        summary.totalHours = summary.totalNormalHours.add(summary.totalOvertimeHours);
        summary.entryCount = entries.size();
        return summary;
    }

    public BigDecimal getWorkerTotalHours(Long workerId, LocalDate startDate, LocalDate endDate) {
        return timeEntryRepository.findByWorkerIdAndWorkDateBetween(workerId, startDate, endDate).stream()
                .map(e -> e.getNormalHours().add(e.getOvertimeHours())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntries", timeEntryRepository.count());
        stats.put("pendingApproval", timeEntryRepository.findByStatus(TimeEntryStatus.SUBMITTED).size());
        return stats;
    }

    public static class JobTimeSummary {
        public Long jobId;
        public BigDecimal totalNormalHours;
        public BigDecimal totalOvertimeHours;
        public BigDecimal totalHours;
        public int entryCount;
    }
}
