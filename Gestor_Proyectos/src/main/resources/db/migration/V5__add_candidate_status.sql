-- V5: Candidate pipeline status
-- Tracks where each candidate is in the recruiter's workflow.
-- Values: saved | contacted | replied | interviewing | offered | rejected

ALTER TABLE saved_candidate
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'saved';
