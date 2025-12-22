package com.erha.quote.service;

import com.erha.ops.entity.Client;
import com.erha.ops.repository.ClientRepository;
import com.erha.ops.rfq.entity.RFQ;
import com.erha.ops.rfq.repository.RFQRepository;
import com.erha.quote.model.Quote;
import com.erha.quote.model.QuoteStatus;
import com.erha.quote.repository.QuoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Pastel Export Service - ERHA OPS
 * Generates CSV files in Sage Pastel compatible format
 *
 * @author PUSH AI Foundation
 * @version 1.0
 */
@Service
public class PastelExportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String NEWLINE = "\r\n";

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired(required = false)
    private RFQRepository rfqRepository;

    /**
     * Export a single quote to Sage Pastel CSV format
     */
    public String exportQuoteToPastelCsv(UUID quoteId) {
        Optional<Quote> quoteOpt = quoteRepository.findById(quoteId);
        if (quoteOpt.isEmpty()) {
            throw new RuntimeException("Quote not found: " + quoteId);
        }

        Quote quote = quoteOpt.get();
        return generateQuoteCsv(quote);
    }

    /**
     * Export multiple quotes to Sage Pastel CSV format
     */
    public String exportQuotesToPastelCsv(List<UUID> quoteIds) {
        StringBuilder csv = new StringBuilder();
        csv.append("sep=,").append(NEWLINE);
        csv.append(getQuoteHeader()).append(NEWLINE);

        BigDecimal grandTotalExclusive = BigDecimal.ZERO;
        BigDecimal grandTotalVat = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (UUID quoteId : quoteIds) {
            Optional<Quote> quoteOpt = quoteRepository.findById(quoteId);
            if (quoteOpt.isPresent()) {
                Quote quote = quoteOpt.get();
                csv.append(getQuoteRow(quote)).append(NEWLINE);

                grandTotalExclusive = grandTotalExclusive.add(
                    quote.getSubtotal() != null ? quote.getSubtotal() : BigDecimal.ZERO);
                grandTotalVat = grandTotalVat.add(
                    quote.getTaxAmount() != null ? quote.getTaxAmount() : BigDecimal.ZERO);
                grandTotal = grandTotal.add(
                    quote.getTotalAmount() != null ? quote.getTotalAmount() : BigDecimal.ZERO);
            }
        }

        csv.append(String.format("\"Grand Total\",,,,,\"%s\",\"%s\",\"%s\",,",
                formatAmount(grandTotalExclusive),
                formatAmount(grandTotalVat),
                formatAmount(grandTotal)
        )).append(NEWLINE);

        return csv.toString();
    }

    /**
     * Export all approved quotes to Sage Pastel CSV format
     */
    public String exportApprovedQuotesToPastelCsv() {
        List<Quote> approvedQuotes = quoteRepository.findByStatus(QuoteStatus.APPROVED);

        StringBuilder csv = new StringBuilder();
        csv.append("sep=,").append(NEWLINE);
        csv.append(getQuoteHeader()).append(NEWLINE);

        BigDecimal grandTotalExclusive = BigDecimal.ZERO;
        BigDecimal grandTotalVat = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Quote quote : approvedQuotes) {
            csv.append(getQuoteRow(quote)).append(NEWLINE);

            grandTotalExclusive = grandTotalExclusive.add(
                quote.getSubtotal() != null ? quote.getSubtotal() : BigDecimal.ZERO);
            grandTotalVat = grandTotalVat.add(
                quote.getTaxAmount() != null ? quote.getTaxAmount() : BigDecimal.ZERO);
            grandTotal = grandTotal.add(
                quote.getTotalAmount() != null ? quote.getTotalAmount() : BigDecimal.ZERO);
        }

        csv.append(String.format("\"Grand Total\",,,,,\"%s\",\"%s\",\"%s\",,",
                formatAmount(grandTotalExclusive),
                formatAmount(grandTotalVat),
                formatAmount(grandTotal)
        )).append(NEWLINE);

        return csv.toString();
    }

    /**
     * Export a single client to Sage Pastel CSV format
     * Note: Client uses Long id, not UUID
     */
    public String exportClientToPastelCsv(Long clientId) {
        Optional<Client> clientOpt = clientRepository.findById(clientId);
        if (clientOpt.isEmpty()) {
            throw new RuntimeException("Client not found: " + clientId);
        }

        Client client = clientOpt.get();

        StringBuilder csv = new StringBuilder();
        csv.append("sep=,").append(NEWLINE);
        csv.append(getClientHeader()).append(NEWLINE);
        csv.append(getClientRow(client)).append(NEWLINE);

        return csv.toString();
    }

    /**
     * Export all clients to Sage Pastel CSV format
     */
    public String exportAllClientsToPastelCsv() {
        List<Client> clients = clientRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("sep=,").append(NEWLINE);
        csv.append(getClientHeader()).append(NEWLINE);

        for (Client client : clients) {
            csv.append(getClientRow(client)).append(NEWLINE);
        }

        return csv.toString();
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private String generateQuoteCsv(Quote quote) {
        StringBuilder csv = new StringBuilder();
        csv.append("sep=,").append(NEWLINE);
        csv.append(getQuoteHeader()).append(NEWLINE);
        csv.append(getQuoteRow(quote)).append(NEWLINE);

        csv.append(String.format("\"Grand Total\",,,,,\"%s\",\"%s\",\"%s\",,",
                formatAmount(quote.getSubtotal()),
                formatAmount(quote.getTaxAmount()),
                formatAmount(quote.getTotalAmount())
        )).append(NEWLINE);

        return csv.toString();
    }

    private String getQuoteHeader() {
        return "\"Date\",\"Expiry Date\",\"Document No.\",\"Customer Ref.\",\"Customer\",\"Exclusive\",\"VAT\",\"Total\",\"Sales Rep\",\"Status\"";
    }

    private String getQuoteRow(Quote quote) {
        String clientName = getClientNameFromQuote(quote);
        String customerRef = getRfqReference(quote.getRfqId());
        String date = formatDate(quote.getCreatedAt());
        String expiryDate = formatDate(quote.getValidUntil());
        String status = mapStatusToSage(quote.getStatus());

        return String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                date,
                expiryDate,
                quote.getQuoteNumber() != null ? quote.getQuoteNumber() : "",
                customerRef,
                clientName,
                formatAmount(quote.getSubtotal()),
                formatAmount(quote.getTaxAmount()),
                formatAmount(quote.getTotalAmount()),
                "",
                status
        );
    }

    private String getClientHeader() {
        return "\"Name\",\"Category\",\"Active\",\"Contact Name\",\"Telephone\",\"Email\",\"Address\",\"VAT Number\",\"Balance\"";
    }

    private String getClientRow(Client client) {
        return String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                escapeQuotes(client.getCompanyName() != null ? client.getCompanyName() : ""),
                client.getIndustry() != null ? escapeQuotes(client.getIndustry()) : "",
                client.getStatus() != null && client.getStatus() == Client.ClientStatus.ACTIVE ? "Yes" : "No",
                escapeQuotes(client.getContactPerson() != null ? client.getContactPerson() : ""),
                escapeQuotes(client.getPhone() != null ? client.getPhone() : ""),
                escapeQuotes(client.getEmail() != null ? client.getEmail() : ""),
                escapeQuotes(formatAddress(client)),
                escapeQuotes(client.getVatNumber() != null ? client.getVatNumber() : ""),
                "0.00"
        );
    }

    private String getClientNameFromQuote(Quote quote) {
        if (quote.getClientId() == null) {
            return quote.getTitle() != null ? quote.getTitle() : "Unknown";
        }
        return quote.getTitle() != null ? quote.getTitle() : "Client-" + quote.getClientId().toString().substring(0, 8);
    }

    private String getRfqReference(UUID rfqId) {
        if (rfqId == null || rfqRepository == null) {
            return "";
        }
        return rfqId.toString().substring(0, 8).toUpperCase();
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DATE_FORMAT);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0";
        return amount.setScale(0, RoundingMode.HALF_UP).toString();
    }

    private String formatAddress(Client client) {
        StringBuilder address = new StringBuilder();
        if (client.getPhysicalAddress() != null && !client.getPhysicalAddress().isEmpty()) {
            address.append(client.getPhysicalAddress());
        }
        if (client.getCity() != null && !client.getCity().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(client.getCity());
        }
        if (client.getProvince() != null && !client.getProvince().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(client.getProvince());
        }
        if (client.getPostalCode() != null && !client.getPostalCode().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(client.getPostalCode());
        }
        return address.toString();
    }

    private String mapStatusToSage(QuoteStatus status) {
        if (status == null) return "Draft";

        switch (status) {
            case DRAFT:
                return "Draft";
            case PENDING_REVIEW:
            case PENDING_QUALITY_REVIEW:
            case PENDING_SAFETY_REVIEW:
            case PENDING_APPROVAL:
                return "Pending";
            case APPROVED:
                return "Approved";
            case SENT:
            case SENT_TO_CLIENT:
                return "Sent";
            case VIEWED:
                return "Viewed";
            case UNDER_NEGOTIATION:
                return "Negotiation";
            case ACCEPTED:
                return "Accepted";
            case REJECTED:
                return "Declined";
            case EXPIRED:
                return "Expired";
            case CANCELLED:
                return "Cancelled";
            case CONVERTED_TO_CONTRACT:
                return "Converted";
            default:
                return status.toString();
        }
    }

    private String escapeQuotes(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
