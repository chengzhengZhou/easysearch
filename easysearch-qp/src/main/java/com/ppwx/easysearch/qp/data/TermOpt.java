package com.ppwx.easysearch.qp.data;

import java.io.Serializable;

public class TermOpt  implements Serializable {

    public static final int ADD = 1;

    public static final int UPDATE = 2;

    public static final int DELETE = -1;

    protected int opt;

    protected String termType;

    public int getOpt() {
        return opt;
    }

    public String getTermType() {
        return termType;
    }

    public void setOpt(int opt) {
        this.opt = opt;
    }

    public void setTermType(String termType) {
        this.termType = termType;
    }
}
