-- ============================================================
--  1. ROLE
-- ============================================================
CREATE TABLE roles
(
    id          BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  BIGINT,
    CONSTRAINT pk_roles PRIMARY KEY (id)
);

-- ============================================================
--  2. SCHOOL
-- ============================================================
CREATE TABLE schools
(
    id          BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  BIGINT,
    CONSTRAINT pk_schools PRIMARY KEY (id)
);

-- ============================================================
--  3. MAJOR
-- ============================================================
CREATE TABLE majors
(
    id         BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    code       VARCHAR(50)  NOT NULL UNIQUE,
    name       VARCHAR(255) NOT NULL UNIQUE,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    school_id  BIGINT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    CONSTRAINT pk_majors PRIMARY KEY (id)
);

-- ============================================================
--  4. ASSOCIATION
-- ============================================================
CREATE TABLE associations
(
    id          BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name        VARCHAR(255) NOT NULL,
    address     VARCHAR(255),
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  BIGINT,
    CONSTRAINT pk_associations PRIMARY KEY (id)
);

-- ============================================================
--  5. USER
-- ============================================================
CREATE TABLE users
(
    id          BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255),
    provider_id VARCHAR(255)          DEFAULT '',
    first_name  VARCHAR(100),
    last_name   VARCHAR(100),
    gender      VARCHAR(10)           DEFAULT 'OTHER',
    avatar_url  TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  BIGINT,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

-- ============================================================
--  6. USER_ROLE (Many-to-Many giữa User và Role)
-- ============================================================
CREATE TABLE user_roles
(
    user_id    BIGINT      NOT NULL,
    role_id    BIGINT      NOT NULL,
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id)
);

-- ============================================================
--  7. STUDENT_PROFILE
-- ============================================================
CREATE TABLE student_profiles
(
    id         BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    student_id VARCHAR(50) UNIQUE,
    k_number   INTEGER,
    major_id   BIGINT,
    user_id    BIGINT      NOT NULL UNIQUE,
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    CONSTRAINT pk_student_profiles PRIMARY KEY (id)
);

-- ============================================================
--  8. FANPAGE_ADMIN_PROFILE
-- ============================================================
CREATE TABLE fanpage_admin_profiles
(
    id           BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    staff_id     VARCHAR(20) NOT NULL,
    org_name     VARCHAR(20),
    title        VARCHAR(100),
    dob          DATE,
    phone_number VARCHAR(20),
    user_id      BIGINT      NOT NULL UNIQUE,
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   BIGINT,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by   BIGINT,
    CONSTRAINT pk_fanpage_admin_profiles PRIMARY KEY (id)
);

-- ============================================================
--  9. FANPAGE
-- ============================================================
CREATE TABLE fanpages
(
    id           BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name         VARCHAR(255) NOT NULL,
    description  TEXT,
    avatar_url   TEXT,
    status       VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    org_type     VARCHAR(100) NOT NULL,
    org_name     VARCHAR(200) NOT NULL,
    parent_org   VARCHAR(255) NOT NULL,
    help_contact VARCHAR(255),
    help_phone   VARCHAR(20),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by   BIGINT,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by   BIGINT,
    CONSTRAINT pk_fanpages PRIMARY KEY (id)
);

-- ============================================================
--  10. FANPAGE_MEMBER
-- ============================================================
CREATE TABLE fanpage_members
(
    fanpage_id BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    role       VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    CONSTRAINT pk_fanpage_members PRIMARY KEY (fanpage_id, user_id)
);

-- ============================================================
--  10.1 FOLLOWING
-- ============================================================
CREATE TABLE followings
(
    id         BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    fanpage_id BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    fol_status VARCHAR(50) NOT NULL DEFAULT 'FOLLOWING',
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    CONSTRAINT pk_followings PRIMARY KEY (id),
    CONSTRAINT uq_followings_fanpage_user UNIQUE (fanpage_id, user_id)
);

-- ============================================================
--  11. EVENT_TYPE
-- ============================================================
CREATE TABLE event_types
(
    id          BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  BIGINT,
    CONSTRAINT pk_event_types PRIMARY KEY (id)
);

-- ============================================================
--  12. CRITERIA (Tiêu chí Sinh viên 5 Tốt)
-- ============================================================
CREATE TABLE criterias
(
    id                BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name              VARCHAR(255) NOT NULL,
    note              TEXT,
    scope             TEXT,
    level             INTEGER      NOT NULL DEFAULT 0,
    children_order    INTEGER      NOT NULL DEFAULT 1,
    required_children INTEGER,
    parent_id         BIGINT,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by        BIGINT,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by        BIGINT,
    CONSTRAINT pk_criterias PRIMARY KEY (id),
    CONSTRAINT ck_criterias_required_children
        CHECK (required_children IS NULL OR required_children > 0)
);

