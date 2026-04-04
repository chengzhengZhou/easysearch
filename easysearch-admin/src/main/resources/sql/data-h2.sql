-- H2-compatible seed data for QP admin (简化版)

-- 1) resource set (id=1) - 无需初始快照
INSERT INTO qp_resource_set (
  id, module_type, scene, env, name, status,
  current_snapshot_id,
  created_by
) VALUES (
  1, 'intervention', 'default', 'prod',
  'intervention-default-prod', 1,
  NULL,
  'system'
);
