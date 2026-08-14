-- Biodex database schema.
-- Executed on every startup by SchemaInitialiser, so every statement is idempotent.

CREATE TABLE IF NOT EXISTS users (
    user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL UNIQUE,
    email         TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    created_at    TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS user_settings (
    user_id            INTEGER PRIMARY KEY,
    theme              TEXT    NOT NULL DEFAULT 'light',
    two_factor_enabled INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS suburbs (
    suburb_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name      TEXT NOT NULL,
    postcode  TEXT NOT NULL,
    latitude  REAL,
    longitude REAL,
    UNIQUE (name, postcode)
);

CREATE TABLE IF NOT EXISTS sightings (
    sighting_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL,
    suburb_id    INTEGER NOT NULL,
    species_name TEXT    NOT NULL,
    description  TEXT,
    image_path   TEXT,
    sighted_at   TEXT    NOT NULL,
    created_at   TEXT    NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    FOREIGN KEY (suburb_id) REFERENCES suburbs (suburb_id)
);

CREATE TABLE IF NOT EXISTS password_reset_codes (
    code_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL,
    code_hash  TEXT    NOT NULL,
    expires_at TEXT    NOT NULL,
    used       INTEGER NOT NULL DEFAULT 0,
    created_at TEXT    NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS two_factor_codes (
    code_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL,
    code_hash  TEXT    NOT NULL,
    expires_at TEXT    NOT NULL,
    used       INTEGER NOT NULL DEFAULT 0,
    created_at TEXT    NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

-- Every user gets a settings row the moment they are created, so no page has to
-- handle a missing one.
CREATE TRIGGER IF NOT EXISTS create_default_user_settings
AFTER INSERT ON users
BEGIN
    INSERT INTO user_settings (user_id) VALUES (NEW.user_id);
END;
