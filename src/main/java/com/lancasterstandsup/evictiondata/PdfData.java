package com.lancasterstandsup.evictiondata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public abstract class PdfData implements Comparable<PdfData>{

    static DateTimeFormatter slashDateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    static DateTimeFormatter dashDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Comparator<PdfData> COMPARATOR =
            Comparator.comparing(PdfData::getFileDate)
                    .thenComparing(PdfData::getJudgeName)
                    .thenComparing(PdfData::getDocket);

    public static LocalDate forceSlashedDateIntoLocalDate(String date) {
        String[] split = date.split("/");
        if (split[0].length() < 2) split[0] = "0" + split[0];
        if (split[1].length() < 2) split[1] = "0" + split[1];
        String useMe = split[0] + "/" + split[1] + "/" + split[2];
        return LocalDate.parse(useMe, slashDateFormatter);
    }

    public static String getYearFromDocket(String docket) {
        int i = docket.lastIndexOf("-");
        return docket.substring(i + 1);
    }

    public static String getJBFileName(String docket) {
        int i = docket.indexOf('-');
        String ret = docket.substring(i + 1);
        i = ret.indexOf("0");
        while (i == 0) {
            ret = ret.substring(1);
            i = ret.indexOf("0");
        }
        i = ret.indexOf('-');
        String courtOffice = ret.substring(0, i);
        ret = ret.substring(i + 1);
        i = ret.indexOf('-');
        ret = ret.substring(i + 1);
        ret = courtOffice + "-" + ret;
        return ret.replace('-', '_');
    }

    private Map<ColumnToken, Object> columns;

    public void setColumn(ColumnToken header, Object value) {
        if (columns == null) columns = new HashMap<>();
        columns.put(header, value);
    }

//    public Object getColumn(ColumnToken header) {
//        return columns.get(header);
//    }

    public String[] getRow() {
        String[] ret = new String[getColumnHeaders().size()];
        int col = 0;
        for (ColumnToken ct: getColumnHeaders()) {
            Object o = columns.get(ct);
            ret[col] = o == null ? "" : o.toString();
            col++;
        }
        return ret;
    }

    abstract List<ColumnToken> getColumnHeaders();
    abstract boolean rescrape(LocalDateTime lastCheck);
    abstract boolean isClosed();
    abstract String getDocket();
    abstract String getCourtOffice();
    abstract String getJudgeName();
    abstract LocalDate getFileDate();

    public int getDocketNumberAsInt() {
        return getDocketNumberAsIntStatic(getDocket());
    }

    //just the 0000001 of CP-36-CR-0000001-2021
    public String getDocketNumberAsString() {
        return getDocketNumberAsStringStatic(getDocket());
    }

    public static int getDocketNumberAsIntStatic(String docket) {
        return Integer.parseInt(docket);
    }

    //just the 0000001 of CP-36-CR-0000001-2021
    public static String getDocketNumberAsStringStatic(String docket) {
        int i = docket.lastIndexOf('-');
        docket = docket.substring(0, i);
        i = docket.lastIndexOf('-');
        docket = docket.substring(i + 1);
        return docket;
    }

    public static String getCourtOfficeFromDocket(String docket) {
        int i = docket.indexOf('-');
        docket = docket.substring(i);
        i = docket.indexOf('-');
        docket = docket.substring(0, i);
        return docket;
    }

    public static Scraper.CourtMode getCourtModeFromDocket(String docket) {
        if (docket.indexOf("MJ") > -1) {
            return docket.indexOf("LT") > -1 ? Scraper.CourtMode.MDJ_LT : Scraper.CourtMode.MDJ_CR;
        }
        return Scraper.CourtMode.CP_CR;
    }

    public int compareTo(PdfData o) {
        return COMPARATOR.compare(this, o);
    }

//    /**
//     *
//     * @param slashDate must be formatted MM/dd/yyyy
//     * @return date formattted yyyy-MM-dd, intended for portal format requirement
//     * when using birthdate as part of person search
//     */
//    public static String convertSlashDateToDashDate(String slashDate) {
//        LocalDate date = LocalDate.parse(slashDate, slashDateFormatter);
//        date.format(dashDateFormatter);
//        return date.toString();
//    }
}
