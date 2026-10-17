-- ============================================================================
-- SEED DATA: CRITERIAS (TIÊU CHUẨN XÉT CHỌN DANH HIỆU SINH VIÊN 5 TỐT ĐHCT)
-- Căn cứ Quyết định số 17/QĐ-HSV ngày 04/3/2026 của Ban Chấp hành Hội Sinh viên VN Đại học Cần Thơ
-- Đối tượng: Chỉ xét CÁ NHÂN Sinh viên 5 tốt
-- Scope: Cấp trường ('UNIVERSITY')
-- Khớp 100% thứ tự cột trong schema.sql:
-- id, name, note, scope, level, children_order, parent_id, is_active
-- ============================================================================

-- Reset bảng criterias
TRUNCATE TABLE criterias RESTART IDENTITY CASCADE;

INSERT INTO criterias
    (id, name, note, scope, level, children_order, parent_id, is_active)
    OVERRIDING SYSTEM VALUE
VALUES
-- ============================================================================
-- DANH HIỆU "SINH VIÊN 5 TỐT" CẤP ĐẠI HỌC CẦN THƠ (CẤP TRƯỜNG - UNIVERSITY)
-- ============================================================================
(1, 'Danh hiệu "Sinh viên 5 tốt" Đại học Cần Thơ',
 'Ban hành kèm Quyết định 17/QĐ-HSV ngày 04/3/2026 (giai đoạn 2025-2028). Để xét cấp ĐHCT phải đạt danh hiệu SV5T cấp cơ sở/khoa',
 'UNIVERSITY', 0, 1, NULL, TRUE),

-- --------------------------------------------------------------------------
-- 1. TIÊU CHUẨN ĐẠO ĐỨC TỐT (Đạt đủ 03 tiêu chuẩn: 1.1, 1.2 và 1.3)
-- --------------------------------------------------------------------------
(2, 'Tiêu chuẩn Đạo đức tốt', 'Đạt đủ 03 tiêu chuẩn (1.1), (1.2) và (1.3)', 'UNIVERSITY', 1, 1, 1, TRUE),
(3, 'Điểm rèn luyện đạt từ 85 điểm trở lên',
 'Điểm rèn luyện TBC học kỳ 3 năm học trước, học kỳ 1 và 2 năm học xét đạt từ 85 điểm trở lên (SV năm nhất chỉ xét HK1 và HK2)',
 'UNIVERSITY', 2, 1, 2, TRUE),
(4, 'Không vi phạm pháp luật và các quy chế, nội quy của nhà trường, địa phương và cộng đồng', NULL, 'UNIVERSITY', 2,
 2, 2, TRUE),
(5, 'Đạt thêm 01 trong 05 tiêu chí về tư tưởng, đạo đức, lối sống',
 'Đạt ít nhất 01 tiêu chí trong 05 tiêu chí bên dưới (1.3.1 đến 1.3.5)', 'UNIVERSITY', 2, 3, 2, TRUE),
(6,
 'Tham gia cuộc thi tìm hiểu về các môn khoa học chính trị, tư tưởng Hồ Chí Minh do Đoàn - Hội cấp cơ sở trở lên tổ chức',
 NULL, 'UNIVERSITY', 3, 1, 5, TRUE),
(7, 'Tham gia hoạt động giáo dục lòng yêu nước, tinh thần tự hào dân tộc',
 'Tham quan di tích, bảo tàng; thăm hỏi mẹ VNAH; thắp nến tri ân; hội thi hát ca khúc truyền thống; hoạt động biên giới hải đảo...',
 'UNIVERSITY', 3, 2, 5, TRUE),
(8, 'Tham gia hoạt động giáo dục đạo đức, lối sống văn hóa cho sinh viên', NULL, 'UNIVERSITY', 3, 3, 5, TRUE),
(9, 'Là thanh niên tiêu biểu trong học tập và làm theo lời Bác', NULL, 'UNIVERSITY', 3, 4, 5, TRUE),
(10, 'Là Đảng viên hoàn thành tốt nhiệm vụ', NULL, 'UNIVERSITY', 3, 5, 5, TRUE),

-- --------------------------------------------------------------------------
-- 2. TIÊU CHUẨN HỌC TẬP TỐT
-- --------------------------------------------------------------------------
(11, 'Tiêu chuẩn Học tập tốt', 'Điểm trung bình cộng năm học đạt từ 3.0 trở lên, không nợ học phần', 'UNIVERSITY', 1, 2,
 1, TRUE),
