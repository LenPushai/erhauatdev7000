package com.erha.ops.repository;

import com.erha.ops.entity.TeamLead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamLeadRepository extends JpaRepository<TeamLead, Long> {
    
    Optional<TeamLead> findByPinCodeAndIsActiveTrue(String pinCode);
    
    Optional<TeamLead> findByWorkerId(Long workerId);
    
    Optional<TeamLead> findByWorkerIdAndIsActiveTrue(Long workerId);
    
    List<TeamLead> findByIsActiveTrue();
    
    List<TeamLead> findByDepartmentAndIsActiveTrue(String department);
    
    long countByIsActiveTrue();
}
