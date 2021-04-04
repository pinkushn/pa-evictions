package com.lancasterstandsup.evictiondata;

/**
 *
 * Validate against state/county reports from earlier years
 *   My anaysis says X evictions in lancaster in 2017. What does
 *   PA say? What does EvictionLab say?
 *
 * Parser upgrade: Add date to Order for Possession Served to know recently ordered
 * evictions
 *
 * What days do courts handle LT cases?
 *
 * Write a system for saving suggested knownFullStops
 *
 * Do courts file at different regularities? On 1/23, I see
 * 2101 filed something on 1/22, but some courts haven't filed
 * any LT all January: 2102, 2203, 2208, 2303, 2307
 *
 * What Landlords have pending/active cases? Count.
 *
 * Idea: Order for possession served chart, by week.
 *
 * Idea: real-time pending evictions. Major dependency on courts updating electronic records
 *   Landlord must wait 10 days after grant of Judgment for Possession
 *     then she can request Order for Possession
 *     which gives 10 more days for tenant to vacate prior to forcible eviction
 * Grant possession = 20 days till forcible eviction
 * Order for Possession served = 10 days till forcible eviction
 * Needed: PdfData knows WHEN possession was granted,
 *         PdfData knows WHEN order for possession served
 *
 * Updater:
 * 1) Any case that is from prior year and not closed: scrape pdf
 * 2) Any case we HAVE that is from current year and not closed: scrape pdf
 * 3) Normal scrape of current year
 *
 * Sharpen meaning of plaintiff win
 *   Is 'judgment for Plaintiff' different than sum of grant and grant if?
 *
 * Hmmmm, NO cases this year for 2102, 2203, 2208, 2303, 2307? MDJ has different interpretation of LT rules now?
 *    Slow electronic filers?
 *
 * Are there judges more likely to dismiss?
 * Are there judges more likely to jump immediately to
 * * Investigate single day spikes. One landlord cleaning house?
 *  *
 *  *
 * Analyze week of 8/31, try to predict what will happen when moratorium is ended,
 *
 * Map eviction data
 *   %chance eviction map?
 *
 * Eviction % change with rent change? High rent --> less eviction?
 * Eviction % change with zip code?
 *
 * Look for waves of evictions
 *
 * a Lancaster 2020 has plaintiff names: Wolf & Kline Property Management, Akron Akron
 * How'd that extra Akron get in there???
 *
 * A plaintiff listed twice: Ulrich, Luke & Ulrich, Luke and Willa
 *
 MJ-02208-LT-0000085-2018 INVALID: inactive case isn't withdrawn, settled, dismissed, or granted
 *
 * ensure full closure of all system resources on scraper
 *
 * ALL active sheet
 *
 * MJ-02203-LT-0000104-2020    dismissed for one defendant, but not the other one
 *
 * If was 'active' on old pull, don't use local cached pdf on later calls
 *   cuz pdf will be updated
 *
 * NEEDED: validation of special format fields: zip, dates
 *   could validate sum of fees/costs add to judgment
 */

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.*;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.*;
import java.util.*;

public class Parser {
    public static final String TARGET_YEAR_FOR_MAIN = "2021";
    public static final String TARGET_COUNTY_FOR_MAIN = "Lancaster";
    public static final String TARGET_COURT_FOR_MAIN = "2101";
    public static final String TARGET_SEQUENCE_FOR_MAIN = "0000051";

    static String judgmentForDefendant = "Judgment for Defendant";
    static String judgmentForPlaintiff = "Judgment for Plaintiff";
    static String dispositionDate = "Disposition Date:";
    static String rentInArrearsS = "Rent in Arrears ";
    static String damages = "Physical Damages to Property ";
    static String rentReservedAndDue = "Rent Reserved and Due ";
    static String serverFeesS = "Server Fees ";
    static String filingFeeS = "Filing Fees ";
    static String attorneyFees = "Attorney Fees ";
    static String costsS = "Costs ";
    static String interestS = "Interest ";
    static String grantPossession = "Grant possession.";
    static String grantPossessionIf = "Grant possession if money judgment is not satisfied by the time of eviction.";

    static final String [] colHeaders = {
            "Court",
            "Presiding Judge",
            "Docket No.",
            "Date Filed",
            "Case Status",
            "Filer",
            "Filer ZIP",
            "Defendant",
            "Defendant ZIP",
            "Hearing Date",
            "Hearing Time",
            "Claim Amt",
            "Judgment Amount",
            "Rent in Arrears",
            "Filing Fees",
            "Costs",
            "Server Fees",
            "Damages",
            "Attorney Fees",
            "Rent Reserved and Due",
            "Interest",
            "Monthly Rent",
            "Withdrawn",
            "Dismissed",
            "Grant Poss",
            "Grant Pos if Judge Not Satisfied",
            "Order for Poss Req",
            "Order for Poss Served",
            "Judgment for Plaintiff",
            "Judgment for Defendant",
            "Settled",
            "Stayed",
            "Appealed",
            "Plaintiff Attorney",
            "Defendant Attorney",
            "Notes"
    };

    static enum SectionType {
        DOCKET("DOCKET", true),
        CASE_INFORMATION("CASE INFORMATION", true),
        CALENDAR_EVENTS("CALENDAR EVENTS", true),
        CASE_PARTICIPANTS("CASE PARTICIPANTS", true),
        DISPOSITION_SUMMARY("DISPOSITION SUMMARY", false),
        CIVIL_DISPOSITION_JUDGMENT_DETAILS("CIVIL DISPOSITION / JUDGMENT DETAILS", false),
        ATTORNEY_INFORMATION("ATTORNEY INFORMATION", false),
        DOCKET_ENTRY_INFORMATION("DOCKET ENTRY INFORMATION", true);

