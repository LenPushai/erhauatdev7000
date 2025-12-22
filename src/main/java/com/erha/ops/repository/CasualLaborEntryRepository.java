package com.erha.ops.repository;

import com.erha.ops.entity.CasualLaborEntry;
import com.erha.ops.entity.CasualLaborEntry.PaymentStatus;
import com.erha.ops.entity.CasualLaborEntry.WorkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CasualLaborEntryRepository extends JpaRepository<CasualLaborEntry, Long> {

    @Query("SELECT e FROM CasualLaborEntry e WHERE e.job.jobId = :jobId")
    List<CasualLaborEntry> findByJobId(@Param("jobId") Long jobId);

    List<CasualLaborEntry> findByCasualWorkerId(Long casualWorkerId);

    List<CasualLaborEntry> findByPaymentStatus(PaymentStatus status);

    List<CasualLaborEntry> findByWorkStatus(WorkStatus status);

    @Query("SELECT e FROM CasualLaborEntry e WHERE e.paymentStatus = 'PENDING' ORDER BY e.dateReceived")
    List<CasualLaborEntry> findPendingPayments();

    @Query("SELECT e FROM CasualLaborEntry e LEFT JOIN FETCH e.casualWorker LEFT JOIN FETCH e.job ORDER BY e.dateReceived DESC")
    List<CasualLaborEntry> findAllWithDetails();

    @Query("SELECT e FROM CasualLaborEntry e LEFT JOIN FETCH e.casualWorker LEFT JOIN FETCH e.job WHERE e.job.jobId = :jobId")
    List<CasualLaborEntry> findByJobIdWithDetails(@Param("jobId") Long jobId);

    @Query("SELECT COALESCE(SUM(e.paymentAmount), 0) FROM CasualLaborEntry e WHERE e.paymentStatus = 'PAID'")
    BigDecimal getTotalPaidAmount();

    @Query("SELECT COALESCE(SUM(e.paymentAmount), 0) FROM CasualLaborEntry e WHERE e.paymentStatus = 'PENDING'")
    BigDecimal getTotalPendingAmount();

    @Query("SELECT COALESCE(SUM(e.paymentAmount), 0) FROM CasualLaborEntry e WHERE e.job.jobId = :jobId")
    BigDecimal getTotalLaborCostForJob(@Param("jobId") Long jobId);

    List<CasualLaborEntry> findAllByOrderByDateReceivedDesc();
}