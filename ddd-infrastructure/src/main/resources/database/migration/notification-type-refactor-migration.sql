-- ============================================================
--  Migration: Refactor Notification title to type (NotificationTypeEnum)
-- ============================================================

-- 1. Rename column title to type
ALTER TABLE notifications
    RENAME COLUMN title TO type;

-- 2. Alter type column length to VARCHAR(50)
ALTER TABLE notifications
    ALTER COLUMN type TYPE VARCHAR(50);

-- 3. (Optional) Create index on type for fast querying by notification type
CREATE INDEX idx_notifications_type ON notifications (type);
