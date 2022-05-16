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

public class CPParser implements Parser {
    
        static enum SectionType {
            DOCKET("DOCKET", true),
            CASE_INFORMATION("CASE INFORMATION", true),
            STATUS_INFORMATION("STATUS INFORMATION", false),
            CALENDAR_EVENTS("CALENDAR EVENTS", true),
            CONFINEMENT_INFORMATION("CONFINEMENT INFORMATION", false),
            DEFENDANT_INFORMATION("DEFENDANT INFORMATION", false),
            CASE_PARTICIPANTS("CASE PARTICIPANTS", true),
            BAIL_INFORMATION("BAIL INFORMATION", false),
            CHARGES("CHARGES", false),
            DISPOSITION_SENTENCING_DETAILS("DISPOSITION / SENTENCING DETAILS", false),
            ATTORNEY_INFORMATION("COMMONWEALTH INFORMATION ATTORNEY INFORMATION", false),
            ENTRIES("ENTRIES", true);

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

        private static Map<String, CPParser.SectionType> stringToSectionType = new HashMap<>();

        private static HashSet<String> placeNames = new HashSet<>();

        static {
            for (CPParser.SectionType sectionType: CPParser.SectionType.values()) {
                stringToSectionType.put(sectionType.toString(), sectionType);
            }
        }

        private static String judgeHeaderStart = "Magisterial District Judge ";
        private static String docketStart = "Docket Number: ";

        public static void main (String [] args) throws IOException, NoSuchFieldException {
            String fileName = Scraper.PDF_CACHE_PATH + Scraper.CourtMode.CP_CR.getFolderName() +
                    "/Lancaster/2022/" +
                    "36_0000354_2022.pdf";
            File file = new File(fileName);
            getSingleton().processFile(file);
        }


        public static CPPdfData processFile(String fileName, boolean printAll) throws Exception {
            return processFile(new File(fileName), printAll);
        }

        public CPPdfData processFile(File file) throws IOException, NoSuchFieldException {
            return processFile(file, false);
        }

        public static CPPdfData processFile(File file, boolean printAll) throws IOException, NoSuchFieldException {
            try {
                InputStream targetStream = new FileInputStream(file);
                CPPdfData data = getSingleton().process(targetStream, printAll);
                targetStream.close();
                return data;
            } catch (Exception e) {
                System.err.println("processFile failed on " + file.getName());
                throw e;
            }
        }

        private static CPParser singleton = new CPParser();

        public static CPParser getSingleton() {
            return singleton;
        }

