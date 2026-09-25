-- Flyway migration: initial schema for TaskNavi

CREATE TABLE IF NOT EXISTS users (
                                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                                     username TEXT NOT NULL UNIQUE,
                                     password TEXT NOT NULL,
                                     email TEXT NOT NULL UNIQUE,
                                     role TEXT DEFAULT 'USER'
);

CREATE TABLE IF NOT EXISTS tasks (
                                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                                     parent_id INTEGER,
                                     level INTEGER DEFAULT 1,
                                     order_index INTEGER DEFAULT 0, -- 【追加】順序管理用カラム
                                     name TEXT,
                                     assignee TEXT,
                                     start_date TEXT,
                                     end_date TEXT,
                                     progress INTEGER DEFAULT 0,
                                     status TEXT DEFAULT '未着手',
                                     priority TEXT DEFAULT '中'
);

-- Add indices for common queries
CREATE INDEX IF NOT EXISTS idx_tasks_parent_id ON tasks(parent_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_tasks_assignee ON tasks(assignee);