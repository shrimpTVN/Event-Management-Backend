-- ============================================================================
-- PATCH: Chuẩn hóa point_categories theo QĐ 4932/QĐ-ĐHCT (áp dụng từ HK1 2026-2027)
--        và tính lại student_semester_points trên mydb
-- ============================================================================

BEGIN;

-- 1. Sửa sai phân cấp cha-con (giữ nguyên ID để bảo toàn FK event_points / student_semester_points)
--    - id = 1045 (Tham gia các CLB học thuật): là mục con trực tiếp của I.a (10), không thuộc 104 (Ngoại ngữ)
UPDATE point_categories
SET parent_category_id = 10,
    level = 2,
    children_order = 5,
    updated_at = NOW()
WHERE id = 1045;

--    - id = 2113 (Tham gia khảo sát ý kiến do ĐHCT tổ chức): là mục con trực tiếp của II.b (21), không thuộc 211 (Tuyên truyền ANTT)
UPDATE point_categories
SET parent_category_id = 21,
    level = 2,
    children_order = 2,
    updated_at = NOW()
WHERE id = 2113;

-- 2. Xác định chính xác các nhóm is_max_child dựa theo Phụ lục QĐ 4932/QĐ-ĐHCT (2026) & HD 1840/HD-ĐHCT (2008)
UPDATE point_categories
SET is_max_child = (id IN (
    102, -- I.a.2 Nghiên cứu khoa học (Tham gia 5 / Chủ nhiệm 8 / Bài báo 8)
    103, -- I.a.3 Chứng chỉ tin học (Cơ bản 3 / Nâng cao 5 - chỉ tính 1 lần toàn khóa)
    104, -- I.a.4 Chứng chỉ ngoại ngữ (A1 3 / A2 5 / B1 7 / >=B2 10 - chỉ tính 1 lần toàn khóa)
    112, -- I.b.2 Kỳ thi chuyên ngành, Olympic (Tham gia 2 / Đạt giải 4)
    122, -- I.c.2 Kết quả học tập ĐTBCHK (8 / 6 / 4 / 2 - loại trừ lẫn nhau)
    211, -- II.b.1 Tuyên truyền ANTT, vệ sinh (Đoàn cơ sở 8 / Đoàn ĐHCT, HSV 8)
    311, -- III.b.1 Lực lượng nòng cốt phong trào (Cấp BM/Khoa 3 / Cấp ĐHCT 5)
    312, -- III.b.2 Khen thưởng hoạt động phong trào (Cơ sở 6 / Đại học 8 / Cao hơn 10)
    50   -- V.a Công tác cán bộ lớp, Đoàn-Hội (10 / 8 / 6 / 3 - "trừ các thành viên ở tiêu chí trên")
)),
    updated_at = NOW();

-- 3. Chuẩn hóa nội dung tên & ghi chú theo Phụ lục QĐ 4932/QĐ-ĐHCT
UPDATE point_categories
SET note = 'Mã tiêu chí: I.a.3 • Chỉ tính 01 lần trong toàn khóa học',
    updated_at = NOW()
WHERE id = 103;

UPDATE point_categories
SET note = 'Mã tiêu chí: I.a.4 • Chỉ tính 01 lần trong toàn khóa học',
    updated_at = NOW()
WHERE id = 104;

UPDATE point_categories
SET note = 'Khung NLNN 6 bậc VN – Bậc 2 (A2) • TOEIC Bridge: 43-75 • TOEFL CBT: 96 • TOEFL iBT: 30 • KET/PET: 120-140',
    updated_at = NOW()
WHERE id = 1042;

UPDATE point_categories
SET note = 'Khung NLNN 6 bậc VN – Bậc 3 (B1) • TOEIC Bridge: 76-89 • TOEFL CBT: 133 • TOEFL iBT: 31-45 • Cambridge: 140-150 KET; 140-160 PET; 140-160 FCE',
    updated_at = NOW()
WHERE id = 1043;

UPDATE point_categories
SET note = 'Khung NLNN 6 bậc VN – Bậc 4 (B2) • TOEIC Bridge: >= 90 • TOEFL CBT: >= 173 • TOEFL iBT: >= 46 • Cambridge: >= 160 PET, FCE, CAE; >= 180 CPE',
    updated_at = NOW()
