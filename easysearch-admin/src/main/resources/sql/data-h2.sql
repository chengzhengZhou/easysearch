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

-- 4) resource set (id=4) - token 模块
INSERT INTO qp_resource_set (
  id, module_type, scene, env, name, status,
  current_snapshot_id,
  created_by
) VALUES (
  4, 'token', 'default', 'prod',
  'token-default-prod', 1,
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

-- 6) 分词词典示例数据
INSERT INTO qp_rule_token_dict (
  resource_set_id, word, nature, frequency, biz_id, enabled
) VALUES
(4, 'iPhone', 'brand', 1000, NULL, 1),
(4, '华为', 'brand', 1000, NULL, 1),
(4, '小米', 'brand', 1000, NULL, 1),
(4, 'MacBook', 'product', 800, NULL, 1),
(4, 'iPad', 'product', 800, NULL, 1),
(4, '蓝牙耳机', 'product', 600, NULL, 1),
(4, '充电宝', 'product', 600, NULL, 1),
(4, '5G', 'feature', 500, NULL, 1),
(4, 'WiFi', 'feature', 500, NULL, 1),
(4, '快充', 'feature', 500, NULL, 1),
(4, '旗舰机', 'tag', 300, NULL, 1),
(4, '性价比', 'tag', 300, NULL, 1),
(4, '全面屏', 'spec', 400, NULL, 1),
(4, '曲面屏', 'spec', 400, NULL, 1),
(4, '鸿蒙', 'os', 450, NULL, 1);
