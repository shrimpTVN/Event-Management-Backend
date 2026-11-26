-- ============================================================================
-- SEED DATA CHUẨN ĐẠI HỌC CẦN THƠ (CTU) PHỤC VỤ DEMO BÁO CÁO CUỐI KỲ
-- - Tương thích 100% với PostgreSQL schema hiện tại của Backend (mydb).
-- - Giữ nguyên bảng danh mục chuẩn: roles, point_categories (73), criterias (40), event_types (18).
-- - Giữ nguyên các tài khoản demo chính & mật khẩu hiện tại của bạn:
--     1. Student chính : loc@student.ctu.edu.vn     (Đoàn Tài Lộc - B2300000 - K49 KTPM)
--     2. Student Kinh tế: thu.kinhte@student.ctu.edu.vn (Trần Thị Minh Thư - B2304512 - K49 QTKD)
--     3. Fanpage Admin : organizer@ctu.edu.vn        (Nguyễn Minh Anh - Đoàn Trường CNTT&TT)
--     4. Super Admin   : admin@ctu.edu.vn & superadmin@gmail.com
-- - Chỉ sử dụng URL ảnh Cloudinary & Unsplash đã kiểm tra HTTP 200 OK (không dùng ảnh AI).
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 0. DỌN DẸP DỮ LIỆU DEMO CŨ (GIỮ NGUYÊN roles, point_categories, criterias, event_types)
-- ----------------------------------------------------------------------------
TRUNCATE TABLE
    stored_events,
    notifications,
    messages,
    conversations,
    reports,
    refresh_tokens,
    otps,
    point_transactions,
    student_semester_points,
    student_criteria_transactions,
    student_criteria_progress,
    registrations,
    event_points,
    events,
    followings,
    fanpage_member_invitations,
    fanpage_members,
    fanpages,
    fanpage_admin_profiles,
    student_profiles,
    user_roles,
    users,
    semesters,
    academic_years,
    associations,
    majors,
    schools
RESTART IDENTITY CASCADE;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'recommendation_view_receipt') THEN
        EXECUTE 'TRUNCATE TABLE recommendation_view_receipt CASCADE';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'student_event_recommendation') THEN
        EXECUTE 'TRUNCATE TABLE student_event_recommendation CASCADE';
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 1. TRƯỜNG / KHOA / VIỆN ĐÀO TẠO ĐẠI HỌC TRỰC THUỘC ĐẠI HỌC CẦN THƠ (13 ĐƠN VỊ)
--    Đối chiếu chính thức từ ctu.edu.vn/don-vi-truc-thuoc.html & webctu_program
-- ----------------------------------------------------------------------------
INSERT INTO schools (id, name, description, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1,  'Trường Công nghệ Thông tin và Truyền thông', 'Đào tạo các ngành Kỹ thuật phần mềm, Công nghệ thông tin, Khoa học máy tính, Hệ thống thông tin, Mạng máy tính, An toàn thông tin, Trí tuệ nhân tạo và Truyền thông đa phương tiện.', TRUE, NOW(), NOW()),
(2,  'Trường Kinh tế', 'Đào tạo các ngành Kinh tế, Quản trị kinh doanh, Marketing, Kinh doanh quốc tế, Kinh doanh thương mại, Thương mại điện tử, Tài chính - Ngân hàng, FinTech, Kế toán, Kiểm toán, Kinh tế nông nghiệp, Kinh tế tài nguyên thiên nhiên và Quản trị dịch vụ du lịch và lữ hành.', TRUE, NOW(), NOW()),
(3,  'Trường Bách khoa', 'Đào tạo khối ngành Kỹ thuật cơ khí, Cơ điện tử, Ô tô, Điện, Điện tử - Viễn thông, Điều khiển & Tự động hóa, Y sinh, Vật liệu, Kỹ thuật máy tính, Hóa học, Quản lý công nghiệp, Logistics và Quản lý chuỗi cung ứng, Kiến trúc và Xây dựng.', TRUE, NOW(), NOW()),
(4,  'Trường Nông nghiệp', 'Đào tạo các ngành Khoa học đất, Chăn nuôi, Nông học, Khoa học cây trồng, Bảo vệ thực vật, Công nghệ rau hoa quả và cảnh quan, Sinh học ứng dụng và Thú y.', TRUE, NOW(), NOW()),
(5,  'Trường Sư phạm', 'Đào tạo giáo viên các bậc học từ Mầm non, Tiểu học, Thể chất đến các môn Sư phạm Toán, Tin, Lý, Hóa, Sinh, Văn, Sử, Địa, Khoa học tự nhiên, Lịch sử - Địa lý và Tâm lý học giáo dục.', TRUE, NOW(), NOW()),
(6,  'Trường Thủy sản', 'Đào tạo chuyên sâu về Nuôi trồng thủy sản, Bệnh học thủy sản, Quản lý thủy sản, Công nghệ chế biến thủy sản và Đảm bảo chất lượng và an toàn thực phẩm.', TRUE, NOW(), NOW()),
(7,  'Trường Khoa học Tự nhiên', 'Đào tạo các ngành Sinh học, Hóa học, Hóa dược, Toán ứng dụng, Khoa học dữ liệu, Thống kê và Vật lý kỹ thuật.', TRUE, NOW(), NOW()),
(8,  'Khoa Luật', 'Đào tạo cử nhân và sau đại học các ngành Luật, Luật dân sự và tố tụng dân sự, và Luật kinh tế.', TRUE, NOW(), NOW()),
(9,  'Khoa Ngoại ngữ', 'Đào tạo các ngành Ngôn ngữ Anh, Ngôn ngữ Pháp, Sư phạm Tiếng Anh và Sư phạm Tiếng Pháp.', TRUE, NOW(), NOW()),
(10, 'Khoa Môi trường và Tài nguyên Thiên nhiên', 'Đào tạo các ngành Khoa học môi trường, Kỹ thuật môi trường, Quy hoạch vùng và đô thị, Kỹ thuật cấp thoát nước, Quản lý tài nguyên và môi trường và Quản lý đất đai.', TRUE, NOW(), NOW()),
(11, 'Khoa Khoa học Chính trị, Xã hội và Nhân văn', 'Đào tạo các ngành Giáo dục Công dân, Triết học, Văn học, Chính trị học, Xã hội học, Báo chí, Thông tin - Thư viện và Du lịch.', TRUE, NOW(), NOW()),
(12, 'Viện Công nghệ Sinh học và Thực phẩm', 'Đơn vị nghiên cứu và đào tạo đại học các ngành Công nghệ sinh học, Công nghệ thực phẩm và Công nghệ sau thu hoạch.', TRUE, NOW(), NOW()),
(13, 'Cơ sở Hậu Giang', 'Đơn vị đào tạo trực thuộc tại Hậu Giang, quản lý ngành Kinh doanh nông nghiệp và tổ chức giảng dạy nhiều ngành đại học chính quy tại cơ sở Hậu Giang.', TRUE, NOW(), NOW());

