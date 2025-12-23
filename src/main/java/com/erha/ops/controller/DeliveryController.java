package com.erha.ops.controller;

import com.erha.ops.entity.Job;
import com.erha.ops.entity.WorkshopStatus;
import com.erha.ops.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/delivery")
@CrossOrigin(origins = "*")
public class DeliveryController {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryController.class);

    @Autowired
    private JobRepository jobRepository;

    @PostMapping("/confirm/{jobId}")
    public ResponseEntity<?> confirmDelivery(
            @PathVariable Long jobId,
            @RequestBody Map<String, Object> deliveryData) {
        
        try {
            Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

            if (job.getWorkshopStatus() != WorkshopStatus.READY_FOR_DELIVERY) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Job is not ready for delivery. Current status: " + job.getWorkshopStatus()
                ));
            }

            // Update job with delivery info
            job.setWorkshopStatus(WorkshopStatus.DELIVERED);
            job.setDeliveredAt(LocalDateTime.now());
            job.setDeliveredBy((String) deliveryData.get("deliveredBy"));
            job.setDeliveryVehicle((String) deliveryData.get("vehicle"));
            job.setDeliveryNoteNumber((String) deliveryData.get("deliveryNoteNumber"));
            job.setReceivedBy((String) deliveryData.get("receivedBy"));
            job.setDeliverySignature((String) deliveryData.get("clientSignature"));
            job.setDeliveryNotes((String) deliveryData.get("notes"));

            jobRepository.save(job);

            logger.info("Job {} delivered successfully", job.getJobNumber());

            return ResponseEntity.ok(Map.of(
                "message", "Delivery confirmed",
                "jobId", job.getJobId(),
                "jobNumber", job.getJobNumber(),
                "deliveryNoteNumber", job.getDeliveryNoteNumber(),
                "deliveredAt", job.getDeliveredAt()
            ));

        } catch (Exception e) {
            logger.error("Delivery confirmation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/note/{jobId}")
    public ResponseEntity<?> getDeliveryNote(@PathVariable Long jobId) {
        try {
            Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

            return ResponseEntity.ok(Map.of(
                "jobId", job.getJobId(),
                "jobNumber", job.getJobNumber(),
                "clientName", "Client ID: " + job.getClientId(),
                "description", job.getDescription() != null ? job.getDescription() : "",
                "deliveryNoteNumber", job.getDeliveryNoteNumber() != null ? job.getDeliveryNoteNumber() : "",
                "deliveredBy", job.getDeliveredBy() != null ? job.getDeliveredBy() : "",
                "vehicle", job.getDeliveryVehicle() != null ? job.getDeliveryVehicle() : "",
                "receivedBy", job.getReceivedBy() != null ? job.getReceivedBy() : "",
                "deliveredAt", job.getDeliveredAt(),
                "signature", job.getDeliverySignature() != null ? job.getDeliverySignature() : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}