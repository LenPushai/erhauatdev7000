package com.erha.ops.repository;

import com.erha.ops.entity.TimeEntry;
import com.erha.ops.entity.TimeEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    List<TimeEntry> findByJobJobId(Long jobId);

    List<TimeEntry> findByWorkerId(Long workerId);

    List<TimeEntry> findByStatus(TimeEntryStatus status);

    List<TimeEntry> findByWorkDateBetween(LocalDate startDate, LocalDate endDate);

    List<TimeEntry> findByWorkerIdAndWorkDate(Long workerId, LocalDate workDate);

    List<TimeEntry> findByWorkerIdAndWorkDateBetween(Long workerId, LocalDate startDate, LocalDate endDate);

    List<TimeEntry> findByWorkerIdAndStatusAndWorkDateBetween(Long workerId, TimeEntryStatus status, LocalDate startDate, LocalDate endDate);

    List<TimeEntry> findByJobJobIdAndStatus(Long jobId, TimeEntryStatus status);

    @Query("SELECT SUM(t.normalHours + t.overtimeHours) FROM TimeEntry t WHERE t.job.jobId = :jobId")
    Double sumHoursByJobId(@Param("jobId") Long jobId);

    @Query("SELECT SUM(t.normalHours + t.overtimeHours) FROM TimeEntry t WHERE t.worker.id = :workerId")
    Double sumHoursByWorkerId(@Param("workerId") Long workerId);

    @Query("SELECT SUM(t.normalHours + t.overtimeHours) FROM TimeEntry t WHERE t.worker.id = :workerId AND MONTH(t.workDate) = :month AND YEAR(t.workDate) = :year")
    Double sumHoursByWorkerAndPeriod(@Param("workerId") Long workerId, @Param("month") int month, @Param("year") int year);

    @Query("SELECT t FROM TimeEntry t WHERE t.worker.id = :workerId AND MONTH(t.workDate) = :month AND YEAR(t.workDate) = :year")
    List<TimeEntry> findByWorkerAndPeriod(@Param("workerId") Long workerId, @Param("month") int month, @Param("year") int year);
}