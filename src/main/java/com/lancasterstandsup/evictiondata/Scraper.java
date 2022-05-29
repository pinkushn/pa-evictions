package com.lancasterstandsup.evictiondata;

import com.itextpdf.text.exceptions.InvalidPdfException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * There are five Lancaster pdfs that are malformed (something wrong with file header for pdf)
 *
 2303_0000057_2017.pdf
 2303_0000067_2017.pdf
 2303_0000006_2017.pdf

 2206_0000182_2017.pdf

 2303_0000028_2018.pdf
 */

public class Scraper {
    private static int BETWEEN_SCRAPE_PAUSE = 200;
    private static int BETWEEN_RESPONSE_PAUSE = 150;

    public enum CourtMode {
        //LandlordTenantMDJ
        MDJ_LT("MJ", "LT", 4,
                "MJ-05227-LT-0000094-2021".length(),
                "https://ujsportal.pacourts.us/Report/MdjDocketSheet?docketNumber=".length()),
        //once I hit a string of six missing when pulling CRs
        //might even be worth counting missing and noting on any summary reports
        //(I'm guessing they are due to 'clean slate' types of record hiding)
        /**
         * No such docket #: MJ-02103-CR-0000473-2019
         * No such docket #: MJ-02103-CR-0000474-2019
         * No such docket #: MJ-02103-CR-0000475-2019
         * No such docket #: MJ-02103-CR-0000476-2019
         * No such docket #: MJ-02103-CR-0000477-2019
         * No such docket #: MJ-02103-CR-0000478-2019
         */
        //CriminalMDJ
        MDJ_CR("MJ", "CR", 15,
                "MJ-05227-CR-0000094-2021".length(),
                "https://ujsportal.pacourts.us/Report/MdjDocketSheet?docketNumber=".length()),
        //Criminal Common Pleas
        //Hit a consecutive string of 9 'no such dockets' in 2017!
        CP_CR("CP", "CR", 15,
                "CP-36-CR-0000094-2021".length(),
                "https://ujsportal.pacourts.us/Report/CpDocketSheet?docketNumber=".length());

        String courtLevel;
        String caseType;
        int missesBeforeGivingUp;
        int docketCharLen;
        int prependURLLength;

        CourtMode(String level, String type, int i, int docketCharLen, int prependURLLength) {
            this.courtLevel = level;
            caseType = type;
            missesBeforeGivingUp = i;
            this.docketCharLen = docketCharLen;
            this.prependURLLength = prependURLLength;
        }

        public String getCaseType() {
            return caseType;
        }

        /**
         * @return TWO letters (MJ or CP)
         */
        public String getCourtLevel() {
            return courtLevel;
        }

        public String getFolderName() {
            return courtLevel + "_" + caseType;
        }

        public int getMissesBeforeGivingUp() {
            return missesBeforeGivingUp;
        }

        public String getDocket(String courtOffice, String sequenceNumber, String year) {
            return courtLevel + "-" + courtOffice + "-" + caseType + "-" + sequenceNumber + "-" + year;
        }

        public String getPdfCachePath() {
            return PDF_CACHE_PATH + getFolderName() + "/";
        }

        public int getDocketCharLen() {
            return docketCharLen;
        }

        public int getPrependURLLength() {
            return prependURLLength;
        }
    }

    public final static String LOCAL_DATA_PATH = "/Users/josh/git/PAEvictionsLocalData/";
    public final static String PDF_CACHE_PATH = LOCAL_DATA_PATH + "pdfCache/";
    private final static String site = "https://ujsportal.pacourts.us/CaseSearch";
    private final static String RESOURCES_PATH = "./src/main/resources/";
    private static HashMap<CourtMode, File> pointerFiles = new HashMap<>();
    private final static String COMPLETION_FILE_NAME = "completion";

    private static int hits = 0;
    private static int urlHits = 0;
    private static int urlLoops = 0;
    //wait after a failed call
    //note to self: got this to manually work after 1.1 hours 1/22/22
    //about
    //had it work at about .56 on manual test 1/23/22
    private static final double HOURS_WAIT = .6;
    private final static long RESET_PERMISSIONS_TIME = (long) (1000 * 60 * 60 * HOURS_WAIT);

    //stop and wait for 'a while' (HOURS_WAIT, above) after this many url hits
    private final static int URL_HITS_PERMITTED = 400;
    private static boolean firstOTNExistsWarning = true;

    private static LocalDateTime lastCheck = null;

    private static HashMap<CourtMode, HashMap<String, List<String>>> courtLevelCountyCourtOffices = new HashMap<>();

    //mode -> county -> year -> ? -> url (is ? docket?)
    private static HashMap<CourtMode, HashMap<String, HashMap<String, Map<String, String>>>> storedURLs =
            new HashMap<>();

    //*** LIMITATION: no spaces in network ssid or pwd
    private static ArrayList<String> networkChoices;

    static {
        networkChoices = new ArrayList<>();
        File networks = new File(RESOURCES_PATH + "networks");

//        if (networks.exists()) {
//            try {
//                try (BufferedReader in = new BufferedReader(new FileReader(networks))) {
//                    String next = null;
//                    while ((next = in.readLine()) != null) {
//                        networkChoices.add(next);
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
        if (networkChoices.isEmpty()) {
            networkChoices.add("fake fake");
        }

        for (CourtMode courtMode : CourtMode.values()) {
            pointerFiles.put(courtMode, new File(RESOURCES_PATH + courtMode.getFolderName() + "pointer"));
            storedURLs.put(courtMode, new HashMap<>());
        }
    }

    public static void scrapeOTNs(String county, String [] years, boolean ignoreLocalCache) throws IOException, ClassNotFoundException, InterruptedException {
        List<CRPdfData> list = ParseAll.get(Scraper.CourtMode.MDJ_CR, county, years);

        Set<String> otns = new HashSet<>();
        for (CRPdfData pdf: list) {
            if (pdf.hasOTNs()) otns.addAll(pdf.getOTNs());
            else {
                System.err.println("No OTN for " + pdf.getDocket());
            }
        }
        List<String> sList = new ArrayList<>();
        sList.addAll(otns);

        Scraper.scrapeOTNListForDocketNames(sList, ignoreLocalCache);
    }

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
        //CourtMode courtMode = CourtMode.MDJ_LT;
        CourtMode courtMode = CourtMode.CP_CR;
//        CourtMode courtMode = CourtMode.MDJ_CR;
        commenceScrapingFromSavedPointer(courtMode);

        //String[] years = {"2021"};
        //scrapeOTNs("Lancaster", years, true);

        // commenceScrapingFromArtificalPointer();
        //getOTNDocketNames("U 684533-3");

//        getPersonDocketNames("Lewis", "Tiekey",
//                PdfData.convertSlashDateToDashDate("07/24/1972"));

//        pingLancasterPrison(
//                "Lewis",
//                "Tiekey",
//                LocalDate.parse("07/24/1972", PdfData.slashDateFormatter));

//        pingLancasterPrison(
//                "Lewis",
//                "Tiekey",
//                null);
    }

