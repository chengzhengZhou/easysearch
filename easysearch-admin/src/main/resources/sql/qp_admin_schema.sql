-- DB-only schema for QP admin（简化版）
-- MySQL 5.7+
-- 简化设计：去掉 staging/version 概念，改为就地编辑 + 结构化快照

-- ==================== 核心表 ====================

-- 资源集（简化：去掉 staging_version_id，改为 current_snapshot_id）
CREATE TABLE IF NOT EXISTS qp_resource_set (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  module_type VARCHAR(64) NOT NULL,
  scene VARCHAR(64) NOT NULL DEFAULT 'default',
  env VARCHAR(32) NOT NULL DEFAULT 'prod',
  name VARCHAR(128) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  current_snapshot_id BIGINT NULL,           -- 线上生效的快照 ID
  created_by VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_module_scene_env (module_type, scene, env)
);

-- 快照元信息（取代 qp_resource_version）
CREATE TABLE IF NOT EXISTS qp_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_set_id BIGINT NOT NULL,
  snapshot_no INT NOT NULL,                  -- 递增编号
  checksum VARCHAR(64) NULL,
  change_log VARCHAR(1024) NULL,
  rule_count INT NOT NULL DEFAULT 0,
  published_by VARCHAR(64) NOT NULL,
  published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  protected_flag TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_set_snapshot (resource_set_id, snapshot_no),
  KEY idx_set_time (resource_set_id, published_at)
);

-- 发布记录
CREATE TABLE IF NOT EXISTS qp_publish_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_set_id BIGINT NOT NULL,
  snapshot_id BIGINT NOT NULL,               -- 改为 snapshot_id
  env VARCHAR(32) NOT NULL,
  publish_status VARCHAR(16) NOT NULL,       -- running/success/failed
  publish_msg VARCHAR(2048) NULL,
  started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at DATETIME NULL,
  operator VARCHAR(64) NOT NULL,
  KEY idx_set_env_time (resource_set_id, env, started_at)
);

-- 操作日志
CREATE TABLE IF NOT EXISTS qp_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_name VARCHAR(64) NOT NULL,
  action VARCHAR(64) NOT NULL,               -- create/update/delete/publish/rollback
  resource_set_id BIGINT NOT NULL,
  snapshot_id BIGINT NULL,                   -- 改为 snapshot_id
  batch_id VARCHAR(64) NULL,
  entity_type VARCHAR(64) NULL,
  entity_id BIGINT NULL,
  before_json JSON NULL,
  after_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_set_time (resource_set_id, created_at)
);

-- ==================== 当前规则表（就地编辑，归属 resource_set_id） ====================

-- 整句干预（当前编辑态）
CREATE TABLE IF NOT EXISTS qp_rule_intervention_sentence (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_set_id BIGINT NOT NULL,           -- 直接归属资源集
  source_text VARCHAR(255) NOT NULL,
  target_text VARCHAR(255) NOT NULL,
  match_type VARCHAR(16) NOT NULL DEFAULT 'EXACT',
  priority INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_set_source (resource_set_id, source_text)
);

-- 词表干预（当前编辑态）
CREATE TABLE IF NOT EXISTS qp_rule_intervention_term (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_set_id BIGINT NOT NULL,
  source_text VARCHAR(255) NOT NULL,
  target_text VARCHAR(255) NOT NULL,
  priority INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_set_source (resource_set_id, source_text)
);

-- 同义词（当前编辑态）
CREATE TABLE IF NOT EXISTS qp_rule_synonym (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_set_id BIGINT NOT NULL,
  source_text VARCHAR(255) NOT NULL,
  direction VARCHAR(8) NOT NULL,             -- => / <= / SYM
  targets_json JSON NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_set_source (resource_set_id, source_text)
);

-- 实体（当前编辑态）
CREATE TABLE IF NOT EXISTS qp_rule_entity (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_set_id BIGINT NOT NULL,
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
  KEY idx_set_entity (resource_set_id, entity_text),
  KEY idx_set_type (resource_set_id, entity_type)
);

