ALTER TABLE rooms
    MODIFY name VARCHAR(100) NULL;

ALTER TABLE rooms
    DROP CONSTRAINT chk_rooms_type;

ALTER TABLE rooms
    ADD CONSTRAINT chk_rooms_type CHECK (type IN ('PUBLIC', 'PRIVATE', 'DIRECT'));