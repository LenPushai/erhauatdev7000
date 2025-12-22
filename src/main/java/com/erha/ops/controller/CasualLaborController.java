package com.erha.ops.controller;

import com.erha.ops.entity.CasualLaborEntry;
import com.erha.ops.entity.CasualWorker;
import com.erha.ops.service.CasualLaborService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/casuals")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CasualLaborController {

    private static final Logger logger = LoggerFactory.getLogger(CasualLaborController.class);

    @Autowired
    private CasualLaborService laborService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(laborService.getSummaryStats());
    }

    @GetMapping("/workers")
    public ResponseEntity<List<Map<String, Object>>> getAllWorkers() {
        return ResponseEntity.ok(laborService.getAllWorkers().stream().map(this::toWorkerDTO).toList());
    }

    @GetMapping("/entries")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getAllEntries() {
        return ResponseEntity.ok(laborService.getAllEntries().stream().map(this::toEntryDTO).toList());
    }

    @PostMapping("/entries/{id}/pay")
    public ResponseEntity<?> markAsPaid(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> payload) {
        try {
            LocalDate payDate = payload != null && payload.get("payDate") != null ? LocalDate.parse(payload.get("payDate").toString()) : null;
            return ResponseEntity.ok(toEntryDTO(laborService.markAsPaid(id, payDate)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importFromExcel(@RequestParam("file") MultipartFile file) {
        logger.info("=== IMPORT REQUEST RECEIVED ===");
        logger.info("File: {}, Size: {} bytes", file.getOriginalFilename(), file.getSize());
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }
        
        try {
            Map<String, Object> result = laborService.importFromExcel(file);
            logger.info("Import result: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Import failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toWorkerDTO(CasualWorker w) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", w.getId());
        dto.put("clockNumber", w.getClockNumber());
        dto.put("name", w.getName());
        dto.put("contactDetails", w.getContactDetails());
        dto.put("bankName", w.getBankName());
        dto.put("bankAccount", w.getBankAccount());
        dto.put("branchCode", w.getBranchCode());
        dto.put("status", w.getStatus() != null ? w.getStatus().name() : null);
        dto.put("notes", w.getNotes());
        return dto;
    }

    private Map<String, Object> toEntryDTO(CasualLaborEntry e) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", e.getId());
        dto.put("requestNumber", e.getRequestNumber());
        dto.put("jobId", e.getJob() != null ? e.getJob().getJobId() : null);
        dto.put("jobNo", e.getJob() != null ? e.getJob().getJobNumber() : null);
        dto.put("workerId", e.getCasualWorker() != null ? e.getCasualWorker().getId() : null);
        dto.put("workerName", e.getCasualWorker() != null ? e.getCasualWorker().getName() : null);
        dto.put("workerClockNo", e.getCasualWorker() != null ? e.getCasualWorker().getClockNumber() : null);
        dto.put("originator", e.getOriginator());
        dto.put("place", e.getPlace());
        dto.put("workItem", e.getWorkItem());
        dto.put("dateReceived", e.getDateReceived());
        dto.put("payMethod", e.getPayMethod() != null ? e.getPayMethod().name() : null);
        dto.put("payDate", e.getPayDate());
        dto.put("paymentAmount", e.getPaymentAmount());
        dto.put("paymentStatus", e.getPaymentStatus() != null ? e.getPaymentStatus().name() : null);
        dto.put("workStatus", e.getWorkStatus() != null ? e.getWorkStatus().name() : null);
        return dto;
    }
}