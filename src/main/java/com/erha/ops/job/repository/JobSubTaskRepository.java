package com.erha.ops.job.repository;

import com.erha.ops.job.entity.JobSubTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobSubTaskRepository extends JpaRepository<JobSubTask, Long> {
    
    List<JobSubTask> findByJobIdOrderByTaskNumberAsc(Long jobId);
    
    void deleteByJobId(Long jobId);
    
    long countByJobId(Long jobId);
    
    List<JobSubTask> findByStatus(String status);
}