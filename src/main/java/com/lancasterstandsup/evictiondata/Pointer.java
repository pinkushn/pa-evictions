package com.lancasterstandsup.evictiondata;

import java.io.Serializable;
import java.util.StringTokenizer;

/**
 * All identifiers needed to restart scrape on a specific record
 */
public class Pointer implements Serializable {
    private int year;
    private String county;
    private String courtOffice;
    private int sequenceNumberUnformatted;

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getCourtOffice() {
        return courtOffice;
    }

    public void setCourtOffice(String courtOffice) {
        this.courtOffice = courtOffice;
    }

    public int getSequenceNumberUnformatted() {
        return sequenceNumberUnformatted;
    }

    public void setSequenceNumberUnformatted(int sequenceNumberUnformatted) {
        this.sequenceNumberUnformatted = sequenceNumberUnformatted;
    }

    public String toString() {
        return county + ", " + courtOffice + ", " + year + ", #" + sequenceNumberUnformatted;
    }

    public boolean hasCourtOffice() {
        return courtOffice != null;
    }

    public Pointer clone() {
        Pointer ret = new Pointer();
        ret.setSequenceNumberUnformatted(sequenceNumberUnformatted);
        ret.setYear(year);
        ret.setCourtOffice(courtOffice);
        ret.setCounty(county);
        return ret;
    }

    public static Pointer fromSerializedPointerString(String s) {
        s = s.replace(",", "");
        String[] split = s.split(" ");
        Pointer ret = new Pointer();
        ret.setCounty(split[0]);
        ret.setCourtOffice(split[1]);
        ret.setYear(Integer.parseInt(split[2]));
        ret.setSequenceNumberUnformatted(Integer.parseInt(split[3].substring(1)));

        return ret;
    }

    public static Pointer fromDocket(String docket, String county) {
        String[] split = docket.split("-");
        Pointer ret = new Pointer();
        ret.setCounty(county);
        ret.setCourtOffice(split[1]);
        ret.setSequenceNumberUnformatted(Integer.parseInt(split[3]));
        ret.setYear(Integer.parseInt(split[4]));
        return ret;
    }
}
