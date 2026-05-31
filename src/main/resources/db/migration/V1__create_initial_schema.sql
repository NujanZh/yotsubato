CREATE TABLE users
(
    id            UUID         NOT NULL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url    VARCHAR(2083),
    status        VARCHAR(20)  NOT NULL DEFAULT 'OFFLINE',
    last_seen     DATETIME(3),
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_users_status CHECK (status IN ('ONLINE', 'OFFLINE', 'AWAY')),
    CONSTRAINT uniq_users_username UNIQUE (username),
    CONSTRAINT uniq_users_email UNIQUE (email)
);

CREATE TABLE rooms
(
    id          UUID        NOT NULL PRIMARY KEY,
    name        VARCHAR(100),
    type        VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    created_by  UUID        NOT NULL,
    description VARCHAR(255),
    dm_key      VARCHAR(73),
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_rooms_type CHECK (type IN ('PUBLIC', 'PRIVATE', 'DIRECT')),
    CONSTRAINT uniq_rooms_dm_key UNIQUE (dm_key),
    CONSTRAINT chk_rooms_dm_key CHECK ((type = 'DIRECT' AND dm_key IS NOT NULL) OR
                                       (type <> 'DIRECT' AND dm_key IS NULL)),
    CONSTRAINT fk_rooms_created_by
        FOREIGN KEY (created_by)
            REFERENCES users (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE
);

CREATE TABLE join_requests
(
    id               UUID PRIMARY KEY,
    room_id          UUID         NOT NULL,
    user_id          UUID         NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    requested_at     TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    reviewed_at      TIMESTAMP(3),
    reviewed_by      UUID,
    rejection_reason VARCHAR(255),
    pending_user_id  UUID GENERATED ALWAYS AS (IF(status = 'PENDING', user_id, NULL)) STORED,
    CONSTRAINT uniq_join_requests_room_id_pending_user_id UNIQUE (room_id, pending_user_id),
    CONSTRAINT chk_join_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT fk_join_requests_room_id FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_join_requests_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_join_requests_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id) ON DELETE SET NULL
);

CREATE TABLE room_members
(
    id        UUID        NOT NULL PRIMARY KEY,
    room_id   UUID        NOT NULL,
    user_id   UUID        NOT NULL,
    role      VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_room_members_role CHECK (role IN ('ADMIN', 'MEMBER')),
    CONSTRAINT uniq_room_members_room_id_user_id UNIQUE (room_id, user_id),
    CONSTRAINT fk_room_member_room_id
        FOREIGN KEY (room_id)
            REFERENCES rooms (id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,
    CONSTRAINT fk_room_member_user_id
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE
);

CREATE TABLE messages
(
    id        UUID        NOT NULL PRIMARY KEY,
    room_id   UUID        NOT NULL,
    sender_id UUID        NOT NULL,
    reply_to  UUID,
    content   TEXT,
    type      VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    edited_at DATETIME(3),
    deleted   BOOLEAN     NOT NULL DEFAULT FALSE,
    sent_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_messages_type CHECK (type IN ('TEXT', 'IMAGE', 'SYSTEM', 'FILE')),
    CONSTRAINT fk_messages_room_id
        FOREIGN KEY (room_id)
            REFERENCES rooms (id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,
    CONSTRAINT fk_messages_sender_id
        FOREIGN KEY (sender_id)
            REFERENCES users (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE,
    CONSTRAINT fk_messages_reply_to
        FOREIGN KEY (reply_to)
            REFERENCES messages (id)
            ON UPDATE CASCADE
);

CREATE TABLE message_status
(
    id         UUID        NOT NULL PRIMARY KEY,
    message_id UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'SENT',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_message_status_status CHECK (status IN ('READ', 'SENT', 'DELIVERED')),
    CONSTRAINT uniq_message_status_message_id_user_id UNIQUE (message_id, user_id),
    CONSTRAINT fk_message_status_user_id
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,
    CONSTRAINT fk_message_status_message_id
        FOREIGN KEY (message_id)
            REFERENCES messages (id)
            ON DELETE CASCADE
            ON UPDATE CASCADE
);

CREATE TABLE attachments
(
    id         UUID          NOT NULL PRIMARY KEY,
    message_id UUID          NOT NULL,
    file_name  VARCHAR(255)  NOT NULL,
    file_url   VARCHAR(2083) NOT NULL,
    file_type  VARCHAR(20)   NOT NULL,
    file_size  BIGINT        NOT NULL,
    created_at DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_attachments_message_id
        FOREIGN KEY (message_id)
            REFERENCES messages (id)
            ON DELETE CASCADE
            ON UPDATE CASCADE
);

CREATE INDEX idx_room_members_room_user ON room_members (room_id, user_id);
CREATE INDEX idx_messages_room_sent ON messages (room_id, sent_at);
