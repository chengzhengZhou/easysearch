/*
 * Copyright 2026 chengzhengZhou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ppwx.easysearch.core.util;

import java.util.Map;

/**
 *
 * Get bean from spring ioc container.
 *
 * @since 1.0.0
 */
public class SpringBeanFactory {

    @SuppressWarnings("unchecked")
    public static <T> T getObject(String name) {
        return (T) ContextHolder.getApplicationContext().getBean(name);
    }

    public static <T> T getObject(Class<? extends T> classz) {
        return (T) ContextHolder.getApplicationContext().getBean(classz);
    }

    public static <T> T getObject(String name, Class<? extends T> classz) {
        return (T) ContextHolder.getApplicationContext().getBean(name, classz);
    }

    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        return ContextHolder.getApplicationContext().getBeansOfType(clazz);
    }
}
