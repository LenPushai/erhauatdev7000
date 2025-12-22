package com.erha.ops.controller;

import com.erha.ops.entity.WorkshopStatus;
import com.erha.ops.service.WorkshopService;
import com.erha.ops.service.WorkshopService.JobKanbanDTO;
import com.erha.ops.service.WorkshopService.WorkshopStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workshop")
@CrossOrigin(origins = "*")
public class WorkshopController {

    private static final Logger logger = LoggerFactory.getLogger(WorkshopController.class);

    @Autowired
    private WorkshopService workshopService;

    @GetMapping("/kanban")
    public ResponseEntity<Map<String, List<JobKanbanDTO>>> getKanbanBoard() {
        return ResponseEntity.ok(workshopService.getKanbanBoard());
    }

    @GetMapping("/jobs/status/{status}")
    public ResponseEntity<List<JobKanbanDTO>> getJobsByStatus(@PathVariable String status) {
        try {
            return ResponseEntity.ok(workshopService.getJobsByWorkshopStatus(WorkshopStatus.valueOf(status.toUpperCase())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/jobs/{jobId}/advance")
    public ResponseEntity<?> advanceJobStatus(@PathVariable Long jobId) {
        try {
            var job = workshopService.advanceJobStatus(jobId);
            return ResponseEntity.ok(Map.of("jobId", job.getJobId(), "workshopStatus", job.getWorkshopStatus().name()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/jobs/{jobId}/status")
    public ResponseEntity<?> setJobStatus(@PathVariable Long jobId, @RequestBody Map<String, String> request) {
        try {
            var job = workshopService.setJobWorkshopStatus(jobId, WorkshopStatus.valueOf(request.get("status").toUpperCase()));
            return ResponseEntity.ok(Map.of("jobId", job.getJobId(), "workshopStatus", job.getWorkshopStatus().name()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/jobs/{jobId}/complete")
    public ResponseEntity<?> completeJob(@PathVariable Long jobId, @RequestBody JobCompletionRequest request) {
        try {
            var job = workshopService.completeJob(jobId, request.qcInspectorId, request.qcInspectorName, 
                request.shopManagerId, request.shopManagerName, request.notes);
            return ResponseEntity.ok(Map.of(
                "jobId", job.getJobId(), 
                "workshopStatus", job.getWorkshopStatus().name(),
                "message", "Job completed and ready for delivery"
            ));
        } catch (Exception e) {
            logger.error("Error completing job: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/jobs/{jobId}/initialize")
    public ResponseEntity<?> initializeJob(@PathVariable Long jobId) {
        try {
            var job = workshopService.initializeJobForWorkshop(jobId);
            return ResponseEntity.ok(Map.of("jobId", job.getJobId(), "message", "Job initialized"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<WorkshopStatistics> getStatistics() {
        return ResponseEntity.ok(workshopService.getWorkshopStatistics());
    }

    @GetMapping("/statuses")
    public ResponseEntity<List<Map<String, String>>> getStatuses() {
        List<Map<String, String>> statuses = new java.util.ArrayList<>();
        for (WorkshopStatus status : WorkshopStatus.values()) {
            statuses.add(Map.of("value", status.name(), "label", status.name().replace("_", " ")));
        }
        return ResponseEntity.ok(statuses);
    }

    public static class JobCompletionRequest {
        public Long qcInspectorId;
        public String qcInspectorName;
        public Long shopManagerId;
        public String shopManagerName;
        public String notes;
    }
}