(12, 'Điểm trung bình cộng năm học xét đạt từ 3.0 trở lên',
 'Điểm TBC học kỳ 3 năm trước, HK1 và HK2 năm học xét đạt từ 3.0 trở lên (SV năm nhất xét HK1 và HK2)', 'UNIVERSITY', 2,
 1, 11, TRUE),
(13, 'Tích lũy tối thiểu 12 tín chỉ/học kỳ và không bị nợ học phần',
 'Số tín chỉ mỗi HK >= 12 tín chỉ (trường hợp đặc biệt do Hội đồng ĐHCT xem xét); không bị nợ học phần', 'UNIVERSITY',
 2, 2, 11, TRUE),

-- --------------------------------------------------------------------------
-- 3. TIÊU CHUẨN THỂ LỰC TỐT (Đạt 01 trong 02 tiêu chuẩn)
-- --------------------------------------------------------------------------
(14, 'Tiêu chuẩn Thể lực tốt',
 'Đạt 01 trong 02 tiêu chuẩn (3.1) hoặc (3.2). SV khuyết tật/bệnh điều trị được miễn theo xác nhận Nhà trường',
 'UNIVERSITY', 1, 3, 1, TRUE),
(15, 'Học ít nhất 01 học phần Giáo dục thể chất có rèn luyện thể lực đạt điểm B trở lên', NULL, 'UNIVERSITY', 2, 1, 14,
 TRUE),
(16, 'Tham gia ít nhất 01 hoạt động rèn luyện thể lực do Đoàn - Hội cấp cơ sở trở lên tổ chức',
 'Thể dục, thể thao, dân vũ...', 'UNIVERSITY', 2, 2, 14, TRUE),

-- --------------------------------------------------------------------------
-- 4. TIÊU CHUẨN TÌNH NGUYỆN TỐT (Đạt 01 trong 03 tiêu chuẩn)
-- --------------------------------------------------------------------------
(17, 'Tiêu chuẩn Tình nguyện tốt', 'Đạt 01 trong 03 tiêu chuẩn (4.1), (4.2) hoặc (4.3)', 'UNIVERSITY', 1, 4, 1, TRUE),
(18, 'Được các cấp Đảng, Đoàn, Hội, chính quyền khen thưởng trong hoạt động tình nguyện', NULL, 'UNIVERSITY', 2, 1, 17,
 TRUE),
(19, 'Tham gia ít nhất 03 ngày tình nguyện trong năm học', NULL, 'UNIVERSITY', 2, 2, 17, TRUE),
(20, 'Tham gia ít nhất 02 lần hiến máu tình nguyện trong năm học', NULL, 'UNIVERSITY', 2, 3, 17, TRUE),

-- --------------------------------------------------------------------------
-- 5. TIÊU CHUẨN HỘI NHẬP TỐT (Đạt 02 trong 03 tiêu chuẩn: Ngoại ngữ, Tin học, Kỹ năng)
-- --------------------------------------------------------------------------
(21, 'Tiêu chuẩn Hội nhập tốt',
 'Đạt 02 trong 03 tiêu chuẩn: (5.1) Ngoại ngữ, (5.2) Tin học, (5.3) Hoạt động phong trào kỹ năng hội nhập',
 'UNIVERSITY', 1, 5, 1, TRUE),

-- (5.1) Ngoại ngữ
(22, 'Tiêu chuẩn Ngoại ngữ', 'Đạt 01 trong 05 tiêu chí bên dưới (5.1.1 đến 5.1.5)', 'UNIVERSITY', 2, 1, 21, TRUE),
(23, 'Được khen thưởng cuộc thi ngoại ngữ cấp Đại học Cần Thơ trở lên', NULL, 'UNIVERSITY', 3, 1, 22, TRUE),
(24, 'Đạt chứng chỉ ngoại ngữ theo bảng quy đổi chuẩn CEFR (IELTS, TOEIC, TOEFL, DELF, HSK, TOPIK, JLPT)',
 'SV năm 1-2: A2 / IELTS 4.0 / TOEIC 400. SV năm 3 trở lên: B1 / IELTS 4.5 / TOEIC 450 (chứng chỉ thi trong năm học xét)',
 'UNIVERSITY', 3, 2, 22, TRUE),
(25, 'Sinh viên năm thứ hai: Đã hoàn thành hoặc được miễn 03 học phần Ngoại ngữ căn bản',
 'Mỗi sinh viên chỉ được xét 01 lần', 'UNIVERSITY', 3, 3, 22, TRUE),
