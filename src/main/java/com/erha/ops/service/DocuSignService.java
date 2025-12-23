package com.erha.ops.service;

import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.ApiException;
import com.docusign.esign.client.auth.OAuth;
import com.docusign.esign.model.*;
import com.erha.ops.rfq.entity.RFQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Service
public class DocuSignService {

    private static final Logger logger = LoggerFactory.getLogger(DocuSignService.class);

    @Value("${docusign.integration-key:}")
    private String integrationKey;

    @Value("${docusign.account-id:}")
    private String accountId;

    @Value("${docusign.user-id:}")
    private String userId;

    @Value("${docusign.secret-key:}")
    private String privateKeyPath;

    @Value("${docusign.base-path:https://demo.docusign.net/restapi}")
    private String basePath;

    @Value("${docusign.oauth-base-path:https://account-d.docusign.com}")
    private String oauthBasePath;

    private ApiClient apiClient;
    private boolean configured = false;

    @PostConstruct
    public void init() {
        if (integrationKey != null && !integrationKey.isEmpty() &&
                accountId != null && !accountId.isEmpty() &&
                userId != null && !userId.isEmpty() &&
                privateKeyPath != null && !privateKeyPath.isEmpty()) {
            configured = true;
            logger.info("DocuSign service configured successfully");
        } else {
            logger.warn("DocuSign service not fully configured - missing credentials");
        }
    }

    public boolean isConfigured() {
        return configured;
    }

    private void initializeApiClient() throws Exception {
        logger.info("Initializing DocuSign API client...");

        apiClient = new ApiClient(basePath);
        apiClient.setOAuthBasePath(oauthBasePath);

        // Read private key
        Path keyPath = Paths.get(privateKeyPath);
        byte[] privateKeyBytes = Files.readAllBytes(keyPath);

        // Get access token using JWT
        java.util.List<String> scopes = Arrays.asList(OAuth.Scope_SIGNATURE, OAuth.Scope_IMPERSONATION);
        OAuth.OAuthToken oAuthToken = apiClient.requestJWTUserToken(
                integrationKey,
                userId,
                scopes,
                privateKeyBytes,
                3600
        );

        apiClient.setAccessToken(oAuthToken.getAccessToken(), oAuthToken.getExpiresIn());
        logger.info("DocuSign API client initialized successfully");
    }

    /**
     * DTO for signatory information
     */
    public static class Signatory {
        private String name;
        private String email;

        public Signatory() {}

        public Signatory(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    /**
     * Send RFQ for signature with multiple managers and clients
     * Managers sign first (in order), then clients sign (in order)
     */
    public String sendRfqForSignatureMultiple(RFQ rfq, List<Signatory> managers, List<Signatory> clients) throws Exception {
        if (!configured) {
            throw new IllegalStateException("DocuSign service is not configured");
        }

        initializeApiClient();

        // Read the PDF file
        Path pdfPath = Paths.get(rfq.getQuotePdfPath());
        if (!Files.exists(pdfPath)) {
            throw new IOException("Quote PDF file not found: " + rfq.getQuotePdfPath());
        }
        byte[] pdfBytes = Files.readAllBytes(pdfPath);
        String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);

        // Create document
        Document document = new Document();
        document.setDocumentBase64(pdfBase64);
        document.setName("Quote for " + rfq.getJobNo());
        document.setFileExtension("pdf");
        document.setDocumentId("1");

        // Create signers with routing order
        List<Signer> signers = new ArrayList<>();
        int routingOrder = 1;
        int recipientId = 1;
        int yPosition = 600; // Starting Y position for signatures

        // Add managers first (they sign in order)
        for (Signatory manager : managers) {
            Signer signer = createSigner(
                    manager.getName(),
                    manager.getEmail(),
                    String.valueOf(recipientId),
                    String.valueOf(routingOrder),
                    yPosition
            );
            signers.add(signer);

            logger.info("Added manager signer: {} ({}) - routing order {}",
                    manager.getName(), manager.getEmail(), routingOrder);

            recipientId++;
            routingOrder++;
            yPosition += 60; // Move down for next signature
        }

        // Add clients second (they sign after managers)
        for (Signatory client : clients) {
            Signer signer = createSigner(
                    client.getName(),
                    client.getEmail(),
                    String.valueOf(recipientId),
                    String.valueOf(routingOrder),
                    yPosition
            );
            signers.add(signer);

            logger.info("Added client signer: {} ({}) - routing order {}",
                    client.getName(), client.getEmail(), routingOrder);

            recipientId++;
            routingOrder++;
            yPosition += 60;
        }

        // Create recipients
        Recipients recipients = new Recipients();
        recipients.setSigners(signers);

        // Create envelope
        EnvelopeDefinition envelope = new EnvelopeDefinition();
        envelope.setEmailSubject("ERHA Quote for Signature: " + rfq.getJobNo());
        envelope.setEmailBlurb("Please review and sign the attached quote for " + rfq.getJobNo() +
                ". Description: " + (rfq.getDescription() != null ? rfq.getDescription() : "N/A"));
        envelope.setDocuments(Arrays.asList(document));
        envelope.setRecipients(recipients);
        envelope.setStatus("sent");

        // Send envelope
        EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
        EnvelopeSummary result = envelopesApi.createEnvelope(accountId, envelope);

        logger.info("DocuSign envelope created: {} with {} signers",
                result.getEnvelopeId(), signers.size());

        return result.getEnvelopeId();
    }

