package com.erha.ops.service;

import com.erha.ops.entity.Job;
import com.erha.ops.entity.JobTask;
import com.erha.ops.repository.JobRepository;
import com.erha.ops.repository.JobTaskRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class JobCardPdfService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobTaskRepository jobTaskRepository;

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font PHASE1_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.GREEN);
    private static final Font PHASE2_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.ORANGE);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 8);
    private static final Font ITALIC_GRAY = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.GRAY);

    public byte[] generateJobCardPdf(Long jobId) throws Exception {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        List<JobTask> tasks = jobTaskRepository.findByJobIdOrderBySequenceNumber(jobId);

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);

        document.open();

        addHeader(document);
        addJobInformation(document, job);
        addJobDescription(document, job);
        addWorkType(document, job);
        addAttachedDocuments(document);
        addAssignedTasks(document, tasks);
        addDailyTimesheet(document);
        addQualityHoldingPoints(document);
        addDeliveryInformation(document, job);
        addPhaseLegend(document);

        document.close();
        return baos.toByteArray();
    }

    private void addHeader(Document document) throws DocumentException {
        Paragraph title = new Paragraph("ERHA OPERATIONS MANAGEMENT SYSTEM", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("Digital Job Card - Phase 1 (Available) + Phase 2 (Roadmap)", NORMAL_FONT);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(10);
        document.add(subtitle);

        Paragraph line = new Paragraph("________________________________________________________________________________", SMALL_FONT);
        document.add(line);
        document.add(Chunk.NEWLINE);
    }

    private void addJobInformation(Document document, Job job) throws DocumentException {
        Paragraph heading = new Paragraph();
        heading.add(new Chunk("JOB INFORMATION [PHASE 1 ✓]", HEADER_FONT));
        document.add(heading);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        addCellBold(table, "Job Number:");
        addCell(table, job.getJobNumber());
        addCellBold(table, "Job Card ID:");
        addCell(table, String.valueOf(job.getJobId()));

        addCellBold(table, "RFQ:");
        addCell(table, job.getRfqId() != null ? String.valueOf(job.getRfqId()) : "N/A");
        addCellBold(table, "Site Req:");
        addCell(table, job.getOrderNumber() != null ? job.getOrderNumber() : "N/A");

        addCellBold(table, "Client:");
        addCell(table, "Client ID: " + job.getClientId());
        addCellBold(table, "Department:");
        addCell(table, job.getDepartment());

        addCellBold(table, "Order Date:");
        addCell(table, formatLocalDate(job.getOrderReceivedDate()));
        addCellBold(table, "Due Date:");
        addCell(table, formatLocalDate(job.getExpectedDeliveryDate()));

        document.add(table);
    }

    private void addJobDescription(Document document, Job job) throws DocumentException {
        document.add(new Paragraph("JOB DESCRIPTION [PHASE 1 ✓]", HEADER_FONT));
        Paragraph desc = new Paragraph(job.getDescription(), NORMAL_FONT);
        desc.setSpacingBefore(5);
        desc.setSpacingAfter(10);
        document.add(desc);
    }

    private void addWorkType(Document document, Job job) throws DocumentException {
        document.add(new Paragraph("WORK TYPE SELECTION", HEADER_FONT));

        Paragraph phase1 = new Paragraph();
        phase1.add(new Chunk("Phase 1: ", PHASE1_FONT));
        phase1.add(new Chunk("☑ MANUFACTURE  ☑ SERVICE  ☑ REPAIR", NORMAL_FONT));
        phase1.setSpacingBefore(5);
        document.add(phase1);

        Paragraph phase2 = new Paragraph();
        phase2.add(new Chunk("Phase 2: ", PHASE2_FONT));
        phase2.add(new Chunk("☐ MODIFY  ☐ INSTALLATION  ☐ SANDBLAST  ☐ PAINT  ☐ CUT  ☐ OTHER", ITALIC_GRAY));
        phase2.setSpacingAfter(10);
        document.add(phase2);
    }

    private void addAttachedDocuments(Document document) throws DocumentException {
        Paragraph heading = new Paragraph();
        heading.add(new Chunk("ATTACHED DOCUMENTS [PHASE 2 ⚠]", PHASE2_FONT));
        document.add(heading);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        addCellBold(table, "Drawing Number:");
        addCellItalicGray(table, "[Multi-file attachments - Phase 2]");

        addCellBold(table, "Service Schedule/QCP:");
        addCellItalicGray(table, "[Document management - Phase 2]");

        addCellBold(table, "Drawings/Sketches:");
        addCellItalicGray(table, "[CAD file support - Phase 2]");

        addCellBold(table, "Quantity:");
        addCellItalicGray(table, "[Item quantity tracking - Phase 2]");

        document.add(table);
    }

    private void addAssignedTasks(Document document, List<JobTask> tasks) throws DocumentException {
        document.add(new Paragraph("ASSIGNED TASKS [PHASE 1 ✓]", HEADER_FONT));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new int[]{5, 65, 15, 15});
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        addHeaderCell(table, "#");
        addHeaderCell(table, "Task Description");
        addHeaderCell(table, "Status");
        addHeaderCell(table, "Completed");

        if (tasks.isEmpty()) {
            String[][] sampleTasks = {
                    {"1", "Receive unit and create tag/documentation", "Pending", "☐"},
                    {"2", "Disassemble valve and clean components", "Pending", "☐"},
                    {"3", "Inspect all components for wear/damage", "Pending", "☐"},
                    {"4", "Replace internals (seats, seals, gaskets)", "Pending", "☐"},
                    {"5", "Reassemble and perform pressure test", "Pending", "☐"},
                    {"6", "Paint, label and prepare for dispatch", "Pending", "☐"}
            };
            for (String[] task : sampleTasks) {
                for (String cell : task) {
                    addCell(table, cell);
                }
            }
        } else {
            for (JobTask task : tasks) {
                addCell(table, String.valueOf(task.getSequenceNumber()));
                addCell(table, task.getDescription());
                addCell(table, task.getCompleted() != null && task.getCompleted() ? "Completed" : "Pending");
                addCell(table, task.getCompleted() != null && task.getCompleted() ? "☑" : "☐");
            }
        }

        document.add(table);
    }

    private void addDailyTimesheet(Document document) throws DocumentException {
        Paragraph heading = new Paragraph();
        heading.add(new Chunk("DAILY TIMESHEET [PHASE 2 ⚠]", PHASE2_FONT));
        document.add(heading);

        PdfPTable table = new PdfPTable(10);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(5);

        String[] headers = {"Date", "Desc", "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN", "TOTAL"};
        for (String header : headers) {
            addHeaderCell(table, header);
        }

        addCellItalicGray(table, "");
        addCellItalicGray(table, "");
        for (int i = 0; i < 8; i++) {
            addCellItalicGray(table, "NT/OT");
        }

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 10; col++) {
                addCellItalicGray(table, "-");
            }
        }

        document.add(table);

        Paragraph note = new Paragraph("Note: Daily hour tracking (NT/OT) will be available in Phase 2", ITALIC_GRAY);
        note.setSpacingAfter(10);
        document.add(note);
    }

    private void addQualityHoldingPoints(Document document) throws DocumentException {
        document.add(new Paragraph("QUALITY HOLDING POINTS [PHASE 1 ✓]", HEADER_FONT));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new int[]{5, 75, 20});
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        addHeaderCell(table, "#");
        addHeaderCell(table, "Quality Check Description");
        addHeaderCell(table, "Pass/Fail");

        String[][] points = {
                {"1", "Mark out all material & check prior to cutting", "☐ Pass  ☐ Fail"},
                {"2", "Cut material, deburr holes, remove sharp edges", "☐ Pass  ☐ Fail"},
                {"3", "Assembly & inspect prior to welding", "☐ Pass  ☐ Fail"},
                {"4", "Complete welding as per WPS", "☐ Pass  ☐ Fail"},
                {"5", "Pressure test on water cooled unit if applicable", "☐ Pass  ☐ Fail"},
                {"6", "Clean spatter and ensure NO sharp edges", "☐ Pass  ☐ Fail"},
                {"7", "100% dimensional & visual inspection pre-paint", "☐ Pass  ☐ Fail"},
                {"8", "Stamp and paint as required", "☐ Pass  ☐ Fail"},
                {"9", "Final Inspection - Ready for Delivery", "☐ Pass  ☐ Fail"}
        };

        for (String[] point : points) {
            addCell(table, point[0]);
            addCell(table, point[1]);
            addCell(table, point[2]);
        }

        document.add(table);
    }

    private void addDeliveryInformation(Document document, Job job) throws DocumentException {
        document.add(new Paragraph("DELIVERY INFORMATION [PHASE 1 ✓]", HEADER_FONT));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        addCellBold(table, "Delivery Type:");
        addCell(table, "INTERNAL");
        addCellBold(table, "Due Date:");
        addCell(table, formatLocalDate(job.getExpectedDeliveryDate()));

        addCellBold(table, "Job Destination:");
        addCell(table, "FOR DELIVERY");
        addCellBold(table, "Status:");
        addCell(table, job.getStatus() != null ? job.getStatus().toString() : "N/A");

        PdfPCell cell1 = new PdfPCell(new Phrase("Logistics (Phase 2):", PHASE2_FONT));
        table.addCell(cell1);
        PdfPCell cell2 = new PdfPCell(new Phrase("[Driver assignment]", ITALIC_GRAY));
        table.addCell(cell2);
        PdfPCell cell3 = new PdfPCell(new Phrase("Delivery Note (Phase 2):", PHASE2_FONT));
        table.addCell(cell3);
        PdfPCell cell4 = new PdfPCell(new Phrase("[Digital delivery note]", ITALIC_GRAY));
        table.addCell(cell4);

        document.add(table);
    }

    private void addPhaseLegend(Document document) throws DocumentException {
        Paragraph line = new Paragraph("________________________________________________________________________________", SMALL_FONT);
        document.add(line);
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("PHASE DELIVERY LEGEND", HEADER_FONT));

        Paragraph phase1 = new Paragraph();
        phase1.add(new Chunk("✓ PHASE 1 - AVAILABLE NOW: ", PHASE1_FONT));
        phase1.add(new Chunk("Job tracking, task management, quality checks, workflow, delivery tracking", NORMAL_FONT));
        phase1.setSpacingBefore(5);
        document.add(phase1);

        Paragraph phase2 = new Paragraph();
        phase2.add(new Chunk("⚠ PHASE 2 - NEXT MILESTONE: ", PHASE2_FONT));
        phase2.add(new Chunk("Daily timesheets (NT/OT), multi-file attachments, quantity tracking, logistics", NORMAL_FONT));
        phase2.setSpacingAfter(10);
        document.add(phase2);

        Paragraph line2 = new Paragraph("________________________________________________________________________________", SMALL_FONT);
        document.add(line2);

        Paragraph footer = new Paragraph("ERHA Operations Management System | PUSH AI Foundation | Generated: " +
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), SMALL_FONT);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private void addCellBold(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addCellItalicGray(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, ITALIC_GRAY));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font whiteFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, whiteFont));
        cell.setBackgroundColor(new BaseColor(68, 114, 196));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private String formatLocalDate(LocalDate date) {
        if (date == null) return "N/A";
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}