WHERE id = 1044;

UPDATE point_categories
SET name = 'Có cố gắng, tiến bộ trong học tập',
    note = 'Có ĐTB học kỳ sau lớn hơn học kỳ trước đó; đối với SV năm thứ nhất, học kỳ I không có điểm F. Nếu: ĐTBHK >= 3,6 thì được cộng 2 điểm.',
    updated_at = NOW()
WHERE id = 121;

UPDATE point_categories
SET note = 'Mỗi lần vi phạm trừ 03 điểm',
    updated_at = NOW()
WHERE id = 201;

UPDATE point_categories
SET name = 'BCH Liên chi hội SV, BCH Chi đoàn, BCH Chi hội SV, BCN CLB/Đội/Nhóm (trừ các thành viên ở tiêu chí trên), Lớp phó, HĐTQ KTX, Đội SV an ninh KTX, Nhà trưởng KTX, HĐTQ ngoại trú',
    updated_at = NOW()
WHERE id = 502;

-- 4. Dọn giao dịch MANUAL test bị cộng dồn vượt trần trên mục 3112 (user_id = 1, semester_id = 1)
--    Giữ lại giao dịch gốc 5 điểm (id = 40), xóa 2 giao dịch cộng trùng lúc test (id = 88, 89)
DELETE FROM point_transactions
WHERE id IN (88, 89)
  AND user_id = 1
  AND semester_id = 1
  AND point_category_id = 3112
  AND type = 'MANUAL';

-- 5. Tính lại toàn bộ student_semester_points từ point_transactions (CONFIRMED)
--    Áp dụng đúng is_max_child và chặn trần LEAST(maximum, ...) ở cả lá lẫn từng cấp cha
TRUNCATE TABLE student_semester_points RESTART IDENTITY CASCADE;

INSERT INTO student_semester_points (
    user_id, semester_id, point_category_id,
    system_point_record, manual_point_record, total_point,
    created_at, updated_at
)
SELECT
    pt.user_id,
    pt.semester_id,
    pt.point_category_id,
    LEAST(
        pc.maximum,
        GREATEST(0, COALESCE(SUM(CASE WHEN pt.type = 'CHECKOUT_REWARD' THEN pt.points ELSE 0 END), 0))
    ),
    LEAST(
        pc.maximum,
        GREATEST(0, COALESCE(SUM(CASE WHEN pt.type = 'MANUAL' THEN pt.points ELSE 0 END), 0))
    ),
    LEAST(
        pc.maximum,
        GREATEST(0, COALESCE(SUM(pt.points), 0))
    ),
    NOW(),
    NOW()
FROM point_transactions pt
JOIN point_categories pc ON pc.id = pt.point_category_id
WHERE pt.status = 'CONFIRMED'
GROUP BY pt.user_id, pt.semester_id, pt.point_category_id, pc.maximum;

DO $$
DECLARE
    curr_lvl INT;
    max_lvl INT;
    min_lvl INT;
BEGIN
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
            LEAST(
                parent_pc.maximum,
                CASE
                    WHEN parent_pc.is_max_child THEN COALESCE(
                        (ARRAY_AGG(child_ssp.system_point_record ORDER BY COALESCE(child_ssp.total_point, -1) DESC NULLS LAST, COALESCE(child_ssp.manual_point_record, -1) DESC NULLS LAST, child_pc.id ASC)
                         FILTER (WHERE child_ssp.id IS NOT NULL))[1],
                        0
                    )
                    ELSE SUM(COALESCE(child_ssp.system_point_record, 0))
                END
            ),
            LEAST(
                parent_pc.maximum,
                CASE
                    WHEN parent_pc.is_max_child THEN COALESCE(
                        (ARRAY_AGG(child_ssp.manual_point_record ORDER BY COALESCE(child_ssp.total_point, -1) DESC NULLS LAST, COALESCE(child_ssp.manual_point_record, -1) DESC NULLS LAST, child_pc.id ASC)
                         FILTER (WHERE child_ssp.id IS NOT NULL))[1],
                        0
                    )
                    ELSE SUM(COALESCE(child_ssp.manual_point_record, 0))
                END
            ),
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

COMMIT;