-- 分词词典（当前编辑态）
CREATE TABLE IF NOT EXISTS qp_rule_token_dict (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_set_id BIGINT NOT NULL,
  word VARCHAR(255) NOT NULL,
  nature VARCHAR(64) NOT NULL,
  frequency INT NULL,
  biz_id VARCHAR(64) NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_set_word (resource_set_id, word)
);

-- 元信息（当前编辑态）
CREATE TABLE IF NOT EXISTS qp_rule_meta (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_set_id BIGINT NOT NULL,
  term_type VARCHAR(16) NOT NULL,            -- category/brand/model
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
  KEY idx_set_term_type (resource_set_id, term_type)
);

-- ==================== 快照规则表（发布时逐行复制） ====================

-- 整句干预快照
CREATE TABLE IF NOT EXISTS qp_snapshot_intervention_sentence (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  snapshot_id BIGINT NOT NULL,
  source_rule_id BIGINT NULL,                -- 来源规则 id，便于 diff 追踪
  source_text VARCHAR(255) NOT NULL,
  target_text VARCHAR(255) NOT NULL,
  match_type VARCHAR(16) NOT NULL DEFAULT 'EXACT',
  priority INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_snapshot (snapshot_id),
  KEY idx_snapshot_source (snapshot_id, source_text)
);

-- 词表干预快照
CREATE TABLE IF NOT EXISTS qp_snapshot_intervention_term (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  snapshot_id BIGINT NOT NULL,
  source_rule_id BIGINT NULL,
  source_text VARCHAR(255) NOT NULL,
  target_text VARCHAR(255) NOT NULL,
  priority INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_snapshot (snapshot_id),
  KEY idx_snapshot_source (snapshot_id, source_text)
);

-- 同义词快照
CREATE TABLE IF NOT EXISTS qp_snapshot_synonym (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  snapshot_id BIGINT NOT NULL,
  source_rule_id BIGINT NULL,
  source_text VARCHAR(255) NOT NULL,
  direction VARCHAR(8) NOT NULL,
  targets_json JSON NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_snapshot (snapshot_id),
  KEY idx_snapshot_source (snapshot_id, source_text)
);

-- 实体快照
CREATE TABLE IF NOT EXISTS qp_snapshot_entity (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  snapshot_id BIGINT NOT NULL,
  source_rule_id BIGINT NULL,
  entity_text VARCHAR(255) NOT NULL,
  entity_type VARCHAR(32) NOT NULL,
  normalized_value VARCHAR(255) NOT NULL,
  aliases_json JSON NULL,
  attributes_json JSON NULL,
  relations_json JSON NULL,
  ids_json JSON NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_snapshot (snapshot_id),
  KEY idx_snapshot_entity (snapshot_id, entity_text)
);

-- 分词词典快照
CREATE TABLE IF NOT EXISTS qp_snapshot_token_dict (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  snapshot_id BIGINT NOT NULL,
  source_rule_id BIGINT NULL,
  word VARCHAR(255) NOT NULL,
  nature VARCHAR(64) NOT NULL,
  frequency INT NULL,
  biz_id VARCHAR(64) NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_snapshot (snapshot_id),
  KEY idx_snapshot_word (snapshot_id, word)
);

-- 元信息快照
CREATE TABLE IF NOT EXISTS qp_snapshot_meta (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  snapshot_id BIGINT NOT NULL,
  source_rule_id BIGINT NULL,
  term_type VARCHAR(16) NOT NULL,
  category_id VARCHAR(64) NULL,
  category_name VARCHAR(255) NULL,
  brand_id VARCHAR(64) NULL,
  brand_name VARCHAR(255) NULL,
  brand_name_en VARCHAR(255) NULL,
  model_id VARCHAR(64) NULL,
  model_name VARCHAR(255) NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_snapshot (snapshot_id),
  KEY idx_snapshot_term_type (snapshot_id, term_type)
);
