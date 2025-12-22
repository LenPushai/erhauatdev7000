package com.erha.ops.controller;

import com.erha.ops.service.PastelExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/export/pastel")
@CrossOrigin(origins = "*")
public class PastelExportController {

    private static final String CSV_MEDIA_TYPE = "text/csv";
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private PastelExportService pastelExportService;

    @GetMapping("/quotes/{id}")
    public ResponseEntity<String> exportQuote(@PathVariable Long id) {
        try {
            String csv = pastelExportService.exportQuoteToPastelCsv(id);
            String filename = "Quote_" + id + "_" + getCurrentTimestamp() + ".csv";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(CSV_MEDIA_TYPE))
                    .body(csv);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/quotes/bulk")
    public ResponseEntity<String> exportQuotesBulk(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String csv = pastelExportService.exportQuotesToPastelCsv(ids);
        String filename = "Quotes_Bulk_" + getCurrentTimestamp() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(CSV_MEDIA_TYPE))
                .body(csv);
    }

    @GetMapping("/quotes/approved")
    public ResponseEntity<String> exportApprovedQuotes() {
        String csv = pastelExportService.exportApprovedQuotesToPastelCsv();
        String filename = "Quotes_Approved_" + getCurrentTimestamp() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(CSV_MEDIA_TYPE))
                .body(csv);
    }

    @GetMapping("/clients/{id}")
    public ResponseEntity<String> exportClient(@PathVariable Long id) {
        try {
            String csv = pastelExportService.exportClientToPastelCsv(id);
            String filename = "Client_" + id + "_" + getCurrentTimestamp() + ".csv";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(CSV_MEDIA_TYPE))
                    .body(csv);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/clients")
    public ResponseEntity<String> exportAllClients() {
        String csv = pastelExportService.exportAllClientsToPastelCsv();
        String filename = "Clients_All_" + getCurrentTimestamp() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(CSV_MEDIA_TYPE))
                .body(csv);
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("{\"status\": \"UP\", \"service\": \"PastelExport\", \"version\": \"1.0\"}");
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(FILE_DATE_FORMAT);
    }

    /**
     * Export a single RFQ to Pastel CSV format.
     */
    @GetMapping("/rfqs/{id}")
    public ResponseEntity<String> exportRfq(@PathVariable Long id) {
        try {
            String csv = pastelExportService.exportRfqToCsv(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename("PASTEL_RFQ_" + id + "_" + getTimestamp() + ".csv")
                            .build()
            );

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csv);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error exporting RFQ: " + e.getMessage());
        }
    }

    /**
     * Export multiple RFQs to Pastel CSV format.
     */
    @PostMapping("/rfqs/bulk")
    public ResponseEntity<String> exportRfqsBulk(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> rfqIds = request.get("rfqIds");
            if (rfqIds == null || rfqIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("No RFQ IDs provided");
            }

            String csv = pastelExportService.exportRfqsToCsv(rfqIds);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename("PASTEL_RFQs_BULK_" + getTimestamp() + ".csv")
                            .build()
            );

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csv);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error exporting RFQs: " + e.getMessage());
        }
    }

    /**
     * Export all RFQs (simplified - no status filtering since repository doesn't support it).
     */
    @GetMapping("/rfqs/all")
    public ResponseEntity<String> exportAllRfqs() {
        try {
            String csv = pastelExportService.exportAllRfqsToCsv();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename("PASTEL_RFQs_ALL_" + getTimestamp() + ".csv")
                            .build()
            );

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csv);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error exporting all RFQs: " + e.getMessage());
        }
    }

    /**
     * Generate timestamp for filenames.
     */
    private String getTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }
}