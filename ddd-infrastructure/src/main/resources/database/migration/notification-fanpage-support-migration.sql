-- ============================================================
--  Migration: Support Fanpage in Notifications
-- ============================================================

-- 1. Make user_id nullable so notifications can belong to a Fanpage
ALTER TABLE notifications
    ALTER COLUMN user_id DROP NOT NULL;

-- 2. Add fanpage_id column
ALTER TABLE notifications
    ADD COLUMN fanpage_id BIGINT;

-- 3. Add foreign key constraint to fanpages table
ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_fanpages
        FOREIGN KEY (fanpage_id) REFERENCES fanpages (id) ON DELETE CASCADE;

-- 4. Create performance indexes for fanpage notifications
CREATE INDEX idx_notifications_fanpage_id ON notifications (fanpage_id);
CREATE INDEX idx_notifications_fanpage_status ON notifications (fanpage_id, status);
CREATE INDEX idx_notifications_fanpage_created ON notifications (fanpage_id, created_at);
