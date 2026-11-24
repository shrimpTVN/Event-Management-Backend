-- ==============================================================================
-- SEED DATA ĐỂ TEST TÍNH NĂNG GỢI Ý SỰ KIỆN TÍCH HỢP AI (RECOMMENDATION TEST DATA)
-- Khớp 100% với PostgreSQL Database Schema thực tế của dự án.
-- ==============================================================================

-- 1. XÓA SẠCH DỮ LIỆU CŨ VÀ RESET TOÀN BỘ CÁC BẢNG CÓ LIÊN QUAN
TRUNCATE TABLE 
    registrations, 
    event_points, 
    events, 
    point_transactions, 
    student_criteria_transactions, 
    student_criteria_progress, 
    student_semester_points, 
    point_categories, 
    criterias, 
    event_types, 
    fanpage_members, 
    followings, 
    stored_events, 
    fanpages, 
    semesters, 
    academic_years, 
    student_profiles, 
    user_roles, 
    users, 
    roles, 
    associations, 
    majors, 
    schools CASCADE;

-- 2. ROLES
INSERT INTO roles (id, name, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'STUDENT', true, NOW(), NOW()),
(2, 'FANPAGE_ADMIN', true, NOW(), NOW()),
(3, 'ADMIN', true, NOW(), NOW());

