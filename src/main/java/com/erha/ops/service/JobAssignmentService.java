package com.erha.ops.service;

import com.erha.ops.entity.*;
import com.erha.ops.repository.JobAssignmentRepository;
import com.erha.ops.repository.JobRepository;
import com.erha.ops.repository.WorkerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class JobAssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(JobAssignmentService.class);

    @Autowired
    private JobAssignmentRepository assignmentRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private WorkerRepository workerRepository;

    public JobAssignment assignWorkerToJob(Long jobId, Long workerId, Long assignedById, RoleOnJob role) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        Worker worker = workerRepository.findById(workerId).orElseThrow(() -> new RuntimeException("Worker not found: " + workerId));
        if (assignmentRepository.findByJobJobIdAndWorkerIdAndStatusIn(jobId, workerId, List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.STARTED)).isPresent()) {
            throw new RuntimeException("Worker already assigned to this job");
        }
        JobAssignment assignment = new JobAssignment();
        assignment.setJob(job);
        assignment.setWorker(worker);
        assignment.setRole(role != null ? role : RoleOnJob.ARTISAN);
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setAssignedById(assignedById);
        JobAssignment saved = assignmentRepository.save(assignment);
        
        // Auto-advance job from NEW to ASSIGNED when first worker is assigned
        if (job.getWorkshopStatus() == WorkshopStatus.NEW) {
            job.setWorkshopStatus(WorkshopStatus.ASSIGNED);
            jobRepository.save(job);
            logger.info("Job {} advanced to ASSIGNED status", job.getJobNumber());
        }
        
        return saved;
    }

    public List<JobAssignment> assignWorkersToJob(Long jobId, List<Long> workerIds, Long assignedById) {
        List<JobAssignment> assignments = new ArrayList<>();
        for (Long workerId : workerIds) {
            try {
                assignments.add(assignWorkerToJob(jobId, workerId, assignedById, RoleOnJob.ARTISAN));
            } catch (Exception e) {
                logger.warn("Could not assign worker {}: {}", workerId, e.getMessage());
            }
        }
        return assignments;
    }

    public JobAssignment setLeadWorker(Long jobId, Long workerId, Long assignedById) {
        Optional<JobAssignment> existing = assignmentRepository.findByJobJobIdAndWorkerIdAndStatusIn(jobId, workerId, List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.STARTED));
        if (existing.isPresent()) {
            JobAssignment assignment = existing.get();
            assignment.setRole(RoleOnJob.LEAD);
            return assignmentRepository.save(assignment);
        } else {
            return assignWorkerToJob(jobId, workerId, assignedById, RoleOnJob.LEAD);
        }
    }

    public JobAssignment startWork(Long jobId, Long workerId) {
        JobAssignment assignment = assignmentRepository.findByJobJobIdAndWorkerIdAndStatusIn(jobId, workerId, List.of(AssignmentStatus.ASSIGNED))
            .orElseThrow(() -> new RuntimeException("No active assignment found"));
        assignment.setStatus(AssignmentStatus.STARTED);
        assignment.setStartedAt(LocalDateTime.now());
        Job job = assignment.getJob();
        if (job.getWorkshopStatus() == WorkshopStatus.ASSIGNED || job.getWorkshopStatus() == WorkshopStatus.NEW) {
            job.setWorkshopStatus(WorkshopStatus.IN_PROGRESS);
            jobRepository.save(job);
        }
        return assignmentRepository.save(assignment);
    }

    public JobAssignment completeAssignment(Long jobId, Long workerId) {
        JobAssignment assignment = assignmentRepository.findByJobJobIdAndWorkerIdAndStatusIn(jobId, workerId, List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.STARTED))
            .orElseThrow(() -> new RuntimeException("No active assignment found"));
        assignment.setStatus(AssignmentStatus.COMPLETED);
        assignment.setCompletedAt(LocalDateTime.now());
        return assignmentRepository.save(assignment);
    }

    public void removeWorkerFromJob(Long jobId, Long workerId) {
        JobAssignment assignment = assignmentRepository.findByJobJobIdAndWorkerIdAndStatusIn(jobId, workerId, List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.STARTED))
            .orElseThrow(() -> new RuntimeException("No active assignment found"));
        assignment.setStatus(AssignmentStatus.REMOVED);
        assignmentRepository.save(assignment);
        
        // Check if all workers removed - revert to NEW if no active assignments remain
        List<JobAssignment> remaining = getActiveJobAssignments(jobId);
        if (remaining.isEmpty()) {
            Job job = assignment.getJob();
            if (job.getWorkshopStatus() == WorkshopStatus.ASSIGNED) {
                job.setWorkshopStatus(WorkshopStatus.NEW);
                jobRepository.save(job);
                logger.info("Job {} reverted to NEW status - no workers assigned", job.getJobNumber());
            }
        }
    }

    public List<JobAssignment> getJobAssignments(Long jobId) { return assignmentRepository.findByJobJobId(jobId); }
    public List<JobAssignment> getActiveJobAssignments(Long jobId) { return assignmentRepository.findByJobJobIdAndStatusIn(jobId, List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.STARTED)); }
    public List<JobAssignment> getWorkerAssignments(Long workerId) { return assignmentRepository.findByWorkerId(workerId); }
    public List<JobAssignment> getActiveWorkerAssignments(Long workerId) { return assignmentRepository.findByWorkerIdAndStatusIn(workerId, List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.STARTED)); }
    public Optional<JobAssignment> getLeadWorker(Long jobId) { return assignmentRepository.findByJobJobIdAndRoleAndStatusIn(jobId, RoleOnJob.LEAD, List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.STARTED)); }
    public boolean isWorkerAssignedToJob(Long jobId, Long workerId) { return assignmentRepository.findByJobJobIdAndWorkerIdAndStatusIn(jobId, workerId, List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.STARTED)).isPresent(); }
    public Map<String, Object> getStatistics() { Map<String, Object> stats = new HashMap<>(); stats.put("totalAssignments", assignmentRepository.count()); return stats; }
}