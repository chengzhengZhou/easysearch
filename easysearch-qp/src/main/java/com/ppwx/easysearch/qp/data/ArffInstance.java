package com.ppwx.easysearch.qp.data;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A <tt>ArffInstance</tt> represents an instance
 * of ARFF format input.
 *
 */
public class ArffInstance {
    /** Attributes of the instance */
    public ArrayList<ArffAttribute> attrs;

    /** Data of the instance */
    private ArrayList<Object> instanceData;

    /**
     * Initializes a newly created {@code ArffInstance} object
     * with instance data.
     *
     * @param instanceData
     *              data of the instance
     * @param attrs column name
     */
    public ArffInstance(ArrayList<String> instanceData, ArrayList<ArffAttribute> attrs) {
        this.attrs = attrs;
        transferType(instanceData);
    }

    private void transferType(ArrayList<String> instanceData) {
        this.instanceData = new ArrayList<>(instanceData.size());
        for (int idx = 0; idx < instanceData.size(); idx++) {
            Object res = null;
            if (instanceData.get(idx) != null) {
                switch (getTypeByIndex(idx).toUpperCase()) {
                    case "NUMERIC":
                    case "REAL":
                        res = Double.parseDouble(instanceData.get(idx));
                        break;
                    case "INTEGER":
                        res = Integer.parseInt(instanceData.get(idx));
                        break;
                    case "LONG":
                        res = Long.parseLong(instanceData.get(idx));
                        break;
                    case "STRING":
                        res = instanceData.get(idx);
                        break;
                    case "NOMINAL":
                        String[] data = instanceData.get(idx).split(",");
                        res = new ArrayList<>(Arrays.asList(data));
                        break;
                }
            }
            this.instanceData.add(res);
        }
    }

    /**
     * Get data value by the attribute name.
     *
     * @param attrName
     *              name of the attribute
     *
     * @return  data value
     */
    public Object getValueByAttrName(String attrName) {
        Object res = null;
        boolean isNameValid = false;
        for (ArffAttribute attr : attrs) {
            if (attrName.equals(attr.getName())) {
                res = getValueByIndex(attr.getIndex());
                isNameValid = true;
                break;
            }
        }
        if (isNameValid == false)
            throw new RuntimeException("invalid attrName: " + attrName);
        return res;
    }

    /**
     * Get data value by index.
     *
     * @param idx
     *          index of the data.
     *
     * @return  data value
     */
    public Object getValueByIndex(int idx) {
        return instanceData.get(idx);
    }

    /**
     * Get attribute type by index.
     *
     * @param idx
     *          index of the attribute
     * @return  attribute type
     */
    public String getTypeByIndex(int idx) {
        ArffAttribute attr = attrs.get(idx);
        return attr.getType();
    }

    public void setAttrs(ArrayList<ArffAttribute> attrs) {
        this.attrs = attrs;
    }
}