-- 3. TRƯỜNG, KHOA/NGÀNH, CHI HỘI
INSERT INTO schools (id, name, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Đại học Bách Khoa', true, NOW(), NOW());

INSERT INTO majors (id, name, code, school_id, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Công nghệ thông tin', 'IT', 1, true, NOW(), NOW());

INSERT INTO associations (id, name, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Liên chi hội Khoa CNTT', true, NOW(), NOW());

-- 4. USERS (Mật khẩu mặc định: password123)
-- Hash bcrypt của 'password123' là '$2y$05$9kbZiV5Affj1cCL8G35HTOfiv1Y51GvWLKubSZPWmRiqRM6QDceb2'
INSERT INTO users (id, email, password, first_name, last_name, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'student1@example.com', '$2y$05$9kbZiV5Affj1cCL8G35HTOfiv1Y51GvWLKubSZPWmRiqRM6QDceb2', 'Văn A', 'Nguyễn', true, NOW(), NOW()),
(2, 'student2@example.com', '$2y$05$9kbZiV5Affj1cCL8G35HTOfiv1Y51GvWLKubSZPWmRiqRM6QDceb2', 'Thị B', 'Trần', true, NOW(), NOW()),
(3, 'admin1@example.com', '$2y$05$9kbZiV5Affj1cCL8G35HTOfiv1Y51GvWLKubSZPWmRiqRM6QDceb2', 'Quản trị', 'Viên', true, NOW(), NOW());

INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1),
(2, 1),
(3, 2);

-- 5. HỒ SƠ SINH VIÊN
INSERT INTO student_profiles (id, user_id, student_id, k_number, major_id, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 1, 'SV001', 47, 1, true, NOW(), NOW()),
(2, 2, 'SV002', 47, 1, true, NOW(), NOW());

-- 6. NĂM HỌC & HỌC KỲ HIỆN TẠI (ONGOING)
INSERT INTO academic_years (id, code, start_date, end_date, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, '2026-2027', CURRENT_DATE - INTERVAL '30 DAYS', CURRENT_DATE + INTERVAL '300 DAYS', true, NOW(), NOW());

INSERT INTO semesters (id, number, academic_year, academic_year_id, start_date, end_date, status, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 1, '2026-2027', 1, CURRENT_DATE - INTERVAL '30 DAYS', CURRENT_DATE + INTERVAL '120 DAYS', 'ONGOING', true, NOW(), NOW());

-- 7. DANH MỤC ĐIỂM RÈN LUYỆN (Tối đa 25 điểm mỗi danh mục)
INSERT INTO point_categories (id, name, maximum, level, children_order, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'I. Đạo đức và Lối sống', 25, 0, 1, true, NOW(), NOW()),
(2, 'II. Học tập và Nghiên cứu khoa học', 25, 0, 2, true, NOW(), NOW()),
(3, 'III. Tình nguyện và Hoạt động xã hội', 25, 0, 3, true, NOW(), NOW()),
(4, 'IV. Kỹ năng và Thể thao', 25, 0, 4, true, NOW(), NOW());

-- 8. TIÊU CHÍ SINH VIÊN 5 TỐT (SV5T)
INSERT INTO criterias (id, name, level, children_order, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Tiêu chuẩn Đạo đức tốt', 1, 1, true, NOW(), NOW()),
(2, 'Tiêu chuẩn Học tập tốt', 1, 2, true, NOW(), NOW()),
(3, 'Tiêu chuẩn Tình nguyện tốt', 1, 3, true, NOW(), NOW()),
(4, 'Tiêu chuẩn Thể lực tốt', 1, 4, true, NOW(), NOW()),
(5, 'Tiêu chuẩn Hội nhập tốt', 1, 5, true, NOW(), NOW());

-- 9. FANPAGES & EVENT TYPES
INSERT INTO fanpages (id, name, org_type, org_name, parent_org, status, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'CLB Học thuật & AI Bách Khoa', 'CLUB', 'CLB AI', 'Khoa CNTT', 'ACTIVE', true, NOW(), NOW()),
(2, 'Đội Tình nguyện Mùa Hè Xanh', 'ASSOCIATION', 'LCH CNTT', 'Khoa CNTT', 'ACTIVE', true, NOW(), NOW());

INSERT INTO event_types (id, name, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Hội thảo / Tọa đàm học thuật', true, NOW(), NOW()),
(2, 'Chiến dịch tình nguyện', true, NOW(), NOW()),
(3, 'Hội thao sinh viên', true, NOW(), NOW());

-- ==============================================================================
-- 10. THIẾT LẬP DỮ LIỆU ĐIỂM VÀ TIÊU CHÍ HIỆN TẠI CHO 2 SINH VIÊN
-- ==============================================================================

-- Sinh viên 1 (student1): 
--   - Đã max điểm Tình nguyện (25/25) và đã đạt tiêu chí "Tình nguyện tốt" (Crit 3).
--   - ĐANG THIẾU: Điểm Học tập mới có 5/25 (THIẾU 20 ĐIỂM) và CHƯA đạt tiêu chí "Học tập tốt" (Crit 2).
INSERT INTO student_semester_points (user_id, semester_id, point_category_id, total_point, created_at, updated_at) VALUES
(1, 1, 1, 20, NOW(), NOW()), -- Đạo đức: 20/25 (thiếu 5)
(1, 1, 2, 5, NOW(), NOW()),  -- Học tập: 5/25 (THIẾU 20 ĐIỂM)
(1, 1, 3, 25, NOW(), NOW()), -- Tình nguyện: 25/25 (ĐÃ MAX)
(1, 1, 4, 15, NOW(), NOW()); -- Kỹ năng: 15/25 (thiếu 10)

INSERT INTO student_criteria_progress (user_id, criteria_id, academic_year_id, is_completed, completion_source, is_active, created_at, updated_at, completed_children_count) VALUES
(1, 3, 1, true, 'EVENT_CHECKOUT', true, NOW(), NOW(), 0);

-- Sinh viên 2 (student2):
--   - Đã max điểm Học tập (25/25) và đã đạt tiêu chí "Học tập tốt" (Crit 2).
--   - ĐANG THIẾU: Điểm Tình nguyện mới có 0/25 (THIẾU 25 ĐIỂM) và CHƯA đạt tiêu chí "Tình nguyện tốt" (Crit 3).
INSERT INTO student_semester_points (user_id, semester_id, point_category_id, total_point, created_at, updated_at) VALUES
(2, 1, 1, 20, NOW(), NOW()), -- Đạo đức: 20/25 (thiếu 5)
(2, 1, 2, 25, NOW(), NOW()), -- Học tập: 25/25 (ĐÃ MAX)
(2, 1, 3, 0, NOW(), NOW()),  -- Tình nguyện: 0/25 (THIẾU 25 ĐIỂM)
(2, 1, 4, 15, NOW(), NOW()); -- Kỹ năng: 15/25 (thiếu 10)

INSERT INTO student_criteria_progress (user_id, criteria_id, academic_year_id, is_completed, completion_source, is_active, created_at, updated_at, completed_children_count) VALUES
(2, 2, 1, true, 'EVENT_CHECKOUT', true, NOW(), NOW(), 0);


-- ==============================================================================
-- 11. CÁC SỰ KIỆN SẮP DIỄN RA (EVENTS POOL)
-- ==============================================================================

-- SỰ KIỆN 1: Workshop AI & Nghiên cứu khoa học
--   - Diễn ra: Sau 2 ngày (KHẨN CẤP!)
--   - Cộng 20 điểm Học tập (Cat 2) + Tiêu chí "Học tập tốt" (Crit 2)
--   - Còn 45 slot (Capacity 50)
--   ==> KẾT QUẢ MONG ĐỢI: TOP 1 CHO STUDENT 1 (Điểm rất cao vì bù 20 điểm học tập + đạt SV5T + khẩn cấp 2 ngày).
--   ==> Với Student 2: Điểm thấp vì student 2 đã max điểm học tập và đã đạt tiêu chí này.
INSERT INTO events (
    id, name, description, location, date_open, date_close, date_happen, 
    capacity, male_quantity, female_quantity, banner_url, status, 
    event_type_id, criteria_id, fanpage_id, semester_id, is_active, created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES
(1, 'Workshop: Lập trình AI & Nghiên cứu Khoa học 2026', 
    'Chia sẻ kinh nghiệm làm NCKH, viết bài báo quốc tế và ứng dụng thực tiễn của Generative AI.', 
    'Hội trường lớn A - Khu 2', 
    NOW() - INTERVAL '5 DAYS', NOW() + INTERVAL '1 DAYS', NOW() + INTERVAL '2 DAYS', 
    50, 25, 25, 'https://images.unsplash.com/photo-1485827404703-89b55fcc595e', 'PUBLISHED', 
    1, 2, 1, 1, true, NOW(), NOW());

INSERT INTO event_points (event_id, point_category_id, point, is_active, created_at, updated_at) VALUES
(1, 2, 20, true, NOW(), NOW());


-- SỰ KIỆN 2: Chiến dịch Tình nguyện Mùa hè xanh & Dọn rác bãi biển
--   - Diễn ra: Sau 3 ngày (KHẨN CẤP!)
--   - Cộng 20 điểm Tình nguyện (Cat 3) + Tiêu chí "Tình nguyện tốt" (Crit 3)
--   - Còn 90 slot (Capacity 100)
--   ==> KẾT QUẢ MONG ĐỢI: TOP 1 CHO STUDENT 2 (Điểm rất cao vì bù 20 điểm tình nguyện + đạt SV5T + khẩn cấp 3 ngày).
--   ==> Với Student 1: Điểm thấp vì student 1 đã max điểm tình nguyện và đã đạt tiêu chí này.
INSERT INTO events (
    id, name, description, location, date_open, date_close, date_happen, 
    capacity, male_quantity, female_quantity, banner_url, status, 
    event_type_id, criteria_id, fanpage_id, semester_id, is_active, created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES
(2, 'Chiến dịch: Mùa Hè Xanh & Ngày Thứ Bảy Tình Nguyện', 
    'Thu gom rác thải bãi biển, trồng cây xanh và hỗ trợ trẻ em khó khăn tại địa phương.', 
    'Bãi biển Cần Giờ & Khu dân cư', 
    NOW() - INTERVAL '5 DAYS', NOW() + INTERVAL '2 DAYS', NOW() + INTERVAL '3 DAYS', 
    100, 50, 50, 'https://images.unsplash.com/photo-1559027615-cd4628902d4a', 'PUBLISHED', 
    2, 3, 2, 1, true, NOW(), NOW());

INSERT INTO event_points (event_id, point_category_id, point, is_active, created_at, updated_at) VALUES
(2, 3, 20, true, NOW(), NOW());


-- SỰ KIỆN 3: Giải chạy Sinh viên Khỏe - Rèn luyện thể lực
--   - Diễn ra: Sau 5 ngày
--   - Cộng 10 điểm Thể thao (Cat 4) + Tiêu chí "Thể lực tốt" (Crit 4)
--   - Cả 2 sinh viên đều thiếu điểm thể thao & tiêu chí này nên đều được gợi ý ở thứ hạng kế tiếp.
INSERT INTO events (
    id, name, description, location, date_open, date_close, date_happen, 
    capacity, male_quantity, female_quantity, banner_url, status, 
    event_type_id, criteria_id, fanpage_id, semester_id, is_active, created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES
(3, 'Giải chạy Marathon Sinh Viên Khỏe 2026', 
    'Chạy cự ly 5km vòng quanh khuôn viên trường nhằm nâng cao thể chất và tinh thần thể thao.', 
    'Sân vận động trung tâm', 
    NOW() - INTERVAL '5 DAYS', NOW() + INTERVAL '4 DAYS', NOW() + INTERVAL '5 DAYS', 
    80, 40, 40, 'https://images.unsplash.com/photo-1452626038306-9aae5e071dd3', 'PUBLISHED', 
    3, 4, 1, 1, true, NOW(), NOW());

INSERT INTO event_points (event_id, point_category_id, point, is_active, created_at, updated_at) VALUES
(3, 4, 10, true, NOW(), NOW());


-- SỰ KIỆN 4: Hội thảo Khoa học tương lai (Diễn ra xa: 45 ngày tới)
--   - Cũng cộng điểm Học tập (Cat 2) nhưng còn tận 45 ngày nữa mới diễn ra!
--   ==> MINH HỌA ĐỘ KHẨN CẤP: Cho Student 1, Sự kiện 1 (sau 2 ngày) sẽ xếp cao hơn hẳn Sự kiện 4 (sau 45 ngày) dù cùng mục học tập.
INSERT INTO events (
    id, name, description, location, date_open, date_close, date_happen, 
    capacity, male_quantity, female_quantity, banner_url, status, 
    event_type_id, criteria_id, fanpage_id, semester_id, is_active, created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES
(4, 'Hội thảo Khoa học: Xu hướng Công nghệ & Điện toán đám mây', 
    'Tổng quan về kiến trúc Cloud Native và Microservices hiện đại.', 
    'Phòng hội thảo tầng 5', 
    NOW() - INTERVAL '2 DAYS', NOW() + INTERVAL '40 DAYS', NOW() + INTERVAL '45 DAYS', 
    100, 50, 50, 'https://images.unsplash.com/photo-1544652478-6653e09f18a2', 'PUBLISHED', 
    1, 2, 1, 1, true, NOW(), NOW());

INSERT INTO event_points (event_id, point_category_id, point, is_active, created_at, updated_at) VALUES
(4, 2, 15, true, NOW(), NOW());


-- SỰ KIỆN 5: Tọa đàm kín (ĐÃ HẾT SLOT)
--   - Capacity: 1, đã có 1 người đăng ký -> Còn 0 slot!
--   ==> MINH HỌA BỘ LỌC DUNG LƯỢNG: Không sinh viên nào thấy sự kiện này trong danh sách gợi ý.
INSERT INTO events (
    id, name, description, location, date_open, date_close, date_happen, 
    capacity, male_quantity, female_quantity, banner_url, status, 
    event_type_id, criteria_id, fanpage_id, semester_id, is_active, created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES
(5, 'Tọa đàm kín: Cơ hội thực tập độc quyền cùng CTO (FULL SLOT)', 
    'Buổi gặp gỡ định hướng nghề nghiệp giới hạn số lượng.', 
    'Phòng họp VIP', 
    NOW() - INTERVAL '5 DAYS', NOW() + INTERVAL '1 DAYS', NOW() + INTERVAL '2 DAYS', 
    1, 1, 0, 'https://images.unsplash.com/photo-1517245386807-bb43f82c33c4', 'PUBLISHED', 
    1, 2, 1, 1, true, NOW(), NOW());

INSERT INTO registrations (event_id, user_id, status, reg_date, created_at, updated_at) VALUES
(5, 3, 'REGISTERED', NOW(), NOW(), NOW());


-- SỰ KIỆN 6: Ngày hội Văn hóa và Kỹ năng giao tiếp
--   - Capacity: 50, Sinh viên 1 ĐÃ ĐĂNG KÝ từ trước!
--   ==> MINH HỌA BỘ LỌC ĐÃ ĐĂNG KÝ: Student 1 KHÔNG thấy sự kiện này, nhưng Student 2 SẼ THẤY!
INSERT INTO events (
    id, name, description, location, date_open, date_close, date_happen, 
    capacity, male_quantity, female_quantity, banner_url, status, 
    event_type_id, criteria_id, fanpage_id, semester_id, is_active, created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES
(6, 'Ngày hội Văn hóa & Kỹ năng Giao tiếp tự tin', 
    'Rèn luyện kỹ năng thuyết trình, giao tiếp và ứng xử chuyên nghiệp trong môi trường đại học.', 
    'Hội trường C', 
    NOW() - INTERVAL '5 DAYS', NOW() + INTERVAL '3 DAYS', NOW() + INTERVAL '4 DAYS', 
    50, 25, 25, 'https://images.unsplash.com/photo-1528605248644-14dd04022da1', 'PUBLISHED', 
    1, 1, 1, 1, true, NOW(), NOW());

INSERT INTO event_points (event_id, point_category_id, point, is_active, created_at, updated_at) VALUES
(6, 1, 10, true, NOW(), NOW());

-- Student 1 đã đăng ký sự kiện 6
INSERT INTO registrations (event_id, user_id, status, reg_date, created_at, updated_at) VALUES
(6, 1, 'REGISTERED', NOW(), NOW(), NOW());


-- ==============================================================================
-- 11. DỮ LIỆU HÀNH VI: FOLLOW FANPAGE & LỊCH SỬ THAM GIA (BEHAVIORAL DATA)
-- ==============================================================================

-- Sinh viên 1 follow CLB Học thuật & AI Bách Khoa (Fanpage 1)
-- Sinh viên 2 follow Đội Tình nguyện Mùa Hè Xanh (Fanpage 2)
INSERT INTO followings (fanpage_id, user_id, fol_status, is_active, created_at, updated_at) VALUES
(1, 1, 'FOLLOWING', true, NOW(), NOW()),
(2, 2, 'FOLLOWING', true, NOW(), NOW());

-- SỰ KIỆN QUÁ KHỨ 10: Hội thảo AI Mùa xuân (Đã diễn ra 30 ngày trước, Type 1 - Học thuật, Cat 2 - Học tập)
INSERT INTO events (
    id, name, description, location, date_open, date_close, date_happen, 
    capacity, male_quantity, female_quantity, banner_url, status, 
    event_type_id, criteria_id, fanpage_id, semester_id, is_active, created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES
(10, 'Hội thảo AI Mùa xuân 2026 (Quá khứ)', 
    'Chia sẻ kiến thức AI căn bản.', 
    'Hội trường A', 
    NOW() - INTERVAL '40 DAYS', NOW() - INTERVAL '31 DAYS', NOW() - INTERVAL '30 DAYS', 
    100, 50, 50, 'https://images.unsplash.com/photo-1485827404703-89b55fcc595e', 'PUBLISHED', 
    1, 2, 1, 1, true, NOW(), NOW());

INSERT INTO event_points (event_id, point_category_id, point, is_active, created_at, updated_at) VALUES
(10, 2, 5, true, NOW(), NOW());

-- Sinh viên 1 đã tham gia và hoàn thành sự kiện quá khứ 10 (Type 1, Cat 2)
INSERT INTO registrations (event_id, user_id, status, reg_date, check_in_at, check_out_at, created_at, updated_at) VALUES
(10, 1, 'COMPLETED', NOW() - INTERVAL '35 DAYS', NOW() - INTERVAL '30 DAYS', NOW() - INTERVAL '30 DAYS', NOW(), NOW());


-- SỰ KIỆN QUÁ KHỨ 11: Ngày thứ Bảy Xanh Mùa Xuân (Đã diễn ra 25 ngày trước, Type 2 - Tình nguyện, Cat 3 - Tình nguyện)
INSERT INTO events (
    id, name, description, location, date_open, date_close, date_happen, 
    capacity, male_quantity, female_quantity, banner_url, status, 
    event_type_id, criteria_id, fanpage_id, semester_id, is_active, created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES
(11, 'Ngày thứ Bảy Xanh Mùa Xuân (Quá khứ)', 
    'Hoạt động nhặt rác bảo vệ môi trường.', 
    'Khuôn viên trường', 
    NOW() - INTERVAL '35 DAYS', NOW() - INTERVAL '26 DAYS', NOW() - INTERVAL '25 DAYS', 
    100, 50, 50, 'https://images.unsplash.com/photo-1559027615-cd4628902d4a', 'PUBLISHED', 
    2, 3, 2, 1, true, NOW(), NOW());

INSERT INTO event_points (event_id, point_category_id, point, is_active, created_at, updated_at) VALUES
(11, 3, 5, true, NOW(), NOW());

-- Sinh viên 2 đã tham gia và hoàn thành sự kiện quá khứ 11 (Type 2, Cat 3)
INSERT INTO registrations (event_id, user_id, status, reg_date, check_in_at, check_out_at, created_at, updated_at) VALUES
(11, 2, 'COMPLETED', NOW() - INTERVAL '30 DAYS', NOW() - INTERVAL '25 DAYS', NOW() - INTERVAL '25 DAYS', NOW(), NOW());


-- ==============================================================================
-- 12. ĐỒNG BỘ IDENTITY SEQUENCES CỦA POSTGRESQL ĐỂ TRÁNH LỖI DUPLICATE KEY VỀ SAU
-- ==============================================================================
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
