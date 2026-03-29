-- 初始化 1 个资源集（示例：intervention / default / prod）
START TRANSACTION;
-- 1) resource set
INSERT INTO qp_resource_set (
  module_type, scene, env, name, status,
  current_version_id, staging_version_id,
  created_by
) VALUES (
  'intervention', 'default', 'prod',
  'intervention-default-prod', 1,
  NULL, NULL,
  'system'
);
SET @rs_id := LAST_INSERT_ID();
-- 2) current version (published) v1
INSERT INTO qp_resource_version (
  resource_set_id, version_no, status, protected,
  checksum, change_log, published_by, published_at,
  archived_at, created_by
) VALUES (
  @rs_id, 1, 'published', 1,
  NULL, 'init published', 'system', NOW(),
  NULL, 'system'
);
SET @current_vid := LAST_INSERT_ID();
-- 3) staging version (draft) v2
INSERT INTO qp_resource_version (
  resource_set_id, version_no, status, protected,
  checksum, change_log, published_by, published_at,
  archived_at, created_by
) VALUES (
  @rs_id, 2, 'draft', 0,
  NULL, 'init staging', NULL, NULL,
  NULL, 'system'
);
SET @staging_vid := LAST_INSERT_ID();
-- 4) set pointers
UPDATE qp_resource_set
SET current_version_id = @current_vid,
    staging_version_id = @staging_vid
WHERE id = @rs_id;
COMMIT;