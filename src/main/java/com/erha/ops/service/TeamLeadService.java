package com.erha.ops.service;

import com.erha.ops.entity.TeamLead;
import com.erha.ops.entity.Worker;
import com.erha.ops.repository.TeamLeadRepository;
import com.erha.ops.repository.WorkerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class TeamLeadService {

    private static final Logger logger = LoggerFactory.getLogger(TeamLeadService.class);

    @Autowired
    private TeamLeadRepository teamLeadRepository;

    @Autowired
    private WorkerRepository workerRepository;

    public Optional<TeamLead> authenticateByPin(String pin) { return teamLeadRepository.findByPinCodeAndIsActiveTrue(pin); }

    public TeamLead createTeamLead(Long workerId, String pin, String department) {
        if (pin == null || pin.length() != 4 || !pin.matches("\\d{4}")) throw new RuntimeException("PIN must be exactly 4 digits");
        Worker worker = workerRepository.findById(workerId).orElseThrow(() -> new RuntimeException("Worker not found"));
        if (teamLeadRepository.findByWorkerId(workerId).isPresent()) throw new RuntimeException("Worker is already a team lead");
        if (teamLeadRepository.findByPinCodeAndIsActiveTrue(pin).isPresent()) throw new RuntimeException("PIN is already in use");
        TeamLead teamLead = new TeamLead();
        teamLead.setWorker(worker);
        teamLead.setPinCode(pin);
        teamLead.setDepartment(department);
        teamLead.setIsActive(true);
        teamLead.setCreatedAt(LocalDateTime.now());
        return teamLeadRepository.save(teamLead);
    }

    public TeamLead updatePin(Long teamLeadId, String newPin) {
        if (newPin == null || newPin.length() != 4 || !newPin.matches("\\d{4}")) throw new RuntimeException("PIN must be exactly 4 digits");
        TeamLead teamLead = teamLeadRepository.findById(teamLeadId).orElseThrow(() -> new RuntimeException("Team lead not found"));
        Optional<TeamLead> existing = teamLeadRepository.findByPinCodeAndIsActiveTrue(newPin);
        if (existing.isPresent() && !existing.get().getId().equals(teamLeadId)) throw new RuntimeException("PIN is already in use");
        teamLead.setPinCode(newPin);
        return teamLeadRepository.save(teamLead);
    }

    public TeamLead updateDepartment(Long teamLeadId, String department) {
        TeamLead teamLead = teamLeadRepository.findById(teamLeadId).orElseThrow(() -> new RuntimeException("Team lead not found"));
        teamLead.setDepartment(department);
        return teamLeadRepository.save(teamLead);
    }

    public TeamLead deactivate(Long teamLeadId) {
        TeamLead teamLead = teamLeadRepository.findById(teamLeadId).orElseThrow(() -> new RuntimeException("Team lead not found"));
        teamLead.setIsActive(false);
        return teamLeadRepository.save(teamLead);
    }

    public TeamLead reactivate(Long teamLeadId) {
        TeamLead teamLead = teamLeadRepository.findById(teamLeadId).orElseThrow(() -> new RuntimeException("Team lead not found"));
        teamLead.setIsActive(true);
        return teamLeadRepository.save(teamLead);
    }

    public boolean isTeamLead(Long workerId) { return teamLeadRepository.findByWorkerIdAndIsActiveTrue(workerId).isPresent(); }
    public Optional<TeamLead> getById(Long id) { return teamLeadRepository.findById(id); }
    public Optional<TeamLead> getByWorkerId(Long workerId) { return teamLeadRepository.findByWorkerId(workerId); }
    public List<TeamLead> getAllActive() { return teamLeadRepository.findByIsActiveTrue(); }
    public List<TeamLead> getByDepartment(String department) { return teamLeadRepository.findByDepartmentAndIsActiveTrue(department); }
    public long countActive() { return teamLeadRepository.countByIsActiveTrue(); }
}
