-- ============================================================
-- Mock Data cho Dự án Centralized Event Management Platform
-- ============================================================

-- Xóa dữ liệu cũ để tránh lỗi trùng lặp khi chạy lại
TRUNCATE TABLE registrations, event_points, events, fanpage_members, fanpages, semesters, point_categories, criterias, event_types, student_profiles, users, associations, majors, schools, roles RESTART IDENTITY CASCADE;

-- ============================================================
-- 1. ROLE
-- ============================================================
INSERT INTO roles (name, description, created_by) VALUES
('ADMIN', 'Quản trị viên hệ thống', 1),
('STUDENT', 'Sinh viên', 1),
('ORGANIZER', 'Ban tổ chức sự kiện / Fanpage Admin', 1);

-- ============================================================
-- 2. SCHOOL
-- ============================================================
INSERT INTO schools (name, description, created_by) VALUES
('Trường Công nghệ Thông tin và Truyền thông', 'Đào tạo các ngành CNTT', 1),
('Trường Kinh tế', 'Đào tạo các ngành Kinh tế, Quản trị', 1),
('Trường Bách Khoa', 'Đào tạo các ngành Kỹ thuật', 1);

-- ============================================================
-- 3. MAJOR
-- ============================================================
INSERT INTO majors (code, name, school_id, created_by) VALUES
('SE', 'Kỹ thuật phần mềm', 1, 1),
('CS', 'Khoa học máy tính', 1, 1),
('BA', 'Quản trị kinh doanh', 2, 1),
('ME', 'Kỹ thuật cơ khí', 3, 1);

-- ============================================================
-- 4. ASSOCIATION
-- ============================================================
INSERT INTO associations (name, address, description, created_by) VALUES
('Đoàn Thanh niên Trường Đại học Cần Thơ', 'Khu II, ĐHCT', 'Đoàn trường', 1),
('Hội Sinh viên Trường Đại học Cần Thơ', 'Khu II, ĐHCT', 'Hội sinh viên trường', 1);

-- ============================================================
-- 5. USER
-- ============================================================
-- Passwords có thể đổi sang hash bcrypt nếu hệ thống yêu cầu hash, ở đây để plaintext hoặc hash tuỳ ý.
INSERT INTO users (email, password, role_id, created_by) VALUES
('admin@example.com', '123456', 1, 1),
('student1@example.com', '123456', 2, 1),
('student2@example.com', '123456', 2, 1),
('org1@example.com', '123456', 3, 1);

-- ============================================================
-- 6. STUDENT_PROFILE
-- ============================================================
INSERT INTO student_profiles (first_name, last_name, student_id, gender, k_number, association_id, major_id, user_id, created_by) VALUES
('Nguyễn Văn', 'A', 'B2012345', 'MALE', 46, 1, 1, 2, 1),
('Trần Thị', 'B', 'B2012346', 'FEMALE', 46, 2, 2, 3, 1);

-- ============================================================
-- 7. EVENT_TYPE
-- ============================================================
INSERT INTO event_types (name, description, created_by) VALUES
('Hội thảo học thuật', 'Các buổi seminar, workshop chuyên ngành', 1),
('Hoạt động tình nguyện', 'Mùa hè xanh, Tiếp sức mùa thi, Hiến máu', 1),
('Văn hóa - Văn nghệ', 'Hội diễn văn nghệ, Giao lưu âm nhạc', 1),
('Thể dục - Thể thao', 'Hội thao, Giải bóng đá', 1);

-- ============================================================
-- 8. CRITERIA (Tiêu chí Sinh viên 5 tốt)
-- ============================================================
INSERT INTO criterias (name, description, scope, created_by) VALUES
('Đạo đức tốt', 'Tiêu chí đánh giá rèn luyện đạo đức', 'Trường', 1),
('Học tập tốt', 'Tiêu chí đánh giá thành tích học tập', 'Trường', 1),
('Thể lực tốt', 'Tiêu chí đánh giá sức khoẻ, thể dục thể thao', 'Trường', 1),
('Tình nguyện tốt', 'Tiêu chí đánh giá hoạt động tình nguyện', 'Trường', 1),
('Hội nhập tốt', 'Tiêu chí đánh giá kỹ năng ngoại ngữ, hội nhập', 'Trường', 1);

-- ============================================================
-- 9. POINT_CATEGORY
-- ============================================================
-- Parent categories
INSERT INTO point_categories (name, description, maximum, date_apply, created_by) VALUES
('Điểm rèn luyện tổng', 'Tổng điểm rèn luyện (tối đa 100đ)', 100, '2023-01-01', 1);

-- Sub categories (parent_category_id = 1)
INSERT INTO point_categories (name, description, maximum, date_apply, parent_category_id, created_by) VALUES
('Mục 1: Ý thức học tập', 'Đánh giá ý thức và kết quả học tập', 20, '2023-01-01', 1, 1),
('Mục 2: Chấp hành nội quy', 'Đánh giá việc chấp hành nội quy', 25, '2023-01-01', 1, 1),
('Mục 3: Hoạt động chính trị - xã hội', 'Đánh giá tham gia HĐ chính trị, xã hội, tình nguyện', 20, '2023-01-01', 1, 1);

-- ============================================================
-- 10. SEMESTER
-- ============================================================
INSERT INTO semesters (number, academic_year, start_date, end_date, status, created_by) VALUES
(1, '2023-2024', '2023-08-15', '2023-12-31', 'COMPLETED', 1),
(2, '2023-2024', '2024-01-15', '2024-05-31', 'ONGOING', 1);

-- ============================================================
-- 11. FANPAGE & FANPAGE_MEMBER
-- ============================================================
INSERT INTO fanpages (name, description, status, created_by) VALUES
('Đoàn khoa Công nghệ Thông tin và Truyền thông', 'Trang thông tin chính thức của Đoàn khoa CNTT&TT', 'ACTIVE', 1),
('Câu lạc bộ Tin học', 'CLB học thuật dành cho sinh viên yêu CNTT', 'ACTIVE', 1);

-- org1@example.com (user_id = 4) làm admin fanpage Đoàn khoa (fanpage_id = 1)
INSERT INTO fanpage_members (fanpage_id, user_id, role, created_by) VALUES
(1, 4, 'ADMIN', 1);

-- ============================================================
-- 12. EVENT
-- ============================================================
INSERT INTO events (name, description, date_open, date_close, date_happen, capacity, status, event_type_id, criteria_id, fanpage_id, created_by) VALUES
('Hội thảo Trí tuệ Nhân tạo 2024', 'Chia sẻ xu hướng AI mới nhất', '2024-03-01 08:00:00+07', '2024-03-10 17:00:00+07', '2024-03-15 08:00:00+07', 150, 'PUBLISHED', 1, 2, 1, 4),
('Chiến dịch Mùa hè xanh 2024', 'Chiến dịch tình nguyện hè thường niên', '2024-06-01 08:00:00+07', '2024-06-15 17:00:00+07', '2024-07-01 07:00:00+07', 500, 'DRAFT', 2, 4, 1, 4);

-- ============================================================
-- 13. EVENT_POINT
-- ============================================================
-- Sự kiện 1 cộng điểm Mục 1 (id=2), Sự kiện 2 cộng điểm Mục 3 (id=4)
INSERT INTO event_points (event_id, point_category_id, point, created_by) VALUES
(1, 2, 5, 4),
(2, 4, 10, 4);

-- ============================================================
-- 14. REGISTRATION
-- ============================================================
INSERT INTO registrations (user_id, event_id, status, created_by) VALUES
(2, 1, 'REGISTERED', 2),
(3, 1, 'CHECKED_IN', 3);
