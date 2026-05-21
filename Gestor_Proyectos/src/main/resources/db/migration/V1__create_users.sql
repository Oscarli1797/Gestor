-- V1: User authentication tables
-- Uses IF NOT EXISTS so this migration is safe to run against an existing database
-- that was previously managed by Hibernate ddl-auto=update.

CREATE TABLE IF NOT EXISTS `user` (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uq_user_name  UNIQUE (name),
    CONSTRAINT uq_user_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- @ElementCollection for User.roles
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT      NOT NULL,
    roles   VARCHAR(255),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES `user` (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
