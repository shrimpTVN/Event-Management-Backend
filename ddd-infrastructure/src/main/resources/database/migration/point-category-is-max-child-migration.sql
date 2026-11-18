-- Migration: Add is_max_child to point_categories
ALTER TABLE point_categories ADD COLUMN IF NOT EXISTS is_max_child BOOLEAN NOT NULL DEFAULT FALSE;
