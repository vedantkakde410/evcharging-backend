-- ============================================================================
-- Module 9 - Authorization & Security Hardening migration.
-- Run by hand against `ev_charging_system` (spring.jpa.hibernate.ddl-auto=none)
-- before deploying this module's backend code.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- STEP 1 - revoked_tokens: backs JWT logout revocation (see
-- security.TokenRevocationService / JwtAuthenticationFilter).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS revoked_tokens (
  jti VARCHAR(36) PRIMARY KEY,
  expires_at TIMESTAMP NOT NULL,
  revoked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- STEP 2 - drop the legacy plaintext `password` column, deferred from
-- Module 8 (auth_redesign_migration.sql) until password_hash was confirmed
-- populated and the new BCrypt login path confirmed working against real
-- accounts - both verified live in Module 8. Nothing in the codebase reads
-- `password` anymore (LegacyPasswordMigrationRunner, the only reader, is
-- deleted in this module). `password_hash` is intentionally left nullable:
-- 3 pre-existing seed rows (ids 1-3) have NULL email and NULL password and
-- can never log in through any path (email lookup fails first) - not worth
-- deleting rows this migration has no product mandate to touch.
-- ---------------------------------------------------------------------------
ALTER TABLE users DROP COLUMN password;
