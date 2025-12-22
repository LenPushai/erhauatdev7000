package com.erha.ops.repository;

import com.erha.ops.entity.CasualWorker;
import com.erha.ops.entity.CasualWorker.CasualWorkerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CasualWorkerRepository extends JpaRepository<CasualWorker, Long> {

    Optional<CasualWorker> findByClockNumber(String clockNumber);

    List<CasualWorker> findByStatus(CasualWorkerStatus status);

    List<CasualWorker> findByNameContainingIgnoreCase(String name);

    List<CasualWorker> findAllByOrderByNameAsc();

    @Query("SELECT c FROM CasualWorker c WHERE c.status = 'ACTIVE' ORDER BY c.name")
    List<CasualWorker> findAllActive();

    @Query("SELECT COUNT(c) FROM CasualWorker c WHERE c.status = 'ACTIVE'")
    long countActive();

    boolean existsByClockNumber(String clockNumber);
}