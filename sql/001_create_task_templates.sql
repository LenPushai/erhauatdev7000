-- Task Templates Schema
-- Production-ready for ERHA Operations Management System

CREATE TABLE IF NOT EXISTS task_templates (
    template_id INT PRIMARY KEY AUTO_INCREMENT,
    template_name VARCHAR(100) NOT NULL,
    description TEXT,
    department VARCHAR(50),
    estimated_total_hours DECIMAL(5,2),
    is_active BOOLEAN DEFAULT TRUE,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_template_name (template_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS template_tasks (
    template_task_id INT PRIMARY KEY AUTO_INCREMENT,
    template_id INT NOT NULL,
    sequence_number INT NOT NULL,
    description VARCHAR(255) NOT NULL,
    estimated_hours DECIMAL(5,2) DEFAULT 0,
    assigned_to VARCHAR(100),
    notes TEXT,
    FOREIGN KEY (template_id) REFERENCES task_templates(template_id) ON DELETE CASCADE,
    KEY idx_template_sequence (template_id, sequence_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO task_templates (template_name, description, department, estimated_total_hours, created_by) VALUES
('Valve Refurbishment Standard', 'Standard workflow for control valve refurbishment', 'MACHINING', 8.0, 'SYSTEM'),
('Heat Exchanger Refurb', 'Complete heat exchanger refurbishment process', 'FABRICATION', 12.0, 'SYSTEM'),
('Pipe Spool Fabrication', 'Standard pipe spool fabrication workflow', 'WELDING', 6.0, 'SYSTEM');

INSERT INTO template_tasks (template_id, sequence_number, description, estimated_hours, assigned_to) VALUES
(1, 10, 'Receive unit and create tag/documentation', 0.5, 'QC'),
(1, 20, 'Disassemble valve and clean components', 2.0, 'Fitter'),
(1, 30, 'Inspect all components for wear/damage', 1.0, 'QC'),
(1, 40, 'Replace internals (seats, seals, gaskets)', 2.5, 'Fitter'),
(1, 50, 'Reassemble and perform pressure test', 1.0, 'QC'),
(1, 60, 'Paint, label and prepare for dispatch', 1.0, 'Finisher');

INSERT INTO template_tasks (template_id, sequence_number, description, estimated_hours, assigned_to) VALUES
(2, 10, 'Receive and inspect heat exchanger unit', 0.5, 'QC'),
(2, 20, 'Drain fluids and decontaminate', 1.0, 'Shop Floor'),
(2, 30, 'Disassemble bundle from shell', 2.0, 'Fitter'),
(2, 40, 'Clean tubes and inspect for damage', 2.0, 'Fitter'),
(2, 50, 'Replace gaskets and seals', 1.5, 'Fitter'),
(2, 60, 'Pressure test tubes', 1.0, 'QC'),
(2, 70, 'Reassemble bundle into shell', 2.0, 'Fitter'),
(2, 80, 'Final pressure test and certification', 2.0, 'QC');

INSERT INTO template_tasks (template_id, sequence_number, description, estimated_hours, assigned_to) VALUES
(3, 10, 'Review drawings and cut materials', 1.0, 'Fitter'),
(3, 20, 'Fit-up pipe sections', 1.5, 'Fitter'),
(3, 30, 'Tack weld assembly', 0.5, 'Welder'),
(3, 40, 'Complete welding per procedure', 2.0, 'Welder'),
(3, 50, 'Grind and finish welds', 0.5, 'Finisher'),
(3, 60, 'Final inspection and NDT if required', 0.5, 'QC');
