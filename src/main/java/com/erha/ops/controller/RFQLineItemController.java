package com.erha.ops.controller;

import com.erha.ops.rfq.entity.RFQLineItem;
import com.erha.ops.rfq.repository.RFQLineItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176", "http://localhost:5177", "http://localhost:5178"})
public class RFQLineItemController {

    private static final Logger logger = LoggerFactory.getLogger(RFQLineItemController.class);

    @Autowired
    private RFQLineItemRepository lineItemRepository;

    @GetMapping("/rfqs/{rfqId}/line-items")
    public ResponseEntity<List<RFQLineItem>> getLineItems(@PathVariable Long rfqId) {
        logger.info("Fetching line items for RFQ ID: {}", rfqId);
        List<RFQLineItem> lineItems = lineItemRepository.findByRfqIdOrderByLineNumberAsc(rfqId);
        return ResponseEntity.ok(lineItems);
    }

    @PostMapping("/rfqs/{rfqId}/line-items")
    public ResponseEntity<RFQLineItem> createLineItem(@PathVariable Long rfqId, @RequestBody RFQLineItem lineItem) {
        logger.info("Creating line item for RFQ ID: {}", rfqId);
        
        lineItem.setRfqId(rfqId);
        lineItem.setCreatedAt(LocalDateTime.now());
        lineItem.setUpdatedAt(LocalDateTime.now());
        
        RFQLineItem saved = lineItemRepository.save(lineItem);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/rfqs/{rfqId}/line-items/batch")
    public ResponseEntity<List<RFQLineItem>> createLineItemsBatch(@PathVariable Long rfqId, @RequestBody List<RFQLineItem> lineItems) {
        logger.info("Creating {} line items for RFQ ID: {}", lineItems.size(), rfqId);
        
        for (int i = 0; i < lineItems.size(); i++) {
            RFQLineItem item = lineItems.get(i);
            item.setRfqId(rfqId);
            item.setLineNumber(i + 1);
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
        }
        
        List<RFQLineItem> saved = lineItemRepository.saveAll(lineItems);
        logger.info("Saved {} line items for RFQ ID: {}", saved.size(), rfqId);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/rfqs/{rfqId}/line-items/{lineItemId}")
    public ResponseEntity<RFQLineItem> updateLineItem(
            @PathVariable Long rfqId, 
            @PathVariable Long lineItemId, 
            @RequestBody RFQLineItem lineItemDetails) {
        
        logger.info("Updating line item {} for RFQ ID: {}", lineItemId, rfqId);
        
        return lineItemRepository.findById(lineItemId)
            .map(lineItem -> {
                if (lineItemDetails.getLineNumber() != null) lineItem.setLineNumber(lineItemDetails.getLineNumber());
                if (lineItemDetails.getDescription() != null) lineItem.setDescription(lineItemDetails.getDescription());
                if (lineItemDetails.getQuantity() != null) lineItem.setQuantity(lineItemDetails.getQuantity());
                if (lineItemDetails.getUnitOfMeasure() != null) lineItem.setUnitOfMeasure(lineItemDetails.getUnitOfMeasure());
                if (lineItemDetails.getEstimatedUnitPrice() != null) lineItem.setEstimatedUnitPrice(lineItemDetails.getEstimatedUnitPrice());
                if (lineItemDetails.getEstimatedLineTotal() != null) lineItem.setEstimatedLineTotal(lineItemDetails.getEstimatedLineTotal());
                if (lineItemDetails.getDrawingReference() != null) lineItem.setDrawingReference(lineItemDetails.getDrawingReference());
                if (lineItemDetails.getNotes() != null) lineItem.setNotes(lineItemDetails.getNotes());
                
                lineItem.setUpdatedAt(LocalDateTime.now());
                
                RFQLineItem updated = lineItemRepository.save(lineItem);
                return ResponseEntity.ok(updated);
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/rfqs/{rfqId}/line-items/{lineItemId}")
    public ResponseEntity<Map<String, Object>> deleteLineItem(@PathVariable Long rfqId, @PathVariable Long lineItemId) {
        logger.info("Deleting line item {} for RFQ ID: {}", lineItemId, rfqId);
        
        if (lineItemRepository.existsById(lineItemId)) {
            lineItemRepository.deleteById(lineItemId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Line item deleted"));
        }
        
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/rfqs/{rfqId}/line-items")
    public ResponseEntity<Map<String, Object>> deleteAllLineItems(@PathVariable Long rfqId) {
        logger.info("Deleting all line items for RFQ ID: {}", rfqId);
        
        long count = lineItemRepository.countByRfqId(rfqId);
        lineItemRepository.deleteByRfqId(rfqId);
        
        return ResponseEntity.ok(Map.of(
            "success", true, 
            "message", "All line items deleted", 
            "count", count
        ));
    }
}