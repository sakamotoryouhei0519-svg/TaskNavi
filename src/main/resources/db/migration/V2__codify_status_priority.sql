-- Persist status/priority as stable codes (NOT_STARTED, HIGH, ...) instead of locale labels.
UPDATE tasks SET status = 'NOT_STARTED' WHERE status IN ('未着手', 'not_started', 'NotStarted');
UPDATE tasks SET status = 'IN_PROGRESS' WHERE status IN ('進行中', 'in_progress', 'InProgress');
UPDATE tasks SET status = 'COMPLETED' WHERE status IN ('完了', 'completed', 'Completed');

UPDATE tasks SET priority = 'HIGH' WHERE priority IN ('高', 'high', 'High');
UPDATE tasks SET priority = 'MEDIUM' WHERE priority IN ('中', 'medium', 'Medium');
UPDATE tasks SET priority = 'LOW' WHERE priority IN ('低', 'low', 'Low');

-- Normalize any unexpected leftovers to defaults.
UPDATE tasks SET status = 'NOT_STARTED' WHERE status IS NULL OR TRIM(status) = ''
  OR status NOT IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED');
UPDATE tasks SET priority = 'MEDIUM' WHERE priority IS NULL OR TRIM(priority) = ''
  OR priority NOT IN ('HIGH', 'MEDIUM', 'LOW');
