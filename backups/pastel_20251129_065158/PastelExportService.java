package com.erha.quote.service;

import com.erha.ops.entity.Client;
import com.erha.ops.repository.ClientRepository;
import com.erha.ops.rfq.entity.RFQ;
import com.erha.ops.rfq.repository.RFQRepository;
import com.erha.quote.model.Quote;
import com.erha.quote.model.QuoteItem;
import com.erha.quote.repository.QuoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.math.BigDecimal;
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
    private static final String CSV_SEPARATOR = ",";
    private static final String NEWLINE = "\r\n";

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired(required = false)
    private RFQRepository rfqRepository;

    /**
     * Export a single quote to Sage Pastel CSV format
     * Format matches Sage Business Cloud Accounting quote import
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

        // Add CSV separator hint for Excel
        csv.append("sep=,").append(NEWLINE);

        // Add header row
        csv.append(getQuoteHeader()).append(NEWLINE);

        BigDecimal grandTotalExclusive = BigDecimal.ZERO;
        BigDecimal grandTotalVat = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (UUID quoteId : quoteIds) {
            Optional<Quote> quoteOpt = quoteRepository.findById(quoteId);
            if (quoteOpt.isPresent()) {
                Quote quote = quoteOpt.get();
                csv.append(getQuoteRow(quote)).append(NEWLINE);

                grandTotalExclusive = grandTotalExclusive.add(quote.getSubtotal() != null ? quote.getSubtotal() : BigDecimal.ZERO);
                grandTotalVat = grandTotalVat.add(quote.getTaxAmount() != null ? quote.getTaxAmount() : BigDecimal.ZERO);
                grandTotal = grandTotal.add(quote.getTotalAmount() != null ? quote.getTotalAmount() : BigDecimal.ZERO);
            }
        }

        // Add grand total row
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
        List<Quote> approvedQuotes = quoteRepository.findByStatus(com.erha.quote.model.QuoteStatus.APPROVED);

        StringBuilder csv = new StringBuilder();
        csv.append("sep=,").append(NEWLINE);
        csv.append(getQuoteHeader()).append(NEWLINE);

        BigDecimal grandTotalExclusive = BigDecimal.ZERO;
        BigDecimal grandTotalVat = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Quote quote : approvedQuotes) {
            csv.append(getQuoteRow(quote)).append(NEWLINE);

            grandTotalExclusive = grandTotalExclusive.add(quote.getSubtotal() != null ? quote.getSubtotal() : BigDecimal.ZERO);
            grandTotalVat = grandTotalVat.add(quote.getTaxAmount() != null ? quote.getTaxAmount() : BigDecimal.ZERO);
            grandTotal = grandTotal.add(quote.getTotalAmount() != null ? quote.getTotalAmount() : BigDecimal.ZERO);
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
     */
    public String exportClientToPastelCsv(UUID clientId) {
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

        // CSV separator hint for Excel
        csv.append("sep=,").append(NEWLINE);

        // Header row
        csv.append(getQuoteHeader()).append(NEWLINE);

        // Data row
        csv.append(getQuoteRow(quote)).append(NEWLINE);

        // Grand total row
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
        // Get client name
        String clientName = getClientName(quote.getClientId());

        // Get RFQ reference if available
        String customerRef = getRfqReference(quote.getRfqId());

        // Format dates
        String date = formatDate(quote.getCreatedAt());
        String expiryDate = formatDate(quote.getValidUntil());

        // Map status to Sage format
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
                "", // Sales Rep - can be enhanced later
                status
        );
    }

    private String getClientHeader() {
        return "\"Name\",\"Category\",\"Active\",\"Contact Name\",\"Telephone\",\"Email\",\"Address\",\"VAT Number\",\"Balance\"";
    }

    private String getClientRow(Client client) {
        return String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                escapeQuotes(client.getCompanyName() != null ? client.getCompanyName() : ""),
                client.getIndustry() != null ? escapeQuotes(client.getIndustry()) : "", // Category = Industry
                client.getStatus() != null && client.getStatus().toString().equals("ACTIVE") ? "Yes" : "No", // Active
                escapeQuotes(client.getContactPerson() != null ? client.getContactPerson() : ""),
                escapeQuotes(client.getPhone() != null ? client.getPhone() : ""),
                escapeQuotes(client.getEmail() != null ? client.getEmail() : ""),
                escapeQuotes(formatAddress(client)),
                escapeQuotes(client.getVatNumber() != null ? client.getVatNumber() : ""),
                "0.00" // Balance - would need to calculate from invoices
        );
    }

    private String getClientName(UUID clientId) {
        if (clientId == null) return "";

        try {
            Optional<Client> clientOpt = clientRepository.findById(clientId);
            return clientOpt.map(Client::getCompanyName).orElse("");
        } catch (Exception e) {
            return "";
        }
    }

    private String getRfqReference(UUID rfqId) {
        if (rfqId == null || rfqRepository == null) return "";

        try {
            Optional<RFQ> rfqOpt = rfqRepository.findById(rfqId);
            return rfqOpt.map(RFQ::getRfqNumber).orElse("");
        } catch (Exception e) {
            return "";
        }
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DATE_FORMAT);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0";
        // Sage expects amounts without currency symbol, comma separated thousands
        return amount.setScale(0, java.math.RoundingMode.HALF_UP).toString();
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

    private String mapStatusToSage(com.erha.quote.model.QuoteStatus status) {
        if (status == null) return "Draft";

        switch (status) {
            case DRAFT:
                return "Draft";
            case PENDING:
            case PENDING_REVIEW:
                return "Pending";
            case APPROVED:
                return "Approved";
            case SENT:
                return "Sent";
            case ACCEPTED:
                return "Accepted";
            case REJECTED:
            case DECLINED:
                return "Declined";
            case EXPIRED:
                return "Expired";
            case CANCELLED:
                return "Cancelled";
            case CONVERTED:
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