package com.erha.ops.repository;

import com.erha.ops.entity.JobTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobTaskRepository extends JpaRepository<JobTask, Long> {
    List<JobTask> findByJobIdOrderBySequenceNumber(Long jobId);
    List<JobTask> findByJobIdOrderBySequenceNumberAsc(Long jobId);
    Long countByJobId(Long jobId);
}