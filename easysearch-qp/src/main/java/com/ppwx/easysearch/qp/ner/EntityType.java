package com.ppwx.easysearch.qp.ner;

/**
 * 实体类型枚举
 */
public enum EntityType {
    CATEGORY("类别"),
    BRAND("品牌"),
    MODEL("型号"),
    CPU("处理器"),
    RAM("内存"),
    STORAGE("存储"),
    PRICE("价格"),
    CONDITION("成色"),
    COLOR("颜色"),
    SIZE("尺寸"),
    WEIGHT("重量"),
    BATTERY("电池"),
    CAMERA("摄像头"),
    SCREEN("屏幕"),
    OS("操作系统"),
    NETWORK("网络"),
    INTERFACE("接口"),
    FEATURE("特性"),
    ACCESSORY("配件"),
    WARRANTY("保修"),
    TAG("标签"),
    UNKNOWN("未知");
    
    private final String description;
    
    EntityType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }

    public static EntityType getByName(String name) {
        for (EntityType type : EntityType.values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    /**
     * 判断是否为已定义的实体类型（非 UNKNOWN）
     *
     * @param name 类型名称，如 BRAND、MODEL 或词性 NR、NN
     * @return 若 name 对应枚举中已定义的实体类型返回 true，否则 false
     */
    public static boolean isDefinedEntityType(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return getByName(name) != UNKNOWN;
    }
}

