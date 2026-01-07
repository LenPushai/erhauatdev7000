package com.erha.ops.rfq.repository;

import com.erha.ops.rfq.entity.RFQLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RFQLineItemRepository extends JpaRepository<RFQLineItem, Long> {
    
    List<RFQLineItem> findByRfqIdOrderByLineNumberAsc(Long rfqId);
    
    @Modifying
    @Transactional
    void deleteByRfqId(Long rfqId);
    
    long countByRfqId(Long rfqId);
}