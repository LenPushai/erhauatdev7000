package com.erha.ops.rfq.repository;

import com.erha.ops.rfq.entity.RFQLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RFQLineItemRepository extends JpaRepository<RFQLineItem, Long> {
    
    List<RFQLineItem> findByRfqIdOrderByLineNumberAsc(Long rfqId);
    
    void deleteByRfqId(Long rfqId);
    
    long countByRfqId(Long rfqId);
}