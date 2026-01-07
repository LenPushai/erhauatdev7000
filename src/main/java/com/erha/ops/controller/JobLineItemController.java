package com.erha.ops.controller;

import com.erha.ops.entity.JobLineItem;
import com.erha.ops.repository.JobLineItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.transaction.Transactional;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class JobLineItemController {

    private static final Logger logger = LoggerFactory.getLogger(JobLineItemController.class);

    @Autowired
    private JobLineItemRepository jobLineItemRepository;

    @GetMapping("/jobs/{jobId}/line-items")
    public ResponseEntity<List<JobLineItem>> getLineItemsByJobId(@PathVariable Long jobId) {
        List<JobLineItem> items = jobLineItemRepository.findByJobIdOrderByLineNumberAsc(jobId);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/jobs/{jobId}/line-items")
    public ResponseEntity<JobLineItem> createLineItem(@PathVariable Long jobId, @RequestBody JobLineItem lineItem) {
        lineItem.setJobId(jobId);
        
        // Auto-assign line number if not provided
        if (lineItem.getLineNumber() == null) {
            int count = jobLineItemRepository.countByJobId(jobId);
            lineItem.setLineNumber(count + 1);
        }
        
        if (lineItem.getSource() == null) {
            lineItem.setSource("MANUAL");
        }
        
        JobLineItem saved = jobLineItemRepository.save(lineItem);
        logger.info("Created line item {} for job {}", saved.getId(), jobId);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/jobs/{jobId}/line-items/batch")
    public ResponseEntity<List<JobLineItem>> createLineItemsBatch(@PathVariable Long jobId, @RequestBody List<JobLineItem> lineItems) {
        for (JobLineItem item : lineItems) {
            item.setJobId(jobId);
        }
        List<JobLineItem> saved = jobLineItemRepository.saveAll(lineItems);
        logger.info("Created {} line items for job {}", saved.size(), jobId);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/jobs/{jobId}/line-items/{itemId}")
    public ResponseEntity<JobLineItem> updateLineItem(
            @PathVariable Long jobId,
            @PathVariable Long itemId,
            @RequestBody JobLineItem lineItem) {
        
        return jobLineItemRepository.findById(itemId)
            .map(existing -> {
                existing.setDescription(lineItem.getDescription());
                existing.setQuantity(lineItem.getQuantity());
                existing.setUnitOfMeasure(lineItem.getUnitOfMeasure());
                existing.setEstimatedHours(lineItem.getEstimatedHours());
                existing.setActualHours(lineItem.getActualHours());
                existing.setDrawingReference(lineItem.getDrawingReference());
                existing.setNotes(lineItem.getNotes());
                existing.setStatus(lineItem.getStatus());
                JobLineItem saved = jobLineItemRepository.save(existing);
                logger.info("Updated line item {} for job {}", itemId, jobId);
                return ResponseEntity.ok(saved);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/jobs/{jobId}/line-items/{itemId}/status")
    public ResponseEntity<JobLineItem> updateLineItemStatus(
            @PathVariable Long jobId,
            @PathVariable Long itemId,
            @RequestBody String status) {
        
        return jobLineItemRepository.findById(itemId)
            .map(existing -> {
                existing.setStatus(status.replace("\"", ""));
                JobLineItem saved = jobLineItemRepository.save(existing);
                logger.info("Updated line item {} status to {}", itemId, status);
                return ResponseEntity.ok(saved);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/jobs/{jobId}/line-items/{itemId}")
    public ResponseEntity<Void> deleteLineItem(@PathVariable Long jobId, @PathVariable Long itemId) {
        jobLineItemRepository.deleteById(itemId);
        logger.info("Deleted line item {} from job {}", itemId, jobId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/jobs/{jobId}/line-items")
    @Transactional
    public ResponseEntity<Void> deleteAllLineItems(@PathVariable Long jobId) {
        jobLineItemRepository.deleteByJobId(jobId);
        logger.info("Deleted all line items for job {}", jobId);
        return ResponseEntity.noContent().build();
    }
}