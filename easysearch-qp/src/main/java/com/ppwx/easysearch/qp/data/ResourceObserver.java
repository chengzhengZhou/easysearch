package com.ppwx.easysearch.qp.data;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Observable;
import java.util.Observer;

public abstract class ResourceObserver<T> implements Observer {

    private final Class<T> tClass;

    public ResourceObserver() {
        tClass = getTClass();
    }

    private Class<T> getTClass() {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof Class) {
            throw new IllegalArgumentException("Must extend with generic type");
        }
        ParameterizedType parameterizedType = (ParameterizedType) superClass;
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        if (actualTypeArguments[0] instanceof ParameterizedType) {
            return (Class<T>) ((ParameterizedType) actualTypeArguments[0]).getRawType();
        }
        return (Class<T>) actualTypeArguments[0];
    }

    @Override
    public void update(Observable o, Object arg) {
        if (tClass.isInstance(arg) && acceptable((T) arg)) {
            doUpdate((T) arg);
        }
    }

    protected void doUpdate(T arg) {

    }

    protected boolean acceptable(T termOpt) {
        return true;
    }
}
