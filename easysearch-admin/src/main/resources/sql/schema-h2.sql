-- H2-compatible schema for QP admin (MySQL-compat mode)

CREATE TABLE IF NOT EXISTS qp_resource_set (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  module_type VARCHAR(64) NOT NULL,
  scene VARCHAR(64) NOT NULL DEFAULT 'default',
  env VARCHAR(32) NOT NULL DEFAULT 'prod',
  name VARCHAR(128) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  current_version_id BIGINT NULL,
  staging_version_id BIGINT NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_module_scene_env UNIQUE (module_type, scene, env)
);

CREATE TABLE IF NOT EXISTS qp_resource_version (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  resource_set_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  status VARCHAR(16) NOT NULL,
  `protected` TINYINT NOT NULL DEFAULT 0,
  checksum VARCHAR(64) NULL,
  change_log VARCHAR(1024) NULL,
  published_by VARCHAR(64) NULL,
  published_at TIMESTAMP NULL,
  archived_at TIMESTAMP NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_set_version UNIQUE (resource_set_id, version_no)
);
CREATE INDEX IF NOT EXISTS idx_ver_set_status ON qp_resource_version(resource_set_id, status);

CREATE TABLE IF NOT EXISTS qp_publish_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  resource_set_id BIGINT NOT NULL,
  version_id BIGINT NOT NULL,
  env VARCHAR(32) NOT NULL,
  publish_status VARCHAR(16) NOT NULL,
  publish_msg VARCHAR(2048) NULL,
  started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at TIMESTAMP NULL,
  operator VARCHAR(64) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_pr_set_env_time ON qp_publish_record(resource_set_id, env, started_at);

CREATE TABLE IF NOT EXISTS qp_operation_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_name VARCHAR(64) NOT NULL,
  action VARCHAR(64) NOT NULL,
  resource_set_id BIGINT NOT NULL,
  version_id BIGINT NULL,
  batch_id VARCHAR(64) NULL,
  entity_type VARCHAR(64) NULL,
  entity_id BIGINT NULL,
  before_json CLOB NULL,
  after_json CLOB NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ol_set_time ON qp_operation_log(resource_set_id, created_at);

CREATE TABLE IF NOT EXISTS qp_rule_intervention_sentence (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  version_id BIGINT NOT NULL,
  source_text VARCHAR(255) NOT NULL,
  target_text VARCHAR(255) NOT NULL,
  match_type VARCHAR(16) NOT NULL DEFAULT 'EXACT',
  priority INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ris_ver_source ON qp_rule_intervention_sentence(version_id, source_text);
CREATE INDEX IF NOT EXISTS idx_ris_ver_priority ON qp_rule_intervention_sentence(version_id, priority);

CREATE TABLE IF NOT EXISTS qp_rule_intervention_term (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  version_id BIGINT NOT NULL,
  source_text VARCHAR(255) NOT NULL,
  target_text VARCHAR(255) NOT NULL,
  priority INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_rit_ver_source ON qp_rule_intervention_term(version_id, source_text);

CREATE TABLE IF NOT EXISTS qp_rule_synonym (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  version_id BIGINT NOT NULL,
  source_text VARCHAR(255) NOT NULL,
  direction VARCHAR(8) NOT NULL,
  targets_json CLOB NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_rs_ver_source ON qp_rule_synonym(version_id, source_text);

CREATE TABLE IF NOT EXISTS qp_rule_entity (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  version_id BIGINT NOT NULL,
  entity_text VARCHAR(255) NOT NULL,
  entity_type VARCHAR(32) NOT NULL,
  normalized_value VARCHAR(255) NOT NULL,
  aliases_json CLOB NULL,
  attributes_json CLOB NULL,
  relations_json CLOB NULL,
  ids_json CLOB NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_re_ver_entity ON qp_rule_entity(version_id, entity_text);
CREATE INDEX IF NOT EXISTS idx_re_ver_type ON qp_rule_entity(version_id, entity_type);

CREATE TABLE IF NOT EXISTS qp_rule_token_dict (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  version_id BIGINT NOT NULL,
  word VARCHAR(255) NOT NULL,
  nature VARCHAR(64) NOT NULL,
  frequency INT NULL,
  biz_id VARCHAR(64) NULL,
  dict_type VARCHAR(16) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_rtd_ver_word ON qp_rule_token_dict(version_id, word);
CREATE INDEX IF NOT EXISTS idx_rtd_ver_type ON qp_rule_token_dict(version_id, dict_type);

CREATE TABLE IF NOT EXISTS qp_rule_meta (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  version_id BIGINT NOT NULL,
  term_type VARCHAR(16) NOT NULL,
  category_id VARCHAR(64) NULL,
  category_name VARCHAR(255) NULL,
  brand_id VARCHAR(64) NULL,
  brand_name VARCHAR(255) NULL,
  brand_name_en VARCHAR(255) NULL,
  model_id VARCHAR(64) NULL,
  model_name VARCHAR(255) NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_rm_ver_term_type ON qp_rule_meta(version_id, term_type);
