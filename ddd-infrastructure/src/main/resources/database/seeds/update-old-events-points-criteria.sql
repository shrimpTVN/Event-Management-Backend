-- ============================================================================
-- PATCH / SEED: GÁN CRITERIA SV5T & EVENT_POINTS CHO 10 SỰ KIỆN CŨ (event-data.sql)
-- - Khớp theo tên sự kiện (events.name), không phụ thuộc vào events.id tự tăng.
-- - An toàn idempotent: chỉ UPDATE nếu criteria_id tồn tại và chỉ INSERT event_points
--   khi chưa có cặp (event_id, point_category_id).
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. CẬP NHẬT criteria_id CHO 10 SỰ KIỆN TRONG event-data.sql
-- ----------------------------------------------------------------------------
UPDATE events AS e
SET criteria_id = v.criteria_id,
    updated_at  = NOW()
FROM (VALUES
    ('Tech Seminar 2026',        5023),
    ('Spring Boot Workshop',     5025),
    ('Data Science Bootcamp',    5025),
    ('UI/UX Design Contest',     5023),
    ('AI Hackathon',             5023),
    ('Cybersecurity Talk',       1033),
    ('Cloud Computing 101',      5025),
    ('DevOps Meeting',           5033),
    ('Web3 Conference',          5031),
    ('Open Source Contribution', 5023)
) AS v(event_name, criteria_id)
WHERE e.name = v.event_name
  AND EXISTS (SELECT 1 FROM criterias c WHERE c.id = v.criteria_id);

-- ----------------------------------------------------------------------------
-- 2. THÊM event_points DÙNG ID LÁ TRONG seed_point_categories.sql (1..73)
-- ----------------------------------------------------------------------------
INSERT INTO event_points (event_id, point_category_id, point, is_active, created_at, updated_at)
SELECT e.id, v.point_category_id, v.point, TRUE, NOW(), NOW()
FROM (VALUES
    ('Tech Seminar 2026',        16, 2),
    ('Tech Seminar 2026',        63, 3),
    ('Spring Boot Workshop',     16, 2),
    ('Spring Boot Workshop',     63, 3),
    ('Data Science Bootcamp',    16, 2),
    ('Data Science Bootcamp',     5, 5),
    ('UI/UX Design Contest',     20, 2),
    ('UI/UX Design Contest',     73, 4),
    ('AI Hackathon',             20, 2),
    ('AI Hackathon',             72, 6),
    ('Cybersecurity Talk',       49, 3),
    ('Cybersecurity Talk',       16, 2),
    ('Cloud Computing 101',      16, 2),
    ('Cloud Computing 101',      63, 3),
    ('DevOps Meeting',           16, 2),
    ('DevOps Meeting',           63, 3),
    ('Web3 Conference',          16, 2),
    ('Web3 Conference',          63, 3),
    ('Open Source Contribution', 16, 2),
    ('Open Source Contribution', 54, 5)
) AS v(event_name, point_category_id, point)
JOIN events e ON e.name = v.event_name
WHERE EXISTS (SELECT 1 FROM point_categories pc WHERE pc.id = v.point_category_id)
  AND NOT EXISTS (
      SELECT 1 FROM event_points ep
      WHERE ep.event_id = e.id AND ep.point_category_id = v.point_category_id
  );

COMMIT;
