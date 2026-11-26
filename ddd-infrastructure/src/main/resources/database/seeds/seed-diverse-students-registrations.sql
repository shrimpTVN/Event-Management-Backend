-- ============================================================================
-- SEED DATA: 30 RANDOM STUDENT USERS & REGISTRATIONS FOR EVENTS 101..116
-- - Không TRUNCATE dữ liệu cũ; dùng ID 501..530 cho users & student_profiles.
-- - Tự động phân bổ ngẫu nhiên sinh viên vào các sự kiện PUBLISHED (101..116)
--   trong giới hạn capacity (10..20 chỗ) và giới hạn nam/nữ khi gender_constraint = TRUE.
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. SINH NGẪU NHIÊN 30 SINH VIÊN (id 501..530) + USER_ROLES (STUDENT) + PROFILES
-- ----------------------------------------------------------------------------
WITH generated_students AS (
    SELECT
        gs AS idx,
        500 + gs AS uid,
        (ARRAY['Nguyễn','Trần','Lê','Phạm','Huỳnh','Võ','Đặng','Bùi','Đỗ','Hồ','Ngô','Dương'])[1 + floor(random() * 12)::int] AS ho,
        (ARRAY['Minh Khoa','Ngọc Anh','Quốc Trung','Thảo Nhi','Hoàng Long','Mỹ Linh','Gia Thịnh','Thanh Vy','Anh Kiệt','Khánh Vy','Tuấn Đạt','Thu Hằng'])[1 + (gs % 12)] AS ten,
        (ARRAY['MALE','FEMALE','MALE','FEMALE','MALE','FEMALE','MALE','FEMALE','MALE','FEMALE','MALE','FEMALE'])[1 + (gs % 12)] AS gt,
        (ARRAY[47, 48, 49, 50])[1 + (gs % 4)] AS khoahoc,
        (ARRAY[
            'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80',
            'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80',
            'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80',
            'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=200&q=80'
        ])[1 + (gs % 4)] AS ava
    FROM generate_series(1, 30) AS gs
),
ins_users AS (
    INSERT INTO users (id, email, password, first_name, last_name, gender, avatar_url, is_active, created_at, updated_at)
    OVERRIDING SYSTEM VALUE
    SELECT
        uid,
        'sv.diverse' || lpad(idx::text, 2, '0') || '@student.ctu.edu.vn',
        '$2a$10$5snZONh3FshSppF3fNtoD.N0VHU31/mhWPRzv2FnnOMyvMRBMw3Ee',
        ten,
        ho,
        gt,
        ava,
        TRUE,
        NOW() - ((idx % 30) || ' days')::INTERVAL,
        NOW()
    FROM generated_students
    ON CONFLICT DO NOTHING
    RETURNING id
)
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'STUDENT'
WHERE u.id BETWEEN 501 AND 530
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO student_profiles (id, user_id, student_id, k_number, major_id, is_active, created_at, updated_at)
OVERRIDING SYSTEM VALUE
SELECT
    500 + gs AS id,
    500 + gs AS user_id,
    'B' || (CASE (ARRAY[47, 48, 49, 50])[1 + (gs % 4)] WHEN 47 THEN '21' WHEN 48 THEN '22' WHEN 49 THEN '23' ELSE '24' END)
        || lpad((5000 + gs)::text, 5, '0') AS student_id,
    (ARRAY[47, 48, 49, 50])[1 + (gs % 4)] AS k_number,
    (SELECT m.id FROM majors m ORDER BY m.id OFFSET (gs % GREATEST((SELECT COUNT(*) FROM majors), 1)) LIMIT 1) AS major_id,
    TRUE,
    NOW(),
    NOW()
FROM generate_series(1, 30) AS gs
WHERE EXISTS (SELECT 1 FROM users u WHERE u.id = 500 + gs)
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------------------------
-- 2. ĐĂNG KÝ NGẪU NHIÊN SINH VIÊN VÀO CÁC SỰ KIỆN PUBLISHED 101..116
--    - Giới hạn mỗi sự kiện tối đa ~60%-80% capacity (không vượt quá male/female_quantity)
-- ----------------------------------------------------------------------------
WITH candidate_pairs AS (
    SELECT
        u.id AS user_id,
        u.gender,
        e.id AS event_id,
        e.date_open,
        e.capacity,
        e.gender_constraint,
        e.male_quantity,
        e.female_quantity,
        ROW_NUMBER() OVER (PARTITION BY e.id ORDER BY random()) AS rn_total,
        ROW_NUMBER() OVER (PARTITION BY e.id, u.gender ORDER BY random()) AS rn_gender
    FROM users u
    JOIN events e ON e.id BETWEEN 101 AND 116
    WHERE u.id BETWEEN 501 AND 530
      AND e.status = 'PUBLISHED'
      AND random() < 0.55
)
INSERT INTO registrations (user_id, event_id, reg_date, status, created_at, updated_at)
SELECT
    cp.user_id,
    cp.event_id,
    LEAST(NOW(), cp.date_open + ((1 + floor(random() * 20)::int) * INTERVAL '1 hour')),
    'REGISTERED',
    NOW(),
    NOW()
FROM candidate_pairs cp
WHERE cp.rn_total <= GREATEST(4, LEAST(cp.capacity - 2, 12))
  AND (
      cp.gender_constraint = FALSE
      OR (cp.gender = 'MALE' AND cp.rn_gender <= COALESCE(cp.male_quantity, cp.capacity))
      OR (cp.gender = 'FEMALE' AND cp.rn_gender <= COALESCE(cp.female_quantity, cp.capacity))
  )
ON CONFLICT (user_id, event_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 3. CẬP NHẬT SEQUENCE AN TOÀN
-- ----------------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('users', 'id'),            GREATEST((SELECT COALESCE(MAX(id), 1) FROM users), 530), TRUE);
SELECT setval(pg_get_serial_sequence('student_profiles', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM student_profiles), 530), TRUE);

COMMIT;