//    private static void commenceScrapingFromArtificalPointer() throws IOException, InterruptedException, ClassNotFoundException {
//
//        //force the county AFTER the one shown
//        Pointer pointer = new Pointer();
//        pointer.setYear(getCurrentYear());
//        //pointer.setCounty("Lackawanna");
//        pointer.setCounty("Lawrence ");
//        advancePointer(pointer, true);
//
//        commenceScraping(pointer);
//    }

    private static void commenceScrapingFromSavedPointer(CourtMode courtMode) throws IOException, ClassNotFoundException, InterruptedException {
        //we use this pointer to trigger starting from the beginning if there is no pointer file
        File pointerFile = pointerFiles.get(courtMode);
        if (!pointerFile.exists()) {
            Pointer pointer = new Pointer();
            pointer.setYear(getCurrentYear());
            pointer.setCounty(Website.counties.get(Website.counties.size() - 1));
            advancePointer(pointer, courtMode, true);
        }

        commenceScraping(readPointer(courtMode), courtMode);
    }

    /**
     * Assumes inbound pointer is already saved to pointer file
     * @param pointer
     */
    public static void commenceScraping(Pointer pointer, CourtMode courtMode) throws InterruptedException, IOException, ClassNotFoundException {
        System.out.println("**********************");
        System.out.println("Starting with " + pointer);
        System.out.println("**********************");

        setLastCheck(pointer.getCounty(), courtMode);

        List<String> courtOffices = getCourtOffices(pointer.getCounty(), courtMode);
        int index = courtOffices.indexOf(pointer.getCourtOffice());
        System.out.println("court office is " + (index + 1) + " of " + courtOffices.size());

        boolean forever = true;

        int misses = 0;
        int missesBeforeGivingUp = courtMode.getMissesBeforeGivingUp();
        long lastTime = 0;

        while (forever) {
            Exception exception = null;
            try {
                boolean scraped = scrape(pointer, courtMode, lastCheck);
                LocalDateTime time = LocalDateTime.now();
                if (!scraped) {
                    misses++;
                }
                else {
                    misses = 0;
                    hits++;
                    String hitWord = hits == 1 ? "hit" : "hits";
                    System.out.println(hits + " " + hitWord + " (" + urlHits + " from url)" +
                            //"   average url hit millis: " + getAverageUrlHitTime() +
                            "   last pointer: " + pointer +
                            "   urlLoop: " + urlLoops +
                            "   time: " + time);
                }

                //if we're getting slowed down, introduce a delay
                if (lastTime != 0) {
                    //millis tween scrapes
                    long diff = (System.currentTimeMillis() - lastTime);
                    System.out.println("diff: " + diff);
                    if (diff > 15000) {
                        int pauseSeconds = 60;
                        System.err.println("Abnormally long scrape of " + diff + " millis. Pause for " +
                                pauseSeconds + " seconds.");
                        Thread.sleep(pauseSeconds * 1000);
                        //millisForAllURLHits -= pauseSeconds * 1000;
                    }
                }
                boolean nextCourtOffice = misses >= missesBeforeGivingUp;
                if (nextCourtOffice) {
                    misses = 0;
                }
                lastTime = System.currentTimeMillis();
                advancePointer(pointer, courtMode, nextCourtOffice);
            }
            catch (Exception e) {
                exception = e;
            }

            if (exception != null || urlLoops >= URL_HITS_PERMITTED) {
                urlLoops = 0;
                lastTime = 0;
                LocalDateTime now = LocalDateTime.now();
                if (exception != null) {
                    System.err.println("Scrape fail on at pointer: " + pointer + " at " + now);
                    System.err.println(exception);
                }
                else {
                    System.out.println("Scrape hit max url hits permitted " +
                            "(" + URL_HITS_PERMITTED + ") with pointer: " + pointer + " at " + now);
                }

                if (haveNetworkChoice()) {
                    switchNetworks();
                }
                else {
                    System.err.println("\nSleeping for " + HOURS_WAIT + " hours, restart at " +
                            now.plus(RESET_PERMISSIONS_TIME, ChronoUnit.MILLIS) + "\n");

                    Thread.sleep(RESET_PERMISSIONS_TIME);
                    //millisForAllURLHits -= RESET_PERMISSIONS_TIME;

                    System.err.println("RESTARTING at " + LocalDateTime.now());
                }
            }
        }
    }

    public static List<String> scrapeOTNListForDocketNames(List<String> otns, boolean ignoreLocalCache) throws InterruptedException {
        System.out.println("**********************");
        System.out.println("Pursuing docket names for " + otns.size() + " OTN(s)");
        System.out.println("**********************");

        if (otns == null || otns.isEmpty()) throw new IllegalStateException("null or empty otns");

        List<String> ret = new ArrayList<>();
        boolean done = false;
        int next = 0;
        while (!done) {
            Exception exception = null;
            try {
                ret.addAll(getOTNDocketNames(otns.get(next), false, ignoreLocalCache));
                System.out.println((next + 1) + "/" + otns.size() + ": " +
                        "Read otn docket name(s) for OTN " + otns.get(next));
                next++;
                if (next >= otns.size()) {
                    done = true;
                }
            }
            catch (Exception e) {
                exception = e;
            }

            if (exception != null) {
                LocalDateTime now = LocalDateTime.now();
                if (exception != null) {
                    System.err.println("Scrape fail on at otn: " + otns.get(next) + " at " + now);
                    System.err.println(exception);
                }
                else {
                    System.out.println("Scrape hit max url hits permitted " +
                            "(" + URL_HITS_PERMITTED + ") with otn: " + otns.get(next) + " at " + now);
                }

                System.err.println("\nSleeping for " + HOURS_WAIT + " hours, restart at " +
                        now.plus(RESET_PERMISSIONS_TIME, ChronoUnit.MILLIS) + "\n");

                Thread.sleep(RESET_PERMISSIONS_TIME);

                System.err.println("RESTARTING at " + LocalDateTime.now());
            }
        }

        return ret;
    }

    public static void scrapeDockets(List<String> dockets) throws InterruptedException {
        System.out.println("**********************");
        System.out.println("Scraping " + dockets.size() + "target dockets");
        System.out.println("**********************");
        System.err.println("Warn: only supported for Lancaster County, no rescraping");
        System.err.println("Warn: only supported for MJ_CR and CP_CR (not MJ_LT)");
        lastCheck = LocalDateTime.now();

        if (dockets == null || dockets.isEmpty()) throw new IllegalStateException("null or empty dockets");

        boolean done = false;
        int next = 0;
        while (!done) {
            Exception exception = null;
            try {
                String docket = dockets.get(next);
                Pointer pointer = Pointer.fromDocket(docket, "Lancaster");
                CourtMode mode =  docket.indexOf("CP") > -1 ?
                        CourtMode.CP_CR :
                        CourtMode.MDJ_CR;
                scrape(pointer, mode, lastCheck);
                //System.out.println("Read " + dockets.get(next));
                next++;
                if (next >= dockets.size()) {
                    done = true;
                };
            }
            catch (Exception e) {
                exception = e;
            }

            if (exception != null || urlLoops >= URL_HITS_PERMITTED) {
                urlLoops = 0;
                LocalDateTime now = LocalDateTime.now();
                if (exception != null) {
                    System.err.println("Scrape fail on docket: " + dockets.get(next) + " at " + now);
                    System.err.println(exception);
                }
                else {
                    System.out.println("Scrape hit max url hits permitted " +
                            "(" + URL_HITS_PERMITTED + ") with docket: " + dockets.get(next) + " at " + now);
                }

                System.err.println("\nSleeping for " + HOURS_WAIT + " hours, restart at " +
                        now.plus(RESET_PERMISSIONS_TIME, ChronoUnit.MILLIS) + "\n");

                Thread.sleep(RESET_PERMISSIONS_TIME);
                //millisForAllURLHits -= RESET_PERMISSIONS_TIME;

                System.err.println("RESTARTING at " + LocalDateTime.now());
            }
        }
    }

    private static boolean haveNetworkChoice() {
        return networkChoices.size() > 1;
    }

    private static void switchNetworks() throws IOException, InterruptedException {
        String next = networkChoices.remove(0);
        networkChoices.add(next);
        System.err.println("Switching network to " + next.substring(0, next.indexOf(' ')));
        //ONLY WORKS ON MAC!!!
        //todo if team grows: support pc
        Runtime.getRuntime().exec("networksetup -setairportnetwork en0 " + next);
        Thread.sleep(15000);
    }

