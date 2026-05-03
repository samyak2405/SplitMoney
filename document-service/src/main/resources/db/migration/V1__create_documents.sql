CREATE SCHEMA IF NOT EXISTS documentdb;

CREATE TABLE documentdb.documents (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  group_id       VARCHAR(255) NOT NULL,
  uploader_id    VARCHAR(255) NOT NULL,
  uploader_email VARCHAR(255),
  file_name      VARCHAR(500) NOT NULL,
  original_name  VARCHAR(500) NOT NULL,
  mime_type      VARCHAR(100) NOT NULL,
  file_size      BIGINT       NOT NULL,
  storage_path   TEXT         NOT NULL,
  created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_docs_group_id ON documentdb.documents(group_id, created_at DESC);
