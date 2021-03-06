package com.lancasterstandsup.evictiondata;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class CRPdfData extends PdfData {
    public static final long serialVersionUID = 1;

    public enum BAIL_TYPE {
        MONETARY("Monetary", true),
        NOMINAL("Nominal", true),
        NON_MONETARY("Nonmonetary", false),
        ROR("ROR", false),
        UNSECURED("Unsecured", false);

        String desc;
        boolean secured;

        BAIL_TYPE(String match, boolean secured) {
            desc = match;
            this.secured = secured;
        }

        public String toString() {
            return desc;
        }

        public boolean isSecured() {
            return secured;
        }
    }

    private static Map<String, BAIL_TYPE> bailTypes;
    static {
        bailTypes = new TreeMap<>();
        for (BAIL_TYPE bt: BAIL_TYPE.values()) {
            bailTypes.put(bt.toString(), bt);
        }
    }

    /**
     *
     * @param match
     * @return null if no match
     */
    public static BAIL_TYPE getBailType(String match) {
        BAIL_TYPE ret = bailTypes.get(match);
        return ret;
    }

    //        Felony (1st degree) (F1)
//                Felony (2nd degree) (F2)
//                Felony (3rd degree) (F3)
//                Ungraded Felony (F3)
//        Misdemeanor (1st degree)(M1)
//                Misdemeanor (2nd degree)(M2)
//                Misdemeanor (3rd degree)(M3)
//                Ungraded Misdemeanor (Same as M3)
//        Summary Offenses
    public enum GRADE {
        F1,
        F2,
        F3,
        F,
        M1,
        M2,
        M3,
        M,
        S,
        NOT_SPECIFIED
    }

    private static List<ColumnToken> columnHeaders;
    static {
        columnHeaders = new ArrayList<>();
        columnHeaders.add(ColumnToken.DOCKET);
        columnHeaders.add(ColumnToken.PAIRED_MDJS_SAME_COUNTY);
        columnHeaders.add(ColumnToken.JUDGE);
        columnHeaders.add(ColumnToken.FILE_DATE);
        columnHeaders.add(ColumnToken.HAS_BAIL_SECTION);
        columnHeaders.add(ColumnToken.BAIL_TYPE);
        columnHeaders.add(ColumnToken.BAIL_PERCENTAGE);
        columnHeaders.add(ColumnToken.BAIL);
        columnHeaders.add(ColumnToken.UNABLE_TO_PAY_BAIL);
        columnHeaders.add(ColumnToken.MOST_SERIOUS_GRADE);
        columnHeaders.add(ColumnToken.GRADES);
        columnHeaders.add(ColumnToken.OTNS);
        columnHeaders.add(ColumnToken.OTHER_DOCKETS);
    }

    public List<ColumnToken> getColumnHeaders() {
        return columnHeaders;
    }

    private String courtOffice;
    private String docket;
    private String fileDate;
    private LocalDate comparableDate = null;

    //CASE INFORMATION
    private String judgeAssigned;
    private LocalDate issueDate;
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
    private boolean hasBailSection;
    private String bailPercent;
    private Integer bail;
    private BAIL_TYPE bailType;

    private String defendantName;
    private LocalDate birthdate;

    // severity of each charge
    private List<GRADE> grades;

    private int countPairedMDJSSameCounty;

    public void setPairedMDJsSameCounty(int i) {
        countPairedMDJSSameCounty = i;
        setColumn(ColumnToken.PAIRED_MDJS_SAME_COUNTY, countPairedMDJSSameCounty);
    }

    public boolean isSecuredCashBail() {

        if (!hasBailType()) {
            return false;
        }

        return bailType.isSecured();
    }

    public void addGrade(GRADE grade) {
        if (grades == null) grades = new ArrayList<>();
        grades.add(grade);
    }

    public GRADE getMostSeriousGrade() {
        if (grades == null || grades.contains(GRADE.NOT_SPECIFIED)) {
            return GRADE.NOT_SPECIFIED;
        }
        TreeSet<GRADE> map = new TreeSet<>();
        map.addAll(grades);
        return map.first();
    }

    public boolean isMostSeriousGradeAtOrBelow(GRADE grade) {
        return getMostSeriousGrade().compareTo(grade) <= 0;
    }

    public boolean hasGrade(GRADE grade) {
        return grades.contains(grade);
    }

    public String getColumn(ColumnToken header) throws IOException, InterruptedException {
        if (header == ColumnToken.GRADES) {
            if (grades == null) return "No grade specified";
            String ret = "";
            for (GRADE grade: grades) {
                if (ret.equals("")) ret = grade.toString();
                else ret += ", " + grade.toString();
            }
            return ret;
        }
        else if (header == ColumnToken.MOST_SERIOUS_GRADE) {
            return getMostSeriousGrade().toString();
        }
        else {
            return super.getColumn(header);
        }
    }

    public void setBailPercent(String s) {
        bailPercent = s;
        setColumn(ColumnToken.BAIL_PERCENTAGE, s);
    }

    public void setBailType(String s) {
        this.bailType = getBailType(s);
        if (bailType == null) {
            throw new IllegalArgumentException("No BAIL_TYPE matches " + s + " for " + getDocket());
        }
        setColumn(ColumnToken.BAIL_TYPE, this.bailType);
    }

    public void setHasBailSection(boolean b) {
        hasBailSection = b;
        setColumn(ColumnToken.HAS_BAIL_SECTION, b);
    }

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
    }

    @Override
    String getCourtOffice() {
        return courtOffice;
    }


    public String getJudgeName() {
        return judgeAssigned;
    }

    public void setDocket(String docket) {
        this.docket = docket;
        setColumn(ColumnToken.DOCKET, docket);
    }

    @Override
    String getDocket() {
        return docket;
    }

    public void setFileDate(String string) {
        this.fileDate = string;
        setColumn(ColumnToken.FILE_DATE, string);
    }

    public LocalDate getFileDate() {
        if (comparableDate != null) return comparableDate;

        comparableDate = LocalDate.parse(fileDate, slashDateFormatter);

        return comparableDate;
    }

    public int getFileYear() {
        return getFileDate().getYear();
    }

    public void setJudgeAssigned(String judgeAssigned) {
        this.judgeAssigned = judgeAssigned;
        setColumn(ColumnToken.JUDGE, judgeAssigned);
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(String issueDate) {
        try {
            this.issueDate = LocalDate.parse(issueDate, slashDateFormatter);
        }
        catch (Exception e) {}
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
            this.arrestDate = LocalDate.parse(arrestDate, slashDateFormatter);
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
        return docket;
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
            this.dispositionDate = LocalDate.parse(dispositionDate, slashDateFormatter);
        }
        catch (Exception e) {}
    }
    public boolean isUnableToPostBail() {
        return unableToPostBail;
    }

    public void setUnableToPostBail(boolean unableToPostBail) {
        this.unableToPostBail = unableToPostBail;
        setColumn(ColumnToken.UNABLE_TO_PAY_BAIL, unableToPostBail);
    }

    public boolean hasStartConfinement() {
        return startConfinement != null;
    }

    public LocalDate getStartConfinement() {
        return startConfinement;
    }

    public void setStartConfinement(String startConfinement) {
        try {
            this.startConfinement = LocalDate.parse(startConfinement, slashDateFormatter);
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
            this.endConfinement = LocalDate.parse(endConfinement, slashDateFormatter);
        }
        catch (Exception e) {}
    }

    public void setBail(int bail) {
        this.bail = bail;
        setColumn(ColumnToken.BAIL, bail);
    }

    public boolean hasBail() {
        return bail != null;
    }

    public boolean hasBailType() {
        return bailType != null;
    }

    public Integer getBail() {
        return bail;
    }

    public String getStoredURL() throws IOException {
        return Scraper.getStoredURL(Scraper.CourtMode.MDJ_CR, county, docket.substring(1 + docket.lastIndexOf('-')), docket);
    }

    public String getDefendantName() {
        return defendantName;
    }

    public void setDefendantName(String defendantName) {
        this.defendantName = defendantName.trim();
    }

    public Person getPerson() {
        Person ret = new Person();
        ret.setFirst(getFirst());
        ret.setLast(getLast());
        ret.setBirthdate(birthdate);

        return ret;
    }

    public String getLast () {
        return defendantName.substring(0, defendantName.indexOf(','));
    }

    /**
     *
     * @return ONLY part of name that is not last name up to first space (ie, excludes middle name)
     */
    public String getFirst() {
        String ret = defendantName.substring(defendantName.indexOf(", ") + 2);
        int i = ret.indexOf(' ');
        if (i < 0) return ret;
        return ret.substring(0, i);
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }

    public boolean wasJailed() {
        return hasStartConfinement();
    }

    public boolean wasJailedThisDay(LocalDate date) {
        if (!wasJailed()) return false;

        LocalDate start = getStartConfinement();
        boolean onOrAfter = date.equals(start) || date.isAfter(start);
        if (!onOrAfter) return false;
        if (!hasEndConfinement()) return true;
        LocalDate end = getEndConfinement();
        return date.isEqual(end) || date.isBefore(end);
    }

    /**
     * TEMPORARY PLZ
     */
    boolean forfeiture;
    public void setForfeiture(boolean b) {
        forfeiture = b;
    }
    public boolean isForfeiture() {
        return forfeiture;
    }
}
