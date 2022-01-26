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
import java.time.temporal.TemporalField;
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

public class Scraper2 {
    private static int BETWEEN_SCRAPE_PAUSE = 200;
    private static int BETWEEN_RESPONSE_PAUSE = 150;

    //wait after a failed call
    //note to self: got this to manually work after 1.1 hours 1/22/22
    //about
    //had it work at about .56 on manual test 1/23/22
    private static final double HOURS_WAIT = .6;
    private final static long RESET_PERMISSIONS_TIME = (long) (1000 * 60 * 60 * HOURS_WAIT);

    private final static String PDF_CACHE_PATH = "./src/main/resources/pdfCache/";
    private final static String site = "https://ujsportal.pacourts.us/CaseSearch";
    private final static String POINTER_PATH = "./src/main/resources/";
    private final static String POINTER_FILE_NAME = "pointer";
    private static File pointerFile;
    private final static String COMPLETION_FILE_NAME = "completion";
    private static int hits = 0;
    private static int urlHits = 0;
    private static long millisForAllURLHits = 0;
    private static int urlLoops = 0;
    //stop and wait for a few hours after this many url hits
    private final static int URL_HITS_PERMITTED = 400;

    private static LocalDateTime lastCheck = null;

    private static HashMap<String, List<String>> countyCourtOffices = new HashMap<>();

//    public final static String[] countiesRaw = {
//            "Lancaster",
//            "York",
//            "Berks",
//            "Dauphin",
//            "Lebanon"
//    };

