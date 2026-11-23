-- ============================================================
--  Migration: Support Fanpage in Notifications
-- ============================================================

-- 1. Make user_id nullable so notifications can belong to a Fanpage
ALTER TABLE notifications
    ALTER COLUMN user_id DROP NOT NULL;

-- 2. Add fanpage_id column
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS fanpage_id BIGINT;

-- 3. Add foreign key constraint to fanpages table if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_notifications_fanpages'
    ) THEN
        ALTER TABLE notifications
            ADD CONSTRAINT fk_notifications_fanpages
                FOREIGN KEY (fanpage_id) REFERENCES fanpages (id) ON DELETE CASCADE;
    END IF;
END $$;

-- 4. Create performance indexes for fanpage notifications
CREATE INDEX IF NOT EXISTS idx_notifications_fanpage_id ON notifications (fanpage_id);
CREATE INDEX IF NOT EXISTS idx_notifications_fanpage_status ON notifications (fanpage_id, status);
CREATE INDEX IF NOT EXISTS idx_notifications_fanpage_created ON notifications (fanpage_id, created_at);
