CREATE SCHEMA IF NOT EXISTS setup;

-- Ensure subsequent objects are created in setup schema
SET search_path TO setup;

-- Country table
CREATE TABLE IF NOT EXISTS country (
    country_id SERIAL PRIMARY KEY,
    country_cd VARCHAR(3) NOT NULL,
    country_en_nm VARCHAR(256) NOT NULL,
    country_ar_nm VARCHAR(256) NOT NULL,
    dialing_code VARCHAR(10),
    nationality_en VARCHAR(256),
    nationality_ar VARCHAR(256),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    en_description VARCHAR(1000),
    ar_description VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);
ALTER TABLE country ADD CONSTRAINT uq_country_cd UNIQUE (country_cd);
CREATE INDEX idx_country_cd ON country(country_cd);
CREATE INDEX idx_country_en_nm ON country(country_en_nm);

-- City table
CREATE TABLE IF NOT EXISTS city (
    city_id SERIAL PRIMARY KEY,
    city_cd VARCHAR(50) NOT NULL,
    city_en_nm VARCHAR(200) NOT NULL,
    city_ar_nm VARCHAR(200) NOT NULL,
    country_id INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_city_country FOREIGN KEY (country_id) REFERENCES country(country_id)
);
ALTER TABLE city ADD CONSTRAINT uk_city_cd UNIQUE (city_cd);
CREATE INDEX idx_city_country_active ON city(country_id, is_active);
CREATE INDEX idx_city_en_nm ON city(city_en_nm);
CREATE INDEX idx_city_ar_nm ON city(city_ar_nm);

-- Lookup table
CREATE TABLE IF NOT EXISTS lookup (
    lookup_item_id INTEGER PRIMARY KEY,
    lookup_item_cd VARCHAR(255),
    lookup_item_en_nm VARCHAR(255),
    lookup_item_ar_nm VARCHAR(255),
    lookup_group_code VARCHAR(255),
    parent_lookup_id VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    item_en_description VARCHAR(255),
    item_ar_description VARCHAR(255)
);

-- Resources table
CREATE TABLE IF NOT EXISTS resources (
    resource_id SERIAL PRIMARY KEY,
    resource_cd VARCHAR(128) NOT NULL,
    resource_en_nm VARCHAR(256) NOT NULL,
    resource_ar_nm VARCHAR(256) NOT NULL,
    path VARCHAR(512),
    http_method VARCHAR(16),
    parent_resource_id INTEGER,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    en_description VARCHAR(1000),
    ar_description VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);
ALTER TABLE resources ADD CONSTRAINT uq_resource_cd UNIQUE (resource_cd);
CREATE INDEX idx_resource_cd ON resources(resource_cd);
CREATE INDEX idx_resource_parent ON resources(parent_resource_id);

-- System Parameter table
CREATE TABLE IF NOT EXISTS system_parameter (
    param_id SERIAL PRIMARY KEY,
    param_key VARCHAR(150) NOT NULL,
    param_value VARCHAR(1000) NOT NULL,
    description VARCHAR(1000),
    group_code VARCHAR(150),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
ALTER TABLE system_parameter ADD CONSTRAINT uq_system_parameter_key UNIQUE (param_key);

