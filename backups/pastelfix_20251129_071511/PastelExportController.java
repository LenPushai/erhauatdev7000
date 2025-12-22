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

@RestController
@RequestMapping("/api/export/pastel")
@CrossOrigin(origins = "*")
public class PastelExportController {

    private static final String CSV_MEDIA_TYPE = "text/csv";
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private PastelExportService pastelExportService;

    @GetMapping("/quotes/{id}")
    public ResponseEntity<String> exportQuote(@PathVariable UUID id) {
        try {
            String csv = pastelExportService.exportQuoteToPastelCsv(id);
            String filename = "Quote_" + id.toString().substring(0, 8) + "_" + getCurrentTimestamp() + ".csv";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(CSV_MEDIA_TYPE))
                    .body(csv);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/quotes/bulk")
    public ResponseEntity<String> exportQuotesBulk(@RequestBody List<UUID> ids) {
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
}