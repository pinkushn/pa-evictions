package com.lancasterstandsup.evictiondata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class CPPdfData extends PdfData{
    public static final long serialVersionUID = 1;

    private static List<ColumnToken> columnHeaders;
    static {
        columnHeaders = new ArrayList<>();
        columnHeaders.add(ColumnToken.JUDGE);
        columnHeaders.add(ColumnToken.DOCKET);
    }

    public List<ColumnToken> getColumnHeaders() {
        return columnHeaders;
    }

    private boolean lancasterBailFund;
    private String docket;
    private String judge;

    public void setDocket(String docket) {
        this.docket = docket;
        setColumn(ColumnToken.DOCKET, docket);
    }

    public void setJudge(String judge) {
        this.judge = judge;
        setColumn(ColumnToken.JUDGE, judge);
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
        return judge;
    }

    @Override
    LocalDate getFileDate() {
        return null;
    }
}
