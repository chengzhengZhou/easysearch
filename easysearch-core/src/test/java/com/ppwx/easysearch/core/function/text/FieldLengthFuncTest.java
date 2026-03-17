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

package com.ppwx.easysearch.core.function.text;

import com.ppwx.easysearch.core.data.element.StringColumn;
import org.junit.Test;

/**
 *
 * FieldLengthFunc
 *
 * @since 1.0.0
 */
public class FieldLengthFuncTest {

    @Test
    public void testFieldLengthFuncWorks() {
        FieldLengthFunc fieldLengthFunc = new FieldLengthFunc();
        Integer score = fieldLengthFunc.apply(new StringColumn("我爱我的中国"));
        System.out.println(score);
    }

}