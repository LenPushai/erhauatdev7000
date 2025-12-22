package com.erha.ops.controller;

import com.erha.ops.service.DocuSignService;
import com.erha.ops.service.DocuSignWebhookService;
import com.erha.ops.rfq.entity.RFQ;
import com.erha.ops.rfq.repository.RFQRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/docusign")
@PreAuthorize("isAuthenticated()")
public class DocuSignController {

    @Autowired
    private DocuSignService docuSignService;

    @Autowired
    private DocuSignWebhookService webhookService;

    @Autowired
    private RFQRepository rfqRepository;

    /**
     * Send quote for signature via DocuSign - Multi-Signatory Support
     * 
     * POST /api/v1/docusign/send-quote
     * Body: {
     *   "quoteId": 12,
     *   "managers": [{"name": "Manager 1", "email": "mgr1@erha.co.za"}],
     *   "clients": [{"name": "Client 1", "email": "client1@company.co.za"}]
     * }
     * 
     * Legacy format also supported:
     * Body: {
     *   "quoteId": 12,
     *   "managerEmail": "manager@erha.co.za",
     *   "managerName": "John Manager",
     *   "clientEmail": "client@company.co.za",
     *   "clientName": "Jane Client"
     * }
     */
    @PostMapping("/send-quote")
    @PreAuthorize("hasAuthority('SEND_QUOTE_DOCUSIGN')")
    public ResponseEntity<?> sendQuoteForSignature(@RequestBody Map<String, Object> request) {       
        try {
            Long quoteId = Long.parseLong(request.get("quoteId").toString());

            // Find RFQ
            Optional<RFQ> optionalRfq = rfqRepository.findById(quoteId);
            if (optionalRfq.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "RFQ not found with ID: " + quoteId
                ));
            }
            RFQ rfq = optionalRfq.get();

            // Parse managers and clients
            List<DocuSignService.Signatory> managers = new ArrayList<>();
            List<DocuSignService.Signatory> clients = new ArrayList<>();

            // Check for new format (arrays)
            if (request.containsKey("managers")) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> managerList = (List<Map<String, String>>) request.get("managers");
                for (Map<String, String> m : managerList) {
                    managers.add(new DocuSignService.Signatory(
                        m.get("name"),
                        m.get("email")
                    ));
                }
            }

            if (request.containsKey("clients")) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> clientList = (List<Map<String, String>>) request.get("clients");
                for (Map<String, String> c : clientList) {
                    clients.add(new DocuSignService.Signatory(
                        c.get("name"),
                        c.get("email")
                    ));
                }
            }

            // Fallback to legacy format
            if (managers.isEmpty() && request.containsKey("managerEmail")) {
                String managerEmail = request.get("managerEmail").toString();
                String managerName = request.containsKey("managerName") 
                    ? request.get("managerName").toString() : "ERHA Manager";
                managers.add(new DocuSignService.Signatory(managerName, managerEmail));
            }

            if (clients.isEmpty() && request.containsKey("clientEmail")) {
                String clientEmail = request.get("clientEmail").toString();
                String clientName = request.containsKey("clientName")
                    ? request.get("clientName").toString() : "Customer";
                clients.add(new DocuSignService.Signatory(clientName, clientEmail));
            }

            // Validate
            if (managers.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "At least one manager is required"
                ));
            }

            if (clients.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "At least one client is required"
                ));
            }

            // Send via DocuSign
            String envelopeId = docuSignService.sendRfqForSignatureMultiple(rfq, managers, clients);

            // Update RFQ
            rfq.setDocusignEnvelopeId(envelopeId);
            rfq.setDocusignStatus("PENDING");
            rfqRepository.save(rfq);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "envelopeId", envelopeId,
                "message", "Quote sent for signature successfully",
                "signerCount", managers.size() + clients.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * DocuSign webhook endpoint
     * DocuSign will call this when envelope status changes
     *
     * POST /api/v1/docusign/webhook
     */
    @PostMapping("/webhook")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> payload) {
        try {
            String envelopeId = payload.get("envelopeId").toString();
            String status = payload.get("status").toString();
            String recipientId = payload.getOrDefault("recipientId", "").toString();

            webhookService.handleWebhook(envelopeId, status, recipientId);

            return ResponseEntity.ok(Map.of("received", true));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get envelope status
     *
     * GET /api/v1/docusign/status/{envelopeId}
     */
    @GetMapping("/status/{envelopeId}")
    @PreAuthorize("hasAnyAuthority('VIEW_ALL_QUOTES', 'VIEW_OWN_QUOTES')")
    public ResponseEntity<?> getEnvelopeStatus(@PathVariable String envelopeId) {
        try {
            String status = docuSignService.getEnvelopeStatus(envelopeId);

            return ResponseEntity.ok(Map.of(
                "envelopeId", envelopeId,
                "status", status
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get detailed envelope info with signer statuses
     *
     * GET /api/v1/docusign/info/{envelopeId}
     */
    @GetMapping("/info/{envelopeId}")
    @PreAuthorize("hasAnyAuthority('VIEW_ALL_QUOTES', 'VIEW_OWN_QUOTES')")
    public ResponseEntity<?> getEnvelopeInfo(@PathVariable String envelopeId) {
        try {
            DocuSignService.EnvelopeInfo info = docuSignService.getEnvelopeInfo(envelopeId);

            return ResponseEntity.ok(info);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
}