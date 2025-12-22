package com.erha.ops.repository;

import com.erha.ops.entity.AssignmentStatus;
import com.erha.ops.entity.JobAssignment;
import com.erha.ops.entity.RoleOnJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobAssignmentRepository extends JpaRepository<JobAssignment, Long> {
    
    List<JobAssignment> findByJobJobId(Long jobId);
    
    List<JobAssignment> findByWorkerId(Long workerId);
    
    List<JobAssignment> findByJobJobIdAndStatusIn(Long jobId, List<AssignmentStatus> statuses);
    
    List<JobAssignment> findByWorkerIdAndStatusIn(Long workerId, List<AssignmentStatus> statuses);
    
    Optional<JobAssignment> findByJobJobIdAndWorkerIdAndStatusIn(Long jobId, Long workerId, List<AssignmentStatus> statuses);
    
    Optional<JobAssignment> findByJobJobIdAndRoleAndStatusIn(Long jobId, RoleOnJob role, List<AssignmentStatus> statuses);
    
    long countByJobJobIdAndStatusIn(Long jobId, List<AssignmentStatus> statuses);
}
