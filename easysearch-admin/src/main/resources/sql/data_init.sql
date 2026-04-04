-- 初始化 1 个资源集（示例：intervention / default / prod）（简化版）
START TRANSACTION;

-- 1) resource set（无需初始快照，直接编辑规则后发布即生成快照）
INSERT INTO qp_resource_set (
  module_type, scene, env, name, status,
  current_snapshot_id,
  created_by
) VALUES (
  'intervention', 'default', 'prod',
  'intervention-default-prod', 1,
  NULL,
  'system'
);

COMMIT;