//    private static String getAverageUrlHitTime() {
//        if (urlHits == 0) return "N/A";
//        return "" + (millisForAllURLHits/urlHits);
//    }

    /**
     * We'll stay on a single courtOffice as we advance through all the years
     *
     * Pro tip: do you want the 'first' pointer?
     * Send int a pointer instance with year at current year
     * and county at last county
     * and nextCourtOffice set to true
     *
     * @param pointer
     * @param nextCourtOffice
     * @return
     */
    private static void advancePointer (Pointer pointer, CourtMode courtMode, boolean nextCourtOffice) throws IOException, ClassNotFoundException {
        boolean commencingScrape = !pointer.hasCourtOffice();
        boolean nextCounty = commencingScrape;

        if (nextCourtOffice) {
            pointer.setSequenceNumberUnformatted(1);

            int year = pointer.getYear();
            //may just increment year if not in current year
            if (year != getCurrentYear()) {
                pointer.setYear(year + 1);
            }
            else if (!commencingScrape){
                pointer.setYear(getStartYear(courtMode));
                List<String> courtOffices = getCourtOffices(pointer.getCounty(), courtMode);
                int index = courtOffices.indexOf(pointer.getCourtOffice());
                if (index == courtOffices.size() - 1) {
                    nextCounty = true;
                }
                else {
                    String advancedCourtOffice = courtOffices.get(index + 1);
                    String prior = pointer.getCourtOffice();
                    pointer.setCourtOffice(advancedCourtOffice);

                    System.out.println("\n*** Bumped " + pointer.getCounty() + " court office from " +
                            prior + " to " + advancedCourtOffice +
                            " (" + (index + 2) + " of " + (courtOffices.size()) + ") ***\n");
                }
            }

            if (nextCounty) {
                //first, save 'done' range on old county
                if (!commencingScrape) saveCountyDone(pointer.getCounty(), courtMode);

                pointer.setYear(getStartYear(courtMode));
                int index = 1 + Website.counties.indexOf(pointer.getCounty());
                if (index == Website.counties.size()) {
                    index = 0;
                    if (!commencingScrape) {
                        System.out.println("\n\n****************************\n");
                        System.out.println("FINISHED ALL COUNTIES at " + LocalDateTime.now() +
                                ". Restarting at the beginning.");
                        System.out.println("\n\n****************************\n");
                        //System.exit(0);
                    }
                }
                String prior = pointer.getCounty();
                pointer.setCounty(Website.counties.get(index));

                System.out.println("*** Bumping county from " + prior +
                        " to " + pointer.getCounty() +
                        " (" + (index + 1) + " of " + (Website.counties.size()) + ")");

                pointer.setCourtOffice(getCourtOffices(pointer.getCounty(), courtMode).get(0));

                setLastCheck(pointer.getCounty(), courtMode);

                System.out.println("\n*** " + pointer.getCounty() + " has " +
                        getCourtOffices(pointer.getCounty(), courtMode).size() + " court offices");

//                if (!commencingScrape && courtMode == CourtMode.MDJ_LT) {
//                    //update github data with each new county
//                    Thread t1 = new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            try {
//                                Update.update(courtMode);
//                            } catch (IOException | ClassNotFoundException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    });
//                    t1.start();
//                }
            }
        }
        else {
            pointer.setSequenceNumberUnformatted(1 + pointer.getSequenceNumberUnformatted());
        }

        savePointer(pointer, courtMode);
    }

    private static void setLastCheck(String county, CourtMode courtMode) throws IOException, ClassNotFoundException {
        CountyCoveredRange ccr = getCountyStartAndEnd(county, courtMode);
        lastCheck = ccr == null ? null : ccr.getEnd();
    }


    /**
     * two time stamps describing date range of local (scraped) pdfs
     * 1) the time this county's pull was completed
     * 2) start year of reliable data
     *
     * #2 is a pull'sstart year UNLESS
     *     already exists and is earlier
     */
    private static void saveCountyDone(String county, CourtMode courtMode) throws IOException, ClassNotFoundException {
        CountyCoveredRange ccr = new CountyCoveredRange();
        ccr.setEnd(LocalDateTime.now());
        LocalDateTime startFromThisRun = LocalDateTime.of(getStartYear(courtMode), 1, 1, 0, 0);

        CountyCoveredRange prior = getCountyStartAndEnd(county, courtMode);
        LocalDateTime start = (prior != null && prior.getStart().compareTo(startFromThisRun) < 0) ?
                prior.getStart() : startFromThisRun;

        File file = new File(courtMode.getPdfCachePath() + county,COMPLETION_FILE_NAME);
        try (FileOutputStream fileOut = new FileOutputStream(file);
             ObjectOutputStream objectOut = new ObjectOutputStream(fileOut)) {

            ccr.setStart(start);
            objectOut.writeObject(ccr);
        }
    }

    public static CountyCoveredRange getCountyStartAndEnd(String county, CourtMode courtMode) throws IOException, ClassNotFoundException {
        File file = new File(courtMode.getPdfCachePath() + county, COMPLETION_FILE_NAME);
        if (!file.exists()) {
            return null;
        }
        try (FileInputStream fileIn = new FileInputStream(file);
             ObjectInputStream objectOut = new ObjectInputStream(fileIn)) {
            return (CountyCoveredRange) objectOut.readObject();
        }
    }

    private static List<String> getCourtOffices(String county, CourtMode courtMode) throws IOException {
        //ensure we have all the court offices for target county
        if (!courtLevelCountyCourtOffices.containsKey(courtMode)) {
            courtLevelCountyCourtOffices.put(courtMode, new HashMap<>());
        }
        HashMap<String, List<String>> countyCourtOffices = courtLevelCountyCourtOffices.get(courtMode);
        if (!countyCourtOffices.containsKey(county)) {
            if (courtMode.getCourtLevel().equals("MJ")) {
                countyCourtOffices.put(county, getCourtOfficesFromServer(county));
            }
            else {
                List<String> list = new ArrayList<>();
                if (county.equals("Lancaster")) {
                    list.add("36");
                    countyCourtOffices.put("Lancaster", list);
                }
                else {
                    throw new IllegalStateException("Not coded for Common Pleas county lookup except Lancaster");
                }
            }
        }
        return countyCourtOffices.get(county);
    }

    private static void savePointer(Pointer pointer, CourtMode courtMode) throws IOException {
            try (PrintWriter out = new PrintWriter(new FileOutputStream(pointerFiles.get(courtMode)));) {
                out.print(pointer.toString());
            }
    }

    private static Pointer readPointer(CourtMode courtMode) throws IOException, ClassNotFoundException {
        try (BufferedReader in = new BufferedReader(new FileReader(pointerFiles.get(courtMode)));) {
            return Pointer.fromSerializedPointerString(in.readLine());
        }
    }

    private static int  getStartYear(CourtMode courtMode) {
        if (courtMode == CourtMode.MDJ_CR) return 2022;
        else if (courtMode == CourtMode.CP_CR) return 2022;
        //return LocalDateTime.now().getYear() - 1;
        else return 2022;
    }

    static int getCurrentYear() {
        return LocalDateTime.now().getYear();
    }
