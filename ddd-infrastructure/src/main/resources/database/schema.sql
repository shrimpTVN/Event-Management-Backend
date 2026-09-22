-- ============================================================
--  PostgreSQL Schema - Centralized Event Management Platform
--  Primary Keys: BIGINT GENERATED ALWAYS AS IDENTITY
--  Audit fields:
--    created_at  TIMESTAMPTZ  – thời điểm tạo bản ghi
--    created_by  BIGINT       – id của user/actor thực hiện tạo
--    updated_at  TIMESTAMPTZ  – thời điểm cập nhật gần nhất
--    updated_by  BIGINT       – id của user/actor thực hiện cập nhật
-- ============================================================

-- ============================================================
--  ROLE
--  Master data vai trò người dùng
-- ============================================================
CREATE TABLE roles
(
    id          BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    -- audit
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  BIGINT,
    CONSTRAINT pk_roles PRIMARY KEY (id)
);

-- ============================================================
--  SCHOOL
--  Master data trường / khoa trực thuộc
-- ============================================================
CREATE TABLE schools
(
    id          BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    -- audit
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  BIGINT,
    CONSTRAINT pk_schools PRIMARY KEY (id)
);

-- ============================================================
--  MAJOR
--  Master data chuyên ngành học
-- ============================================================
CREATE TABLE majors
(
    id         BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    code       VARCHAR(50)  NOT NULL UNIQUE,
    name       VARCHAR(255) NOT NULL UNIQUE,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    school_id  BIGINT,
    -- audit
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    CONSTRAINT pk_majors PRIMARY KEY (id)
);

-- ============================================================
--  ASSOCIATION
--  Đoàn / Hội / Tổ chức quản trị
-- ============================================================
CREATE TABLE associations
(
    id          BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name        VARCHAR(255) NOT NULL,
    address     TEXT,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    -- audit
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  BIGINT,
    CONSTRAINT pk_associations PRIMARY KEY (id)
);

-- ============================================================
--  USER
--  Thực thể tài khoản người dùng
-- ============================================================
CREATE TABLE users
(
    id          BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255),
    provider_id VARCHAR(255),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    role_id     BIGINT       NOT NULL,
    -- audit
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  BIGINT,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

-- ============================================================
--  STUDENT_PROFILE
--  Hồ sơ thông tin sinh viên
-- ============================================================
CREATE TABLE student_profiles
(
    id             BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    first_name     VARCHAR(100),
    last_name      VARCHAR(100),
    student_id     VARCHAR(50) UNIQUE,
    gender         VARCHAR(10),
    k_number       INTEGER,
    avatar_url     TEXT,
    association_id BIGINT,
    major_id       BIGINT,
    user_id        BIGINT      NOT NULL UNIQUE,
    --audit
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by     BIGINT,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by     BIGINT,
    CONSTRAINT pk_student_profiles PRIMARY KEY (id)
);
-- ============================================================
--  NOTIFICATION
--  Thông báo cá nhân cho người dùng
-- ============================================================
CREATE TABLE notifications
(
    id         BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    title      VARCHAR(255),
    message    TEXT,
    status     VARCHAR(50) NOT NULL DEFAULT 'UNREAD',
    icon_url   TEXT,
    url        TEXT,
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    user_id    BIGINT      NOT NULL,
    -- audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    CONSTRAINT pk_notifications PRIMARY KEY (id)
);

-- ============================================================
--  FANPAGE
--  Trang / Ban tổ chức sự kiện
-- ============================================================
CREATE TABLE fanpages
(
    id          BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    name        VARCHAR(255),
    description TEXT,
    avatar_url  TEXT,
    status      VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',--'SPENDING','ACTIVE','BANNED'
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    -- audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by  BIGINT,
    CONSTRAINT pk_fanpages PRIMARY KEY (id)
);

-- ============================================================
--  POINT_CATEGORY
--  Mục điểm rèn luyện (Cấu trúc phân cấp cây)
-- ============================================================
CREATE TABLE point_categories
(
    id                 BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name               VARCHAR(255) NOT NULL,
    description        TEXT,
    maximum            INTEGER      NOT NULL DEFAULT 0,
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    date_apply         DATE,
    parent_category_id BIGINT,
    -- audit
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by         BIGINT,
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by         BIGINT,
    CONSTRAINT pk_point_categories PRIMARY KEY (id)
);

