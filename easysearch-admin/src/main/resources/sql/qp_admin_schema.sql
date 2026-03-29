-- DB-only schema for QP admin
-- MySQL 5.7+

CREATE TABLE IF NOT EXISTS qp_resource_set (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  module_type VARCHAR(64) NOT NULL,
  scene VARCHAR(64) NOT NULL DEFAULT 'default',
  env VARCHAR(32) NOT NULL DEFAULT 'prod',
  name VARCHAR(128) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  current_version_id BIGINT NULL,
  staging_version_id BIGINT NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_module_scene_env (module_type, scene, env)
);

CREATE TABLE IF NOT EXISTS qp_resource_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_set_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  status VARCHAR(16) NOT NULL, -- draft/published/archived
  protected TINYINT NOT NULL DEFAULT 0,
  checksum VARCHAR(64) NULL,
  change_log VARCHAR(1024) NULL,
  published_by VARCHAR(64) NULL,
  published_at DATETIME NULL,
  archived_at DATETIME NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_set_version (resource_set_id, version_no),
  KEY idx_set_status (resource_set_id, status)
);

CREATE TABLE IF NOT EXISTS qp_publish_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_set_id BIGINT NOT NULL,
  version_id BIGINT NOT NULL,
  env VARCHAR(32) NOT NULL,
  publish_status VARCHAR(16) NOT NULL, -- running/success/failed
  publish_msg VARCHAR(2048) NULL,
  started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at DATETIME NULL,
  operator VARCHAR(64) NOT NULL,
  KEY idx_set_env_time (resource_set_id, env, started_at)
);

CREATE TABLE IF NOT EXISTS qp_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_name VARCHAR(64) NOT NULL,
  action VARCHAR(64) NOT NULL, -- create/update/delete/publish/rollback
  resource_set_id BIGINT NOT NULL,
  version_id BIGINT NULL,
  batch_id VARCHAR(64) NULL,
  entity_type VARCHAR(64) NULL,
  entity_id BIGINT NULL,
  before_json JSON NULL,
  after_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_set_time (resource_set_id, created_at)
);

CREATE TABLE IF NOT EXISTS qp_rule_intervention_sentence (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  version_id BIGINT NOT NULL,
  source_text VARCHAR(255) NOT NULL,
  target_text VARCHAR(255) NOT NULL,
  match_type VARCHAR(16) NOT NULL DEFAULT 'EXACT',
  priority INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_ver_source (version_id, source_text),
  KEY idx_ver_priority (version_id, priority)
);

CREATE TABLE IF NOT EXISTS qp_rule_intervention_term (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  version_id BIGINT NOT NULL,
  source_text VARCHAR(255) NOT NULL,
  target_text VARCHAR(255) NOT NULL,
  priority INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_ver_source (version_id, source_text)
);

CREATE TABLE IF NOT EXISTS qp_rule_synonym (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  version_id BIGINT NOT NULL,
  source_text VARCHAR(255) NOT NULL,
  direction VARCHAR(8) NOT NULL, -- => / <= / SYM
  targets_json JSON NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_ver_source (version_id, source_text)
);

CREATE TABLE IF NOT EXISTS qp_rule_entity (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  version_id BIGINT NOT NULL,
  entity_text VARCHAR(255) NOT NULL,
  entity_type VARCHAR(32) NOT NULL,
  normalized_value VARCHAR(255) NOT NULL,
  aliases_json JSON NULL,
  attributes_json JSON NULL,
  relations_json JSON NULL,
  ids_json JSON NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_ver_entity (version_id, entity_text),
  KEY idx_ver_type (version_id, entity_type)
);

CREATE TABLE IF NOT EXISTS qp_rule_token_dict (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  version_id BIGINT NOT NULL,
  word VARCHAR(255) NOT NULL,
  nature VARCHAR(64) NOT NULL,
  frequency INT NULL,
  biz_id VARCHAR(64) NULL,
  dict_type VARCHAR(16) NOT NULL, -- dic/id
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_ver_word (version_id, word),
  KEY idx_ver_type (version_id, dict_type)
);

CREATE TABLE IF NOT EXISTS qp_rule_meta (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  version_id BIGINT NOT NULL,
  term_type VARCHAR(16) NOT NULL, -- category/brand/model
  category_id VARCHAR(64) NULL,
  category_name VARCHAR(255) NULL,
  brand_id VARCHAR(64) NULL,
  brand_name VARCHAR(255) NULL,
  brand_name_en VARCHAR(255) NULL,
  model_id VARCHAR(64) NULL,
  model_name VARCHAR(255) NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_ver_term_type (version_id, term_type)
);

