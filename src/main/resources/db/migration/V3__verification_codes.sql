-- パスワード再設定コード。アプリを閉じても、有効期限まで残す。

CREATE TABLE IF NOT EXISTS verification_codes (
    email TEXT PRIMARY KEY,
    code TEXT NOT NULL,
    generated_at INTEGER NOT NULL
);
