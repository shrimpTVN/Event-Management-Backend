-- Migration: Add system_point_record and manual_point_record to student_semester_points
ALTER TABLE student_semester_points ADD COLUMN IF NOT EXISTS system_point_record INTEGER NOT NULL DEFAULT 0;
ALTER TABLE student_semester_points ADD COLUMN IF NOT EXISTS manual_point_record INTEGER NOT NULL DEFAULT 0;
