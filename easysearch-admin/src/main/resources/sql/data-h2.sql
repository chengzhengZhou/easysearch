-- H2-compatible seed data for QP admin (简化版)

-- 1) resource set (id=1) - intervention 模块
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

-- 2) resource set (id=2) - synonym 模块
INSERT INTO qp_resource_set (
  id, module_type, scene, env, name, status,
  current_snapshot_id,
  created_by
) VALUES (
  2, 'synonym', 'default', 'prod',
  'synonym-default-prod', 1,
  NULL,
  'system'
);

-- 3) resource set (id=3) - entity 模块
INSERT INTO qp_resource_set (
  id, module_type, scene, env, name, status,
  current_snapshot_id,
  created_by
) VALUES (
  3, 'entity', 'default', 'prod',
  'entity-default-prod', 1,
  NULL,
  'system'
);

-- 4) 同义词示例数据
INSERT INTO qp_rule_synonym (
  resource_set_id, source_text, direction, targets_json, enabled, remark
) VALUES
(2, '手机', '=>', '["智能手机","移动电话","电话"]', 1, '手机同义词'),
(2, '电脑', 'SYM', '["计算机","PC","个人电脑"]', 1, '电脑双向同义词'),
(2, '好', '=>', '["优秀","棒","不错","赞"]', 1, '正向评价词');

-- 5) 实体词典示例数据
INSERT INTO qp_rule_entity (
  resource_set_id, entity_text, entity_type, normalized_value, aliases_json, attributes_json, relations_json, ids_json, enabled
) VALUES
(3, '苹果', 'BRAND', 'Apple', '["Apple","APPLE"]', '{"country":"美国","founded":"1976"}', '{}', '[]', 1),
(3, '华为', 'BRAND', 'HUAWEI', '["HUAWEI","Huawei"]', '{"country":"中国","founded":"1987"}', '{}', '[]', 1),
(3, '小米', 'BRAND', 'Xiaomi', '["MI","Xiaomi"]', '{"country":"中国","founded":"2010"}', '{}', '[]', 1),
(3, 'iPhone 15', 'MODEL', 'iPhone 15', '["iPhone15","苹果15"]', '{"series":"iPhone","year":"2023"}', '{"brand":"Apple"}', '[]', 1),
(3, '128GB', 'STORAGE', '128GB', '["128G","128gb"]', '{"unit":"GB","value":128}', '{}', '[]', 1),
(3, '白色', 'COLOR', '白色', '["白","纯白","snow white"]', '{}', '{}', '[]', 1),
(3, '智能手机', 'CATEGORY', '智能手机', '["手机","移动电话","cell phone"]', '{}', '{}', '[]', 1);
