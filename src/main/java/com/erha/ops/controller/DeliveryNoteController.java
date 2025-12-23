package com.erha.ops.controller;

import com.erha.ops.entity.DeliveryNote;
import com.erha.ops.entity.DeliveryNoteStatus;
import com.erha.ops.service.DeliveryNoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/delivery-notes")
@CrossOrigin(origins = "*")
public class DeliveryNoteController {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryNoteController.class);

    @Autowired
    private DeliveryNoteService deliveryNoteService;

    @PostMapping("/jobs/{jobId}")
    public ResponseEntity<?> generateDeliveryNote(@PathVariable Long jobId) {
        try { return ResponseEntity.ok(toDTO(deliveryNoteService.generateDeliveryNote(jobId))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{id}/dispatch")
    public ResponseEntity<?> dispatch(@PathVariable Long id, @RequestBody DispatchRequest request) {
        try { return ResponseEntity.ok(toDTO(deliveryNoteService.dispatch(id, request.deliveredBy, request.vehicleInfo, request.notes))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{id}/delivered")
    public ResponseEntity<?> markDelivered(@PathVariable Long id, @RequestBody DeliveryRequest request) {
        try { return ResponseEntity.ok(toDTO(deliveryNoteService.markDelivered(id, request.receivedBy, request.notes))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{id}/sign")
    public ResponseEntity<?> recordSignature(@PathVariable Long id, @RequestBody SignatureRequest request) {
        try { return ResponseEntity.ok(toDTO(deliveryNoteService.recordCustomerSignature(id, request.customerName, request.signatureData))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) { return deliveryNoteService.getById(id).map(n -> ResponseEntity.ok(toDTO(n))).orElse(ResponseEntity.notFound().build()); }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<?> getByJobId(@PathVariable Long jobId) { return deliveryNoteService.getByJobId(jobId).map(n -> ResponseEntity.ok(toDTO(n))).orElse(ResponseEntity.notFound().build()); }

    @GetMapping("/ready")
    public ResponseEntity<List<Map<String, Object>>> getReadyForDispatch() { return ResponseEntity.ok(deliveryNoteService.getReadyForDispatch().stream().map(this::toDTO).toList()); }

    @GetMapping("/in-transit")
    public ResponseEntity<List<Map<String, Object>>> getInTransit() { return ResponseEntity.ok(deliveryNoteService.getInTransit().stream().map(this::toDTO).toList()); }

    @GetMapping("/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecent(@RequestParam(defaultValue = "10") int limit) { return ResponseEntity.ok(deliveryNoteService.getRecent(limit).stream().map(this::toDTO).toList()); }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() { return ResponseEntity.ok(deliveryNoteService.getStatistics()); }

    @GetMapping("/statuses")
    public ResponseEntity<List<Map<String, String>>> getStatuses() {
        List<Map<String, String>> statuses = new java.util.ArrayList<>();
        for (DeliveryNoteStatus status : DeliveryNoteStatus.values()) { statuses.add(Map.of("value", status.name(), "label", status.name().replace("_", " "))); }
        return ResponseEntity.ok(statuses);
    }

    private Map<String, Object> toDTO(DeliveryNote n) { return Map.of("id", n.getId(), "deliveryNoteNumber", n.getDeliveryNoteNumber(), "jobId", n.getJob().getJobId(), "jobNumber", n.getJob().getJobNumber(), "status", n.getStatus().name()); }

    public static class DispatchRequest { public String deliveredBy; public String vehicleInfo; public String notes; }
    public static class DeliveryRequest { public String receivedBy; public String notes; }
    public static class SignatureRequest { public String customerName; public String signatureData; }
}