-- ----------------------------------------------------------------------------
-- 2. NGÀNH ĐÀO TẠO ĐẠI HỌC CHÍNH QUY ĐHCT (97 NGÀNH THEO MÃ NGÀNH BỘ GD&ĐT)
--    Đối chiếu chính thức từ tuyensinh.ctu.edu.vn (2026) & webctu_program
-- ----------------------------------------------------------------------------
INSERT INTO majors (id, code, name, school_id, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
-- Trường Công nghệ Thông tin và Truyền thông (1) - 8 ngành
(1,  '7480103', 'Kỹ thuật phần mềm', 1, TRUE, NOW(), NOW()),
(2,  '7480201', 'Công nghệ thông tin', 1, TRUE, NOW(), NOW()),
(3,  '7480101', 'Khoa học máy tính', 1, TRUE, NOW(), NOW()),
(4,  '7480104', 'Hệ thống thông tin', 1, TRUE, NOW(), NOW()),
(5,  '7480102', 'Mạng máy tính và truyền thông dữ liệu', 1, TRUE, NOW(), NOW()),
(6,  '7480202', 'An toàn thông tin', 1, TRUE, NOW(), NOW()),
(7,  '7480107', 'Trí tuệ nhân tạo', 1, TRUE, NOW(), NOW()),
(8,  '7320104', 'Truyền thông đa phương tiện', 1, TRUE, NOW(), NOW()),
-- Trường Kinh tế (2) - 13 ngành
(9,  '7340101', 'Quản trị kinh doanh', 2, TRUE, NOW(), NOW()),
(10, '7310101', 'Kinh tế', 2, TRUE, NOW(), NOW()),
(11, '7340115', 'Marketing', 2, TRUE, NOW(), NOW()),
(12, '7340120', 'Kinh doanh quốc tế', 2, TRUE, NOW(), NOW()),
(13, '7340121', 'Kinh doanh thương mại', 2, TRUE, NOW(), NOW()),
(14, '7340122', 'Thương mại điện tử', 2, TRUE, NOW(), NOW()),
(15, '7340201', 'Tài chính - Ngân hàng', 2, TRUE, NOW(), NOW()),
(16, '7340205', 'Công nghệ tài chính', 2, TRUE, NOW(), NOW()),
(17, '7340301', 'Kế toán', 2, TRUE, NOW(), NOW()),
(18, '7340302', 'Kiểm toán', 2, TRUE, NOW(), NOW()),
(19, '7620115', 'Kinh tế nông nghiệp', 2, TRUE, NOW(), NOW()),
(20, '7810103', 'Quản trị dịch vụ du lịch và lữ hành', 2, TRUE, NOW(), NOW()),
(21, '7850102', 'Kinh tế tài nguyên thiên nhiên', 2, TRUE, NOW(), NOW()),
-- Trường Bách khoa (3) - 17 ngành
(22, '7480106', 'Kỹ thuật máy tính', 3, TRUE, NOW(), NOW()),
(23, '7510401', 'Công nghệ kỹ thuật hóa học', 3, TRUE, NOW(), NOW()),
(24, '7510601', 'Quản lý công nghiệp', 3, TRUE, NOW(), NOW()),
(25, '7510605', 'Logistics và Quản lý chuỗi cung ứng', 3, TRUE, NOW(), NOW()),
(26, '7520103', 'Kỹ thuật cơ khí', 3, TRUE, NOW(), NOW()),
(27, '7520114', 'Kỹ thuật cơ điện tử', 3, TRUE, NOW(), NOW()),
(28, '7520130', 'Kỹ thuật ô tô', 3, TRUE, NOW(), NOW()),
(29, '7520201', 'Kỹ thuật điện', 3, TRUE, NOW(), NOW()),
(30, '7520207', 'Kỹ thuật điện tử - viễn thông', 3, TRUE, NOW(), NOW()),
(31, '7520212', 'Kỹ thuật y sinh', 3, TRUE, NOW(), NOW()),
(32, '7520216', 'Kỹ thuật điều khiển và tự động hóa', 3, TRUE, NOW(), NOW()),
(33, '7520309', 'Kỹ thuật vật liệu', 3, TRUE, NOW(), NOW()),
(34, '7580101', 'Kiến trúc', 3, TRUE, NOW(), NOW()),
(35, '7580201', 'Kỹ thuật xây dựng', 3, TRUE, NOW(), NOW()),
(36, '7580202', 'Kỹ thuật xây dựng công trình thủy', 3, TRUE, NOW(), NOW()),
(37, '7580205', 'Kỹ thuật xây dựng công trình giao thông', 3, TRUE, NOW(), NOW()),
(38, '7580302', 'Quản lý xây dựng', 3, TRUE, NOW(), NOW()),
-- Trường Nông nghiệp (4) - 8 ngành
(39, '7420203', 'Sinh học ứng dụng', 4, TRUE, NOW(), NOW()),
(40, '7620103', 'Khoa học đất', 4, TRUE, NOW(), NOW()),
(41, '7620105', 'Chăn nuôi', 4, TRUE, NOW(), NOW()),
(42, '7620109', 'Nông học', 4, TRUE, NOW(), NOW()),
(43, '7620110', 'Khoa học cây trồng', 4, TRUE, NOW(), NOW()),
(44, '7620112', 'Bảo vệ thực vật', 4, TRUE, NOW(), NOW()),
(45, '7620113', 'Công nghệ rau hoa quả và cảnh quan', 4, TRUE, NOW(), NOW()),
(46, '7640101', 'Thú y', 4, TRUE, NOW(), NOW()),
-- Trường Sư phạm (5) - 14 ngành
(47, '7140201', 'Giáo dục Mầm non', 5, TRUE, NOW(), NOW()),
(48, '7140202', 'Giáo dục Tiểu học', 5, TRUE, NOW(), NOW()),
(49, '7140206', 'Giáo dục Thể chất', 5, TRUE, NOW(), NOW()),
(50, '7140209', 'Sư phạm Toán học', 5, TRUE, NOW(), NOW()),
(51, '7140210', 'Sư phạm Tin học', 5, TRUE, NOW(), NOW()),
(52, '7140211', 'Sư phạm Vật lý', 5, TRUE, NOW(), NOW()),
(53, '7140212', 'Sư phạm Hóa học', 5, TRUE, NOW(), NOW()),
(54, '7140213', 'Sư phạm Sinh học', 5, TRUE, NOW(), NOW()),
(55, '7140217', 'Sư phạm Ngữ văn', 5, TRUE, NOW(), NOW()),
(56, '7140218', 'Sư phạm Lịch sử', 5, TRUE, NOW(), NOW()),
(57, '7140219', 'Sư phạm Địa lý', 5, TRUE, NOW(), NOW()),
(58, '7140247', 'Sư phạm Khoa học tự nhiên', 5, TRUE, NOW(), NOW()),
(59, '7140249', 'Sư phạm Lịch sử - Địa lý', 5, TRUE, NOW(), NOW()),
(60, '7310403', 'Tâm lý học giáo dục', 5, TRUE, NOW(), NOW()),
-- Trường Thủy sản (6) - 5 ngành
(61, '7540105', 'Công nghệ chế biến thủy sản', 6, TRUE, NOW(), NOW()),
(62, '7540106', 'Đảm bảo chất lượng và an toàn thực phẩm', 6, TRUE, NOW(), NOW()),
(63, '7620301', 'Nuôi trồng thủy sản', 6, TRUE, NOW(), NOW()),
(64, '7620302', 'Bệnh học thủy sản', 6, TRUE, NOW(), NOW()),
(65, '7620305', 'Quản lý thủy sản', 6, TRUE, NOW(), NOW()),
-- Trường Khoa học Tự nhiên (7) - 7 ngành
(66, '7420101', 'Sinh học', 7, TRUE, NOW(), NOW()),
(67, '7440112', 'Hóa học', 7, TRUE, NOW(), NOW()),
(68, '7460108', 'Khoa học dữ liệu', 7, TRUE, NOW(), NOW()),
(69, '7460112', 'Toán ứng dụng', 7, TRUE, NOW(), NOW()),
(70, '7460201', 'Thống kê', 7, TRUE, NOW(), NOW()),
(71, '7520401', 'Vật lý kỹ thuật', 7, TRUE, NOW(), NOW()),
(72, '7720203', 'Hóa dược', 7, TRUE, NOW(), NOW()),
-- Khoa Luật (8) - 3 ngành
(73, '7380101', 'Luật', 8, TRUE, NOW(), NOW()),
(74, '7380103', 'Luật dân sự và tố tụng dân sự', 8, TRUE, NOW(), NOW()),
(75, '7380107', 'Luật kinh tế', 8, TRUE, NOW(), NOW()),
-- Khoa Ngoại ngữ (9) - 4 ngành
(76, '7140231', 'Sư phạm Tiếng Anh', 9, TRUE, NOW(), NOW()),
(77, '7140233', 'Sư phạm Tiếng Pháp', 9, TRUE, NOW(), NOW()),
(78, '7220201', 'Ngôn ngữ Anh', 9, TRUE, NOW(), NOW()),
(79, '7220203', 'Ngôn ngữ Pháp', 9, TRUE, NOW(), NOW()),
-- Khoa Môi trường và Tài nguyên Thiên nhiên (10) - 6 ngành
(80, '7440301', 'Khoa học môi trường', 10, TRUE, NOW(), NOW()),
(81, '7520320', 'Kỹ thuật môi trường', 10, TRUE, NOW(), NOW()),
(82, '7580105', 'Quy hoạch vùng và đô thị', 10, TRUE, NOW(), NOW()),
(83, '7580213', 'Kỹ thuật cấp thoát nước', 10, TRUE, NOW(), NOW()),
(84, '7850101', 'Quản lý tài nguyên và môi trường', 10, TRUE, NOW(), NOW()),
(85, '7850103', 'Quản lý đất đai', 10, TRUE, NOW(), NOW()),
-- Khoa Khoa học Chính trị, Xã hội và Nhân văn (11) - 8 ngành
(86, '7140204', 'Giáo dục Công dân', 11, TRUE, NOW(), NOW()),
(87, '7229001', 'Triết học', 11, TRUE, NOW(), NOW()),
(88, '7229030', 'Văn học', 11, TRUE, NOW(), NOW()),
(89, '7310201', 'Chính trị học', 11, TRUE, NOW(), NOW()),
(90, '7310301', 'Xã hội học', 11, TRUE, NOW(), NOW()),
(91, '7320101', 'Báo chí', 11, TRUE, NOW(), NOW()),
(92, '7320201', 'Thông tin - Thư viện', 11, TRUE, NOW(), NOW()),
(93, '7810101', 'Du lịch', 11, TRUE, NOW(), NOW()),
-- Viện Công nghệ Sinh học và Thực phẩm (12) - 3 ngành
(94, '7420201', 'Công nghệ sinh học', 12, TRUE, NOW(), NOW()),
(95, '7540101', 'Công nghệ thực phẩm', 12, TRUE, NOW(), NOW()),
(96, '7540104', 'Công nghệ sau thu hoạch', 12, TRUE, NOW(), NOW()),
-- Cơ sở Hậu Giang (13) - 1 ngành
(97, '7620114', 'Kinh doanh nông nghiệp', 13, TRUE, NOW(), NOW());

-- ----------------------------------------------------------------------------
-- 3. LIÊN CHI HỘI / CHI HỘI SINH VIÊN ĐBSCL (8 ĐƠN VỊ)
-- ----------------------------------------------------------------------------
INSERT INTO associations (id, name, address, description, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Liên chi hội Sinh viên Cần Thơ', 'Khu II, Đường 3/2, Ninh Kiều, TP. Cần Thơ', 'Mái nhà chung kết nối sinh viên TP. Cần Thơ đang học tập tại ĐHCT.', TRUE, NOW(), NOW()),
(2, 'Liên chi hội Sinh viên Đồng Tháp', 'Khu II, Đường 3/2, Ninh Kiều, TP. Cần Thơ', 'Tổ chức hội đồng hương sinh viên tỉnh Đồng Tháp tại Đại học Cần Thơ.', TRUE, NOW(), NOW()),
(3, 'Liên chi hội Sinh viên Vĩnh Long', 'Khu II, Đường 3/2, Ninh Kiều, TP. Cần Thơ', 'Kết nối sinh viên tỉnh Vĩnh Long tham gia phong trào học tập và tình nguyện.', TRUE, NOW(), NOW()),
(4, 'Liên chi hội Sinh viên An Giang', 'Khu II, Đường 3/2, Ninh Kiều, TP. Cần Thơ', 'Kết nối sinh viên tỉnh An Giang tại Đại học Cần Thơ.', TRUE, NOW(), NOW()),
(5, 'Liên chi hội Sinh viên Cà Mau', 'Khu II, Đường 3/2, Ninh Kiều, TP. Cần Thơ', 'Tổ chức sinh viên đồng hương Cà Mau tại Đại học Cần Thơ.', TRUE, NOW(), NOW()),
(6, 'Liên chi hội Sinh viên Kiên Giang', 'Khu II, Đường 3/2, Ninh Kiều, TP. Cần Thơ', 'Kết nối sinh viên tỉnh Kiên Giang tại Đại học Cần Thơ.', TRUE, NOW(), NOW()),
(7, 'Liên chi hội Sinh viên Sóc Trăng', 'Khu II, Đường 3/2, Ninh Kiều, TP. Cần Thơ', 'Tổ chức hội sinh viên tỉnh Sóc Trăng tại Đại học Cần Thơ.', TRUE, NOW(), NOW()),
(8, 'Chi hội Sinh viên Ngã Bảy', 'Khu II, Đường 3/2, Ninh Kiều, TP. Cần Thơ', 'Chi hội sinh viên khu vực Ngã Bảy trực thuộc LCHSV Hậu Giang.', TRUE, NOW(), NOW());

-- ----------------------------------------------------------------------------
-- 4. NĂM HỌC & HỌC KỲ (THEO CÔNG VĂN BỐ TRÍ 3 HỌC KỲ CHÍNH CỦA ĐHCT)
-- ----------------------------------------------------------------------------
INSERT INTO academic_years (id, code, start_date, end_date, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, '2025-2026', '2025-09-08', '2026-08-23', TRUE, NOW(), NOW()),
(2, '2026-2027', '2026-09-07', '2027-08-29', TRUE, NOW(), NOW());

INSERT INTO semesters (id, number, academic_year_id, start_date, end_date, status, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1,   1, 2, '2026-09-07', '2026-12-20', 'ONGOING',   TRUE, NOW(), NOW()),
(2,   2, 2, '2027-01-04', '2027-05-02', 'UPCOMING',  TRUE, NOW(), NOW()),
(3,   3, 2, '2027-05-17', '2027-08-29', 'UPCOMING',  TRUE, NOW(), NOW()),
(101, 1, 1, '2025-09-08', '2025-12-21', 'COMPLETED', TRUE, NOW(), NOW()),
(102, 2, 1, '2025-12-29', '2026-04-26', 'COMPLETED', TRUE, NOW(), NOW()),
(103, 3, 1, '2026-05-11', '2026-08-23', 'COMPLETED', TRUE, NOW(), NOW());

-- ----------------------------------------------------------------------------
-- 5. TÀI KHOẢN DEMO CHÍNH + 120 SINH VIÊN NỀN
--    Mật khẩu giữ nguyên như hiện tại của bạn
-- ----------------------------------------------------------------------------
INSERT INTO users (id, email, password, first_name, last_name, gender, avatar_url, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1,  'loc@student.ctu.edu.vn',         '$2a$10$5snZONh3FshSppF3fNtoD.N0VHU31/mhWPRzv2FnnOMyvMRBMw3Ee', 'Tài Lộc',      'Đoàn',     'MALE',   'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80', TRUE, NOW(), NOW()),
(2,  'organizer@ctu.edu.vn',           '$2a$10$5snZONh3FshSppF3fNtoD.N0VHU31/mhWPRzv2FnnOMyvMRBMw3Ee', 'Minh Anh',     'Nguyễn',   'FEMALE', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80', TRUE, NOW(), NOW()),
(3,  'admin@ctu.edu.vn',               '$2a$10$5snZONh3FshSppF3fNtoD.N0VHU31/mhWPRzv2FnnOMyvMRBMw3Ee', 'Quản Trị',     'Hệ Thống', 'OTHER',  'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=200&q=80', TRUE, NOW(), NOW()),
(4,  'thu.kinhte@student.ctu.edu.vn',  '$2a$10$5snZONh3FshSppF3fNtoD.N0VHU31/mhWPRzv2FnnOMyvMRBMw3Ee', 'Minh Thư',     'Trần Thị', 'FEMALE', 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=200&q=80', TRUE, NOW(), NOW()),
(5,  'organizer.kinhte@ctu.edu.vn',    '$2a$10$5snZONh3FshSppF3fNtoD.N0VHU31/mhWPRzv2FnnOMyvMRBMw3Ee', 'Hoàng Nam',    'Lê',       'MALE',   'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80', TRUE, NOW(), NOW()),
(6,  'organizer.bachkhoa@ctu.edu.vn',  '$2a$10$5snZONh3FshSppF3fNtoD.N0VHU31/mhWPRzv2FnnOMyvMRBMw3Ee', 'Gia Bảo',      'Phạm',     'MALE',   'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80', TRUE, NOW(), NOW()),
(7,  'organizer.hoisv@ctu.edu.vn',     '$2a$10$5snZONh3FshSppF3fNtoD.N0VHU31/mhWPRzv2FnnOMyvMRBMw3Ee', 'Thanh Trúc',   'Võ Thị',   'FEMALE', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80', TRUE, NOW(), NOW()),
(99, 'superadmin@gmail.com',           '$2a$10$5snZONh3FshSppF3fNtoD.N0VHU31/mhWPRzv2FnnOMyvMRBMw3Ee', 'Admin',        'Super',    'OTHER',  'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=200&q=80', TRUE, NOW(), NOW());

INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1),
(4, 1),
(2, 2),
(5, 2),
(6, 2),
(7, 2),
(3, 3),
(99, 3);

INSERT INTO student_profiles (id, user_id, student_id, k_number, major_id, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 1, 'B2300000', 49, 1, TRUE, NOW(), NOW()),
(2, 4, 'B2304512', 49, 9, TRUE, NOW(), NOW());

INSERT INTO fanpage_admin_profiles (id, user_id, staff_id, org_name, title, dob, phone_number, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 2, 'CB-0104', 'Đoàn Trường Công nghệ Thông tin và Truyền thông - Đại học Cần Thơ', 'Bí thư Đoàn Trường',     '1995-10-15', '02923872254', TRUE, NOW(), NOW()),
(2, 5, 'CB-0208', 'Đoàn Trường Kinh tế - Đại học Cần Thơ',                           'Bí thư Đoàn Trường',     '1996-04-20', '02923831771', TRUE, NOW(), NOW()),
(3, 6, 'CB-0315', 'Đoàn Trường Bách khoa - Đại học Cần Thơ',                         'Phó Bí thư Đoàn Trường', '1997-08-12', '02923831550', TRUE, NOW(), NOW()),
(4, 7, 'CB-0001', 'Đoàn Thanh niên - Hội Sinh viên Đại học Cần Thơ',                 'Chủ tịch Hội Sinh viên', '1994-03-26', '02923832663', TRUE, NOW(), NOW());

-- Sinh tự động 120 sinh viên nền CTU (id 201..320) trải đều 13 đơn vị / 97 ngành & K47-K49
WITH generated AS (
    SELECT
        gs AS idx,
        200 + gs AS uid,
        (ARRAY['Nguyễn','Trần','Lê','Phạm','Huỳnh','Võ','Đặng','Bùi','Đỗ','Hồ','Ngô','Dương'])[1 + (gs % 12)] AS ho,
        (ARRAY['Văn Anh','Thị Ngọc','Minh Quân','Quốc Huy','Thu Thảo','Hoàng Phúc','Ngọc Hân','Thanh Tâm','Gia Huy','Khánh Linh','Anh Tuấn','Mỹ Duyên'])[1 + ((gs * 5) % 12)] AS ten,
        (ARRAY['MALE','FEMALE','MALE','FEMALE','FEMALE','MALE','FEMALE','FEMALE','MALE','FEMALE','MALE','FEMALE'])[1 + ((gs * 5) % 12)] AS gt,
        (ARRAY[47, 48, 49])[1 + (gs % 3)] AS khoahoc,
        (1 + (gs % 97)) AS nganh_id,
        (ARRAY[
            'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80',
            'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80',
            'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80',
            'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=200&q=80',
            'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80',
            'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=200&q=80'
        ])[1 + (gs % 6)] AS ava
    FROM generate_series(1, 120) AS gs
),
ins_users AS (
    INSERT INTO users (id, email, password, first_name, last_name, gender, avatar_url, is_active, created_at, updated_at)
    OVERRIDING SYSTEM VALUE
    SELECT
        uid,
        'sv' || lpad(idx::text, 3, '0') || 'b' || (CASE khoahoc WHEN 47 THEN '21' WHEN 48 THEN '22' ELSE '23' END) || lpad((1000 + idx)::text, 5, '0') || '@student.ctu.edu.vn',
        '$2a$10$5snZONh3FshSppF3fNtoD.N0VHU31/mhWPRzv2FnnOMyvMRBMw3Ee',
        ten,
        ho,
        gt,
        ava,
        TRUE,
        NOW() - ((idx % 90) || ' days')::INTERVAL,
        NOW()
    FROM generated
    RETURNING id
),
ins_roles AS (
    INSERT INTO user_roles (user_id, role_id)
    SELECT id, 1 FROM ins_users
)
INSERT INTO student_profiles (id, user_id, student_id, k_number, major_id, is_active, created_at, updated_at)
OVERRIDING SYSTEM VALUE
SELECT
    200 + idx,
    uid,
    'B' || (CASE khoahoc WHEN 47 THEN '21' WHEN 48 THEN '22' ELSE '23' END) || lpad((1000 + idx)::text, 5, '0'),
    khoahoc,
    nganh_id,
    TRUE,
    NOW(),
    NOW()
FROM generated;

-- ----------------------------------------------------------------------------
-- 6. FANPAGES ĐOÀN - HỘI & CLB THỰC TẾ CTU (9 ACTIVE + 2 PENDING DUYỆT)
-- ----------------------------------------------------------------------------
INSERT INTO fanpages (id, name, description, avatar_url, status, org_type, org_name, parent_org, help_contact, help_phone, evidence_url, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1,  'Đoàn Trường Công nghệ Thông tin và Truyền thông - Đại học Cần Thơ',
     'Kênh thông tin chính thức các hoạt động Đoàn, Hội thảo chuyên ngành, Cuộc thi học thuật và Chiến dịch tình nguyện của sinh viên Trường Công nghệ Thông tin và Truyền thông - Đại học Cần Thơ.',
     'https://res.cloudinary.com/depaiphq0/image/upload/v1793680012/ctu-event/fanpages/u_2_14fdaacdf2da4081.jpg',
     'ACTIVE', 'YOUTH_UNION', 'Đoàn Trường Công nghệ Thông tin và Truyền thông - Đại học Cần Thơ', 'Đoàn Thanh niên Cộng sản Hồ Chí Minh Đại học Cần Thơ', 'doantruongcntt@ctu.edu.vn', '02923872254', NULL, TRUE, NOW(), NOW()),
(2,  'CLB Tin học - Trường Công nghệ Thông tin và Truyền thông - Đại học Cần Thơ',
     'Môi trường học thuật, chia sẻ kiến thức lập trình Web, AI, DevOps, Olympic Tin học và tổ chức các buổi Tech Talk cuối tuần cho sinh viên.',
     'https://images.unsplash.com/photo-1531403009284-440f080d1e12?auto=format&fit=crop&w=200&q=80',
     'ACTIVE', 'Câu lạc bộ / Đội / Nhóm', 'CLB Tin học - Trường Công nghệ Thông tin và Truyền thông - Đại học Cần Thơ', 'Đoàn Trường Công nghệ Thông tin và Truyền thông - Đại học Cần Thơ', 'itclub.cict@ctu.edu.vn', '0939112233', NULL, TRUE, NOW(), NOW()),
(3,  'CLB Tiếng Anh - Đại học Cần Thơ',
     'Nơi giao lưu ngôn ngữ, rèn luyện kỹ năng giao tiếp tiếng Anh, hội nhập quốc tế và chuẩn bị chứng chỉ ngoại ngữ cho sinh viên Đại học Cần Thơ.',
     'https://images.unsplash.com/photo-1543269865-cbf427effbad?auto=format&fit=crop&w=200&q=80',
     'ACTIVE', 'Câu lạc bộ / Đội / Nhóm', 'CLB Tiếng Anh - Đại học Cần Thơ', 'Hội Sinh viên Đại học Cần Thơ', 'englishclub@ctu.edu.vn', '0939445566', NULL, TRUE, NOW(), NOW()),
(4,  'Đội Công tác Xã hội - Đại học Cần Thơ',
     'Lực lượng xung kích trong các chiến dịch Mùa Hè Xanh, Tiếp sức mùa thi, Chủ nhật xanh và các chương trình thiện nguyện vì cộng đồng Đồng bằng sông Cửu Long.',
     'https://images.unsplash.com/photo-1559027615-cd4628902d4a?auto=format&fit=crop&w=200&q=80',
     'ACTIVE', 'Câu lạc bộ / Đội / Nhóm', 'Đội Công tác Xã hội - Đại học Cần Thơ', 'Hội Sinh viên Đại học Cần Thơ', 'ctxh@ctu.edu.vn', '0918223344', NULL, TRUE, NOW(), NOW()),
(5,  'CLB Hiến máu Tình nguyện - Đại học Cần Thơ',
     'Đơn vị nòng cốt tuyên truyền và phối hợp tổ chức các ngày hội Hiến máu nhân đạo "Giọt hồng Tây Đô" tại Khu II Đại học Cần Thơ.',
     'https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?auto=format&fit=crop&w=200&q=80',
     'ACTIVE', 'Câu lạc bộ / Đội / Nhóm', 'CLB Hiến máu Tình nguyện - Đại học Cần Thơ', 'Hội Sinh viên Đại học Cần Thơ', 'hienmau@ctu.edu.vn', '0918556677', NULL, TRUE, NOW(), NOW()),
(6,  'Đoàn Trường Kinh tế - Đại học Cần Thơ',
     'Tổ chức các hoạt động học thuật, cuộc thi ý tưởng khởi nghiệp, hội thảo nghề nghiệp và phong trào rèn luyện sinh viên Trường Kinh tế - Đại học Cần Thơ.',
     'https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=200&q=80',
     'ACTIVE', 'YOUTH_UNION', 'Đoàn Trường Kinh tế - Đại học Cần Thơ', 'Đoàn Thanh niên Cộng sản Hồ Chí Minh Đại học Cần Thơ', 'doankinhte@ctu.edu.vn', '02923831771', NULL, TRUE, NOW(), NOW()),
(7,  'Đoàn Thanh niên - Hội Sinh viên Đại học Cần Thơ',
     'Trang thông tin chính thức của Đoàn Thanh niên và Hội Sinh viên cấp Trường Đại học Cần Thơ về các sự kiện cấp trường và xét chọn Sinh viên 5 tốt.',
     'https://res.cloudinary.com/depaiphq0/image/upload/v1793963013/ctu-event/fanpages/u_2_606320c599474b7d.jpg',
     'ACTIVE', 'YOUTH_UNION', 'Đoàn Thanh niên - Hội Sinh viên Đại học Cần Thơ', 'Đại học Cần Thơ', 'doanhoi@ctu.edu.vn', '02923832663', NULL, TRUE, NOW(), NOW()),
(8,  'Đoàn Trường Bách khoa - Đại học Cần Thơ',
     'Kết nối sinh viên kỹ thuật, tổ chức ngày hội sáng tạo kỹ thuật, robocon và các hội thảo chuyển giao công nghệ công nghiệp.',
     'https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?auto=format&fit=crop&w=200&q=80',
     'ACTIVE', 'YOUTH_UNION', 'Đoàn Trường Bách khoa - Đại học Cần Thơ', 'Đoàn Thanh niên Cộng sản Hồ Chí Minh Đại học Cần Thơ', 'doanbachkhoa@ctu.edu.vn', '02923831550', NULL, TRUE, NOW(), NOW()),
(9,  'CLB Thể thao và Sức khỏe Sinh viên - Đại học Cần Thơ',
     'Sân chơi rèn luyện thể lực, tổ chức giải bóng đá truyền thống, chạy việt dã và hội thao sinh viên khỏe hằng năm.',
     'https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=200&q=80',
     'ACTIVE', 'Câu lạc bộ / Đội / Nhóm', 'CLB Thể thao và Sức khỏe Sinh viên - Đại học Cần Thơ', 'Hội Sinh viên Đại học Cần Thơ', 'thethao@ctu.edu.vn', '0939778899', NULL, TRUE, NOW(), NOW()),
(10, 'CLB Sáng tạo và Khởi nghiệp Sinh viên - Đại học Cần Thơ',
     'Đơn vị ươm mầm ý tưởng khởi nghiệp đổi mới sáng tạo cho sinh viên các khối ngành tại Đại học Cần Thơ (đang chờ Super Admin xét duyệt).',
     'https://images.unsplash.com/photo-1552664730-d307ca884978?auto=format&fit=crop&w=200&q=80',
     'PENDING', 'Câu lạc bộ / Đội / Nhóm', 'CLB Sáng tạo và Khởi nghiệp Sinh viên - Đại học Cần Thơ', 'Hội Sinh viên Đại học Cần Thơ', 'startup.club@ctu.edu.vn', '0907112233', 'https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=1080&q=80', TRUE, NOW(), NOW()),
(11, 'Liên chi hội Sinh viên Đồng Tháp - Đại học Cần Thơ',
     'Kênh hoạt động chính thức của sinh viên đồng hương Đồng Tháp tại Đại học Cần Thơ (hồ sơ mở Fanpage chờ duyệt).',
     'https://images.unsplash.com/photo-1464226184884-fa280b87c399?auto=format&fit=crop&w=200&q=80',
     'PENDING', 'STUDENT_ASSOCIATION', 'Liên chi hội Sinh viên Đồng Tháp - Đại học Cần Thơ', 'Hội Sinh viên Đại học Cần Thơ', 'lchsv.dongthap@ctu.edu.vn', '0907445566', 'https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=1080&q=80', TRUE, NOW(), NOW());

INSERT INTO fanpage_members (fanpage_id, user_id, role, is_active, created_at, updated_at) VALUES
(1, 2, 'ADMIN', TRUE, NOW(), NOW()),
(2, 2, 'ADMIN', TRUE, NOW(), NOW()),
(3, 7, 'ADMIN', TRUE, NOW(), NOW()),
(4, 7, 'ADMIN', TRUE, NOW(), NOW()),
(5, 7, 'ADMIN', TRUE, NOW(), NOW()),
(6, 5, 'ADMIN', TRUE, NOW(), NOW()),
(7, 7, 'ADMIN', TRUE, NOW(), NOW()),
(7, 2, 'MEMBER', TRUE, NOW(), NOW()),
(8, 6, 'ADMIN', TRUE, NOW(), NOW()),
(9, 7, 'ADMIN', TRUE, NOW(), NOW()),
(10, 5, 'ADMIN', TRUE, NOW(), NOW()),
(11, 2, 'ADMIN', TRUE, NOW(), NOW());

-- Tài khoản loc@student.ctu.edu.vn đang follow 4 fanpage quen thuộc (để tab Đang theo dõi có bài ngay)
INSERT INTO followings (fanpage_id, user_id, fol_status, is_active, created_at, updated_at) VALUES
(1, 1, 'FOLLOWING', TRUE, NOW() - INTERVAL '40 days', NOW()),
(2, 1, 'FOLLOWING', TRUE, NOW() - INTERVAL '35 days', NOW()),
(4, 1, 'FOLLOWING', TRUE, NOW() - INTERVAL '25 days', NOW()),
(7, 1, 'FOLLOWING', TRUE, NOW() - INTERVAL '50 days', NOW()),
(6, 4, 'FOLLOWING', TRUE, NOW() - INTERVAL '20 days', NOW()),
(7, 4, 'FOLLOWING', TRUE, NOW() - INTERVAL '20 days', NOW());

-- ----------------------------------------------------------------------------
-- 7. 32 SỰ KIỆN THỰC TẾ CTU PHÂN BỐ ĐỦ TRẠNG THÁI & LỊCH SỬ
--    - ID 1..14 : PUBLISHED & ĐANG MỞ ĐĂNG KÝ (hiển thị đẹp trên Trang chủ)
--    - ID 15..18: PUBLISHED nhưng SẮP MỞ ĐĂNG KÝ (date_open tương lai -> hiện ở Tìm kiếm)
--    - ID 19..21: PUBLISHED nhưng ĐÃ ĐÓNG ĐĂNG KÝ (date_close quá khứ -> hiện ở Tìm kiếm)
--    - ID 22..30: COMPLETED / CLOSED trong 8 tháng qua (hiện ở Lịch sử, Chứng nhận, ĐRL, SV5T, Thống kê)
--    - ID 31..32: CANCELLED (hiện ở tab Đã hủy của Lịch sử)
-- ----------------------------------------------------------------------------
INSERT INTO events (
    id, name, description, location,
    date_open, date_close, date_happen,
    capacity, male_quantity, female_quantity, banner_url,
    status, allow_self_check,
    event_type_id, criteria_id, fanpage_id, semester_id, is_active, created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES

-- === NHÓM A: 14 SỰ KIỆN ĐANG MỞ ĐĂNG KÝ (TRANG CHỦ) ===
(1, 'Workshop: Xây dựng ứng dụng AI thực chiến với Spring Boot & React',
 E'Nhằm giúp sinh viên CNTT tiếp cận kiến trúc hiện đại khi tích hợp mô hình ngôn ngữ lớn (LLM) vào ứng dụng Web, CLB Tin học phối hợp Đoàn Trường CNTT&TT tổ chức buổi Workshop chuyên sâu.\n\nNội dung chính:\n- Thiết kế RESTful API chuẩn Clean Architecture với Spring Boot\n- Tích hợp Gemini API cho hệ thống gợi ý và tìm kiếm ngữ nghĩa\n- Tối ưu trải nghiệm người dùng trên React & TailwindCSS\n- Giao lưu hỏi đáp trực tiếp cùng cựu sinh viên CICT đang làm việc tại doanh nghiệp.\n\nSinh viên tham gia đầy đủ và check-out cuối buổi sẽ được cộng điểm rèn luyện mục I và cấp giấy chứng nhận điện tử.',
 'Hội trường 1 - Trường CNTT&TT (Khu II ĐHCT)',
 NOW() - INTERVAL '5 days', NOW() + INTERVAL '20 hours', NOW() + INTERVAL '4 days',
 120, 60, 60, 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 1, 5023, 2, 1, TRUE, NOW() - INTERVAL '5 days', NOW()),

(2, 'Ngày hội Việc làm CNTT & Kết nối Doanh nghiệp CICT 2026',
 E'Sự kiện thường niên lớn nhất trong năm của Trường Công nghệ Thông tin và Truyền thông dành cho sinh viên khóa 47, 48, 49 chuẩn bị thực tập và định hướng nghề nghiệp.\n\nQuyền lợi khi tham gia:\n- Gặp gỡ trực tiếp hơn 25 doanh nghiệp công nghệ tại Cần Thơ và TP.HCM\n- Được góp ý chỉnh sửa CV 1-1 cùng chuyên gia nhân sự\n- Phỏng vấn thử nhận suất thực tập có lương\n- Cộng điểm rèn luyện mục V và ghi nhận hoạt động hướng nghiệp.',
 'Sảnh Trường Công nghệ Thông tin và Truyền thông - Khu II ĐHCT',
 NOW() - INTERVAL '7 days', NOW() + INTERVAL '6 days', NOW() + INTERVAL '10 days',
 300, 150, 150, 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 8, 5033, 1, 1, TRUE, NOW() - INTERVAL '7 days', NOW()),

(3, 'Chiến dịch "Chủ nhật Xanh" - Làm sạch cảnh quan Khu II ĐHCT',
 E'Hưởng ứng phong trào xây dựng môi trường học đường Xanh - Sạch - Đẹp, Đội Công tác Xã hội Đại học Cần Thơ phát động chiến dịch Chủ nhật Xanh đợt 2 học kỳ 1.\n\nCông việc cụ thể:\n- Thu gom rác thải nhựa và phân loại rác tại khuôn viên Khu II, đường 3/2\n- Chăm sóc cây xanh khu vực Ký túc xá A & B\n- Tuyên truyền giảm thiểu túi ni-lông dùng một lần trong sinh viên.\n\nLưu ý: Sinh viên mang trang phục gọn gàng (ưu tiên áo Thanh niên Việt Nam hoặc áo CLB), BTC trang bị sẵn bao tay và nước uống.',
 'Sân Nhà Điều hành - Khu II Đại học Cần Thơ',
 NOW() - INTERVAL '4 days', NOW() + INTERVAL '30 hours', NOW() + INTERVAL '3 days',
 100, 50, 50, 'https://images.unsplash.com/photo-1542601906990-b4d3fb778b09?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 16, 402, 4, 1, TRUE, NOW() - INTERVAL '4 days', NOW()),

(4, 'Ngày hội Hiến máu Nhân đạo "Giọt hồng Tây Đô" Đợt 2/2026',
 E'Mỗi giọt máu cho đi, một cuộc đời ở lại. CLB Hiến máu Tình nguyện ĐHCT phối hợp Bệnh viện Huyết học - Truyền máu TP. Cần Thơ tổ chức chương trình hiến máu tình nguyện dành cho toàn thể cán bộ, giảng viên và sinh viên.\n\nQuyền lợi & Lưu ý:\n- Được khám sàng lọc sức khỏe miễn phí trước khi hiến máu\n- Nhận giấy chứng nhận hiến máu tình nguyện (tính vào tiêu chí 4.3 Sinh viên 5 tốt)\n- Cộng điểm rèn luyện mục IV hoạt động vì cộng đồng\n- Ăn nhẹ đầy đủ trước khi đến, mang theo CCCD hoặc thẻ sinh viên.',
 'Hội trường Rùa - Khu II Đại học Cần Thơ',
 NOW() - INTERVAL '6 days', NOW() + INTERVAL '5 days', NOW() + INTERVAL '7 days',
 250, 125, 125, 'https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', FALSE,
 15, 403, 5, 1, TRUE, NOW() - INTERVAL '6 days', NOW()),

(5, 'Giải Việt dã Sinh viên CTU Khỏe Chào mừng Ngày Nhà giáo 20/11',
 E'CLB Thể thao & Sức khỏe Sinh viên CTU tổ chức giải chạy Việt dã cự ly 3km (Nữ) và 5km (Nam) quanh khuôn viên Khu II.\n\nTham gia giải chạy giúp sinh viên rèn luyện thể lực, giao lưu kết bạn và đủ điều kiện xét tiêu chí Thể lực tốt (3.2) trong danh hiệu Sinh viên 5 tốt.\nCơ cấu giải thưởng gồm Huy chương, Giấy khen cấp Hội Sinh viên và điểm rèn luyện mục III.',
 'Sân vận động Khu II - Đại học Cần Thơ',
 NOW() - INTERVAL '3 days', NOW() + INTERVAL '8 days', NOW() + INTERVAL '12 days',
 180, 90, 90, 'https://images.unsplash.com/photo-1452626038306-9aae5e071dd3?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 3, 302, 9, 1, TRUE, NOW() - INTERVAL '3 days', NOW()),

(6, 'English Speaking Arena: Cross-Cultural Integration & Career',
 E'CLB Tiếng Anh CTU tổ chức buổi sinh hoạt chuyên đề giao tiếp tiếng Anh với chủ đề Hội nhập văn hóa và Kỹ năng phỏng vấn bằng tiếng Anh.\n\nHình thức: Thảo luận nhóm nhỏ cùng sinh viên quốc tế đang trao đổi tại ĐHCT, mini-game phản xạ và thuyết trình ngắn.\nHoạt động được ghi nhận vào tiêu chí 5.3.1 (Hoạt động hội nhập cấp ĐHCT) và cộng điểm rèn luyện mục I.',
 'Phòng Hội thảo Tầng 2 - Trung tâm Học liệu ĐHCT',
 NOW() - INTERVAL '2 days', NOW() + INTERVAL '7 days', NOW() + INTERVAL '9 days',
 80, 40, 40, 'https://images.unsplash.com/photo-1543269865-cbf427effbad?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 14, 5031, 3, 1, TRUE, NOW() - INTERVAL '2 days', NOW()),

(7, 'Hackathon Đổi mới Sáng tạo Số vùng Đồng bằng sông Cửu Long 2026',
 E'Cuộc thi lập trình 24 giờ tìm kiếm giải pháp công nghệ phục vụ nông nghiệp thông minh, chuyển đổi số giáo dục và quản lý đô thị xanh cho khu vực ĐBSCL.\n\nĐăng ký theo đội (tối đa 4 sinh viên/đội) hoặc đăng ký cá nhân để BTC ghép đội.\nTop 10 đội xuất sắc nhận giấy khen cấp Đại học Cần Thơ và cộng điểm mục V.',
 'Nhà học B1 & Phòng Lab Trường CNTT&TT',
 NOW() - INTERVAL '8 days', NOW() + INTERVAL '10 days', NOW() + INTERVAL '15 days',
 160, 80, 80, 'https://images.unsplash.com/photo-1504384308090-c894fdcc538d?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 7, 5023, 1, 1, TRUE, NOW() - INTERVAL '8 days', NOW()),

(8, 'Tọa đàm: Khởi nghiệp Kinh tế Số & Thương mại Điện tử Bền vững',
 E'Đoàn Trường Kinh tế mời các diễn giả là nhà sáng lập doanh nghiệp thương mại điện tử tại Cần Thơ chia sẻ hành trình xây dựng thương hiệu từ giảng đường đại học.\n\nChủ đề trọng tâm: Mô hình kinh doanh tinh gọn, ứng dụng phân tích dữ liệu trong Marketing và gọi vốn cho sinh viên.',
 'Hội trường Trường Kinh tế - Khu II ĐHCT',
 NOW() - INTERVAL '5 days', NOW() + INTERVAL '9 days', NOW() + INTERVAL '13 days',
 150, 75, 75, 'https://images.unsplash.com/photo-1515187029135-18ee286d815b?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 6, 5034, 6, 1, TRUE, NOW() - INTERVAL '5 days', NOW()),

(9, 'Lớp Tập huấn Kỹ năng Lãnh đạo & Tổ chức Sự kiện cho Cán bộ Đoàn - Hội',
 E'Đoàn Thanh niên - Hội Sinh viên ĐHCT tổ chức lớp tập huấn nâng cao nghiệp vụ công tác Đoàn - Hội, kỹ năng lập kế hoạch, điều phối nhân sự và quản lý rủi ro sự kiện.\n\nDành cho Ban chấp hành Chi đoàn, Chi hội, Chủ nhiệm CLB và sinh viên có định hướng phấn đấu danh hiệu Sinh viên 5 tốt (tiêu chí 5.3.3).',
 'Hội trường Khoa Khoa học Chính trị - Khu II ĐHCT',
 NOW() - INTERVAL '4 days', NOW() + INTERVAL '11 days', NOW() + INTERVAL '14 days',
 200, 100, 100, 'https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 13, 5033, 7, 1, TRUE, NOW() - INTERVAL '4 days', NOW()),

(10, 'Hội thảo Trực tuyến: An toàn Thông tin & Phòng chống Lừa đảo Không gian mạng',
 E'Buổi chuyên đề trực tuyến cung cấp kiến thức bảo mật tài khoản cá nhân, nhận diện mã độc, phishing và tuân thủ quy định an toàn thông tin trong môi trường đại học.\n\nLink phòng họp Google Meet sẽ được gửi đến các bạn đã đăng ký trước 1 giờ diễn ra sự kiện.',
 'Trực tuyến qua Google Meet (Link trong email xác nhận)',
 NOW() - INTERVAL '3 days', NOW() + INTERVAL '6 days', NOW() + INTERVAL '8 days',
 400, 200, 200, 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 5, 1033, 1, 1, TRUE, NOW() - INTERVAL '3 days', NOW()),

(11, 'Kỳ thi Tuyển chọn Đội tuyển Olympic Tin học Sinh viên CTU 2026',
 E'CLB Tin học CICT phối hợp Bộ môn Khoa học Máy tính tổ chức kỳ thi sơ tuyển tìm kiếm thành viên đội tuyển Olympic Tin học Đại học Cần Thơ tham dự vòng toàn quốc.\n\nNội dung thi: Giải thuật, Cấu trúc dữ liệu, Quy hoạch động trên hệ thống chấm bài tự động.',
 'Phòng Máy tính Thực hành CICT - Tầng 2',
 NOW() - INTERVAL '6 days', NOW() + INTERVAL '12 days', NOW() + INTERVAL '16 days',
 90, 45, 45, 'https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 7, 5023, 2, 1, TRUE, NOW() - INTERVAL '6 days', NOW()),

(12, 'Hội nghị Phổ biến Quy chế Rèn luyện & Đổi mới Giáo dục Đạo đức Lối sống',
 E'Chương trình sinh hoạt chính trị đầu khóa và phổ biến nội quy nhà trường, văn hóa ứng xử học đường giúp sinh viên rèn luyện lối sống văn minh, trách nhiệm công dân.\n\nTham dự đầy đủ được ghi nhận tiêu chí 1.3.3 Đạo đức tốt và cộng điểm rèn luyện mục II.',
 'Hội trường Rùa - Khu II Đại học Cần Thơ',
 NOW() - INTERVAL '2 days', NOW() + INTERVAL '15 days', NOW() + INTERVAL '18 days',
 500, 250, 250, 'https://images.unsplash.com/photo-1523580494863-6f3031224c94?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 12, 1033, 7, 1, TRUE, NOW() - INTERVAL '2 days', NOW()),

(13, 'Workshop Kỹ năng Thuyết trình Tự tin & Xây dựng Thương hiệu Cá nhân',
 E'Buổi hướng dẫn thực hành trực tiếp kỹ năng làm slide chuyên nghiệp, làm chủ sân khấu và giao tiếp hiệu quả khi bảo vệ đồ án hoặc phỏng vấn học bổng.',
 'Hội trường B - Trường Bách khoa ĐHCT',
 NOW() - INTERVAL '1 day', NOW() + INTERVAL '10 days', NOW() + INTERVAL '12 days',
 110, 55, 55, 'https://images.unsplash.com/photo-1475721027785-f74eccf877e2?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 13, 5033, 8, 1, TRUE, NOW() - INTERVAL '1 day', NOW()),

(14, 'Ngày hội Tình nguyện "Chung tay vì Cộng đồng & Môi trường Sạch"',
 E'Hoạt động tình nguyện cuối tuần kết hợp thu gom pin cũ, đổi rác lấy cây xanh và tặng quà cho các em học sinh khó khăn vùng ven TP. Cần Thơ.',
 'Công viên Lưu Hữu Phước & Khu II ĐHCT',
 NOW() - INTERVAL '3 days', NOW() + INTERVAL '4 days', NOW() + INTERVAL '6 days',
 140, 70, 70, 'https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 4, 402, 4, 1, TRUE, NOW() - INTERVAL '3 days', NOW()),

-- === NHÓM B: 4 SỰ KIỆN PUBLISHED CHƯA MỞ ĐĂNG KÝ (HIỂN THỊ KHI TÌM KIẾM) ===
(15, 'Hội thảo Trí tuệ Nhân tạo Tạo sinh (GenAI) trong Nghiên cứu Khoa học',
 E'Giới thiệu phương pháp ứng dụng AI hỗ trợ tổng hợp tài liệu khoa học, phân tích dữ liệu thí nghiệm và tuân thủ liêm chính học thuật.',
 'Hội trường 1 - Trường CNTT&TT',
 NOW() + INTERVAL '3 days', NOW() + INTERVAL '15 days', NOW() + INTERVAL '20 days',
 120, 60, 60, 'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 5, 5023, 2, 1, TRUE, NOW(), NOW()),

(16, 'Giải Bóng đá Truyền thống Sinh viên ĐHCT Cup Mùa Thu 2026',
 E'Giải đấu bóng đá nam/nữ 5 người dành cho sinh viên các Liên chi hội và các Trường trực thuộc tranh tài.',
 'Sân Bóng đá Khu II - ĐHCT',
 NOW() + INTERVAL '4 days', NOW() + INTERVAL '14 days', NOW() + INTERVAL '17 days',
 200, 120, 80, 'https://images.unsplash.com/photo-1574629810360-7efbbe195018?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 3, 302, 9, 1, TRUE, NOW(), NOW()),

(17, 'Diễn đàn Giao lưu Sinh viên Quốc tế & Văn hóa Mekong 2026',
 E'Cơ hội kết nối giữa sinh viên ĐHCT với lưu học sinh Lào, Campuchia, Thái Lan, Nhật Bản và châu Âu.',
 'Hội trường Nhà Điều hành ĐHCT',
 NOW() + INTERVAL '4 days', NOW() + INTERVAL '14 days', NOW() + INTERVAL '18 days',
 180, 90, 90, 'https://images.unsplash.com/photo-1511632765486-a01980e01a18?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 2, 5031, 3, 1, TRUE, NOW(), NOW()),

(18, 'Triển lãm Sản phẩm Sáng tạo Kỹ thuật & Tự động hóa Bách khoa',
 E'Trưng bày hơn 30 mô hình robot tự hành, hệ thống IoT nông nghiệp thông minh và đồ án tốt nghiệp tiêu biểu.',
 'Sảnh Trường Bách khoa - Khu II ĐHCT',
 NOW() + INTERVAL '2 days', NOW() + INTERVAL '14 days', NOW() + INTERVAL '19 days',
 250, 125, 125, 'https://images.unsplash.com/photo-1505373877841-8d25f7d46678?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 17, 5023, 8, 1, TRUE, NOW(), NOW()),

-- === NHÓM C: 3 SỰ KIỆN PUBLISHED ĐÃ ĐÓNG ĐĂNG KÝ (HIỂN THỊ KHI TÌM KIẾM) ===
(19, 'Tọa đàm Hướng nghiệp: Con đường trở thành Software Engineer chuyên nghiệp',
 E'Chương trình giao lưu cùng các Tech Lead đến từ các công ty phần mềm hàng đầu, giải đáp lộ trình từ sinh viên năm 3 lên Junior/Middle.',
 'Hội trường 1 - Trường CNTT&TT',
 NOW() - INTERVAL '12 days', NOW() - INTERVAL '1 day', NOW() + INTERVAL '2 days',
 100, 50, 50, 'https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 8, 5023, 1, 1, TRUE, NOW() - INTERVAL '12 days', NOW()),

(20, 'Cuộc thi Tìm hiểu Tư tưởng Hồ Chí Minh & Truyền thống ĐHCT 2026',
 E'Cuộc thi trắc nghiệm và hùng biện trực tiếp về lịch sử truyền thống và tư tưởng đạo đức Hồ Chí Minh.',
 'Hội trường Khoa KHCT - ĐHCT',
 NOW() - INTERVAL '15 days', NOW() - INTERVAL '2 days', NOW() + INTERVAL '1 day',
 150, 75, 75, 'https://images.unsplash.com/photo-1531482615713-2afd69097998?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 7, 1031, 7, 1, TRUE, NOW() - INTERVAL '15 days', NOW()),

(21, 'Chương trình "Giao lưu Âm nhạc Acoustic & Kết nối Tân sinh viên"',
 E'Đêm nhạc giao lưu văn nghệ gây quỹ hỗ trợ sinh viên vượt khó học giỏi đầu năm học.',
 'Sân khấu ngoài trời KTX Khu B - ĐHCT',
 NOW() - INTERVAL '10 days', NOW() - INTERVAL '6 hours', NOW() + INTERVAL '1 day',
 200, 100, 100, 'https://images.unsplash.com/photo-1528605248644-14dd04022da1?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 9, 1033, 4, 1, TRUE, NOW() - INTERVAL '10 days', NOW()),

-- === NHÓM D: 9 SỰ KIỆN ĐÃ KẾT THÚC / ĐÃ ĐÓNG TRẢI ĐỀU 8 THÁNG QUA (LỊCH SỬ, ĐRL, SV5T, THỐNG KÊ) ===
(22, 'Tuần Sinh hoạt Công dân Sinh viên Khóa 49 - Đầu năm học 2026-2027',
 E'Phổ biến quy chế đào tạo tín chỉ, nội quy nhà trường, phòng chống tệ nạn xã hội và định hướng rèn luyện cho sinh viên.',
 'Hội trường Rùa - Khu II ĐHCT',
 NOW() - INTERVAL '45 days', NOW() - INTERVAL '32 days', NOW() - INTERVAL '30 days',
 500, 250, 250, 'https://images.unsplash.com/photo-1523580494863-6f3031224c94?auto=format&fit=crop&w=1080&q=80',
 'COMPLETED', TRUE,
 12, 1033, 7, 1, TRUE, NOW() - INTERVAL '45 days', NOW()),

(23, 'Ngày hội Hiến máu Tình nguyện "Sẻ chia giọt hồng" Đợt 1/2026',
 E'Đợt hiến máu đầu học kỳ 1 thu hút hơn 200 đơn vị máu từ sinh viên Đại học Cần Thơ.',
 'Hội trường Rùa - Khu II ĐHCT',
 NOW() - INTERVAL '35 days', NOW() - INTERVAL '22 days', NOW() - INTERVAL '20 days',
 220, 110, 110, 'https://images.unsplash.com/photo-1526256262350-7da7584cf5eb?auto=format&fit=crop&w=1080&q=80',
 'COMPLETED', FALSE,
 15, 403, 5, 1, TRUE, NOW() - INTERVAL '35 days', NOW()),

(24, 'Chiến dịch "Thứ Bảy Tình nguyện" Dọn vệ sinh giảng đường B1 & CICT',
 E'Hoạt động tình nguyện đợt 1 học kỳ 1 tại Khu II Đại học Cần Thơ.',
 'Khu II - Đại học Cần Thơ',
 NOW() - INTERVAL '28 days', NOW() - INTERVAL '16 days', NOW() - INTERVAL '14 days',
 100, 50, 50, 'https://images.unsplash.com/photo-1559027615-cd4628902d4a?auto=format&fit=crop&w=1080&q=80',
 'COMPLETED', TRUE,
 4, 402, 4, 1, TRUE, NOW() - INTERVAL '28 days', NOW()),

(25, 'Workshop: Git, Docker & CI/CD thực tế cho Sinh viên Phần mềm',
 E'Hướng dẫn sử dụng Git flow, đóng gói Docker container và triển khai tự động.',
 'Phòng Lab 201 - Trường CNTT&TT',
 NOW() - INTERVAL '20 days', NOW() - INTERVAL '10 days', NOW() - INTERVAL '8 days',
 90, 45, 45, 'https://images.unsplash.com/photo-1531403009284-440f080d1e12?auto=format&fit=crop&w=1080&q=80',
 'COMPLETED', TRUE,
 1, 5023, 2, 1, TRUE, NOW() - INTERVAL '20 days', NOW()),

(26, 'Hội thảo Khởi nghiệp & Phân tích Tài chính Cá nhân cho Sinh viên',
 E'Trang bị kỹ năng quản lý tài chính và tư duy khởi nghiệp cho sinh viên ĐHCT.',
 'Hội trường Trường Kinh tế',
 NOW() - INTERVAL '60 days', NOW() - INTERVAL '48 days', NOW() - INTERVAL '45 days',
 140, 70, 70, 'https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?auto=format&fit=crop&w=1080&q=80',
 'COMPLETED', TRUE,
 6, 5034, 6, 1, TRUE, NOW() - INTERVAL '60 days', NOW()),

(27, 'Lễ Tuyên dương "Sinh viên 5 tốt" & Giải thưởng Ngôi sao tháng Giêng',
 E'Vinh danh các gương mặt sinh viên tiêu biểu đạt danh hiệu Sinh viên 5 tốt cấp Trường và cấp Thành phố.',
 'Hội trường Rùa - Khu II ĐHCT',
 '2026-05-05 08:00:00+07', '2026-05-15 17:00:00+07', '2026-05-18 08:00:00+07',
 300, 150, 150, 'https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=1080&q=80',
 'COMPLETED', FALSE,
 10, 5032, 7, 103, TRUE, '2026-05-05 08:00:00+07', NOW()),

(28, 'Chiến dịch Mùa Hè Xanh ĐHCT 2026 - Mặt trận Hậu Giang & Cần Thơ',
 E'Chiến dịch tình nguyện hè quy mô lớn của sinh viên Đại học Cần Thơ hỗ trợ xây dựng nông thôn mới và phổ cập tin học.',
 'Xã Tân Thành, Ngã Bảy & Phong Điền, Cần Thơ',
 '2026-06-20 08:00:00+07', '2026-07-04 17:00:00+07', '2026-07-08 07:00:00+07',
 150, 75, 75, 'https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?auto=format&fit=crop&w=1080&q=80',
 'COMPLETED', FALSE,
 4, 402, 4, 103, TRUE, '2026-06-20 08:00:00+07', NOW()),

(29, 'Giải Chạy Bộ & Hội thao Sinh viên CICT Open 2026',
 E'Sân chơi thể dục thể thao rèn luyện sức khỏe học kỳ 2 năm học 2025-2026.',
 'Sân vận động Khu II ĐHCT',
 '2026-03-01 08:00:00+07', '2026-03-18 17:00:00+07', '2026-03-22 06:30:00+07',
 120, 60, 60, 'https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=1080&q=80',
 'COMPLETED', FALSE,
 3, 302, 1, 102, TRUE, '2026-03-01 08:00:00+07', NOW()),

(30, 'Cuộc thi Thiết kế Giao diện UI/UX & Sản phẩm Số CICT 2025',
 E'Cuộc thi học thuật dành cho sinh viên đam mê thiết kế sản phẩm phần mềm và trải nghiệm người dùng.',
 'Trường CNTT&TT - Khu II ĐHCT',
 '2025-10-20 08:00:00+07', '2025-11-10 17:00:00+07', '2025-11-15 08:00:00+07',
 100, 50, 50, 'https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=1080&q=80',
 'CLOSED', FALSE,
 7, 5023, 2, 101, TRUE, '2025-10-20 08:00:00+07', NOW()),

-- === NHÓM E: 2 SỰ KIỆN ĐÃ HỦY (ĐỂ TEST TAB ĐÃ HỦY) ===
(31, 'Buổi dã ngoại Giao lưu Tân sinh viên tại Cồn Sơn (Tạm hoãn do thời tiết)',
 E'Sự kiện dã ngoại ngoài trời tạm hủy do dự báo mưa bão lớn khu vực sông Hậu.',
 'Bến phà Cô Bắc - Cồn Sơn, Cần Thơ',
 NOW() - INTERVAL '15 days', NOW() - INTERVAL '7 days', NOW() - INTERVAL '5 days',
 60, 30, 30, 'https://images.unsplash.com/photo-1464226184884-fa280b87c399?auto=format&fit=crop&w=1080&q=80',
 'CANCELLED', TRUE,
 2, 1032, 3, 1, TRUE, NOW() - INTERVAL '15 days', NOW()),

(32, 'Workshop Thực hành Thiết bị Mạng Cisco (Chuyển lịch sang HK2)',
 E'Do bảo trì phòng Lab mạng, sự kiện tạm hủy ở HK1 và sẽ mở lại vào đầu HK2.',
 'Phòng Lab Mạng - Trường CNTT&TT',
 NOW() - INTERVAL '22 days', NOW() - INTERVAL '12 days', NOW() - INTERVAL '10 days',
 50, 25, 25, 'https://images.unsplash.com/photo-1544652478-6653e09f18a2?auto=format&fit=crop&w=1080&q=80',
 'CANCELLED', TRUE,
 1, 5023, 2, 1, TRUE, NOW() - INTERVAL '22 days', NOW()),

-- === NHÓM F: 3 SỰ KIỆN ĐÃ KẾT THÚC TRONG HK1 2026-2027 CỦA FANPAGE #1 (ĐỦ DỮ LIỆU BIỂU ĐỒ & VẮNG NHIỀU LẦN) ===
(33, 'Chào đón Tân sinh viên Khóa 52 - Trường Công nghệ Thông tin và Truyền thông',
 E'Chương trình gặp gỡ Ban Chủ nhiệm Trường, định hướng lộ trình học tập và giới thiệu các câu lạc bộ học thuật cho tân sinh viên.',
 'Hội trường 1 - Trường CNTT&TT (Khu II ĐHCT)',
 '2026-09-05 08:00:00+07', '2026-09-18 17:00:00+07', '2026-09-20 07:30:00+07',
 300, 150, 150, 'https://images.unsplash.com/photo-1523580494863-6f3031224c94?auto=format&fit=crop&w=1080&q=80',
 'COMPLETED', FALSE,
 12, 1033, 1, 1, TRUE, '2026-09-05 08:00:00+07', NOW()),

(34, 'Tập huấn Kỹ năng Lập trình thi đấu ACM/ICPC & An toàn thông tin',
 E'Bồi dưỡng kỹ năng giải thuật và thực hành phòng thủ hệ thống mạng cho đội tuyển sinh viên CNTT&TT.',
 'Phòng Lab 201 - Trường CNTT&TT (Khu II ĐHCT)',
 '2026-10-02 08:00:00+07', '2026-10-16 17:00:00+07', '2026-10-18 08:00:00+07',
 120, 60, 60, 'https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=1080&q=80',
 'COMPLETED', FALSE,
 1, 5023, 1, 1, TRUE, '2026-10-02 08:00:00+07', NOW()),

(35, 'Ngày hội Chuyển đổi số & Sáng tạo Sinh viên Trường CNTT&TT 2026',
 E'Trưng bày đồ án sáng tạo, trải nghiệm công nghệ mới và giao lưu với diễn giả doanh nghiệp công nghệ tại Cần Thơ.',
 'Sảnh Trường Công nghệ Thông tin và Truyền thông - Khu II ĐHCT',
 '2026-10-25 08:00:00+07', '2026-11-08 17:00:00+07', '2026-11-10 08:00:00+07',
 250, 125, 125, 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=1080&q=80',
 'COMPLETED', FALSE,
 5, 5033, 1, 1, TRUE, '2026-10-25 08:00:00+07', NOW()),

-- === NHÓM G: SỰ KIỆN FANPAGE #1 Ở HK1 & HK3 NĂM HỌC 2025-2026 (ĐỂ MỌI HỌC KỲ ĐỀU CÓ DỮ LIỆU THỐNG KÊ) ===
(36, 'Ngày hội Chào đón Tân sinh viên Khóa 51 & Kết nối Doanh nghiệp CICT',
 E'Sự kiện đầu khóa năm học 2025-2026 dành cho sinh viên Trường Công nghệ Thông tin và Truyền thông.',
 'Hội trường Rùa - Khu II ĐHCT',
 '2025-09-25 08:00:00+07', '2025-10-10 17:00:00+07', '2025-10-12 08:00:00+07',
 280, 140, 140, 'https://images.unsplash.com/photo-1523580494863-6f3031224c94?auto=format&fit=crop&w=1080&q=80',
 'COMPLETED', FALSE,
 12, 1033, 1, 101, TRUE, '2025-09-25 08:00:00+07', NOW()),

(37, 'Lớp Bồi dưỡng Kỹ năng Công nghệ & Thiết kế Web Mùa hè 2026',
 E'Khóa tập huấn chuyên đề Học kỳ 3 giúp sinh viên rèn luyện kỹ năng thực hành xây dựng ứng dụng Web trong dịp hè.',
 'Phòng Lab 201 - Trường CNTT&TT (Khu II ĐHCT)',
 '2026-06-01 08:00:00+07', '2026-06-12 17:00:00+07', '2026-06-14 08:00:00+07',
 120, 60, 60, 'https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=1080&q=80',
 'COMPLETED', FALSE,
 1, 5023, 1, 103, TRUE, '2026-06-01 08:00:00+07', NOW()),

(38, 'Trại Huấn luyện Cán bộ Đoàn - Hội Trường CNTT&TT 2026',
 E'Hoạt động rèn luyện kỹ năng tổ chức phong trào, làm việc nhóm và sinh hoạt tập thể trong Học kỳ 3 năm học 2025-2026.',
 'Khu II - Đại học Cần Thơ',
 '2026-07-05 08:00:00+07', '2026-07-17 17:00:00+07', '2026-07-19 07:30:00+07',
 150, 75, 75, 'https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=1080&q=80',
 'COMPLETED', FALSE,
 9, 5033, 1, 103, TRUE, '2026-07-05 08:00:00+07', NOW()),

-- === NHÓM H: 2 SỰ KIỆN ĐANG DIỄN RA HIỆN TẠI (DEMO GỬI ẢNH CHECK-IN & ĐIỂM DANH THỦ CÔNG) ===
(39, 'Workshop Thực hành Cloud Computing & DevOps Thực chiến 2026 (Đang diễn ra)',
 E'Phiên thực hành triển khai hệ thống trên Cloud và Docker trực tiếp tại phòng Lab. Sinh viên tham gia gửi ảnh Check-in và Check-out ngay trong buổi thực hành.',
 'Phòng Lab 302 - Trường Công nghệ Thông tin và Truyền thông',
 NOW() - INTERVAL '7 days', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour',
 100, 50, 50, 'https://images.unsplash.com/photo-1504384308090-c894fdcc538d?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE,
 1, 5023, 1, 1, TRUE, NOW() - INTERVAL '7 days', NOW()),

(40, 'Hội thảo Phỏng vấn Kỹ thuật & Review CV cùng Doanh nghiệp CNTT (Đang diễn ra)',
 E'Chương trình giao lưu trực tiếp với các kỹ sư tuyển dụng và góp ý CV cho sinh viên năm cuối. Ban tổ chức điểm danh thủ công trực tiếp tại hội trường.',
 'Hội trường 1 - Trường Công nghệ Thông tin và Truyền thông',
 NOW() - INTERVAL '7 days', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour',
 160, 80, 80, 'https://images.unsplash.com/photo-1552664730-d307ca884978?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', FALSE,
 6, 5033, 1, 1, TRUE, NOW() - INTERVAL '7 days', NOW());

-- Nếu DB đã chạy event-school-migration.sql (có cột school_id trên events), gắn phạm vi trường cho 3 sự kiện mẫu
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'events' AND column_name = 'school_id') THEN
        UPDATE events SET school_id = 1 WHERE id IN (1, 2, 11); -- Dành riêng cho Trường CNTT&TT
        UPDATE events SET school_id = 2 WHERE id IN (8);         -- Dành riêng cho Trường Kinh tế
        UPDATE events SET school_id = 3 WHERE id IN (13, 18);    -- Dành riêng cho Trường Bách khoa
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 8. GẮN ĐIỂM RÈN LUYỆN CHO TỪNG SỰ KIỆN (event_points)
--    Dùng đúng các ID lá trong point_categories của mydb!
-- ----------------------------------------------------------------------------
INSERT INTO event_points (event_id, point_category_id, point, is_active, created_at, updated_at) VALUES
(1,  1045, 2,  TRUE, NOW(), NOW()), -- CLB học thuật (I)
(1,  511,  3,  TRUE, NOW(), NOW()), -- Hỗ trợ/tham gia phong trào trường (V)
(2,  511,  4,  TRUE, NOW(), NOW()), -- Hướng nghiệp CICT (V)
(3,  2112, 4,  TRUE, NOW(), NOW()), -- Bảo vệ môi trường cấp ĐHCT (II)
(3,  411,  5,  TRUE, NOW(), NOW()), -- Công tác xã hội cộng đồng (IV)
(4,  411,  5,  TRUE, NOW(), NOW()), -- Hiến máu nhân đạo (IV)
(5,  301,  4,  TRUE, NOW(), NOW()), -- Hoạt động TDTT (III)
(6,  1045, 2,  TRUE, NOW(), NOW()), -- CLB Tiếng Anh (I)
(7,  1121, 2,  TRUE, NOW(), NOW()), -- Tham gia thi Hackathon (I)
(7,  529,  4,  TRUE, NOW(), NOW()), -- Thành tích cuộc thi học thuật (V)
(8,  528,  3,  TRUE, NOW(), NOW()), -- Tọa đàm khởi nghiệp (V)
(9,  3112, 4,  TRUE, NOW(), NOW()), -- Tập huấn lực lượng nòng cốt ĐHCT (III)
(10, 321,  3,  TRUE, NOW(), NOW()), -- Phòng chống tội phạm mạng (III)
(11, 1121, 2,  TRUE, NOW(), NOW()), -- Olympic Tin học (I)
(12, 201,  5,  TRUE, NOW(), NOW()), -- Chấp hành nội quy quy chế (II)
(13, 511,  3,  TRUE, NOW(), NOW()), -- Kỹ năng mềm (V)
(14, 411,  5,  TRUE, NOW(), NOW()), -- Tình nguyện cộng đồng (IV)
(15, 1021, 5,  TRUE, NOW(), NOW()),
(16, 301,  4,  TRUE, NOW(), NOW()),
(17, 401,  4,  TRUE, NOW(), NOW()),
(18, 1121, 2,  TRUE, NOW(), NOW()),
(19, 511,  3,  TRUE, NOW(), NOW()),
(20, 401,  4,  TRUE, NOW(), NOW()),
(21, 301,  3,  TRUE, NOW(), NOW()),
-- Các sự kiện đã hoàn thành (22..30)
(22, 201,  15, TRUE, NOW(), NOW()), -- Tuần SHCD: 15đ Mục II
(22, 321,  3,  TRUE, NOW(), NOW()), -- Phổ biến PCTP: 3đ Mục III
(23, 411,  5,  TRUE, NOW(), NOW()), -- Hiến máu đợt 1: 5đ Mục IV
(24, 2112, 4,  TRUE, NOW(), NOW()), -- Chủ nhật xanh đợt 1: 4đ Mục II
(24, 411,  5,  TRUE, NOW(), NOW()), -- Tình nguyện đợt 1: 5đ Mục IV
(25, 1045, 2,  TRUE, NOW(), NOW()), -- Workshop Git/Docker: 2đ Mục I
(25, 511,  3,  TRUE, NOW(), NOW()), -- Hỗ trợ/tham gia hoạt động CICT: 3đ Mục V
(26, 511,  4,  TRUE, NOW(), NOW()), -- Hội thảo Khởi nghiệp: 4đ Mục V
(27, 523,  6,  TRUE, NOW(), NOW()), -- Tuyên dương SV5T: 6đ Mục V (HK2 25-26)
(28, 411,  10, TRUE, NOW(), NOW()), -- Mùa hè xanh: 10đ Mục IV (HK2 25-26)
(28, 3112, 5,  TRUE, NOW(), NOW()), -- Nòng cốt ĐHCT: 5đ Mục III (HK2 25-26)
(29, 301,  6,  TRUE, NOW(), NOW()), -- Giải chạy CICT: 6đ Mục III (HK2 25-26)
(30, 1122, 4,  TRUE, NOW(), NOW()), -- Cuộc thi UI/UX: 4đ Mục I (HK1 25-26)
(33, 201,  4,  TRUE, NOW(), NOW()), -- Chào đón K52: 4đ Mục II (HK1 26-27)
(34, 1045, 3,  TRUE, NOW(), NOW()), -- Tập huấn ACM/ICPC: 3đ Mục I (HK1 26-27)
(35, 511,  4,  TRUE, NOW(), NOW()), -- Chuyển đổi số CNTT&TT: 4đ Mục V (HK1 26-27)
(36, 201,  4,  TRUE, NOW(), NOW()), -- Chào đón K51: 4đ Mục II (HK1 25-26)
(37, 1045, 3,  TRUE, NOW(), NOW()), -- Kỹ năng Web Hè: 3đ Mục I (HK3 25-26)
(38, 511,  4,  TRUE, NOW(), NOW()), -- Trại Huấn luyện Cán bộ: 4đ Mục V (HK3 25-26)
(39, 1045, 2,  TRUE, NOW(), NOW()), -- Cloud & DevOps (Đang diễn ra - Self check): 2đ Mục I
(39, 511,  3,  TRUE, NOW(), NOW()), -- Cloud & DevOps: 3đ Mục V
(40, 511,  4,  TRUE, NOW(), NOW()); -- Review CV (Đang diễn ra - Điểm danh thủ công): 4đ Mục V

-- ----------------------------------------------------------------------------
-- 9. ĐĂNG KÝ SỰ KIỆN CHO 120 SINH VIÊN NỀN + TÀI KHOẢN DEMO CHÍNH
-- ----------------------------------------------------------------------------
-- 9a. Sinh viên nền (user_id 201..320) đăng ký rải đều vào các sự kiện để có số lượng thật & biểu đồ đẹp
INSERT INTO registrations (user_id, event_id, reg_date, check_in_at, check_out_at, check_in_by, check_out_by, status, created_at, updated_at)
SELECT
    200 + s_idx AS user_id,
    e.id        AS event_id,
    e.date_open + (((s_idx % 5) + 1) * INTERVAL '6 hours'),
    CASE WHEN e.status IN ('COMPLETED', 'CLOSED') AND (s_idx % 7 <> 0)
         THEN e.date_happen + INTERVAL '10 minutes' END,
    CASE WHEN e.status IN ('COMPLETED', 'CLOSED') AND (s_idx % 7 <> 0)
         THEN e.date_happen + INTERVAL '2 hours' END,
    CASE WHEN e.status IN ('COMPLETED', 'CLOSED') AND (s_idx % 7 <> 0)
         THEN CASE
             WHEN e.fanpage_id IN (1, 2, 11) THEN 2
             WHEN e.fanpage_id IN (6, 10) THEN 5
             WHEN e.fanpage_id = 8 THEN 6
             ELSE 7
         END END,
    CASE WHEN e.status IN ('COMPLETED', 'CLOSED') AND (s_idx % 7 <> 0)
         THEN CASE
             WHEN e.fanpage_id IN (1, 2, 11) THEN 2
             WHEN e.fanpage_id IN (6, 10) THEN 5
             WHEN e.fanpage_id = 8 THEN 6
             ELSE 7
         END END,
    CASE
        WHEN e.status = 'CANCELLED' THEN 'CANCELLED'
        WHEN e.status IN ('COMPLETED', 'CLOSED') THEN
            CASE
                WHEN e.id IN (33, 34, 35) AND s_idx IN (7, 14, 21, 28, 35) THEN 'ABSENT'
                WHEN e.id IN (34, 35) AND s_idx IN (42, 49, 56) THEN 'ABSENT'
                WHEN e.id IN (37, 38) AND s_idx IN (7, 14, 21, 28) THEN 'ABSENT'
                WHEN s_idx % 7 = 0 THEN 'ABSENT'
                ELSE 'COMPLETED'
            END
        ELSE 'REGISTERED'
    END AS status,
    e.date_open + (((s_idx % 5) + 1) * INTERVAL '6 hours'),
    NOW()
FROM events e
CROSS JOIN generate_series(1, 120) AS s_idx
WHERE
    -- Mỗi sự kiện nhận từ 30 đến 95 sinh viên tùy theo sự kiện
    (e.id IN (1, 3, 4, 7, 12) AND s_idx <= 88)
 OR (e.id IN (2, 5, 8, 9, 10) AND s_idx <= 64)
 OR (e.id IN (6, 11, 13, 14) AND s_idx <= 42)
 OR (e.id BETWEEN 15 AND 21 AND (s_idx % 3 = 0))
 OR (e.id BETWEEN 22 AND 30 AND s_idx <= (50 + (e.id % 5) * 10))
 OR (e.id IN (31, 32) AND s_idx <= 25)
 OR (e.id IN (33, 34, 35, 36, 37, 38) AND s_idx <= 85)
 OR (e.id IN (39, 40) AND s_idx <= 45);

-- 9b. Lịch sử & Trạng thái đăng ký phong phú cho tài khoản demo loc@student.ctu.edu.vn (user_id = 1)
INSERT INTO registrations (user_id, event_id, reg_date, check_in_at, check_out_at, check_in_by, check_out_by, check_in_evidence_url, check_out_evidence_url, status, reason, created_at, updated_at) VALUES
-- 2 sự kiện ĐANG DIỄN RA HIỆN TẠI (#39 gửi ảnh check-in, #40 điểm danh thủ công)
(1, 39, NOW() - INTERVAL '4 days', NULL, NULL, NULL, NULL, NULL, NULL, 'REGISTERED', NULL, NOW() - INTERVAL '4 days', NOW()),
(1, 40, NOW() - INTERVAL '4 days', NULL, NULL, NULL, NULL, NULL, NULL, 'REGISTERED', NULL, NOW() - INTERVAL '4 days', NOW()),
-- Đã đăng ký 3 sự kiện ĐANG MỞ ĐĂNG KÝ (hiện nút xanh "Xem đăng ký" trên Trang chủ & tab Sắp diễn ra)
(1, 2,  NOW() - INTERVAL '2 days',   NULL, NULL, NULL, NULL, NULL, NULL, 'REGISTERED', NULL, NOW() - INTERVAL '2 days', NOW()),
(1, 5,  NOW() - INTERVAL '1 day',    NULL, NULL, NULL, NULL, NULL, NULL, 'REGISTERED', NULL, NOW() - INTERVAL '1 day',  NOW()),
(1, 9,  NOW() - INTERVAL '12 hours', NULL, NULL, NULL, NULL, NULL, NULL, 'REGISTERED', NULL, NOW() - INTERVAL '12 hours', NOW()),
-- Đã hoàn thành & điểm danh 8 sự kiện (có đầy đủ người điểm danh & ảnh minh chứng)
(1, 22, NOW() - INTERVAL '40 days',  NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days' + INTERVAL '3 hours', 7, 7, NULL, NULL, 'COMPLETED', NULL, NOW() - INTERVAL '40 days', NOW()),
(1, 23, NOW() - INTERVAL '30 days',  NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days' + INTERVAL '2 hours', 7, 7, NULL, NULL, 'COMPLETED', NULL, NOW() - INTERVAL '30 days', NOW()),
(1, 24, NOW() - INTERVAL '24 days',  NOW() - INTERVAL '14 days', NOW() - INTERVAL '14 days' + INTERVAL '3 hours', 1, 7, 'https://images.unsplash.com/photo-1559027615-cd4628902d4a?auto=format&fit=crop&w=800&q=80', 'https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?auto=format&fit=crop&w=800&q=80', 'COMPLETED', NULL, NOW() - INTERVAL '24 days', NOW()),
(1, 25, NOW() - INTERVAL '15 days',  NOW() - INTERVAL '8 days',  NOW() - INTERVAL '8 days'  + INTERVAL '3 hours', 2, 2, NULL, NULL, 'COMPLETED', NULL, NOW() - INTERVAL '15 days', NOW()),
(1, 27, NOW() - INTERVAL '110 days', NOW() - INTERVAL '100 days',NOW() - INTERVAL '100 days'+ INTERVAL '2 hours', 7, 7, NULL, NULL, 'COMPLETED', NULL, NOW() - INTERVAL '110 days', NOW()),
(1, 28, NOW() - INTERVAL '150 days', NOW() - INTERVAL '140 days',NOW() - INTERVAL '135 days',                     7, 7, NULL, NULL, 'COMPLETED', NULL, NOW() - INTERVAL '150 days', NOW()),
(1, 29, NOW() - INTERVAL '180 days', NOW() - INTERVAL '170 days',NOW() - INTERVAL '170 days'+ INTERVAL '3 hours', 2, 2, NULL, NULL, 'COMPLETED', NULL, NOW() - INTERVAL '180 days', NOW()),
(1, 30, NOW() - INTERVAL '240 days', NOW() - INTERVAL '230 days',NOW() - INTERVAL '230 days'+ INTERVAL '4 hours', 2, 2, NULL, NULL, 'COMPLETED', NULL, NOW() - INTERVAL '240 days', NOW()),
(1, 37, '2026-06-05 10:00:00+07', '2026-06-14 08:10:00+07', '2026-06-14 16:20:00+07', 2, 2, NULL, NULL, 'COMPLETED', NULL, '2026-06-05 10:00:00+07', NOW()),
-- Vắng 1 sự kiện có gửi đơn xin phép (hiện tab Vắng & bảng Fanpage Absence Panel)
(1, 26, NOW() - INTERVAL '52 days', NULL, NULL, NULL, NULL, NULL, NULL, 'ABSENT', 'Trùng lịch thi kết thúc học phần Lập trình Web', NOW() - INTERVAL '52 days', NOW()),
-- Đã hủy 1 sự kiện (hiện tab Đã hủy)
(1, 31, NOW() - INTERVAL '14 days', NULL, NULL, NULL, NULL, NULL, NULL, 'CANCELLED', 'Sự kiện tạm hoãn do thời tiết xấu', NOW() - INTERVAL '14 days', NOW());

-- Lịch sử cho sinh viên Kinh tế (user_id = 4) để demo thêm
INSERT INTO registrations (user_id, event_id, reg_date, check_in_at, check_out_at, status, created_at, updated_at) VALUES
(4, 8,  NOW() - INTERVAL '2 days',  NULL, NULL, 'REGISTERED', NOW() - INTERVAL '2 days', NOW()),
(4, 26, NOW() - INTERVAL '50 days', NOW() - INTERVAL '45 days', NOW() - INTERVAL '45 days' + INTERVAL '3 hours', 'COMPLETED', NOW() - INTERVAL '50 days', NOW()),
(4, 22, NOW() - INTERVAL '38 days', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days' + INTERVAL '3 hours', 'COMPLETED', NOW() - INTERVAL '38 days', NOW());

-- ----------------------------------------------------------------------------
-- 10. GIAO DỊCH ĐIỂM RÈN LUYỆN (point_transactions) & TỔNG HỢP ĐIỂM (student_semester_points)
-- ----------------------------------------------------------------------------
-- 10a. Giao dịch ĐRL cho loc@student.ctu.edu.vn (user_id = 1) - HK1 2026-2027 (semester_id = 1)
INSERT INTO point_transactions (user_id, semester_id, point_category_id, event_id, points, type, status, description, created_at, updated_at) VALUES
-- Hệ thống cộng từ sự kiện đã tham gia (CHECKOUT_REWARD)
(1, 1, 201,  22, 15, 'CHECKOUT_REWARD', 'CONFIRMED', 'Tham gia Tuần Sinh hoạt Công dân Sinh viên K49', NOW() - INTERVAL '30 days', NOW()),
(1, 1, 321,  22, 3,  'CHECKOUT_REWARD', 'CONFIRMED', 'Chuyên đề phòng chống tội phạm và tệ nạn xã hội', NOW() - INTERVAL '30 days', NOW()),
(1, 1, 411,  23, 5,  'CHECKOUT_REWARD', 'CONFIRMED', 'Hiến máu tình nguyện "Sẻ chia giọt hồng" Đợt 1/2026', NOW() - INTERVAL '20 days', NOW()),
(1, 1, 2112, 24, 4,  'CHECKOUT_REWARD', 'CONFIRMED', 'Chiến dịch Thứ Bảy Tình nguyện Khu II ĐHCT', NOW() - INTERVAL '14 days', NOW()),
(1, 1, 411,  24, 5,  'CHECKOUT_REWARD', 'CONFIRMED', 'Hoạt động cộng đồng Thứ Bảy Tình nguyện', NOW() - INTERVAL '14 days', NOW()),
(1, 1, 1045, 25, 2,  'CHECKOUT_REWARD', 'CONFIRMED', 'Tham gia Workshop Git, Docker & CI/CD (CLB Tin học)', NOW() - INTERVAL '8 days', NOW()),
(1, 1, 511,  25, 3,  'CHECKOUT_REWARD', 'CONFIRMED', 'Hỗ trợ tham gia hoạt động chuyên môn Trường CNTT&TT', NOW() - INTERVAL '8 days', NOW()),
-- Tự đánh giá mẫu của sinh viên (MANUAL) để bảng ĐRL đạt tổng ~83 điểm (Xếp loại Tốt)
(1, 1, 101,  NULL, 6, 'MANUAL', 'CONFIRMED', 'Đi học đầy đủ các học phần HK1, không có học phần bị cấm thi', NOW() - INTERVAL '5 days', NOW()),
(1, 1, 111,  NULL, 6, 'MANUAL', 'CONFIRMED', 'Không vi phạm quy chế thi giữa kỳ', NOW() - INTERVAL '5 days', NOW()),
(1, 1, 1222, NULL, 6, 'MANUAL', 'CONFIRMED', 'Điểm TBC học kỳ dự kiến đạt 3.45/4.0', NOW() - INTERVAL '4 days', NOW()),
(1, 1, 2113, NULL, 3, 'MANUAL', 'CONFIRMED', 'Hoàn thành khảo sát ý kiến giảng dạy học kỳ 1', NOW() - INTERVAL '4 days', NOW()),
(1, 1, 301,  NULL, 8, 'MANUAL', 'CONFIRMED', 'Tham gia đầy đủ hoạt động phong trào do Chi đoàn lớp tổ chức', NOW() - INTERVAL '3 days', NOW()),
(1, 1, 401,  NULL, 8, 'MANUAL', 'CONFIRMED', 'Chấp hành tốt pháp luật, quy định nơi cư trú KTX', NOW() - INTERVAL '3 days', NOW()),
(1, 1, 504,  NULL, 3, 'MANUAL', 'CONFIRMED', 'Tổ trưởng nhóm đồ án và hỗ trợ học tập lớp KTPM K49', NOW() - INTERVAL '2 days', NOW()),

-- Giao dịch ĐRL năm cũ 2025-2026 (HK3: semester_id = 103, HK2: semester_id = 102, HK1: semester_id = 101)
(1, 103, 523,  27, 6,  'CHECKOUT_REWARD', 'CONFIRMED', 'Tham dự Lễ Tuyên dương SV5T cấp Trường', NOW() - INTERVAL '100 days', NOW()),
(1, 103, 411,  28, 10, 'CHECKOUT_REWARD', 'CONFIRMED', 'Chiến dịch Mùa Hè Xanh 2026', NOW() - INTERVAL '135 days', NOW()),
(1, 103, 3112, 28, 5,  'CHECKOUT_REWARD', 'CONFIRMED', 'Lực lượng nòng cốt Mùa Hè Xanh cấp ĐHCT', NOW() - INTERVAL '135 days', NOW()),
(1, 103, 1045, 37, 3,  'CHECKOUT_REWARD', 'CONFIRMED', 'Lớp Bồi dưỡng Kỹ năng Công nghệ & Thiết kế Web Mùa hè 2026', '2026-06-14 17:00:00+07', NOW()),
(1, 103, 101,  NULL, 6, 'MANUAL', 'CONFIRMED', 'Chuyên cần học phần HK3 (Học kỳ hè 2025-2026)', NOW() - INTERVAL '105 days', NOW()),
(1, 103, 1221, NULL, 8, 'MANUAL', 'CONFIRMED', 'Điểm TBC HK3 đạt 3.60/4.0', NOW() - INTERVAL '105 days', NOW()),
(1, 103, 201,  NULL, 15,'MANUAL', 'CONFIRMED', 'Chấp hành tốt nội quy nhà trường HK3', NOW() - INTERVAL '105 days', NOW()),
(1, 103, 301,  NULL, 6, 'MANUAL', 'CONFIRMED', 'Tham gia hoạt động sinh hoạt hè', NOW() - INTERVAL '105 days', NOW()),
(1, 103, 401,  NULL, 8, 'MANUAL', 'CONFIRMED', 'Thực hiện tốt nghĩa vụ công dân tại địa phương', NOW() - INTERVAL '105 days', NOW()),

(1, 102, 301,  29, 6,  'CHECKOUT_REWARD', 'CONFIRMED', 'Giải Chạy bộ CICT Open 2026', NOW() - INTERVAL '170 days', NOW()),
(1, 102, 101,  NULL, 6, 'MANUAL', 'CONFIRMED', 'Chuyên cần HK2 năm 2025-2026', NOW() - INTERVAL '120 days', NOW()),
(1, 102, 111,  NULL, 6, 'MANUAL', 'CONFIRMED', 'Không vi phạm quy chế thi HK2', NOW() - INTERVAL '120 days', NOW()),
(1, 102, 1221, NULL, 8, 'MANUAL', 'CONFIRMED', 'ĐTBCHK2 đạt 3.68/4.0', NOW() - INTERVAL '120 days', NOW()),
(1, 102, 201,  NULL, 15,'MANUAL', 'CONFIRMED', 'Chấp hành tốt nội quy nhà trường', NOW() - INTERVAL '120 days', NOW()),
(1, 102, 2111, NULL, 8, 'MANUAL', 'CONFIRMED', 'Tham gia giữ gìn vệ sinh môi trường lớp học', NOW() - INTERVAL '120 days', NOW()),
(1, 102, 301,  NULL, 6, 'MANUAL', 'CONFIRMED', 'Tham gia hoạt động văn nghệ Khoa', NOW() - INTERVAL '120 days', NOW()),
(1, 102, 401,  NULL, 8, 'MANUAL', 'CONFIRMED', 'Thực hiện tốt nghĩa vụ công dân', NOW() - INTERVAL '120 days', NOW()),
(1, 102, 511,  NULL, 6, 'MANUAL', 'CONFIRMED', 'Hỗ trợ công tác đón tân sinh viên', NOW() - INTERVAL '120 days', NOW()),

(1, 101, 1122, 30, 4,  'CHECKOUT_REWARD', 'CONFIRMED', 'Đạt giải Khuyến khích cuộc thi UI/UX CICT', NOW() - INTERVAL '230 days', NOW()),
(1, 101, 101,  NULL, 6, 'MANUAL', 'CONFIRMED', 'Đi học đầy đủ HK1 2025-2026', NOW() - INTERVAL '220 days', NOW()),
(1, 101, 201,  NULL, 15,'MANUAL', 'CONFIRMED', 'Chấp hành tốt nội quy ĐHCT', NOW() - INTERVAL '220 days', NOW()),
(1, 101, 301,  NULL, 10,'MANUAL', 'CONFIRMED', 'Tham gia sinh hoạt chính trị & văn nghệ', NOW() - INTERVAL '220 days', NOW()),
(1, 101, 401,  NULL, 8, 'MANUAL', 'CONFIRMED', 'Không vi phạm pháp luật', NOW() - INTERVAL '220 days', NOW()),
(1, 101, 511,  NULL, 8, 'MANUAL', 'CONFIRMED', 'Tích cực tham gia phong trào lớp', NOW() - INTERVAL '220 days', NOW());

-- 10b. Tính tổng điểm ĐRL bottom-up (lá -> cấp 2 -> cấp 1 -> cấp 0) vào bảng student_semester_points
DO $$
DECLARE
    curr_lvl INT;
    max_lvl  INT;
    min_lvl  INT;
BEGIN
    -- Mức lá từ point_transactions
    INSERT INTO student_semester_points (
        user_id, semester_id, point_category_id,
        system_point_record, manual_point_record, total_point,
        created_at, updated_at
    )
    SELECT
        pt.user_id,
        pt.semester_id,
        pt.point_category_id,
        COALESCE(SUM(CASE WHEN pt.type = 'CHECKOUT_REWARD' THEN pt.points ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN pt.type = 'MANUAL' THEN pt.points ELSE 0 END), 0),
        LEAST(
            pc.maximum,
            COALESCE(SUM(pt.points), 0)
        ),
        NOW(),
        NOW()
    FROM point_transactions pt
    JOIN point_categories pc ON pc.id = pt.point_category_id
    WHERE pt.status = 'CONFIRMED'
    GROUP BY pt.user_id, pt.semester_id, pt.point_category_id, pc.maximum;

    -- Tính ngược lên các cấp cha từ level lớn nhất về level 0
    SELECT COALESCE(MAX(level), 0), COALESCE(MIN(level), 0) INTO max_lvl, min_lvl FROM point_categories;

    FOR curr_lvl IN REVERSE (max_lvl - 1)..min_lvl LOOP
        INSERT INTO student_semester_points (
            user_id, semester_id, point_category_id,
            system_point_record, manual_point_record, total_point,
            created_at, updated_at
        )
        SELECT
            ctx.user_id,
            ctx.semester_id,
            parent_pc.id,
            CASE
                WHEN parent_pc.is_max_child THEN COALESCE(
                    (ARRAY_AGG(child_ssp.system_point_record ORDER BY COALESCE(child_ssp.total_point, -1) DESC NULLS LAST, child_pc.id ASC)
                     FILTER (WHERE child_ssp.id IS NOT NULL))[1],
                    0
                )
                ELSE SUM(COALESCE(child_ssp.system_point_record, 0))
            END,
            CASE
                WHEN parent_pc.is_max_child THEN COALESCE(
                    (ARRAY_AGG(child_ssp.manual_point_record ORDER BY COALESCE(child_ssp.total_point, -1) DESC NULLS LAST, child_pc.id ASC)
                     FILTER (WHERE child_ssp.id IS NOT NULL))[1],
                    0
                )
                ELSE SUM(COALESCE(child_ssp.manual_point_record, 0))
            END,
            LEAST(
                parent_pc.maximum,
                CASE
                    WHEN parent_pc.is_max_child THEN COALESCE(MAX(child_ssp.total_point), 0)
                    ELSE COALESCE(SUM(child_ssp.total_point), 0)
                END
            ),
            NOW(),
            NOW()
        FROM point_categories parent_pc
        CROSS JOIN (SELECT DISTINCT user_id, semester_id FROM student_semester_points) ctx
        JOIN point_categories child_pc ON child_pc.parent_category_id = parent_pc.id AND child_pc.is_active = TRUE
        LEFT JOIN student_semester_points child_ssp
            ON child_ssp.point_category_id = child_pc.id
           AND child_ssp.user_id = ctx.user_id
           AND child_ssp.semester_id = ctx.semester_id
        WHERE parent_pc.level = curr_lvl AND parent_pc.is_active = TRUE
        GROUP BY ctx.user_id, ctx.semester_id, parent_pc.id, parent_pc.maximum, parent_pc.is_max_child
        HAVING SUM(COALESCE(child_ssp.total_point, 0)) > 0
            OR SUM(COALESCE(child_ssp.system_point_record, 0)) > 0
            OR SUM(COALESCE(child_ssp.manual_point_record, 0)) > 0;
    END LOOP;
END $$;

-- ----------------------------------------------------------------------------
-- 11. GIAO DỊCH TIÊU CHÍ SV5T (student_criteria_transactions) & TIẾN ĐỘ (student_criteria_progress)
-- ----------------------------------------------------------------------------
INSERT INTO student_criteria_transactions (user_id, criteria_id, academic_year_id, event_id, completion_source, note, is_active, created_at, updated_at) VALUES
-- Năm học 2026-2027 (academic_year_id = 2) cho loc@student.ctu.edu.vn (user_id = 1):
--   - Đạt Tiêu chuẩn 1 (Đạo đức tốt): 101 (tự ĐG), 102 (tự ĐG), 1033 (từ sự kiện 22)
--   - Đạt Tiêu chuẩn 2 (Học tập tốt): 201 (tự ĐG GPA 3.45), 202 (tự ĐG 18 tín chỉ)
--   - Chưa đạt Tiêu chuẩn 3 (Thể lực tốt) -> đã đăng ký giải chạy #5 nhưng chưa diễn ra!
--   - Đang làm dở Tiêu chuẩn 4 (Tình nguyện tốt):
--       + 402 (cần 3 ngày): mới có 1 ngày từ sự kiện #24 (đang 1/3 ngày!)
--       + 403 (cần 2 lần hiến máu): mới có 1 lần từ sự kiện #23 (đang 1/2 lần!) -> AI sẽ ưu tiên gợi ý sự kiện #3, #4, #14!
--   - Đang làm dở Tiêu chuẩn 5 (Hội nhập tốt - cần 2/3 nhóm):
--       + Đã đạt 5.2 Tin học (5023 từ sự kiện #25 Workshop Git/Docker)
--       + Đang thiếu 5.1 Ngoại ngữ và 5.3 Hội nhập -> AI sẽ ưu tiên gợi ý sự kiện #6, #9!
(1, 101,  2, NULL, 'MANUAL',         'ĐRL tạm tính HK1 đạt 85 điểm', TRUE, NOW() - INTERVAL '5 days', NOW()),
(1, 102,  2, NULL, 'MANUAL',         'Chấp hành tốt nội quy, quy chế nhà trường', TRUE, NOW() - INTERVAL '5 days', NOW()),
(1, 1033, 2, 22,   'EVENT_CHECKOUT', 'Tuần Sinh hoạt Công dân K49', TRUE, NOW() - INTERVAL '30 days', NOW()),
(1, 201,  2, NULL, 'MANUAL',         'Điểm TBC học kỳ đạt 3.45/4.0', TRUE, NOW() - INTERVAL '4 days', NOW()),
(1, 202,  2, NULL, 'MANUAL',         'Đăng ký đủ 18 tín chỉ, không nợ học phần', TRUE, NOW() - INTERVAL '4 days', NOW()),
(1, 402,  2, 24,   'EVENT_CHECKOUT', 'Chiến dịch Thứ Bảy Tình nguyện Khu II ĐHCT', TRUE, NOW() - INTERVAL '14 days', NOW()),
(1, 403,  2, 23,   'EVENT_CHECKOUT', 'Hiến máu tình nguyện "Sẻ chia giọt hồng" Đợt 1/2026', TRUE, NOW() - INTERVAL '20 days', NOW()),
(1, 5023, 2, 25,   'EVENT_CHECKOUT', 'Workshop Git, Docker & CI/CD thực tế', TRUE, NOW() - INTERVAL '8 days', NOW()),

-- Năm học cũ 2025-2026 (academic_year_id = 1) cho loc@student.ctu.edu.vn: đạt trọn vẹn 5/5 tiêu chuẩn!
(1, 101,  1, NULL, 'MANUAL',         'ĐRL năm học 2025-2026 đạt 90 điểm', TRUE, NOW() - INTERVAL '120 days', NOW()),
(1, 102,  1, NULL, 'MANUAL',         'Không vi phạm pháp luật và nội quy', TRUE, NOW() - INTERVAL '120 days', NOW()),
(1, 1033, 1, NULL, 'MANUAL',         'Tham gia tuần sinh hoạt công dân 2025-2026', TRUE, NOW() - INTERVAL '200 days', NOW()),
(1, 201,  1, NULL, 'MANUAL',         'TBC cả năm đạt 3.56/4.0', TRUE, NOW() - INTERVAL '115 days', NOW()),
(1, 202,  1, NULL, 'MANUAL',         'Hoàn thành 36 tín chỉ, không nợ môn', TRUE, NOW() - INTERVAL '115 days', NOW()),
(1, 302,  1, 29,   'EVENT_CHECKOUT', 'Giải Chạy bộ & Hội thao CICT Open 2026', TRUE, NOW() - INTERVAL '170 days', NOW()),
(1, 402,  1, 28,   'EVENT_CHECKOUT', 'Chiến dịch Mùa Hè Xanh 2026', TRUE, NOW() - INTERVAL '135 days', NOW()),
(1, 401,  1, NULL, 'MANUAL',         'Giấy khen Chiến sĩ Mùa Hè Xanh xuất sắc', TRUE, NOW() - INTERVAL '130 days', NOW()),
(1, 5012, 1, NULL, 'MANUAL',         'Đạt chứng chỉ Tiếng Anh B1 (TOEIC 580)', TRUE, NOW() - INTERVAL '150 days', NOW()),
(1, 5023, 1, 30,   'EVENT_CHECKOUT', 'Cuộc thi Thiết kế Giao diện UI/UX CICT 2025', TRUE, NOW() - INTERVAL '230 days', NOW()),
(1, 5032, 1, 27,   'EVENT_CHECKOUT', 'Lễ Tuyên dương Sinh viên 5 tốt ĐHCT', TRUE, NOW() - INTERVAL '100 days', NOW());

-- Tính toán bottom-up bảng student_criteria_progress (tôn trọng required_occurrences và required_children)
DO $$
DECLARE
    curr_lvl INT;
    min_lvl  INT;
    max_lvl  INT;
BEGIN
    INSERT INTO student_criteria_progress (
        user_id, criteria_id, academic_year_id,
        is_completed, completed_at, completed_children_count, completed_occurrences,
        is_active, created_at, updated_at
    )
    SELECT
        t.user_id,
        t.criteria_id,
        t.academic_year_id,
        COUNT(*) >= COALESCE(c.required_occurrences, 1) AS is_completed,
        CASE WHEN COUNT(*) >= COALESCE(c.required_occurrences, 1) THEN MAX(t.created_at) ELSE NULL END,
        0,
        COUNT(*)::INT,
        TRUE,
        MIN(t.created_at),
        NOW()
    FROM student_criteria_transactions t
    JOIN criterias c ON c.id = t.criteria_id
    WHERE c.is_active = TRUE
      AND t.is_active = TRUE
      AND NOT EXISTS (SELECT 1 FROM criterias ch WHERE ch.parent_id = c.id AND ch.is_active = TRUE)
    GROUP BY t.user_id, t.criteria_id, t.academic_year_id, c.required_occurrences;

    SELECT COALESCE(MAX(level), 0), COALESCE(MIN(level), 0)
    INTO max_lvl, min_lvl
    FROM criterias
    WHERE is_active = TRUE
      AND EXISTS (SELECT 1 FROM criterias ch WHERE ch.parent_id = criterias.id AND ch.is_active = TRUE);

    FOR curr_lvl IN REVERSE max_lvl..min_lvl LOOP
        INSERT INTO student_criteria_progress (
            user_id, criteria_id, academic_year_id,
            is_completed, completed_at, completed_children_count, completed_occurrences,
            is_active, created_at, updated_at
        )
        SELECT
            ctx.user_id,
            p.id,
            ctx.academic_year_id,
            (COALESCE(cs.completed_count, 0) >= COALESCE(p.required_children, cs.total_count) AND COALESCE(cs.total_count, 0) > 0),
            CASE
                WHEN COALESCE(cs.completed_count, 0) >= COALESCE(p.required_children, cs.total_count) AND COALESCE(cs.total_count, 0) > 0
                    THEN cs.latest_completed_at
                ELSE NULL
            END,
            COALESCE(cs.completed_count, 0),
            0,
            TRUE,
            NOW(),
            NOW()
        FROM criterias p
        CROSS JOIN (SELECT DISTINCT user_id, academic_year_id FROM student_criteria_transactions) ctx
        LEFT JOIN LATERAL (
            SELECT
                COUNT(c.id) AS total_count,
                COUNT(c.id) FILTER (WHERE COALESCE(scp.is_completed, FALSE) = TRUE) AS completed_count,
                MAX(scp.completed_at) FILTER (WHERE COALESCE(scp.is_completed, FALSE) = TRUE) AS latest_completed_at
            FROM criterias c
            LEFT JOIN student_criteria_progress scp
                ON scp.criteria_id = c.id
               AND scp.user_id = ctx.user_id
               AND scp.academic_year_id = ctx.academic_year_id
            WHERE c.parent_id = p.id AND c.is_active = TRUE
        ) cs ON TRUE
        WHERE p.level = curr_lvl
          AND p.is_active = TRUE
          AND EXISTS (SELECT 1 FROM criterias ch WHERE ch.parent_id = p.id AND ch.is_active = TRUE);
    END LOOP;
END $$;

-- ----------------------------------------------------------------------------
-- 12. SỰ KIỆN ĐÃ LƯU, THÔNG BÁO & TIN NHẮN CHO DEMO
-- ----------------------------------------------------------------------------
INSERT INTO stored_events (user_id, event_id, is_active, created_at, updated_at) VALUES
(1, 1, TRUE, NOW() - INTERVAL '2 days', NOW()),
(1, 4, TRUE, NOW() - INTERVAL '1 day', NOW()),
(1, 7, TRUE, NOW() - INTERVAL '10 hours', NOW());

INSERT INTO notifications (type, message, status, icon_url, url, user_id, fanpage_id, is_active, created_at, updated_at) VALUES
('EVENT_CREATED', 'CLB Tin học vừa mở đăng ký Workshop: Xây dựng ứng dụng AI thực chiến với Spring Boot & React.', 'UNREAD', 'https://images.unsplash.com/photo-1531403009284-440f080d1e12?auto=format&fit=crop&w=200&q=80', '/events/1', 1, 2, TRUE, NOW() - INTERVAL '2 hours', NOW()),
('EVENT_CREATED', 'Đội Công tác Xã hội - ĐHCT: Chiến dịch "Chủ nhật Xanh" sắp đóng đăng ký trong 30 giờ tới. Đăng ký ngay để tích lũy ngày tình nguyện!', 'UNREAD', 'https://images.unsplash.com/photo-1559027615-cd4628902d4a?auto=format&fit=crop&w=200&q=80', '/events/3', 1, 4, TRUE, NOW() - INTERVAL '5 hours', NOW()),
('EVENT_CHECKOUT_AWARDED', 'Cộng điểm rèn luyện thành công: Bạn đã được ghi nhận +2 điểm mục I.a và +3 điểm mục V.b từ Workshop Git, Docker & CI/CD.', 'UNREAD', 'https://res.cloudinary.com/depaiphq0/image/upload/v1793680012/ctu-event/fanpages/u_2_14fdaacdf2da4081.jpg', '/training-points', 1, NULL, TRUE, NOW() - INTERVAL '8 days', NOW()),
('EVENT_ABSENCE_ALERT', 'Thông báo vắng mặt sự kiện: Hệ thống ghi nhận bạn vắng mặt tại "Hội thảo Khởi nghiệp & Phân tích Tài chính Cá nhân". Bạn có thể gửi đơn giải trình nếu có lý do chính đáng.', 'READ', NULL, '/history', 1, 6, TRUE, NOW() - INTERVAL '44 days', NOW()),
('EVENT_CHECKOUT_AWARDED', 'Ghi nhận tiêu chí Hiến máu SV5T: Bạn đã hoàn thành 1/2 lần hiến máu tình nguyện năm học 2026-2027 từ Ngày hội "Sẻ chia giọt hồng".', 'READ', NULL, '/sv5t', 1, 5, TRUE, NOW() - INTERVAL '20 days', NOW()),
('SYSTEM', 'Nhắc lịch tham gia sự kiện sắp tới: Bạn đã đăng ký "Ngày hội Việc làm CNTT & Kết nối Doanh nghiệp CICT 2026". Đừng quên mang theo CV nhé!', 'READ', NULL, '/events/2', 1, NULL, TRUE, NOW() - INTERVAL '1 day', NOW()),
-- Thông báo phía Fanpage Admin (#1 -> user_id 2, #6 -> user_id 5)
('SYSTEM', 'Cập nhật danh sách đăng ký sự kiện: Ngày hội Việc làm CICT 2026 đã đạt 65 lượt đăng ký mới trong tuần này.', 'UNREAD', NULL, '/fanpage/1/events/2', 2, 1, TRUE, NOW() - INTERVAL '3 hours', NOW()),
('EVENT_ABSENCE_ALERT', 'Có sinh viên gửi giải trình vắng mặt: Sinh viên Đoàn Tài Lộc (B2300000) đã gửi lý do vắng mặt cần xem xét.', 'UNREAD', NULL, '/fanpage/6', 5, 6, TRUE, NOW() - INTERVAL '1 day', NOW());

INSERT INTO conversations (id, user_id, fanpage_id, created_at) OVERRIDING SYSTEM VALUE VALUES
(1, 1, 1, NOW() - INTERVAL '3 days'),
(2, 1, 4, NOW() - INTERVAL '2 days');

INSERT INTO messages (conversation_id, sender_type, sender_id, content, status, is_active, created_at, updated_at) VALUES
(1, 'STUDENT', 1, 'Chào anh/chị Đoàn Trường Công nghệ Thông tin và Truyền thông - Đại học Cần Thơ, cho em hỏi Ngày hội Việc làm có cần in sẵn CV giấy không ạ?', 'READ', TRUE, NOW() - INTERVAL '3 days', NOW()),
(1, 'FANPAGE', 1, 'Chào bạn Lộc, bạn nên chuẩn bị 2-3 bản CV giấy hoặc bản PDF trên điện thoại để nộp trực tiếp tại gian hàng doanh nghiệp nhé!', 'READ', TRUE, NOW() - INTERVAL '2 days' - INTERVAL '20 hours', NOW()),
(2, 'STUDENT', 1, 'Chủ nhật Xanh đợt này tập trung ở cổng nào của Khu II vậy ạ?', 'SENT', TRUE, NOW() - INTERVAL '1 day', NOW());

-- Nếu DB đã chạy daily-event-recommendation.sql, đồng bộ sẵn summary cho hệ thống gợi ý AI
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'sync_event_recommendation_summary') THEN
        PERFORM sync_event_recommendation_summary(id) FROM events;
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 13. RESET TOÀN BỘ IDENTITY / SEQUENCES ĐỂ KHÔNG LỖI TRÙNG ID KHI TẠO MỚI QUA APP
-- ----------------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('academic_years', 'id'),                COALESCE((SELECT MAX(id) FROM academic_years), 1));
SELECT setval(pg_get_serial_sequence('associations', 'id'),                  COALESCE((SELECT MAX(id) FROM associations), 1));
SELECT setval(pg_get_serial_sequence('conversations', 'id'),                 COALESCE((SELECT MAX(id) FROM conversations), 1));
SELECT setval(pg_get_serial_sequence('criterias', 'id'),                     COALESCE((SELECT MAX(id) FROM criterias), 1));
SELECT setval(pg_get_serial_sequence('event_points', 'id'),                  COALESCE((SELECT MAX(id) FROM event_points), 1));
SELECT setval(pg_get_serial_sequence('event_types', 'id'),                   COALESCE((SELECT MAX(id) FROM event_types), 1));
SELECT setval(pg_get_serial_sequence('events', 'id'),                        COALESCE((SELECT MAX(id) FROM events), 1));
SELECT setval(pg_get_serial_sequence('fanpage_admin_profiles', 'id'),        COALESCE((SELECT MAX(id) FROM fanpage_admin_profiles), 1));
SELECT setval(pg_get_serial_sequence('fanpages', 'id'),                      COALESCE((SELECT MAX(id) FROM fanpages), 1));
SELECT setval(pg_get_serial_sequence('followings', 'id'),                    COALESCE((SELECT MAX(id) FROM followings), 1));
SELECT setval(pg_get_serial_sequence('majors', 'id'),                        COALESCE((SELECT MAX(id) FROM majors), 1));
SELECT setval(pg_get_serial_sequence('messages', 'id'),                      COALESCE((SELECT MAX(id) FROM messages), 1));
SELECT setval(COALESCE(pg_get_serial_sequence('notifications', 'id'), 'notifications_id_seq'), COALESCE((SELECT MAX(id) FROM notifications), 1));
SELECT setval(pg_get_serial_sequence('point_categories', 'id'),              COALESCE((SELECT MAX(id) FROM point_categories), 1));
SELECT setval(pg_get_serial_sequence('point_transactions', 'id'),            COALESCE((SELECT MAX(id) FROM point_transactions), 1));
SELECT setval(pg_get_serial_sequence('roles', 'id'),                         COALESCE((SELECT MAX(id) FROM roles), 1));
SELECT setval(pg_get_serial_sequence('schools', 'id'),                       COALESCE((SELECT MAX(id) FROM schools), 1));
SELECT setval(pg_get_serial_sequence('semesters', 'id'),                     COALESCE((SELECT MAX(id) FROM semesters), 1));
SELECT setval(pg_get_serial_sequence('stored_events', 'id'),                 COALESCE((SELECT MAX(id) FROM stored_events), 1));
SELECT setval(pg_get_serial_sequence('student_criteria_progress', 'id'),     COALESCE((SELECT MAX(id) FROM student_criteria_progress), 1));
SELECT setval(pg_get_serial_sequence('student_criteria_transactions', 'id'), COALESCE((SELECT MAX(id) FROM student_criteria_transactions), 1));
SELECT setval(pg_get_serial_sequence('student_profiles', 'id'),              COALESCE((SELECT MAX(id) FROM student_profiles), 1));
SELECT setval(pg_get_serial_sequence('student_semester_points', 'id'),       COALESCE((SELECT MAX(id) FROM student_semester_points), 1));
SELECT setval(pg_get_serial_sequence('users', 'id'),                         COALESCE((SELECT MAX(id) FROM users), 1));

COMMIT;
