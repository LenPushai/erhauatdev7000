package com.erha.ops.repository;

import com.erha.ops.entity.TemplateTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TemplateTaskRepository extends JpaRepository<TemplateTask, Long> {
    List<TemplateTask> findByTemplate_TemplateIdOrderBySequenceNumberAsc(Long templateId);
}