//
//    private static File getPdfDir(Pointer pointer, Mode mode) {
//        return new File(PDF_CACHE_PATH_WITHOUT_CASE_TYPE +
//                mode.getFolderName() +
//                "/" + pointer.getCounty() +
//                "/" + pointer.getYear());
//    }

    public static boolean scrape(Pointer pointer, CourtMode courtMode, LocalDateTime lastCheck) throws IOException, InterruptedException, NoSuchFieldException {
        String county = pointer.getCounty();
        String courtOffice = pointer.getCourtOffice();
        String year = pointer.getYear() + "";
        String sequenceNumber = buildSequenceNumber(pointer.getSequenceNumberUnformatted());

        File dir = new File(courtMode.getPdfCachePath() + pointer.getCounty() + "/" + pointer.getYear());

        if (!dir.exists()) dir.mkdirs();

        String pathToFile = getPdfFilePath(pointer, courtMode);
        File file = new File(pathToFile);

        boolean foundOrRead = false;
        try {
            String docket = courtMode.getDocket(courtOffice, sequenceNumber, year);
            boolean scrape = true;
            boolean rescrape = false;

            if (file.exists()) {
                foundOrRead = true;
                scrape = false;
                PdfData oldData = null;
//                if (courtMode == CourtMode.MDJ_LT) {
//                    oldData = LTParser.getSingleton().processFile(file);
//                }
//                else {
//                    oldData = CRParser.processFile(file);
//                }
                oldData = ParseAll.getParser(courtMode).processFile(file);
                if (oldData.rescrape(lastCheck)) {
                    scrape = true;
                    rescrape = true;
                }
            }
            // Is this a known gap from 'clean slate' hiding records or expunging?
            else {
                //probe for subsequent file. If it exists, we've already identified this gap
                Pointer probePointer = pointer.clone();
                int probeSequence = Integer.parseInt(sequenceNumber);
                for (int x = 0; x < courtMode.getMissesBeforeGivingUp(); x++) {
                    probeSequence++;
                    probePointer.setSequenceNumberUnformatted(probeSequence);
                    String probeFilePath = getPdfFilePath(probePointer, courtMode);
                    File probeFile = new File(probeFilePath);
                    if (probeFile.exists()) {
                        System.out.println("Skipping missing docket (expunged/hidden): " + docket);
                        foundOrRead = true;
                        scrape = false;
                        break;
                    }
                }
            }

            if (scrape) {
                try {
                    Thread.sleep(BETWEEN_SCRAPE_PAUSE);
                } catch (InterruptedException e) {
                    throw e;
                }

                foundOrRead = getDocket(courtMode, county, year, docket, rescrape) || foundOrRead;
            }
        } catch (IllegalStateException ise) {
            throw ise;
        } catch (InvalidPdfException ipe) {
            System.err.println("Cannot process saved pdf for scraping: " + pointer);
            System.err.println("Next step: delete ^ locally and re-call " + pointer);
            deleteLocalPdf(pointer, courtMode);
            return scrape(pointer, courtMode, lastCheck);
        } catch (Exception e) {
            System.err.println("Cannot scrape " + pointer);
            e.printStackTrace();
            throw e;
        }

        return foundOrRead;
    }

    private static void deleteLocalPdf(Pointer pointer, CourtMode courtMode) {
        String pathToFile = getPdfFilePath(pointer, courtMode);
        File file = new File(pathToFile);
        file.delete();
    }

    public static boolean getDocket(
            CourtMode courtMode,
            String county,
            String year,
            String docket,
            boolean rescrape)
            throws IOException, InterruptedException {

        long startMillis = System.currentTimeMillis();

        String url = null;
        boolean storedURL = false;

        if (rescrape) {
            String savedURL = getStoredURL(courtMode, county, year, docket);
            if (savedURL != null) {
                url = savedURL;
                storedURL = true;
            }
        }

        CloseableHttpClient httpclient = HttpClients.createDefault();

        if (url == null) {
            HttpGet httpGet = new HttpGet(site);

            CloseableHttpResponse response = httpclient.execute(httpGet);

            int status = response.getStatusLine().getStatusCode();
            if (status != 200) {
                response.close();
                httpGet.releaseConnection();
                httpclient.close();
                throw new IllegalStateException("Failure in getDocket's first response, status = " + status);
            }

            Header[] headers = response.getAllHeaders();

            //******** Get Cookies from base page return **********

            String cookies = "";
            for (Header header : headers) {
                String val = header.getValue();
                //System.out.println("header... " + header.getName() + ", " + val);
                if (header.getName().equals("Set-Cookie")) {
                    if (!cookies.equals("")) {
                        cookies += "; ";
                    }
                    cookies += val;
                }
            }

            Set<String> badCookieChunks = new HashSet<>();
            badCookieChunks.add(" path=/");
            badCookieChunks.add(" samesite=strict");
            badCookieChunks.add(" httponly");
            badCookieChunks.add(" HttpOnly");
            badCookieChunks.add(" secure");

            String[] cookiePrint = cookies.split(";");
            List<String> finalCookies = new ArrayList<>();
            for (String s : cookiePrint) {
                if (!badCookieChunks.contains(s)) {
                    finalCookies.add(s.trim());
                }
            }

            cookies = "";
            for (String s : finalCookies) {
                if (!cookies.equals("")) {
                    cookies += "; ";
                }
                cookies += s;
            }


            //***** Extract RequestVerificationToken ****

            String requestValidationToken = null;

            try {
                HttpEntity entity = response.getEntity();
                InputStream inputStream = entity.getContent();

                String tokenFlag = "RequestVerificationToken";
                BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                while (in.ready()) {
                    String next = in.readLine();
                    boolean done = false;
                    while (!done) {
                        int i = next.indexOf(tokenFlag);
                        if (i < 0) {
                            done = true;
                        } else {
                            i = i + tokenFlag.length();
                            next = next.substring(i);
                            String valueOf = "value=\"";
                            int iv = next.indexOf(valueOf);

                            next = next.substring(iv + valueOf.length());
                            int qi = next.indexOf('"');
                            requestValidationToken = next.substring(0, qi);
                        }
                    }
                }
            } catch (IOException e) {
                response.close();
                httpGet.releaseConnection();
                httpclient.close();
                throw e;
            }


            try {
                Thread.sleep(BETWEEN_RESPONSE_PAUSE);
            } catch (InterruptedException e) {
                httpclient.close();
                throw e;
            }


            HttpPost httpPost = new HttpPost(site);

            httpPost.addHeader("Cookie", cookies);

            List<NameValuePair> nvps = new ArrayList<>();

            nvps.add(new BasicNameValuePair("SearchBy", "DocketNumber"));
            nvps.add(new BasicNameValuePair("DocketNumber", docket));
            nvps.add(new BasicNameValuePair("ParticipantSID=", ""));
            nvps.add(new BasicNameValuePair("ParticipantSSN", ""));
            nvps.add(new BasicNameValuePair("PADriversLicenseNumber", ""));
            nvps.add(new BasicNameValuePair("__RequestVerificationToken", requestValidationToken));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));

            CloseableHttpResponse response2 = httpclient.execute(httpPost);
            String dnh;

            try {
                HttpEntity entity2 = response2.getEntity();

                ByteArrayOutputStream os = new ByteArrayOutputStream();
                InputStream inputStream = entity2.getContent();
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

                String xml = new String(os.toByteArray());
                xml = xml.trim();
                inputStream.close();
                os.close();

                String dnhPrefix = "dnh=";
                int i = xml.indexOf(dnhPrefix);
                //there was no case/docket number
                if (i < 0) {
                    response2.close();
                    System.out.println("No such docket #: " + docket);
                    return false;
                }
                i += dnhPrefix.length();
                dnh = xml.substring(i);
                dnh = dnh.substring(0, dnh.indexOf('"'));

                EntityUtils.consume(entity2);
            } catch (Exception e) {
                e.printStackTrace();
                response2.close();
                httpPost.releaseConnection();
                httpclient.close();
                throw e;
            }


            try {
                Thread.sleep(BETWEEN_RESPONSE_PAUSE);
            } catch (InterruptedException e) {
                httpclient.close();
                throw e;
            }


