package com.erha.ops.service;

import com.erha.ops.entity.CasualLaborEntry;
import com.erha.ops.entity.CasualLaborEntry.PaymentStatus;
import com.erha.ops.entity.CasualLaborEntry.WorkStatus;
import com.erha.ops.entity.CasualWorker;
import com.erha.ops.entity.CasualWorker.CasualWorkerStatus;
import com.erha.ops.entity.Job;
import com.erha.ops.repository.CasualLaborEntryRepository;
import com.erha.ops.repository.CasualWorkerRepository;
import com.erha.ops.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CasualLaborService {

    private static final Logger logger = LoggerFactory.getLogger(CasualLaborService.class);

    @Autowired
    private CasualWorkerRepository workerRepository;

    @Autowired
    private CasualLaborEntryRepository entryRepository;

    @Autowired
    private JobRepository jobRepository;

    public List<CasualWorker> getAllWorkers() {
        return workerRepository.findAllByOrderByNameAsc();
    }

    public List<CasualWorker> getActiveWorkers() {
        return workerRepository.findAllActive();
    }

    public Optional<CasualWorker> getWorkerById(Long id) {
        return workerRepository.findById(id);
    }

    @Transactional
    public CasualWorker createWorker(CasualWorker worker) {
        if (worker.getClockNumber() != null && workerRepository.existsByClockNumber(worker.getClockNumber())) {
            throw new RuntimeException("Clock number already exists: " + worker.getClockNumber());
        }
        return workerRepository.save(worker);
    }

    @Transactional
    public CasualWorker updateWorker(Long id, CasualWorker updated) {
        CasualWorker existing = workerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Worker not found: " + id));
        existing.setName(updated.getName());
        existing.setClockNumber(updated.getClockNumber());
        existing.setContactDetails(updated.getContactDetails());
        existing.setBankName(updated.getBankName());
        existing.setBankAccount(updated.getBankAccount());
        existing.setBranchCode(updated.getBranchCode());
        existing.setStatus(updated.getStatus());
        existing.setNotes(updated.getNotes());
        return workerRepository.save(existing);
    }

    @Transactional
    public void deleteWorker(Long id) {
        workerRepository.deleteById(id);
    }

    public long countActiveWorkers() {
        return workerRepository.countActive();
    }

    public List<CasualLaborEntry> getAllEntries() {
        return entryRepository.findAllWithDetails();
    }

    public List<CasualLaborEntry> getEntriesByJob(Long jobId) {
        return entryRepository.findByJobIdWithDetails(jobId);
    }

    public List<CasualLaborEntry> getEntriesByWorker(Long workerId) {
        return entryRepository.findByCasualWorkerId(workerId);
    }

    public List<CasualLaborEntry> getPendingPayments() {
        return entryRepository.findPendingPayments();
    }

    public Optional<CasualLaborEntry> getEntryById(Long id) {
        return entryRepository.findById(id);
    }

    @Transactional
    public CasualLaborEntry createEntry(CasualLaborEntry entry, Long workerId, Long jobId) {
        if (workerId != null) {
            CasualWorker worker = workerRepository.findById(workerId)
                    .orElseThrow(() -> new RuntimeException("Worker not found: " + workerId));
            entry.setCasualWorker(worker);
        }
        if (jobId != null) {
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
            entry.setJob(job);
        }
        return entryRepository.save(entry);
    }

    @Transactional
    public CasualLaborEntry updateEntry(Long id, CasualLaborEntry updated, Long workerId, Long jobId) {
        CasualLaborEntry existing = entryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entry not found: " + id));
        existing.setRequestNumber(updated.getRequestNumber());
        existing.setOriginator(updated.getOriginator());
        existing.setPlace(updated.getPlace());
        existing.setWorkItem(updated.getWorkItem());
        existing.setRequiredDates(updated.getRequiredDates());
        existing.setDateReceived(updated.getDateReceived());
        existing.setPayMethod(updated.getPayMethod());
        existing.setPayDate(updated.getPayDate());
        existing.setPaymentAmount(updated.getPaymentAmount());
        existing.setPaymentStatus(updated.getPaymentStatus());
        existing.setWorkStatus(updated.getWorkStatus());
        existing.setNotes(updated.getNotes());
        if (workerId != null) {
            CasualWorker worker = workerRepository.findById(workerId)
                    .orElseThrow(() -> new RuntimeException("Worker not found: " + workerId));
            existing.setCasualWorker(worker);
        }
        if (jobId != null) {
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
            existing.setJob(job);
        }
        return entryRepository.save(existing);
    }

    @Transactional
    public CasualLaborEntry markAsPaid(Long entryId, LocalDate payDate) {
        CasualLaborEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new RuntimeException("Entry not found: " + entryId));
        entry.setPaymentStatus(PaymentStatus.PAID);
        entry.setPayDate(payDate != null ? payDate : LocalDate.now());
        return entryRepository.save(entry);
    }

    @Transactional
    public void deleteEntry(Long id) {
        entryRepository.deleteById(id);
    }

    public Map<String, Object> getSummaryStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalWorkers", workerRepository.count());
        stats.put("activeWorkers", workerRepository.countActive());
        stats.put("totalEntries", entryRepository.count());
        stats.put("pendingPayments", entryRepository.findPendingPayments().size());
        BigDecimal totalPaid = entryRepository.getTotalPaidAmount();
        BigDecimal totalPending = entryRepository.getTotalPendingAmount();
        stats.put("totalPaid", totalPaid != null ? totalPaid : BigDecimal.ZERO);
        stats.put("totalPending", totalPending != null ? totalPending : BigDecimal.ZERO);
        return stats;
    }

    public BigDecimal getLaborCostForJob(Long jobId) {
        BigDecimal cost = entryRepository.getTotalLaborCostForJob(jobId);
        return cost != null ? cost : BigDecimal.ZERO;
    }

    // NO @Transactional - we handle each row independently
    public Map<String, Object> importFromExcel(MultipartFile file) throws Exception {
        logger.info("=== STARTING IMPORT FROM: {} ===", file.getOriginalFilename());
        
        int workersCreated = 0;
        int workersUpdated = 0;
        int entriesCreated = 0;
        List<String> errors = new ArrayList<>();
        Set<String> missingJobs = new HashSet<>();

        // Context for grouped rows (header row info carries to detail rows)
        LocalDate currentDateReceived = null;
        String currentReqNo = null;
        String currentJobNo = null;
        String currentOriginator = null;
        String currentPlace = null;
        String currentWorkItem = null;
        String currentRequiredDates = null;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int totalRows = sheet.getLastRowNum();
            logger.info("Processing {} rows", totalRows);

            for (int rowNum = 1; rowNum <= totalRows; rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;

                try {
                    // Check if this is a HEADER row (has date in col 0 and req no in col 1)
                    LocalDate dateReceived = getCellDateValue(row, 0);
                    String reqNo = getCellStringValue(row, 1);
                    
                    if (dateReceived != null || (reqNo != null && reqNo.contains("-"))) {
                        // This is a header row - capture context
                        if (dateReceived != null) currentDateReceived = dateReceived;
                        if (reqNo != null && !reqNo.isEmpty()) currentReqNo = reqNo;
                        
                        String jobNo = getCellStringValue(row, 2);
                        if (jobNo != null && !jobNo.isEmpty()) currentJobNo = jobNo;
                        
                        String originator = getCellStringValue(row, 3);
                        if (originator != null && !originator.isEmpty()) currentOriginator = originator;
                        
                        String place = getCellStringValue(row, 4);
                        if (place != null && !place.isEmpty()) currentPlace = place;
                        
                        String workItem = getCellStringValue(row, 5);
                        if (workItem != null && !workItem.isEmpty()) currentWorkItem = workItem;
                        
                        String reqDates = getCellStringValue(row, 7);
                        if (reqDates != null && !reqDates.isEmpty()) currentRequiredDates = reqDates;
                        
                        logger.debug("Header row {}: ReqNo={}, JobNo={}, Originator={}", 
                            rowNum + 1, currentReqNo, currentJobNo, currentOriginator);
                        continue;
                    }

                    // Check if this is a DETAIL row (has worker name in col 6)
                    String workerName = getCellStringValue(row, 6);
                    if (workerName == null || workerName.trim().isEmpty()) {
                        continue; // Empty or separator row
                    }

                    logger.debug("Detail row {}: Worker={}", rowNum + 1, workerName);

                    // Get or create worker
                    String clockNo = getCellStringValue(row, 9);
                    CasualWorker worker = findOrCreateWorker(workerName.trim(), clockNo);
                    if (worker.getId() == null) {
                        workersCreated++;
                    } else {
                        workersUpdated++;
                    }

                    // Try to find job (optional - don't fail if not found)
                    Job job = null;
                    if (currentJobNo != null && !currentJobNo.isEmpty() 
                            && !currentJobNo.equalsIgnoreCase("STORES") 
                            && !currentJobNo.equalsIgnoreCase("GENERAL")) {
                        job = findJobByNumber(currentJobNo);
                        if (job == null) {
                            missingJobs.add(currentJobNo);
                        }
                    }

                    // Create entry
                    CasualLaborEntry entry = new CasualLaborEntry();
                    entry.setRequestNumber(currentReqNo);
                    entry.setDateReceived(currentDateReceived);
                    entry.setJob(job);  // Can be null
                    entry.setCasualWorker(worker);
                    entry.setOriginator(currentOriginator);
                    entry.setPlace(currentPlace);
                    entry.setWorkItem(currentWorkItem);
                    entry.setRequiredDates(currentRequiredDates);

                    // Payment details from columns 12-14
                    String payMethodStr = getCellStringValue(row, 12);
                    if (payMethodStr != null) {
                        if (payMethodStr.equalsIgnoreCase("EFT")) {
                            entry.setPayMethod(CasualLaborEntry.PayMethod.EFT);
                        } else if (payMethodStr.equalsIgnoreCase("CASH")) {
                            entry.setPayMethod(CasualLaborEntry.PayMethod.CASH);
                        }
                    }

                    LocalDate payDate = getCellDateValue(row, 13);
                    BigDecimal payAmount = getCellNumericValue(row, 14);
                    entry.setPayDate(payDate);
                    entry.setPaymentAmount(payAmount);

                    // Set status based on payment info
                    if (payDate != null && payAmount != null && payAmount.compareTo(BigDecimal.ZERO) > 0) {
                        entry.setPaymentStatus(PaymentStatus.PAID);
                        entry.setWorkStatus(WorkStatus.COMPLETED);
                    } else if (payAmount != null && payAmount.compareTo(BigDecimal.ZERO) > 0) {
                        entry.setPaymentStatus(PaymentStatus.PENDING);
                        entry.setWorkStatus(WorkStatus.COMPLETED);
                    } else {
                        entry.setPaymentStatus(PaymentStatus.PENDING);
                        entry.setWorkStatus(WorkStatus.ASSIGNED);
                    }

                    // Save entry
                    saveEntry(entry);
                    entriesCreated++;

                    if (entriesCreated % 25 == 0) {
                        logger.info("Progress: {} entries created...", entriesCreated);
                    }

                } catch (Exception e) {
                    String error = "Row " + (rowNum + 1) + ": " + e.getMessage();
                    errors.add(error);
                    logger.warn(error);
                }
            }
        }

        logger.info("=== IMPORT COMPLETE ===");
        logger.info("Workers created: {}, updated: {}", workersCreated, workersUpdated);
        logger.info("Entries created: {}", entriesCreated);
        logger.info("Errors: {}", errors.size());
        if (!missingJobs.isEmpty()) {
            logger.info("Missing jobs (not linked): {}", missingJobs);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("workersCreated", workersCreated);
        result.put("workersUpdated", workersUpdated);
        result.put("entriesCreated", entriesCreated);
        result.put("errors", errors.size() > 20 ? errors.subList(0, 20) : errors);
        result.put("errorCount", errors.size());
        result.put("missingJobs", new ArrayList<>(missingJobs));
        result.put("success", true);
        return result;
    }

    @Transactional
    protected CasualWorker findOrCreateWorker(String name, String clockNo) {
        // Try to find by clock number first
        if (clockNo != null && !clockNo.trim().isEmpty()) {
            Optional<CasualWorker> byClockNo = workerRepository.findByClockNumber(clockNo.trim());
            if (byClockNo.isPresent()) {
                return byClockNo.get();
            }
        }
        
        // Try to find by name
        List<CasualWorker> byName = workerRepository.findByNameContainingIgnoreCase(name);
        if (!byName.isEmpty()) {
            CasualWorker existing = byName.get(0);
            // Update clock number if we have one and they don't
            if (clockNo != null && !clockNo.trim().isEmpty() && 
                (existing.getClockNumber() == null || existing.getClockNumber().isEmpty())) {
                existing.setClockNumber(clockNo.trim());
                return workerRepository.save(existing);
            }
            return existing;
        }
        
        // Create new worker
        CasualWorker worker = new CasualWorker();
        worker.setName(name);
        if (clockNo != null && !clockNo.trim().isEmpty()) {
            worker.setClockNumber(clockNo.trim());
        }
        worker.setStatus(CasualWorkerStatus.ACTIVE);
        return workerRepository.save(worker);
    }

    private Job findJobByNumber(String jobNo) {
        if (jobNo == null || jobNo.isEmpty()) return null;
        
        // Try exact match first
        Optional<Job> job = jobRepository.findByJobNumber(jobNo);
        if (job.isPresent()) return job.get();
        
        // Try with different prefix (25-XXX -> convert to match format in DB)
        // The DB might have jobs like "24-509" while Excel has "25-009"
        return null;
    }

    @Transactional
    protected void saveEntry(CasualLaborEntry entry) {
        entryRepository.save(entry);
    }

    private String getCellStringValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        try {
            switch (cell.getCellType()) {
                case STRING: return cell.getStringCellValue().trim();
                case NUMERIC: 
                    double val = cell.getNumericCellValue();
                    if (val == Math.floor(val)) {
                        return String.valueOf((long) val);
                    }
                    return String.valueOf(val);
                case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
                default: return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate getCellDateValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {
                String dateStr = cell.getStringCellValue().trim();
                if (dateStr.isEmpty()) return null;
                
                String[] formats = {"dd/MM/yyyy", "d/MM/yyyy", "dd/M/yyyy", "d/M/yyyy", "yyyy-MM-dd", "yyyy/MM/dd"};
                for (String format : formats) {
                    try {
                        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(format));
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private BigDecimal getCellNumericValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue()).setScale(2, java.math.RoundingMode.HALF_UP);
            } else if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim().replaceAll("[^0-9.]", "");
                if (!val.isEmpty()) return new BigDecimal(val).setScale(2, java.math.RoundingMode.HALF_UP);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
}