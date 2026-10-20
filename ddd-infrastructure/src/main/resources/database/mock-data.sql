-- ============================================================
-- Mock Data cho Dự án Centralized Event Management Platform
-- ============================================================

-- ============================================================
-- 1. ROLE
-- ============================================================
INSERT INTO roles (name, description, created_by) VALUES
('ADMIN', 'Quản trị viên hệ thống', 1),
('STUDENT', 'Sinh viên', 1),
('FANPAGE_ADMIN', 'Quản trị viên Fanpage / Ban tổ chức sự kiện', 1);

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
-- 5. USER (Mật khẩu mặc định: 123456 -> BCrypt hash)
-- ============================================================
INSERT INTO users (email, password, provider_id, first_name, last_name, gender, avatar_url, created_by) VALUES
('admin@example.com', '$2a$10$WySCxBLtmNxmzRZ7tg62qu.Vmi7JfFNWhsLeZn/ukRQMT0nX6Zp6S', '', 'Admin', 'System', 'MALE', 'https://ui-avatars.com/api/?name=Admin+System', 1),
('student1@example.com', '$2a$10$WySCxBLtmNxmzRZ7tg62qu.Vmi7JfFNWhsLeZn/ukRQMT0nX6Zp6S', '', 'Văn A', 'Nguyễn', 'MALE', 'https://ui-avatars.com/api/?name=Van+A', 1),
('student2@example.com', '$2a$10$WySCxBLtmNxmzRZ7tg62qu.Vmi7JfFNWhsLeZn/ukRQMT0nX6Zp6S', '', 'Thị B', 'Trần', 'FEMALE', 'https://ui-avatars.com/api/?name=Thi+B', 1),
('org1@example.com', '$2a$10$WySCxBLtmNxmzRZ7tg62qu.Vmi7JfFNWhsLeZn/ukRQMT0nX6Zp6S', '', 'Văn C', 'Lê', 'MALE', 'https://ui-avatars.com/api/?name=Van+C', 1);

-- ============================================================
-- 6. USER_ROLES (Gán vai trò cho người dùng)
-- ============================================================
INSERT INTO user_roles (user_id, role_id, created_by) VALUES
(1, 1, 1), -- admin: ADMIN
(2, 2, 1), -- student1: STUDENT
(3, 2, 1), -- student2: STUDENT
(4, 3, 1); -- org1: ORGANIZER

-- ============================================================
-- 7. STUDENT_PROFILE
-- ============================================================
INSERT INTO student_profiles (student_id, k_number, major_id, user_id, created_by) VALUES
('B2012345', 46, 1, 2, 1),
('B2012346', 46, 2, 3, 1);

-- ============================================================
-- 8. EVENT_TYPE
-- ============================================================
INSERT INTO event_types (name, description, created_by) VALUES
('Hội thảo học thuật', 'Các buổi seminar, workshop chuyên ngành', 1),
('Hoạt động tình nguyện', 'Mùa hè xanh, Tiếp sức mùa thi, Hiến máu', 1),
('Văn hóa - Văn nghệ', 'Hội diễn văn nghệ, Giao lưu âm nhạc', 1),
('Thể dục - Thể thao', 'Hội thao, Giải bóng đá', 1);

-- ============================================================
-- 9. SEMESTER (Học kỳ)
-- ============================================================
INSERT INTO semesters (number, academic_year, start_date, end_date, status, created_by) VALUES
(1, '2023-2024', '2023-08-15', '2023-12-31', 'COMPLETED', 1),
(2, '2023-2024', '2024-01-15', '2024-05-31', 'COMPLETED', 1),
(1, '2024-2025', '2024-08-15', '2024-12-31', 'ONGOING', 1);

-- ============================================================
-- 10. FANPAGE & FANPAGE_MEMBER & FANPAGE_ADMIN_PROFILE
-- ============================================================
INSERT INTO fanpages (name, description, avatar_url, status, org_type, org_name, parent_org, help_contact, help_phone, created_by) VALUES
('Đoàn khoa Công nghệ Thông tin và Truyền thông', 'Trang thông tin chính thức của Đoàn khoa CNTT&TT', 'https://example.com/avatar_doankhoa.jpg', 'ACTIVE', 'FACULTY', 'Đoàn khoa CNTT&TT', 'Đoàn trường ĐHCT', 'doankhoacntt@ctu.edu.vn', '02923832000', 1),
('Câu lạc bộ Tin học', 'CLB học thuật dành cho sinh viên yêu CNTT', 'https://example.com/avatar_clb.jpg', 'ACTIVE', 'CLUB', 'CLB Tin học', 'Đoàn khoa CNTT&TT', 'clbtinhoc@ctu.edu.vn', '02923832001', 1);