-- ============================================================
--  13. POINT_CATEGORY (Mục điểm rèn luyện)
-- ============================================================
CREATE TABLE point_categories
(
    id                 BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name               VARCHAR(255) NOT NULL,
    note               TEXT,
    maximum            INTEGER      NOT NULL DEFAULT 0,
    date_apply         DATE         NOT NULL DEFAULT CURRENT_DATE,
    level              INTEGER      NOT NULL DEFAULT 0,
    children_order     INTEGER      NOT NULL DEFAULT 1,
    parent_category_id BIGINT,
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by         BIGINT,
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by         BIGINT,
    CONSTRAINT pk_point_categories PRIMARY KEY (id)
);

-- ============================================================
--  14. ACADEMIC_YEAR (Năm học)
-- ============================================================
CREATE TABLE academic_years
(
    id         BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    code       VARCHAR(20) NOT NULL UNIQUE,
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    CONSTRAINT pk_academic_years PRIMARY KEY (id),
    CONSTRAINT ck_academic_year_dates CHECK (start_date <= end_date)
);

-- ============================================================
--  15. SEMESTER (Học kỳ)
-- ============================================================
CREATE TABLE semesters
(
    id               BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    number           INTEGER     NOT NULL,
    academic_year_id BIGINT,
    start_date       DATE        NOT NULL,
    end_date         DATE        NOT NULL,
    status           VARCHAR(50) NOT NULL DEFAULT 'ONGOING',
    is_active        BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by       BIGINT,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by       BIGINT,
    CONSTRAINT pk_semesters PRIMARY KEY (id),
    CONSTRAINT uq_semester_number_per_year UNIQUE (academic_year_id, number)
);

-- ============================================================
--  16. EVENT (Sự kiện)
-- ============================================================
CREATE TABLE events
(
    id              BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name            VARCHAR(255) NOT NULL,
    description     TEXT         NOT NULL,
    location        TEXT         NOT NULL,
    date_open       TIMESTAMPTZ  NOT NULL,
    date_close      TIMESTAMPTZ  NOT NULL,
    date_happen     TIMESTAMPTZ  NOT NULL,
    capacity        INTEGER      NOT NULL DEFAULT 0,
    male_quantity   INTEGER               DEFAULT 0,
    female_quantity INTEGER               DEFAULT 0,
    banner_url      TEXT         NOT NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'DRAFT',
    event_type_id   BIGINT,
    criteria_id     BIGINT,
    fanpage_id      BIGINT,
    semester_id     BIGINT,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by      BIGINT,
    CONSTRAINT pk_events PRIMARY KEY (id)
);

-- ============================================================
--  17. EVENT_POINT
-- ============================================================
CREATE TABLE event_points
(
    id                BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    event_id          BIGINT      NOT NULL,
    point_category_id BIGINT      NOT NULL,
    point             INTEGER     NOT NULL DEFAULT 0,
    is_active         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by        BIGINT,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by        BIGINT,
    CONSTRAINT pk_event_points PRIMARY KEY (id)
);

-- ============================================================
--  18. REGISTRATION
-- ============================================================
CREATE TABLE registrations
(
    user_id                BIGINT      NOT NULL,
    event_id               BIGINT      NOT NULL,
    reg_date               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    check_in_at            TIMESTAMPTZ,
    check_in_evidence_url  TEXT,
    check_in_by            BIGINT,
    check_out_at           TIMESTAMPTZ,
    check_out_evidence_url TEXT,
    check_out_by           BIGINT,
    status                 VARCHAR(50) NOT NULL DEFAULT 'REGISTERED',
    reason                 TEXT,
    created_at             TIMESTAMPTZ          DEFAULT NOW(),
    created_by             BIGINT,
    updated_at             TIMESTAMPTZ          DEFAULT NOW(),
    updated_by             BIGINT,
    CONSTRAINT pk_registrations PRIMARY KEY (user_id, event_id)
);

-- ============================================================
--  19. STUDENT_CRITERIA_PROGRESS
-- ============================================================
CREATE TABLE student_criteria_progress
(
    id                BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id           BIGINT      NOT NULL,
    criteria_id       BIGINT      NOT NULL,
    academic_year_id  BIGINT      NOT NULL,
    event_id          BIGINT,
    is_completed      BOOLEAN     NOT NULL DEFAULT FALSE,
    completion_source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    completed_at      TIMESTAMPTZ,
    is_active         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by        BIGINT,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by        BIGINT,
    CONSTRAINT pk_student_criteria_progress PRIMARY KEY (id),
    CONSTRAINT uq_student_criteria_progress UNIQUE (user_id, criteria_id, academic_year_id)
);