-- ============================================================
--  SEMESTER
--  Học kỳ niên khóa
-- ============================================================
CREATE TABLE semesters
(
    id            BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    number        INTEGER     NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    start_date    DATE,
    end_date      DATE,
    status        VARCHAR(50) NOT NULL DEFAULT 'UPCOMING',-- 'UPCOMING', 'ONGOING', 'COMPLETED'
    is_current    BOOLEAN     NOT NULL DEFAULT FALSE,
    -- audit
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by    BIGINT,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by    BIGINT,
    CONSTRAINT pk_semesters PRIMARY KEY (id)
);

-- ============================================================
--  STUDENT_SEMESTER_POINT
--  Tổng kết điểm rèn luyện theo từng kỳ và mục điểm
-- ============================================================
CREATE TABLE student_semester_points
(
    user_id           BIGINT         NOT NULL,
    semester_id       BIGINT         NOT NULL,
    point_category_id BIGINT         NOT NULL,
    total_point       NUMERIC(10, 2) NOT NULL DEFAULT 0,
    finalized_at      TIMESTAMPTZ,
    -- audit
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by        BIGINT,
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_by        BIGINT,
    CONSTRAINT pk_student_semester_points PRIMARY KEY (user_id, semester_id, point_category_id)
);

-- ============================================================
--  CRITERIA
--  Tiêu chí Sinh viên 5 Tốt
-- ============================================================
CREATE TABLE criterias
(
    id          BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    scope       TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    -- audit
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  BIGINT,
    CONSTRAINT pk_criterias PRIMARY KEY (id)
);

-- ============================================================
--  EVENT_TYPE
--  Phân loại sự kiện
-- ============================================================
CREATE TABLE event_types
(
    id          BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    -- audit
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  BIGINT,
    CONSTRAINT pk_event_types PRIMARY KEY (id)
);

-- ============================================================
--  EVENT
--  Sự kiện tổ chức
-- ============================================================
CREATE TABLE events
(
    id                  BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    date_open           TIMESTAMPTZ,
    date_close          TIMESTAMPTZ,
    date_happen         TIMESTAMPTZ,
    capacity            INTEGER               DEFAULT 0,
    male_quantity       INTEGER               DEFAULT 0,
    female_quantity     INTEGER               DEFAULT 0,
    banner_url          TEXT,
    ai_screening_result TEXT,
    ai_screening_score  NUMERIC(5, 2)         DEFAULT 0.00,
    status              VARCHAR(50)  NOT NULL DEFAULT 'DRAFT',--'DRAFT','PUBLISHED','CLOSED','CANCELLED'
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    event_type_id       BIGINT,
    criteria_id         BIGINT,
    fanpage_id          BIGINT,
    -- audit
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by          BIGINT,
    CONSTRAINT pk_events PRIMARY KEY (id)
);

-- ============================================================
--  EVENT_POINT
--  Thiết lập điểm rèn luyện cho sự kiện
-- ============================================================
CREATE TABLE event_points
(
    event_id          BIGINT      NOT NULL,
    point_category_id BIGINT      NOT NULL,
    point             INTEGER     NOT NULL DEFAULT 0,
    -- audit
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by        BIGINT,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by        BIGINT,
    CONSTRAINT pk_event_points PRIMARY KEY (event_id, point_category_id)
);

-- ============================================================
--  REGISTRATION
--  Đăng ký tham gia sự kiện và check-in minh chứng
-- ============================================================
CREATE TABLE registrations
(
    user_id      BIGINT      NOT NULL,
    event_id     BIGINT      NOT NULL,
    reg_date     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    check_in_at  TIMESTAMPTZ,
    check_out_at TIMESTAMPTZ,
    evidence_url TEXT,
    status       VARCHAR(50) NOT NULL DEFAULT 'REGISTERED',--'REGISTERED','CHECKED_IN','CHECKED_OUT','CANCELLED','ABSENTED'
    reason       TEXT,
    -- audit
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   BIGINT,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by   BIGINT,
    CONSTRAINT pk_registrations PRIMARY KEY (user_id, event_id)
);

