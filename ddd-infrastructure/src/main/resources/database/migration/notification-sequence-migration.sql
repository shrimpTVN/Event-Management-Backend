-- 1. Create sequence starting after current max id
CREATE SEQUENCE IF NOT EXISTS notifications_id_seq
        START WITH 1
        INCREMENT BY 50
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;

-- 2. Sync sequence to avoid PK collision with existing rows
SELECT setval('notifications_id_seq', COALESCE((SELECT MAX(id) FROM notifications), 0) + 1, false);

-- 3. Remove IDENTITY default, point to sequence instead
ALTER TABLE notifications ALTER COLUMN id DROP DEFAULT ;
ALTER TABLE notifications ALTER COLUMN id SET DEFAULT nextval('notifications_id_seq');

SHOW log_statement;