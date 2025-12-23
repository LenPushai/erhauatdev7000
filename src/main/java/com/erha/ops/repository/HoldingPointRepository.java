package com.erha.ops.repository;

import com.erha.ops.entity.HoldingPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoldingPointRepository extends JpaRepository<HoldingPoint, Long> {
    
    Optional<HoldingPoint> findBySequenceNumber(Integer sequenceNumber);
    
    List<HoldingPoint> findByIsActiveTrueOrderBySequenceNumberAsc();
    
    long countByIsActiveTrue();
}
