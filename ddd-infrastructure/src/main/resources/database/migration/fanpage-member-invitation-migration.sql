CREATE TABLE fanpage_member_invitations
(
    id         BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    fanpage_id BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    status     VARCHAR(20) NOT NULL, -- PENDING, ACCEPTED, DECLINE, CANCELLED
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    CONSTRAINT pk_fanpage_member_invitation PRIMARY KEY (id),
    CONSTRAINT fk_invitation_fanpage FOREIGN KEY (fanpage_id) REFERENCES fanpages (id) ON DELETE CASCADE,
    CONSTRAINT fk_invitation_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

ALTER TABLE fanpage_member_invitations
    ADD COLUMN inviter_name VARCHAR(255) NOT NULL;

CREATE TRIGGER trg_fanpage_member_invitation_updated_at
    BEFORE UPDATE
    ON fanpage_member_invitations
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();