-- org1 (user_id = 4) là admin fanpage Đoàn khoa (fanpage_id = 1) và CLB Tin học (fanpage_id = 2)
INSERT INTO fanpage_members (fanpage_id, user_id, role, created_by) VALUES
(1, 4, 'ADMIN', 1),
(2, 4, 'ADMIN', 1);

INSERT INTO fanpage_admin_profiles (staff_id, org_name, title, dob, phone_number, user_id, created_by) VALUES
('NV001', 'Đoàn khoa CNTT&TT', 'Bí thư', '1995-05-10', '0912345678', 4, 1);

-- ============================================================
-- 11. EVENT
-- ============================================================
INSERT INTO events (name, description, location, date_open, date_close, date_happen, capacity, banner_url, status, event_type_id, criteria_id, fanpage_id, semester_id, created_by) VALUES
('Hội thảo Trí tuệ Nhân tạo và Xu hướng Công nghệ 2024', 'Chia sẻ xu hướng AI mới nhất dành cho sinh viên CNTT', 'Hội trường Rùa, Khu II ĐHCT', '2024-09-01 08:00:00+07', '2024-09-10 17:00:00+07', '2024-09-15 08:00:00+07', 200, 'https://example.com/ai_banner.jpg', 'PUBLISHED', 1, 1, 1, 3, 4),
('Chiến dịch Tiếp sức Mùa thi 2024', 'Chiến dịch tình nguyện thường niên hỗ trợ thí sinh THPT', 'Các điểm thi TP Cần Thơ', '2024-06-01 08:00:00+07', '2024-06-15 17:00:00+07', '2024-06-25 07:00:00+07', 100, 'https://example.com/tsmt_banner.jpg', 'PUBLISHED', 2, 17, 1, 2, 4);

-- ============================================================
-- 12. EVENT_POINT
-- ============================================================
-- Event 1 cộng điểm Mục 2 (Trách nhiệm tham gia hoạt động học tập, NCKH)
-- Event 2 cộng điểm Mục 17 (Tiêu chuẩn tình nguyện)
INSERT INTO event_points (event_id, point_category_id, point, created_by) VALUES
(1, 2, 5, 4),
(2, 17, 10, 4);

-- ============================================================
-- 13. REGISTRATION
-- ============================================================
INSERT INTO registrations (user_id, event_id, reg_date, status, created_by) VALUES
(2, 1, NOW(), 'REGISTERED', 2),
(3, 1, NOW(), 'CHECKED_IN', 3);

-- ============================================================
-- 14. POINT_TRANSACTION & STUDENT_SEMESTER_POINT
-- ============================================================
INSERT INTO point_transactions (user_id, semester_id, point_category_id, event_id, points, type, status, description, created_by) VALUES
(2, 3, 2, 1, 5, 'EVENT', 'CONFIRMED', 'Cộng điểm tham gia Hội thảo AI 2024', 4),
(3, 2, 17, 2, 10, 'EVENT', 'CONFIRMED', 'Cộng điểm tham gia Tiếp sức mùa thi 2024', 4);

INSERT INTO student_semester_points (user_id, semester_id, point_category_id, total_point, created_by) VALUES
(2, 3, 2, 5, 4),
(3, 2, 17, 10, 4);

-- ============================================================
-- 15. CONVERSATION & MESSAGE
-- ============================================================
INSERT INTO conversations (user_id, fanpage_id, created_by) VALUES
(2, 1, 2);

INSERT INTO messages (conversation_id, sender_type, sender_id, content, created_by) VALUES
(1, 'USER', 2, 'Xin chào Ban tổ chức, em muốn hỏi hội thảo có cấp giấy chứng nhận không ạ?', 2),
(1, 'FANPAGE', 1, 'Chào bạn, sự kiện có cấp giấy chứng nhận và cộng 5 điểm rèn luyện nhé!', 4);

-- ============================================================
-- 16. NOTIFICATION
-- ============================================================
INSERT INTO notifications (user_id, title, message, status, url, created_by) VALUES
(2, 'Đăng ký thành công', 'Bạn đã đăng ký thành công sự kiện Hội thảo Trí tuệ Nhân tạo 2024.', 'UNREAD', '/events/1', 1);

-- ============================================================
-- 17. REPORT
-- ============================================================
INSERT INTO reports (user_id, fanpage_id, subject, message, status, created_by) VALUES
(2, 1, 'Góp ý về địa điểm', 'Đề xuất BTC mở thêm bàn check-in ở sảnh phụ để tránh ùn tắc.', 'OPEN', 2);
