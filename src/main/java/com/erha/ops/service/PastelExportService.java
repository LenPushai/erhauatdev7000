package com.erha.ops.service;

import com.erha.ops.entity.Client;
import com.erha.ops.rfq.entity.RFQ;
import com.erha.ops.repository.RfqRepository;
import java.io.StringWriter;
import java.util.Arrays;

import com.erha.ops.entity.Quote;
import com.erha.ops.repository.ClientRepository;
import com.erha.ops.repository.QuoteRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class PastelExportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String NEWLINE = "\r\n";
    private static final String CSV_SEPARATOR = "sep=,";

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired(required = false)
    private RfqRepository rfqRepository;

    public String exportQuoteToPastelCsv(Long quoteId) {
        Optional<Quote> quoteOpt = quoteRepository.findById(quoteId);
        if (quoteOpt.isEmpty()) {
            throw new RuntimeException("Quote not found: " + quoteId);
        }
        Quote quote = quoteOpt.get();
        return generateQuoteCsv(quote);
    }

    public String exportQuotesToPastelCsv(List<Long> quoteIds) {
        StringBuilder csv = new StringBuilder();
        csv.append("sep=,").append(NEWLINE);
        csv.append(getQuoteHeader()).append(NEWLINE);

        BigDecimal grandTotalExclusive = BigDecimal.ZERO;
        BigDecimal grandTotalVat = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Long quoteId : quoteIds) {
            Optional<Quote> quoteOpt = quoteRepository.findById(quoteId);
            if (quoteOpt.isPresent()) {
                Quote quote = quoteOpt.get();
                csv.append(getQuoteRow(quote)).append(NEWLINE);
                grandTotalExclusive = grandTotalExclusive.add(quote.getValueExclVat() != null ? quote.getValueExclVat() : BigDecimal.ZERO);
                grandTotalVat = grandTotalVat.add(quote.getVatAmount() != null ? quote.getVatAmount() : BigDecimal.ZERO);
                grandTotal = grandTotal.add(quote.getValueInclVat() != null ? quote.getValueInclVat() : BigDecimal.ZERO);
            }
        }

        csv.append(String.format("\"Grand Total\",,,,,\"%s\",\"%s\",\"%s\",,", formatAmount(grandTotalExclusive), formatAmount(grandTotalVat), formatAmount(grandTotal))).append(NEWLINE);
        return csv.toString();
    }

    public String exportApprovedQuotesToPastelCsv() {
        List<Quote> allQuotes = quoteRepository.findAll();
        List<Quote> approvedQuotes = allQuotes.stream()
                .filter(q -> q.getQuoteStatus() != null && q.getQuoteStatus() == Quote.QuoteStatus.APPROVED)
                .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("sep=,").append(NEWLINE);
        csv.append(getQuoteHeader()).append(NEWLINE);

        BigDecimal grandTotalExclusive = BigDecimal.ZERO;
        BigDecimal grandTotalVat = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Quote quote : approvedQuotes) {
            csv.append(getQuoteRow(quote)).append(NEWLINE);
            grandTotalExclusive = grandTotalExclusive.add(quote.getValueExclVat() != null ? quote.getValueExclVat() : BigDecimal.ZERO);
            grandTotalVat = grandTotalVat.add(quote.getVatAmount() != null ? quote.getVatAmount() : BigDecimal.ZERO);
            grandTotal = grandTotal.add(quote.getValueInclVat() != null ? quote.getValueInclVat() : BigDecimal.ZERO);
        }

        csv.append(String.format("\"Grand Total\",,,,,\"%s\",\"%s\",\"%s\",,", formatAmount(grandTotalExclusive), formatAmount(grandTotalVat), formatAmount(grandTotal))).append(NEWLINE);
        return csv.toString();
    }

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

    private String generateQuoteCsv(Quote quote) {
        StringBuilder csv = new StringBuilder();
        csv.append("sep=,").append(NEWLINE);
        csv.append(getQuoteHeader()).append(NEWLINE);
        csv.append(getQuoteRow(quote)).append(NEWLINE);
        csv.append(String.format("\"Grand Total\",,,,,\"%s\",\"%s\",\"%s\",,", formatAmount(quote.getValueExclVat()), formatAmount(quote.getVatAmount()), formatAmount(quote.getValueInclVat()))).append(NEWLINE);
        return csv.toString();
    }

    private String getQuoteHeader() {
        return "\"Date\",\"Expiry Date\",\"Document No.\",\"Customer Ref.\",\"Customer\",\"Exclusive\",\"VAT\",\"Total\",\"Sales Rep\",\"Status\"";
    }

    private String getQuoteRow(Quote quote) {
        String clientName = quote.getClient() != null ? quote.getClient() : getClientNameById(quote.getClientId());
        String customerRef = getRfqReference(quote.getRfqId());
        String date = formatDate(quote.getQuoteDate());
        String expiryDate = formatDate(quote.getValidUntilDate());
        String status = quote.getQuoteStatus() != null ? quote.getQuoteStatus().toString() : "DRAFT";

        return String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                date, expiryDate, quote.getQuoteNumber() != null ? quote.getQuoteNumber() : "",
                customerRef, escapeQuotes(clientName), formatAmount(quote.getValueExclVat()),
                formatAmount(quote.getVatAmount()), formatAmount(quote.getValueInclVat()), "", status);
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
                "0.00");
    }

    private String getClientNameById(Long clientId) {
        if (clientId == null) return "Unknown";
        Optional<Client> clientOpt = clientRepository.findById(clientId);
        return clientOpt.map(Client::getCompanyName).orElse("Unknown");
    }

    private String getRfqReference(Long rfqId) {
        if (rfqId == null || rfqRepository == null) return "";
        Optional<RFQ> rfqOpt = rfqRepository.findById(rfqId);
        return rfqOpt.map(RFQ::getJobNo).orElse("");
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_FORMAT);
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

    private String escapeQuotes(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    /**
     * Export a single RFQ to Sage Pastel CSV format.
     */
    public String exportRfqToCsv(Long rfqId) {
        RFQ rfq = rfqRepository.findById(rfqId)
                .orElseThrow(() -> new RuntimeException("RFQ not found: " + rfqId));

        StringWriter writer = new StringWriter();
        writer.write(CSV_SEPARATOR);
        writer.write("\n");
        writer.write(getRfqCsvHeader());
        writer.write("\n");
        writer.write(formatRfqRow(rfq));

        return writer.toString();
    }

    /**
     * Export multiple RFQs to CSV.
     */
    public String exportRfqsToCsv(List<Long> rfqIds) {
        List<RFQ> rfqs = rfqRepository.findAllById(rfqIds);

        StringWriter writer = new StringWriter();
        writer.write(CSV_SEPARATOR);
        writer.write("\n");
        writer.write(getRfqCsvHeader());

        for (RFQ rfq : rfqs) {
            writer.write("\n");
            writer.write(formatRfqRow(rfq));
        }

        return writer.toString();
    }

    /**
     * Export all RFQs to CSV (simplified - no status filter since repository doesn't support it).
     */
    public String exportAllRfqsToCsv() {
        List<RFQ> rfqs = rfqRepository.findAll();

        StringWriter writer = new StringWriter();
        writer.write(CSV_SEPARATOR);
        writer.write("\n");
        writer.write(getRfqCsvHeader());

        for (RFQ rfq : rfqs) {
            writer.write("\n");
            writer.write(formatRfqRow(rfq));
        }

        return writer.toString();
    }

    /**
     * CSV Header for RFQ export - matches actual RFQ entity fields.
     */
    private String getRfqCsvHeader() {
        return String.join(",",
                "\"Job No\"",
                "\"Request Date\"",
                "\"Required Date\"",
                "\"Client Code\"",
                "\"Client Name\"",
                "\"Contact Person\"",
                "\"Contact Email\"",
                "\"Contact Phone\"",
                "\"Department\"",
                "\"Operating Entity\"",
                "\"Description\"",
                "\"Status\"",
                "\"Priority\"",
                "\"Estimated Value\"",
                "\"Special Requirements\"",
                "\"Assigned To\"",
                "\"Notes\"",
                "\"Created At\""
        );
    }

    /**
     * Format single RFQ as CSV row - uses actual RFQ entity getters.
     */
    private String formatRfqRow(RFQ rfq) {
        // Fetch client details using clientId
        Client client = null;
        if (rfq.getClientId() != null) {
            Optional<Client> clientOpt = clientRepository.findById(rfq.getClientId());
            if (clientOpt.isPresent()) {
                client = clientOpt.get();
            }
        }

        return String.join(",",
                formatCsvField(rfq.getJobNo()),
                formatCsvField(rfq.getRequestDate() != null ? rfq.getRequestDate().format(DATE_FORMAT) : ""),
                formatCsvField(rfq.getRequiredDate() != null ? rfq.getRequiredDate().format(DATE_FORMAT) : ""),
                formatCsvField(client != null ? client.getClientCode() : ""),
                formatCsvField(client != null ? client.getCompanyName() : ""),
                formatCsvField(rfq.getContactPerson()),
                formatCsvField(rfq.getContactEmail()),
                formatCsvField(rfq.getContactPhone()),
                formatCsvField(rfq.getDepartment()),
                formatCsvField(rfq.getOperatingEntity()),
                formatCsvField(rfq.getDescription()),
                formatCsvField(rfq.getStatus() != null ? rfq.getStatus().toString() : ""),
                formatCsvField(rfq.getPriority() != null ? rfq.getPriority().toString() : ""),
                formatCsvField(rfq.getEstimatedValue() != null ? String.format("%.2f", rfq.getEstimatedValue()) : "0.00"),
                formatCsvField(rfq.getSpecialRequirements()),
                formatCsvField(rfq.getAssignedTo()),
                formatCsvField(rfq.getNotes()),
                formatCsvField(rfq.getCreatedAt() != null ? rfq.getCreatedAt().format(DATE_FORMAT) : "")
        );
    }

    /**
     * Format field for CSV with proper quoting and escaping.
     */
    private String formatCsvField(String value) {
        if (value == null || value.isEmpty()) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String formatCsvField(Object value) {
        if (value == null) {
            return "\"\"";
        }
        return formatCsvField(value.toString());
    }
}