-- ============================================================
--  CONVERSATION
--  Hội thoại trao đổi
-- ============================================================
CREATE TABLE conversations
(
    id         BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id    BIGINT      NOT NULL,
    fanpage_id BIGINT      NOT NULL,
    -- audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   BIGINT,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by   BIGINT,
    CONSTRAINT pk_conversations PRIMARY KEY (id)
);

-- ============================================================
--  MESSAGE
--  Tin nhắn chi tiết
-- ============================================================
CREATE TABLE messages
(
    id              BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    sender_type     VARCHAR(50),
    sender_id       BIGINT,
    content         TEXT,
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status          VARCHAR(50) NOT NULL DEFAULT 'SENT',--'SENT','DELIVERED','READ'
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    conversation_id BIGINT      NOT NULL,
    -- audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   BIGINT,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by      BIGINT,
    CONSTRAINT pk_messages PRIMARY KEY (id)
);

-- ============================================================
--  REPORT
--  Khiếu nại / Báo cáo sự kiện hoặc người dùng
-- ============================================================
CREATE TABLE reports
(
    id         BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    subject    VARCHAR(255),
    message    TEXT,
    status     VARCHAR(50) NOT NULL DEFAULT 'OPEN', --  'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    user_id    BIGINT      NOT NULL,
    fanpage_id BIGINT      NOT NULL,
    -- audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    CONSTRAINT pk_reports PRIMARY KEY (id)
);

-- ============================================================
--  GROUP
-- --  Nhóm cộng đồng
-- -- ============================================================
-- CREATE TABLE groups
-- (
--     id          BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
--     name        VARCHAR(255) NOT NULL,
--     description TEXT,
--     banner      TEXT,
--     is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
--     fanpage_id  BIGINT       NOT NULL,
--     -- audit
--     created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
--     created_by  BIGINT,
--     updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
--     updated_by  BIGINT,
--     CONSTRAINT pk_groups PRIMARY KEY (id)
-- );
--
-- -- ============================================================
-- --  JOINING
-- --  Thành viên tham gia nhóm cộng đồng
-- -- ============================================================
-- CREATE TABLE joinings
-- (
--     user_id    BIGINT      NOT NULL,
--     group_id   BIGINT      NOT NULL,
--     join_date  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
--     -- audit
--     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
--     created_by BIGINT,
--     CONSTRAINT pk_joinings PRIMARY KEY (user_id, group_id)
-- );

-- ============================================================
--  FOREIGN KEY CONSTRAINTS
-- ============================================================

ALTER TABLE majors
    ADD CONSTRAINT fk_majors_schools
        FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE SET NULL;

ALTER TABLE users
    ADD CONSTRAINT fk_users_roles
        FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT;

ALTER TABLE student_profiles
    ADD CONSTRAINT fk_student_profiles_users
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE student_profiles
    ADD CONSTRAINT fk_student_profiles_majors
        FOREIGN KEY (major_id) REFERENCES majors (id) ON DELETE SET NULL;

ALTER TABLE student_profiles
    ADD CONSTRAINT fk_student_profiles_associations
        FOREIGN KEY (association_id) REFERENCES associations (id) ON DELETE SET NULL;

ALTER TABLE events
    ADD CONSTRAINT fk_events_event_types
        FOREIGN KEY (event_type_id) REFERENCES event_types (id) ON DELETE SET NULL;

ALTER TABLE events
    ADD CONSTRAINT fk_events_criterias
        FOREIGN KEY (criteria_id) REFERENCES criterias (id) ON DELETE SET NULL;

ALTER TABLE events
    ADD CONSTRAINT fk_events_fanpages
        FOREIGN KEY (fanpage_id) REFERENCES fanpages (id) ON DELETE RESTRICT;

ALTER TABLE point_categories
    ADD CONSTRAINT fk_point_categories_parent
        FOREIGN KEY (parent_category_id) REFERENCES point_categories (id) ON DELETE SET NULL;

ALTER TABLE event_points
    ADD CONSTRAINT fk_event_points_events
        FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE;

ALTER TABLE event_points
    ADD CONSTRAINT fk_event_points_categories
        FOREIGN KEY (point_category_id) REFERENCES point_categories (id) ON DELETE RESTRICT;

ALTER TABLE conversations
    ADD CONSTRAINT fk_conversations_users
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE conversations
    ADD CONSTRAINT fk_conversations_fanpages
        FOREIGN KEY (fanpage_id) REFERENCES fanpages (id) ON DELETE CASCADE;

