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
 * Token 在词组转移状态机中的分类。
 * 用于驱动状态转移，支持按 Profile 将 token 归为 ATOM、CONNECTOR 等。
 */
public enum TokenClass {
    /** 主体片段：字母/数字/混合，如 matebook、16、60mm、Type、C */
    ATOM,
    /** 连接符：+、-、/、.、_ 等，需与前后 ATOM 无空格拼接 */
    CONNECTOR,
    /** 左括号：(、（、[ */
    LP,
    /** 右括号：)、）、] */
    RP,
    /** 纯空白 token */
    SPACE,
    /** 其它（中文词、标点等），通常触发 STOP */
    OTHER
}
