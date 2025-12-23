package com.erha.ops.service;

import com.erha.ops.entity.*;
import com.erha.ops.repository.DeliveryNoteRepository;
import com.erha.ops.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class DeliveryNoteService {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryNoteService.class);

    @Autowired
    private DeliveryNoteRepository deliveryNoteRepository;

    @Autowired
    private JobRepository jobRepository;

    public DeliveryNote generateDeliveryNote(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        Optional<DeliveryNote> existing = deliveryNoteRepository.findByJobJobId(jobId);
        if (existing.isPresent()) return existing.get();
        String dnNumber = generateDeliveryNoteNumber();
        DeliveryNote note = new DeliveryNote();
        note.setJob(job);
        note.setDeliveryNoteNumber(dnNumber);
        note.setStatus(DeliveryNoteStatus.GENERATED);
        note.setCreatedAt(LocalDateTime.now());
        return deliveryNoteRepository.save(note);
    }

    public DeliveryNote dispatch(Long noteId, String deliveredBy, String vehicleInfo, String notes) {
        DeliveryNote note = deliveryNoteRepository.findById(noteId).orElseThrow(() -> new RuntimeException("Delivery note not found"));
        note.setStatus(DeliveryNoteStatus.DISPATCHED);
        note.setDeliveredBy(deliveredBy);
        note.setVehicleInfo(vehicleInfo);
        note.setNotes(notes);
        note.setDispatchedAt(LocalDateTime.now());
        return deliveryNoteRepository.save(note);
    }

    public DeliveryNote markDelivered(Long noteId, String receivedBy, String notes) {
        DeliveryNote note = deliveryNoteRepository.findById(noteId).orElseThrow(() -> new RuntimeException("Delivery note not found"));
        note.setStatus(DeliveryNoteStatus.DELIVERED);
        note.setReceivedBy(receivedBy);
        if (notes != null) note.setNotes(notes);
        note.setDeliveredAt(LocalDateTime.now());
        Job job = note.getJob();
        job.setWorkshopStatus(WorkshopStatus.DELIVERED);
        jobRepository.save(job);
        return deliveryNoteRepository.save(note);
    }

    public DeliveryNote recordCustomerSignature(Long noteId, String customerName, String signatureData) {
        DeliveryNote note = deliveryNoteRepository.findById(noteId).orElseThrow(() -> new RuntimeException("Delivery note not found"));
        note.setStatus(DeliveryNoteStatus.SIGNED);
        note.setCustomerSignature(signatureData);
        note.setReceivedBy(customerName);
        note.setSignedAt(LocalDateTime.now());
        return deliveryNoteRepository.save(note);
    }

    public Optional<DeliveryNote> getById(Long id) { return deliveryNoteRepository.findById(id); }
    public Optional<DeliveryNote> getByNumber(String number) { return deliveryNoteRepository.findByDeliveryNoteNumber(number); }
    public Optional<DeliveryNote> getByJobId(Long jobId) { return deliveryNoteRepository.findByJobJobId(jobId); }
    public List<DeliveryNote> getByStatus(DeliveryNoteStatus status) { return deliveryNoteRepository.findByStatus(status); }
    public List<DeliveryNote> getReadyForDispatch() { return deliveryNoteRepository.findByStatus(DeliveryNoteStatus.GENERATED); }
    public List<DeliveryNote> getInTransit() { return deliveryNoteRepository.findByStatus(DeliveryNoteStatus.DISPATCHED); }
    public List<DeliveryNote> getUnsigned() { return deliveryNoteRepository.findByStatusIn(List.of(DeliveryNoteStatus.DISPATCHED, DeliveryNoteStatus.DELIVERED)); }
    public List<DeliveryNote> getRecent(int limit) { return deliveryNoteRepository.findAllByOrderByCreatedAtDesc().stream().limit(limit).toList(); }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", deliveryNoteRepository.count());
        stats.put("generated", deliveryNoteRepository.findByStatus(DeliveryNoteStatus.GENERATED).size());
        stats.put("dispatched", deliveryNoteRepository.findByStatus(DeliveryNoteStatus.DISPATCHED).size());
        stats.put("delivered", deliveryNoteRepository.findByStatus(DeliveryNoteStatus.DELIVERED).size());
        stats.put("signed", deliveryNoteRepository.findByStatus(DeliveryNoteStatus.SIGNED).size());
        return stats;
    }

    private String generateDeliveryNoteNumber() {
        String prefix = "DN-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy")) + "-";
        Optional<String> maxNumber = deliveryNoteRepository.findMaxDeliveryNoteNumberByPrefix(prefix);
        int nextNumber = 1;
        if (maxNumber.isPresent()) {
            String num = maxNumber.get().substring(prefix.length());
            nextNumber = Integer.parseInt(num) + 1;
        }
        return prefix + String.format("%04d", nextNumber);
    }
}
