package com.erha.ops.repository;

import com.erha.ops.entity.JobLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobLineItemRepository extends JpaRepository<JobLineItem, Long> {
    List<JobLineItem> findByJobIdOrderByLineNumberAsc(Long jobId);
    void deleteByJobId(Long jobId);
    int countByJobId(Long jobId);
}