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
    language           TEXT    NOT NULL DEFAULT 'en-AU',
    measurement_unit   TEXT    NOT NULL DEFAULT 'METRIC',
    default_suburb_id  INTEGER,
    auto_identify_on_upload             INTEGER NOT NULL DEFAULT 1,
    identification_confidence_threshold REAL    NOT NULL DEFAULT 0.70,
    wifi_only_upload                    INTEGER NOT NULL DEFAULT 0,
    auto_compress_photos                INTEGER NOT NULL DEFAULT 1,
    max_upload_resolution               TEXT    NOT NULL DEFAULT '1920x1080',
    notify_new_sightings_nearby         INTEGER NOT NULL DEFAULT 1,
    notify_community_alerts             INTEGER NOT NULL DEFAULT 1,
    notify_app_updates                  INTEGER NOT NULL DEFAULT 0,
    share_location_publicly             INTEGER NOT NULL DEFAULT 0,
    anonymize_uploads                   INTEGER NOT NULL DEFAULT 0,
    text_size                           TEXT    NOT NULL DEFAULT 'MEDIUM',
    two_factor_enabled INTEGER NOT NULL DEFAULT 0,
    updated_at         TEXT    NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    FOREIGN KEY (default_suburb_id) REFERENCES suburbs (suburb_id)
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

-- Local curated species table (per pest-detail-technical-spec.md)
-- Holds threat ratings, habitat, disposal guidance, etc. that ALA does not provide.
CREATE TABLE IF NOT EXISTS species (
    species_id        INTEGER PRIMARY KEY AUTOINCREMENT,
    common_name       TEXT NOT NULL,
    scientific_name   TEXT NOT NULL,
    threat_level      TEXT NOT NULL,   -- LOW, MEDIUM, HIGH
    aggression        INTEGER NOT NULL,  -- 0-100
    sting_severity    INTEGER NOT NULL,  -- 0-100
    spread_risk       INTEGER NOT NULL,  -- 0-100
    typical_habitat   TEXT,
    size_min_mm       REAL,
    size_max_mm       REAL,
    disposal_guidance TEXT,
    photo_path        TEXT,
    ala_guid          TEXT UNIQUE      -- links to Atlas of Living Australia taxon
);

CREATE TABLE IF NOT EXISTS species_tags (
    species_id   INTEGER NOT NULL,
    tag          TEXT NOT NULL,   -- Invasive, Stinging, Toxic, Native, etc.
    PRIMARY KEY (species_id, tag),
    FOREIGN KEY (species_id) REFERENCES species (species_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_species_scientific ON species (scientific_name);
CREATE INDEX IF NOT EXISTS idx_species_common ON species (common_name);
CREATE INDEX IF NOT EXISTS idx_species_ala_guid ON species (ala_guid);

-- Local sighting reports (per pest-detail-technical-spec.md)
-- Submitted by users, density by suburb, verification status.
CREATE TABLE IF NOT EXISTS sighting_reports (
    report_id        INTEGER PRIMARY KEY AUTOINCREMENT,
    species_id       INTEGER NOT NULL,
    suburb           TEXT NOT NULL,
    location_label   TEXT NOT NULL,
    latitude         REAL,
    longitude        REAL,
    reported_at      TEXT NOT NULL DEFAULT (datetime('now')),
    verified         INTEGER NOT NULL DEFAULT 0,
    reporter_user_id INTEGER,
    photo_path       TEXT,
    FOREIGN KEY (species_id) REFERENCES species (species_id) ON DELETE CASCADE,
    FOREIGN KEY (reporter_user_id) REFERENCES users (user_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_reports_species_suburb ON sighting_reports (species_id, suburb);
CREATE INDEX IF NOT EXISTS idx_reports_species_date   ON sighting_reports (species_id, reported_at);
CREATE INDEX IF NOT EXISTS idx_reports_suburb_date    ON sighting_reports (suburb, reported_at);

CREATE TABLE IF NOT EXISTS password_reset_codes (
    code_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER NOT NULL,
    code_hash     TEXT    NOT NULL,
    expires_at    TEXT    NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    used          INTEGER NOT NULL DEFAULT 0,
    created_at    TEXT    NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_password_reset_codes_user_id
ON password_reset_codes (user_id);

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

-- Raw JSON responses from the Atlas of Living Australia, keyed by the request that
-- produced them. Lets the app show species data while offline. Written and read only
-- by the API layer through ApiCacheDao; entries are disposable and may be deleted at
-- any time.
CREATE TABLE IF NOT EXISTS api_cache (
    cache_key  VARCHAR(255) PRIMARY KEY,
    payload    TEXT NOT NULL,
    fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);