    /**
     * Create a signer with signature tab
     */
    private Signer createSigner(String name, String email, String recipientId,
                                String routingOrder, int yPosition) {
        // Create signature tab
        SignHere signHere = new SignHere();
        signHere.setAnchorString("/sig" + recipientId + "/");
        signHere.setAnchorUnits("pixels");
        signHere.setAnchorXOffset("0");
        signHere.setAnchorYOffset("0");

        // Fallback to absolute positioning if anchor not found
        signHere.setDocumentId("1");
        signHere.setPageNumber("1");
        signHere.setXPosition("400");
        signHere.setYPosition(String.valueOf(yPosition));

        // Create date signed tab
        DateSigned dateSigned = new DateSigned();
        dateSigned.setDocumentId("1");
        dateSigned.setPageNumber("1");
        dateSigned.setXPosition("500");
        dateSigned.setYPosition(String.valueOf(yPosition));

        // Create tabs
        Tabs tabs = new Tabs();
        tabs.setSignHereTabs(Arrays.asList(signHere));
        tabs.setDateSignedTabs(Arrays.asList(dateSigned));

        // Create signer
        Signer signer = new Signer();
        signer.setEmail(email);
        signer.setName(name);
        signer.setRecipientId(recipientId);
        signer.setRoutingOrder(routingOrder);
        signer.setTabs(tabs);

        return signer;
    }

    /**
     * Legacy method - single manager and client
     * Kept for backward compatibility
     */
    public String sendRfqForSignature(RFQ rfq, String managerEmail, String managerName) throws Exception {
        List<Signatory> managers = Arrays.asList(new Signatory(managerName, managerEmail));
        List<Signatory> clients = Arrays.asList(new Signatory(
                rfq.getContactPerson() != null ? rfq.getContactPerson() : "Customer",
                rfq.getContactEmail()
        ));
        return sendRfqForSignatureMultiple(rfq, managers, clients);
    }

    /**
     * Get envelope status
     */
    public String getEnvelopeStatus(String envelopeId) throws Exception {
        if (!configured) {
            throw new IllegalStateException("DocuSign service is not configured");
        }

        initializeApiClient();

        EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
        Envelope envelope = envelopesApi.getEnvelope(accountId, envelopeId);

        return envelope.getStatus();
    }

    /**
     * Get detailed envelope information including signer statuses
     */
    public EnvelopeInfo getEnvelopeInfo(String envelopeId) throws Exception {
        if (!configured) {
            throw new IllegalStateException("DocuSign service is not configured");
        }

        initializeApiClient();

        EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
        Envelope envelope = envelopesApi.getEnvelope(accountId, envelopeId);
        Recipients recipients = envelopesApi.listRecipients(accountId, envelopeId);

        EnvelopeInfo info = new EnvelopeInfo();
        info.setEnvelopeId(envelopeId);
        info.setStatus(envelope.getStatus());
        info.setStatusDateTime(envelope.getStatusChangedDateTime());

        List<SignerInfo> signerInfos = new ArrayList<>();
        if (recipients.getSigners() != null) {
            for (Signer signer : recipients.getSigners()) {
                SignerInfo si = new SignerInfo();
                si.setName(signer.getName());
                si.setEmail(signer.getEmail());
                si.setStatus(signer.getStatus());
                si.setSignedDateTime(signer.getSignedDateTime());
                si.setRoutingOrder(signer.getRoutingOrder());
                signerInfos.add(si);
            }
        }
        info.setSigners(signerInfos);

        return info;
    }

    /**
     * DTO for envelope information
     */
    public static class EnvelopeInfo {
        private String envelopeId;
        private String status;
        private String statusDateTime;
        private List<SignerInfo> signers;

        public String getEnvelopeId() { return envelopeId; }
        public void setEnvelopeId(String envelopeId) { this.envelopeId = envelopeId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getStatusDateTime() { return statusDateTime; }
        public void setStatusDateTime(String statusDateTime) { this.statusDateTime = statusDateTime; }
        public List<SignerInfo> getSigners() { return signers; }
        public void setSigners(List<SignerInfo> signers) { this.signers = signers; }
    }

    /**
     * DTO for signer information
     */
    public static class SignerInfo {
        private String name;
        private String email;
        private String status;
        private String signedDateTime;
        private String routingOrder;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getSignedDateTime() { return signedDateTime; }
        public void setSignedDateTime(String signedDateTime) { this.signedDateTime = signedDateTime; }
        public String getRoutingOrder() { return routingOrder; }
        public void setRoutingOrder(String routingOrder) { this.routingOrder = routingOrder; }
    }
}
