package com.ppwx.easysearch.qp.data;

import java.util.Collection;

public final class Joiner {

    public static String join(String delimiter, String... args) {
        return String.join(delimiter, args);
    }

    public static String join(String delimiter, Collection<String> collection) {
        return String.join(delimiter, collection);
    }

}