        public CPPdfData process (InputStream pdfStream, boolean printAll) throws IOException, NoSuchFieldException {
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
//
//                for (String s : stringsWithExtraDockets) {
//                    if (s.toLowerCase().indexOf("lancaster bail") > -1) {
//                        System.out.println("******** BAIL FUND *******");
//                    }
//                }

                //excise page headers ('DOCKET' sections) from page 2 on
                List<String> list = new ArrayList<>();
                boolean foundDocket = false;
                for (int x = 0; x < stringsWithExtraDockets.length; x++) {
                    if (stringsWithExtraDockets[x].equals(CPParser.SectionType.DOCKET.toString())) {
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

                TreeMap<Integer, CPParser.SectionType> sectionStarts = buildSections(strings);

                List<CPParser.Section> sections = new ArrayList<>();
                Integer lastStart = null;
                CPParser.SectionType lastSectionType = null;
                for (Integer i: sectionStarts.keySet()) {
                    CPParser.SectionType thisSectionType = sectionStarts.get(i);
                    if (lastStart != null) {
                        CPParser.Section section = new CPParser.Section();
                        section.setSectionType(lastSectionType);
                        if (lastSectionType == SectionType.DOCKET) {
                            section.setJudgeHeader(strings[lastStart-1]);
                        }
                        int minusCuzDocketJudge = thisSectionType == CPParser.SectionType.DOCKET ? 1 : 0;
                        section.setStrings(Arrays.copyOfRange(strings, lastStart + 1, i - minusCuzDocketJudge));
                        sections.add(section);
                    }
                    lastStart = i;
                    lastSectionType = thisSectionType;
                }
                CPParser.Section section = new CPParser.Section();
                section.setSectionType(lastSectionType);
                section.setStrings(Arrays.copyOfRange(strings, lastStart + 1, strings.length));
                if (lastSectionType == CPParser.SectionType.DOCKET) {
                    section.setJudgeHeader(strings[lastStart-1]);
                }
                sections.add(section);

                CPPdfData data = new CPPdfData();
                for (CPParser.Section s: sections) {
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

        private static void parseSection(CPParser.Section section, CPPdfData data) throws IOException {
            String[] strings = section.getStrings();
            CPParser.SectionType sectionType = section.getSectionType();

            if (sectionType == SectionType.DOCKET) {
                String judgeHeader = section.getJudgeHeader();
                //data.setCourtOffice("MDJ " + judgeHeader.substring(judgeHeaderStart.length()));
                data.setDocket(strings[0].substring(docketStart.length()));
            }
            else if (sectionType == CPParser.SectionType.CASE_INFORMATION) {
//                String s = strings[0];
//                String ja = "Judge Assigned: ";
//                String id = "Issue Date: ";
//
//                data.setJudgeAssigned(s.substring(ja.length(), s.indexOf(id)).trim());
//                data.setIssueDate(s.substring(s.indexOf(id) + id.length()).trim());
//
//                //judge name can bleed into next line
//                int next = 1;
//                if (strings[1].indexOf("OTN") < 0) {
//                    data.setJudgeAssigned(data.getJudgeAssigned() + " " + strings[1]);
//                    next = 2;
//                }
//
//                s = strings[next++];
//                String a = "OTN: ";
//                String b = "File Date: ";
//
//                data.setOTN(s.substring(a.length(), s.indexOf(b)).trim());
//                data.setFileDate(s.substring(s.indexOf(b) + b.length()).trim());
//
//                s = strings[next++];
//                a = "Arresting Agency:";
//                b = "Arrest Date:";
//
//                data.setArrestingAgency(s.substring(a.length(), s.indexOf(b)).trim());
//                data.setArrestDate(s.substring(s.indexOf(b) + b.length()).trim());
//
//                s = strings[next++];
//                a = "Complaint No.:";
//                b = "Incident No.:";
//
//                data.setComplaintNumber(s.substring(a.length(), s.indexOf(b)).trim());
//                data.setIncidentNumber(s.substring(s.indexOf(b) + b.length()).trim());
//
//                s = strings[next++];
//                a = "Disposition:";
//                b = "Disposition Date:";
//
//                data.setDisposition(s.substring(a.length(), s.indexOf(b)).trim());
//                data.setDispositionDate(s.substring(s.indexOf(b) + b.length()).trim());
//
//                s = strings[next++];
//                a = "County:";
//                b = "Township:";
//
//                data.setCounty(s.substring(a.length(), s.indexOf(b)).trim());
//                data.setTownship(s.substring(s.indexOf(b) + b.length()).trim());
//
//                s = strings[next++];
//                a = "Case Status:";
//
//                data.setCaseStatus(s.substring(a.length()).trim());
            }
            else if (sectionType == CPParser.SectionType.STATUS_INFORMATION) {
//            for (String s: strings) {
//                System.out.println(s);
//            }
            }
            else if (sectionType == SectionType.BAIL_INFORMATION) {
//                boolean foundSet = false;
//                for (String s: strings) {
//                    //if (foundSet) System.out.println("beyond bail Set: " + s);
//                    if (s.indexOf("Set") == 0) {
//                        foundSet = true;
//                        int d = s.indexOf('$');
//                        String bailWithCommas = s.substring(d + 1, s.lastIndexOf('.')).trim();
//                        String bailWithoutCommas = bailWithCommas.replaceAll(",", "");
//                        data.setBail(Integer.parseInt(bailWithoutCommas));
//                    }
//                    if (s.indexOf("orfeiture") > -1) {
//                        data.setForfeiture(true);
//                    }
//                }
            }
            else if (sectionType == CPParser.SectionType.DEFENDANT_INFORMATION) {
//            for (String s: strings) {
//                System.out.println(s);
//            }
//                String s = strings[0];
//                String name = s.substring("Name: ".length(), s.indexOf(" Sex:"));
//                data.setDefendantName(name);
//
//                s = strings[1].toLowerCase();
//                String dob = s.substring("date of birth:".length(), s.indexOf(" race:"));
//                dob = dob.trim();
//                //some mj dockets don't show dob
//                if (dob.length() > 0) {
//                    data.setBirthdate(LocalDate.parse(dob, PdfData.slashDateFormatter));
//                }
            }
            else if (sectionType == SectionType.CONFINEMENT_INFORMATION) {
//                String unable = "unable to post bail";
//                HashSet<String> skippers = new HashSet<>();
//                skippers.add("confinement location confinement type confinement reason confinement confinement ");
//                skippers.add("date end date");
//                skippers.add("case confinement");
//                boolean foundRecent = false;
//                for (String s: strings) {
//                    s = s.toLowerCase();
//                    if (s.indexOf("recent entries") > -1) {
//                        foundRecent = true;
//                    }
//                    if (s.indexOf(unable) > -1) {
//                        String remains = s.substring(s.indexOf(unable) + unable.length()).trim();
//                        String startConfinement = remains;
//                        String endConfinement = null;
//                        if (remains.indexOf(' ') > 0) {
//                            startConfinement = remains.substring(0, remains.indexOf(' '));
//                            endConfinement = remains.substring(remains.indexOf(' '));
//                        }
//                        data.setUnableToPostBail(true);
//                        data.setStartConfinement(startConfinement);
//                        data.setEndConfinement(endConfinement);
//                    }
////                else if (!foundRecent && !skippers.contains(s) && s.indexOf("mdjs") < 0){
////                    System.out.println("unprocessed confinement line: " + s);
////                }
//                }
            }
            else if (sectionType == SectionType.ENTRIES) {
                boolean lancasterBailFund = false;
                for (String s: strings) {
                    if (s.toLowerCase().indexOf("lancaster bail fund") > -1) {
                        lancasterBailFund = true;
                    }
                }
                data.setLancasterBailFund(lancasterBailFund);
            }
        }

        private static class Section {
            private CPParser.SectionType sectionType;
            private String[] strings;
            private String judgeHeader;

            public void setStrings(String[] strings) {
                this.strings = strings;
            }

            public void setSectionType(CPParser.SectionType sectionType) {
                this.sectionType = sectionType;
            }

            public void setJudgeHeader(String judgeHeader) {
                this.judgeHeader = judgeHeader;
            }

            public String toString() {
                return sectionType.toString();
            }

            public CPParser.SectionType getSectionType() {
                return sectionType;
            }

            public String[] getStrings() {
                return strings;
            }

            public String getJudgeHeader() {
                return judgeHeader;
            }
        }

        private static TreeMap<Integer, CPParser.SectionType> buildSections(String[] strings) {
            TreeMap<Integer, CPParser.SectionType> ret = new TreeMap<>();

            SectionType priorSectionType = null;
            for (int x = 0; x < strings.length; x++) {
                String string = strings[x];
                CPParser.SectionType sectionType = stringToSectionType.get(string);
                if (sectionType != null) {
                    if (sectionType != priorSectionType) {
                        ret.put(x, sectionType);
                    }
                    priorSectionType = sectionType;
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
