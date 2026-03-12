--liquibase formatted sql
--changeset owner-pratik:20260305183100_alter_refresh_token_column_to_text
--comment: Change token column from VARCHAR(512) to TEXT to accommodate JWT tokens

ALTER TABLE refresh_tokens ALTER COLUMN token TYPE TEXT;
