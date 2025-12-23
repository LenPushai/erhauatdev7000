package com.erha.ops.controller;

import com.erha.ops.entity.TaskTemplate;
import com.erha.ops.entity.TemplateTask;
import com.erha.ops.repository.TaskTemplateRepository;
import com.erha.ops.repository.TemplateTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/v1/task-templates")
@CrossOrigin(origins = "*")
public class TaskTemplateController {

    private static final Logger logger = LoggerFactory.getLogger(TaskTemplateController.class);

    @Autowired
    private TaskTemplateRepository taskTemplateRepository;

    @Autowired
    private TemplateTaskRepository templateTaskRepository;

    @GetMapping
    public ResponseEntity<List<TaskTemplate>> getAllTemplates() {
        logger.info("Fetching all task templates");
        try {
            List<TaskTemplate> templates = taskTemplateRepository.findByIsActiveTrue();
            logger.info("Found {} active templates", templates.size());
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            logger.error("Error fetching templates: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskTemplate> getTemplateById(@PathVariable Long id) {
        logger.info("Fetching template by ID: {}", id);
        try {
            Optional<TaskTemplate> template = taskTemplateRepository.findById(id);
            if (template.isPresent()) {
                logger.info("Found template: {}", template.get().getTemplateName());
                return ResponseEntity.ok(template.get());
            } else {
                logger.warn("Template not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error fetching template: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<TaskTemplate> createTemplate(@RequestBody TaskTemplate template) {
        logger.info("Creating new task template: {}", template.getTemplateName());
        try {
            template.setCreatedBy("SYSTEM");
            template.setCreatedDate(LocalDateTime.now());
            template.setUpdatedDate(LocalDateTime.now());
            if (template.getTasks() != null) {
                for (TemplateTask task : template.getTasks()) {
                    task.setTemplate(template);
                }
            }
            TaskTemplate saved = taskTemplateRepository.save(template);
            logger.info("Created template: {} with {} tasks", saved.getTemplateName(), saved.getTasks() != null ? saved.getTasks().size() : 0);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            logger.error("Error creating template: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskTemplate> updateTemplate(@PathVariable Long id, @RequestBody TaskTemplate template) {
        logger.info("Updating template ID: {}", id);
        try {
            Optional<TaskTemplate> existing = taskTemplateRepository.findById(id);
            if (existing.isEmpty()) {
                logger.warn("Template not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            TaskTemplate existingTemplate = existing.get();
            existingTemplate.setTemplateName(template.getTemplateName());
            existingTemplate.setDescription(template.getDescription());
            existingTemplate.setDepartment(template.getDepartment());
            existingTemplate.setEstimatedTotalHours(template.getEstimatedTotalHours());
            existingTemplate.setUpdatedBy("SYSTEM");
            existingTemplate.setUpdatedDate(LocalDateTime.now());
            if (template.getTasks() != null) {
                existingTemplate.getTasks().clear();
                for (TemplateTask task : template.getTasks()) {
                    task.setTemplate(existingTemplate);
                    existingTemplate.getTasks().add(task);
                }
            }
            TaskTemplate updated = taskTemplateRepository.save(existingTemplate);
            logger.info("Updated template: {}", updated.getTemplateName());
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.error("Error updating template: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTemplate(@PathVariable Long id) {
        logger.info("Deleting template ID: {}", id);
        try {
            Optional<TaskTemplate> template = taskTemplateRepository.findById(id);
            if (template.isEmpty()) {
                logger.warn("Template not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            TaskTemplate temp = template.get();
            temp.setIsActive(false);
            temp.setUpdatedDate(LocalDateTime.now());
            taskTemplateRepository.save(temp);
            logger.info("Soft deleted template: {}", temp.getTemplateName());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Template deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting template: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TemplateTask>> getTemplateTasks(@PathVariable Long id) {
        logger.info("Fetching tasks for template ID: {}", id);
        try {
            List<TemplateTask> tasks = templateTaskRepository.findByTemplate_TemplateIdOrderBySequenceNumberAsc(id);
            logger.info("Found {} tasks for template {}", tasks.size(), id);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            logger.error("Error fetching template tasks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
