package com.ppwx.easysearch.qp.eval;

import com.google.common.collect.Maps;
import com.ppwx.easysearch.qp.data.MetaTermOpt;
import com.ppwx.easysearch.qp.ner.Entity;
import com.ppwx.easysearch.qp.ner.EntityType;
import com.ppwx.easysearch.qp.prediction.Category;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import static com.ppwx.easysearch.qp.data.MetaTermOpt.*;
import static com.ppwx.easysearch.qp.data.MetaTermOpt.MODEL;

public class MetaObserver implements Observer {

    private final Map<String, MetaTermOpt> categoryIdIndex;
    private final Map<String, MetaTermOpt> brandIdIndex;
    private final Map<String, MetaTermOpt> modelIdIndex;

    public MetaObserver() {
        categoryIdIndex = Maps.newHashMap();
        brandIdIndex = Maps.newHashMap();
        modelIdIndex = Maps.newHashMap();
    }

    @Override
    public void update(Observable o, Object arg) {
        MetaTermOpt metaTermOpt = (MetaTermOpt) arg;
        if (MetaTermOpt.DELETE == metaTermOpt.getOpt()) {
            switch (metaTermOpt.getTermType()) {
                case CATEGORY:
                    categoryIdIndex.remove(metaTermOpt.getCategoryId());
                    break;
                case BRAND:
                    brandIdIndex.remove(metaTermOpt.getBrandId());
                    break;
                case MODEL:
                    modelIdIndex.remove(metaTermOpt.getModelId());
                    break;
                default:
                    break;
            }
        } else {
            switch (metaTermOpt.getTermType()) {
                case CATEGORY:
                    categoryIdIndex.put(metaTermOpt.getCategoryId(), metaTermOpt);
                    break;
                case BRAND:
                    brandIdIndex.put(metaTermOpt.getBrandId(), metaTermOpt);
                    break;
                case MODEL:
                    modelIdIndex.put(metaTermOpt.getModelId(), metaTermOpt);
                    break;
                default:
                    break;
            }
        }
    }

    public String getCategoryName(String categoryId) {
        MetaTermOpt metaTermOpt = categoryIdIndex.get(categoryId);
        return metaTermOpt == null ? null : metaTermOpt.getCategoryName();
    }

    public String getBrandName(String brandId) {
        MetaTermOpt metaTermOpt = brandIdIndex.get(brandId);
        return metaTermOpt == null ? null : metaTermOpt.getBrandName();
    }

    public String getModelName(String modelId) {
        MetaTermOpt metaTermOpt = modelIdIndex.get(modelId);
        return metaTermOpt == null ? null : metaTermOpt.getModelName();
    }

    public MetaTermOpt getMetaTermOpt(Entity entity) {
        if (CollectionUtils.isEmpty(entity.getId())) return null;
        switch (entity.getType()) {
            case CATEGORY:
                return categoryIdIndex.get(entity.getId().get(0));
            case BRAND:
                return brandIdIndex.get(entity.getId().get(0));
            case MODEL:
                return modelIdIndex.get(entity.getId().get(0));
            default:
                return null;
        }
    }
}
