-- ============================================================================
-- Auth redesign migration — see AUTHENTICATION_DESIGN.md sections 2 and 12.
--
-- spring.jpa.hibernate.ddl-auto=none, so nothing here applies automatically.
-- Run this by hand against `ev_charging_system` BEFORE deploying the new
-- backend code (the new code expects password_hash / email_verified / the
-- two new tables to already exist).
--
-- Run as, e.g.:
--   mysql -u root -p ev_charging_system < auth_redesign_migration.sql
-- ============================================================================

-- ---------------------------------------------------------------------------
-- STEP 0 — pre-flight check, run first and read the result before continuing.
-- If this returns any rows with a non-NULL email, those duplicate accounts
-- must be resolved (merge or rename) by hand first. Confirmed on this DB
-- (2026-07-29): the only "duplicate" is 3 rows sharing email=NULL, which is
-- not a real collision (MySQL's UNIQUE index already allows multiple NULLs)
-- and `email` already carries a UNIQUE index from the original schema, so
-- STEP 1 below does not add another one.
-- ---------------------------------------------------------------------------
-- SELECT email, COUNT(*) AS c FROM users GROUP BY email HAVING c > 1;

-- ---------------------------------------------------------------------------
-- STEP 1 — additive changes to `users`. The old `password` column is kept
-- (not renamed/dropped) so the previous AuthService can still authenticate
-- against it if this deploy needs to be rolled back — see design doc §12.
-- ---------------------------------------------------------------------------
ALTER TABLE users
  ADD COLUMN password_hash VARCHAR(255) NULL AFTER password,
  ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE AFTER role,
  ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER email_verified,
  ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

-- Grandfather in every existing account as email-verified — they already
-- have working accounts and never went through OTP, but their email was
-- never disproven either (see design doc §12 point 3).
UPDATE users SET email_verified = TRUE WHERE email_verified = FALSE;

-- password_hash is populated by the one-time CommandLineRunner migration
-- (app.migration.hash-legacy-passwords=true — see AuthController package
-- LegacyPasswordMigrationRunner), not by this script, since it needs
-- BCrypt in application code. Do not drop the old `password` column until
-- that has run successfully and the new login path has been verified
-- against a real account.

-- ---------------------------------------------------------------------------
-- STEP 2 — new tables.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS email_verification_otp (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL,
  otp_hash VARCHAR(255) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  max_attempts INT NOT NULL DEFAULT 5,
  resend_count INT NOT NULL DEFAULT 0,
  max_resends INT NOT NULL DEFAULT 3,
  last_sent_at TIMESTAMP NOT NULL,
  consumed BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_email_verification_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS password_reset_otp (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  otp_hash VARCHAR(255) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  max_attempts INT NOT NULL DEFAULT 5,
  resend_count INT NOT NULL DEFAULT 0,
  max_resends INT NOT NULL DEFAULT 3,
  last_sent_at TIMESTAMP NOT NULL,
  consumed BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_password_reset_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Future — schema only, not written to until refresh tokens are actually
-- issued (see AUTHENTICATION_DESIGN.md §7). Harmless to create now.
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  token_hash VARCHAR(255) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  revoked BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- STEP 3 (run later, separately, only once password_hash is fully populated
-- and the new login path is confirmed working against real accounts) —
-- cleanup. Not run as part of this script on purpose.
-- ---------------------------------------------------------------------------
-- ALTER TABLE users DROP COLUMN password;
-- ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255) NOT NULL;
