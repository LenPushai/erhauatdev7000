package com.erha.ops.repository;

import com.erha.ops.entity.DeliveryNote;
import com.erha.ops.entity.DeliveryNoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryNoteRepository extends JpaRepository<DeliveryNote, Long> {
    
    Optional<DeliveryNote> findByJobJobId(Long jobId);
    
    Optional<DeliveryNote> findByDeliveryNoteNumber(String deliveryNoteNumber);
    
    List<DeliveryNote> findByStatus(DeliveryNoteStatus status);
    
    List<DeliveryNote> findByStatusIn(List<DeliveryNoteStatus> statuses);
    
    List<DeliveryNote> findAllByOrderByCreatedAtDesc();
    
    @Query("SELECT MAX(d.deliveryNoteNumber) FROM DeliveryNote d WHERE d.deliveryNoteNumber LIKE :prefix%")
    Optional<String> findMaxDeliveryNoteNumberByPrefix(@Param("prefix") String prefix);
}
