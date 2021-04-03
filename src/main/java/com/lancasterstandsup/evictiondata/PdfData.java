package com.lancasterstandsup.evictiondata;

import org.apache.commons.lang3.math.NumberUtils;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PdfData implements Comparable<PdfData>, Serializable {
    private static final long serialVersionUID = 1L;

    public static final String MISSING_ZIP = "MISSING ZIP";
    public static final String MONEY_FORMAT_ERROR = "ERROR";
    public static final String ACTIVE_STATUS = "Active";
    public static final String MISSING_CASE_STATUS = "MISSING CASE STATUS";
    public static final String MISSING_CLAIM = "MISSING CLAIM";

    private static final String[] ignoreAttorneyProvenanceSource =
    {
            "MJ-02204-LT-0000123-2019",
    };
    private static Set<String> ignoreAttorneyProvenance;

    private static final String[] ignoreInvalidByActiveSource =
    {
            //last docket activity: filed for bankruptcy
            "MJ-02208-LT-0000101-2019",
    };
    private static Set<String> ignoreInvalidByActive;

    static {
        ignoreAttorneyProvenance = new HashSet<>();
        for (String s: ignoreAttorneyProvenanceSource) {
            ignoreAttorneyProvenance.add(s);
        }

        ignoreInvalidByActive = new HashSet<>();
        for (String s: ignoreInvalidByActiveSource) {
            ignoreInvalidByActive.add(s);
        }
    }

    private static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private String courtOffice;
    private String docketNumber;
    private String judgeName;
    private String fileDate;
    private String caseStatus;
    private String plaintiffNames;
    private String plaintiffZips;
    private String defendantNames;
    private String defendantZips;
    private String hearingDate;
    private String hearingTime;
    private String claim;
    private String judgment;
    private String rentInArrears;
    private String filingFees;
    private String serverFees;
    private String rentReservedAndDue;
    private String attorneyFees;
    private String damages;
    private String costs;
    private String interest;
    private String monthlyRent;
    private String grantPossession;
    private String grantPossessionIf;
    private String orderForPossessionRequested;
    private LocalDate orderForPossessionServedDate;
    private String orderForPossessionServed;
    private String tenantWin;
    private String served;
    private String withdrawn;
    private String dismissed;
    private String dismissedWithPrejudice;
    private String settled;
    boolean stayed;
    boolean judgmentForPlaintiff;
    boolean judgmentForDefendant;
    private String dispositionDate;
    private boolean bankruptcy;
    private String notes;
    private List<String> attorneys;
    private boolean defendantAttorneyExists;
    private boolean plaintiffAttorneyExists;

    private LocalDate comparableDate;

    //row --> cell value
    private TreeMap<Integer, String> row = new TreeMap<>();

    public String[] getRow() {
        String[] ret = new String[Parser.colHeaders.length];

        for (Integer i: row.keySet()) {
            ret[i] = row.get(i);
        }

        //We're adding server fees and damages dynamically to notes
        //kinda assuming we'll give them their own column
        String moddedNotes = getModdedNotes();

        if (moddedNotes != null) {
            int notesI = -1;
            for (int x = Parser.colHeaders.length - 1; x >= 0; x--) {
                String header = Parser.colHeaders[x];
                if (header.equals("Notes")) {
                    notesI = x;
                }
            }

            ret[notesI] = moddedNotes;
        }

        return ret;
    }

    private String getModdedNotes() {
        String notes = this.notes;
        if (serverFees != null) {
            if (notes == null) notes = "Server fees: " + serverFees;
            else notes += "; " + "Server fees: " + serverFees;
        }
        if (damages != null) {
            if (notes == null) notes = "Damages: " + damages;
            else notes += "; " + "Damages: " + damages;
        }
        if (attorneyFees != null) {
            if (notes == null) notes = "Attorney fees: " + attorneyFees;
            else notes += "; " + "Attorney fees: " + attorneyFees;
        }
        if (rentReservedAndDue != null) {
            if (notes == null) notes = "Rent reserved and due: " + rentReservedAndDue;
            else notes += "; " + "Rent reserved and due: " + rentReservedAndDue;
        }
        return notes;
    }

    public void setCourtOffice(String courtOffice) {
        this.courtOffice = courtOffice;
        row.put(0, courtOffice);
    }

    public void setJudgeName(String judgeName) {
        this.judgeName = judgeName;
        row.put(1, judgeName);
    }

    public void setDocketNumber(String docketNumber) {
        this.docketNumber = docketNumber;
        row.put(2, docketNumber);
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

    public void setCaseStatus(String caseStatus) {
        this.caseStatus = caseStatus;
        row.put(4, caseStatus);
    }

    public void setPlaintiffs(String plaintiffNames) {
        this.plaintiffNames = plaintiffNames;
        row.put(5, plaintiffNames);
    }

    public void setPlaintiffZips(String plaintiffZips) {
        this.plaintiffZips = plaintiffZips;
        row.put(6, plaintiffZips);
    }

    public void setDefendant(String defendantNames) {
        this.defendantNames = defendantNames;
        row.put(7, defendantNames);
    }

    public void setDefendantZips(String defendantZips) {
        this.defendantZips = defendantZips;
        row.put(8, defendantZips);
    }

    public void setScheduledDate(String hd) {
        hearingDate = hd;
        row.put(9, hd);
    }

    public boolean hasHearingDate() {
        return hearingDate != null;
    }

    public LocalDate getHearingDate() {
        if (hearingDate == null) return null;
        return LocalDate.parse(fileDate, dateFormatter);
    }

    public void setScheduledHour(String ht) {
        hearingTime = ht;
        row.put(10, ht);
    }

    public void setClaim(String claim) {
        this.claim = claim;
        row.put(11, claim);
    }

    public void setJudgment(String judgment) {
        this.judgment = validateMoney(judgment);
        row.put(12, judgment);
    }

    public void setRentInArrears(String m) {
        if (rentInArrears == null) rentInArrears = m;
        else {
            rentInArrears = addMoneyStrings(rentInArrears, m);
        }
        row.put(13, m);
    }

    public void setFilingFees(String m) {
        if (filingFees == null) filingFees = m;
        else {
            filingFees = addMoneyStrings(filingFees, m);
        }
        row.put(14, filingFees);
    }

    public void setServerFees(String m) {
        if (serverFees == null) serverFees = m;
        else {
            serverFees = addMoneyStrings(serverFees, m);
        }
    }

    public void setRentReservedAndDue(String m) {
        if (rentReservedAndDue == null) rentReservedAndDue = m;
        else {
            rentReservedAndDue = addMoneyStrings(rentReservedAndDue, m);
        }
    }

    public void setAttorneyFees(String m) {
        if (attorneyFees == null) attorneyFees = m;
        else {
            attorneyFees = addMoneyStrings(attorneyFees, m);
        }
    }

    public void setDamages(String m) {
        if (damages == null) damages = m;
        else {
            damages = addMoneyStrings(damages, m);
        }
    }

    public void setCosts(String m) {
        if (costs == null) costs = m;
        else {
            costs = addMoneyStrings(costs, m);
        }
        row.put(15, costs);
    }

    public void setInterest(String m) {
        if (interest == null) interest = m;
        else {
            interest = addMoneyStrings(interest, m);
        }
    }

    public void setMonthlyRent(String s) {
        this.monthlyRent = s;
        row.put(16, s);
    }

    public void setWithdrawn(boolean b) {
        this.withdrawn = b ? "TRUE" : "FALSE";
        row.put(17, this.withdrawn);
    }

    public void setDismissed(boolean b) {
        this.dismissed = b ? "TRUE" : "FALSE";
        row.put(18, this.dismissed);

        if (b) setTenantWin(true);
    }

    public void setDismissedWithPrejudice(boolean b) {
        this.dismissedWithPrejudice = b ? "TRUE" : "FALSE";

        if (b) setTenantWin(true);
    }

    public void setGrantPossession(String gp) {
        //System.out.println(docketNumber);
        this.grantPossession = gp;
        row.put(19, gp);
    }

    public void setGrantPossessionIf(String gp) {
        this.grantPossessionIf = gp;
        row.put(20, gp);
    }

    public void setOrderForPossessionRequested(boolean b, String date) {
        this.orderForPossessionRequested = b ? "TRUE" : "FALSE";
        row.put(21, this.orderForPossessionRequested);

        orderForPossessionServedDate = LocalDate.parse(date, dateFormatter);
    }

    public void setOrderForPossessionServed(boolean b) {
        this.orderForPossessionServed = b ? "TRUE" : "FALSE";
        row.put(22, this.orderForPossessionServed);
    }

    //judgment for defendant or dismissed
    public void setTenantWin(boolean b) {
        this.tenantWin = b ? "TRUE" : "FALSE";
        row.put(23, this.tenantWin);
    }

    public void setServed(boolean b) {
        this.served = b ? "TRUE" : "FALSE";
        row.put(24, this.served);
    }

    public void setSettled(boolean b, String date) {
        this.settled = b ? "TRUE" : "FALSE";

        String note = "Case was settled";
        if (date != null) note += " " + date;
        addNote(note);
    }

    public void setStayed (String stayedString) {
        stayed = true;
        addNote(stayedString);
    }

    private boolean isStayed() {
        return stayed;
    }

    public void addNote(String note) {
        if (notes != null && notes.indexOf(note) > -1) return;

        if (notes != null) {
            notes += "; " + note;
        }
        else notes = note;
        row.put(26, notes);
    }

    public String getJudgeName() {
        return judgeName;
    }

    public String getCourtOfficeFull() {
        return courtOffice;
    }

    public String getCourtOfficeNumberOnly() {
        String ret = courtOffice.replaceAll("[\\D.]", "");
        while (ret.indexOf("0") == 0) ret = ret.substring(1);
        return ret;
    }

    public String getDocketNumber() {
        return docketNumber;
    }

    public String getSequenceNumber() {
        int i = docketNumber.indexOf("LT-");
        String ret = docketNumber.substring(i + 3);
        return ret.substring(0, ret.indexOf('-'));
    }

    public String toString() {
        return "\nCourt: " + courtOffice + "\n" +
                "docketNumber: " + docketNumber + "\n" +
                "fileDate: " + fileDate + "\n" +
                "judgeName: " + judgeName + "\n" +
                "case status: " + caseStatus + "\n" +
                "plaintiff(s): " + plaintiffNames + "\n" +
                "plaintiff zip(s): " + plaintiffZips + "\n" +
                "defendant(s): " + defendantNames + "\n" +
                "defendant zip(s): " + defendantZips + "\n" +
                "hearingDate: " + hearingDate + "\n" +
                "hearingTime: " + hearingTime + "\n" +
                "claim: " + claim + "\n" +
                "judgment: " + judgment + "\n" +
                "disposition date: " + dispositionDate + "\n" +
                "rent in arrears: " + rentInArrears + "\n" +
                "filing fees: " + filingFees + "\n" +
                "server fees: " + serverFees + "\n" +
                "attorney fees: " + attorneyFees + "\n" +
                "damages: " + damages + "\n" +
                "costs: " + costs + "\n" +
                "interest: " + interest + "\n" +
                "monthlyRent: " + monthlyRent + "\n" +
                "withdrawn: " + withdrawn + "\n" +
                "grant possession: " + grantPossession + "\n" +
                "grant possession if: " + grantPossessionIf + "\n" +
                "order for possession requested: " + orderForPossessionRequested + "\n" +
                "order for possession served: " + orderForPossessionServed + "\n" +
                "tenant win: " + tenantWin + "\n" +
                "served: " + served + "\n" +
                "settled: " + settled + "\n" +
                "notes: " + getModdedNotes() + "\n\n" +
                toTestDataRowOutput();
    }

    public String toTestDataRowOutput() {
        StringBuffer sb = new StringBuffer();
        sb.append("\n{");

        String [] row = getRow();
        for (int x = 0; x < row.length; x++) {
            String next = row[x];
            if (x != 0) sb.append(", ");
            if (next == null) sb.append("null");
            else sb.append("\"" + next + "\"");
        }

        sb.append("};");

        return sb.toString();
    }

    public String validateMoney(String money) {
        String testMe = money;
        testMe = testMe.trim();
        int i = testMe.indexOf('$');
        if (i > 0) {
            return MONEY_FORMAT_ERROR;
        }
        else if (i == 0) {
            testMe = testMe.substring(1);
        }
        String noCommas = testMe.replace(",", "");
        if (!NumberUtils.isCreatable(noCommas)) {
            return MONEY_FORMAT_ERROR;
        }
        return money;
    }

    //Assumes correctly formatted input
    private static int integerFromMoney(String m) {
        String testMe = m;
        testMe = testMe.trim();
        int i = testMe.indexOf('$');
        if (i == 0) {
            testMe = testMe.substring(1);
        }
        String noCommas = testMe.replace(",", "");
        String noDecimal = noCommas;
        if (noDecimal.indexOf('.') > -1) noDecimal = noCommas.substring(0, noCommas.indexOf('.'));
        return Integer.parseInt(noDecimal);
    }

    public static String addMoneyStrings(String a, String b) {
        int aI = integerFromMoney(a);
        int bI = integerFromMoney(b);
        int totalI = aI + bI;

        String postDot = "";
        String decimalA = "100";
        String decimalB = "100";
        if (a.indexOf('.') > -1) decimalA = 1 + a.substring(a.indexOf('.') + 1);
        if (b.indexOf('.') > -1) decimalB = 1 + b.substring(b.indexOf('.') + 1);
        int decimalSum = integerFromMoney(decimalA) + integerFromMoney(decimalB);
        if (decimalSum > 200) {
            if (decimalSum >= 300) {
                totalI++;
            }
            postDot = "." + ("" + decimalSum).substring(1);
        }

        return "$" + totalI + postDot;
    }


    public boolean isValid() {
        return validate(false);
    }

    public boolean validate(boolean debugOutput) {
        if (caseStatus == null) {
            if (debugOutput) invalidMessage("null caseStatus");
            return false;
        }
        //If not 'active', should be withdrawn, settled, tenantWin (judgment or dismissed),
        //or Yes or Yes
        if (!this.caseStatus.equals(ACTIVE_STATUS)) {
            if (!isWithdrawn() && !isSettled() && !isTenantWin() && !isTransferred() && !hasJudgment() &&
                !isGrantPossession() && !isGrantPossessionIf() && !isOrderForPossessionServed() &&
                    !stayed && !judgmentForPlaintiff && !bankruptcy) {
                if (debugOutput && !ignoreInvalidByActive.contains(getDocketNumber())) {
                    invalidMessage("inactive case isn't withdrawn, settled, dismissed, or granted");
                }
                return false;
            }
        }

        return true;
    }

    public boolean isGrantPossessionIf() {
        return "Yes".equals(grantPossessionIf);
    }

    public boolean isOrderForPossessionServed() {
        return "TRUE".equals(orderForPossessionServed);
    }

    public boolean isGrantPossession() {
        return "Yes".equals(grantPossession);
    }

    private boolean isDismissed() {
        return "TRUE".equals(dismissed);
    }

    private boolean isSettled() {
        return this.settled != null;
    }

    private boolean isWithdrawn() {
        return "TRUE".equals(withdrawn);
    }

    private boolean isTenantWin() {
        return "TRUE".equals(tenantWin);
    }

    private boolean isTransferred() {
        return notes != null && notes.indexOf("transferred") > -1;
    }

//    private boolean isOrderedForPossession() {
//        return "TRUE".equals(orderForPossessionRequested);
//    }

    private boolean hasJudgment() {
        return judgment != null && integerFromMoney(judgment) > 0;
    }

    private void invalidMessage(String msg) {
        System.err.println(docketNumber + " INVALID: " + msg);
    }

    public void addAttorney(String name, String representing) {
        if (attorneys == null) {
            attorneys = new ArrayList<>();
        }
        String attorneyPhrase = name + " representing " + representing;
        if (containsMatchingEntity(representing, defendantNames)) {
            attorneyPhrase = name + " representing defendant " + representing;
            defendantAttorneyExists = true;
        }
        else if (containsMatchingEntity(representing, plaintiffNames)) {
            attorneyPhrase = name + " representing plaintiff " + representing;
            plaintiffAttorneyExists = true;
        }
        else if (!ignoreAttorneyProvenance.contains(getDocketNumber())) {
            System.err.println("Can't determine attorney provenance for " + docketNumber);
        }
        attorneys.add(attorneyPhrase);
        addNote(attorneyPhrase);
    }

    private boolean containsMatchingEntity(String represented, String parties) {
        represented = represented.replace(" ", "");
        String[] split = parties.split("&");
        for (String party: split) {
            party = party.replace(" ", "");
            int maxLength = Math.min(party.length(), represented.length());
            party = party.substring(0, maxLength);
            String rep = represented.substring(0, maxLength);
            if (party.equals(rep)) return true;
        }
        return false;
    }

    private static final Comparator<PdfData> COMPARATOR =
            Comparator.comparing(PdfData::getFileDate)
                    .thenComparing(PdfData::getJudgeName)
                    .thenComparing(PdfData::getDocketNumber);

    @Override
    public int compareTo(PdfData o) {
        return COMPARATOR.compare(this, o);
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof PdfData)) return false;

        PdfData op = (PdfData) o;

        return getDocketNumber().equals(op.getDocketNumber());
    }

    public void judgmentForPlaintiff(boolean b) {
        judgmentForPlaintiff = true;
    }

    public void judgmentForDefendant(boolean b) {
        judgmentForDefendant = true;
        setTenantWin(true);
    }

    public void setDispositionDate(String dispositionDate) {
        this.dispositionDate = dispositionDate;
    }

    public void bankruptcy(String bankruptcy_petition_filed) {
        bankruptcy = true;
        addNote(bankruptcy_petition_filed);
    }

    public String getPlaintiff() {
        return plaintiffNames;
    }

    public boolean isClosed() {
        return "Closed".equals(caseStatus);
    }

    public boolean isInactive() {
//        if (docketNumber.equals("MJ-02204-LT-0000007-2020")) {
//            System.out.println("ksjdf");
//        }
        return "Inactive".equals(caseStatus);
    }

    public boolean isPlaintiffAttorney() {
        return plaintiffAttorneyExists;
    }

    public boolean isDefendantAttorney() {
        return defendantAttorneyExists;
    }

    public boolean isGrantPossessionOrOrderForEvictionServed() {
        return isGrantPossession() || isGrantPossessionIf() || isOrderForPossessionServed();
    }

    public boolean isEviction() {
        return isOrderForPossessionServed();
    }

    public boolean isJudgmentForPlaintiff() {
        return judgmentForPlaintiff;
    }

    public boolean isJudgmentForDefendant() {
        return judgmentForDefendant;
    }

    public boolean isPlaintiffWin() {
        return isJudgmentForPlaintiff() || isGrantPossessionOrOrderForEvictionServed();
    }

    public boolean isResolved() {
        return isClosed() || isInactive() || isJudgmentForDefendant() ||
                isJudgmentForPlaintiff() || isDismissed() ||
                isWithdrawn() || isGrantPossessionOrOrderForEvictionServed() ||
                isTransferred() || judgment != null;
    }

    public boolean isEitherGrant() {
        return isGrantPossession() || isGrantPossessionIf();
    }

    /**
     * Is tenant in imminent danger of eviction?
     */
    public boolean isEvictionWarning() {
        if (isResolved() || !(isEitherGrant() || isOrderForPossessionServed())) {
            return false;
        }
        return true;
    }

    public String getDefendant() {
        return defendantNames;
    }

    public String getNotes() {
        return notes;
    }

    public String[] getZip() {
        String zips = defendantZips + "";
        zips = zips.replace(",", " ");
        return zips.split(" ");
    }
}