    static {
        pointerFile = new File(POINTER_PATH, POINTER_FILE_NAME);
    }

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
        commenceScrapingFromSavedPointer();
        //commenceScrapingFromArtificalPointer();
    }

    private static void commenceScrapingFromArtificalPointer() throws IOException, InterruptedException, ClassNotFoundException {

        //force the county AFTER the one shown
        Pointer pointer = new Pointer();
        pointer.setYear(getCurrentYear());
        pointer.setCounty("Lancaster");
        advancePointer(pointer, true);

        commenceScraping(pointer);
    }

    private static void commenceScrapingFromSavedPointer() throws IOException, ClassNotFoundException, InterruptedException {
        //we use this pointer to trigger starting from the beginning if there is no pointer file
        if (!pointerFile.exists()) {
            Pointer pointer = new Pointer();
            pointer.setYear(getCurrentYear());
            pointer.setCounty(Website.counties.get(Website.counties.size() - 1));
            advancePointer(pointer, true);
        }

        commenceScraping(readPointer());
    }

    /**
     * Assumes inbound pointer is already saved to pointer file
     * @param pointer
     */
    public static void commenceScraping(Pointer pointer) throws InterruptedException {
        System.out.println("**********************");
        System.out.println("Starting with " + pointer);
        System.out.println("**********************");

        boolean forever = true;

        int misses = 0;
        int missesBeforeGivingUp = 3;
        long lastTime = 0;

        while (forever) {
            Exception exception = null;
            try {
                boolean scraped = scrape(pointer, lastCheck);
                LocalDateTime time = LocalDateTime.now();
                if (!scraped) {
                    misses++;
                }
                else {
                    misses = 0;
                    hits++;
                    String hitWord = hits == 1 ? "hit" : "hits";
                    System.out.println(hits + " " + hitWord + " (" + urlHits + " from url)" +
                            "   average url hit millis: " + getAverageUrlHitTime() +
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
                        millisForAllURLHits -= pauseSeconds * 1000;
                    }
                }
                boolean nextCourtOffice = misses >= missesBeforeGivingUp;
                if (nextCourtOffice) {
                    misses = 0;
                }
                lastTime = System.currentTimeMillis();
                advancePointer(pointer, nextCourtOffice);
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
                    System.err.println("Scrape hit max url hits permitted " +
                            "(" + URL_HITS_PERMITTED + ") with pointer: " + pointer + " at " + now);
                }
                System.err.println("\nSleeping for " + HOURS_WAIT + " hours, restart at " +
                        now.plus(RESET_PERMISSIONS_TIME, ChronoUnit.MILLIS) + "\n");

                Thread.sleep(RESET_PERMISSIONS_TIME);
                millisForAllURLHits -= RESET_PERMISSIONS_TIME;

                System.err.println("RESTARTING at " + LocalDateTime.now());
            }
        }
    }

    private static String getAverageUrlHitTime() {
        if (urlHits == 0) return "N/A";
        return "" + (millisForAllURLHits/urlHits);
    }

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
    private static void advancePointer (Pointer pointer, boolean nextCourtOffice) throws IOException, ClassNotFoundException {
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
                pointer.setYear(getStartYear());
                List<String> courtOffices = getCourtOffices(pointer.getCounty());
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
                            " (" + (index + 2) + " of " + (courtOffices.size() + 1) + ") ***\n");
                }
            }

            if (nextCounty) {
                //first, save 'done' range on old county
                if (!commencingScrape) saveCountyDone(pointer.getCounty());

                pointer.setYear(getStartYear());
                int index = 1 + Website.counties.indexOf(pointer.getCounty());
                if (index == Website.counties.size()) {
                    index = 0;
                    if (!commencingScrape) {
                        System.out.println("FINISHED ALL COUNTIES at " + LocalDateTime.now() +
                                ". Requires manual restart.");
                        System.exit(0);
                    }
                }
                String prior = pointer.getCounty();
                pointer.setCounty(Website.counties.get(index));

                System.out.println("*** Bumping county from " + prior +
                        " to " + pointer.getCounty() +
                        " (" + (index + 1) + " of " + (Website.counties.size()) + ")");

                pointer.setCourtOffice(getCourtOffices(pointer.getCounty()).get(0));

                CountyCoveredRange ccr = getCountyStartAndEnd(pointer.getCounty());

                lastCheck = ccr == null ? null : ccr.getEnd();
            }
        }
        else {
            pointer.setSequenceNumberUnformatted(1 + pointer.getSequenceNumberUnformatted());
        }

        savePointer(pointer);
    }

    /**
     * two time stamps describing date range of local (scraped) pdfs
     * 1) the time this county's pull was completed
     * 2) start year of reliable data
     *
     * #2 is a pull'sstart year UNLESS
     *     already exists and is earlier
     */
    private static void saveCountyDone(String county) throws IOException, ClassNotFoundException {
        CountyCoveredRange ccr = new CountyCoveredRange();
        ccr.setEnd(LocalDateTime.now());
        LocalDateTime startFromThisRun = LocalDateTime.of(getStartYear(), 1, 1, 0, 0);

        CountyCoveredRange prior = getCountyStartAndEnd(county);
        LocalDateTime start = (prior != null && prior.getStart().compareTo(startFromThisRun) < 0) ?
                prior.getStart() : startFromThisRun;

        File file = new File(PDF_CACHE_PATH + county,COMPLETION_FILE_NAME);
        try (FileOutputStream fileOut = new FileOutputStream(file);
             ObjectOutputStream objectOut = new ObjectOutputStream(fileOut)) {

            ccr.setStart(start);
            objectOut.writeObject(ccr);
        }
    }

    public static CountyCoveredRange getCountyStartAndEnd(String county) throws IOException, ClassNotFoundException {
        File file = new File(PDF_CACHE_PATH + county,COMPLETION_FILE_NAME);
        if (!file.exists()) {
            return null;
        }
        try (FileInputStream fileIn = new FileInputStream(file);
             ObjectInputStream objectOut = new ObjectInputStream(fileIn)) {
            return (CountyCoveredRange) objectOut.readObject();
        }
    }

    private static List<String> getCourtOffices(String county) throws IOException {
        //ensure we have all the court offices for target county
        if (!countyCourtOffices.containsKey(county)) {
            countyCourtOffices.put(county, getCourtOfficesFromServer(county));
        }
        return countyCourtOffices.get(county);
    }

    private static void savePointer(Pointer pointer) throws IOException {
            try (FileOutputStream fileOut = new FileOutputStream(pointerFile);
                ObjectOutputStream objectOut = new ObjectOutputStream(fileOut)) {

                objectOut.writeObject(pointer);
            }
    }

    private static Pointer readPointer() throws IOException, ClassNotFoundException {
        try (FileInputStream fileIn = new FileInputStream(pointerFile);
             ObjectInputStream objectOut = new ObjectInputStream(fileIn)) {

            return (Pointer) objectOut.readObject();
        }
    }

    /**
     * We'll start in prior year if we are presently in first six months of a year
     */
    private static int getStartYear() {
//        LocalDateTime now = LocalDateTime.now();
//        if (now.getMonthValue() < 7) {
//            return now.getYear() - 1;
//        }
//        else return now.getYear();
        return 2019;
    }

    static int getCurrentYear() {
        return LocalDateTime.now().getYear();
    }

    public static boolean scrape(Pointer pointer, LocalDateTime lastCheck) throws IOException, InterruptedException {
        String county = pointer.getCounty();
        String courtOffice = pointer.getCourtOffice();
        String year = pointer.getYear() + "";
        String sequenceNumber = buildSequenceNumber(pointer.getSequenceNumberUnformatted());

        File dir = new File(PDF_CACHE_PATH + pointer.getCounty() + "/" + pointer.getYear());
        if (!dir.exists()) dir.mkdirs();

        String pathToFile = getPdfFilePath(pointer);
        File file = new File(pathToFile);

        boolean foundOrRead = false;
        try {
            String docket = "MJ-" + courtOffice + "-LT-" + sequenceNumber + "-" + year;
            boolean scrape = true;
            boolean rescrape = false;

            if (file.exists()) {
                foundOrRead = true;
                scrape = false;
                PdfData oldData = Parser.processFile(file);
                if (oldData.rescrape(lastCheck)) {
                    scrape = true;
                    rescrape = true;
                }
            }

            if (scrape) {
                try {
                    Thread.sleep(BETWEEN_SCRAPE_PAUSE);
                } catch (InterruptedException e) {
                    throw e;
                }

                foundOrRead = getDocket(county, year, docket, rescrape) || foundOrRead;
            }
        } catch (IllegalStateException ise) {
            throw ise;
        } catch (InvalidPdfException ipe) {
            System.err.println("Cannot process saved pdf for scraping: " + pointer);
            System.err.println("Next step: delete ^ locally and re-call " + pointer);
            deleteLocalPdf(pointer);
            return scrape(pointer, lastCheck);
        } catch (Exception e) {
            System.err.println("Cannot scrape " + pointer);
            e.printStackTrace();
            throw e;
        }

        return foundOrRead;
    }

    private static void deleteLocalPdf(Pointer pointer) {
        String pathToFile = getPdfFilePath(pointer);
        File file = new File(pathToFile);
        file.delete();
    }

    /**
     * Some potential for infinite loop, since scrape triggers process triggers ParseAll triggers this
     */
    public static void deleteAndReloadPdf(Pointer pointer) throws IOException, InterruptedException {
        deleteLocalPdf(pointer);
        scrape(pointer, null);
    }

    public static boolean getDocket(String county, String year, String docket, boolean rescrape) throws IOException, InterruptedException {
        long startMillis = System.currentTimeMillis();

        CloseableHttpClient httpclient = HttpClients.createDefault();

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
                    }
                    else {
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
            while((bytesRead = inputStream.read(buffer)) != -1){
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


        String url = "https://ujsportal.pacourts.us/Report/MdjDocketSheet?" +
                "docketNumber=" + docket + "&" +
                "dnh=" + dnh;

        String rescrapeString = rescrape ? "rescrape" : "new";
        System.out.println(rescrapeString + " pdf url: " + url);

        httpGet = new HttpGet(url);

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

            String pathToFile = getPdfFilePath(county, year, docket);
            FileOutputStream fos = new FileOutputStream(pathToFile);
            fos.write(bytes);
            fos.close();

            EntityUtils.consume(entity3);

            long endMillis = System.currentTimeMillis();
            long millis = endMillis - startMillis;
            millisForAllURLHits += millis;
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

    private static String getPdfFilePath(Pointer pointer) {
       return getPdfFilePath(pointer.getCounty(),
               pointer.getYear() + "",
               pointer.getCourtOffice(),
               buildSequenceNumber(pointer.getSequenceNumberUnformatted()));
    }

    private static String getPdfFilePath(String county, String year, String courtOffice, String sequence) {
        while (courtOffice.startsWith("0")) courtOffice = courtOffice.substring(1);
        return PDF_CACHE_PATH + county + "/" + year + "/" +
                courtOffice + "_" + sequence + "_" + year + ".pdf";
    }

    static String mdj = "MJ-";
    static String lt = "LT-";
    private static String getPdfFilePath(String county, String year, String docket) {
        docket = excise(excise(docket, mdj), lt);
        while (docket.startsWith("0")) {
            docket = docket.substring(1);
        }
        docket = docket.replace('-', '_');
        return PDF_CACHE_PATH + county + "/" + year + "/" + docket + ".pdf";
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

    public static Pointer getPointerFromPdfFileName(String county, String pdfFileName) {
        Pointer ret = new Pointer();
        ret.setCounty(county);
        int i = pdfFileName.indexOf('_');

        String courtOffice = pdfFileName.substring(0, i);
        while (courtOffice.length() < 5) {
            courtOffice = "0" + courtOffice;
        }
        ret.setCourtOffice(courtOffice);
        pdfFileName = pdfFileName.substring(i + 1);

        i = pdfFileName.indexOf('_');
        ret.setSequenceNumberUnformatted(Integer.parseInt(pdfFileName.substring(0, i)));
        pdfFileName = pdfFileName.substring(i + 1);

        i = pdfFileName.indexOf('.');
        ret.setYear(Integer.parseInt(pdfFileName.substring(0, i)));

        return ret;
    }
}
