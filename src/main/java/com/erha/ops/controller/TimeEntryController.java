package com.erha.ops.controller;

import com.erha.ops.entity.TimeEntry;
import com.erha.ops.service.TimeEntryService;
import com.erha.ops.service.TimeEntryService.JobTimeSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/time-entries")
@CrossOrigin(origins = "*")
public class TimeEntryController {

    private static final Logger logger = LoggerFactory.getLogger(TimeEntryController.class);

    @Autowired
    private TimeEntryService timeEntryService;

    @PostMapping
    public ResponseEntity<?> logTime(@RequestBody TimeEntryRequest request) {
        try {
            LocalDate workDate = request.workDate != null ? LocalDate.parse(request.workDate) : LocalDate.now();
            TimeEntry entry = timeEntryService.logTime(request.jobId, request.workerId, workDate, request.normalHours, request.overtimeHours, request.notes);
            return ResponseEntity.ok(toDTO(entry));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/today")
    public ResponseEntity<?> logTimeToday(@RequestBody TimeEntryRequest request) {
        try {
            TimeEntry entry = timeEntryService.logTimeToday(request.jobId, request.workerId, request.normalHours, request.overtimeHours, request.notes);
            return ResponseEntity.ok(toDTO(entry));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submitTimeEntry(@PathVariable Long id) {
        try { return ResponseEntity.ok(toDTO(timeEntryService.submitTimeEntry(id))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveTimeEntry(@PathVariable Long id, @RequestBody Map<String, Long> request) {
        try { return ResponseEntity.ok(toDTO(timeEntryService.approveTimeEntry(id, request.get("approvedById")))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectTimeEntry(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Long rejectedById = request.get("rejectedById") != null ? ((Number) request.get("rejectedById")).longValue() : null;
            return ResponseEntity.ok(toDTO(timeEntryService.rejectTimeEntry(id, rejectedById, (String) request.get("reason"))));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<List<Map<String, Object>>> getJobTimeEntries(@PathVariable Long jobId) {
        return ResponseEntity.ok(timeEntryService.getJobTimeEntries(jobId).stream().map(this::toDTO).toList());
    }

    @GetMapping("/workers/{workerId}/today")
    public ResponseEntity<List<Map<String, Object>>> getTodayEntries(@PathVariable Long workerId) {
        return ResponseEntity.ok(timeEntryService.getTodayEntriesForWorker(workerId).stream().map(this::toDTO).toList());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingApproval() {
        return ResponseEntity.ok(timeEntryService.getPendingApproval().stream().map(this::toDTO).toList());
    }

    @GetMapping("/jobs/{jobId}/summary")
    public ResponseEntity<JobTimeSummary> getJobTimeSummary(@PathVariable Long jobId) {
        return ResponseEntity.ok(timeEntryService.getJobTimeSummary(jobId));
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() { return ResponseEntity.ok(timeEntryService.getStatistics()); }

    private Map<String, Object> toDTO(TimeEntry e) {
        return Map.of("id", e.getId(), "jobId", e.getJob().getJobId(), "jobNumber", e.getJob().getJobNumber(),
            "workerId", e.getWorker().getId(), "workerName", e.getWorker().getFirstName() + " " + e.getWorker().getLastName(),
            "workDate", e.getWorkDate().toString(), "normalHours", e.getNormalHours(), "overtimeHours", e.getOvertimeHours(), "status", e.getStatus().name());
    }

    public static class TimeEntryRequest { public Long jobId; public Long workerId; public String workDate; public BigDecimal normalHours; public BigDecimal overtimeHours; public String notes; }
}
