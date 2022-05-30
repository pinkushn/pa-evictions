package com.lancasterstandsup.evictiondata;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public abstract class PdfData implements Comparable<PdfData>, Serializable {

    static DateTimeFormatter slashDateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    //static DateTimeFormatter dashDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//

    public static final Comparator<PdfData> DATE_COMPARATOR =
            Comparator.comparing(PdfData::getFileDate)
                    .thenComparing(PdfData::getJudgeName)
                    .thenComparing(PdfData::getDocket);

    public static final Comparator<PdfData> DOCKET_COMPARATOR =
            Comparator.comparing(PdfData::getDocket);

    protected Map<ColumnToken, Object> columns;

    private String hyperlink;

    private Set<String> OTNs;

    public Set<String> getOTNs() {
        return OTNs;
    }

    public boolean hasOTNs() {
        return OTNs != null;
    }

    public void setOTNs(String otns) {
        if (otns.trim().length() == 0) return;
        String[] split = otns.split("/");
        OTNs = new HashSet<>();
        for (String s: split) {
            OTNs.add(s);
        }
        // sometimes the 'two' OTNS are identical (most of the time, perhaps)
        String cellVal = "";
        for (String s: OTNs) {
            if (cellVal.equals("")) {
                cellVal = s;
            }
            else {
                cellVal += ", " + s;
            }
        }
        setColumn(ColumnToken.OTNS, cellVal);
    }

    public void setHyperlink(String s) {
        hyperlink = s;
    }

    public void setColumn(ColumnToken header, Object value) {
        if (columns == null) columns = new HashMap<>();
        columns.put(header, value);
    }

    public String getOtherDockets() throws IOException, InterruptedException {
        if (!hasOTNs()) return "";
        String ret = "";
        for (String otn: getOTNs()) {
            try {
                List<String> others = Scraper.getOTNDocketNames(otn, true, false);
                for (String docket: others) {
                    if (!docket.equals(getDocket())) {
                        if (ret.equals("Unable to procure associated dockets")) {
                            ret = "";
                        }
                        if (ret.equals("")) ret += docket;
                        else ret += ", " + docket;
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                System.err.println("unable to read otn associated dockets for " + getDocket());
                ret = "Unable to procure associated dockets";
            }
        }
        return ret.equals("") ? "No associated dockets" : ret;
    }

    public String getColumn(ColumnToken header) throws IOException, InterruptedException {
        if (columns == null) {
            throw new NullPointerException("no columns for " + getDocket());
        }
        if (header == ColumnToken.OTHER_DOCKETS) {
            return getOtherDockets();
        }
        Object ret = columns.get(header);
        return ret == null ? "" : ret.toString();
    }

    public String[] getRowStrings() throws IOException, InterruptedException {
        String[] ret = new String[getColumnHeaders().size()];
        int col = 0;
        for (ColumnToken ct: getColumnHeaders()) {
            ret[col] = getColumn(ct);
            col++;
        }
        return ret;
    }

    public RowValues getRowValues() throws IOException, InterruptedException {
        RowValues ret = new RowValues();
        ret.setRow(getRowStrings());
        if (hyperlink != null) {
            int i = getColumnHeaders().indexOf(ColumnToken.DOCKET);
            ret.setHyperlink(i, hyperlink);
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

    public int compareTo(PdfData o) {
        return DOCKET_COMPARATOR.compare(this, o);
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
    public static int getDocketNumberAsIntStatic(String docket) {
        return Integer.parseInt(getDocketNumberAsStringStatic(docket));
    }

    //just the 0000001 of CP-36-CR-0000001-2021
    public static String getDocketNumberAsStringStatic(String docket) {
        int i = docket.lastIndexOf('-');
        docket = docket.substring(0, i);
        i = docket.lastIndexOf('-');
        docket = docket.substring(i + 1);
        return docket;
    }

    public boolean hasNoColumns() {
        return columns == null;
    }
}
