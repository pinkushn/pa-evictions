package com.lancasterstandsup.evictiondata;

import org.apache.poi.ss.usermodel.Hyperlink;

import java.util.HashMap;
import java.util.Map;

public class RowValues {
    String [] vals;
    Map<Integer, String> links;

    public void setRow(String [] row) {
        vals = row;
;   }

    public String[] getRow() {
        return vals;
    }

    public String getHyperlink(int i) {
        return links == null ? null : links.get(i);
    }

    public void setHyperlink(int i, String hyperlink) {
        if (links == null) links = new HashMap<>();
        links.put(i, hyperlink);
    }
}
