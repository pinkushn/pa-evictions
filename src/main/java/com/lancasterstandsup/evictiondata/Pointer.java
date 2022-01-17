package com.lancasterstandsup.evictiondata;

import java.io.Serializable;

/**
 * All identifiers needed to restart scrape on a specific record
 */
public class Pointer implements Serializable {
    private int year;
    private String county;
    private String courthouse;
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

    public String getCourthouse() {
        return courthouse;
    }

    public void setCourthouse(String courthouse) {
        this.courthouse = courthouse;
    }

    public int getSequenceNumberUnformatted() {
        return sequenceNumberUnformatted;
    }

    public void setSequenceNumberUnformatted(int sequenceNumberUnformatted) {
        this.sequenceNumberUnformatted = sequenceNumberUnformatted;
    }

    public String toString() {
        return county + ", " + courthouse + ", " + year + ", #" + sequenceNumberUnformatted;
    }
}
