package com.lancasterstandsup.evictiondata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.TreeMap;

public class CPPdfData extends PdfData{

    //private TreeMap<Integer, String> row = new TreeMap<>();

    private boolean lancasterBailFund;
    private String docket;

    public void setDocket(String docket) {
        this.docket = docket;
        //row.put(2, docket);
    }

    @Override
    String getDocket() {
        return docket;
    }

    public boolean isLancasterBailFund() {
        return lancasterBailFund;
    }

    public void setLancasterBailFund(boolean lancasterBailFund) {
        this.lancasterBailFund = lancasterBailFund;
    }

    @Override
    boolean rescrape(LocalDateTime lastCheck) {
        return false;
    }

    @Override
    boolean isClosed() {
        return false;
    }

    @Override
    String getCourtOffice() {
        return null;
    }

    @Override
    String getJudgeName() {
        return null;
    }

    @Override
    LocalDate getFileDate() {
        return null;
    }
}
