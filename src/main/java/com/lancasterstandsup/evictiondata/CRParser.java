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

public class CRParser {
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
    private static String docketNumberStart = "Docket Number: ";


    public static final String TARGET_YEAR_FOR_MAIN = "2022";
    public static final String TARGET_COUNTY_FOR_MAIN = "Lancaster";
    public static final String TARGET_COURT_FOR_MAIN = "2000";
    public static final String TARGET_SEQUENCE_FOR_MAIN = "0000001";
    public static void main (String[] args) {
        try {
            String pathToFile = Scraper.PDF_CACHE_PATH_WITHOUT_CASE_TYPE + "CR/" +
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

    public static CRPdfData processFile(File file) throws IOException, NoSuchFieldException {
        return processFile(file, false);
    }

    public static CRPdfData processFile(File file, boolean printAll) throws IOException, NoSuchFieldException {
        try {
            InputStream targetStream = new FileInputStream(file);
            CRPdfData data = process(targetStream, printAll);
            targetStream.close();
            return data;
        } catch (Exception e) {
            System.err.println("processFile failed on " + file.getName());
            throw e;
        }
    }

    public static CRPdfData process (InputStream pdfStream, boolean printAll) throws IOException, NoSuchFieldException {
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
            data.setDocketNumber(strings[0].substring(docketNumberStart.length()));
        }
        else if (sectionType == SectionType.CASE_INFORMATION) {
            String s = strings[0];
            String ja = "Judge Assigned: ";
            String id = "Issue Date: ";

            data.setJudgeAssigned(s.substring(ja.length(), s.indexOf(id)).trim());
            data.setIssueDate(s.substring(s.indexOf(id) + id.length()).trim());

            //judge name can bleed into next line
            int next = 1;
            if (strings[1].indexOf("OTN") < 0) {
                data.setJudgeAssigned(data.getJudgeAssigned() + " " + strings[1]);
                next = 2;
            }

            s = strings[next++];
            String a = "OTN: ";
            String b = "File Date: ";

            data.setOTN(s.substring(a.length(), s.indexOf(b)).trim());
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
//        else if (sectionType == SectionType.STATUS_INFORMATION) {
//            for (String s: strings) {
//                System.out.println(s);
//            }
//        }
        else if (sectionType == SectionType.BAIL) {
            for (String s: strings) {
                //System.out.println(s);
                if (s.indexOf("Set") == 0) {
                    int d = s.indexOf('$');
                    String bailWithCommas = s.substring(d + 1, s.lastIndexOf('.')).trim();
                    String bailWithoutCommas = bailWithCommas.replaceAll(",", "");
                    data.setBail(Integer.parseInt(bailWithoutCommas));
                }
            }
        }
        else if (sectionType == SectionType.DEFENDANT_INFORMATION) {
//            for (String s: strings) {
//                System.out.println(s);
//            }
            String s = strings[0];
            String name = s.substring("Name: ".length(), s.indexOf("Sex:"));
            data.setDefendantName(name);
        }
        else if (sectionType == SectionType.CONFINEMENT) {
            String unable = "unable to post bail";
            for (String s: strings) {
                s = s.toLowerCase();
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
