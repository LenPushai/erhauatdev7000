package com.erha.ops.controller;

import com.erha.ops.entity.TeamLead;
import com.erha.ops.service.TeamLeadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/team-leads")
@CrossOrigin(origins = "*")
public class TeamLeadController {

    private static final Logger logger = LoggerFactory.getLogger(TeamLeadController.class);

    @Autowired
    private TeamLeadService teamLeadService;

    @PostMapping("/auth/pin")
    public ResponseEntity<?> authenticateByPin(@RequestBody Map<String, String> request) {
        String pin = request.get("pin");
        if (pin == null || pin.length() != 4) return ResponseEntity.badRequest().body(Map.of("authenticated", false, "error", "Invalid PIN format"));
        return teamLeadService.authenticateByPin(pin).map(tl -> ResponseEntity.ok(Map.of("authenticated", true, "teamLead", toDTO(tl))))
            .orElse(ResponseEntity.ok(Map.of("authenticated", false, "error", "Invalid PIN")));
    }

    @GetMapping("/check/{workerId}")
    public ResponseEntity<?> checkIfTeamLead(@PathVariable Long workerId) { return ResponseEntity.ok(Map.of("workerId", workerId, "isTeamLead", teamLeadService.isTeamLead(workerId))); }

    @PostMapping
    public ResponseEntity<?> createTeamLead(@RequestBody CreateTeamLeadRequest request) {
        try { return ResponseEntity.ok(Map.of("teamLead", toDTO(teamLeadService.createTeamLead(request.workerId, request.pin, request.department)), "message", "Team lead created")); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/{id}/pin")
    public ResponseEntity<?> updatePin(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try { return ResponseEntity.ok(toDTO(teamLeadService.updatePin(id, request.get("pin")))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        try { return ResponseEntity.ok(toDTO(teamLeadService.deactivate(id))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable Long id) {
        try { return ResponseEntity.ok(toDTO(teamLeadService.reactivate(id))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) { return teamLeadService.getById(id).map(tl -> ResponseEntity.ok(toDTO(tl))).orElse(ResponseEntity.notFound().build()); }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllActive() { return ResponseEntity.ok(teamLeadService.getAllActive().stream().map(this::toDTO).toList()); }

    @GetMapping("/department/{department}")
    public ResponseEntity<List<Map<String, Object>>> getByDepartment(@PathVariable String department) { return ResponseEntity.ok(teamLeadService.getByDepartment(department).stream().map(this::toDTO).toList()); }

    @GetMapping("/count")
    public ResponseEntity<?> countActive() { return ResponseEntity.ok(Map.of("count", teamLeadService.countActive())); }

    private Map<String, Object> toDTO(TeamLead tl) { return Map.of("id", tl.getId(), "workerId", tl.getWorker().getId(), "workerName", tl.getWorker().getFirstName() + " " + tl.getWorker().getLastName(), "department", tl.getDepartment() != null ? tl.getDepartment() : "ALL", "isActive", tl.getIsActive()); }

    public static class CreateTeamLeadRequest { public Long workerId; public String pin; public String department; }
}
