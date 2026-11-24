-- =============================================================================
-- MOCK DATA SCRIPT FOR TESTING POSTMAN & EVENT - SCHOOL REGISTRATION FLOW
-- =============================================================================

-- 0. ĐẢM BẢO CÁC CỘT MỚI TỒN TẠI VÀ KHÔNG XUNG ĐỘT RÀNG BUỘC CŨ
ALTER TABLE IF EXISTS point_categories ADD COLUMN IF NOT EXISTS is_max_child BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE IF EXISTS events ADD COLUMN IF NOT EXISTS school_id BIGINT;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'semesters' AND column_name = 'academic_year'
    ) THEN
        ALTER TABLE semesters ALTER COLUMN academic_year DROP NOT NULL;
        ALTER TABLE semesters ALTER COLUMN academic_year SET DEFAULT '2025-2026';
    END IF;
END $$;

-- 0.1 XÓA SẠCH DỮ LIỆU CŨ THEO THỨ TỰ RÀNG BUỘC KHÓA NGOẠI
TRUNCATE TABLE 
    registrations, 
    stored_events, 
    event_points, 
    events, 
    fanpage_members, 
    fanpage_admin_profiles, 
    fanpages, 
    student_profiles, 
    user_roles, 
    users, 
    majors, 
    schools, 
    associations, 
    semesters, 
    academic_years, 
    criterias, 
    event_types, 
    point_categories, 
    roles 
CASCADE;

-- =============================================================================
-- 1. ROLES
-- =============================================================================
INSERT INTO roles (id, name, description, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'STUDENT', 'Sinh viên', true, NOW(), NOW()),
(2, 'FANPAGE_ADMIN', 'Quản trị viên Ban tổ chức Fanpage', true, NOW(), NOW()),
(3, 'ADMIN', 'Quản trị viên hệ thống', true, NOW(), NOW());

