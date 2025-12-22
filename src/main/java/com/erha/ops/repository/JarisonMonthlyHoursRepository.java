package com.erha.ops.repository;

import com.erha.ops.entity.JarisonMonthlyHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JarisonMonthlyHoursRepository extends JpaRepository<JarisonMonthlyHours, Long> {
    
    List<JarisonMonthlyHours> findByBatchImportId(Long importId);
    
    List<JarisonMonthlyHours> findByBatchImportIdOrderByEmployeeNameAsc(Long importId);
    
    @Query("SELECT j FROM JarisonMonthlyHours j WHERE j.batchImport.id = :importId AND j.reconciliationStatus = 'VARIANCE'")
    List<JarisonMonthlyHours> findVariancesByImportId(@Param("importId") Long importId);
    
    @Query("SELECT j FROM JarisonMonthlyHours j WHERE j.batchImport.id = :importId AND j.worker IS NULL")
    List<JarisonMonthlyHours> findUnmatchedByImportId(@Param("importId") Long importId);
    
    @Query("SELECT j FROM JarisonMonthlyHours j WHERE j.worker.id = :workerId ORDER BY j.batchImport.periodYear DESC, j.batchImport.periodMonth DESC")
    List<JarisonMonthlyHours> findByWorkerId(@Param("workerId") Long workerId);
}