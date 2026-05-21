-- V4: Add contact info and LinkedIn URL to saved_candidate.
-- email / blog come from the developer's public GitHub profile.
-- linkedin_url is filled in manually by the recruiter.

ALTER TABLE saved_candidate
    ADD COLUMN email        VARCHAR(255),
    ADD COLUMN blog         VARCHAR(512),
    ADD COLUMN linkedin_url VARCHAR(512);
