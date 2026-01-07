package com.erha.ops.controller;

import org.springframework.security.access.prepost.PreAuthorize;

import com.erha.ops.entity.Job;
import com.erha.ops.entity.Client;
import com.erha.ops.rfq.entity.RFQ;
import com.erha.ops.entity.Quote;
import com.erha.ops.entity.WorkProgress;
import com.erha.ops.service.JobService;
import com.erha.ops.repository.JobRepository;
import com.erha.ops.repository.JobTaskRepository;
import com.erha.ops.entity.JobTask;
import com.erha.ops.rfq.repository.RFQRepository;
import com.erha.ops.repository.QuoteRepository;
import com.erha.ops.repository.ClientRepository;
import com.erha.ops.rfq.repository.RFQLineItemRepository;
import com.erha.ops.repository.JobLineItemRepository;
import com.erha.ops.rfq.entity.RFQLineItem;
import com.erha.ops.entity.JobLineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/v1/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    private static final Logger logger = LoggerFactory.getLogger(JobController.class);

    @Autowired
    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobTaskRepository jobTaskRepository;

    @Autowired
    private RFQRepository rfqRepository;

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private RFQLineItemRepository rfqLineItemRepository;

    @Autowired
    private JobLineItemRepository jobLineItemRepository;

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        logger.info("Fetching all jobs");
        try {
            List<Job> jobs = jobService.getAllJobs();
            logger.info("Found {} jobs", jobs.size());
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            logger.error("Error fetching jobs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<Job> createJob(@RequestBody Job job) {
        logger.info("Creating new job: {}", job.getJobNumber());
        try {
            Job createdJob = jobService.createJob(job);
            logger.info("Created job: {} - {}", createdJob.getJobNumber(), createdJob.getDescription());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdJob);
        } catch (Exception e) {
            logger.error("Error creating job: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/from-rfq/{rfqId}")
    public ResponseEntity<Map<String, Object>> createJobFromRfq(@PathVariable Long rfqId) {
        logger.info("Creating job from RFQ ID: {}", rfqId);

        try {
            Optional<RFQ> rfqOpt = rfqRepository.findById(rfqId);
            if (!rfqOpt.isPresent()) {
                logger.warn("RFQ not found with ID: {}", rfqId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "RFQ not found"));
            }
            RFQ rfq = rfqOpt.get();

            List<Job> existingJobs = jobRepository.findByRfqId(rfqId);
            if (!existingJobs.isEmpty()) {
                Job existingJob = existingJobs.get(0);
                logger.warn("Job already exists for RFQ {}: Job ID {}", rfqId, existingJob.getJobId());
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Job already exists for this RFQ");
                response.put("jobId", existingJob.getJobId());
                response.put("jobNumber", existingJob.getJobNumber());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            List<Quote> quotes = quoteRepository.findByRfqId(rfqId);
            Quote quote = quotes.isEmpty() ? null : quotes.get(0);

            Client client = null;
            if (rfq.getClientId() != null) {
                client = clientRepository.findById(rfq.getClientId()).orElse(null);
            }

            Job job = new Job();
            String jobNumber = generateJobNumber();
            job.setJobNumber(jobNumber);
            logger.info("Generated job number: {}", jobNumber);

            job.setRfqId(rfqId);
            job.setClientId(rfq.getClientId());
            job.setDescription(rfq.getDescription());
            job.setOrderNumber(rfq.getOrderNumber());
            job.setOrderReceivedDate(rfq.getOrderDate());
            job.setDepartment(rfq.getOperatingEntity());
            job.setLocation(Job.JobLocation.SHOP);

            if (quote != null) {
                job.setQuoteId(quote.getQuoteId());
                job.setOrderValueExcl(quote.getValueExclVat());
                job.setOrderValueIncl(quote.getValueInclVat());
                logger.info("Linked to Quote {}: Excl={}, Incl={}",
                        quote.getQuoteId(), quote.getValueExclVat(), quote.getValueInclVat());
            } else {
                job.setOrderValueExcl(rfq.getEstimatedValue());
                if (rfq.getEstimatedValue() != null) {
                    job.setOrderValueIncl(rfq.getEstimatedValue().multiply(new BigDecimal("1.15")));
                }
                logger.info("No quote found - using RFQ estimated value: {}", rfq.getEstimatedValue());
            }

            job.setOrderReceivedDate(rfq.getOrderDate() != null ? rfq.getOrderDate() : LocalDate.now());

            if (rfq.getRequiredDate() != null) {
                job.setExpectedDeliveryDate(rfq.getRequiredDate());
            } else {
                job.setExpectedDeliveryDate(LocalDate.now().plusDays(30));
            }

            job.setStatus(Job.JobStatus.NEW);

            String rfqPriority = rfq.getPriority().toString();
            if ("URGENT".equalsIgnoreCase(rfqPriority)) {
                job.setPriority(Job.JobPriority.URGENT);
            } else if ("HIGH".equalsIgnoreCase(rfqPriority)) {
                job.setPriority(Job.JobPriority.HIGH);
            } else if ("LOW".equalsIgnoreCase(rfqPriority)) {
                job.setPriority(Job.JobPriority.LOW);
            } else {
                job.setPriority(Job.JobPriority.MEDIUM);
            }

            job.setJobType(Job.JobType.NORMAL);
            job.setProgressPercentage(0);
            job.setCreatedBy("SYSTEM");
            job.setCreatedDate(LocalDateTime.now());
            job.setLastModifiedBy("SYSTEM");
            job.setLastModifiedDate(LocalDateTime.now());

            Job savedJob = jobRepository.save(job);
            logger.info("Saved job to database: ID={}", savedJob.getJobId());

            // Copy RFQ line items to Job line items
            List<RFQLineItem> rfqLineItems = rfqLineItemRepository.findByRfqIdOrderByLineNumberAsc(rfqId);
            logger.info("Found {} RFQ line items to copy", rfqLineItems.size());
            for (RFQLineItem rfqItem : rfqLineItems) {
                JobLineItem jobItem = new JobLineItem();
                jobItem.setJobId(savedJob.getJobId());
                jobItem.setLineNumber(rfqItem.getLineNumber());
                jobItem.setDescription(rfqItem.getDescription());
                jobItem.setQuantity(rfqItem.getQuantity());
                jobItem.setUnitOfMeasure(rfqItem.getUnitOfMeasure());
                jobItem.setSource("RFQ");
                jobItem.setStatus("PENDING");
                jobLineItemRepository.save(jobItem);
            }
            logger.info("Copied {} line items from RFQ to Job", rfqLineItems.size());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobId", savedJob.getJobId());
            response.put("jobNumber", savedJob.getJobNumber());
            response.put("message", "Job created successfully from RFQ");
            response.put("rfqNumber", rfq.getJobNo());

            logger.info("SUCCESS: Created job {} from RFQ {}", savedJob.getJobNumber(), rfq.getJobNo());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("ERROR creating job from RFQ: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create job: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private String generateJobNumber() {
        int year = LocalDate.now().getYear() % 100;
        String yearPrefix = String.format("%02d-", year);

        List<Job> allJobs = jobRepository.findAll();
        List<Job> jobsThisYear = allJobs.stream()
                .filter(j -> j.getJobNumber() != null && j.getJobNumber().startsWith(yearPrefix))
                .collect(java.util.stream.Collectors.toList());

        int nextNumber = 1;
        if (!jobsThisYear.isEmpty()) {
            Optional<Integer> maxNumber = jobsThisYear.stream()
                    .map(j -> {
                        try {
                            String[] parts = j.getJobNumber().split("-");
                            if (parts.length >= 2) {
                                return Integer.parseInt(parts[1]);
                            }
                            return 0;
                        } catch (Exception e) {
                            logger.warn("Could not parse job number: {}", j.getJobNumber());
                            return 0;
                        }
                    })
                    .max(Integer::compareTo);

            nextNumber = maxNumber.orElse(0) + 1;
        }

        String jobNumber = String.format("%02d-%d", year, nextNumber);
        logger.info("Generated job number: {} (year={}, sequence={})", jobNumber, year, nextNumber);
        return jobNumber;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        logger.info("Fetching job by ID: {}", id);
        try {
            Optional<Job> job = jobService.getJobById(id);
            if (job.isPresent()) {
                logger.info("Found job: {}", job.get().getJobNumber());
                return ResponseEntity.ok(job.get());
            } else {
                logger.warn("Job not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error fetching job: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(@PathVariable Long id, @RequestBody Job job) {
        logger.info("Updating job with ID: {}", id);
        try {
            Job updatedJob = jobService.updateJob(id, job);
            logger.info("Updated job: {}", updatedJob.getJobNumber());
            return ResponseEntity.ok(updatedJob);
        } catch (RuntimeException e) {
            logger.warn("Job not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating job: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteJob(@PathVariable Long id) {
        logger.info("Deleting job with ID: {}", id);
        try {
            Optional<Job> job = jobService.getJobById(id);
            if (job.isEmpty()) {
                logger.warn("Job not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            jobService.deleteJob(id);
            logger.info("Deleted job: {}", job.get().getJobNumber());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Job deleted successfully");
            response.put("jobId", id);
            response.put("jobNumber", job.get().getJobNumber());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting job: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Job> partialUpdateJob(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        logger.info("Partial update job with ID: {} - Fields: {}", id, updates.keySet());

        try {
            Optional<Job> existingJob = jobService.getJobById(id);

            if (existingJob.isEmpty()) {
                logger.warn("Job not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            Job job = existingJob.get();

            if (updates.containsKey("jobNumber")) {
                job.setJobNumber((String) updates.get("jobNumber"));
            }
            if (updates.containsKey("description")) {
                job.setDescription((String) updates.get("description"));
            }
            if (updates.containsKey("department")) {
                job.setDepartment((String) updates.get("department"));
            }
            if (updates.containsKey("status")) {
                String statusStr = (String) updates.get("status");
                job.setStatus(Job.JobStatus.valueOf(statusStr.toUpperCase()));
                logger.info("Changing job {} status to: {}", job.getJobNumber(), statusStr);
            }
            if (updates.containsKey("location")) {
                String locationStr = (String) updates.get("location");
                job.setLocation(Job.JobLocation.valueOf(locationStr.toUpperCase()));
            }
            if (updates.containsKey("clientId")) {
                job.setClientId(Long.valueOf(updates.get("clientId").toString()));
            }
            if (updates.containsKey("quoteId")) {
                job.setQuoteId(Long.valueOf(updates.get("quoteId").toString()));
                logger.info("Linking job {} to quote ID: {}", job.getJobNumber(), updates.get("quoteId"));
            }
            if (updates.containsKey("rfqId")) {
                job.setRfqId(Long.valueOf(updates.get("rfqId").toString()));
            }
            if (updates.containsKey("orderNumber")) {
                job.setOrderNumber((String) updates.get("orderNumber"));
            }
            if (updates.containsKey("orderValueExcl")) {
                Object value = updates.get("orderValueExcl");
                job.setOrderValueExcl(new BigDecimal(value.toString()));
            }
            if (updates.containsKey("orderValueIncl")) {
                Object value = updates.get("orderValueIncl");
                job.setOrderValueIncl(new BigDecimal(value.toString()));
            }
            if (updates.containsKey("expectedDeliveryDate")) {
                String dateStr = (String) updates.get("expectedDeliveryDate");
                job.setExpectedDeliveryDate(LocalDate.parse(dateStr));
            }
            if (updates.containsKey("actualDeliveryDate")) {
                String dateStr = (String) updates.get("actualDeliveryDate");
                job.setActualDeliveryDate(LocalDate.parse(dateStr));
            }

            Job updatedJob = jobService.updateJob(id, job);
            logger.info("Partial update successful for job: {}", updatedJob.getJobNumber());
            return ResponseEntity.ok(updatedJob);

        } catch (Exception e) {
            logger.error("Error in partial update: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Job> updateJobStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        logger.info("Updating job {} status to: {}", id, status);
        try {
            Job.JobStatus jobStatus = Job.JobStatus.valueOf(status.toUpperCase());
            Job updatedJob = jobService.updateJobStatus(id, jobStatus);
            logger.info("Job {} status updated to: {}", updatedJob.getJobNumber(), status);
            return ResponseEntity.ok(updatedJob);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid status '{}': {}", status, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error updating job status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Job>> getJobsByStatus(@PathVariable String status) {
        logger.info("Fetching jobs by status: {}", status);
        try {
            Job.JobStatus jobStatus = Job.JobStatus.valueOf(status.toUpperCase());
            List<Job> jobs = jobService.getJobsByStatus(jobStatus);
            logger.info("Found {} jobs with status {}", jobs.size(), jobStatus);
            return ResponseEntity.ok(jobs);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid status '{}': {}", status, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error fetching jobs by status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/department/{department}")
    public ResponseEntity<List<Job>> getJobsByDepartment(@PathVariable String department) {
        logger.info("Fetching jobs for department: {}", department);
        try {
            List<Job> jobs = jobService.getJobsByDepartment(department);
            logger.info("Found {} jobs for department {}", jobs.size(), department);
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            logger.error("Error fetching jobs by department: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Job>> getJobsByClient(@PathVariable Long clientId) {
        logger.info("Fetching jobs for client ID: {}", clientId);
        try {
            List<Job> jobs = jobService.getJobsByClient(clientId);
            logger.info("Found {} jobs for client {}", jobs.size(), clientId);
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            logger.error("Error fetching jobs by client: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/emergency")
    public ResponseEntity<List<Job>> getEmergencyJobs() {
        logger.info("Fetching urgent/overdue jobs");
        try {
            List<Job> jobs = jobService.getOverdueJobs();
            logger.info("Found {} urgent/overdue jobs", jobs.size());
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            logger.error("Error fetching urgent jobs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/job-card")
    public ResponseEntity<Map<String, Object>> getJobCard(@PathVariable Long id) {
        logger.info("Generating job card for job ID: {}", id);

        try {
            Optional<Job> jobOpt = jobService.getJobById(id);

            if (jobOpt.isEmpty()) {
                logger.warn("Job not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            Job job = jobOpt.get();

            Map<String, Object> jobCard = new HashMap<>();
            jobCard.put("jobNumber", job.getJobNumber());
            jobCard.put("description", job.getDescription());
            jobCard.put("department", job.getDepartment());
            jobCard.put("status", job.getStatus().toString());
            jobCard.put("location", job.getLocation().toString());
            jobCard.put("clientId", job.getClientId());
            jobCard.put("orderNumber", job.getOrderNumber());
            jobCard.put("orderValue", job.getOrderValueExcl());
            jobCard.put("expectedDelivery", job.getExpectedDeliveryDate());
            jobCard.put("generatedAt", LocalDate.now());

            logger.info("Generated job card for: {}", job.getJobNumber());
            return ResponseEntity.ok(jobCard);

        } catch (Exception e) {
            logger.error("Error generating job card: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getJobStatistics() {
        logger.info("Fetching job statistics");
        try {
            List<Job> allJobs = jobService.getAllJobs();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalJobs", allJobs.size());
            stats.put("activeJobs", allJobs.stream().filter(j ->
                    j.getStatus() == Job.JobStatus.IN_PROGRESS ||
                            j.getStatus() == Job.JobStatus.QUALITY_CHECK ||
                            j.getStatus() == Job.JobStatus.READY
            ).count());
            stats.put("completedJobs", allJobs.stream().filter(j ->
                    j.getStatus() == Job.JobStatus.COMPLETE ||
                            j.getStatus() == Job.JobStatus.DELIVERED ||
                            j.getStatus() == Job.JobStatus.INVOICED
            ).count());
            stats.put("overdueJobs", jobService.getOverdueJobs().size());

            logger.info("Statistics: Total={}, Active={}, Completed={}, Overdue={}",
                    stats.get("totalJobs"), stats.get("activeJobs"),
                    stats.get("completedJobs"), stats.get("overdueJobs"));

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error fetching statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{parentId}/children")
    public ResponseEntity<List<Map<String, Object>>> createChildJobs(
            @PathVariable Long parentId,
            @RequestBody List<Job> childJobs) {

        logger.info("Creating {} child job(s) under parent ID: {}", childJobs.size(), parentId);

        try {
            Optional<Job> parentOpt = jobService.getJobById(parentId);
            if (parentOpt.isEmpty()) {
                logger.warn("Parent job not found with ID: {}", parentId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
            }

            Job parent = parentOpt.get();

            if (!Boolean.TRUE.equals(parent.getIsParentJob())) {
                parent.setIsParentJob(true);
                jobService.updateJob(parentId, parent);
                logger.info("Marked job {} as parent", parent.getJobNumber());
            }

            List<Job> existingChildren = jobRepository.findByParentJobId(parentId);
            int startSequence = existingChildren.size() + 1;

            List<Map<String, Object>> responses = new ArrayList<>();

            for (int i = 0; i < childJobs.size(); i++) {
                Job childJob = childJobs.get(i);

                int sequenceNum = startSequence + i;
                String sequence = String.format("%02d", sequenceNum);
                String childJobNumber = parent.getJobNumber() + "-" + sequence;

                childJob.setJobNumber(childJobNumber);
                childJob.setParentJobId(parentId);
                childJob.setJobSequence(sequence);
                childJob.setIsParentJob(false);

                if (childJob.getClientId() == null) {
                    childJob.setClientId(parent.getClientId());
                }
                if (childJob.getDepartment() == null) {
                    childJob.setDepartment(parent.getDepartment());
                }
                if (childJob.getBillingType() == null) {
                    childJob.setBillingType(parent.getBillingType());
                }

                if (childJob.getCreationSource() == null) {
                    childJob.setCreationSource("MANUAL");
                }
                if (childJob.getStatus() == null) {
                    childJob.setStatus(Job.JobStatus.NEW);
                }
                if (childJob.getPriority() == null) {
                    childJob.setPriority(Job.JobPriority.MEDIUM);
                }

                childJob.setCreatedBy("SYSTEM");
                childJob.setCreatedDate(LocalDateTime.now());
                childJob.setLastModifiedBy("SYSTEM");
                childJob.setLastModifiedDate(LocalDateTime.now());

                Job savedChild = jobRepository.save(childJob);

                logger.info("Created child job {} under parent {}",
                        savedChild.getJobNumber(), parent.getJobNumber());

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("jobId", savedChild.getJobId());
                response.put("jobNumber", savedChild.getJobNumber());
                response.put("parentJobNumber", parent.getJobNumber());
                response.put("sequence", sequence);

                responses.add(response);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(responses);

        } catch (Exception e) {
            logger.error("Error creating child jobs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<Job>> getChildJobs(@PathVariable Long parentId) {
        logger.info("Fetching child jobs for parent ID: {}", parentId);

        try {
            List<Job> children = jobRepository.findByParentJobId(parentId);
            logger.info("Found {} child jobs for parent {}", children.size(), parentId);
            return ResponseEntity.ok(children);
        } catch (Exception e) {
            logger.error("Error fetching child jobs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{jobId}/tasks")
    public ResponseEntity<List<JobTask>> getJobTasks(@PathVariable Long jobId) {
        logger.info("Fetching tasks for job ID: {}", jobId);

        try {
            List<JobTask> tasks = jobTaskRepository.findByJobIdOrderBySequenceNumberAsc(jobId);
            logger.info("Found {} tasks for job {}", tasks.size(), jobId);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            logger.error("Error fetching tasks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{jobId}/tasks")
    public ResponseEntity<List<JobTask>> addJobTasks(
            @PathVariable Long jobId,
            @RequestBody List<JobTask> tasks) {

        logger.info("Adding {} task(s) to job ID: {}", tasks.size(), jobId);

        try {
            Optional<Job> jobOpt = jobService.getJobById(jobId);
            if (jobOpt.isEmpty()) {
                logger.warn("Job not found with ID: {}", jobId);
                return ResponseEntity.notFound().build();
            }

            long taskCount = jobTaskRepository.countByJobId(jobId);
            List<JobTask> savedTasks = new ArrayList<>();

            for (int i = 0; i < tasks.size(); i++) {
                JobTask task = tasks.get(i);

                if (task.getSequenceNumber() == null || task.getSequenceNumber() == 0) {
                    task.setSequenceNumber((int) (taskCount + i + 1));
                }

                task.setJobId(jobId);
                task.setCompleted(false);
                task.setCreatedDate(LocalDateTime.now());
                task.setUpdatedDate(LocalDateTime.now());

                JobTask savedTask = jobTaskRepository.save(task);
                savedTasks.add(savedTask);
                logger.info("Added task {} to job {}", savedTask.getTaskId(), jobId);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(savedTasks);

        } catch (Exception e) {
            logger.error("Error adding tasks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{jobId}/tasks/{taskId}")
    public ResponseEntity<JobTask> updateTask(
            @PathVariable Long jobId,
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> updates) {

        logger.info("Updating task {} for job {}", taskId, jobId);

        try {
            Optional<JobTask> taskOpt = jobTaskRepository.findById(taskId);
            if (taskOpt.isEmpty()) {
                logger.warn("Task not found with ID: {}", taskId);
                return ResponseEntity.notFound().build();
            }

            JobTask task = taskOpt.get();

            if (updates.containsKey("completed")) {
                boolean completed = (Boolean) updates.get("completed");
                task.setCompleted(completed);
                if (completed && task.getCompletedDate() == null) {
                    task.setCompletedDate(LocalDate.now());
                }
            }

            if (updates.containsKey("completedBy")) {
                task.setCompletedBy((String) updates.get("completedBy"));
            }

            if (updates.containsKey("description")) {
                task.setDescription((String) updates.get("description"));
            }

            if (updates.containsKey("assignedTo")) {
                task.setAssignedTo((String) updates.get("assignedTo"));
            }

            if (updates.containsKey("actualHours")) {
                task.setActualHours(new BigDecimal(updates.get("actualHours").toString()));
            }

            if (updates.containsKey("notes")) {
                task.setNotes((String) updates.get("notes"));
            }

            task.setUpdatedDate(LocalDateTime.now());

            JobTask savedTask = jobTaskRepository.save(task);
            logger.info("Updated task {}", taskId);

            return ResponseEntity.ok(savedTask);

        } catch (Exception e) {
            logger.error("Error updating task: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{jobId}/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> deleteTask(
            @PathVariable Long jobId,
            @PathVariable Long taskId) {

        logger.info("Deleting task {} from job {}", taskId, jobId);

        try {
            Optional<JobTask> taskOpt = jobTaskRepository.findById(taskId);
            if (taskOpt.isEmpty()) {
                logger.warn("Task not found with ID: {}", taskId);
                return ResponseEntity.notFound().build();
            }

            jobTaskRepository.deleteById(taskId);
            logger.info("Deleted task {}", taskId);

            return ResponseEntity.ok(Map.of("success", true, "message", "Task deleted"));

        } catch (Exception e) {
            logger.error("Error deleting task: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
