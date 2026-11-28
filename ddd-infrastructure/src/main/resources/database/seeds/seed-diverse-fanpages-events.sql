-- ============================================================================
-- SEED DATA: DIVERSE FANPAGES, ORGANIZER USERS & EVENTS (NON-DESTRUCTIVE)
-- - Tương thích 100% với schema.sql hiện tại (mydb).
-- - Không TRUNCATE dữ liệu cũ; dùng ID 401..405 (users/profiles), 101..105 (fanpages),
--   101..116 (events) và ON CONFLICT DO NOTHING để chạy an toàn nhiều lần.
-- - Yêu cầu đã có dữ liệu chuẩn: roles, event_types (1..4), point_categories (1..73),
--   criterias (UNIVERSITY 1..5035 & FACULTY 6000..65035), semesters.
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. 5 TÀI KHOẢN ORGANIZER MỚI (id 401..405) + USER_ROLES + ADMIN PROFILES
-- ----------------------------------------------------------------------------
INSERT INTO users (id, email, password, first_name, last_name, gender, avatar_url, is_active, created_at, updated_at)
OVERRIDING SYSTEM VALUE VALUES
(401, 'organizer.clb.ai@ctu.edu.vn',      '$2a$10$5snZONh3FshSppF3fNtoD.N0VHU31/mhWPRzv2FnnOMyvMRBMw3Ee', 'Quốc Bảo',    'Trần',    'MALE',   'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80', TRUE, NOW(), NOW()),
(402, 'organizer.vhnt@ctu.edu.vn',        '$2a$10$5snZONh3FshSppF3fNtoD.N0VHU31/mhWPRzv2FnnOMyvMRBMw3Ee', 'Thảo Vy',     'Nguyễn',  'FEMALE', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80', TRUE, NOW(), NOW()),
(403, 'organizer.ctxh.cict@ctu.edu.vn',   '$2a$10$5snZONh3FshSppF3fNtoD.N0VHU31/mhWPRzv2FnnOMyvMRBMw3Ee', 'Hữu Nghĩa',   'Lê',      'MALE',   'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80', TRUE, NOW(), NOW()),
(404, 'organizer.thethao.ctu@ctu.edu.vn', '$2a$10$5snZONh3FshSppF3fNtoD.N0VHU31/mhWPRzv2FnnOMyvMRBMw3Ee', 'Minh Trí',    'Huỳnh',   'MALE',   'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80', TRUE, NOW(), NOW()),
(405, 'organizer.ngoaingu@ctu.edu.vn',    '$2a$10$5snZONh3FshSppF3fNtoD.N0VHU31/mhWPRzv2FnnOMyvMRBMw3Ee', 'Ngọc Mai',    'Phạm',    'FEMALE', 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=200&q=80', TRUE, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'FANPAGE_ADMIN'
WHERE u.id BETWEEN 401 AND 405
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO fanpage_admin_profiles (id, user_id, staff_id, org_name, title, dob, phone_number, is_active, created_at, updated_at)
OVERRIDING SYSTEM VALUE VALUES
(401, 401, 'CB-401', 'CLB Trí tuệ Nhân tạo & Khoa học Dữ liệu - Trường CNTT&TT', 'Chủ nhiệm CLB',              '2001-05-12', '02923872401', TRUE, NOW(), NOW()),
(402, 402, 'CB-402', 'Đội Văn nghệ Xung kích - Đại học Cần Thơ',                  'Đội trưởng',                 '2000-11-20', '02923872402', TRUE, NOW(), NOW()),
(403, 403, 'CB-403', 'Đoàn Trường Công nghệ Thông tin và Truyền thông - CICT',    'Phó Bí thư Đoàn Trường',     '1998-03-18', '02923872403', TRUE, NOW(), NOW()),
(404, 404, 'CB-404', 'Liên chi hội Sinh viên Thể thao - Đại học Cần Thơ',         'Liên chi hội trưởng',        '1999-09-02', '02923872404', TRUE, NOW(), NOW()),
(405, 405, 'CB-405', 'Đoàn Khoa Ngoại ngữ - Đại học Cần Thơ',                     'Bí thư Đoàn Khoa',           '1997-07-25', '02923872405', TRUE, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------------------------
-- 2. 5 FANPAGE ĐA DẠNG LOẠI HÌNH TỔ CHỨC (id 101..105) + FANPAGE_MEMBERS
-- ----------------------------------------------------------------------------
INSERT INTO fanpages (id, name, description, avatar_url, status, org_type, org_name, parent_org, help_contact, help_phone, evidence_url, is_active, created_at, updated_at)
OVERRIDING SYSTEM VALUE VALUES
(101, 'CLB Trí tuệ Nhân tạo & Khoa học Dữ liệu - CICT',
      'Cộng đồng nghiên cứu AI, Machine Learning, LLM và tổ chức các buổi seminar học thuật chuyên sâu tại Trường CNTT&TT.',
      'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?auto=format&fit=crop&w=200&q=80',
      'ACTIVE', 'Câu lạc bộ / Đội / Nhóm', 'CLB Trí tuệ Nhân tạo & Khoa học Dữ liệu - CICT', 'Đoàn Trường Công nghệ Thông tin và Truyền thông - ĐHCT', 'ai.club@ctu.edu.vn', '02923872401', NULL, TRUE, NOW(), NOW()),
(102, 'Đội Văn nghệ Xung kích - Đại học Cần Thơ',
      'Tổ chức các chương trình biểu diễn nghệ thuật, Acoustic, hội thi văn nghệ truyền thống và giao lưu văn hóa sinh viên.',
      'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=200&q=80',
      'ACTIVE', 'Câu lạc bộ / Đội / Nhóm', 'Đội Văn nghệ Xung kích - Đại học Cần Thơ', 'Hội Sinh viên Đại học Cần Thơ', 'vannghe@ctu.edu.vn', '02923872402', NULL, TRUE, NOW(), NOW()),
(103, 'Đoàn Thanh niên Tình nguyện CICT - Đại học Cần Thơ',
      'Điều phối các chiến dịch tình nguyện vì cộng đồng, Chủ nhật Xanh, Tiếp sức mùa thi và hiến máu nhân đạo của sinh viên CNTT&TT.',
      'https://images.unsplash.com/photo-1559027615-cd4628902d4a?auto=format&fit=crop&w=200&q=80',
      'ACTIVE', 'YOUTH_UNION', 'Đoàn Thanh niên Tình nguyện CICT - Đại học Cần Thơ', 'Đoàn Thanh niên Cộng sản Hồ Chí Minh Đại học Cần Thơ', 'tinhnguyen.cict@ctu.edu.vn', '02923872403', NULL, TRUE, NOW(), NOW()),
(104, 'Liên chi hội Sinh viên Thể thao - Đại học Cần Thơ',
      'Kết nối phong trào rèn luyện thể lực, giải bóng đá, cầu lông, điền kinh và hội thao sinh viên khỏe các cấp.',
      'https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=200&q=80',
      'ACTIVE', 'STUDENT_ASSOCIATION', 'Liên chi hội Sinh viên Thể thao - Đại học Cần Thơ', 'Hội Sinh viên Đại học Cần Thơ', 'lch.thethao@ctu.edu.vn', '02923872404', NULL, TRUE, NOW(), NOW()),
(105, 'Đoàn Khoa Ngoại ngữ - Đại học Cần Thơ',
      'Tổ chức các cuộc thi hùng biện, giao lưu văn hóa quốc tế, hội thảo phương pháp học ngoại ngữ và nâng cao năng lực hội nhập.',
      'https://images.unsplash.com/photo-1543269865-cbf427effbad?auto=format&fit=crop&w=200&q=80',
      'ACTIVE', 'YOUTH_UNION', 'Đoàn Khoa Ngoại ngữ - Đại học Cần Thơ', 'Đoàn Thanh niên Cộng sản Hồ Chí Minh Đại học Cần Thơ', 'doan.ngoaingu@ctu.edu.vn', '02923872405', NULL, TRUE, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO fanpage_members (fanpage_id, user_id, role, is_active, created_at, updated_at) VALUES
(101, 401, 'ADMIN',  TRUE, NOW(), NOW()),
(102, 402, 'ADMIN',  TRUE, NOW(), NOW()),
(103, 403, 'ADMIN',  TRUE, NOW(), NOW()),
(104, 404, 'ADMIN',  TRUE, NOW(), NOW()),
(105, 405, 'ADMIN',  TRUE, NOW(), NOW()),
(101, 403, 'MEMBER', TRUE, NOW(), NOW()),
(105, 402, 'MEMBER', TRUE, NOW(), NOW())
ON CONFLICT (fanpage_id, user_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 3. 16 SỰ KIỆN ĐA DẠNG LOẠI HÌNH, TRẠNG THÁI, ĐIỂM RÈN LUYỆN & TIÊU CHÍ SV5T
--    - event_type_id: 1 (Hội thảo học thuật), 2 (Tình nguyện), 3 (Văn hóa - Văn nghệ), 4 (Thể dục - Thể thao)
--    - criteria_id: kết hợp cả UNIVERSITY (1031..5035) và FACULTY (61032..65031)
-- ----------------------------------------------------------------------------
INSERT INTO events (
    id, name, description, location,
    date_open, date_close, date_happen,
    capacity, male_quantity, female_quantity, gender_constraint, banner_url,
    status, allow_self_check,
    event_type_id, criteria_id, fanpage_id, semester_id, school_id, is_active, created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES

-- === LOẠI 1: HỘI THẢO HỌC THUẬT (event_type_id = 1) ===
(101, 'Seminar: Ứng dụng RAG & Agentic AI trong Hệ thống Doanh nghiệp',
 'Chia sẻ kiến trúc triển khai hệ thống AI tìm kiếm tri thức nội bộ và đánh giá độ chính xác mô hình ngôn ngữ lớn.',
 'Hội trường 1 - Trường CNTT&TT (Khu II ĐHCT)',
 NOW() - INTERVAL '2 days', NOW() + INTERVAL '5 days', NOW() + INTERVAL '8 days',
 16, 8, 8, FALSE, 'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE, 1, 5023, 101,
 COALESCE((SELECT id FROM semesters WHERE status = 'ONGOING' ORDER BY id LIMIT 1), (SELECT id FROM semesters ORDER BY id LIMIT 1)),
 (SELECT id FROM schools WHERE id = 1), TRUE, NOW() - INTERVAL '2 days', NOW()),

(102, 'Cuộc thi Nghiên cứu Khoa học & Sáng tạo Khởi nghiệp Sinh viên 2026',
 'Sân chơi trình bày đề tài NCKH cấp cơ sở và ý tưởng khởi nghiệp công nghệ dành cho sinh viên các khóa.',
 'Nhà học B1 - Khu II ĐHCT',
 NOW() - INTERVAL '3 days', NOW() + INTERVAL '8 days', NOW() + INTERVAL '12 days',
 20, 10, 10, FALSE, 'https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', FALSE, 1, 5034, 101,
 COALESCE((SELECT id FROM semesters WHERE status = 'ONGOING' ORDER BY id LIMIT 1), (SELECT id FROM semesters ORDER BY id LIMIT 1)),
 NULL, TRUE, NOW() - INTERVAL '3 days', NOW()),

(103, 'Hội thảo Phương pháp Học Thuật & Hùng biện Tiếng Anh Học thuật',
 'Hướng dẫn kỹ năng viết luận, thuyết trình học thuật bằng tiếng Anh và chiến lược ôn thi chuẩn CEFR B1-B2.',
 'Hội trường Khoa Ngoại ngữ - Khu II ĐHCT',
 NOW() - INTERVAL '1 day', NOW() + INTERVAL '10 days', NOW() + INTERVAL '14 days',
 12, 6, 6, FALSE, 'https://images.unsplash.com/photo-1543269865-cbf427effbad?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE, 1, 5011, 105,
 COALESCE((SELECT id FROM semesters WHERE status = 'ONGOING' ORDER BY id LIMIT 1), (SELECT id FROM semesters ORDER BY id LIMIT 1)),
 (SELECT id FROM schools WHERE id = 9), TRUE, NOW() - INTERVAL '1 day', NOW()),

(104, 'Workshop: Nhập môn Phân tích Dữ liệu với Python & SQL',
 'Buổi thực hành trực tiếp xử lý dữ liệu và trực quan hóa biểu đồ phục vụ nghiên cứu khoa học.',
 'Phòng Lab 202 - Trường CNTT&TT',
 NOW() - INTERVAL '2 days', NOW() + INTERVAL '6 days', NOW() + INTERVAL '9 days',
 14, 7, 7, FALSE, 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE, 1, 5023, 101,
 COALESCE((SELECT id FROM semesters WHERE status = 'ONGOING' ORDER BY id LIMIT 1), (SELECT id FROM semesters ORDER BY id LIMIT 1)),
 (SELECT id FROM schools WHERE id = 1), TRUE, NOW() - INTERVAL '2 days', NOW()),

-- === LOẠI 2: HOẠT ĐỘNG TÌNH NGUYỆN (event_type_id = 2) ===
(105, 'Chiến dịch "Ngày Thứ Bảy Tình Nguyện" Cải tạo Không gian Xanh CICT',
 'Trồng cây xanh, thu gom rác tái chế và vệ sinh khuôn viên giảng đường Trường CNTT&TT.',
 'Sảnh Trường CNTT&TT - Khu II ĐHCT',
 NOW() - INTERVAL '3 days', NOW() + INTERVAL '4 days', NOW() + INTERVAL '7 days',
 16, 8, 8, FALSE, 'https://images.unsplash.com/photo-1542601906990-b4d3fb778b09?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE, 2, 6402, 103,
 COALESCE((SELECT id FROM semesters WHERE status = 'ONGOING' ORDER BY id LIMIT 1), (SELECT id FROM semesters ORDER BY id LIMIT 1)),
 (SELECT id FROM schools WHERE id = 1), TRUE, NOW() - INTERVAL '3 days', NOW()),

(106, 'Chương trình Tuyên truyền An toàn Giao thông & Phòng chống Tội phạm Mạng',
 'Phổ biến pháp luật giao thông đường bộ và kỹ năng phòng tránh lừa đảo trực tuyến trong cộng đồng sinh viên.',
 'Hội trường Rùa - Khu II ĐHCT',
 NOW() - INTERVAL '2 days', NOW() + INTERVAL '7 days', NOW() + INTERVAL '10 days',
 20, 10, 10, FALSE, 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', FALSE, 2, 1033, 103,
 COALESCE((SELECT id FROM semesters WHERE status = 'ONGOING' ORDER BY id LIMIT 1), (SELECT id FROM semesters ORDER BY id LIMIT 1)),
 NULL, TRUE, NOW() - INTERVAL '2 days', NOW()),

(107, 'Ngày hội Hiến máu Nhân đạo "Nghĩa tình Sinh viên CTU"',
 'Chương trình hiến máu tình nguyện phối hợp Bệnh viện Huyết học - Truyền máu TP. Cần Thơ.',
 'Nhà Điều hành - Khu II ĐHCT',
 NOW() - INTERVAL '1 day', NOW() + INTERVAL '9 days', NOW() + INTERVAL '12 days',
 18, 9, 9, FALSE, 'https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', FALSE, 2, 403, 103,
 COALESCE((SELECT id FROM semesters WHERE status = 'ONGOING' ORDER BY id LIMIT 1), (SELECT id FROM semesters ORDER BY id LIMIT 1)),
 NULL, TRUE, NOW() - INTERVAL '1 day', NOW()),

(108, 'Chuyến đi Thiện nguyện "Áo ấm Vùng ven" - Huyện Phong Điền',
 'Trao quà và hỗ trợ học sinh có hoàn cảnh khó khăn tại địa bàn huyện Phong Điền, TP. Cần Thơ.',
 'Huyện Phong Điền, TP. Cần Thơ',
 NOW() - INTERVAL '1 day', NOW() + INTERVAL '11 days', NOW() + INTERVAL '15 days',
 10, 5, 5, FALSE, 'https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', FALSE, 2, 402, 103,
 COALESCE((SELECT id FROM semesters WHERE status = 'ONGOING' ORDER BY id LIMIT 1), (SELECT id FROM semesters ORDER BY id LIMIT 1)),
 NULL, TRUE, NOW() - INTERVAL '1 day', NOW()),

-- === LOẠI 3: VĂN HÓA - VĂN NGHỆ (event_type_id = 3) ===
(109, 'Hội thi Tiếng hát Sinh viên & Ca khúc Truyền thống CTU 2026',
 'Sân chơi âm nhạc ca ngợi quê hương, đất nước và tuổi trẻ sinh viên Đại học Cần Thơ.',
 'Hội trường Rùa - Khu II ĐHCT',
 NOW() - INTERVAL '3 days', NOW() + INTERVAL '6 days', NOW() + INTERVAL '9 days',
 20, 10, 10, FALSE, 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE, 3, 61032, 102,
 COALESCE((SELECT id FROM semesters WHERE status = 'ONGOING' ORDER BY id LIMIT 1), (SELECT id FROM semesters ORDER BY id LIMIT 1)),
 NULL, TRUE, NOW() - INTERVAL '3 days', NOW()),

(110, 'Đêm hội Giao lưu Văn hóa Quốc tế & Âm nhạc Mekong',
 'Giao lưu văn nghệ, trình diễn trang phục truyền thống và ẩm thực giữa sinh viên Việt Nam và lưu học sinh quốc tế.',
 'Sân khấu ngoài trời - Khu II ĐHCT',
 NOW() - INTERVAL '2 days', NOW() + INTERVAL '9 days', NOW() + INTERVAL '13 days',
 18, 9, 9, FALSE, 'https://images.unsplash.com/photo-1511632765486-a01980e01a18?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE, 3, 65031, 105,
 COALESCE((SELECT id FROM semesters WHERE status = 'ONGOING' ORDER BY id LIMIT 1), (SELECT id FROM semesters ORDER BY id LIMIT 1)),
 NULL, TRUE, NOW() - INTERVAL '2 days', NOW()),

(111, 'Cuộc thi Tìm hiểu Tư tưởng Hồ Chí Minh qua Sân khấu hóa',
 'Hội thi tuyên truyền tư tưởng, đạo đức, phong cách Hồ Chí Minh kết hợp tiểu phẩm sân khấu.',
 'Hội trường Khoa KHCT - Khu II ĐHCT',
 NOW() - INTERVAL '2 days', NOW() + INTERVAL '12 days', NOW() + INTERVAL '16 days',
 14, 7, 7, FALSE, 'https://images.unsplash.com/photo-1531482615713-2afd69097998?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', FALSE, 3, 1031, 102,
 COALESCE((SELECT id FROM semesters WHERE status = 'ONGOING' ORDER BY id LIMIT 1), (SELECT id FROM semesters ORDER BY id LIMIT 1)),
 NULL, TRUE, NOW() - INTERVAL '2 days', NOW()),

(112, 'Đêm nhạc Acoustic "Mùa Thu Sân Trường"',
 'Chương trình giao lưu âm nhạc gây quỹ học bổng sinh viên vượt khó tại Ký túc xá Khu B.',
 'Sân Ký túc xá Khu B - ĐHCT',
 NOW() - INTERVAL '1 day', NOW() + INTERVAL '13 days', NOW() + INTERVAL '17 days',
 12, 6, 6, FALSE, 'https://images.unsplash.com/photo-1528605248644-14dd04022da1?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE, 3, 1032, 102,
 COALESCE((SELECT id FROM semesters WHERE status = 'ONGOING' ORDER BY id LIMIT 1), (SELECT id FROM semesters ORDER BY id LIMIT 1)),
 NULL, TRUE, NOW() - INTERVAL '1 day', NOW()),

-- === LOẠI 4: THỂ DỤC - THỂ THAO (event_type_id = 4) ===
(113, 'Giải Chạy Việt dã "Bước chân Sinh viên Khỏe" ĐHCT 2026',
 'Giải chạy phong trào cự ly 3km và 5km quanh khuôn viên Khu II, tích lũy tiêu chí Thể lực tốt SV5T.',
 'Sân vận động Khu II - Đại học Cần Thơ',
 NOW() - INTERVAL '3 days', NOW() + INTERVAL '7 days', NOW() + INTERVAL '11 days',
 16, 8, 8, TRUE, 'https://images.unsplash.com/photo-1452626038306-9aae5e071dd3?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE, 4, 302, 104,
 COALESCE((SELECT id FROM semesters WHERE status = 'ONGOING' ORDER BY id LIMIT 1), (SELECT id FROM semesters ORDER BY id LIMIT 1)),
 NULL, TRUE, NOW() - INTERVAL '3 days', NOW()),

(114, 'Giải Cầu lông Sinh viên Mở rộng Cúp Liên chi hội 2026',
 'Thi đấu nội dung đơn nam, đơn nữ và đôi nam nữ dành cho sinh viên chính quy Đại học Cần Thơ.',
 'Nhà thi đấu TDTT - Khu II ĐHCT',
 NOW() - INTERVAL '2 days', NOW() + INTERVAL '10 days', NOW() + INTERVAL '15 days',
 12, 6, 6, TRUE, 'https://images.unsplash.com/photo-1574629810360-7efbbe195018?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', FALSE, 4, 6302, 104,
 COALESCE((SELECT id FROM semesters WHERE status = 'ONGOING' ORDER BY id LIMIT 1), (SELECT id FROM semesters ORDER BY id LIMIT 1)),
 NULL, TRUE, NOW() - INTERVAL '2 days', NOW()),

(115, 'Ngày hội Đồng diễn Dân vũ & Thể dục Nhịp điệu Sinh viên',
 'Hoạt động rèn luyện thể lực tập thể và giao lưu các CLB thể thao trường trực thuộc.',
 'Quảng trường Khu II - ĐHCT',
 NOW() - INTERVAL '1 day', NOW() + INTERVAL '12 days', NOW() + INTERVAL '16 days',
 18, 6, 12, TRUE, 'https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', TRUE, 4, 302, 104,
 COALESCE((SELECT id FROM semesters WHERE status = 'ONGOING' ORDER BY id LIMIT 1), (SELECT id FROM semesters ORDER BY id LIMIT 1)),
 NULL, TRUE, NOW() - INTERVAL '1 day', NOW()),

(116, 'Hội thao Truyền thống Sinh viên ĐHCT Lần thứ XX',
 'Giải đấu tổng hợp các môn bóng đá mini, bóng chuyền và kéo co chào mừng ngày truyền thống sinh viên.',
 'Khu Liên hợp TDTT - Khu II ĐHCT',
 NOW() - INTERVAL '2 days', NOW() + INTERVAL '14 days', NOW() + INTERVAL '18 days',
 20, 12, 8, TRUE, 'https://images.unsplash.com/photo-1574629810360-7efbbe195018?auto=format&fit=crop&w=1080&q=80',
 'PUBLISHED', FALSE, 4, 302, 104,
 COALESCE((SELECT id FROM semesters WHERE status = 'ONGOING' ORDER BY id LIMIT 1), (SELECT id FROM semesters ORDER BY id LIMIT 1)),
 NULL, TRUE, NOW() - INTERVAL '2 days', NOW())
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 4. GẮN ĐA DẠNG ĐIỂM RÈN LUYỆN (event_points) DÙNG ĐÚNG ID LÁ TRONG seed_point_categories.sql (1..73)
-- ----------------------------------------------------------------------------
INSERT INTO event_points (event_id, point_category_id, point, is_active, created_at, updated_at)
SELECT v.event_id, v.point_category_id, v.point, TRUE, NOW(), NOW()
FROM (VALUES
    -- Seminar AI (I.a.5 CLB học thuật + V.b hỗ trợ hoạt động chung)
    (101, 16, 2),
    (101, 63, 3),
    -- Cuộc thi NCKH & Khởi nghiệp (I.b.2 tham gia thi + V.c NCKH/Khởi nghiệp)
    (102, 20, 2),
    (102, 72, 6),
    -- Hội thảo Hùng biện Tiếng Anh (I.a.5 CLB học thuật + V.c cuộc thi học thuật)
    (103, 16, 2),
    (103, 73, 4),
    -- Workshop Python & SQL (I.a.5 + V.b)
    (104, 16, 2),
    (104, 63, 3),
    -- Ngày Thứ Bảy Tình nguyện CICT (II.b.1 bảo vệ cảnh quan + IV.b công tác xã hội)
    (105, 34, 4),
    (105, 54, 5),
    -- Tuyên truyền ATGT & PCTP Mạng (II.a chấp hành quy định + III.c phòng chống tội phạm)
    (106, 31, 4),
    (106, 49, 3),
    -- Hiến máu nhân đạo (IV.b công tác xã hội + III.b.1 lực lượng nòng cốt)
    (107, 54, 5),
    (107, 43, 3),
    -- Thiện nguyện Phong Điền (IV.a tuyên truyền + IV.b công tác xã hội)
    (108, 52, 3),
    (108, 54, 5),
    -- Hội thi Tiếng hát Sinh viên (III.a hoạt động văn nghệ + III.b.1 nòng cốt cơ sở)
    (109, 39, 4),
    (109, 42, 3),
    -- Đêm hội Giao lưu Văn hóa Quốc tế (III.a + V.b)
    (110, 39, 4),
    (110, 63, 3),
    -- Cuộc thi Tư tưởng Hồ Chí Minh (III.a + II.b.2 khảo sát/sinh hoạt)
    (111, 39, 4),
    (111, 36, 1),
    -- Đêm nhạc Acoustic (III.a)
    (112, 39, 3),
    -- Giải Chạy Việt dã (III.a + III.b.1 nòng cốt ĐHCT)
    (113, 39, 4),
    (113, 43, 4),
    -- Giải Cầu lông Mở rộng (III.a + V.b)
    (114, 39, 4),
    (114, 63, 3),
    -- Ngày hội Đồng diễn Dân vũ (III.a)
    (115, 39, 4),
    -- Hội thao Truyền thống (III.a + III.b.1 nòng cốt ĐHCT)
    (116, 39, 5),
    (116, 43, 5)
) AS v(event_id, point_category_id, point)
WHERE EXISTS (SELECT 1 FROM events e WHERE e.id = v.event_id)
  AND EXISTS (SELECT 1 FROM point_categories pc WHERE pc.id = v.point_category_id)
  AND NOT EXISTS (
      SELECT 1 FROM event_points ep
      WHERE ep.event_id = v.event_id AND ep.point_category_id = v.point_category_id
  );

-- ----------------------------------------------------------------------------
-- 5. CẬP NHẬT SEQUENCE AN TOÀN
-- ----------------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('users', 'id'),                  GREATEST((SELECT COALESCE(MAX(id), 1) FROM users), 405), TRUE);
SELECT setval(pg_get_serial_sequence('fanpage_admin_profiles', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM fanpage_admin_profiles), 405), TRUE);
SELECT setval(pg_get_serial_sequence('fanpages', 'id'),               GREATEST((SELECT COALESCE(MAX(id), 1) FROM fanpages), 105), TRUE);
SELECT setval(pg_get_serial_sequence('events', 'id'),                 GREATEST((SELECT COALESCE(MAX(id), 1) FROM events), 116), TRUE);
SELECT setval(pg_get_serial_sequence('event_points', 'id'),           GREATEST((SELECT COALESCE(MAX(id), 1) FROM event_points), 1), TRUE);

COMMIT;
