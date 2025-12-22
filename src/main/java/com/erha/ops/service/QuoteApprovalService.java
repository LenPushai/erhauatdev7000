package com.erha.ops.service;

import com.erha.ops.dto.ApprovalPinResponse;
import com.erha.ops.dto.ApprovePinRequest;
import com.erha.ops.entity.Quote;
import com.erha.ops.entity.Client;
import com.erha.ops.repository.QuoteRepository;
import com.erha.ops.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class QuoteApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(QuoteApprovalService.class);

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired(required = false)
    private ClientRepository clientRepository;

    @Autowired(required = false)
    private EmailService emailService;

    /**
     * Generate a 6-digit PIN for quote approval and send email notification
     */
    public ApprovalPinResponse generateApprovalPin(Long quoteId) {
        logger.info("Generating approval PIN for quote ID: {}", quoteId);

        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote not found"));

        // Check if quote is in valid state for approval
        if (quote.getQuoteStatus() != Quote.QuoteStatus.DRAFT &&
                quote.getQuoteStatus() != Quote.QuoteStatus.NEEDS_REVISION &&
                quote.getQuoteStatus() != Quote.QuoteStatus.SENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Quote must be in DRAFT, SENT, or NEEDS_REVISION status to submit for approval. Current status: " + quote.getQuoteStatus());
        }

        // Generate random 6-digit PIN
        String pin = String.format("%06d", new Random().nextInt(1000000));

        // Set expiry time (24 hours from now)
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        // Update quote with PIN details
        quote.setApprovalPin(pin);
        quote.setPinGeneratedAt(LocalDateTime.now());
        quote.setPinExpiresAt(expiresAt);
        quote.setPinUsedAt(null);
        quote.setQuoteStatus(Quote.QuoteStatus.PENDING_APPROVAL);

        quoteRepository.save(quote);

        logger.info("PIN generated for quote {}: {} (expires: {})", quote.getQuoteNumber(), pin, expiresAt);

        // Get client name for email
        String clientName = null;
        if (quote.getClientId() != null && clientRepository != null) {
            try {
                Client client = clientRepository.findById(quote.getClientId()).orElse(null);
                if (client != null) {
                    clientName = client.getCompanyName();
                }
            } catch (Exception e) {
                logger.warn("Could not fetch client name: {}", e.getMessage());
            }
        }

        // Get current user
        String submittedBy = "System";
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
                submittedBy = auth.getName();
            }
        } catch (Exception e) {
            // Use default
        }

        // Send email notification to manager
        if (emailService != null) {
            try {
                emailService.sendApprovalRequestEmail(
                        quote.getQuoteNumber(),
                        quote.getQuoteId(),
                        clientName,
                        quote.getValueInclVat(),
                        pin,
                        expiresAt,
                        submittedBy
                );
            } catch (Exception e) {
                logger.error("Failed to send approval email: {}", e.getMessage());
                // Don't fail the PIN generation if email fails
            }
        } else {
            logger.warn("EmailService not available - skipping email notification");
        }

        return new ApprovalPinResponse(pin, expiresAt, quote.getQuoteNumber(), quote.getQuoteId());
    }

    /**
     * Approve quote using PIN
     */
    public Quote approveWithPin(ApprovePinRequest request) {
        logger.info("Attempting to approve quote {} with PIN", request.getQuoteNumber());

        Quote quote = quoteRepository.findByQuoteNumber(request.getQuoteNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Quote not found with number: " + request.getQuoteNumber()));

        if (quote.getApprovalPin() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No approval PIN found for this quote. Please request approval first.");
        }

        if (!quote.getApprovalPin().equals(request.getPin())) {
            logger.warn("Invalid PIN attempt for quote {}", request.getQuoteNumber());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PIN");
        }

        if (LocalDateTime.now().isAfter(quote.getPinExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.GONE,
                    "PIN has expired. Please request a new approval PIN.");
        }

        if (quote.getPinUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "PIN has already been used. This quote was approved on " + quote.getApprovedDate());
        }

        // Get approver name
        String approvedBy = "MANAGER";
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
                approvedBy = auth.getName().toUpperCase();
            }
        } catch (Exception e) {
            // Use default
        }

        quote.setQuoteStatus(Quote.QuoteStatus.APPROVED);
        quote.setPinUsedAt(LocalDateTime.now());
        quote.setApprovedDate(LocalDateTime.now());
        quote.setApprovedBy(approvedBy);

        quoteRepository.save(quote);

        logger.info("Quote {} approved by {}", quote.getQuoteNumber(), approvedBy);

        return quote;
    }

    /**
     * Check PIN status for a quote
     */
    public String checkPinStatus(Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote not found"));

        if (quote.getApprovalPin() == null) {
            return "NO_PIN";
        }

        if (quote.getPinUsedAt() != null) {
            return "USED";
        }

        if (LocalDateTime.now().isAfter(quote.getPinExpiresAt())) {
            return "EXPIRED";
        }

        return "ACTIVE";
    }
}