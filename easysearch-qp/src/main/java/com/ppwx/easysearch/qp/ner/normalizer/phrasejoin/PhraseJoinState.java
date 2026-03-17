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

package com.ppwx.easysearch.qp.ner.normalizer.phrasejoin;

/**
 * 词组转移状态机的状态。
 */
public enum PhraseJoinState {
    /** 尚未输出或上一可合并块已结束 */
    START,
    /** 刚输出到 ATOM 结尾 */
    AFTER_ATOM,
    /** 刚输出 CONNECTOR，下一 token 应为 ATOM */
    AFTER_CONNECTOR,
    /** 终止，不再消费 token */
    END
}
