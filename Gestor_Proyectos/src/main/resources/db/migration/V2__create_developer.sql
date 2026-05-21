-- V2: Developer profile table
-- Persisted when a recruiter saves a candidate (Step 4).
-- The composite String PK uses the format "platform:username" (e.g. "github:torvalds").
-- Column names follow Spring's SpringPhysicalNamingStrategy (camelCase → snake_case).

CREATE TABLE IF NOT EXISTS developer (
    -- Identity
    id           VARCHAR(255) NOT NULL,
    platform     VARCHAR(50)  NOT NULL,
    username     VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    avatar_url   VARCHAR(512),
    profile_url  VARCHAR(512),
    bio          TEXT,
    location     VARCHAR(255),
    company      VARCHAR(255),
    joined_at    VARCHAR(20),

    -- Engagement
    followers          INT,
    following          INT,
    total_public_repos INT,
    owned_repos        INT,
    total_stars        INT,
    total_forks        INT,

    -- Activity (GitHub GraphQL, Step 2)
    total_commits_last_year INT,
    last90_days_activity    INT,   -- explicit column name: digit before uppercase breaks default
    last30_days_activity    INT,
    total_pull_requests     INT,
    total_issues            INT,
    total_pr_reviews        INT,

    -- Languages (JSON array, Step 2)
    top_languages_json TEXT,

    -- Scoring (Step 3)
    score        INT,
    score_recent INT,
    score_impact INT,
    score_contrib INT,
    score_collab INT,
    score_tech   INT,
    score_tier   VARCHAR(20),

    fetched_at DATETIME,

    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
