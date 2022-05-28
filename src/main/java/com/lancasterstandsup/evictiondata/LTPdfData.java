package com.lancasterstandsup.evictiondata;

import org.apache.commons.lang3.math.NumberUtils;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class LTPdfData extends PdfData implements Serializable {
    private static final long serialVersionUID = 1L;

    public static Set<String> missingWennerstrom = new HashSet<>();

    private static List<ColumnToken> columnHeaders;
    static {
        columnHeaders = new ArrayList<>();
        columnHeaders.add(ColumnToken.COURT);
        columnHeaders.add(ColumnToken.JUDGE);
        columnHeaders.add(ColumnToken.DOCKET);
        columnHeaders.add(ColumnToken.FILE_DATE);
        columnHeaders.add(ColumnToken.STATUS);
        columnHeaders.add(ColumnToken.PLAINTIFF);
        columnHeaders.add(ColumnToken.PLAINTIFF_ZIP);
        columnHeaders.add(ColumnToken.DEFENDANT);
        columnHeaders.add(ColumnToken.DEFENDANT_ZIP);
        columnHeaders.add(ColumnToken.HEARING_DATE);
        columnHeaders.add(ColumnToken.HEARING_TIME);
        columnHeaders.add(ColumnToken.CLAIM_AMOUNT);
        columnHeaders.add(ColumnToken.JUDGMENT_AMOUNT);
        columnHeaders.add(ColumnToken.RENT_IN_ARREARS);
        columnHeaders.add(ColumnToken.FILING_FEES);
        columnHeaders.add(ColumnToken.COSTS);
        columnHeaders.add(ColumnToken.SERVER_FEES);
        columnHeaders.add(ColumnToken.DAMAGES);
        columnHeaders.add(ColumnToken.ATTORNEY_FEES);
        columnHeaders.add(ColumnToken.RENT_RESERVED_AND_DUE);
        columnHeaders.add(ColumnToken.INTEREST);
        columnHeaders.add(ColumnToken.MONTHLY_RENT);
        columnHeaders.add(ColumnToken.WITHDRAWN);
        columnHeaders.add(ColumnToken.DISMISSED);
        columnHeaders.add(ColumnToken.GRANT_POSS);
        columnHeaders.add(ColumnToken.GRANT_POSS_IF_JUDGE_NOT_SATISFIED);
        columnHeaders.add(ColumnToken.ORDER_FOR_POSS_REQ);
        columnHeaders.add(ColumnToken.ORDER_FOR_POSS_SERVED);
        columnHeaders.add(ColumnToken.JUDGMENT_FOR_PLAINTIFF);
        columnHeaders.add(ColumnToken.JUDGMENT_FOR_DEFENDANT);
        columnHeaders.add(ColumnToken.SETTLED);
        columnHeaders.add(ColumnToken.STAYED);
        columnHeaders.add(ColumnToken.APPEALED);
        columnHeaders.add(ColumnToken.PLAINTIFF_ATTORNEY);
        columnHeaders.add(ColumnToken.DEFENDANT_ATTORNEY);
        columnHeaders.add(ColumnToken.NOTES);
    }

    public List<ColumnToken> getColumnHeaders() {
        return columnHeaders;
    }

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

    private static Map<String, String> nameNormalizations;

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

    private static void initNameNormalizations() throws IOException {
        nameNormalizations = new HashMap<>();
        File source = new File("./src/main/resources/name_normalization.txt");
        BufferedReader in = new BufferedReader(new FileReader(source));
        String next = null;
        while ((next = in.readLine()) != null) {
            if (next.length() == 0) break;
            String variantLine = in.readLine();
            String [] variants = variantLine.split("\\|");
            for (String v: variants) {
                nameNormalizations.put(v, next);
            }
        }
    }

    public static void main (String [] args) throws IOException {
        buildPlaces();
    }

    private static void buildMunicipalities() throws IOException {
        File source = new File("./src/main/resources/municipalities_source.txt");
        File dest = new File("./src/main/resources/municipalities.txt");
        BufferedReader in = new BufferedReader(new FileReader(source));
        PrintWriter out = new PrintWriter(new FileWriter(dest));
        String next = null;
        while ((next = in.readLine()) != null) {
            if (next.length() == 0) break;
            String muni = in.readLine();
            String stripped = muni.replace(" Township", "");
            stripped = stripped.replace(" Borough", "");
            out.println(muni);
            if (!muni.equals(stripped)) {
                out.println(stripped);
            }
        }
    }

    private static void buildPlaces() throws IOException {
        File source = new File("./src/main/resources/census_designated_places_source.txt");
        File dest = new File("./src/main/resources/pa_places.txt");
        BufferedReader in = new BufferedReader(new FileReader(source));
        PrintWriter out = new PrintWriter(new FileWriter(dest));
        String line = null;
        String targetDash = null;
        while ((line = in.readLine()) != null) {
            if (line.length() > 0 && line.indexOf("[edit]") < 0 &&
                    line.replace("\t", "").trim().length() > 0) {
                String [] chunks = line.split("\t");
                for (String chunk: chunks) {
                    int x = chunk.indexOf(" - ");
                    String place = chunk.substring(0, x);
                    //System.out.println(place + " from " + chunk);
                    out.println(place.replace('-', ' '));
                }
            }
        }

        in.close();

        source = new File("./src/main/resources/municipalities_source.txt");
        in = new BufferedReader(new FileReader(source));
        String next = null;
        while ((next = in.readLine()) != null) {
            if (next.length() == 0) break;
            String muni = in.readLine();
            String stripped = muni.replace(" Township", "");
            stripped = stripped.replace(" Borough", "");
            out.println(muni);
            if (!muni.equals(stripped)) {
                out.println(stripped);
            }
        }

        in.close();
        out.close();
    }

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
    private String orderForPossessionRequired;
    private LocalDate orderForPossessionRequiredDate;
    private String orderForPossessionServed;
    private LocalDate orderForPossessionServedDate;
    private String served;
    private String withdrawn;
    private String dismissed;
    private String dismissedWithPrejudice;
    private String settled;
    boolean stayed;
    boolean appealed;
    boolean judgmentForPlaintiff;
    boolean judgmentForDefendant;
    private String dispositionDate;
    private boolean bankruptcy;
    private String notes;
    private String plaintiffAttorney;
    private String defendantAttorney;

    private LocalDate comparableDate;

    public void setCourtOffice(String courtOffice) {
        this.courtOffice = courtOffice;
        setColumn(ColumnToken.COURT, courtOffice);
    }

    /**
     *
     * @return format for Lanco is "MDJ ##-#-##", ex: MDJ 02-3-09
     */
    public String getCourtOffice() {
        return courtOffice;
    }

    public void setJudgeName(String judgeName) {
        this.judgeName = judgeName;

        setColumn(ColumnToken.JUDGE, judgeName);
    }

    public void setDocketNumber(String docketNumber) {
        this.docketNumber = docketNumber;
        setColumn(ColumnToken.DOCKET, docketNumber);
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

    public void setCaseStatus(String caseStatus) {
        this.caseStatus = caseStatus;
        setColumn(ColumnToken.STATUS, caseStatus);
    }

    public void setPlaintiffs(String plaintiffNames) {
        this.plaintiffNames = getNormalizedPlaintiff(plaintiffNames);
        setColumn(ColumnToken.PLAINTIFF, plaintiffNames);
    }

    private String getNormalizedPlaintiff(String plaintiff) {
        if (nameNormalizations == null) {
            try {
                initNameNormalizations();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        if (nameNormalizations.containsKey(plaintiffNames)) {
            plaintiff = nameNormalizations.get(plaintiffNames);
        }

        return plaintiff;
    }

    public void setPlaintiffZips(String plaintiffZips) {
        this.plaintiffZips = plaintiffZips;
        setColumn(ColumnToken.PLAINTIFF_ZIP, plaintiffZips);
    }

    public void setDefendant(String defendantNames) {
        this.defendantNames = defendantNames;
        setColumn(ColumnToken.DEFENDANT, defendantNames);
    }

    private String obfuscateNames(String s) {
        String [] split = s.split(" ");
        String ret = "";
        for (String x: split) {
            if (x.trim().length() > 0) {
                ret += x.substring(0, 1).toUpperCase() + ".";
                if (x.lastIndexOf(',') == x.length() - 1) ret += ",";
                ret += " ";
            }
        }
        return ret.replace("&.", "&").trim();
    }

    public void setDefendantZips(String defendantZips) {
        this.defendantZips = defendantZips;
        setColumn(ColumnToken.DEFENDANT_ZIP, defendantZips);
    }

    public void setScheduledDate(String hd) {
        hearingDate = hd;
        setColumn(ColumnToken.HEARING_DATE, hd);
    }

    public boolean hasHearingDate() {
        return hearingDate != null;
    }

    public LocalDate getHearingDate() {
        if (hearingDate == null) return null;
        return LocalDate.parse(fileDate, slashDateFormatter);
    }

    public void setScheduledHour(String ht) {
        hearingTime = ht;
        setColumn(ColumnToken.HEARING_TIME, ht);
    }

    public void setClaim(String claim) {
        this.claim = claim;
        setColumn(ColumnToken.CLAIM_AMOUNT, claim);
    }

    public int getClaim() {
        return claim == null ? 0 : convertMoneyToDollars(claim);
    }

    public void setJudgment(String judgment) {
        this.judgment = validateMoney(judgment);
        setColumn(ColumnToken.JUDGMENT_AMOUNT, this.judgment);
    }

    public void setRentInArrears(String m) {
        if (rentInArrears == null) rentInArrears = m;
        else {
            rentInArrears = addMoneyStrings(rentInArrears, m);
        }
        setColumn(ColumnToken.RENT_IN_ARREARS, rentInArrears);
    }

    public void setFilingFees(String m) {
        if (filingFees == null) filingFees = m;
        else {
            filingFees = addMoneyStrings(filingFees, m);
        }
        setColumn(ColumnToken.FILING_FEES, filingFees);
    }

    public void setCosts(String m) {
        if (costs == null) costs = m;
        else {
            costs = addMoneyStrings(costs, m);
        }
        setColumn(ColumnToken.COSTS, costs);
    }

    public void setServerFees(String m) {
        if (serverFees == null) serverFees = m;
        else {
            serverFees = addMoneyStrings(serverFees, m);
        }
        setColumn(ColumnToken.SERVER_FEES, serverFees);
    }

    public int getDamages() {
        return damages == null ? 0 : convertMoneyToDollars(damages);
    }

    public int getRentInArrears() {
        return rentInArrears == null ? 0: convertMoneyToDollars(rentInArrears);
    }

    private int convertMoneyToDollars(String money) {
        if (money.indexOf('$') == 0) {
            money = money.substring(1);
        }
        int i = money.indexOf('.');
        money = money.substring(0, i);

        money = money.replace(",", "");

        return Integer.parseInt(money);
    }

    public void setDamages(String m) {
        if (damages == null) damages = m;
        else {
            damages = addMoneyStrings(damages, m);
        }
        setColumn(ColumnToken.DAMAGES, damages);
    }

    public void setAttorneyFees(String m) {
        if (attorneyFees == null) attorneyFees = m;
        else {
            attorneyFees = addMoneyStrings(attorneyFees, m);
        }
        setColumn(ColumnToken.ATTORNEY_FEES, attorneyFees);
    }

    public void setRentReservedAndDue(String m) {
        if (rentReservedAndDue == null) rentReservedAndDue = m;
        else {
            rentReservedAndDue = addMoneyStrings(rentReservedAndDue, m);
        }
        setColumn(ColumnToken.RENT_RESERVED_AND_DUE, rentReservedAndDue);
    }

    public void setInterest(String m) {
        if (interest == null) interest = m;
        else {
            interest = addMoneyStrings(interest, m);
        }
        setColumn(ColumnToken.INTEREST, interest);
    }

    public void setMonthlyRent(String s) {
        this.monthlyRent = s;
        setColumn(ColumnToken.MONTHLY_RENT, monthlyRent);
    }

    public void setWithdrawn(boolean b) {
        this.withdrawn = b ? "TRUE" : "FALSE";
        setColumn(ColumnToken.WITHDRAWN, withdrawn);
    }

    public void setDismissed(boolean b) {
        this.dismissed = b ? "TRUE" : "FALSE";
        setColumn(ColumnToken.DISMISSED, dismissed);
    }

    public void setDismissedWithPrejudice(boolean b) {
        if (!b) return;
        this.dismissedWithPrejudice ="TRUE";

        setColumn(ColumnToken.DISMISSED, "TRUE");
    }

    public boolean isTenantWin() {
        return judgmentForDefendant || dismissed != null || dismissedWithPrejudice != null;
    }

    public void setGrantPossession(String gp) {
        //System.out.println(docketNumber);
        this.grantPossession = gp;
        setColumn(ColumnToken.GRANT_POSS, grantPossession);
    }

    public void setGrantPossessionIf(String gp) {
        this.grantPossessionIf = gp;
        setColumn(ColumnToken.GRANT_POSS_IF_JUDGE_NOT_SATISFIED, grantPossessionIf);
    }

    public void setOrderForPossessionRequested(boolean b, String date) {
        this.orderForPossessionRequired = b ? "TRUE" : "FALSE";

        orderForPossessionRequiredDate = LocalDate.parse(date, slashDateFormatter);

        setColumn(ColumnToken.ORDER_FOR_POSS_REQ, orderForPossessionRequired);

    }

    public void setOrderForPossessionServed(boolean b, String date) {
        this.orderForPossessionServed = b ? "TRUE" : "FALSE";

        orderForPossessionServedDate = LocalDate.parse(date, slashDateFormatter);

        setColumn(ColumnToken.ORDER_FOR_POSS_SERVED, orderForPossessionServed);
    }

    public void judgmentForPlaintiff(boolean b) {
        judgmentForPlaintiff = b;
        setColumn(ColumnToken.JUDGMENT_FOR_PLAINTIFF, b ? "TRUE" : "FALSE");
    }

    public void judgmentForDefendant(boolean b) {
        judgmentForDefendant = b;
        setColumn(ColumnToken.JUDGMENT_FOR_DEFENDANT, b ? "TRUE" : "FALSE");
    }

    public void setSettled(boolean b, String date) {
        if (!b) return;
        this.settled = date != null ? date : "TRUE";
        setColumn(ColumnToken.SETTLED, settled);
    }

    public void setStayed (String stayedString) {
        if (stayedString == null) return;
        stayed = true;
        setColumn(ColumnToken.STAYED, "TRUE");
    }

    public void setServed(boolean b) {
        this.served = b ? "TRUE" : "FALSE";
    }

    public void setAppeal(String s) {
        this.appealed = true;

        if (notes != null) {
            notes += "; " + s;
        }
        else notes = s;

        setColumn(ColumnToken.APPEALED, "TRUE");
    }

    private boolean isStayed() {
        return stayed;
    }

    public void addAttorney(String name, String representing) {
        if (containsMatchingEntity(representing, defendantNames, false)) {
            if (defendantAttorney == null) defendantAttorney = name;
            else defendantAttorney += ", " + name;
        }
        else if (containsMatchingEntity(representing, plaintiffNames, true)) {
            if (plaintiffAttorney == null) plaintiffAttorney = name;
            else plaintiffAttorney += ", " + name;
            setColumn(ColumnToken.PLAINTIFF_ATTORNEY, plaintiffAttorney);
        }
        else if (!ignoreAttorneyProvenance.contains(getDocket())) {
            System.err.println("Can't determine attorney provenance for " + docketNumber);
            addNote("Attorney " + name + " with indeterminant provenance");
        }
    }

    public void addNote(String note) {
        if (notes != null && notes.indexOf(note) > -1) return;

        if (notes != null) {
            notes += "; " + note;
        }
        else notes = note;
        setColumn(ColumnToken.NOTES, notes);
    }

    public boolean sentToCommonPleas() {
        return notes.indexOf(LTParser.sentToCommonPleas) > -1;
    }

    public String getJudgeName() {
        return judgeName;
    }

    /**
     *
     * @return reduces courtOffice 'MDJ 06-3-08' to '06-3-08'
     */
    public String getCourtOfficeWithoutMDJ() {
        int i = courtOffice.indexOf(' ');
        return courtOffice.substring(i + 1);
    }

    public String getCourtOfficeNumberOnly() {
        String ret = courtOffice.replaceAll("[\\D.]", "");
        while (ret.indexOf("0") == 0) ret = ret.substring(1);
        return ret;
    }

    public String getDocket() {
        return docketNumber;
    }

    public String getSequenceNumber() {
        int i = docketNumber.indexOf("LT-");
        String ret = docketNumber.substring(i + 3);
        return ret.substring(0, ret.indexOf('-'));
    }

    public String toString() {
        try {
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
                    "order for possession requested: " + orderForPossessionRequired + "\n" +
                    "order for possession served: " + orderForPossessionServed + "\n" +
                    //"tenant win: " + tenantWin + "\n" +
                    "judgment for plaintiff: " + judgmentForPlaintiff + "\n" +
                    "judgment for defendant: " + judgmentForDefendant + "\n" +
                    "served: " + served + "\n" +
                    "settled: " + settled + "\n" +
                    "notes: " + notes + "\n\n" +
                    toTestDataRowOutput();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "error in toString for " + docketNumber;
    }

    public String toTestDataRowOutput() throws IOException, InterruptedException {
        StringBuffer sb = new StringBuffer();
        sb.append("\n{");

        String [] row = getRowValues().getRow();
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
        //or sent to Common Pleas
        if (!this.caseStatus.equals(ACTIVE_STATUS)) {
            if (!isWithdrawn() && !isSettled() && !isTenantWin() && !isTransferred() && !hasJudgment() &&
                !isGrantPossession() && !isGrantPossessionIf() && !isOrderForPossessionServed() &&
                    !stayed && !judgmentForPlaintiff && !bankruptcy &&
                    !sentToCommonPleas()) {
                if (debugOutput && !ignoreInvalidByActive.contains(getDocket())) {
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

    private boolean containsMatchingEntity(String represented, String parties, boolean plaintiff) {
        //if (plaintiff) represented = getNormalizedPlaintiff(represented);
        represented = represented.replace(" ", "");
        String[] split = parties.split("&");
        for (String party: split) {
            party = party.replace(" ", "");
            //forcing match when one long string used 'Apts.' at end and other uses 'Apartments'
            int maxLength = Math.min(12, Math.min(party.length(), represented.length()));
            party = party.substring(0, maxLength);
            String rep = represented.substring(0, maxLength);
            if (party.equals(rep)) return true;
        }

        //found one where represented was first last (instead of last first as listed normally)

        return false;
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof LTPdfData)) return false;

        LTPdfData op = (LTPdfData) o;

        return getDocket().equals(op.getDocket());
    }

    public void setDispositionDate(String dispositionDate) {
        this.dispositionDate = dispositionDate;
    }

    public LocalDate getDispositionDate() {
        return dispositionDate != null ? LocalDate.parse(dispositionDate, slashDateFormatter) : null;
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
        return "Inactive".equals(caseStatus);
    }

    public boolean isAlive() {
        return !isClosed() && !isInactive();
    }

    public boolean rescrape(LocalDateTime lastCheck) {
        //don't rescrape 2019
        if (getFileDate().getYear() < 2020) return false;

        if (isDismissed()) return false;

        if (isWithdrawn()) return false;

        if (isSettled()) return false;

        if (!isAlive() && isOrderForPossessionServed()) {
            return false;
        }

        //isAlive is true if file's status is not 'closed' or 'inactive'
        if (isAlive()) {
            System.out.println("Rescrape " + getDocket() + " because it is alive.");
            return true;
        }
        //'closed' might still have been updated with order for possession
        //there's a judgment and we don't have grant poss if or grant poss and the
        //date of judgment is less than X (60?) days from last scrape
        LocalDate disposition = getDispositionDate();
        if (disposition == null) {
            System.out.println("Rescrape " + getDocket() + " because it has no disposition date.");
            return true;
        }

        if (!hasJudgment()) {
            System.out.println("Rescrape " + getDocket()  + " because it has no judgment.");
            return true;
        }

        //Otis analysis shows almost all orders within 100 days of disposition
        int window = 100;
        LocalDateTime now = LocalDateTime.now();
        if (disposition.until(now, ChronoUnit.DAYS) < window) {
            System.out.println("Rescrape " + getDocket()  + " because it is within " + window +
                    " days of disposition without order for possession");
            return true;
        }

        if (lastCheck == null) {
            System.out.println("Rescrape " + getDocket()  + " because lastCheck is null.");
            return true;
        }

        //we've checked at least once since window expired
        if (getDispositionDate().until(lastCheck, ChronoUnit.DAYS) >= window) {
            return false;
        }

        System.out.println("Rescrape " + getDocket() + " because, err, defaulting after many checks.");
        return true;
    }

    public boolean isPlaintiffAttorney() {
        return plaintiffAttorney != null;
    }

    public boolean isDefendantAttorney() {
        return defendantAttorney != null;
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

    /**
     * DANGER: turns out 'closed' gets marked BEFORE orders for possession
     */
    @Deprecated
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
     *
     * RETIRED 6/25/21 cuz 'resolved' may have occurred but eviction hasn't happened yet
     */
//    public boolean isEvictionWarning() {
//        if (isResolved() || !(isEitherGrant() || isOrderForPossessionServed())) {
//            return false;
//        }
//        return true;
//    }

    public boolean evictionWarning() {
        if (dispositionDate == null) return false;
        if (!isGrantPossessionOrOrderForEvictionServed()) return false;
        LocalDate disposedPlusTwenty = getDispositionDate().plusDays(20);
        return disposedPlusTwenty.compareTo(LocalDate.now()) >= 0;
    }

    public String getDefendant() {
        return defendantNames;
    }

    public String getNotes() {
        return notes;
    }

    public boolean defendantZip(String target) {
        return defendantZips.indexOf(target) > -1;
    }

    public int getDocketDiff(LTPdfData previous) {
        return getDocketNumberAsInt() - previous.getDocketNumberAsInt();
    }
}