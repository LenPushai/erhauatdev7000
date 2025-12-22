package com.erha.ops.service;

import com.erha.ops.entity.*;
import com.erha.ops.repository.HoldingPointRepository;
import com.erha.ops.repository.JobHoldingPointSignoffRepository;
import com.erha.ops.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class HoldingPointSignoffService {

    private static final Logger logger = LoggerFactory.getLogger(HoldingPointSignoffService.class);

    @Autowired
    private HoldingPointRepository holdingPointRepository;

    @Autowired
    private JobHoldingPointSignoffRepository signoffRepository;

    @Autowired
    private JobRepository jobRepository;

    public List<HoldingPoint> getAllHoldingPoints() { return holdingPointRepository.findAll(); }
    public List<HoldingPoint> getActiveHoldingPoints() { return holdingPointRepository.findByIsActiveTrueOrderBySequenceNumberAsc(); }

    public void initializeJobSignoffs(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        if (!signoffRepository.findByJobJobId(jobId).isEmpty()) return;
        List<HoldingPoint> holdingPoints = holdingPointRepository.findByIsActiveTrueOrderBySequenceNumberAsc();
        for (HoldingPoint hp : holdingPoints) {
            JobHoldingPointSignoff signoff = new JobHoldingPointSignoff();
            signoff.setJob(job);
            signoff.setHoldingPoint(hp);
            signoff.setStatus(SignoffStatus.PENDING);
            signoff.setCreatedAt(LocalDateTime.now());
            signoffRepository.save(signoff);
        }
    }

    public List<JobHoldingPointSignoff> getJobSignoffs(Long jobId) { return signoffRepository.findByJobJobIdOrderByHoldingPointSequenceNumberAsc(jobId); }

    public JobHoldingPointSignoff passHoldingPoint(Long jobId, Long holdingPointId, Long signedById, String notes) {
        JobHoldingPointSignoff signoff = findSignoff(jobId, holdingPointId);
        signoff.setStatus(SignoffStatus.PASSED);
        signoff.setSignedById(signedById);
        signoff.setSignedAt(LocalDateTime.now());
        signoff.setNotes(notes);
        signoff = signoffRepository.save(signoff);
        checkAllSignoffsComplete(jobId);
        return signoff;
    }

    public JobHoldingPointSignoff failHoldingPoint(Long jobId, Long holdingPointId, Long signedById, String notes) {
        JobHoldingPointSignoff signoff = findSignoff(jobId, holdingPointId);
        signoff.setStatus(SignoffStatus.FAILED);
        signoff.setSignedById(signedById);
        signoff.setSignedAt(LocalDateTime.now());
        signoff.setNotes(notes);
        return signoffRepository.save(signoff);
    }

    public JobHoldingPointSignoff markNotApplicable(Long jobId, Long holdingPointId, Long signedById, String notes) {
        JobHoldingPointSignoff signoff = findSignoff(jobId, holdingPointId);
        signoff.setStatus(SignoffStatus.NOT_APPLICABLE);
        signoff.setSignedById(signedById);
        signoff.setSignedAt(LocalDateTime.now());
        signoff.setNotes(notes);
        signoff = signoffRepository.save(signoff);
        checkAllSignoffsComplete(jobId);
        return signoff;
    }

    public JobHoldingPointSignoff resetHoldingPoint(Long jobId, Long holdingPointId) {
        JobHoldingPointSignoff signoff = findSignoff(jobId, holdingPointId);
        signoff.setStatus(SignoffStatus.PENDING);
        signoff.setSignedById(null);
        signoff.setSignedAt(null);
        signoff.setNotes(null);
        return signoffRepository.save(signoff);
    }

    public void resetAllSignoffs(Long jobId) {
        List<JobHoldingPointSignoff> signoffs = signoffRepository.findByJobJobId(jobId);
        for (JobHoldingPointSignoff signoff : signoffs) {
            signoff.setStatus(SignoffStatus.PENDING);
            signoff.setSignedById(null);
            signoff.setSignedAt(null);
            signoff.setNotes(null);
        }
        signoffRepository.saveAll(signoffs);
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job != null) {
            job.setWorkshopStatus(WorkshopStatus.IN_PROGRESS);
            jobRepository.save(job);
        }
    }

    public QcProgress getQcProgress(Long jobId) {
        List<JobHoldingPointSignoff> signoffs = signoffRepository.findByJobJobId(jobId);
        QcProgress progress = new QcProgress();
        progress.jobId = jobId;
        progress.total = signoffs.size();
        for (JobHoldingPointSignoff signoff : signoffs) {
            switch (signoff.getStatus()) {
                case PASSED: progress.passed++; break;
                case FAILED: progress.failed++; break;
                case NOT_APPLICABLE: progress.notApplicable++; break;
                case PENDING: progress.pending++; break;
            }
        }
        int completable = progress.total - progress.notApplicable;
        progress.percentComplete = completable > 0 ? (progress.passed * 100 / completable) : 100;
        progress.isComplete = (progress.pending == 0 && progress.failed == 0);
        return progress;
    }

    public Optional<JobHoldingPointSignoff> getNextPendingCheckpoint(Long jobId) {
        return signoffRepository.findByJobJobIdOrderByHoldingPointSequenceNumberAsc(jobId).stream()
            .filter(s -> s.getStatus() == SignoffStatus.PENDING).findFirst();
    }

    private JobHoldingPointSignoff findSignoff(Long jobId, Long holdingPointId) {
        return signoffRepository.findByJobJobIdAndHoldingPointId(jobId, holdingPointId)
            .orElseThrow(() -> new RuntimeException("Signoff not found"));
    }

    private void checkAllSignoffsComplete(Long jobId) {
        QcProgress progress = getQcProgress(jobId);
        if (progress.isComplete) {
            Job job = jobRepository.findById(jobId).orElse(null);
            if (job != null && job.getWorkshopStatus() == WorkshopStatus.QC_IN_PROGRESS) {
                job.setWorkshopStatus(WorkshopStatus.READY_FOR_DELIVERY);
                jobRepository.save(job);
            }
        }
    }

    public static class QcProgress {
        public Long jobId;
        public int total;
        public int passed;
        public int failed;
        public int pending;
        public int notApplicable;
        public int percentComplete;
        public boolean isComplete;
    }
}
