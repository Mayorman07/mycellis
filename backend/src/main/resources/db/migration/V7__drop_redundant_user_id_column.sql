-- Drop redundant user_id column from users table.
-- The 'id' column (real UUID primary key) is the canonical user identifier.
-- This removes a legacy field from when modules were built in different orders.

ALTER TABLE users
DROP COLUMN user_id;

