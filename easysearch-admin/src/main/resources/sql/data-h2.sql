-- H2-compatible seed data for QP admin

-- 1) resource set (id=1)
INSERT INTO qp_resource_set (
  id, module_type, scene, env, name, status,
  current_version_id, staging_version_id,
  created_by
) VALUES (
  1, 'intervention', 'default', 'prod',
  'intervention-default-prod', 1,
  NULL, NULL,
  'system'
);

-- 2) current version (published) v1 (id=1)
INSERT INTO qp_resource_version (
  id, resource_set_id, version_no, status, `protected`,
  checksum, change_log, published_by, published_at,
  archived_at, created_by
) VALUES (
  1, 1, 1, 'published', 1,
  NULL, 'init published', 'system', NOW(),
  NULL, 'system'
);

-- 3) staging version (draft) v2 (id=2)
INSERT INTO qp_resource_version (
  id, resource_set_id, version_no, status, `protected`,
  checksum, change_log, published_by, published_at,
  archived_at, created_by
) VALUES (
  2, 1, 2, 'draft', 0,
  NULL, 'init staging', NULL, NULL,
  NULL, 'system'
);

-- 4) set pointers
UPDATE qp_resource_set
SET current_version_id = 1,
    staging_version_id = 2
WHERE id = 1;
