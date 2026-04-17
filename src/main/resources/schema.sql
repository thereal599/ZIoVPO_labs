CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(100) NOT NULL,
    is_account_expired BOOLEAN NOT NULL DEFAULT FALSE,
    is_account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    is_credentials_expired BOOLEAN NOT NULL DEFAULT FALSE,
    is_disabled BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS product (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS license_type (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    default_duration_in_days INTEGER NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS device (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    mac_address VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID,
    CONSTRAINT fk_device_user
        FOREIGN KEY (user_id) REFERENCES users(id)
            ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS license (
    id UUID PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID,
    product_id UUID NOT NULL,
    type_id UUID NOT NULL,
    first_activation_date DATE,
    ending_date DATE,
    blocked BOOLEAN NOT NULL DEFAULT FALSE,
    device_count INTEGER NOT NULL,
    owner_id UUID NOT NULL,
    description TEXT,
    CONSTRAINT fk_license_user
        FOREIGN KEY (user_id) REFERENCES users(id)
            ON DELETE SET NULL,
    CONSTRAINT fk_license_product
        FOREIGN KEY (product_id) REFERENCES product(id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_license_type
        FOREIGN KEY (type_id) REFERENCES license_type(id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_license_owner
        FOREIGN KEY (owner_id) REFERENCES users(id)
            ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS device_license (
    id UUID PRIMARY KEY,
    license_id UUID NOT NULL,
    device_id UUID NOT NULL,
    activation_date DATE NOT NULL,
    CONSTRAINT fk_device_license_license
        FOREIGN KEY (license_id) REFERENCES license(id)
            ON DELETE CASCADE,
    CONSTRAINT fk_device_license_device
        FOREIGN KEY (device_id) REFERENCES device(id)
            ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS license_history (
    id UUID PRIMARY KEY,
    license_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(100) NOT NULL,
    change_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description TEXT,
    CONSTRAINT fk_license_history_license
        FOREIGN KEY (license_id) REFERENCES license(id)
            ON DELETE CASCADE,
    CONSTRAINT fk_license_history_user
        FOREIGN KEY (user_id) REFERENCES users(id)
            ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS user_sessions (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    access_token VARCHAR(512),
    refresh_token VARCHAR(512) UNIQUE,
    access_token_expiry TIMESTAMP,
    refresh_token_expiry TIMESTAMP,
    status VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS malware_signatures (
    id UUID PRIMARY KEY,
    threat_name VARCHAR(255) NOT NULL,
    first_bytes_hex VARCHAR(512) NOT NULL,
    remainder_hash_hex VARCHAR(512) NOT NULL,
    remainder_length BIGINT NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    offset_start BIGINT NOT NULL,
    offset_end BIGINT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL,
    digital_signature_base64 TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS signatures_history (
    history_id BIGSERIAL PRIMARY KEY,
    signature_id UUID NOT NULL,
    version_created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    threat_name VARCHAR(255) NOT NULL,
    first_bytes_hex VARCHAR(512) NOT NULL,
    remainder_hash_hex VARCHAR(512) NOT NULL,
    remainder_length BIGINT NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    offset_start BIGINT NOT NULL,
    offset_end BIGINT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL,
    digital_signature_base64 TEXT NOT NULL,

    CONSTRAINT fk_signatures_history_signature
        FOREIGN KEY (signature_id) REFERENCES malware_signatures(id)
);

CREATE TABLE IF NOT EXISTS signatures_audit (
    audit_id BIGSERIAL PRIMARY KEY,
    signature_id UUID NOT NULL,
    changed_by VARCHAR(255) NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    fields_changed TEXT,
    description TEXT NOT NULL,

    CONSTRAINT fk_signatures_audit_signature
      FOREIGN KEY (signature_id) REFERENCES malware_signatures(id)
);