package com.ppwx.easysearch.qp.data;

import java.util.List;


public class DictionaryTermOpt extends TermOpt {

    public static final String TYPE_DIC = "dic";

    public static final String TYPE_ID = "id";

    private List<String> word;

    private List<String> nature;

    private List<String> frequency;

    private List<String> id;

    public DictionaryTermOpt() {
    }

    public List<String> getWord() {
        return word;
    }

    public void setWord(List<String> word) {
        this.word = word;
    }

    public List<String> getNature() {
        return nature;
    }

    public void setNature(List<String> nature) {
        this.nature = nature;
    }

    public List<String> getFrequency() {
        return frequency;
    }

    public void setFrequency(List<String> frequency) {
        this.frequency = frequency;
    }

    public List<String> getId() {
        return id;
    }

    public void setId(List<String> id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "DictionaryTermOpt{" +
                "termType='" + termType + '\'' +
                ", opt=" + opt +
                ", word=" + word +
                ", nature=" + nature +
                ", frequency=" + frequency +
                ", id='" + id + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String termType;
        private int opt;
        private List<String> word;
        private List<String> nature;
        private List<String> frequency;
        private List<String> id;

        private Builder() {
        }

        public Builder termType(String termType) {
            this.termType = termType;
            return this;
        }

        public Builder opt(int opt) {
            this.opt = opt;
            return this;
        }

        public Builder word(List<String> word) {
            this.word = word;
            return this;
        }

        public Builder nature(List<String> nature) {
            this.nature = nature;
            return this;
        }

        public Builder frequency(List<String> frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder id(List<String> id) {
            this.id = id;
            return this;
        }

        public DictionaryTermOpt build() {
            DictionaryTermOpt dictionaryTermOpt = new DictionaryTermOpt();
            dictionaryTermOpt.setTermType(this.termType);
            dictionaryTermOpt.setOpt(this.opt);
            dictionaryTermOpt.setWord(this.word);
            dictionaryTermOpt.setNature(this.nature);
            dictionaryTermOpt.setFrequency(this.frequency);
            dictionaryTermOpt.setId(this.id);
            return dictionaryTermOpt;
        }
    }
}