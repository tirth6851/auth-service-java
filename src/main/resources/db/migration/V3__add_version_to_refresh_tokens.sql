-- Optimistic-locking version column for refresh_tokens (JPA @Version).
-- Prevents two concurrent refreshes of the same token from both rotating it.
-- Existing rows default to 0; Hibernate manages the value on subsequent updates.
ALTER TABLE refresh_tokens ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
