-- アプリのログインをやめた（PC は Windows のログインで守る）。
-- 個人のタスクは「この PC のタスク」なので、どのアカウントのものかは持たない。前のアカウントのタスクは全部残す。
DROP INDEX IF EXISTS idx_tasks_owner_id;
ALTER TABLE tasks DROP COLUMN owner_id;

DROP TABLE IF EXISTS verification_codes;
DROP TABLE IF EXISTS users;
