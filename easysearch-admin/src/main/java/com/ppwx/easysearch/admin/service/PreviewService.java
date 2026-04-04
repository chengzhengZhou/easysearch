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

package com.ppwx.easysearch.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ppwx.easysearch.admin.domain.enums.InterventionMode;
import com.ppwx.easysearch.admin.domain.enums.RuleModule;
import com.ppwx.easysearch.admin.domain.model.*;
import com.ppwx.easysearch.admin.mapper.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PreviewService {
    private final InterventionSentenceRuleMapper sentenceRuleMapper;
    private final InterventionTermRuleMapper termRuleMapper;
    private final SynonymRuleMapper synonymRuleMapper;
    private final EntityRuleMapper entityRuleMapper;
    private final TokenDictRuleMapper tokenDictRuleMapper;

    public PreviewService(InterventionSentenceRuleMapper sentenceRuleMapper,
                          InterventionTermRuleMapper termRuleMapper,
                          SynonymRuleMapper synonymRuleMapper,
                          EntityRuleMapper entityRuleMapper,
                          TokenDictRuleMapper tokenDictRuleMapper) {
        this.sentenceRuleMapper = sentenceRuleMapper;
        this.termRuleMapper = termRuleMapper;
        this.synonymRuleMapper = synonymRuleMapper;
        this.entityRuleMapper = entityRuleMapper;
        this.tokenDictRuleMapper = tokenDictRuleMapper;
    }

    public PreviewResult preview(Long resourceSetId, RuleModule module, InterventionMode mode, String query) {
        String q = query == null ? "" : query;
        List<String> hits = new ArrayList<>();
        if (module == RuleModule.intervention) {
            InterventionMode m = mode == null ? InterventionMode.sentence : mode;
            if (m == InterventionMode.sentence) {
                List<InterventionSentenceRuleDO> rules = sentenceRuleMapper.selectList(new LambdaQueryWrapper<InterventionSentenceRuleDO>().eq(InterventionSentenceRuleDO::getResourceSetId, resourceSetId));
                for (InterventionSentenceRuleDO r : rules) {
                    if (r.getSourceText() != null && q.contains(r.getSourceText())) {
                        hits.add("source=" + r.getSourceText() + " -> " + r.getTargetText());
                    }
                }
            } else {
                List<InterventionTermRuleDO> rules = termRuleMapper.selectList(new LambdaQueryWrapper<InterventionTermRuleDO>().eq(InterventionTermRuleDO::getResourceSetId, resourceSetId));
                for (InterventionTermRuleDO r : rules) {
                    if (r.getSourceText() != null && q.contains(r.getSourceText())) {
                        hits.add("source=" + r.getSourceText() + " -> " + r.getTargetText());
                    }
                }
            }
            return PreviewResult.of(q, q, hits);
        }
        if (module == RuleModule.synonym) {
            List<SynonymRuleDO> rules = synonymRuleMapper.selectList(new LambdaQueryWrapper<SynonymRuleDO>().eq(SynonymRuleDO::getResourceSetId, resourceSetId));
            for (SynonymRuleDO r : rules) {
                if (r.getSourceText() != null && q.contains(r.getSourceText())) {
                    hits.add("source=" + r.getSourceText() + " " + r.getDirection() + " " + r.getTargetsJson());
                }
            }
            return PreviewResult.of(q, q, hits);
        }
        if (module == RuleModule.entity) {
            List<EntityRuleDO> rules = entityRuleMapper.selectList(new LambdaQueryWrapper<EntityRuleDO>().eq(EntityRuleDO::getResourceSetId, resourceSetId));
            for (EntityRuleDO r : rules) {
                if (r.getEntityText() != null && q.contains(r.getEntityText())) {
                    hits.add("entity=" + r.getEntityText() + " type=" + r.getEntityType() + " norm=" + r.getNormalizedValue());
                }
            }
            return PreviewResult.of(q, q, hits);
        }
        if (module == RuleModule.token) {
            List<TokenDictRuleDO> rules = tokenDictRuleMapper.selectList(new LambdaQueryWrapper<TokenDictRuleDO>().eq(TokenDictRuleDO::getResourceSetId, resourceSetId));
            for (TokenDictRuleDO r : rules) {
                if (r.getWord() != null && q.contains(r.getWord())) {
                    hits.add("word=" + r.getWord() + "/" + r.getNature());
                }
            }
            return PreviewResult.of(q, q, hits);
        }
        return PreviewResult.of(q, q, hits);
    }

    public static class PreviewResult {
        private String input;
        private String output;
        private List<String> hits;

        public static PreviewResult of(String input, String output, List<String> hits) {
            PreviewResult r = new PreviewResult();
            r.input = input;
            r.output = output;
            r.hits = hits;
            return r;
        }

        public String getInput() {
            return input;
        }

        public void setInput(String input) {
            this.input = input;
        }

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
            this.output = output;
        }

        public List<String> getHits() {
            return hits;
        }

        public void setHits(List<String> hits) {
            this.hits = hits;
        }
    }
}

