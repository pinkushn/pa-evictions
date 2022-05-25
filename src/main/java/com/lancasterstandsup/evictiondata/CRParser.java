package com.lancasterstandsup.evictiondata;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

/**
 * 5/8/22 next up: probe for other section types
 */

public class CRParser implements Parser {
    static final String [] colHeaders = {
            "Court",
            "Presiding Judge",
            "Docket No.",
            "Date Filed",
            "Case Status",
            "Plaintiff",
            "Plaintiff ZIP",
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
        STATUS_INFORMATION("STATUS INFORMATION", false),
        CALENDAR_EVENTS("CALENDAR EVENTS", true),
        DEFENDANT_INFORMATION("DEFENDANT INFORMATION", false),
        CASE_PARTICIPANTS("CASE PARTICIPANTS", true),
        BAIL("BAIL", false),
        CHARGES("CHARGES", false),
        DISPOSITION_SENTENCING_DETAILS("DISPOSITION / SENTENCING DETAILS", false),
        ATTORNEY_INFORMATION("ATTORNEY INFORMATION", false),
        DOCKET_ENTRY_INFORMATION("DOCKET ENTRY INFORMATION", true),
        CASE_FINANCIAL_INFORMATION("CASE FINANCIAL INFORMATION", false),
        PAYMENT_PLAN_SUMMARY("PAYMENT PLAN SUMMARY", false),

        //DISPOSITION_SUMMARY("DISPOSITION SUMMARY", false),
        //CIVIL_DISPOSITION_JUDGMENT_DETAILS("CIVIL DISPOSITION / JUDGMENT DETAILS", false),

        CONFINEMENT("CONFINEMENT", false);

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

    public static final String SECTION_SIGN = "§";

    private static Map<String, CRParser.SectionType> stringToSectionType = new HashMap<>();

    private static final String[] nonPAStates = {"AK", "AL", "AR", "AS", "AZ", "CA", "CO", "CT", "DC", "DE", "FL", "GA", "GU", "HI", "IA", "ID", "IL", "IN", "KS", "KY", "LA", "MA", "MD", "ME", "MI", "MN", "MO", "MP", "MS", "MT", "NC", "ND", "NE", "NH", "NJ", "NM", "NV", "NY", "OH", "OK", "OR", "PR", "RI", "SC", "SD", "TN", "TX", "UM", "UT", "VA", "VI", "VT", "WA", "WI", "WV", "WY"};
    private static Set<String> states = new HashSet<>();

    private static HashSet<String> placeNames = new HashSet<>();

    static {
        for (CRParser.SectionType sectionType: CRParser.SectionType.values()) {
            stringToSectionType.put(sectionType.toString(), sectionType);
        }

        for (String s: nonPAStates) {
            states.add(s);
        }
    }


    private static String judgeHeaderStart = "Magisterial District Judge ";
    //private static String presidingJudgeStart = "Judge Assigned: Magisterial District Judge ";
    //private static String magisterialDistrictJudge = "Magisterial District Judge ";
    private static String docketStart = "Docket Number: ";


    public static final String TARGET_YEAR_FOR_MAIN = "2022";
    public static final String TARGET_COUNTY_FOR_MAIN = "Lancaster";
    public static final String TARGET_COURT_FOR_MAIN = "2000";
    public static final String TARGET_SEQUENCE_FOR_MAIN = "0000001";
    public static void main (String[] args) {
        try {
            String pathToFile = Scraper.PDF_CACHE_PATH + "CR/" +
                    TARGET_COUNTY_FOR_MAIN + "/" + TARGET_YEAR_FOR_MAIN +
                    "/" + TARGET_COURT_FOR_MAIN + "_" +
                    TARGET_SEQUENCE_FOR_MAIN + "_" +
                    TARGET_YEAR_FOR_MAIN +
                    ".pdf";

            CRPdfData data = processFile(pathToFile, true);
            System.out.println(data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static CRPdfData processFile(String fileName, boolean printAll) throws Exception {
        return processFile(new File(fileName), printAll);
    }

    public CRPdfData processFile(File file) throws IOException, NoSuchFieldException {
        return processFile(file, false);
    }

    public static CRPdfData processFile(File file, boolean printAll) throws IOException, NoSuchFieldException {
        try {
            InputStream targetStream = new FileInputStream(file);
            CRPdfData data = getSingleton().process(targetStream, printAll);
            targetStream.close();
            return data;
        } catch (Exception e) {
            System.err.println("processFile failed on " + file.getName());
            throw e;
        }
    }

    private static CRParser singleton = new CRParser();
    public static CRParser getSingleton() {
        return singleton;
    }

    public CRPdfData process (InputStream pdfStream, boolean printAll) throws IOException, NoSuchFieldException {
        try {
            PdfReader pdfReader = new PdfReader(pdfStream);
            PdfReaderContentParser parser = new PdfReaderContentParser(pdfReader);

            int pages = pdfReader.getNumberOfPages();
            String fullText = null;
            for (int page = 1; page <= pages; page++) {
                LocationTextExtractionStrategy location =
                        parser.processContent(page, new HorizontalTextExtractionStrategy2());
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
                if (stringsWithExtraDockets[x].equals(CRParser.SectionType.DOCKET.toString())) {
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

            TreeMap<Integer, CRParser.SectionType> sectionStarts = buildSections(strings);

            List<CRParser.Section> sections = new ArrayList<>();
            Integer lastStart = null;
            CRParser.SectionType lastSectionType = null;
            for (Integer i: sectionStarts.keySet()) {
                CRParser.SectionType thisSectionType = sectionStarts.get(i);
                if (lastStart != null) {
                    CRParser.Section section = new CRParser.Section();
                    section.setSectionType(lastSectionType);
                    if (lastSectionType == CRParser.SectionType.DOCKET) {
                        section.setJudgeHeader(strings[lastStart-1]);
                    }
                    int minusCuzDocketJudge = thisSectionType == CRParser.SectionType.DOCKET ? 1 : 0;
                    section.setStrings(Arrays.copyOfRange(strings, lastStart + 1, i - minusCuzDocketJudge));
                    sections.add(section);
                }
                lastStart = i;
                lastSectionType = thisSectionType;
            }
            CRParser.Section section = new CRParser.Section();
            section.setSectionType(lastSectionType);
            section.setStrings(Arrays.copyOfRange(strings, lastStart + 1, strings.length));
            if (lastSectionType == CRParser.SectionType.DOCKET) {
                section.setJudgeHeader(strings[lastStart-1]);
            }
            sections.add(section);

            CRPdfData data = new CRPdfData();
            for (CRParser.Section s: sections) {
                parseSection(s, data);
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
        catch (Exception eee) {
            System.err.println(eee);
            eee.printStackTrace();
            throw eee;
        }
    }

    private static void parseSection(CRParser.Section section, CRPdfData data) throws IOException {
        String[] strings = section.getStrings();
        CRParser.SectionType sectionType = section.getSectionType();

        if (sectionType == CRParser.SectionType.DOCKET) {
            String judgeHeader = section.getJudgeHeader();
            data.setCourtOffice("MDJ " + judgeHeader.substring(judgeHeaderStart.length()));
            data.setDocket(strings[0].substring(docketStart.length()));
        }
        else if (sectionType == SectionType.CASE_INFORMATION) {
            String s = strings[0];
            String ja = "Judge Assigned: ";
            String id = "Issue Date: ";

            String judge = s.substring(ja.length(), s.indexOf(id)).trim();
            data.setIssueDate(s.substring(s.indexOf(id) + id.length()).trim());

            //judge name can bleed into next line
            int next = 1;
            if (strings[1].indexOf("OTN") < 0) {
                judge += " " + strings[1].trim();
                next = 2;
            }

            judge = judge.replace("Magisterial District Judge ", "");
            judge = judge.replace("The Honorable ", "");
            data.setJudgeAssigned(judge);

            s = strings[next++];
            String a = "OTN: ";
            String a2 = "OTN/LOTN: ";
            int i = s.contains(a2) ? a2.length() : a.length();
            String b = "File Date: ";

            data.setOTNs(s.substring(i, s.indexOf(b)).trim());
            data.setFileDate(s.substring(s.indexOf(b) + b.length()).trim());

            s = strings[next++];
            a = "Arresting Agency:";
            b = "Arrest Date:";

            data.setArrestingAgency(s.substring(a.length(), s.indexOf(b)).trim());
            data.setArrestDate(s.substring(s.indexOf(b) + b.length()).trim());

            s = strings[next++];
            a = "Complaint No.:";
            b = "Incident No.:";

            data.setComplaintNumber(s.substring(a.length(), s.indexOf(b)).trim());
            data.setIncidentNumber(s.substring(s.indexOf(b) + b.length()).trim());

            s = strings[next++];
            a = "Disposition:";
            b = "Disposition Date:";

            data.setDisposition(s.substring(a.length(), s.indexOf(b)).trim());
            data.setDispositionDate(s.substring(s.indexOf(b) + b.length()).trim());

            s = strings[next++];
            a = "County:";
            b = "Township:";

            data.setCounty(s.substring(a.length(), s.indexOf(b)).trim());
            data.setTownship(s.substring(s.indexOf(b) + b.length()).trim());

            s = strings[next++];
            a = "Case Status:";

            data.setCaseStatus(s.substring(a.length()).trim());
        }
        else if (sectionType == SectionType.STATUS_INFORMATION) {
//            for (String s: strings) {
//                System.out.println(s);
//            }
        }
        // 3 18 § 5505 S Public Drunkenness And Similar Misconduct 02/13/2022
        // 4 18 § 903 F2  Conspiracy - Robbery-Inflict Threat Imm Bod Inj 03/19/2022 Held for Court
        // 1 18 § 3921 §§ A Theft By...
        // 2 18 § 2709 §§ A1 S Harassment - Subject Other to Physical Contact 02/13/2022
        // 1 18 § 2702 §§ A3 F2...
        // 1 75 § 3802 §§ D2* M ...
        // 2 75 § 3802 §§ D1i* M
        // 3 35 § 780-113 §§ A16 M
        // 1 18 § 2701 §§ A1 M2
        // 2 18 § 5503 §§ A4 M3

        // 1 35 § 780-113 §§ A30 F Manufacture, Delivery,
        // 2 18 § 903 F Conspiracy - Manufacture,
//        Murder
//        Felony (1st degree) (F1)
//                Felony (2nd degree) (F2)
//                Felony (3rd degree) (F3)
//                Ungraded Felony (F3)
//        Misdemeanor (1st degree)(M1)
//                Misdemeanor (2nd degree)(M2)
//                Misdemeanor (3rd degree)(M3)
//                Ungraded Misdemeanor (Same as M3)
//        Summary Offenses
        else if (sectionType == SectionType.CHARGES) {
            for (String s: strings) {
                s = s.trim();
                //if (s.indexOf(SECTION_SIGN) > -1 && s.indexOf(SECTION_SIGN+SECTION_SIGN) < 0) System.out.println(s);
                if (s.indexOf(SECTION_SIGN) > -1) {
                    String [] split = s.split(" ");
                    int proposedGradeI;
                    CRPdfData.GRADE grade;
                    if (s.indexOf(SECTION_SIGN+SECTION_SIGN) > -1) {
                        // 1 18 § 2702 §§ A3 F2
                        // would be broken by
                        // 1 18 § 3921 §§ A Theft By...
                        proposedGradeI = 6;
                    }
                    else {
                        // 3 18 § 5505 S
                        // would be broken by
                        // 3 18 § 5505 Public Drunkenness
                        proposedGradeI = 4;
                    }
                    try {
                        grade = CRPdfData.GRADE.valueOf(split[proposedGradeI]);
                    }

                    catch (IllegalArgumentException iae) {
//                        HashSet allowedPre = new HashSet();
//                        allowedPre.add("A");
//                        allowedPre.add("A1");
//                        allowedPre.add("A1.1-34");
//                        allowedPre.add("A1i");
//                        allowedPre.add("A1*");
//                        allowedPre.add("A2");
//                        allowedPre.add("A2*");
//                        allowedPre.add("A2-43");
//                        allowedPre.add("A3");
//                        allowedPre.add("A31I");
//                        allowedPre.add("A3-17");
//                        allowedPre.add("A3-20");
//                        allowedPre.add("A3-21");
//                        allowedPre.add("A3-23");
//                        allowedPre.add("A3-27");
//                        allowedPre.add("A3-31");
//                        allowedPre.add("A4");
//                        allowedPre.add("A7");
//                        allowedPre.add("A16");
//                        allowedPre.add("A30");
//                        allowedPre.add("A32");
//                        allowedPre.add("B");
//                        allowedPre.add("B1i");
//                        allowedPre.add("B1I");
//                        allowedPre.add("B.11I");
//                        allowedPre.add("B*");
//                        allowedPre.add("B**");
//                        allowedPre.add("C*");
//                        allowedPre.add("C***");
//                        allowedPre.add("D1i*");
//                        allowedPre.add("D1ii*");
//                        allowedPre.add("D1iii*");
//                        allowedPre.add("D2");
//                        allowedPre.add("D2*");
//                        allowedPre.add("D2***");
//
//                        String pre = split[proposedGradeI - 1];
//                        if (!allowedPre.contains(pre)) {
                            //Ugh, two cases of not space delimited out of 2885 2022 pdfs in Lancaster as of 5/22/22
                            //1 75§3362§§A1-026 S EXCEED 35
                        if (split[1].contains(SECTION_SIGN+SECTION_SIGN)) {
                            try {
                                grade = CRPdfData.GRADE.valueOf(split[2]);
                            }
                            catch (Exception e) {
                                grade = CRPdfData.GRADE.NOT_SPECIFIED;
                            }
                        }
                        else {
                            grade = CRPdfData.GRADE.NOT_SPECIFIED;
                        }
                    }
                    data.addGrade(grade);
                }
            }
        }
        else if (sectionType == SectionType.BAIL) {
            data.setHasBailSection(true);
            boolean foundSet = false;
            for (String s: strings) {
                //if (foundSet) System.out.println("beyond bail Set: " + s);
                if (!foundSet && s.indexOf("Set") == 0) {
                    foundSet = true;

                    String[] split = s.split(" ");
                    String bailType = split[2];
                    data.setBailType(bailType);

                    for (String temp: split) {
                        if (temp.indexOf('%') > -1) {
                            data.setBailPercent(temp);
                            break;
                        }
                    }

                    int d = s.indexOf('$');
                    String bailWithCommas = s.substring(d + 1, s.lastIndexOf('.')).trim();
                    String bailWithoutCommas = bailWithCommas.replaceAll(",", "");
                    data.setBail(Integer.parseInt(bailWithoutCommas));
                }
                if (s.indexOf("orfeiture") > -1) {
                    data.setForfeiture(true);
                }
            }
        }
        else if (sectionType == SectionType.DEFENDANT_INFORMATION) {
//            for (String s: strings) {
//                System.out.println(s);
//            }
            String s = strings[0];
            String name = s.substring("Name: ".length(), s.indexOf(" Sex:"));
            data.setDefendantName(name);

            s = strings[1].toLowerCase();
            String dob = s.substring("date of birth:".length(), s.indexOf(" race:"));
            dob = dob.trim();
            //some mj dockets don't show dob
            if (dob.length() > 0) {
                data.setBirthdate(LocalDate.parse(dob, PdfData.slashDateFormatter));
            }
        }
        else if (sectionType == SectionType.CONFINEMENT) {
            String unable = "unable to post bail";
            HashSet<String> skippers = new HashSet<>();
            skippers.add("confinement location confinement type confinement reason confinement confinement ");
            skippers.add("date end date");
            skippers.add("case confinement");
            boolean foundRecent = false;
            for (String s: strings) {
                s = s.toLowerCase();
                if (s.indexOf("recent entries") > -1) {
                    foundRecent = true;
                }
                if (s.indexOf(unable) > -1) {
                    String remains = s.substring(s.indexOf(unable) + unable.length()).trim();
                    String startConfinement = remains;
                    String endConfinement = null;
                    if (remains.indexOf(' ') > 0) {
                        startConfinement = remains.substring(0, remains.indexOf(' '));
                        endConfinement = remains.substring(remains.indexOf(' '));
                    }
                    data.setUnableToPostBail(true);
                    data.setStartConfinement(startConfinement);
                    data.setEndConfinement(endConfinement);
                }
//                else if (!foundRecent && !skippers.contains(s) && s.indexOf("mdjs") < 0){
//                    System.out.println("unprocessed confinement line: " + s);
//                }
            }
        }
    }

    private static class Section {
        private CRParser.SectionType sectionType;
        private String[] strings;
        private String judgeHeader;

        public void setStrings(String[] strings) {
            this.strings = strings;
        }

        public void setSectionType(CRParser.SectionType sectionType) {
            this.sectionType = sectionType;
        }

        public void setJudgeHeader(String judgeHeader) {
            this.judgeHeader = judgeHeader;
        }

        public String toString() {
            return sectionType.toString();
        }

        public CRParser.SectionType getSectionType() {
            return sectionType;
        }

        public String[] getStrings() {
            return strings;
        }

        public String getJudgeHeader() {
            return judgeHeader;
        }
    }

    private static TreeMap<Integer, CRParser.SectionType> buildSections(String[] strings) {
        TreeMap<Integer, CRParser.SectionType> ret = new TreeMap<>();

        for (int x = 0; x < strings.length; x++) {
            String string = strings[x];
            CRParser.SectionType sectionType = stringToSectionType.get(string);
            if (sectionType != null) {
                ret.put(x, sectionType);
            }
//            else if (string.toUpperCase().equals(string)) {
//                if (string.indexOf("$") < 0 && string.indexOf("OTN") < 0) {
//                    System.err.println("Missing section type: " + string);
//                }
//            }
        }

        return ret;
    }
}
