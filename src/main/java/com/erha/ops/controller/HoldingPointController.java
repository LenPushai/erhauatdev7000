package com.erha.ops.controller;

import com.erha.ops.entity.HoldingPoint;
import com.erha.ops.entity.JobHoldingPointSignoff;
import com.erha.ops.service.HoldingPointSignoffService;
import com.erha.ops.service.HoldingPointSignoffService.QcProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/qc")
@CrossOrigin(origins = "*")
public class HoldingPointController {

    private static final Logger logger = LoggerFactory.getLogger(HoldingPointController.class);

    @Autowired
    private HoldingPointSignoffService signoffService;

    @GetMapping("/holding-points")
    public ResponseEntity<List<Map<String, Object>>> getAllHoldingPoints() {
        return ResponseEntity.ok(signoffService.getAllHoldingPoints().stream().map(this::toHoldingPointDTO).toList());
    }

    @GetMapping("/holding-points/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveHoldingPoints() {
        return ResponseEntity.ok(signoffService.getActiveHoldingPoints().stream().map(this::toHoldingPointDTO).toList());
    }

    @Transactional(readOnly = true)
    @GetMapping("/jobs/{jobId}/signoffs")
    public ResponseEntity<List<Map<String, Object>>> getJobSignoffs(@PathVariable Long jobId) {
        return ResponseEntity.ok(signoffService.getJobSignoffs(jobId).stream().map(this::toSignoffDTO).toList());
    }

    @GetMapping("/jobs/{jobId}/progress")
    public ResponseEntity<QcProgress> getQcProgress(@PathVariable Long jobId) {
        return ResponseEntity.ok(signoffService.getQcProgress(jobId));
    }

    @PostMapping("/jobs/{jobId}/holding-points/{holdingPointId}/pass")
    public ResponseEntity<?> passHoldingPoint(@PathVariable Long jobId, @PathVariable Long holdingPointId, @RequestBody SignoffRequest request) {
        try {
            JobHoldingPointSignoff signoff = signoffService.passHoldingPoint(jobId, holdingPointId, request.signedById, request.notes);
            return ResponseEntity.ok(Map.of("message", "Passed", "progress", signoffService.getQcProgress(jobId)));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/jobs/{jobId}/holding-points/{holdingPointId}/fail")
    public ResponseEntity<?> failHoldingPoint(@PathVariable Long jobId, @PathVariable Long holdingPointId, @RequestBody SignoffRequest request) {
        try {
            JobHoldingPointSignoff signoff = signoffService.failHoldingPoint(jobId, holdingPointId, request.signedById, request.notes);
            return ResponseEntity.ok(Map.of("message", "Failed", "progress", signoffService.getQcProgress(jobId)));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/jobs/{jobId}/initialize")
    public ResponseEntity<?> initializeJobSignoffs(@PathVariable Long jobId) {
        try { signoffService.initializeJobSignoffs(jobId); return ResponseEntity.ok(Map.of("message", "QC signoffs initialized", "progress", signoffService.getQcProgress(jobId))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/jobs/{jobId}/reset-all")
    public ResponseEntity<?> resetAllSignoffs(@PathVariable Long jobId) {
        try { signoffService.resetAllSignoffs(jobId); return ResponseEntity.ok(Map.of("message", "All signoffs reset", "progress", signoffService.getQcProgress(jobId))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    private Map<String, Object> toHoldingPointDTO(HoldingPoint hp) {
        return Map.of("id", hp.getId(), "sequenceNumber", hp.getSequenceNumber(), "name", hp.getName(), "description", hp.getDescription() != null ? hp.getDescription() : "", "isActive", hp.getIsActive());
    }

    private Map<String, Object> toSignoffDTO(JobHoldingPointSignoff s) {
        return Map.of("id", s.getId(), "jobId", s.getJob().getJobId(), "holdingPointId", s.getHoldingPoint().getId(), "holdingPointName", s.getHoldingPoint().getName(), "sequenceNumber", s.getHoldingPoint().getSequenceNumber(), "status", s.getStatus().name(), "notes", s.getNotes() != null ? s.getNotes() : "");
    }

    public static class SignoffRequest { public Long signedById; public String notes; }
}