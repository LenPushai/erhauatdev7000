package com.erha.ops.controller;

import com.erha.ops.entity.JarisonBatchImport;
import com.erha.ops.entity.JarisonMonthlyHours;
import com.erha.ops.service.JarisonImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/v1/jarison")
@CrossOrigin(origins = "*")
public class JarisonController {

    private static final Logger logger = LoggerFactory.getLogger(JarisonController.class);

    @Autowired
    private JarisonImportService importService;

    @PostMapping("/import")
    public ResponseEntity<?> importBatchFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("month") int month,
            @RequestParam("year") int year,
            @RequestParam(value = "importedBy", defaultValue = "admin") String importedBy) {
        try {
            logger.info("Importing Jarison file: {} for {}/{}", file.getOriginalFilename(), month, year);
            JarisonBatchImport result = importService.importBatchFile(file, month, year, importedBy);
            return ResponseEntity.ok(Map.of(
                "message", "Import successful",
                "importId", result.getId(),
                "totalEmployees", result.getTotalEmployees(),
                "totalHours", result.getTotalHours(),
                "matched", result.getMatchedCount(),
                "unmatched", result.getUnmatchedCount()
            ));
        } catch (Exception e) {
            logger.error("Import failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/imports")
    public ResponseEntity<List<Map<String, Object>>> getAllImports() {
        return ResponseEntity.ok(importService.getAllImports().stream().map(this::toImportDTO).collect(Collectors.toList()));
    }

    @GetMapping("/imports/{id}")
    public ResponseEntity<?> getImport(@PathVariable Long id) {
        JarisonBatchImport batch = importService.getImportById(id);
        return batch != null ? ResponseEntity.ok(toImportDTO(batch)) : ResponseEntity.notFound().build();
    }

    @GetMapping("/imports/{id}/hours")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getImportHours(@PathVariable Long id) {
        return ResponseEntity.ok(importService.getHoursByImportId(id).stream().map(this::toHoursDTO).collect(Collectors.toList()));
    }

    @GetMapping("/imports/{id}/variances")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getVariances(@PathVariable Long id) {
        return ResponseEntity.ok(importService.getVariances(id).stream().map(this::toHoursDTO).collect(Collectors.toList()));
    }

    @GetMapping("/imports/{id}/unmatched")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getUnmatched(@PathVariable Long id) {
        return ResponseEntity.ok(importService.getUnmatched(id).stream().map(this::toHoursDTO).collect(Collectors.toList()));
    }

    @PostMapping("/hours/{recordId}/link")
    public ResponseEntity<?> linkWorker(@PathVariable Long recordId, @RequestBody Map<String, Long> body) {
        try {
            Long workerId = body.get("workerId");
            if (workerId == null) return ResponseEntity.badRequest().body(Map.of("error", "workerId is required"));
            return ResponseEntity.ok(toHoursDTO(importService.linkWorker(recordId, workerId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getReconciliationSummary() {
        List<JarisonBatchImport> imports = importService.getAllImports();
        if (imports.isEmpty()) return ResponseEntity.ok(Map.of("hasData", false, "message", "No Jarison imports yet"));
        
        JarisonBatchImport latest = imports.get(0);
        List<JarisonMonthlyHours> variances = importService.getVariances(latest.getId());
        List<JarisonMonthlyHours> unmatched = importService.getUnmatched(latest.getId());
        
        return ResponseEntity.ok(Map.of(
            "hasData", true,
            "latestImport", toImportDTO(latest),
            "varianceCount", variances.size(),
            "unmatchedCount", unmatched.size(),
            "needsAttention", variances.size() + unmatched.size()
        ));
    }

    private Map<String, Object> toImportDTO(JarisonBatchImport b) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", b.getId());
        dto.put("fileName", b.getFileName());
        dto.put("periodMonth", b.getPeriodMonth());
        dto.put("periodYear", b.getPeriodYear());
        dto.put("periodDisplay", getMonthName(b.getPeriodMonth()) + " " + b.getPeriodYear());
        dto.put("totalEmployees", b.getTotalEmployees());
        dto.put("totalHours", b.getTotalHours());
        dto.put("matchedCount", b.getMatchedCount());
        dto.put("unmatchedCount", b.getUnmatchedCount());
        dto.put("importedBy", b.getImportedBy());
        dto.put("importedAt", b.getImportedAt());
        dto.put("status", b.getStatus().name());
        return dto;
    }

    private Map<String, Object> toHoursDTO(JarisonMonthlyHours h) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", h.getId());
        dto.put("jarisonCode", h.getJarisonCode());
        dto.put("employeeName", h.getEmployeeName());
        dto.put("workerId", h.getWorker() != null ? h.getWorker().getId() : null);
        dto.put("workerName", h.getWorker() != null ? h.getWorker().getFirstName() + " " + h.getWorker().getLastName() : null);
        dto.put("totalHours", h.getTotalHours());
        dto.put("normalHours", h.getNormalHours());
        dto.put("otHours15", h.getOtHours15());
        dto.put("otHours20", h.getOtHours20());
        dto.put("erhaJobHours", h.getErhaJobHours());
        dto.put("varianceHours", h.getVarianceHours());
        dto.put("reconciliationStatus", h.getReconciliationStatus().name());
        return dto;
    }

    private String getMonthName(int m) {
        String[] months = {"", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        return m >= 1 && m <= 12 ? months[m] : "Unknown";
    }
}