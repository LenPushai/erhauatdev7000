package com.erha.ops.controller;

import com.erha.ops.job.entity.JobSubTask;
import com.erha.ops.job.repository.JobSubTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176", "http://localhost:5177", "http://localhost:5178"})
public class JobSubTaskController {

    private static final Logger logger = LoggerFactory.getLogger(JobSubTaskController.class);

    @Autowired
    private JobSubTaskRepository subTaskRepository;

    @GetMapping("/jobs/{jobId}/sub-tasks")
    public ResponseEntity<List<JobSubTask>> getSubTasks(@PathVariable Long jobId) {
        logger.info("Fetching sub tasks for Job ID: {}", jobId);
        List<JobSubTask> subTasks = subTaskRepository.findByJobIdOrderByTaskNumberAsc(jobId);
        return ResponseEntity.ok(subTasks);
    }

    @PostMapping("/jobs/{jobId}/sub-tasks/batch")
    public ResponseEntity<List<JobSubTask>> createSubTasksBatch(@PathVariable Long jobId, @RequestBody List<JobSubTask> subTasks) {
        logger.info("Creating {} sub tasks for Job ID: {}", subTasks.size(), jobId);
        
        for (int i = 0; i < subTasks.size(); i++) {
            JobSubTask task = subTasks.get(i);
            task.setJobId(jobId);
            task.setTaskNumber(i + 1);
            if (task.getCreatedAt() == null) task.setCreatedAt(LocalDateTime.now());
            if (task.getUpdatedAt() == null) task.setUpdatedAt(LocalDateTime.now());
        }
        
        List<JobSubTask> saved = subTaskRepository.saveAll(subTasks);
        logger.info("Saved {} sub tasks for Job ID: {}", saved.size(), jobId);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/jobs/{jobId}/sub-tasks/{subTaskId}")
    public ResponseEntity<JobSubTask> updateSubTask(@PathVariable Long jobId, @PathVariable Long subTaskId, @RequestBody JobSubTask subTaskDetails) {
        logger.info("Updating sub task {} for Job ID: {}", subTaskId, jobId);
        
        return subTaskRepository.findById(subTaskId).map(subTask -> {
            if (subTaskDetails.getTaskNumber() != null) subTask.setTaskNumber(subTaskDetails.getTaskNumber());
            if (subTaskDetails.getOperationType() != null) subTask.setOperationType(subTaskDetails.getOperationType());
            if (subTaskDetails.getDescription() != null) subTask.setDescription(subTaskDetails.getDescription());
            if (subTaskDetails.getAssignedTo() != null) subTask.setAssignedTo(subTaskDetails.getAssignedTo());
            if (subTaskDetails.getEstimatedHours() != null) subTask.setEstimatedHours(subTaskDetails.getEstimatedHours());
            if (subTaskDetails.getActualHours() != null) subTask.setActualHours(subTaskDetails.getActualHours());
            if (subTaskDetails.getStatus() != null) subTask.setStatus(subTaskDetails.getStatus());
            if (subTaskDetails.getDueDate() != null) subTask.setDueDate(subTaskDetails.getDueDate());
            if (subTaskDetails.getNotes() != null) subTask.setNotes(subTaskDetails.getNotes());
            
            subTask.setUpdatedAt(LocalDateTime.now());
            JobSubTask updated = subTaskRepository.save(subTask);
            return ResponseEntity.ok(updated);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/jobs/{jobId}/sub-tasks/{subTaskId}")
    public ResponseEntity<Map<String, Object>> deleteSubTask(@PathVariable Long jobId, @PathVariable Long subTaskId) {
        logger.info("Deleting sub task {} for Job ID: {}", subTaskId, jobId);
        
        if (subTaskRepository.existsById(subTaskId)) {
            subTaskRepository.deleteById(subTaskId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Sub task deleted"));
        }
        return ResponseEntity.notFound().build();
    }
}