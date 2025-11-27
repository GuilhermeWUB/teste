-- Migration V10: Add active column to app_users table
-- This column allows users to be blocked/unblocked instead of deleted

ALTER TABLE app_users
ADD COLUMN active BOOLEAN NOT NULL DEFAULT true;

-- Create an index on active column for better query performance
CREATE INDEX idx_app_users_active ON app_users(active);

-- Create an index on role for better filtering
CREATE INDEX idx_app_users_role ON app_users(role);
