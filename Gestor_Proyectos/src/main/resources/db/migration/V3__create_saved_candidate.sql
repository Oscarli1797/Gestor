-- V3: Saved candidates — links a recruiter (User) to a Developer snapshot.
-- developer_id uses the same "platform:username" format as developer.id
-- but is NOT a strict FK so that candidates can be saved without a full
-- Developer row (e.g. for GitLab/SO/Bitbucket developers not yet persisted).

CREATE TABLE IF NOT EXISTS saved_candidate (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    recruiter_id BIGINT       NOT NULL,
    developer_id VARCHAR(255) NOT NULL,

    -- Snapshot fields (denormalized for offline access)
    platform     VARCHAR(50),
    username     VARCHAR(255),
    display_name VARCHAR(255),
    avatar_url   VARCHAR(512),
    profile_url  VARCHAR(512),
    location     VARCHAR(255),
    score        INT,
    score_tier   VARCHAR(20),

    -- Recruiter metadata
    notes    TEXT,
    saved_at DATETIME NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_recruiter_developer UNIQUE (recruiter_id, developer_id),
    CONSTRAINT fk_saved_candidate_user
        FOREIGN KEY (recruiter_id) REFERENCES `user` (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
