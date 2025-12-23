package com.erha.ops.repository;

import com.erha.ops.entity.JobHoldingPointSignoff;
import com.erha.ops.entity.SignoffStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobHoldingPointSignoffRepository extends JpaRepository<JobHoldingPointSignoff, Long> {
    
    List<JobHoldingPointSignoff> findByJobJobId(Long jobId);
    
    List<JobHoldingPointSignoff> findByJobJobIdOrderByHoldingPointSequenceNumberAsc(Long jobId);
    
    Optional<JobHoldingPointSignoff> findByJobJobIdAndHoldingPointId(Long jobId, Long holdingPointId);
    
    List<JobHoldingPointSignoff> findByJobJobIdAndStatus(Long jobId, SignoffStatus status);
    
    long countByJobJobIdAndStatus(Long jobId, SignoffStatus status);
}