ALTER TABLE student_criteria_progress
    DROP COLUMN completed_at,
    DROP COLUMN is_completed,
    ADD COLUMN note TEXT default '';

-- ============================================================
--  20. POINT_TRANSACTION
-- ============================================================
CREATE TABLE point_transactions
(
    id                BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id           BIGINT      NOT NULL,
    semester_id       BIGINT      NOT NULL,
    point_category_id BIGINT      NOT NULL,
    event_id          BIGINT,
    points            INTEGER     NOT NULL,
    type              VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    status            VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    description       TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by        BIGINT,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by        BIGINT,
    CONSTRAINT pk_point_transactions PRIMARY KEY (id)
);

-- ============================================================
--  21. STUDENT_SEMESTER_POINT
-- ============================================================
CREATE TABLE student_semester_points
(
    id                BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id           BIGINT      NOT NULL,
    semester_id       BIGINT      NOT NULL,
    point_category_id BIGINT      NOT NULL,
    total_point       INTEGER     NOT NULL DEFAULT 0,
    finalized_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by        BIGINT,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by        BIGINT,
    CONSTRAINT pk_student_semester_points PRIMARY KEY (id),
    CONSTRAINT uq_student_semester_points UNIQUE (user_id, semester_id, point_category_id)
);

-- ============================================================
--  22. REFRESH_TOKEN
-- ============================================================
CREATE TABLE refresh_tokens
(
    id         BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    token      VARCHAR(64) NOT NULL UNIQUE,
    user_id    BIGINT      NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id)
);

-- ============================================================
--  23. CONVERSATION
-- ============================================================
CREATE TABLE conversations
(
    id         BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id    BIGINT      NOT NULL,
    fanpage_id BIGINT      NOT NULL,
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    CONSTRAINT pk_conversations PRIMARY KEY (id)
);

-- ============================================================
--  24. MESSAGE
-- ============================================================
CREATE TABLE messages
(
    id              BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    conversation_id BIGINT      NOT NULL,
    sender_type     VARCHAR(50) NOT NULL,
    sender_id       BIGINT      NOT NULL,
    content         TEXT        NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'SENT',
    read_at         TIMESTAMPTZ,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by      BIGINT,
    CONSTRAINT pk_messages PRIMARY KEY (id)
);

-- ============================================================
--  25. NOTIFICATION
-- ============================================================
CREATE TABLE notifications
(
    id         BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    title      VARCHAR(255),
    message    TEXT,
    status     VARCHAR(50) NOT NULL DEFAULT 'SENT',
    icon_url   TEXT,
    url        TEXT,
    user_id    BIGINT      NOT NULL,
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    CONSTRAINT pk_notifications PRIMARY KEY (id)
);

-- ============================================================
--  26. REPORT
-- ============================================================
CREATE TABLE reports
(
    id         BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    subject    VARCHAR(255),
    message    TEXT,
    status     VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    user_id    BIGINT      NOT NULL,
    fanpage_id BIGINT      NOT NULL,
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    CONSTRAINT pk_reports PRIMARY KEY (id)
);

-- ============================================================
--  FOREIGN KEY CONSTRAINTS
-- ============================================================

ALTER TABLE majors
    ADD CONSTRAINT fk_majors_schools
        FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE SET NULL;

ALTER TABLE user_roles
    ADD CONSTRAINT fk_user_roles_users
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_user_roles_roles
        FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT;

ALTER TABLE student_profiles
    ADD CONSTRAINT fk_student_profiles_users
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_student_profiles_majors
        FOREIGN KEY (major_id) REFERENCES majors (id) ON DELETE SET NULL;