//            url = "https://ujsportal.pacourts.us/Report/MdjDocketSheet?" +
//                    "docketNumber=" + docket + "&" +
//                    "dnh=" + dnh;
            url = "https://ujsportal.pacourts.us/Report/" +
                    (courtMode.getCourtLevel().equals("MJ") ?
                    "MdjDocketSheet" :
                    "CpDocketSheet") +
                    "?" +
                    "docketNumber=" + docket + "&" +
                    "dnh=" + dnh;

            storeURL(courtMode, county, year, url);
        }



        String rescrapeString = rescrape ? "rescrape, request URL: " : "new: ";
        if (storedURL) {
            rescrapeString = "rescrape, use STORED URL: ";
        }
        System.out.println(rescrapeString + url);

        HttpGet httpGet = new HttpGet(url);

        CloseableHttpResponse response3 = httpclient.execute(httpGet);

        if (response3.getStatusLine().getStatusCode() != 200) {
            response3.close();
            httpclient.close();
            throw new IllegalStateException("Pdf call failed with call status: " + response3.getStatusLine());
        }

        HttpEntity entity3;
        InputStream inputStream = null;
        try {
            entity3 = response3.getEntity();
            inputStream = entity3.getContent();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            byte[] bytes = os.toByteArray();
            os.close();

            String pathToFile = getPdfFilePath(courtMode, county, year, docket);
            FileOutputStream fos = new FileOutputStream(pathToFile);
            fos.write(bytes);
            fos.close();

            EntityUtils.consume(entity3);

            //long endMillis = System.currentTimeMillis();
            //long millis = endMillis - startMillis;
            //millisForAllURLHits += millis;
            urlHits++;
            urlLoops++;
            return true;
        } catch (Exception e) {
            System.err.println("Tripped up when writing pdf to file");
            e.printStackTrace();
            throw e;
        } finally {
            if (inputStream != null) inputStream.close();
            if (response3 != null) response3.close();
            httpGet.releaseConnection();
            httpclient.close();
        }
    }

    public static boolean isInLancasterCountyPrison(Person person) throws IOException, InterruptedException {
        return isInLancasterCountyPrison(
                person.getLast(),
                person.getFirst(),
                person.getBirthdate()
        );
    }

    public static boolean isInLancasterCountyPrison(
            String last,
            String first,
            LocalDate dob) throws IOException, InterruptedException {
        return pingLancasterPrison(last, first, dob) != null;
    }

    public static LocalDate getDateOfIncarcerationIfCurrentlyJailedInLancaster(Person p) throws IOException, InterruptedException {
        return pingLancasterPrison(p.getLast(), p.getFirst(), p.getBirthdate());
    }

    public static LocalDate pingLancasterPrison(
            String last,
            String first,
            LocalDate dob)
            throws IOException, InterruptedException {

        CloseableHttpClient httpclient = HttpClients.createDefault();

        String jailSite = "https://it.co.lancaster.pa.us/SPS/Public";

        HttpPost httpPost = new HttpPost(jailSite);

        List<NameValuePair> nvps = new ArrayList<>();

//        LastName: Lewis
//        FirstName: Tiekey
//        DateOfBirth: 7/24/1972    note: 07/24/1972 works
        nvps.add(new BasicNameValuePair("LastName", last));
        nvps.add(new BasicNameValuePair("FirstName", first));
        nvps.add(new BasicNameValuePair("DateOfBirth",
                (dob != null ? dob.format(PdfData.slashDateFormatter): "")));
        nvps.add(new BasicNameValuePair("PermanentIncarcerationNumber", ""));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));

        CloseableHttpResponse response2 = httpclient.execute(httpPost);

        try {
            HttpEntity entity2 = response2.getEntity();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            InputStream inputStream = entity2.getContent();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            String xml = new String(os.toByteArray()).trim();
            inputStream.close();
            os.close();
            EntityUtils.consume(entity2);

            try {
                Thread.sleep(BETWEEN_RESPONSE_PAUSE);
            } catch (InterruptedException e) {
                httpclient.close();
                throw e;
            }

            if (xml.indexOf("No Results") > -1) return null;

            int i = xml.lastIndexOf("<tr>");
            xml = xml.substring(i);
            i = xml.indexOf("<td>") + "<td>".length();
            xml = xml.substring(i);
            String[] split = xml.split("<td>");
            String date = split[5].trim();
            date = date.substring(0, date.indexOf(' '));

            //System.out.println("jail start: " + date);



            LocalDate ret = PdfData.forceSlashedDateIntoLocalDate(date);
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            response2.close();
            httpPost.releaseConnection();
            httpclient.close();
            throw e;
        }
    }

    public static boolean getPersonDocketNames(
            String last,
            String first,
            String dob)
            throws IOException, InterruptedException {

        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet(site);

        CloseableHttpResponse response = httpclient.execute(httpGet);

        int status = response.getStatusLine().getStatusCode();
        if (status != 200) {
            response.close();
            httpGet.releaseConnection();
            httpclient.close();
            throw new IllegalStateException("Failure in getOTN's first response, status = " + status);
        }

        Header[] headers = response.getAllHeaders();

        //******** Get Cookies from base page return **********

        String cookies = "";
        for (Header header : headers) {
            String val = header.getValue();
            if (header.getName().equals("Set-Cookie")) {
                if (!cookies.equals("")) {
                    cookies += "; ";
                }
                cookies += val;
            }
        }

        Set<String> badCookieChunks = new HashSet<>();
        badCookieChunks.add(" path=/");
        badCookieChunks.add(" samesite=strict");
        badCookieChunks.add(" httponly");
        badCookieChunks.add(" HttpOnly");
        badCookieChunks.add(" secure");

        String[] cookiePrint = cookies.split(";");
        List<String> finalCookies = new ArrayList<>();
        for (String s : cookiePrint) {
            if (!badCookieChunks.contains(s)) {
                finalCookies.add(s.trim());
            }
        }

        cookies = "";
        for (String s : finalCookies) {
            if (!cookies.equals("")) {
                cookies += "; ";
            }
            cookies += s;
        }


        //***** Extract RequestVerificationToken ****

        String requestValidationToken = null;

        try {
            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();

            String tokenFlag = "RequestVerificationToken";
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            while (in.ready()) {
                String next = in.readLine();
                boolean done = false;
                while (!done) {
                    int i = next.indexOf(tokenFlag);
                    if (i < 0) {
                        done = true;
                    } else {
                        i = i + tokenFlag.length();
                        next = next.substring(i);
                        String valueOf = "value=\"";
                        int iv = next.indexOf(valueOf);

                        next = next.substring(iv + valueOf.length());
                        int qi = next.indexOf('"');
                        requestValidationToken = next.substring(0, qi);
                    }
                }
            }
        } catch (IOException e) {
            response.close();
            httpGet.releaseConnection();
            httpclient.close();
            throw e;
        }


        try {
            Thread.sleep(BETWEEN_RESPONSE_PAUSE);
        } catch (InterruptedException e) {
            httpclient.close();
            throw e;
        }


        HttpPost httpPost = new HttpPost(site);

        httpPost.addHeader("Cookie", cookies);

        List<NameValuePair> nvps = new ArrayList<>();

        //SearchBy: ParticipantName
        //ParticipantLastName: Eckert
        //ParticipantFirstName: John  **** make sure to excise middle name ****
        //DocketType: Criminal
        //ParticipantDateOfBirth: 2022-05-12
        nvps.add(new BasicNameValuePair("SearchBy", "ParticipantName"));
        nvps.add(new BasicNameValuePair("ParticipantLastName", last));
        nvps.add(new BasicNameValuePair("ParticipantFirstName", first));
        nvps.add(new BasicNameValuePair("ParticipantDateOfBirth", dob));
        nvps.add(new BasicNameValuePair("DocketType", "Criminal"));
        nvps.add(new BasicNameValuePair("ParticipantSID=", ""));
        nvps.add(new BasicNameValuePair("ParticipantSSN", ""));
        nvps.add(new BasicNameValuePair("PADriversLicenseNumber", ""));
        nvps.add(new BasicNameValuePair("__RequestVerificationToken", requestValidationToken));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));

        CloseableHttpResponse response2 = httpclient.execute(httpPost);

        try {
            HttpEntity entity2 = response2.getEntity();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            InputStream inputStream = entity2.getContent();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            String xml = new String(os.toByteArray()).trim();
            inputStream.close();
            os.close();

            Set<String> otns = new TreeSet<>();
            int i = xml.indexOf("caseSearchResultGrid");
            xml = xml.substring(i);
            i = xml.indexOf("<tbody>") + "<tbody><tr>".length();
            int end = xml.indexOf("</tbody>");
            xml = xml.substring(i, end);
            String [] rows = xml.split("<tr>");
            for (String row: rows) {
                String[] cells = row.split("<td>");
                String otn = cells[10].substring(0, cells[10].indexOf('<'));
                otns.add(otn);
            }

            File dir = new File(PDF_CACHE_PATH + "People");
            if (!dir.exists()) dir.mkdir();

            File file = new File(PDF_CACHE_PATH + "People/" + getPersonFileName(last, first, dob));
            try (PrintWriter out = new PrintWriter(file);) {
                for (String otn: otns) {
                    out.println(otn);
                }
            }
            catch (Exception ugh) {
                ugh.printStackTrace();
            }

            EntityUtils.consume(entity2);
        } catch (Exception e) {
            e.printStackTrace();
            response2.close();
            httpPost.releaseConnection();
            httpclient.close();
            throw e;
        }

        return false;
    }

    private static String getPersonFileName(String last, String first, String dob) {
        return last + ", " + first + " " + dob;
    }

    public static List<String> getOTNDocketNames(String otn, boolean failIfNotLocal, boolean ignoreLocalCache)
            throws IOException, InterruptedException {

        List<String> ret = new ArrayList<>();

        File dir = new File(PDF_CACHE_PATH + "OTN");
        if (!dir.exists()) dir.mkdir();

        File file = new File(PDF_CACHE_PATH + "OTN/" + otn);

        if (file.exists() && !ignoreLocalCache) {
            if (firstOTNExistsWarning) {
                System.err.println("Warn: otn file already exists for " + otn + ", not re-reading. " +
                        "Risks missing docket created after earlier read. " +
                        "Someday, check for CP docket name in existing, re-read if non-existent.\n" +
                        "**** This msg will not be repeated for subsequent otns ****");
                firstOTNExistsWarning = false;
            }
            try (BufferedReader in = new BufferedReader(new FileReader(file));) {
                String next;
                while ((next = in.readLine()) != null) {
                    ret.add(next);
                }
            }
            catch (Exception eek) {
                eek.printStackTrace();
                throw eek;
            }
            return ret;
        }

        if (failIfNotLocal && !ignoreLocalCache) {
            throw new IllegalStateException("Cannot find local record of dockets for OTN " + otn);
        }

        System.out.println("Going to portal for OTN " + otn);

        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet(site);

        CloseableHttpResponse response = httpclient.execute(httpGet);

        int status = response.getStatusLine().getStatusCode();
        if (status != 200) {
            response.close();
            httpGet.releaseConnection();
            httpclient.close();
            throw new IllegalStateException("Failure in getOTN's first response, status = " + status);
        }

        Header[] headers = response.getAllHeaders();

        //******** Get Cookies from base page return **********

        String cookies = "";
        for (Header header : headers) {
            String val = header.getValue();
            if (header.getName().equals("Set-Cookie")) {
                if (!cookies.equals("")) {
                    cookies += "; ";
                }
                cookies += val;
            }
        }

        Set<String> badCookieChunks = new HashSet<>();
        badCookieChunks.add(" path=/");
        badCookieChunks.add(" samesite=strict");
        badCookieChunks.add(" httponly");
        badCookieChunks.add(" HttpOnly");
        badCookieChunks.add(" secure");

        String[] cookiePrint = cookies.split(";");
        List<String> finalCookies = new ArrayList<>();
        for (String s : cookiePrint) {
            if (!badCookieChunks.contains(s)) {
                finalCookies.add(s.trim());
            }
        }

        cookies = "";
        for (String s : finalCookies) {
            if (!cookies.equals("")) {
                cookies += "; ";
            }
            cookies += s;
        }


        //***** Extract RequestVerificationToken ****

        String requestValidationToken = null;

        try {
            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();

            String tokenFlag = "RequestVerificationToken";
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            while (in.ready()) {
                String next = in.readLine();
                boolean done = false;
                while (!done) {
                    int i = next.indexOf(tokenFlag);
                    if (i < 0) {
                        done = true;
                    } else {
                        i = i + tokenFlag.length();
                        next = next.substring(i);
                        String valueOf = "value=\"";
                        int iv = next.indexOf(valueOf);

                        next = next.substring(iv + valueOf.length());
                        int qi = next.indexOf('"');
                        requestValidationToken = next.substring(0, qi);
                    }
                }
            }
        } catch (IOException e) {
            response.close();
            httpGet.releaseConnection();
            httpclient.close();
            throw e;
        }


        try {
            Thread.sleep(BETWEEN_RESPONSE_PAUSE);
        } catch (InterruptedException e) {
            httpclient.close();
            throw e;
        }


        HttpPost httpPost = new HttpPost(site);

        httpPost.addHeader("Cookie", cookies);

        List<NameValuePair> nvps = new ArrayList<>();

        //SearchBy: OTN
        //OTN: X 344046-3
        nvps.add(new BasicNameValuePair("SearchBy", "OTN"));
        nvps.add(new BasicNameValuePair("OTN", otn));
        nvps.add(new BasicNameValuePair("ParticipantSID=", ""));
        nvps.add(new BasicNameValuePair("ParticipantSSN", ""));
        nvps.add(new BasicNameValuePair("PADriversLicenseNumber", ""));
        nvps.add(new BasicNameValuePair("__RequestVerificationToken", requestValidationToken));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));

        CloseableHttpResponse response2 = httpclient.execute(httpPost);

        try {
            HttpEntity entity2 = response2.getEntity();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            InputStream inputStream = entity2.getContent();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            String xml = new String(os.toByteArray()).trim();
            inputStream.close();
            os.close();

            List<String> cpDocketsPlusDNH = extractOTNURLs(xml, "/Report/CpDocketSheet?docketNumber=");
            List<String> mjDocketsPlusDNH = extractOTNURLs(xml, "/Report/MdjDocketSheet?docketNumber=");
            //**** WOAH, did you know you can get a full summary of mj and cp for anyone? Just use same url ending, but route by 'CourtSummary' as below
            //List<String> cpCourtSummaryPlusDNH = extractOTNURLs(xml, "/Report/CpCourtSummary?docketNumber=");
            //List<String> mjCourtSummaryPlusDNH = extractOTNURLs(xml, "/Report/MdjCourtSummary?docketNumber=");

            try (PrintWriter out = new PrintWriter(file);) {
                for (String s: cpDocketsPlusDNH) {
                    out.println(s);
                    ret.add(s);
                }
                for (String s: mjDocketsPlusDNH) {
                    out.println(s);
                    ret.add(s);
                }
            }
            catch (Exception ugh) {
                ugh.printStackTrace();
            }

            EntityUtils.consume(entity2);
        } catch (Exception e) {
            e.printStackTrace();
            response2.close();
            httpPost.releaseConnection();
            httpclient.close();
            throw e;
        }

        return ret;
    }

    private static List<String> extractOTNURLs(String html, String startOfURL) {
        String terminator = "&dnh=";

        List<String> ret = new ArrayList<>();

        String copy = html;
        int i = copy.indexOf(startOfURL);
        while (i > -1) {
            copy = copy.substring(i + startOfURL.length());
            i = copy.indexOf(terminator);
            ret.add(copy.substring(0, i));
            copy = copy.substring(i);
            i = copy.indexOf(startOfURL);
        }
        return ret;
    }

    static String preCountyFlag = "<option data-aopc-County=\"(";
    static String postCountyFlag = ")\" data-aopc-JudicialDistrict=\"(";
    /**
     *
     * @return List of court offices (judge codes)
     * @throws IOException
     */
    private static List<String> getCourtOfficesFromServer(String county) throws IOException {
        System.out.println("Scraping court offices for " + county);

        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet(site);

        CloseableHttpResponse response = httpclient.execute(httpGet);
        int status = response.getStatusLine().getStatusCode();
        if (status != 200) {
            System.err.println("getCourtOffices status: " + response.getStatusLine());
            httpGet.releaseConnection();
            httpclient.close();
            throw new IllegalStateException("start over plz");
        }

        List<String> ret = new ArrayList<>();

        try {
            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();

            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String findMe = preCountyFlag + county + postCountyFlag;
            while (in.ready()) {
                String next = in.readLine();
                boolean done = false;
                while (!done) {
                    int i = next.indexOf(findMe);
                    if (i < 0) {
                        done = true;
                    }
                    else {
                        i = i + findMe.length();
                        next = next.substring(i);
                        String valueOf = "value=\"MDJ-";
                        int iv = next.indexOf(valueOf);
                        if (iv < 0 || iv > 10) {
                            done = true;
                        }
                        else {
                            next = next.substring(iv + valueOf.length());
                            int qi = next.indexOf('"');
                            String snip = next.substring(0, qi);
                            ret.add(snip.replace("-", ""));
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            response.close();
            httpclient.close();
        }

        return ret;
    }

    private static String getPdfFilePath(Pointer pointer, CourtMode courtMode) {
       return getPdfFilePath(courtMode,
               pointer.getCounty(),
               pointer.getYear() + "",
               pointer.getCourtOffice(),
               buildSequenceNumber(pointer.getSequenceNumberUnformatted()));
    }

    private static String getPdfFilePath(CourtMode courtMode, String county, String year, String courtOffice, String sequence) {
        while (courtOffice.startsWith("0")) courtOffice = courtOffice.substring(1);
        return courtMode.getPdfCachePath() + county + "/" + year + "/" +
                courtOffice + "_" + sequence + "_" + year + ".pdf";
    }


    private static String getPdfFilePath(CourtMode courtMode, String county, String year, String docket) {
        String mdj = courtMode.getCourtLevel() + "-";
        String lt = courtMode.getCaseType() + "-";

        docket = excise(excise(docket, mdj), lt);
        while (docket.startsWith("0")) {
            docket = docket.substring(1);
        }
        docket = docket.replace('-', '_');
        return courtMode.getPdfCachePath() + county + "/" + year + "/" + docket + ".pdf";
    }

    private static String excise(String full, String target) {
        int i = full.indexOf(target);
        if (i == 0) return full.substring(target.length());
        String pre = full.substring(0, i);
        return pre + full.substring(i + target.length());
    }

    private static String buildSequenceNumber(int i) {
        StringBuffer sb = new StringBuffer(String.valueOf(i));
        while (sb.length() < 7) {
            sb.insert(0, 0);
        }
        return sb.toString();
    }
//
//    public static Pointer getPointerFromPdfFileName(String county, String pdfFileName) {
//        Pointer ret = new Pointer();
//        ret.setCounty(county);
//        int i = pdfFileName.indexOf('_');
//
//        String courtOffice = pdfFileName.substring(0, i);
//        while (courtOffice.length() < 5) {
//            courtOffice = "0" + courtOffice;
//        }
//        ret.setCourtOffice(courtOffice);
//        pdfFileName = pdfFileName.substring(i + 1);
//
//        i = pdfFileName.indexOf('_');
//        ret.setSequenceNumberUnformatted(Integer.parseInt(pdfFileName.substring(0, i)));
//        pdfFileName = pdfFileName.substring(i + 1);
//
//        i = pdfFileName.indexOf('.');
//        ret.setYear(Integer.parseInt(pdfFileName.substring(0, i)));
//
//        return ret;
//    }

    //county --> years
//    private static HashMap<String, Set<String>> probedURLFile = new HashMap<>();
    //mode -> county -> years
    private static HashMap<CourtMode, HashMap<String, Set<String>>> modesWithProbedURLFiles = new HashMap<>();
    private final static String URL_STORE_FILE_NAME = "url_store";
//    private final static String prepend = "https://ujsportal.pacourts.us/Report/MdjDocketSheet?docketNumber=";
//    private final static int PREPEND_URL_LENGTH = prepend.length();
//    private final static HashMap<Mode, Integer> docketCharLengths;
    /**
     *
     * @param fullDocket   ex: MJ-05227-LT-0000094-2021
     * @return url used in past to get this pdf OR null if doesn't exist
     */
    //HashMap<String, HashMap<String, Map<String, String>>>
    public static String getStoredURL(CourtMode courtMode, String county, String year, String fullDocket) throws IOException {
        String ret = getStoredURLS(courtMode, county, year).get(fullDocket);
        if (ret == null) return null;
        String prepend = "https://ujsportal.pacourts.us/Report/" +
                (courtMode.getCourtLevel().equals("MJ") ? "Mdj" : "Cp") +
                "DocketSheet?docketNumber=";
        return prepend + ret;
    }

    private static Map<String, String> getStoredURLS(CourtMode courtMode, String county, String year) throws IOException {
        if (!modesWithProbedURLFiles.containsKey(courtMode)) {
            modesWithProbedURLFiles.put(courtMode, new HashMap<>());
        }
        HashMap<String, Set<String>> probedURLFile = modesWithProbedURLFiles.get(courtMode);
        if (probedURLFile.containsKey(county) && probedURLFile.get(county).contains(year)) {
            return storedURLs.get(courtMode).get(county).get(year);
        }

        //county may pre-exist, just not for this year
        if (!storedURLs.get(courtMode).containsKey(county)) {
            storedURLs.get(courtMode).put(county, new HashMap<>());
        }

        //year can't pre-exist
        Map<String, String> urls = new HashMap<>();
        storedURLs.get(courtMode).get(county).put(year, urls);

        File file = new File(courtMode.getPdfCachePath() + county + "/" + year, URL_STORE_FILE_NAME);
        if (file.exists()) {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String next = null;
            while ((next = in.readLine()) != null) {
                String docket = docketFromFullURL(next, courtMode);
                if (docket != null) {
                    urls.put(docket, next);
                }
            }
        }

        if (!probedURLFile.containsKey(county)) {
            probedURLFile.put(county, new HashSet<>());
        }
        probedURLFile.get(county).add(year);

        return urls;
    }

    private static String docketFromFullURL(String fullURL, CourtMode courtMode) {
        int i = fullURL.indexOf(courtMode.getCourtLevel() + "-");
        if (i > -1) {
            return fullURL.substring(i, i + courtMode.getDocketCharLen());
        }
        return null;
    }

    /**
     *
     * @param county
     * @param year
     * @param fullURL ex: https://ujsportal.pacourts.us/Report/MdjDocketSheet?docketNumber=MJ-05227-LT-0000094-2021&dnh=PyIaNfEty5auetXme6GeJg%3D%3D
     */
    private static void storeURL(CourtMode courtMode, String county, String year, String fullURL) throws IOException {
        String shortenedURL = fullURL.substring(courtMode.getPrependURLLength());
        Map<String, String> urls = getStoredURLS(courtMode, county, year);
        urls.put(docketFromFullURL(shortenedURL, courtMode), shortenedURL);

        long start = System.currentTimeMillis();

        File file = new File(courtMode.getPdfCachePath() + county + "/" + year, URL_STORE_FILE_NAME);
        PrintWriter out = new PrintWriter(new FileWriter(file));
        for (String url: urls.values()) {
            out.println(url);
        }
        out.close();

        long end = System.currentTimeMillis();
        long total = end - start;
        if (total > 50) {
            System.out.println();
            System.out.println();
            System.out.println();
            System.out.println();
            System.err.println("******************");
            System.err.println("******************");
            System.err.println("******************");
            System.err.println("******************");
            System.err.println("******************");
            System.err.println("Writing to store url for " + county + " " + year +
                    " took " + total + " millis");
            System.err.println("******************");
            System.err.println("******************");
            System.err.println("******************");
            System.err.println("******************");
            System.err.println("******************");
            System.out.println();
            System.out.println();
            System.out.println();
            System.out.println();
        }
    }
}
