package com.ppwx.easysearch.qp.ner;

import com.ppwx.easysearch.qp.data.DictionaryTermOpt;
import com.ppwx.easysearch.qp.data.ResourceObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于词典的简单id映射
 * 使用关键词精确匹配
 */
public class DefaultEntityIdentityMapper extends ResourceObserver<DictionaryTermOpt> implements EntityIdentityMapper {

    private static final Map<EntityType, Map<String, IdentityItem>> bucket = new ConcurrentHashMap<>(16);

    static {
        for (EntityType type : EntityType.values()) {
            bucket.put(type, new HashMap<>());
        }
    }

    @Override
    public List<String> map(EntityType type, String word, String normalizedValue) {
        Map<String, IdentityItem> itemMap = bucket.get(type);
        if (itemMap != null) {
            if (itemMap.containsKey(word)) {
                return itemMap.get(word).getId();
            } else if (itemMap.containsKey(normalizedValue)) {
                return itemMap.get(normalizedValue).getId();
            } else if (itemMap.containsKey(noSpace(word))) {
                return itemMap.get(noSpace(word)).getId();
            }
        }
        return null;
    }

    @Override
    protected void doUpdate(DictionaryTermOpt termOpt) {
        if (DictionaryTermOpt.DELETE == termOpt.getOpt()) {
            deleteItem(termOpt);
        } else {
            addItem(termOpt);
        }
    }

    protected void deleteItem(DictionaryTermOpt termOpt) {
        List<IdentityItem> items = getItems(termOpt);
        for (IdentityItem item : items) {
            Map<String, IdentityItem> itemMap = bucket.get(item.getType());
            synchronized (itemMap) {
                if (itemMap.containsKey(item.getName())) {
                    List<String> ids = itemMap.get(item.getName()).getId();
                    item.getId().forEach(ids::remove);
                    if (ids.isEmpty()) {
                        itemMap.remove(item.getName());
                    }
                }
            }
        }
    }

    protected void addItem(DictionaryTermOpt termOpt) {
        List<IdentityItem> items = getItems(termOpt);
        for (IdentityItem item : items) {
            Map<String, IdentityItem> itemMap = bucket.get(item.getType());
            synchronized (itemMap) {
                itemMap.compute(item.getName(), (k, v) -> {
                    if (v == null) {
                        return item;
                    } else {
                        List<String> ids = v.getId();
                        item.getId().forEach(id -> {
                            if (!ids.contains(id)) {
                                ids.add(id);
                            }
                        });
                        return v;
                    }
                });
            }
        }
    }

    private List<IdentityItem> getItems(DictionaryTermOpt termOpt) {
        List<IdentityItem> items = new ArrayList<>();
        for (String nature :termOpt.getNature()) {
            EntityType entityType = EntityType.getByName(nature);
            for (String word : termOpt.getWord()) {
                items.add(new IdentityItem(termOpt.getId().stream().distinct().collect(Collectors.toList()),
                        word, entityType));
                String noSpace = noSpace(word);
                // no space word
                if (!noSpace.equals(word)) {
                    items.add(new IdentityItem(termOpt.getId().stream().distinct().collect(Collectors.toList()),
                            noSpace, entityType));
                }
            }
        }
        return items;
    }

    @Override
    protected boolean acceptable(DictionaryTermOpt termOpt) {
        if (termOpt.getId() == null || termOpt.getId().isEmpty()) {
            return false;
        }
        return DictionaryTermOpt.TYPE_DIC.equals(termOpt.getTermType())
                || DictionaryTermOpt.TYPE_ID.equals(termOpt.getTermType());
    }

    /**
     * 去空格
     */
    private String noSpace(String word) {
        return word.replaceAll("\\s+", "").trim();
    }

    private static class IdentityItem {
        private final List<String> id;
        private final String name;
        private final EntityType type;

        public IdentityItem(List<String> id, String name, EntityType type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }

        public List<String> getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public EntityType getType() {
            return type;
        }
    }
}
