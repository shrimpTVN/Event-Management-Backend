-- Xóa sạch dữ liệu cũ và reset ID (Chạy riêng cái này nếu DDL auto k reset)
-- TRUNCATE TABLE roles, users, schools, majors, associations, student_profiles, fanpages, fanpage_members, semesters, criterias, event_types, point_categories, events, event_points, registrations CASCADE;

-- 1. ROLES
INSERT INTO roles (id, name, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'STUDENT', true, NOW(), NOW()),
(2, 'FANPAGE_ADMIN', true, NOW(), NOW()),
(3, 'ADMIN', true, NOW(), NOW());

-- 2. TRƯỜNG HỌC, NGÀNH HỌC, LIÊN CHI HỘI
INSERT INTO schools (id, name, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Đại học Bách Khoa', true, NOW(), NOW());

INSERT INTO majors (id, name, code, school_id, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Công nghệ thông tin', 'IT', 1, true, NOW(), NOW());

INSERT INTO associations (id, name, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Liên chi hội Khoa CNTT', true, NOW(), NOW());

-- 3. USERS (Mật khẩu mặc định là: password123)
-- Hash bcrypt của 'password123' là '$2a$12$R.K1q2l6M1L/6T8N3R4I/eF.9yD3Qp/tW3Z1M8O0L/V1R7F1L/y3S'
INSERT INTO users (id, email, password, role_id, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'student1@example.com', '$2y$05$9kbZiV5Affj1cCL8G35HTOfiv1Y51GvWLKubSZPWmRiqRM6QDceb2', 1, true, NOW(), NOW()),
(2, 'student2@example.com', '$2y$05$9kbZiV5Affj1cCL8G35HTOfiv1Y51GvWLKubSZPWmRiqRM6QDceb2', 1, true, NOW(), NOW()),
(3, 'admin1@example.com', '$2y$05$9kbZiV5Affj1cCL8G35HTOfiv1Y51GvWLKubSZPWmRiqRM6QDceb2', 2, true, NOW(), NOW());

-- 4. HỒ SƠ SINH VIÊN VÀ ADMIN
INSERT INTO student_profiles (user_id, student_id, first_name, last_name, avatar_url, major_id, association_id, is_active, created_at, updated_at) VALUES
(1, 'SV001', 'Nguyễn Văn', 'A', 'https://ui-avatars.com/api/?name=NVA', 1, 1, true, NOW(), NOW()),
(2, 'SV002', 'Trần Thị', 'B', 'https://ui-avatars.com/api/?name=TTB', 1, 1, true, NOW(), NOW());

INSERT INTO fanpage_admin_profiles (user_id, staff_id, first_name, last_name, phone_number, is_active, created_at, updated_at) VALUES
(3, 'STAFF01', 'Quản trị viên', 'Fanpage', '0999999999', true, NOW(), NOW());

-- 5. FANPAGES
INSERT INTO fanpages (id, name, org_type, org_name, parent_org, status, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Fanpage Sự Kiện IT', 'CLUB', 'CLB Công Nghệ IT', 'Khoa CNTT', 'ACTIVE', true, NOW(), NOW()),
(2, 'Fanpage Mùa Hè Xanh', 'ASSOCIATION', 'LCH CNTT', 'Khoa CNTT', 'ACTIVE', true, NOW(), NOW());

-- 6. THÀNH VIÊN FANPAGE
INSERT INTO fanpage_members (user_id, fanpage_id, is_active, created_at, updated_at) VALUES
(3, 1, true, NOW(), NOW()),
(3, 2, true, NOW(), NOW());

-- 7. DANH MỤC THÔNG TIN SỰ KIỆN (Kỳ học, tiêu chí, loại sự kiện, điểm)
INSERT INTO semesters (id, number, academic_year, start_date, end_date, status, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 1, '2026-2027', '2026-09-01', '2026-12-31', 'UPCOMING', true, NOW(), NOW()),
(2, 2, '2026-2027', '2027-01-01', '2027-05-31', 'UPCOMING', true, NOW(), NOW());

INSERT INTO criterias (id, name, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Sinh viên 5 tốt', true, NOW(), NOW()),
(2, 'Hoạt động cộng đồng', true, NOW(), NOW());

INSERT INTO event_types (id, name, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Hội thảo / Seminar', true, NOW(), NOW()),
(2, 'Chiến dịch tình nguyện', true, NOW(), NOW());

INSERT INTO point_categories (id, name, note, maximum, date_apply, level, children_order, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Điểm rèn luyện chung', '', 100, '2026-01-01', 0, 1, true, NOW(), NOW()),
(2, 'Điểm CTXH', '', 100, '2026-01-01', 0, 2, true, NOW(), NOW());

-- 8. SỰ KIỆN
INSERT INTO events (
    id, name, description, location, date_open, date_close, date_happen, 
    capacity, male_quantity, female_quantity, banner_url, status, 
    event_type_id, criteria_id, fanpage_id, semester_id, is_active, created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES 
(1, 'Hội thảo AI 2026', 'Chia sẻ về AI', 'Hội trường A', '2024-01-01 00:00:00', '2027-12-31 00:00:00', '2028-01-01 00:00:00', 100, 50, 50, 'https://img.com/ai.jpg', 'PUBLISHED', 1, 1, 1, 1, true, NOW(), NOW()),
(2, 'Mùa hè xanh 2026', 'Dọn rác bãi biển', 'Bãi biển Vũng Tàu', '2024-01-01 00:00:00', '2027-12-31 00:00:00', '2028-01-01 00:00:00', 200, 100, 100, 'https://img.com/mhx.jpg', 'PUBLISHED', 2, 2, 2, 1, true, NOW(), NOW()),
(3, 'Cuộc thi Hackathon', 'Code xuyên đêm', 'Phòng máy số 1', '2028-01-01 00:00:00', '2028-02-01 00:00:00', '2028-02-05 00:00:00', 50, 25, 25, 'https://img.com/hack.jpg', 'DRAFT', 1, 1, 1, 1, true, NOW(), NOW());

-- 9. ĐIỂM SỰ KIỆN
INSERT INTO event_points (id, event_id, point_category_id, point, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 1, 1, 15, true, NOW(), NOW()),
(2, 2, 2, 20, true, NOW(), NOW()),
(3, 3, 1, 30, true, NOW(), NOW());

-- 10. ĐĂNG KÝ (REGISTRATION) - Dành cho student1
INSERT INTO registrations (event_id, user_id, status, reg_date, created_at, updated_at) VALUES
(2, 1, 'REGISTERED', NOW(), NOW(), NOW());

-- 11. ĐỒNG BỘ IDENTITY SEQUENCES TRÁNH LỖI DUPLICATE KEY
DO $$
DECLARE
    rec RECORD;
    seq TEXT;
BEGIN
    FOR rec IN 
        SELECT table_name, column_name 
        FROM information_schema.columns 
        WHERE table_schema = 'public' AND column_name = 'id'
    LOOP
        seq := pg_get_serial_sequence(rec.table_name, rec.column_name);
        IF seq IS NOT NULL THEN
            EXECUTE format('SELECT setval(%L, COALESCE((SELECT MAX(%I) FROM %I), 1))', seq, rec.column_name, rec.table_name);
        END IF;
    END LOOP;
END $$;
