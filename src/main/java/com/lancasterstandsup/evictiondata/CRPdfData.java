package com.lancasterstandsup.evictiondata;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.TreeMap;

public class CRPdfData extends PdfData {

    private TreeMap<Integer, String> row = new TreeMap<>();

    private String courtOffice;
    private String docketNumber;
    private String judgeName;
    private String fileDate;
    private LocalDate comparableDate = null;

    //CASE INFORMATION
    private String judgeAssigned;
    private LocalDate issueDate;
    private String OTN;
    //private LocalDate fileDate;
    private String arrestingAgency;
    private LocalDate arrestDate;
    //sometimes has letters
    private String complaintNumber;
    private String incidentNumber;
    private String caseStatus;
    private String disposition;
    private LocalDate dispositionDate;
    private String county;
    private String township;

    private boolean unableToPostBail;
    private LocalDate startConfinement;
    private LocalDate endConfinement;
    private Integer bail;

    private String defendantName;

    @Override
    public boolean rescrape(LocalDateTime lastCheck) {
        return false;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    public void setCourtOffice(String courtOffice) {
        this.courtOffice = courtOffice;
        row.put(0, courtOffice);
    }

    @Override
    String getCourtOffice() {
        return courtOffice;
    }

    public void setJudgeName(String judgeName) {
        this.judgeName = judgeName;
        row.put(1, judgeName);
    }

    public String getJudgeName() {
        return judgeName;
    }

    public void setDocketNumber(String docketNumber) {
        this.docketNumber = docketNumber;
        row.put(2, docketNumber);
    }

    @Override
    String getDocketNumber() {
        return docketNumber;
    }

    public void setFileDate(String string) {
        this.fileDate = string;
        row.put(3, string);
    }

    public LocalDate getFileDate() {
        if (comparableDate != null) return comparableDate;

        comparableDate = LocalDate.parse(fileDate, dateFormatter);

        return comparableDate;
    }
    public String getJudgeAssigned() {
        return judgeAssigned;
    }

    public void setJudgeAssigned(String judgeAssigned) {
        this.judgeAssigned = judgeAssigned;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(String issueDate) {
        try {
            this.issueDate = LocalDate.parse(issueDate, dateFormatter);
        }
        catch (Exception e) {}
    }

    public String getOTN() {
        return OTN;
    }

    public void setOTN(String OTN) {
        this.OTN = OTN;
    }

    public String getArrestingAgency() {
        return arrestingAgency;
    }

    public void setArrestingAgency(String arrestingAgency) {
        this.arrestingAgency = arrestingAgency;
    }

    public LocalDate getArrestDate() {
        return arrestDate;
    }

    public void setArrestDate(String arrestDate) {
        try {
            this.arrestDate = LocalDate.parse(arrestDate, dateFormatter);
        }
        catch (Exception e) {}
    }

    public String getComplaintNumber() {
        return complaintNumber;
    }

    public void setComplaintNumber(String complaintNumber) {
        this.complaintNumber = complaintNumber;
    }

    public String getIncidentNumber() {
        return incidentNumber;
    }

    public void setIncidentNumber(String incidentNumber) {
        this.incidentNumber = incidentNumber;
    }

    public String getTownship() {
        return township;
    }

    public void setTownship(String township) {
        this.township = township;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getCaseStatus() {
        return caseStatus;
    }

    public void setCaseStatus(String caseStatus) {
        this.caseStatus = caseStatus;
    }

    public String toString() {
        return docketNumber;
    }

    public String getDisposition() {
        return disposition;
    }

    public void setDisposition(String disposition) {
        this.disposition = disposition;
    }

    public LocalDate getDispositionDate() {
        return dispositionDate;
    }

    public void setDispositionDate(String dispositionDate) {
        try {
            this.dispositionDate = LocalDate.parse(dispositionDate, dateFormatter);
        }
        catch (Exception e) {}
    }
    public boolean isUnableToPostBail() {
        return unableToPostBail;
    }

    public void setUnableToPostBail(boolean unableToPostBail) {
        this.unableToPostBail = unableToPostBail;
    }

    public boolean hasStartConfinement() {
        return startConfinement != null;
    }

    public LocalDate getStartConfinement() {
        return startConfinement;
    }

    public void setStartConfinement(String startConfinement) {
        try {
            this.startConfinement = LocalDate.parse(startConfinement, dateFormatter);
        }
        catch (Exception e) {}
    }

    public boolean hasEndConfinement() {
        return endConfinement != null;
    }

    public LocalDate getEndConfinement() {
        return endConfinement;
    }

    public void setEndConfinement(String endConfinement) {
        try {
            this.endConfinement = LocalDate.parse(endConfinement, dateFormatter);
        }
        catch (Exception e) {}
    }

    public void setBail(int bail) {
        this.bail = bail;
    }

    public boolean hasBail() {
        return bail != null;
    }

    public Integer getBail() {
        return bail;
    }

    public String getStoredURL() throws IOException {
        return Scraper.getStoredURL(Scraper.Mode.MDJ_CR, county, docketNumber.substring(1 + docketNumber.lastIndexOf('-')), docketNumber);
    }

    public String getDefendantName() {
        return defendantName;
    }

    public void setDefendantName(String defendantName) {
        this.defendantName = defendantName;
    }
}
