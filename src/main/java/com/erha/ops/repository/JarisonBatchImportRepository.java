package com.erha.ops.repository;

import com.erha.ops.entity.JarisonBatchImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JarisonBatchImportRepository extends JpaRepository<JarisonBatchImport, Long> {
    
    Optional<JarisonBatchImport> findByPeriodMonthAndPeriodYear(Integer month, Integer year);
    
    List<JarisonBatchImport> findAllByOrderByPeriodYearDescPeriodMonthDesc();
}