package com.erha.ops.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${erha.mail.from:noreply@erha.co.za}")
    private String fromEmail;

    @Value("${erha.mail.manager-email:manager@erha.co.za}")
    private String managerEmail;

    @Value("${erha.mail.manager-name:ERHA Manager}")
    private String managerName;

    @Value("${erha.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Send quote approval request email to manager
     */
    public void sendApprovalRequestEmail(
            String quoteNumber,
            Long quoteId,
            String clientName,
            BigDecimal quoteValue,
            String pin,
            LocalDateTime expiresAt,
            String submittedBy
    ) {
        if (mailSender == null) {
            logger.warn("Email not configured - would send approval request for quote {} to {}", quoteNumber, managerEmail);
            logger.info("PIN: {} | Expires: {}", pin, expiresAt);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(managerEmail);
            helper.setSubject("Quote Approval Required: " + quoteNumber + " - " + formatCurrency(quoteValue));

            String htmlContent = buildApprovalEmailHtml(
                    quoteNumber, quoteId, clientName, quoteValue, pin, expiresAt, submittedBy
            );

            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Approval request email sent to {} for quote {}", managerEmail, quoteNumber);

        } catch (MessagingException e) {
            logger.error("Failed to send approval email for quote {}: {}", quoteNumber, e.getMessage());
        }
    }

    /**
     * Send approval confirmation email
     */
    public void sendApprovalConfirmationEmail(
            String quoteNumber,
            String clientName,
            BigDecimal quoteValue,
            String approvedBy,
            String recipientEmail
    ) {
        if (mailSender == null) {
            logger.warn("Email not configured - would send approval confirmation for quote {}", quoteNumber);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("Quote Approved: " + quoteNumber);

            String htmlContent = buildConfirmationEmailHtml(quoteNumber, clientName, quoteValue, approvedBy);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Approval confirmation email sent to {} for quote {}", recipientEmail, quoteNumber);

        } catch (MessagingException e) {
            logger.error("Failed to send confirmation email for quote {}: {}", quoteNumber, e.getMessage());
        }
    }

    private String buildApprovalEmailHtml(
            String quoteNumber,
            Long quoteId,
            String clientName,
            BigDecimal quoteValue,
            String pin,
            LocalDateTime expiresAt,
            String submittedBy
    ) {
        String approvalUrl = frontendUrl + "/quotes/approve?quote=" + quoteNumber;
        String formattedExpiry = expiresAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
        String formattedValue = formatCurrency(quoteValue);
        String safeClientName = clientName != null ? clientName : "N/A";
        String safeSubmittedBy = submittedBy != null ? submittedBy : "System";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset=\"UTF-8\"></head>");
        html.append("<body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;\">");

        // Header
        html.append("<div style=\"background: linear-gradient(135deg, #1a5f2a 0%, #2d8a3e 100%); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;\">");
        html.append("<h1 style=\"color: white; margin: 0; font-size: 24px;\">Quote Approval Required</h1>");
        html.append("</div>");

        // Body
        html.append("<div style=\"background: #f9f9f9; padding: 30px; border: 1px solid #ddd; border-top: none;\">");
        html.append("<p style=\"margin-top: 0;\">Dear ").append(managerName).append(",</p>");
        html.append("<p>A quote has been submitted for your approval:</p>");

        // Quote details table
        html.append("<div style=\"background: white; border: 1px solid #e0e0e0; border-radius: 8px; padding: 20px; margin: 20px 0;\">");
        html.append("<table style=\"width: 100%; border-collapse: collapse;\">");
        html.append("<tr><td style=\"padding: 8px 0; color: #666;\">Quote Number:</td>");
        html.append("<td style=\"padding: 8px 0; font-weight: bold; text-align: right;\">").append(quoteNumber).append("</td></tr>");
        html.append("<tr><td style=\"padding: 8px 0; color: #666;\">Client:</td>");
        html.append("<td style=\"padding: 8px 0; font-weight: bold; text-align: right;\">").append(safeClientName).append("</td></tr>");
        html.append("<tr><td style=\"padding: 8px 0; color: #666;\">Value (Incl VAT):</td>");
        html.append("<td style=\"padding: 8px 0; font-weight: bold; text-align: right; color: #1a5f2a; font-size: 18px;\">").append(formattedValue).append("</td></tr>");
        html.append("<tr><td style=\"padding: 8px 0; color: #666;\">Submitted By:</td>");
        html.append("<td style=\"padding: 8px 0; text-align: right;\">").append(safeSubmittedBy).append("</td></tr>");
        html.append("</table></div>");

        // PIN Box
        html.append("<div style=\"background: #fff3cd; border: 2px solid #ffc107; border-radius: 8px; padding: 25px; margin: 25px 0; text-align: center;\">");
        html.append("<p style=\"margin: 0 0 10px 0; color: #856404; font-weight: bold;\">YOUR APPROVAL PIN</p>");
        html.append("<div style=\"font-size: 42px; font-weight: bold; letter-spacing: 8px; color: #1a5f2a; font-family: monospace;\">");
        html.append(pin);
        html.append("</div>");
        html.append("<p style=\"margin: 15px 0 0 0; color: #856404; font-size: 14px;\">Expires: ").append(formattedExpiry).append("</p>");
        html.append("</div>");

        // Approve button
        html.append("<div style=\"text-align: center; margin: 30px 0;\">");
        html.append("<a href=\"").append(approvalUrl).append("\" style=\"display: inline-block; background: #1a5f2a; color: white; padding: 15px 40px; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px;\">");
        html.append("Approve Quote Now</a></div>");

        // Instructions
        html.append("<div style=\"background: #e8f5e9; border-radius: 8px; padding: 15px; margin-top: 20px;\">");
        html.append("<p style=\"margin: 0; font-size: 14px; color: #2e7d32;\">");
        html.append("<strong>How to approve:</strong><br>");
        html.append("1. Click the button above or go to the ERHA OPS system<br>");
        html.append("2. Navigate to Quotes and then Approve Quote<br>");
        html.append("3. Enter the quote number and PIN<br>");
        html.append("4. Click Approve</p></div>");

        html.append("<hr style=\"border: none; border-top: 1px solid #ddd; margin: 30px 0;\">");
        html.append("<p style=\"color: #666; font-size: 12px; margin-bottom: 0;\">");
        html.append("This is an automated message from ERHA Operations Management System.<br>");
        html.append("If you did not request this approval, please contact the system administrator.</p>");
        html.append("</div>");

        // Footer
        html.append("<div style=\"background: #333; color: white; padding: 20px; text-align: center; border-radius: 0 0 10px 10px; font-size: 12px;\">");
        html.append("ERHA Fabrication and Construction | ERHA Steel Supplies<br>");
        html.append("Powered by PUSH AI Foundation</div>");

        html.append("</body></html>");

        return html.toString();
    }

    private String buildConfirmationEmailHtml(
            String quoteNumber,
            String clientName,
            BigDecimal quoteValue,
            String approvedBy
    ) {
        String formattedValue = formatCurrency(quoteValue);
        String safeClientName = clientName != null ? clientName : "N/A";
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset=\"UTF-8\"></head>");
        html.append("<body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;\">");

        // Header
        html.append("<div style=\"background: linear-gradient(135deg, #1a5f2a 0%, #2d8a3e 100%); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;\">");
        html.append("<h1 style=\"color: white; margin: 0;\">Quote Approved</h1>");
        html.append("</div>");

        // Body
        html.append("<div style=\"background: #f9f9f9; padding: 30px; border: 1px solid #ddd; border-top: none;\">");
        html.append("<div style=\"text-align: center; margin-bottom: 20px; font-size: 60px;\">&#9989;</div>");
        html.append("<p style=\"text-align: center; font-size: 18px;\">Quote <strong>").append(quoteNumber).append("</strong> has been approved!</p>");

        // Details table
        html.append("<div style=\"background: white; border: 1px solid #e0e0e0; border-radius: 8px; padding: 20px; margin: 20px 0;\">");
        html.append("<table style=\"width: 100%; border-collapse: collapse;\">");
        html.append("<tr><td style=\"padding: 8px 0; color: #666;\">Client:</td>");
        html.append("<td style=\"padding: 8px 0; font-weight: bold; text-align: right;\">").append(safeClientName).append("</td></tr>");
        html.append("<tr><td style=\"padding: 8px 0; color: #666;\">Value:</td>");
        html.append("<td style=\"padding: 8px 0; font-weight: bold; text-align: right; color: #1a5f2a;\">").append(formattedValue).append("</td></tr>");
        html.append("<tr><td style=\"padding: 8px 0; color: #666;\">Approved By:</td>");
        html.append("<td style=\"padding: 8px 0; text-align: right;\">").append(approvedBy).append("</td></tr>");
        html.append("<tr><td style=\"padding: 8px 0; color: #666;\">Approved At:</td>");
        html.append("<td style=\"padding: 8px 0; text-align: right;\">").append(now).append("</td></tr>");
        html.append("</table></div>");

        html.append("<p style=\"text-align: center;\">You can now send the quote to the client for signature.</p>");
        html.append("</div>");

        // Footer
        html.append("<div style=\"background: #333; color: white; padding: 20px; text-align: center; border-radius: 0 0 10px 10px; font-size: 12px;\">");
        html.append("ERHA Operations Management System</div>");

        html.append("</body></html>");

        return html.toString();
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) return "R 0.00";
        return "R " + String.format("%,.2f", value);
    }
}