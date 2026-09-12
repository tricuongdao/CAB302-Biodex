-- =========================================================
-- Pest/Species Tracker Database Schema
-- Reconciled from spec (see notes to user for merge decisions)
-- Target: SQLite (types/CHECKs are portable to MySQL/Postgres with minor tweaks)
-- =========================================================

PRAGMA foreign_keys = ON;

-- ---------------------------------------------------------
-- users
-- ---------------------------------------------------------
CREATE TABLE users (
    user_id             INTEGER PRIMARY KEY AUTOINCREMENT,
    username            VARCHAR(50)  UNIQUE NOT NULL,
    email               VARCHAR(100) UNIQUE NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    display_name        VARCHAR(100),
    two_factor_enabled  BOOLEAN NOT NULL DEFAULT 0,
    failed_attempts     INTEGER NOT NULL DEFAULT 0,
    lock_expires_at     TIMESTAMP,
    last_login_at       TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (length(username) BETWEEN 3 AND 20)
);

-- ---------------------------------------------------------
-- suburbs
-- NOTE: not defined in the original spec but referenced by
-- user_settings.default_suburb_id and sightings.suburb_id.
-- Minimal reasonable structure added so those FKs resolve.
-- ---------------------------------------------------------
CREATE TABLE suburbs (
    suburb_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    suburb_name   VARCHAR(100) NOT NULL,
    postcode      VARCHAR(10),
    state         VARCHAR(10) DEFAULT 'QLD'
);

-- ---------------------------------------------------------
-- user_settings
-- ---------------------------------------------------------
CREATE TABLE user_settings (
    setting_id                              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id                                 INTEGER UNIQUE NOT NULL
                                             REFERENCES users(user_id) ON DELETE CASCADE,
    theme                                   VARCHAR(10) NOT NULL DEFAULT 'LIGHT'
                                             CHECK (theme IN ('LIGHT','DARK','SYSTEM')),
    language                                VARCHAR(10) NOT NULL DEFAULT 'en-AU',
    default_suburb_id                       INTEGER REFERENCES suburbs(suburb_id),
    auto_identify_on_upload                 BOOLEAN NOT NULL DEFAULT 1,
    identification_confidence_threshold     DECIMAL(3,2) NOT NULL DEFAULT 0.70
                                             CHECK (identification_confidence_threshold BETWEEN 0.00 AND 1.00),
    auto_compress_photos                    BOOLEAN NOT NULL DEFAULT 1,
    max_upload_resolution                   VARCHAR(20) NOT NULL DEFAULT '1920x1080'
                                             CHECK (max_upload_resolution IN
                                                    ('640x480','1280x720','1920x1080','3840x2160')),
    notify_new_sightings_nearby             BOOLEAN NOT NULL DEFAULT 1,
    notify_community_alerts                 BOOLEAN NOT NULL DEFAULT 1,
    notify_app_updates                      BOOLEAN NOT NULL DEFAULT 0,
    share_location_publicly                 BOOLEAN NOT NULL DEFAULT 0,
    anonymize_uploads                       BOOLEAN NOT NULL DEFAULT 0,
    updated_at                              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------
-- species (base table + Species Details Screen extension merged)
-- ---------------------------------------------------------
CREATE TABLE species (
    species_id          INTEGER PRIMARY KEY AUTOINCREMENT,
    common_name         VARCHAR(100) NOT NULL,
    scientific_name      VARCHAR(150),
    dex_number          INTEGER UNIQUE,
    description         TEXT,
    image_url           VARCHAR(255),
    typical_habitat     VARCHAR(255),
    size_range          VARCHAR(50),
    threat_level        VARCHAR(10) NOT NULL DEFAULT 'MODERATE'
                         CHECK (threat_level IN ('LOW','MODERATE','HIGH','SEVERE')),
    disposal_guidance   TEXT,
    report_authority    VARCHAR(100),
    ala_guid            VARCHAR(255) UNIQUE,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------
-- species_tags
-- ---------------------------------------------------------
CREATE TABLE species_tags (
    tag_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    species_id  INTEGER NOT NULL REFERENCES species(species_id) ON DELETE CASCADE,
    label       VARCHAR(30) NOT NULL,
    UNIQUE (species_id, label)
);

-- ---------------------------------------------------------
-- species_threat_ratings
-- ---------------------------------------------------------
CREATE TABLE species_threat_ratings (
    rating_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    species_id     INTEGER NOT NULL REFERENCES species(species_id) ON DELETE CASCADE,
    metric         VARCHAR(40) NOT NULL,
    score          INTEGER NOT NULL CHECK (score BETWEEN 0 AND 100),
    display_order  INTEGER NOT NULL DEFAULT 0,
    UNIQUE (species_id, metric)
);

-- ---------------------------------------------------------
-- password_resets
-- ---------------------------------------------------------
CREATE TABLE password_resets (
    reset_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash   VARCHAR(64) NOT NULL UNIQUE,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at   TIMESTAMP NOT NULL,
    used_at      TIMESTAMP
);

-- ---------------------------------------------------------
-- sightings
-- MERGED from the two conflicting "Sightings" table specs
-- (Heat map section + Upload section). See chat notes for
-- the reconciliation decisions (severity enum, dropped
-- erroneous UNIQUE on user_id, unified lat/long types, etc).
-- ---------------------------------------------------------
CREATE TABLE sightings (
    sighting_id             INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id                 INTEGER REFERENCES users(user_id) ON DELETE SET NULL,
    species_id              INTEGER NOT NULL REFERENCES species(species_id),
    suburb_id               INTEGER REFERENCES suburbs(suburb_id),
    species_classification  VARCHAR(20)
                             CHECK (species_classification IN
                                    ('Reptile','Aviary','Insect','Mammal','Amphibian','Plant','Other')),
    severity                VARCHAR(10) NOT NULL DEFAULT 'MINOR'
                             CHECK (severity IN ('MINOR','MODERATE','SEVERE')),
    sighting_date           DATE NOT NULL,
    latitude                DECIMAL(9,6) NOT NULL,
    longitude               DECIMAL(9,6) NOT NULL,
    location_accuracy       INTEGER,
    verification_status     VARCHAR(15) NOT NULL DEFAULT 'UNVERIFIED'
                             CHECK (verification_status IN ('UNVERIFIED','VERIFIED','REJECTED')),
    uploaded_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------
-- sighting_images (renamed from "Sightings_server" for clarity)
-- ---------------------------------------------------------
CREATE TABLE sighting_images (
    image_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    sighting_id    INTEGER NOT NULL REFERENCES sightings(sighting_id) ON DELETE CASCADE,
    image_url      VARCHAR(128) NOT NULL,
    thumbnail_url  VARCHAR(128),
    uploaded_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------
-- Indexes for common lookups (map filtering, moderation, joins)
-- ---------------------------------------------------------
CREATE INDEX idx_sightings_species     ON sightings(species_id);
CREATE INDEX idx_sightings_suburb      ON sightings(suburb_id);
CREATE INDEX idx_sightings_user        ON sightings(user_id);
CREATE INDEX idx_sightings_date        ON sightings(sighting_date);
CREATE INDEX idx_sightings_status      ON sightings(verification_status);
CREATE INDEX idx_species_tags_species  ON species_tags(species_id);
CREATE INDEX idx_threat_ratings_species ON species_threat_ratings(species_id);
CREATE INDEX idx_sighting_images_sighting ON sighting_images(sighting_id);
CREATE INDEX idx_password_resets_user  ON password_resets(user_id);