ALTER TABLE messages
    ADD CONSTRAINT fk_messages_conversations
        FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE;

ALTER TABLE registrations
    ADD CONSTRAINT fk_registrations_users
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE registrations
    ADD CONSTRAINT fk_registrations_events
        FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE;

ALTER TABLE student_semester_points
    ADD CONSTRAINT fk_ssp_users
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE student_semester_points
    ADD CONSTRAINT fk_ssp_semesters
        FOREIGN KEY (semester_id) REFERENCES semesters (id) ON DELETE RESTRICT;

ALTER TABLE student_semester_points
    ADD CONSTRAINT fk_ssp_point_categories
        FOREIGN KEY (point_category_id) REFERENCES point_categories (id) ON DELETE RESTRICT;

ALTER TABLE reports
    ADD CONSTRAINT fk_reports_users
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE reports
    ADD CONSTRAINT fk_reports_fanpages
        FOREIGN KEY (fanpage_id) REFERENCES fanpages (id) ON DELETE SET NULL;

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_users
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

-- ALTER TABLE groups
--     ADD CONSTRAINT fk_groups_fanpages
--         FOREIGN KEY (fanpage_id) REFERENCES fanpages (id) ON DELETE RESTRICT;
-- ALTER TABLE joinings
--     ADD CONSTRAINT fk_joinings_users
--         FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
--
-- ALTER TABLE joinings
--     ADD CONSTRAINT fk_joinings_groups
--         FOREIGN KEY (group_id) REFERENCES groups (id) ON DELETE CASCADE;

-- ============================================================
--  AUTO-UPDATE updated_at VIA TRIGGER
-- ============================================================
CREATE OR REPLACE FUNCTION fn_set_updated_at()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_roles_updated_at
    BEFORE UPDATE
    ON roles
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_schools_updated_at
    BEFORE UPDATE
    ON schools
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_majors_updated_at
    BEFORE UPDATE
    ON majors
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE
    ON users
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_student_profiles_updated_at
    BEFORE UPDATE
    ON student_profiles
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_event_types_updated_at
    BEFORE UPDATE
    ON event_types
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_criterias_updated_at
    BEFORE UPDATE
    ON criterias
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_events_updated_at
    BEFORE UPDATE
    ON events
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_point_categories_updated_at
    BEFORE UPDATE
    ON point_categories
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_event_points_updated_at
    BEFORE UPDATE
    ON event_points
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_associations_updated_at
    BEFORE UPDATE
    ON associations
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_fanpages_updated_at
    BEFORE UPDATE
    ON fanpages
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_groups_updated_at
    BEFORE UPDATE
    ON groups
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_messages_updated_at
    BEFORE UPDATE
    ON messages
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_registrations_updated_at
    BEFORE UPDATE
    ON registrations
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_semesters_updated_at
    BEFORE UPDATE
    ON semesters
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_ssp_updated_at
    BEFORE UPDATE
    ON student_semester_points
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_reports_updated_at
    BEFORE UPDATE
    ON reports
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_notifications_updated_at
    BEFORE UPDATE
    ON notifications
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();

-- ============================================================
--  INDEXES
-- ============================================================
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role_id ON users (role_id);
CREATE INDEX idx_student_profiles_user_id ON student_profiles (user_id);
CREATE INDEX idx_student_profiles_major_id ON student_profiles (major_id);
CREATE INDEX idx_events_status ON events (status);
CREATE INDEX idx_events_date_happen ON events (date_happen);
CREATE INDEX idx_events_type_id ON events (event_type_id);
CREATE INDEX idx_registrations_event_id ON registrations (event_id);
CREATE INDEX idx_registrations_status ON registrations (status);
CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_status ON notifications (status);
CREATE INDEX idx_messages_conversation ON messages (conversation_id);
CREATE INDEX idx_semesters_academic_year ON semesters (academic_year);
CREATE INDEX idx_pct_parent ON point_categories (parent_category_id);
CREATE INDEX idx_events_created_by ON events (created_by);
CREATE INDEX idx_events_created_at ON events (created_at);
CREATE INDEX idx_registrations_updated_by ON registrations (updated_by);
CREATE INDEX idx_ssp_updated_by ON student_semester_points (updated_by);