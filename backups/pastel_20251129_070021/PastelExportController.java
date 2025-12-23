
package com.erha.quote.controller;

import com.erha.quote.service.PastelExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Pastel Export Controller - ERHA OPS
 * REST endpoints for exporting data to Sage Pastel format
 *
 * @author PUSH AI Foundation
 * @version 1.0
 */
@RestController
@RequestMapping("/api/export/pastel")
@CrossOrigin(origins = "*")
public class PastelExportController {

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private PastelExportService pastelExportService;

    /**
     * Export a single quote to Pastel CSV format
     * GET /api/export/pastel/quotes/{id}
     */
    @GetMapping("/quotes/{id}")
    public ResponseEntity<byte[]> exportQuote(@PathVariable UUID id) {
        try {
            String csv = pastelExportService.exportQuoteToPastelCsv(id);
            String filename = String.format("ERHA_Quote_%s_%s.csv",
                    id.toString().substring(0, 8),
                    LocalDateTime.now().format(FILE_DATE_FORMAT));

            return createCsvResponse(csv, filename);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Export multiple quotes to Pastel CSV format
     * POST /api/export/pastel/quotes/bulk
     * Body: ["uuid1", "uuid2", ...]
     */
    @PostMapping("/quotes/bulk")
    public ResponseEntity<byte[]> exportQuotesBulk(@RequestBody List<UUID> quoteIds) {
        try {
            String csv = pastelExportService.exportQuotesToPastelCsv(quoteIds);
            String filename = String.format("ERHA_Quotes_Bulk_%s.csv",
                    LocalDateTime.now().format(FILE_DATE_FORMAT));

            return createCsvResponse(csv, filename);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Export all approved quotes to Pastel CSV format
     * GET /api/export/pastel/quotes/approved
     */
    @GetMapping("/quotes/approved")
    public ResponseEntity<byte[]> exportApprovedQuotes() {
        try {
            String csv = pastelExportService.exportApprovedQuotesToPastelCsv();
            String filename = String.format("ERHA_Quotes_Approved_%s.csv",
                    LocalDateTime.now().format(FILE_DATE_FORMAT));

            return createCsvResponse(csv, filename);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Export a single client to Pastel CSV format
     * GET /api/export/pastel/clients/{id}
     */
    @GetMapping("/clients/{id}")
    public ResponseEntity<byte[]> exportClient(@PathVariable Long id) {
        try {
            String csv = pastelExportService.exportClientToPastelCsv(id);
            String filename = String.format("ERHA_Client_%d_%s.csv",
                    id,
                    LocalDateTime.now().format(FILE_DATE_FORMAT));

            return createCsvResponse(csv, filename);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Export all clients to Pastel CSV format
     * GET /api/export/pastel/clients
     */
    @GetMapping("/clients")
    public ResponseEntity<byte[]> exportAllClients() {
        try {
            String csv = pastelExportService.exportAllClientsToPastelCsv();
            String filename = String.format("ERHA_Clients_%s.csv",
                    LocalDateTime.now().format(FILE_DATE_FORMAT));

            return createCsvResponse(csv, filename);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Health check endpoint
     * GET /api/export/pastel/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Pastel Export Service is running");
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private ResponseEntity<byte[]> createCsvResponse(String csv, String filename) {
        byte[] csvBytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(csvBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }
}