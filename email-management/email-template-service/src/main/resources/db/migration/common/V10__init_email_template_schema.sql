-- Email template service schema managed by Flyway.
-- Tables target the current schema configured via spring.flyway.

CREATE TABLE IF NOT EXISTS email_template (
  id            BIGSERIAL PRIMARY KEY,
  tenant_id     VARCHAR(64),
  created_at    TIMESTAMPTZ NOT NULL,
  updated_at    TIMESTAMPTZ NOT NULL,
  version       BIGINT NOT NULL,
  name          VARCHAR(128) NOT NULL,
  locale        VARCHAR(32)  NOT NULL,
  description   VARCHAR(512),
  archived      BOOLEAN      NOT NULL DEFAULT FALSE,
  metadata      JSONB,
  CONSTRAINT uq_email_template_name_locale UNIQUE (tenant_id, name, locale)
);

CREATE TABLE IF NOT EXISTS email_template_attachment (
  template_id        BIGINT      NOT NULL REFERENCES email_template (id) ON DELETE CASCADE,
  attachment_name    VARCHAR(255) NOT NULL,
  attachment_type    VARCHAR(255),
  attachment_url     VARCHAR(1024),
  attachment_size    BIGINT,
  inline_attachment  BOOLEAN,
  PRIMARY KEY (template_id, attachment_name)
);

CREATE TABLE IF NOT EXISTS email_template_version (
  id                  BIGSERIAL PRIMARY KEY,
  tenant_id           VARCHAR(64),
  created_at          TIMESTAMPTZ NOT NULL,
  updated_at          TIMESTAMPTZ NOT NULL,
  version             BIGINT NOT NULL,
  template_id         BIGINT NOT NULL REFERENCES email_template (id) ON DELETE CASCADE,
  version_number      INT    NOT NULL,
  subject             VARCHAR(256) NOT NULL,
  html_body           TEXT   NOT NULL,
  text_body           TEXT,
  metadata            JSONB,
  status              VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  published_at        TIMESTAMPTZ,
  sendgrid_template_id VARCHAR(128),
  sendgrid_version_id VARCHAR(64),
  CONSTRAINT uq_template_version UNIQUE (template_id, version_number)
);
CREATE INDEX IF NOT EXISTS idx_email_template_version_template ON email_template_version (template_id);

CREATE TABLE IF NOT EXISTS email_template_version_attachment (
  template_version_id BIGINT NOT NULL REFERENCES email_template_version (id) ON DELETE CASCADE,
  attachment_name     VARCHAR(255) NOT NULL,
  attachment_type     VARCHAR(255),
  attachment_url      VARCHAR(1024),
  attachment_size     BIGINT,
  inline_attachment   BOOLEAN,
  PRIMARY KEY (template_version_id, attachment_name)
);

CREATE TABLE IF NOT EXISTS email_template_allowed_variable (
  template_version_id BIGINT      NOT NULL REFERENCES email_template_version (id) ON DELETE CASCADE,
  variable_name       VARCHAR(128) NOT NULL,
  PRIMARY KEY (template_version_id, variable_name)
);

CREATE TABLE IF NOT EXISTS sendgrid_setting (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    VARCHAR(64),
  created_at   TIMESTAMPTZ NOT NULL,
  updated_at   TIMESTAMPTZ NOT NULL,
  version      BIGINT NOT NULL,
  secret_id    VARCHAR(128) NOT NULL,
  from_email   VARCHAR(128),
  from_name    VARCHAR(128),
  reply_to_email VARCHAR(128),
  sandbox_mode BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS email_send (
  id                   BIGSERIAL PRIMARY KEY,
  tenant_id            VARCHAR(64),
  created_at           TIMESTAMPTZ NOT NULL,
  updated_at           TIMESTAMPTZ NOT NULL,
  version              BIGINT NOT NULL,
  template_version_id  BIGINT REFERENCES email_template_version (id) ON DELETE SET NULL,
  dynamic_data         JSONB,
  status               VARCHAR(32) NOT NULL DEFAULT 'QUEUED',
  mode                 VARCHAR(32) NOT NULL DEFAULT 'PRODUCTION',
  idempotency_key      VARCHAR(128),
  sendgrid_message_id  VARCHAR(128),
  requested_at         TIMESTAMPTZ,
  processed_at         TIMESTAMPTZ,
  error_code           VARCHAR(128),
  error_message        VARCHAR(512),
  CONSTRAINT uq_email_send_idempotency UNIQUE (idempotency_key)
);
CREATE INDEX IF NOT EXISTS idx_email_send_template_version ON email_send (template_version_id);

CREATE TABLE IF NOT EXISTS email_send_recipient (
  send_id     BIGINT NOT NULL REFERENCES email_send (id) ON DELETE CASCADE,
  list_order  INT    NOT NULL,
  recipient   TEXT   NOT NULL,
  PRIMARY KEY (send_id, list_order)
);

CREATE TABLE IF NOT EXISTS email_send_cc (
  send_id     BIGINT NOT NULL REFERENCES email_send (id) ON DELETE CASCADE,
  list_order  INT    NOT NULL,
  cc          TEXT   NOT NULL,
  PRIMARY KEY (send_id, list_order)
);

CREATE TABLE IF NOT EXISTS email_send_bcc (
  send_id     BIGINT NOT NULL REFERENCES email_send (id) ON DELETE CASCADE,
  list_order  INT    NOT NULL,
  bcc         TEXT   NOT NULL,
  PRIMARY KEY (send_id, list_order)
);

CREATE TABLE IF NOT EXISTS email_send_attachment (
  send_id             BIGINT NOT NULL REFERENCES email_send (id) ON DELETE CASCADE,
  attachment_name     VARCHAR(255) NOT NULL,
  attachment_type     VARCHAR(255),
  attachment_url      VARCHAR(1024),
  attachment_size     BIGINT,
  inline_attachment   BOOLEAN,
  PRIMARY KEY (send_id, attachment_name)
);

CREATE TABLE IF NOT EXISTS email_event (
  id                 BIGSERIAL PRIMARY KEY,
  tenant_id          VARCHAR(64),
  created_at         TIMESTAMPTZ NOT NULL,
  updated_at         TIMESTAMPTZ NOT NULL,
  version            BIGINT NOT NULL,
  email_send_id      BIGINT REFERENCES email_send (id) ON DELETE SET NULL,
  event_type         VARCHAR(32) NOT NULL,
  event_timestamp    TIMESTAMPTZ,
  payload            JSONB,
  sendgrid_message_id VARCHAR(128)
);
CREATE INDEX IF NOT EXISTS idx_email_event_send ON email_event (email_send_id);
CREATE INDEX IF NOT EXISTS idx_email_event_message ON email_event (sendgrid_message_id);

