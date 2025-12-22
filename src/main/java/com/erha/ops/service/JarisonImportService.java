package com.erha.ops.service;

import com.erha.ops.entity.*;
import com.erha.ops.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class JarisonImportService {

    private static final Logger logger = LoggerFactory.getLogger(JarisonImportService.class);

    @Autowired
    private JarisonBatchImportRepository batchImportRepository;

    @Autowired
    private JarisonMonthlyHoursRepository monthlyHoursRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    public JarisonBatchImport importBatchFile(MultipartFile file, int periodMonth, int periodYear, String importedBy) throws Exception {
        logger.info("Starting Jarison batch import for {}/{}", periodMonth, periodYear);

        Optional<JarisonBatchImport> existing = batchImportRepository.findByPeriodMonthAndPeriodYear(periodMonth, periodYear);
        if (existing.isPresent()) {
            monthlyHoursRepository.deleteAll(monthlyHoursRepository.findByBatchImportId(existing.get().getId()));
            batchImportRepository.delete(existing.get());
            logger.info("Deleted existing import for {}/{}", periodMonth, periodYear);
        }

        JarisonBatchImport batchImport = new JarisonBatchImport();
        batchImport.setFileName(file.getOriginalFilename());
        batchImport.setPeriodMonth(periodMonth);
        batchImport.setPeriodYear(periodYear);
        batchImport.setImportedBy(importedBy);
        batchImport.setImportedAt(LocalDateTime.now());
        batchImport.setStatus(JarisonBatchImport.ImportStatus.PENDING);
        batchImport = batchImportRepository.save(batchImport);

        List<JarisonMonthlyHours> records = parseExcelFile(file.getInputStream(), batchImport);
        
        int matched = 0;
        int unmatched = 0;
        BigDecimal totalHours = BigDecimal.ZERO;

        for (JarisonMonthlyHours record : records) {
            Worker worker = matchWorker(record.getJarisonCode(), record.getEmployeeName());
            if (worker != null) {
                record.setWorker(worker);
                
                if (worker.getJarisonCode() == null || worker.getJarisonCode().isEmpty()) {
                    worker.setJarisonCode(record.getJarisonCode());
                    workerRepository.save(worker);
                }
                
                BigDecimal erhaHours = calculateErhaHours(worker.getId(), periodMonth, periodYear);
                record.setErhaJobHours(erhaHours);
                
                BigDecimal jarisonTotal = record.getTotalHours() != null ? record.getTotalHours() : BigDecimal.ZERO;
                BigDecimal variance = jarisonTotal.subtract(erhaHours);
                record.setVarianceHours(variance);
                
                if (variance.abs().compareTo(new BigDecimal("1")) <= 0) {
                    record.setReconciliationStatus(JarisonMonthlyHours.ReconciliationStatus.MATCHED);
                } else {
                    record.setReconciliationStatus(JarisonMonthlyHours.ReconciliationStatus.VARIANCE);
                }
                
                matched++;
            } else {
                record.setReconciliationStatus(JarisonMonthlyHours.ReconciliationStatus.UNMATCHED);
                unmatched++;
            }
            
            if (record.getTotalHours() != null) {
                totalHours = totalHours.add(record.getTotalHours());
            }
            
            monthlyHoursRepository.save(record);
        }

        batchImport.setTotalEmployees(records.size());
        batchImport.setTotalHours(totalHours);
        batchImport.setMatchedCount(matched);
        batchImport.setUnmatchedCount(unmatched);
        batchImport.setStatus(JarisonBatchImport.ImportStatus.PROCESSED);
        batchImport = batchImportRepository.save(batchImport);

        logger.info("Jarison import complete: {} employees, {} matched, {} unmatched", records.size(), matched, unmatched);
        return batchImport;
    }

    private List<JarisonMonthlyHours> parseExcelFile(InputStream inputStream, JarisonBatchImport batchImport) throws Exception {
        List<JarisonMonthlyHours> records = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            int empCodeCol = 0, nameCol = 1, totalHoursCol = 3;
            int normalHoursCol = 5, ot15Col = 6, ot20Col = 7;

            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;

                Cell empCodeCell = row.getCell(empCodeCol);
                String empCode = getCellStringValue(empCodeCell);
                if (empCode == null || empCode.isEmpty() || empCode.equalsIgnoreCase("POINT EMP.CODE")) continue;

                String empName = getCellStringValue(row.getCell(nameCol));
                if (empName == null || empName.isEmpty()) continue;

                JarisonMonthlyHours record = new JarisonMonthlyHours();
                record.setBatchImport(batchImport);
                record.setJarisonCode(empCode.trim());
                record.setEmployeeName(empName.trim());
                record.setTotalHours(getCellNumericValue(row.getCell(totalHoursCol)));
                record.setNormalHours(getCellNumericValue(row.getCell(normalHoursCol)));
                record.setOtHours15(getCellNumericValue(row.getCell(ot15Col)));
                record.setOtHours20(getCellNumericValue(row.getCell(ot20Col)));

                records.add(record);
            }
        }

        logger.info("Parsed {} employee records from Excel", records.size());
        return records;
    }

    private Worker matchWorker(String jarisonCode, String employeeName) {
        List<Worker> workers = workerRepository.findAll();
        for (Worker w : workers) {
            if (jarisonCode.equals(w.getJarisonCode())) return w;
        }

        String cleanName = employeeName.replace("Mr ", "").replace("Mrs ", "").replace("Miss ", "").replace("Ms ", "").replace("MR ", "").trim();
        String[] parts = cleanName.split(" ");
        if (parts.length >= 2) {
            String initials = parts[0];
            String surname = parts[parts.length - 1];

            for (Worker w : workers) {
                if (w.getLastName().equalsIgnoreCase(surname) && initials.toUpperCase().startsWith(w.getFirstName().substring(0, 1).toUpperCase())) {
                    logger.info("Matched {} to worker {} {}", employeeName, w.getFirstName(), w.getLastName());
                    return w;
                }
            }
        }
        return null;
    }

    private BigDecimal calculateErhaHours(Long workerId, int month, int year) {
        try {
            Double hours = timeEntryRepository.sumHoursByWorkerAndPeriod(workerId, month, year);
            return hours != null ? BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
            default: return null;
        }
    }

    private BigDecimal getCellNumericValue(Cell cell) {
        if (cell == null) return null;
        try {
            switch (cell.getCellType()) {
                case NUMERIC: return BigDecimal.valueOf(cell.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
                case STRING:
                    String val = cell.getStringCellValue().trim();
                    return val.isEmpty() ? null : new BigDecimal(val).setScale(2, RoundingMode.HALF_UP);
                default: return null;
            }
        } catch (Exception e) { return null; }
    }

    public List<JarisonBatchImport> getAllImports() { return batchImportRepository.findAllByOrderByPeriodYearDescPeriodMonthDesc(); }
    public JarisonBatchImport getImportById(Long id) { return batchImportRepository.findById(id).orElse(null); }
    public List<JarisonMonthlyHours> getHoursByImportId(Long importId) { return monthlyHoursRepository.findByBatchImportIdOrderByEmployeeNameAsc(importId); }
    public List<JarisonMonthlyHours> getVariances(Long importId) { return monthlyHoursRepository.findVariancesByImportId(importId); }
    public List<JarisonMonthlyHours> getUnmatched(Long importId) { return monthlyHoursRepository.findUnmatchedByImportId(importId); }

    public JarisonMonthlyHours linkWorker(Long recordId, Long workerId) {
        JarisonMonthlyHours record = monthlyHoursRepository.findById(recordId).orElseThrow();
        Worker worker = workerRepository.findById(workerId).orElseThrow();
        record.setWorker(worker);
        worker.setJarisonCode(record.getJarisonCode());
        workerRepository.save(worker);
        
        JarisonBatchImport batch = record.getBatchImport();
        BigDecimal erhaHours = calculateErhaHours(workerId, batch.getPeriodMonth(), batch.getPeriodYear());
        record.setErhaJobHours(erhaHours);
        BigDecimal variance = record.getTotalHours().subtract(erhaHours);
        record.setVarianceHours(variance);
        record.setReconciliationStatus(variance.abs().compareTo(new BigDecimal("1")) <= 0 ? 
            JarisonMonthlyHours.ReconciliationStatus.MATCHED : JarisonMonthlyHours.ReconciliationStatus.VARIANCE);
        
        return monthlyHoursRepository.save(record);
    }
}