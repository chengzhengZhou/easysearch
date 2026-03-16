package com.ppwx.easysearch.qp.data;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ext.ahs.zhouchzh1@jd.com
 * @className ArffSourceConvertor
 * @description Arff格式文件
 * @date 2024/10/11 19:28
 **/
public class ArffSourceConvertor extends AbstractFileSourceConvertor{
    /**
     * data part mark
     */
    boolean dataFlag = false;

    /** The relation name of input data */
    private String relationName;

    /** The attribute types of the input data */
    private ArrayList<String> attrTypes;

    /** The attributes the input data */
    private ArrayList<ArffAttribute> attributes;

    /** The instances of the input data */
    private ArrayList<ArffInstance> instances;

    /** The column ids of the input data */
    private ArrayList<BiMap<String, Integer>> columnIds;

    public ArffSourceConvertor(String inputDataPath) {
        super(inputDataPath);
        attributes = new ArrayList<>();
        columnIds = new ArrayList<>();
        attrTypes = new ArrayList<>();
        instances = new ArrayList<>();
    }

    @Override
    public void readData() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(getSourceStream(), StandardCharsets.UTF_8));
        boolean dataFlag = false;

        int attrIdx = 0;

        String attrName = null;
        String attrType = null;
        String line = null;

        while (true) {

            // parse DATA if valid
            if (dataFlag) {
                // get all attribute types
                for (ArffAttribute attr : attributes) {
                    attrTypes.add(attr.getType());
                }
                // let data reader control the bufferedReader
                dataReader(br);
            }

            line = br.readLine();

            if (line == null) // finish reading
                break;
            if (line.isEmpty() || line.startsWith("%")) // skip empty or
                // annotation
                continue;

            String[] data = line.trim().split("[ \t]");

            // parse RELATION
            if (data[0].toUpperCase().equals("@RELATION")) {
                relationName = data[1];
            }

            // parse ATTRIBUTE
            else if (data[0].toUpperCase().equals("@ATTRIBUTE")) {
                attrName = data[1];
                attrType = data[2];
                boolean isNominal = false;
                // parse NOMINAL type
                if (attrType.startsWith("{") && attrType.endsWith("}")) {
                    isNominal = true;
                }
                BiMap<String, Integer> colId = HashBiMap.create();
                // if nominal type, set columnIds
                if (isNominal) {
                    String nominalAttrs = attrType.substring(1, attrType.length() - 1);
                    int val = 0;
                    for (String attr : nominalAttrs.split(",")) {
                        colId.put(attr.trim(), val++);
                    }
                    attrType = "NOMINAL";
                }
                columnIds.add(colId);
                attributes.add(new ArffAttribute(attrName, attrType.toUpperCase(), attrIdx++));
            }
            // set DATA flag (finish reading ATTRIBUTES)
            else if (data[0].toUpperCase().equals("@DATA")) {
                dataFlag = true;
            }
        }
        br.close();

        // initialize attributes
        for (int i = 0; i < attributes.size(); i++) {
            attributes.get(i).setColumnSet(columnIds.get(i).keySet());
        }
    }

    /**
     * Parse @DATA part of the file.
     *
     * @param rd  the reader of the input file.
     * @throws IOException
     */
    private void dataReader(Reader rd) throws IOException {
        ArrayList<String> dataLine = new ArrayList<>();
        StringBuilder subString = new StringBuilder();
        boolean isInQuote = false;
        boolean isInBracket = false;

        int c = 0;
        while ((c = rd.read()) != -1) {
            char ch = (char) c;
            // read line by line
            if (ch == '\n') {
                if (dataLine.size() != 0) { // check if empty line
                    if (!dataLine.get(0).startsWith("%")) { // check if
                        // annotation line
                        dataLine.add(subString.toString());
                        // raise error if inconsistent with attribute define
                        if (dataLine.size() != attrTypes.size()) {
                            throw new IOException("Read data error, inconsistent attribute number!");
                        }

                        // pul column value into columnIds, for one-hot encoding
                        for (int i = 0; i < dataLine.size(); i++) {
                            String col = dataLine.get(i).trim();
                            String type = attrTypes.get(i);
                            BiMap<String, Integer> colId = columnIds.get(i);
                            switch (type) {
                                case "NUMERIC":
                                case "REAL":
                                case "INTEGER":
                                case "LONG":
                                    break;
                                case "STRING":
                                    int val = colId.containsKey(col) ? colId.get(col) : colId.size();
                                    colId.put(col, val);
                                    break;
                                case "NOMINAL":
                                    StringBuilder sb = new StringBuilder();
                                    String[] ss = col.split(",");
                                    for (int ns = 0; ns < ss.length; ns++) {
                                        String _s = ss[ns].trim();
                                        if (!colId.containsKey(_s)) {
                                            throw new IOException("Read data error, inconsistent nominal value!");
                                        }
                                        sb.append(_s);
                                        if (ns != ss.length - 1)
                                            sb.append(",");
                                    }
                                    col = sb.toString();
                                    break;
                            }
                            dataLine.set(i, col);
                        }

                        instances.add(new ArffInstance(dataLine, attributes));

                        subString = new StringBuilder();
                        dataLine = new ArrayList<>();
                    }
                }
            } else if (ch == '[' || ch == ']') {
                isInBracket = !isInBracket;
            } else if (ch == '\r') {
                // skip '\r'
            } else if (ch == '\"') {
                isInQuote = !isInQuote;
            } else if (ch == ',' && (!isInQuote && !isInBracket)) {
                dataLine.add(subString.toString());
                subString = new StringBuilder();
            } else {
                subString.append(ch);
            }
        }
    }

    public String getRelationName() {
        return relationName;
    }

    public List<ArffInstance> getInstances() {
        return instances;
    }
}
