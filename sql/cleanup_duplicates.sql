-- Clean duplicate template_tasks
DELETE t1 FROM template_tasks t1
INNER JOIN template_tasks t2 
WHERE 
    t1.template_task_id > t2.template_task_id 
    AND t1.template_id = t2.template_id 
    AND t1.sequence_number = t2.sequence_number 
    AND t1.description = t2.description;

SELECT 'Cleanup complete. Remaining tasks:' as message;
SELECT template_id, COUNT(*) as task_count 
FROM template_tasks 
GROUP BY template_id;