(26, 'Sinh viên năm thứ nhất: Đã hoàn thành hoặc được miễn 02 học phần Ngoại ngữ căn bản trở lên',
 'Mỗi sinh viên chỉ được xét 01 lần', 'UNIVERSITY', 3, 4, 22, TRUE),
(27, 'Sinh viên chuyên ngành ngoại ngữ: Điểm TBC học tập năm học xét đạt từ 3.2 trở lên',
 'TBC HK3 năm trước, HK1 và HK2 năm xét >= 3.2 (SV năm nhất xét HK1, HK2); xét chứng chỉ môn ngoại ngữ 2', 'UNIVERSITY',
 3, 5, 22, TRUE),

-- (5.2) Tin học
(28, 'Tiêu chuẩn Tin học', 'Đạt 01 trong 06 tiêu chí bên dưới (5.2.1 đến 5.2.6)', 'UNIVERSITY', 2, 2, 21, TRUE),
(29, 'Đạt chứng chỉ Ứng dụng công nghệ thông tin cơ bản trở lên do Đại học Cần Thơ cấp', NULL, 'UNIVERSITY', 3, 1,
 28, TRUE),
(30, 'Đạt chứng chỉ tin học quốc tế IC3 hoặc MOS (Word, Excel, PowerPoint)', NULL, 'UNIVERSITY', 3, 2, 28, TRUE),
(31, 'Được cấp giấy khen hoặc giấy chứng nhận tham gia cuộc thi tin học do Đoàn cấp cơ sở trở lên tổ chức', NULL,
 'UNIVERSITY', 3, 3, 28, TRUE),
(32, 'Điểm trung bình của 2 học phần Năng lực số ứng dụng và Thực hành năng lực số ứng dụng đạt từ B trở lên', NULL,
 'UNIVERSITY', 3, 4, 28, TRUE),
(33, 'Học ít nhất 01 học phần/khóa học CNTT đạt từ B / 7.0 trở lên hoặc hoàn thành khóa UDCNTT cơ bản ĐHCT',
 'Trong năm học có học ít nhất 01 học phần (khác 5.2.4) đạt B trở lên, hoặc khóa học đạt từ 7.0 trở lên', 'UNIVERSITY',
 3, 5, 28, TRUE),
(34, 'Sinh viên chuyên ngành Công nghệ thông tin: Điểm TBC học tập năm học xét đạt từ 3.2 trở lên',
 'TBC HK3 năm trước, HK1 và HK2 năm xét đạt từ 3.2 trở lên (SV năm nhất xét HK1 và HK2)', 'UNIVERSITY', 3, 6, 28,
 TRUE),

-- (5.3) Hoạt động phong trào kỹ năng hội nhập
(35, 'Tham gia các hoạt động, phong trào nâng cao kỹ năng hội nhập',
 'Đạt 01 trong 05 tiêu chí bên dưới (5.3.1 đến 5.3.5)', 'UNIVERSITY', 2, 3, 21, TRUE),
(36, 'Tham gia 02 hoạt động hội nhập do cấp Đại học Cần Thơ trở lên tổ chức',
 'Giao lưu quốc tế, trao đổi SV, cuộc thi ngoại ngữ, tìm hiểu văn hóa lịch sử xã hội trong nước và thế giới...',
 'UNIVERSITY', 3, 1, 35, TRUE),
(37, 'Được Đoàn Thanh niên, Hội Sinh viên cấp ĐHCT trở lên khen thưởng thành tích xuất sắc phong trào TN-SV', NULL,
 'UNIVERSITY', 3, 2, 35, TRUE),
(38, 'Trực tiếp tổ chức hoặc tham gia ít nhất 01 lớp tập huấn bồi dưỡng kỹ năng do Đoàn-Hội ĐHCT trở lên tổ chức',
 NULL, 'UNIVERSITY', 3, 3, 35, TRUE),
(39, 'Đạt điểm B trở lên học phần kỹ năng (Kỹ năng mềm, ĐMST & khởi nghiệp) và tham gia 01 hoạt động hội nhập ĐHCT',
 NULL, 'UNIVERSITY', 3, 4, 35, TRUE),
(40, 'Đạt giải Ba trở lên trong cuộc thi kiến thức hội nhập hoặc cuộc thi học thuật bằng ngoại ngữ cấp cơ sở/ĐHCT',
 NULL, 'UNIVERSITY', 3, 5, 35, TRUE);

-- Reset sequence để backend chèn thêm tiêu chí sau này không bị lỗi trùng ID
ALTER TABLE criterias
    ALTER COLUMN id RESTART WITH 41;