ALTER TABLE fanpage_admin_profiles
    ADD CONSTRAINT fk_fanpage_admin_profiles_users
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE fanpage_members
    ADD CONSTRAINT fk_fanpage_members_fanpages
        FOREIGN KEY (fanpage_id) REFERENCES fanpages (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_fanpage_members_users
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE followings
    ADD CONSTRAINT fk_followings_fanpages
        FOREIGN KEY (fanpage_id) REFERENCES fanpages (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_followings_users
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE criterias
    ADD CONSTRAINT fk_criterias_parent
        FOREIGN KEY (parent_id) REFERENCES criterias (id) ON DELETE SET NULL;

ALTER TABLE semesters
    ADD CONSTRAINT fk_semesters_academic_year
        FOREIGN KEY (academic_year_id) REFERENCES academic_years (id) ON DELETE RESTRICT;

ALTER TABLE point_categories
    ADD CONSTRAINT fk_point_categories_parent
        FOREIGN KEY (parent_category_id) REFERENCES point_categories (id) ON DELETE SET NULL;

ALTER TABLE events
    ADD CONSTRAINT fk_events_event_types
        FOREIGN KEY (event_type_id) REFERENCES event_types (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_events_criterias
        FOREIGN KEY (criteria_id) REFERENCES criterias (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_events_fanpages
        FOREIGN KEY (fanpage_id) REFERENCES fanpages (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_events_semesters
        FOREIGN KEY (semester_id) REFERENCES semesters (id) ON DELETE RESTRICT;

ALTER TABLE event_points
    ADD CONSTRAINT fk_event_points_events
        FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_event_points_categories
        FOREIGN KEY (point_category_id) REFERENCES point_categories (id) ON DELETE RESTRICT;

ALTER TABLE registrations
    ADD CONSTRAINT fk_registrations_users
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_registrations_events
        FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE;

ALTER TABLE student_criteria_progress
    ADD CONSTRAINT fk_progress_event
        FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_progress_users
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_progress_criteria
        FOREIGN KEY (criteria_id) REFERENCES criterias (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_progress_academic_year
        FOREIGN KEY (academic_year_id) REFERENCES academic_years (id) ON DELETE RESTRICT;

ALTER TABLE point_transactions
    ADD CONSTRAINT fk_point_transactions_users
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_point_transactions_semesters
        FOREIGN KEY (semester_id) REFERENCES semesters (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_point_transactions_categories
        FOREIGN KEY (point_category_id) REFERENCES point_categories (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_point_transactions_events
        FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE SET NULL;

ALTER TABLE student_semester_points
    ADD CONSTRAINT fk_ssp_users
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_ssp_semesters
        FOREIGN KEY (semester_id) REFERENCES semesters (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_ssp_categories
        FOREIGN KEY (point_category_id) REFERENCES point_categories (id) ON DELETE RESTRICT;

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_users
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE conversations
    ADD CONSTRAINT fk_conversations_users
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_conversations_fanpages
        FOREIGN KEY (fanpage_id) REFERENCES fanpages (id) ON DELETE CASCADE;

ALTER TABLE messages
    ADD CONSTRAINT fk_messages_conversations
        FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE;

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_users
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE reports
    ADD CONSTRAINT fk_reports_users
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_reports_fanpages
        FOREIGN KEY (fanpage_id) REFERENCES fanpages (id) ON DELETE CASCADE;

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

CREATE TRIGGER trg_roles_updated_at BEFORE UPDATE ON roles FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_schools_updated_at BEFORE UPDATE ON schools FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_majors_updated_at BEFORE UPDATE ON majors FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_associations_updated_at BEFORE UPDATE ON associations FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_user_roles_updated_at BEFORE UPDATE ON user_roles FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_student_profiles_updated_at BEFORE UPDATE ON student_profiles FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_fanpage_admin_profiles_updated_at BEFORE UPDATE ON fanpage_admin_profiles FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_fanpages_updated_at BEFORE UPDATE ON fanpages FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_fanpage_members_updated_at BEFORE UPDATE ON fanpage_members FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_event_types_updated_at BEFORE UPDATE ON event_types FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_criterias_updated_at BEFORE UPDATE ON criterias FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_point_categories_updated_at BEFORE UPDATE ON point_categories FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_semesters_updated_at BEFORE UPDATE ON semesters FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_events_updated_at BEFORE UPDATE ON events FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_event_points_updated_at BEFORE UPDATE ON event_points FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_registrations_updated_at BEFORE UPDATE ON registrations FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_point_transactions_updated_at BEFORE UPDATE ON point_transactions FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_student_semester_points_updated_at BEFORE UPDATE ON student_semester_points FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_refresh_tokens_updated_at BEFORE UPDATE ON refresh_tokens FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_conversations_updated_at BEFORE UPDATE ON conversations FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_messages_updated_at BEFORE UPDATE ON messages FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_notifications_updated_at BEFORE UPDATE ON notifications FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_reports_updated_at BEFORE UPDATE ON reports FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_followings_updated_at BEFORE UPDATE ON followings FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- ============================================================
--  INDEXES
-- ============================================================
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_user_roles_user ON user_roles (user_id);
CREATE INDEX idx_user_roles_role ON user_roles (role_id);
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
CREATE INDEX idx_pct_parent ON point_categories (parent_category_id);
CREATE INDEX idx_criterias_parent ON criterias (parent_id);
CREATE INDEX idx_progress_user_year ON student_criteria_progress (user_id, academic_year_id);
CREATE INDEX idx_progress_criteria ON student_criteria_progress (criteria_id);
CREATE INDEX idx_point_transactions_user ON point_transactions (user_id, semester_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_followings_fanpage ON followings (fanpage_id);
CREATE INDEX idx_followings_user ON followings (user_id);
CREATE INDEX idx_followings_status ON followings (fol_status);
