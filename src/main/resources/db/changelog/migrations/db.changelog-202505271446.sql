--liquibase formatted sql
--changeset thiago:202408191938
--comment: boards table create

CREATE TABLE BOARDS(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    deleted BOOLEAN NOT NULL,
    deleted_at TIMESTAMP NULL
) ENGINE=InnoDB;

--rollback DROP TABLE BOARDS