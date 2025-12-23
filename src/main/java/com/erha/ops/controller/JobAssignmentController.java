package com.erha.ops.controller;

import com.erha.ops.entity.JobAssignment;
import com.erha.ops.entity.RoleOnJob;
import com.erha.ops.service.JobAssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/assignments")
@CrossOrigin(origins = "*")
public class JobAssignmentController {

    private static final Logger logger = LoggerFactory.getLogger(JobAssignmentController.class);

    @Autowired
    private JobAssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<?> assignWorker(@RequestBody AssignmentRequest request) {
        try {
            RoleOnJob role = request.role != null ? RoleOnJob.valueOf(request.role.toUpperCase()) : RoleOnJob.ARTISAN;
            JobAssignment a = assignmentService.assignWorkerToJob(request.jobId, request.workerId, request.assignedById, role);
            return ResponseEntity.ok(toDTO(a));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> assignMultipleWorkers(@RequestBody BulkAssignmentRequest request) {
        try {
            List<JobAssignment> list = assignmentService.assignWorkersToJob(request.jobId, request.workerIds, request.assignedById);
            return ResponseEntity.ok(Map.of("assigned", list.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/jobs/{jobId}/workers/{workerId}/start")
    public ResponseEntity<?> startWork(@PathVariable Long jobId, @PathVariable Long workerId) {
        try { return ResponseEntity.ok(toDTO(assignmentService.startWork(jobId, workerId))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/jobs/{jobId}/workers/{workerId}/complete")
    public ResponseEntity<?> completeAssignment(@PathVariable Long jobId, @PathVariable Long workerId) {
        try { return ResponseEntity.ok(toDTO(assignmentService.completeAssignment(jobId, workerId))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/jobs/{jobId}/workers/{workerId}")
    public ResponseEntity<?> removeWorker(@PathVariable Long jobId, @PathVariable Long workerId) {
        try { assignmentService.removeWorkerFromJob(jobId, workerId); return ResponseEntity.ok(Map.of("message", "Worker removed")); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @Transactional(readOnly = true)
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<List<Map<String, Object>>> getJobAssignments(@PathVariable Long jobId) {
        return ResponseEntity.ok(assignmentService.getJobAssignments(jobId).stream().map(this::toDTO).toList());
    }

    @Transactional(readOnly = true)
    @GetMapping("/jobs/{jobId}/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveJobAssignments(@PathVariable Long jobId) {
        return ResponseEntity.ok(assignmentService.getActiveJobAssignments(jobId).stream().map(this::toDTO).toList());
    }

    @Transactional(readOnly = true)
    @GetMapping("/workers/{workerId}/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveWorkerAssignments(@PathVariable Long workerId) {
        return ResponseEntity.ok(assignmentService.getActiveWorkerAssignments(workerId).stream().map(this::toDTO).toList());
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() { return ResponseEntity.ok(assignmentService.getStatistics()); }

    private Map<String, Object> toDTO(JobAssignment a) {
        return Map.of("id", a.getId(), "jobId", a.getJob().getJobId(), "jobNumber", a.getJob().getJobNumber(),
            "workerId", a.getWorker().getId(), "workerName", a.getWorker().getFirstName() + " " + a.getWorker().getLastName(),
            "role", a.getRole().name(), "status", a.getStatus().name());
    }

    public static class AssignmentRequest { public Long jobId; public Long workerId; public Long assignedById; public String role; }
    public static class BulkAssignmentRequest { public Long jobId; public List<Long> workerIds; public Long assignedById; }
}