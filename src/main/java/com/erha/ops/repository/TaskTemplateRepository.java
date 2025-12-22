package com.erha.ops.repository;

import com.erha.ops.entity.TaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, Long> {
    List<TaskTemplate> findByIsActiveTrue();
    TaskTemplate findByTemplateName(String templateName);
    List<TaskTemplate> findByDepartment(String department);
}
