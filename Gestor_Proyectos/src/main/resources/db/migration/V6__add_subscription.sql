-- V6: Stripe subscription + search quota
-- plan: 'free' | 'pro'
-- search_count / search_reset_at: rolling monthly quota for free users

ALTER TABLE user
    ADD COLUMN plan                   VARCHAR(10)  NOT NULL DEFAULT 'free',
    ADD COLUMN stripe_customer_id     VARCHAR(100) DEFAULT NULL,
    ADD COLUMN stripe_subscription_id VARCHAR(100) DEFAULT NULL,
    ADD COLUMN search_count           INT          NOT NULL DEFAULT 0,
    ADD COLUMN search_reset_at        DATE         DEFAULT NULL;
