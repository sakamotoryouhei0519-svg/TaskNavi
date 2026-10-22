-- タスクはログインしたユーザーのもの。既存の行は、最初のユーザーに残す。
ALTER TABLE tasks ADD COLUMN owner_id INTEGER;

CREATE INDEX IF NOT EXISTS idx_tasks_owner_id ON tasks(owner_id);

UPDATE tasks
SET owner_id = (SELECT MIN(id) FROM users)
WHERE owner_id IS NULL
  AND EXISTS (SELECT 1 FROM users);