-- =============================================================================
-- 2. SCHOOLS (Trường trực thuộc / Khoa lớn)
-- =============================================================================
INSERT INTO schools (id, name, description, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Trường Công nghệ Thông tin và Truyền thông', 'Trường CNTT & TT - Đại học Cần Thơ', true, NOW(), NOW()),
(2, 'Trường Kinh tế', 'Trường Kinh tế - Đại học Cần Thơ', true, NOW(), NOW()),
(3, 'Trường Nông nghiệp', 'Trường Nông nghiệp (Tạm dừng hoạt động)', false, NOW(), NOW());

-- =============================================================================
-- 3. MAJORS (Ngành học)
-- =============================================================================
INSERT INTO majors (id, code, name, school_id, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'IT', 'Công nghệ thông tin', 1, true, NOW(), NOW()),
(2, 'CS', 'Khoa học máy tính', 1, true, NOW(), NOW()),
(3, 'BA', 'Quản trị kinh doanh', 2, true, NOW(), NOW()),
(4, 'AGRI', 'Kỹ thuật nông nghiệp', 3, false, NOW(), NOW());

-- =============================================================================
-- 4. ASSOCIATIONS (Liên chi hội / Đơn vị quản lý sinh viên)
-- =============================================================================
INSERT INTO associations (id, name, address, description, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Liên chi hội Khoa CNTT', 'Khu II, ĐH Cần Thơ', 'LCH trực thuộc Đoàn khoa CNTT', true, NOW(), NOW()),
(2, 'Liên chi hội Khoa Kinh Tế', 'Khu II, ĐH Cần Thơ', 'LCH trực thuộc Đoàn khoa Kinh tế', true, NOW(), NOW());

-- =============================================================================
-- 5. USERS (Tài khoản người dùng)
-- Tất cả tài khoản có mật khẩu: password123
-- BCrypt Hash: $2y$05$9kbZiV5Affj1cCL8G35HTOfiv1Y51GvWLKubSZPWmRiqRM6QDceb2
-- =============================================================================
INSERT INTO users (id, email, password, first_name, last_name, gender, avatar_url, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'student1@example.com', '$2y$05$9kbZiV5Affj1cCL8G35HTOfiv1Y51GvWLKubSZPWmRiqRM6QDceb2', 'Văn A', 'Nguyễn', 'MALE', 'https://ui-avatars.com/api/?name=Nguyen+Van+A', true, NOW(), NOW()),
(2, 'student2@example.com', '$2y$05$9kbZiV5Affj1cCL8G35HTOfiv1Y51GvWLKubSZPWmRiqRM6QDceb2', 'Thị B', 'Trần', 'FEMALE', 'https://ui-avatars.com/api/?name=Tran+Thi+B', true, NOW(), NOW()),
(3, 'student3@example.com', '$2y$05$9kbZiV5Affj1cCL8G35HTOfiv1Y51GvWLKubSZPWmRiqRM6QDceb2', 'Văn C', 'Lê', 'MALE', 'https://ui-avatars.com/api/?name=Le+Van+C', true, NOW(), NOW()),
(4, 'student4@example.com', '$2y$05$9kbZiV5Affj1cCL8G35HTOfiv1Y51GvWLKubSZPWmRiqRM6QDceb2', 'Thị D', 'Phạm', 'FEMALE', 'https://ui-avatars.com/api/?name=Pham+Thi+D', true, NOW(), NOW()),
(5, 'admin1@example.com', '$2y$05$9kbZiV5Affj1cCL8G35HTOfiv1Y51GvWLKubSZPWmRiqRM6QDceb2', 'Quản trị', 'Fanpage', 'MALE', 'https://ui-avatars.com/api/?name=Admin+Fanpage', true, NOW(), NOW()),
(6, 'superadmin@example.com', '$2y$05$9kbZiV5Affj1cCL8G35HTOfiv1Y51GvWLKubSZPWmRiqRM6QDceb2', 'Quản trị viên', 'Hệ thống', 'OTHER', 'https://ui-avatars.com/api/?name=System+Admin', true, NOW(), NOW());

-- =============================================================================
-- 6. USER_ROLES
-- =============================================================================
INSERT INTO user_roles (user_id, role_id, is_active, created_at, updated_at) VALUES
(1, 1, true, NOW(), NOW()), -- student1: STUDENT
(2, 1, true, NOW(), NOW()), -- student2: STUDENT
(3, 1, true, NOW(), NOW()), -- student3: STUDENT
(4, 1, true, NOW(), NOW()), -- student4: STUDENT
(5, 2, true, NOW(), NOW()), -- admin1: FANPAGE_ADMIN
(6, 3, true, NOW(), NOW()); -- superadmin: ADMIN

-- =============================================================================
-- 7. STUDENT_PROFILES
--   - User 1: Major 1 (IT -> School 1: CNTT & TT)
--   - User 2: Major 3 (BA -> School 2: Kinh tế)
--   - User 3: Major NULL (Chưa chọn ngành/trường)
--   - User 4: Chưa tạo hồ sơ sinh viên
-- =============================================================================
INSERT INTO student_profiles (id, student_id, k_number, major_id, user_id, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'B2100001', 47, 1, 1, true, NOW(), NOW()),
(2, 'B2100002', 47, 3, 2, true, NOW(), NOW()),
(3, 'B2100003', 47, NULL, 3, true, NOW(), NOW());

-- =============================================================================
-- 8. FANPAGE_ADMIN_PROFILES
-- =============================================================================
INSERT INTO fanpage_admin_profiles (id, staff_id, org_name, title, dob, phone_number, user_id, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'STAFF001', 'CLB Tin Học', 'Trưởng ban', '1995-05-15', '0912345678', 5, true, NOW(), NOW());

-- =============================================================================
-- 9. FANPAGES
-- =============================================================================
INSERT INTO fanpages (id, name, description, avatar_url, status, org_type, org_name, parent_org, help_contact, help_phone, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Fanpage Sự Kiện IT', 'Fanpage CLB Công Nghệ Thông Tin & Truyền Thông', 'https://ui-avatars.com/api/?name=IT', 'ACTIVE', 'CLUB', 'CLB Tin Học', 'Khoa CNTT', 'clb.it@ctu.edu.vn', '0912345678', true, NOW(), NOW()),
(2, 'Fanpage Khoa Kinh Tế', 'Fanpage Ban Chấp Hành Đoàn Trường Kinh Tế', 'https://ui-avatars.com/api/?name=KT', 'ACTIVE', 'FACULTY', 'Đoàn Trường Kinh Tế', 'Trường Kinh Tế', 'doan.kt@ctu.edu.vn', '0912345679', true, NOW(), NOW());

-- =============================================================================
-- 10. FANPAGE_MEMBERS
-- =============================================================================
INSERT INTO fanpage_members (fanpage_id, user_id, role, is_active, created_at, updated_at) VALUES
(1, 5, 'ADMIN', true, NOW(), NOW()),
(2, 5, 'MEMBER', true, NOW(), NOW());

-- =============================================================================
-- 11. ACADEMIC_YEARS & SEMESTERS
-- =============================================================================
INSERT INTO academic_years (id, code, start_date, end_date, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, '2025-2026', '2025-09-01', '2026-08-31', true, NOW(), NOW());

INSERT INTO semesters (id, number, academic_year_id, start_date, end_date, status, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 1, 1, '2025-09-01', '2026-01-31', 'ONGOING', true, NOW(), NOW()),
(2, 2, 1, '2026-02-01', '2026-06-30', 'UPCOMING', true, NOW(), NOW());

-- =============================================================================
-- 12. CRITERIAS, EVENT_TYPES, POINT_CATEGORIES
-- =============================================================================
INSERT INTO criterias (id, name, note, scope, level, children_order, required_children, parent_id, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Sinh viên 5 tốt', 'Tiêu chuẩn xét danh hiệu SV 5 Tốt', 'Toàn trường', 0, 1, NULL, NULL, true, NOW(), NOW()),
(2, 'Hoạt động phong trào', 'Tham gia các hoạt động cộng đồng', 'Cấp khoa/trường', 0, 2, NULL, NULL, true, NOW(), NOW());

INSERT INTO event_types (id, name, description, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Hội thảo / Seminar', 'Các buổi báo cáo chuyên đề học thuật', true, NOW(), NOW()),
(2, 'Cuộc thi học thuật', 'Các cuộc thi lập trình, Hackathon, giải thưởng', true, NOW(), NOW()),
(3, 'Ngày hội giao lưu', 'Ngày hội việc làm, văn hóa, thể thao', true, NOW(), NOW());

INSERT INTO point_categories (id, name, note, maximum, date_apply, level, children_order, is_max_child, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Điểm rèn luyện', 'ĐRL tích lũy học kỳ', 100, '2025-09-01', 0, 1, false, true, NOW(), NOW()),
(2, 'Điểm CTXH', 'Điểm công tác xã hội', 100, '2025-09-01', 0, 2, false, true, NOW(), NOW());

-- =============================================================================
-- 13. EVENTS (Dữ liệu sự kiện đa dạng các kịch bản)
-- =============================================================================
INSERT INTO events (
    id, name, description, location, date_open, date_close, date_happen,
    capacity, male_quantity, female_quantity, banner_url, status,
    event_type_id, criteria_id, fanpage_id, semester_id, school_id, is_active, created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES
-- Event 1: Chỉ cho sinh viên Trường CNTT & TT (school_id = 1), đang mở đăng ký
(1, 'Hội thảo Trí Tuệ Nhân Tạo & Điện Toán Đám Mây 2026', 
    'Hội thảo chuyên sâu về AI, ML và Cloud Native cho sinh viên Trường CNTT & TT.', 
    'Hội trường A - Trường CNTT & TT', 
    '2025-01-01 00:00:00+00', '2027-12-31 23:59:59+00', '2028-01-05 08:00:00+00', 
    100, 50, 50, 'https://images.unsplash.com/photo-1518770660439-4636190af475', 'PUBLISHED', 
    1, 1, 1, 1, 1, true, NOW(), NOW()),

-- Event 2: Chỉ cho sinh viên Trường Kinh Tế (school_id = 2), đang mở đăng ký
(2, 'Cuộc thi Khởi nghiệp Sinh viên Kinh tế 2026', 
    'Sân chơi khởi nghiệp sáng tạo dành riêng cho sinh viên Trường Kinh Tế.', 
    'Hội trường Lớn - Trường Kinh Tế', 
    '2025-01-01 00:00:00+00', '2027-12-31 23:59:59+00', '2028-02-10 08:30:00+00', 
    150, 75, 75, 'https://images.unsplash.com/photo-1559136555-9303baea8ebd', 'PUBLISHED', 
    2, 1, 2, 1, 2, true, NOW(), NOW()),

-- Event 3: Mở tự do cho toàn trường (school_id = NULL), đang mở đăng ký
(3, 'Ngày hội Việc làm & Kết nối Doanh nghiệp 2026', 
    'Cơ hội gặp gỡ hơn 50 doanh nghiệp hàng đầu mở cho tất cả sinh viên mọi trường.', 
    'Nhà thi đấu Đa năng - Đại học Cần Thơ', 
    '2025-01-01 00:00:00+00', '2027-12-31 23:59:59+00', '2028-03-15 07:30:00+00', 
    500, 250, 250, 'https://images.unsplash.com/photo-1511578314322-379afb476865', 'PUBLISHED', 
    3, 2, 1, 1, NULL, true, NOW(), NOW()),

-- Event 4: Sự kiện đã hết hạn đăng ký (date_close trong quá khứ)
(4, 'Tập huấn Kỹ năng Mềm (Đã đóng đăng ký)', 
    'Khóa tập huấn kỹ năng thuyết trình và làm việc nhóm.', 
    'Phòng chuyên đề 1', 
    '2024-01-01 00:00:00+00', '2024-02-01 00:00:00+00', '2024-02-05 08:00:00+00', 
    50, 25, 25, 'https://images.unsplash.com/photo-1524178232363-1fb2b075b655', 'PUBLISHED', 
    1, 2, 1, 1, 1, true, NOW(), NOW()),

-- Event 5: Sự kiện trạng thái bản nháp DRAFT (chưa công bố)
(5, 'Giải đấu Thể thao Điện tử E-Sports (Bản nháp)', 
    'Giải đấu Liên Quân và Valorant sinh viên.', 
    'Sảnh TTTT', 
    '2025-01-01 00:00:00+00', '2027-12-31 23:59:59+00', '2028-04-01 08:00:00+00', 
    64, 32, 32, 'https://images.unsplash.com/photo-1542751371-adc38448a05e', 'DRAFT', 
    3, 2, 1, 1, 1, true, NOW(), NOW()),

-- Event 6: Sự kiện đã bị vô hiệu hóa (is_active = false)
(6, 'Hội thảo Blockchain (Đã ẩn / Vô hiệu hóa)', 
    'Tổng quan Web3 và Smart Contract.', 
    'Online qua Zoom', 
    '2025-01-01 00:00:00+00', '2027-12-31 23:59:59+00', '2028-05-01 08:00:00+00', 
    200, 100, 100, 'https://images.unsplash.com/photo-1639762681485-074b7f938ba0', 'PUBLISHED', 
    1, 1, 1, 1, 1, false, NOW(), NOW());

-- =============================================================================
-- 14. EVENT_POINTS
-- =============================================================================
INSERT INTO event_points (id, event_id, point_category_id, point, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 1, 1, 10, true, NOW(), NOW()),
(2, 2, 1, 15, true, NOW(), NOW()),
(3, 3, 2, 20, true, NOW(), NOW());

-- =============================================================================
-- 15. REGISTRATIONS (Dữ liệu đăng ký sẵn có để test các nhánh logic đặc biệt)
-- =============================================================================
INSERT INTO registrations (user_id, event_id, reg_date, status, reason, created_at, updated_at) VALUES
-- Student 1 (user_id = 1) đã từng hủy đăng ký Event 2 -> Test tái đăng ký (re-register CANCELLED -> REGISTERED)
(1, 2, NOW() - INTERVAL '1 day', 'CANCELLED', 'Bận lịch học đột xuất', NOW() - INTERVAL '1 day', NOW()),

-- Student 2 (user_id = 2) đã đăng ký Event 3 thành công -> Test trùng lặp (Duplicate registration -> 409 Conflict)
(2, 3, NOW() - INTERVAL '2 hours', 'REGISTERED', NULL, NOW() - INTERVAL '2 hours', NOW());

-- =============================================================================
-- 16. ĐỒNG BỘ IDENTITY SEQUENCES TRÁNH LỖI DUPLICATE KEY KHI INSERT MỚI
-- =============================================================================
DO $$
DECLARE
    rec RECORD;
    seq TEXT;
BEGIN
    FOR rec IN 
        SELECT table_name, column_name 
        FROM information_schema.columns 
        WHERE table_schema = 'public' 
          AND column_name = 'id'
          AND is_identity = 'YES'
    LOOP
        seq := pg_get_serial_sequence(rec.table_name, rec.column_name);
        IF seq IS NOT NULL THEN
            EXECUTE format('SELECT setval(%L, COALESCE((SELECT MAX(%I) FROM %I), 1))', seq, rec.column_name, rec.table_name);
        END IF;
    END LOOP;
END $$;
