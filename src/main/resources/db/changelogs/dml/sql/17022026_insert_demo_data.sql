--liquibase formatted sql
--changeset owner:17022026_insert_demo_data
--comment: Insert initial sample demo data (DML)
INSERT INTO demo (first_name, last_name)
VALUES ('John', 'Doe');

--rollback: DELETE FROM demo WHERE first_name='John' AND last_name='Doe';