        String name;
        boolean required;
        SectionType(String name, boolean required) {
            this.name = name;
            this.required = required;
        }

        public String toString() {
            return name;
        }

        public boolean isRequired() {
            return required;
        }
    }

    private static Map<String, SectionType> stringToSectionType = new HashMap<>();

    private static final String[] nonPAStates = {"AK", "AL", "AR", "AS", "AZ", "CA", "CO", "CT", "DC", "DE", "FL", "GA", "GU", "HI", "IA", "ID", "IL", "IN", "KS", "KY", "LA", "MA", "MD", "ME", "MI", "MN", "MO", "MP", "MS", "MT", "NC", "ND", "NE", "NH", "NJ", "NM", "NV", "NY", "OH", "OK", "OR", "PR", "RI", "SC", "SD", "TN", "TX", "UM", "UT", "VA", "VI", "VT", "WA", "WI", "WV", "WY"};
    private static Set<String> states = new HashSet<>();

    static {
        for (SectionType sectionType: SectionType.values()) {
            stringToSectionType.put(sectionType.toString(), sectionType);
        }

        for (String s: nonPAStates) {
            states.add(s);
        }
    }


    public static void main (String[] args) {
        try {
            String pathToFile = "./src/main/resources/pdfCache/" +
                    TARGET_COUNTY_FOR_MAIN + "/" + TARGET_YEAR_FOR_MAIN +
                    "/" + TARGET_COURT_FOR_MAIN + "_" +
                    TARGET_SEQUENCE_FOR_MAIN + "_" +
                    TARGET_YEAR_FOR_MAIN +
                    ".pdf";

            PdfData data = processFile(pathToFile, true);
            System.out.println(data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static PdfData processFile(String fileName) {
        try {
            return processFile(fileName, false);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static PdfData processFile(String fileName, boolean printAll) throws Exception {
        return processFile(new File(fileName), printAll);
    }

    public static PdfData processFile(File file) throws IOException {
        return processFile(file, false);
    }

    public static PdfData processFile(File file, boolean printAll) throws IOException {
        try {
            InputStream targetStream = new FileInputStream(file);
            PdfData data = process(targetStream, printAll);
            targetStream.close();
            return data;
        } catch (Exception e) {
            System.err.println("processFile failed on " + file.getName());
            throw e;
        }
    }

    public static PdfData process (InputStream pdfStream, boolean printAll) throws IOException {
        try {
            PdfReader pdfReader = new PdfReader(pdfStream);
            PdfReaderContentParser parser = new PdfReaderContentParser(pdfReader);

            int pages = pdfReader.getNumberOfPages();
            String fullText = null;
            for (int page = 1; page <= pages; page++) {
                LocationTextExtractionStrategy location =
                        parser.processContent(page, new LocationTextExtractionStrategy());
                if (fullText == null) fullText = location.getResultantText();
                else fullText += "\n" + location.getResultantText();
            }

            String[] stringsWithExtraDockets = fullText.split("\n");

            if (printAll) {
                for (String s : stringsWithExtraDockets) {
                    System.out.println(s);
                }
            }

            //excise page headers ('DOCKET' sections) from page 2 on
            List<String> list = new ArrayList<>();
            boolean foundDocket = false;
            for (int x = 0; x < stringsWithExtraDockets.length; x++) {
                if (stringsWithExtraDockets[x].equals(SectionType.DOCKET.toString())) {
                    if (!foundDocket) {
                        list.add(stringsWithExtraDockets[x]);
                        foundDocket = true;
                    } else {
                        //second or later page. push to end of docket to continue previous page's section
                        for (int y = x + 1; y < stringsWithExtraDockets.length; y++) {
                            String probe = stringsWithExtraDockets[y];
                            String[] bits = probe.split(" ");
                            if (bits.length == 4 && bits[0].equals("Page") && bits[2].equals("of")) {
                                x = y;
                                break;
                            }
                        }
                    }
                }
                else {
                    list.add(stringsWithExtraDockets[x]);
                }
            }

            Object[] gfg= list.toArray();
            String[] strings = Arrays.copyOf(gfg,
                    gfg.length,
                    String[].class);

            TreeMap<Integer, SectionType> sectionStarts = buildSections(strings);

            List<Section> sections = new ArrayList<>();
            Integer lastStart = null;
            SectionType lastSectionType = null;
            for (Integer i: sectionStarts.keySet()) {
                SectionType thisSectionType = sectionStarts.get(i);
                if (lastStart != null) {
                    Section section = new Section();
                    section.setSectionType(lastSectionType);
                    if (lastSectionType == SectionType.DOCKET) {
                        section.setJudgeHeader(strings[lastStart-1]);
                    }
                    int minusCuzDocketJudge = thisSectionType == SectionType.DOCKET ? 1 : 0;
                    section.setStrings(Arrays.copyOfRange(strings, lastStart + 1, i - minusCuzDocketJudge));
                    sections.add(section);
                }
                lastStart = i;
                lastSectionType = thisSectionType;
            }
            Section section = new Section();
            section.setSectionType(lastSectionType);
            section.setStrings(Arrays.copyOfRange(strings, lastStart + 1, strings.length));
            if (lastSectionType == SectionType.DOCKET) {
                section.setJudgeHeader(strings[lastStart-1]);
            }
            sections.add(section);

            PdfData data = new PdfData();
            for (Section s: sections) {
                parseSection(s, data);
            }

            //System.out.println("\n****** Processed Data *****" + data);
            if (!data.isValid()) {
                data.validate(true);
            }

            return data;
        }
        catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        catch (NullPointerException npe) {
            System.out.println("NPE: probably page number doesn't exist");
            npe.printStackTrace();
            throw npe;
        }
    }

    private static TreeMap<Integer, SectionType> buildSections(String[] strings) {
        TreeMap<Integer, SectionType> ret = new TreeMap<>();

        for (int x = 0; x < strings.length; x++) {
            String string = strings[x];
            SectionType sectionType = stringToSectionType.get(string);
            if (sectionType != null) {
                ret.put(x, sectionType);
            }
        }

        return ret;
    }

    private static String judgeHeaderStart = "Magisterial District Judge ";
    private static String presidingJudgeStart = "Judge Assigned: Magisterial District Judge ";
    private static String magisterialDistrictJudge = "Magisterial District Judge ";
    private static String docketNumberStart = "Docket Number: ";

    private static String stay = "Rule 513 - Stay of Proceedings";
    private static String orderForPosReq = "Order for Possession Requested";
    private static String bankruptcy = "Bankruptcy Petition Filed";
    private static String orderForPosServed = "Order for Possession Successfully Served";
    private static String served = "Landlord/Tenant Complaint Successfully";
    private static String execution = "Order of Execution Issued";
    private static String commonPleas = "Certified Judgment to Common Pleas";
    //String handDelivery = "Hand Delivery";
    private static String transferred = "Case Transferred";
    private static String tenantWinByDismissal = "Dismissed";
    private static String dismissedWithPrejudice = "Dismissed with Prejudice";
    private static String withdrawn = "Withdrawn";
    private static String settled = "Settled";
    private static String possessionAppeal = "Landlord/Tenant Possession Appeal Filed";
    private static String monetaryAppeal = "Landlord/Tenant Monetary Appeal Filed";
    private static String abandonment = "Home and Property Abandoned";

    private static void parseSection(Section section, PdfData data) {
        String[] strings = section.getStrings();
        SectionType sectionType = section.getSectionType();
        if (sectionType == SectionType.DOCKET) {
            String judgeHeader = section.getJudgeHeader();
            data.setCourtOffice("MDJ " + judgeHeader.substring(judgeHeaderStart.length()));
            data.setDocketNumber(strings[0].substring(docketNumberStart.length()));
        }
        else if (sectionType == SectionType.CASE_INFORMATION) {
            //example2
//            09/04/2020
//            Judge Assigned: Magisterial District Judge Jonathan W. File Date:
//            Heisse
//            $6,190.00 Closed
//            Claim Amount: Case Status:
//            Lancaster
//            Judgment Amount: County:

            //example4: has judgment
//            11/25/2020
//            Judge Assigned: Magisterial District Judge Jonathan W. File Date:
//            Heisse
//            $0.00 Closed
//            Claim Amount: Case Status:
//            $217.35 Lancaster
//            Judgment Amount: County:

            //2102-LT-0000001-2020: date on same line as other strings
//            CASE INFORMATION
//            Magisterial District Judge David P. Miller 01/02/2020
//            Judge Assigned: File Date:
//            $1,800.00 Closed
//            Claim Amount: Case Status:
//            $2,248.35 Lancaster
//            Judgment Amount: County:

            //Missing claim amount (normally $____ or at least zero!)
            //2304-0000026-2018
            //Magisterial District Judge Stuart J. Mylin 02/21/2018
            //Judge Assigned: File Date:
            //Closed
            //Claim Amount: Case Status:
            //$2,489.50 Lancaster
            //Judgment Amount: County:

            //if first line is one string, it's the date
            //otherwise, date is at end after name
            String next;
            String judgeName = null;
            int index = 0;
            String[] split = strings[0].split(" ");
            if (split.length == 1) {
                data.setFileDate(strings[0]);
                next = strings[1];
                judgeName = next.substring(presidingJudgeStart.length(), next.indexOf(" File Date:"));
                index = 2;
                next = strings[index];
                if (next.indexOf('$') != 0) {
                    judgeName += " " + next;
                    index++;
                }
            }
            else {
                int mdjI = strings[0].indexOf(magisterialDistrictJudge);
                //'judge' doesn't have honorable prefix
                if (mdjI < 0) {
//                    split = strings[0].split(" ");
//                    data.setFileDate(split[split.length - 1]);
//                    for (int x = 0; x < split.length - 1; x++) {
//                        if (x > 0) judgeName += " " + split[x];
//                        else judgeName = split[x];
//                    }
                    next = strings[0].trim();
                    int lastSpace = next.lastIndexOf(' ');
                    data.setFileDate(next.substring(lastSpace + 1));
                    judgeName = next.substring(0, lastSpace);
                    index = 2;
                }
                else {
                    next = strings[0].substring(magisterialDistrictJudge.length());
                    split = next.split(" ");
                    data.setFileDate(split[split.length - 1]);
                    for (int x = 0; x < split.length - 1; x++) {
                        if (x > 0) judgeName += " " + split[x];
                        else judgeName = split[x];
                    }
                    index = 2;
                }
            }
            data.setJudgeName(judgeName);

            //claim and status
            //Once, was only status (blank claim)
            //(see snippet above)
            next = strings[index];
            String[] twoItems = next.split(" ");
            if (twoItems.length == 2) {
                data.setClaim(twoItems[0]);
                data.setCaseStatus(twoItems[1]);
            }
            else if (twoItems.length > 2) {
                throw new IllegalStateException("Can't have more than 2 strings here");
            }
            else {
                String single = twoItems[0];
                if (single.indexOf('$') > -1) {
                    data.setClaim(single);
                    data.setCaseStatus(PdfData.MISSING_CASE_STATUS);
                }
                else {
                    data.setClaim(PdfData.MISSING_CLAIM);
                    data.setCaseStatus(single);
                }
            }

            //judgment amount
            index+=2;
            next = strings[index];
            if (next.indexOf('$') == 0) {
                data.setJudgment(next.substring(0, next.indexOf(' ')));
            }
        }
        else if (sectionType == SectionType.CALENDAR_EVENTS) {
            //example 4: just one event
//            CALENDAR EVENTS
//            Case Calendar Schedule Schedule
//            Start Time Room Judge Name
//            Event Type Start Date Status
//            12/04/2020 10:00 am Scheduled
//            Recovery of Real Property Magisterial District Judge
//            Hearing Jonathan W. Heisse

            //example 1: three events
//            Case Calendar Schedule Schedule
//            Start Time Room Judge Name
//            Event Type Start Date Status
//            09/23/2020 10:45 am Continued
//            Recovery of Real Property Magisterial District Judge
//            Hearing Jonathan W. Heisse
//            Recovery of Real Property 01/11/2021 1:30 pm Magisterial District Judge Continued
//            Hearing Jonathan W. Heisse
//            02/16/2021 2:15 pm Scheduled
//            Recovery of Real Property Magisterial District Judge
//            Hearing Jonathan W. Heisse

            //'moved' example from MJ-02101-LT-0000158-2020
//            Case Calendar Schedule Schedule
//            Start Time Room Judge Name
//            Event Type Start Date Status
//            09/18/2020 10:15 am Moved
//            Recovery of Real Property Magisterial District Judge Adam
//            Hearing J. Witkonis
//            Recovery of Real Property 09/22/2020 10:15 am Magisterial District Judge Adam Scheduled
//            Hearing J. Witkonis

            StringBuffer sb = new StringBuffer();
            for (String s: strings) {
                s = s.trim();
                sb.append(s + " ");
            }
            String all = sb.toString();

            boolean done = false;
            int continued = 0;
            String hearingDate = null;
            String hearingTime = null;
            while (!done && all.indexOf('/') > -1) {
                int nextSlash = all.indexOf('/');
                if (nextSlash > -1) {
                    hearingDate = all.substring(nextSlash - 2, nextSlash + 8);
                    all = all.substring(nextSlash + 9);
                    int nextSpace = all.indexOf(' ');
                    hearingTime = all.substring(0, nextSpace);
                    //am or pm?
                    all = all.substring(nextSpace + 1);
                    hearingTime += " " + all.substring(0, 2);

                    String nextKeyWord = getNextKeyWord(all, "Scheduled", "Continued", "Cancelled", "Moved");
                    if (nextKeyWord != null) {
                        if (nextKeyWord.equals("Scheduled")) {
                            data.setScheduledDate(hearingDate);
                            data.setScheduledHour(hearingTime);
                        }
                        else if (nextKeyWord.equals("Continued")) {
                            continued++;
                        }
                        //ignoring Moved and Cancelled
                    }

//                    int nextScheduled = all.indexOf("Scheduled");
//                    int nextContinued = all.indexOf("Continued");
//                    if (nextScheduled > -1 && nextContinued > -1) {
//                        if (nextContinued > -1 && nextContinued < nextScheduled) {
//                            continued++;
//                        }
//                        else {
//                            done = true;
//                            data.setHearingDate(hearingDate);
//                            data.setHearingTime(hearingTime);
//                        }
//                    }
//                    else if (nextScheduled > -1) {
//                        done = true;
//                        data.setHearingDate(hearingDate);
//                        data.setHearingTime(hearingTime);
//                    }
//                    else if (nextContinued > -1) {
//                        continued++;
//                    }
//                    else if (all.indexOf("Cancelled") > -1) {
//                        done = true;
//                    }
//                    else {
//                        System.err.println("Missing a calendar event status for a hearing time?");
//                    }
                }
            }

            if (continued > 0) data.addNote("Continued " + continued + " time(s)");
        }
        else if (sectionType == SectionType.CASE_PARTICIPANTS) {
            //PATTERN 1
            //2306-0000001-2020
//            Participant Type Participant Name Address
//            Defendant New Holland, PA 17557
//            Greist, Robin
//            Plaintiff New Holland, PA 17557
//            KS Rentals LLC

            //PATTERN 2
            //2101-0000040-2020
            //Address
            //Participant Type Participant Name
            //Plaintiff Royersford Gardens LTD,  Southeastern Southeastern, PA 19399
            //Defendant Perez Lozada, Ismael Lancaster, PA 17603

            //PATTERN 3
            //2101-0000045-2020
//            Participant Type Participant Name Address
//            Plaintiff Lancaster, PA 17603
//            Ben Zee Group in care of Rick
//            Wennerstrom's Property Management,
//            Lancaster
//            Defendant Wilson, Domouniq Lancaster, PA 17603

            //PATTERN 4 no zip for defendant?!?
//            Participant Type Participant Name Address
//            Defendant Lancaster, PA 17602
//            Valentin, Ruth
//            Plaintiff Lancaster, PA
//            Hostetter, Jeffrey

            //PATTERN 5 address is part of name as well as standalone addres
//            Participant Type Participant Name Address
//            Defendant Lancaster, PA 17602
//            Semprit, Yaceila
//            Plaintiff Lancaster, PA 17602
//            Hillrise Mutual Housing Association Inc,
//                    455 Rockland Street Lancaster Pa 17602

            //PATTERN 6
//            Address
//            Participant Type Participant Name
//            Plaintiff Trademark Property Management, LLC,  Lancaster, PA 17603
//            Lancaster
//            Defendant Lancaster, PA 17603
//            Santos, Junaito Deaza

            //name place zip
            //place zip name

            //'PATTERN 1' (see above)
            //1) get chunks that map Defendant --> String, Plantiff --> String
            //2) split chunk on space.
            //3) numeric for zip
            //4) everything after zip is name
            int firstChunkLine = -1;
            for (int x = 0; x < strings.length; x++) {
                String next = strings[x];
                if (next.indexOf("Defendant") > -1 || next.indexOf("Plaintiff") > -1) {
                    firstChunkLine = x;
                    break;
                }
            }
            if (firstChunkLine == -1) {
                String msg = "Must have a defendant or a plaintiff";
                System.err.println(msg);
                throw new IllegalStateException(msg);
            }
            List<List<String>> plaintiffs = new ArrayList<>();
            List<List<String>> defendants = new ArrayList<>();

            List<String> current = null;
            for (int x = firstChunkLine; x < strings.length; x++) {
                String line = strings[x];
                //hit a double space in a plaintiff name once
                line = line.replace("  ", " ");
                //if no space, it's a trailing line that's part of name
                int firstSpaceI = line.indexOf(' ');
                boolean addedToCurrent = false;
                if (firstSpaceI > -1) {
                    String firstWord = line.substring(0, firstSpaceI);
                    if (firstWord.equals("Plaintiff") || firstWord.equals("Defendant")) {
                        current = new ArrayList<>();
                        if (firstWord.equals("Plaintiff")) {
                            plaintiffs.add(current);
                        } else {
                            defendants.add(current);
                        }
                        current.add(line.substring(line.indexOf(' ') + 1).trim());
                        addedToCurrent = true;
                    }
                }
                if (!addedToCurrent) {
                    current.add(line);
                }
            }

//            String all = "";
//            for (int x = firstChunkLine; x < strings.length; x++) {
//                String next = strings[x].trim();
//                all += next + " ";
//            }
//            String terminator = "|||terminal|||";
//            all += terminator;
//            String chunk = "";
//            String[] split = all.split(" ");
//            for (String s : split) {
//                if (s.equals("Defendant") || s.equals("Plaintiff") || s.equals(terminator)) {
//                    if (chunk.length() > 0) {
//                        boolean plaintiff = chunk.indexOf("Plaintiff") > -1;
//                        chunk = chunk.substring(chunk.indexOf(' ') + 1);
//                        if (plaintiff) plaintiffs.add(chunk);
//                        else defendants.add(chunk);
//                        chunk = "";
//                    }
//                }
//                if (chunk.length() > 0) chunk += " ";
//                chunk += s;
//            }

            String[] plaintiffData = processParticipantChunks(plaintiffs);
            String[] defendantData = processParticipantChunks(defendants);

            data.setPlaintiffs(plaintiffData[0]);
            data.setPlaintiffZips(plaintiffData[1]);
            data.setDefendant(defendantData[0]);
            data.setDefendantZips(defendantData[1]);
 //           }
            //'PATTERN 2' (see example above)
//            else if (strings[0].equals("Address")) {
//                String plaintiffs = null;
//                String plaintiffZips = null;
//                String defendants = null;
//                String defendantZips = null;
//                for (int x = 2; x < strings.length; x++) {
//                    String next = strings[x];
//                    boolean plaintiff = next.indexOf("Plaintiff") == 0;
//                    next = next.substring(next.indexOf(' ') + 1);
//                    int comma = next.indexOf(',');
//                    if (comma < 0) {
//                        throw new IllegalStateException("No comma in PARTICIPANT PATTER 2");
//                    }
//                    String preComma = next.substring(0, comma + 1).trim();
//                    next = next.substring(comma + 1).trim();
//                    String[] split = next.split(" ");
//                    String name = preComma + " " + split[0];
//                    String zip = split[split.length - 1];
//
//                    if (plaintiff) {
//                        if (plaintiffs == null) plaintiffs = name;
//                        else plaintiffs += " & " + name;
//
//                        if (plaintiffZips == null) plaintiffZips = zip;
//                        else if (plaintiffZips.indexOf(zip) < 0) {
//                            plaintiffZips += " & " + zip;
//                        }
//                    }
//                    else {
//                        if (defendants == null) defendants = name;
//                        else defendants += " & " + name;
//
//                        if (defendantZips == null) defendantZips = zip;
//                        else if (defendantZips.indexOf(zip) < 0) {
//                            defendantZips += " & " + zip;
//                        }
//                    }
//                }
//                data.setPlaintiffs(plaintiffs);
//                data.setPlaintiffZips(plaintiffZips);
//                data.setDefendant(defendants);
//                data.setDefendantZips(defendantZips);
//            }
        }
        else if (sectionType == SectionType.DISPOSITION_SUMMARY ||
                sectionType == SectionType.CIVIL_DISPOSITION_JUDGMENT_DETAILS) {
//2306-0000001-2020
//            Disposition Date:  01/15/2020 Monthly Rent:  $750.00
//            Joint/Several Individual Net
//            Defendant(s) Plaintiff(s) Disposition
//            Liability Liability Judgment
//            Robin Greist KS Rentals LLC Judgment for Plaintiff $0.00 $3,548.25 $3,548.25
//            Judgment Components:
//            Type Amount Deposit Amount Adjusted Amount
//            Rent in Arrears $3,360.00 $0.00 $3,360.00
//            Server Fees $21.25 $0.00 $21.25
//            Costs $5.00 $0.00 $5.00
//            Interest $140.75 $0.00 $140.75
//                    * Is Joint/Several
//            Civil Disposition Details:
//            No
//            Grant possession.
//            Yes
//            Grant possession if money judgment is not satisfied by the time of eviction.
//            MDJS 1200 Printed: 01/14/2021  10:18 am
//            Recent entries made in the court filing offices may not be immediately reflected on these docket sheets . Neither the courts of the Unified Judicial System of
//            the Commonwealth of Pennsylvania nor the Administrative Office of Pennsylvania Courts assumes any liability for inaccurate or delayed data , errors or
//            omissions on these docket sheets.  You should verify that the information is accurate and current by personally consulting the official record reposing in
//            the court wherein the record is maintained.
            //doesn't occur when it's only 'Disposition Summary'
            if (strings[0].indexOf("Monthly") > -1) {
                String[] split = strings[0].split(" ");
                data.setMonthlyRent(split[split.length - 1]);
            }

            String previous = null;
            for (String s: strings) {
                if (s.indexOf(dispositionDate) > -1) {
                    String other = s;
                    other = other.substring(dispositionDate.length()).trim();
                    data.setDispositionDate(other.substring(0, other.indexOf(' ')));
                }
                if (s.indexOf(judgmentForDefendant) > -1) {
                    data.judgmentForDefendant(true);
                }
                else if (s.indexOf(judgmentForPlaintiff) > -1) {
                    data.judgmentForPlaintiff(true);
                }
                else if (s.indexOf(rentInArrearsS) > -1) {
                    //fancy cuz sometimes preceded by asterisk
                    int i = s.indexOf(rentInArrearsS);
                    String m = s.substring(i + rentInArrearsS.length());
                    m = m.substring(0, m.indexOf(' '));
                    data.setRentInArrears(m);
                }
                else if (s.indexOf(damages) > -1) {
                    int i = s.indexOf(damages);
                    String m = s.substring(i + damages.length());
                    m = m.substring(0, m.indexOf(' '));
                    data.setDamages(m);
                }
                else if (s.indexOf(serverFeesS) > -1) {
                    int i = s.indexOf(serverFeesS);
                    String m = s.substring(i + serverFeesS.length());
                    m = m.substring(0, m.indexOf(' '));
                    data.setServerFees(m);
                }
                else if (s.indexOf(rentReservedAndDue) > -1) {
                    int i = s.indexOf(rentReservedAndDue);
                    String m = s.substring(i + rentReservedAndDue.length());
                    m = m.substring(0, m.indexOf(' '));
                    data.setRentReservedAndDue(m);
                }
                else if (s.indexOf(attorneyFees) > -1) {
                    int i = s.indexOf(attorneyFees);
                    String m = s.substring(i + attorneyFees.length());
                    m = m.substring(0, m.indexOf(' '));
                    data.setAttorneyFees(m);
                }
                else if (s.indexOf(filingFeeS) > -1) {
                    int i = s.indexOf(filingFeeS);
                    String m = s.substring(i + filingFeeS.length());
                    m = m.substring(0, m.indexOf(' '));
                    data.setFilingFees(m);
                }
                else if (s.indexOf(costsS) > -1) {
                    int i = s.indexOf(costsS);
                    String m = s.substring(i + costsS.length());
                    m = m.substring(0, m.indexOf(' '));
                    data.setCosts(m);
                }
                else if (s.indexOf(interestS) > -1) {
                    int i = s.indexOf(interestS);
                    String m = s.substring(i + interestS.length());
                    m = m.substring(0, m.indexOf(' '));
                    data.setInterest(m);
                }
                else if (s.indexOf(grantPossession) > -1) {
                    if (previous.equals("No") || previous.equals("Yes")) {
                        data.setGrantPossession(previous);
                    }
                    //once found a Yes at end of grantPossIf (2306-0000034-2020)
                    else {
                        int i = s.indexOf(grantPossession);
                        String yn = s.substring(i + grantPossession.length()).trim();
                        if (!(yn.equals("No") || yn.equals("Yes"))) {
                            throw new IllegalStateException("Can't find Yes or No for grantPoss");
                        }
                        data.setGrantPossession(yn);
                    }
                }
                else if (s.indexOf(grantPossessionIf) > -1) {
                    if (previous.equals("No") || previous.equals("Yes")) {
                        data.setGrantPossessionIf(previous);
                    }
                    //once found a Yes at end of grantPossIf (2306-0000034-2020)
                    else {
                        int i = s.indexOf(grantPossessionIf);
                        String yn = s.substring(i + grantPossessionIf.length()).trim();
                        if (!(yn.equals("No") || yn.equals("Yes"))) {
                            throw new IllegalStateException("Can't find Yes or No for grantPossIf");
                        }
                        data.setGrantPossessionIf(yn);
                    }
                }

                previous = s;
            }
        }
        else if (sectionType == SectionType.ATTORNEY_INFORMATION) {
            List<String> names = new ArrayList<>();
            List<String> representing = new ArrayList<>();
            for (int x = 0; x < strings.length; x++) {
                if (strings[x].indexOf("Name") == 0) {
                    String s = strings[x];
                    while (s.indexOf("Name:") > -1) {
                        int nameI = s.indexOf("Name:");
                        //5 is char count of "Name:"
                        s = s.substring(nameI + 5);
                        int nextNameI = s.indexOf("Name:");
                        if (nextNameI < 0) {
                            names.add(s.trim());
                        }
                        else {
                            names.add(s.substring(0, nextNameI).trim());
                        }
                    }
                }
                else if (strings[x].indexOf("Representing") == 0) {
                    String s = strings[x];
                    while (s.indexOf("Representing:") > -1) {
                        int representingI = s.indexOf("Representing:");
                        //13 is char count of "Representing:"
                        s = s.substring(representingI + 13);
                        int nextNameI = s.indexOf("Representing:");
                        if (nextNameI < 0) {
                            representing.add(s.trim());
                        }
                        else {
                            representing.add(s.substring(0, nextNameI).trim());
                        }
                    }
                }
            }

            if (names.size() != representing.size()) {
                throw new IllegalStateException("unequal list sizes in ATTORNEY section");
            }

            for (int y = 0; y < names.size(); y++) {
                String name = names.get(y);
                String rep = representing.get(y);
                //once had a double space
                data.addAttorney(name, rep.replace("  ", " "));
            }

//            System.out.println("ATTORNEY INFORMATION " + data.getDocketNumber());
//            for (String s: strings) {
//                System.out.println(s);
//            }
        }
        else if (sectionType == SectionType.DOCKET_ENTRY_INFORMATION) {
//2306-0000001-2020
//            Filed Date Entry Filer Applies To
//            01/28/2020 Order for Possession Successfully Served Magisterial District Court 02-3-06 Robin Greist, Defendant
//            01/28/2020 Order for Possession Issued Magisterial District Court 02-3-06 Robin Greist, Defendant
//            01/28/2020 First Class Order for Possession Issued Magisterial District Court 02-3-06 Robin Greist, Defendant
//            01/28/2020 Order for Possession Requested KS Rentals LLC Robin Greist, Defendant
//            01/15/2020 Judgment for Plaintiff Magisterial District Court 02-3-06 Robin Greist, Defendant
//            01/15/2020 Judgment Entered Magisterial District Court 02-3-06 Robin Greist, Defendant
//            01/05/2020 Magisterial District Court 02-3-06 Robin Greist, Defendant
//            Landlord/Tenant Complaint Successfully
//            Served
//            01/03/2020 Magisterial District Court 02-3-06 Robin Greist, Defendant
//            Landlord/Tenant Complaint Issued via
//            Hand Delivery
//            01/03/2020 Landlord/Tenant Complaint Filed KS Rentals LLC
//            MDJS 1200 Page 2 of 2 Printed: 01/14/2021  10:18 am
//            Recent entries made in the court filing offices may not be immediately reflected on these docket sheets . Neither the courts of the Unified Judicial System of
//            the Commonwealth of Pennsylvania nor the Administrative Office of Pennsylvania Courts assumes any liability for inaccurate or delayed data , errors or
//            omissions on these docket sheets.  You should verify that the information is accurate and current by personally consulting the official record reposing in
//            the court wherein the record is maintained.

            for (String s: strings) {
                if (s.indexOf(orderForPosReq) > -1) {
                    String date = s.substring(0, s.indexOf(' '));
                    data.setOrderForPossessionRequested(true, date);
                }
                else if (s.indexOf(stay) > -1) {
                    data.setStayed(stay);
                }
                else if (s.indexOf(orderForPosServed) > -1) {
                    data.setOrderForPossessionServed(true);
                }
                else if (s.indexOf(execution) > -1) {
                    data.addNote(execution);
                }
                else if (s.indexOf(commonPleas) > -1) {
                    data.addNote(commonPleas);
                }
                else if (s.indexOf(served) > -1) {
                    data.setServed(true);
                }
//                else if (s.indexOf(handDelivery) > -1) {
//                    data.setServed(true);
//                    data.addNote("Served via hand delivery");
//                }
                else if (s.indexOf(tenantWinByDismissal) > -1) {
                    data.setDismissed(true);
                    if (s.indexOf(dismissedWithPrejudice) > -1) {
                        data.setDismissedWithPrejudice(true);
                    }
                }
                else if (s.indexOf(withdrawn) > -1) {
                    data.setWithdrawn(true);
                }
                else if (s.indexOf(settled) > -1) {
                    String maybeDate = null;
                    if (Character.isDigit(s.charAt(0))) {
                        maybeDate = s.substring(0, s.indexOf(' '));
                    }
                    data.setSettled(true, maybeDate);
                }
                else if (s.indexOf(transferred) > -1) {
                    data.addNote("Case transferred");
                }
                else if (s.indexOf(possessionAppeal) > -1) {
                    data.setAppeal(possessionAppeal);
                }
                else if (s.indexOf(bankruptcy) > -1) {
                    data.bankruptcy("Bankruptcy Petition Filed");
                }
                else if (s.indexOf(monetaryAppeal) > -1) {
                    data.setAppeal(monetaryAppeal);
                }
                else if (s.indexOf(abandonment) > -1) {
                    data.addNote(abandonment);
                }
            }
        }
    }

    private static String getNextKeyWord(String all, String... keys) {
        int best = -1;
        String bestS = null;
        for (String key: keys) {
            int i = all.indexOf(key);
            if (best == -1 || (i > -1 && i < best)) {
                best = i;
                bestS = key;
            }
        }
        return best < 0 ? null : bestS;
    }

    //out: 0 is & delimited names, 1 is & delimited zips
    private static String[] processParticipantChunks(List<List<String>> plaintiffGroups) {
        List<String> names = new LinkedList<>();
        List<String> zips = new LinkedList<>();

        //filter dups, as discovered in 2306-0000034-2020
        List<List<String>> filteredChunks = new ArrayList<>();
        for (List<String> chunk: plaintiffGroups) {
            if (!checkForMatchingListOfStrings(filteredChunks, chunk)) {
                filteredChunks.add(chunk);
            }
        }
        for (List<String> chunk: filteredChunks) {
            String [] processed = processParticipantChunk(chunk);
            if (processed != null) {
                names.add(processed[0]);
                if (processed[1] != null) zips.add(processed[1]);
            }
        }
        if (zips.isEmpty()) zips.add(PdfData.MISSING_ZIP);
        String [] ret = {"", ""};
        for (String n: names) {
            if (ret[0].length() > 0) ret[0] += " & ";
            ret[0] += n.trim();
        }
        Set<String> uniqueZips = new HashSet<>();
        for (String n: zips) {
            if (!uniqueZips.contains(n.trim())) {
                if (ret[1].length() > 0) ret[1] += " & ";
                ret[1] += n.trim();
                uniqueZips.add(n.trim());
            }
        }
        return ret;
    }

    private static boolean checkForMatchingListOfStrings(List<List<String>> master, List<String> chunk) {
        if (master.isEmpty()) return false;
        boolean foundAMatch = false;
        for (List<String> child: master) {
            foundAMatch = true;
            if (child.size() != chunk.size()) foundAMatch = false;
            else {
                for (int x = 0; x < child.size(); x++) {
                    if (!child.get(x).equals(chunk.get(x))) foundAMatch = false;
                }
            }
            if (foundAMatch) break;
        }
        return foundAMatch;
    }

    //city zip name in
    //{name, zip} out. zip can be null if missing zip

    //could be:
    //name place zip
    //OR
    //place zip name
    //could probe for whether comma is followed by 'PA', but for now
    //I'll check for zip location
    private static String[] processParticipantChunk(List<String> strings) {
        List<String> noDoubleSpaces = new ArrayList<>(strings.size());
        for (String s: strings) {
            noDoubleSpaces.add(s.replace("  ", " "));
        }
        strings = noDoubleSpaces;

        String[] ret = new String[2];

        //either way, it's .....zip on first line (unless zip is MISSING ex: 2201_0000002_2020)
        String firstString = strings.get(0);
        String[] all = firstString.split(" ");
        String probablyZipUnlessZipIsMissing = all[all.length - 1];
        if (NumberUtils.isCreatable(probablyZipUnlessZipIsMissing)) {
            ret[1] = probablyZipUnlessZipIsMissing;
        }

        //if length is 1, it's
        //name place zip
        //if commaCount is > 1 and length is not 1,
        //it's
        //name place zip
        //more name
        long commaCount = firstString.chars().filter(ch -> ch == ',').count();
        if (strings.size() == 1 || commaCount > 1) {
            int comma = firstString.indexOf(',');
            String toComma = firstString.substring(0, comma + 1);
            //common case
            if (commaCount > 1) {
                String postComma = firstString.substring(comma + 1).trim();
                postComma = postComma.substring(0, postComma.indexOf(' '));
                ret[0] = toComma + " " + postComma;
            }
            //exception: MJ-02203-LT-0000079-2020 has...
            //Quail Run Southeastern PA, 19399
            //not perfect, but lop off last town word and PA
            else {
                //catch the (so far) singular instance of massive data entry error on
                //pdf, listing 4 plaintiffs when there was only one, so that three
                //ended up just being one line with [city, PA zip]
                //^ 2201_0000047_2020
                int lastSpace = toComma.lastIndexOf(' ');
                if (lastSpace < 0) return null;
                toComma = toComma.substring(0, lastSpace);
                ret[0] = toComma.trim();
            }
        }
        //if there are two strings (or more) it's
        //place zip
        //name
        //OR
        //name place zip
        //more name
        if (strings.size() > 1) {
            for (int x = 1; x < strings.size(); x++) {
                String next = strings.get(x);
                if (ret[0] == null) ret[0] = next.trim();
                else ret[0] += " " + next.trim();
            }
        }

        return ret;
    }

    private static class Section {
        private SectionType sectionType;
        private String[] strings;
        private String judgeHeader;

        public void setStrings(String[] strings) {
            this.strings = strings;
        }

        public void setSectionType(SectionType sectionType) {
            this.sectionType = sectionType;
        }

        public void setJudgeHeader(String judgeHeader) {
            this.judgeHeader = judgeHeader;
        }

        public String toString() {
            return sectionType.toString();
        }

        public SectionType getSectionType() {
            return sectionType;
        }

        public String[] getStrings() {
            return strings;
        }

        public String getJudgeHeader() {
            return judgeHeader;
        }
    }
}

