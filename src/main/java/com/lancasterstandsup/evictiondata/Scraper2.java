package com.lancasterstandsup.evictiondata;

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
import java.util.*;

/*

Continuous scraping, all of PA?

* Scrape till blocked vs. stop scrape partway, pre-block
I like latter, as it stays within site's parameters
would need to
1) know parameters? Is it number of calls? And how long till it opens back up?
2) code county pull as a partial thing

a typewriter ribbon, endless loop
Next county?
  which year to start with?
      Normal year rule: if we're in first 6 months, start with last year
      Alternate year rule: start with forced year

Every call, save all arguments for that call to file. It's a 'pointer' to
the last place on the ribbon.
Is it the last successful call or the last record of a call about to be made?
Former is fragile, could succeed by crash before recording, maybe
Latter is more reliable. Whenever we restart, we can pick up with that even though
it may be one record extra to read.



 */

public class Scraper2 {
    private static int BETWEEN_CALL_PAUSE = 200;

    private final static String PDF_CACHE_PATH = "./src/main/resources/pdfCache/";
    private final static String site = "https://ujsportal.pacourts.us/CaseSearch";
    private final static String POINTER_PATH = "./src/main/resources/";
    private static File pointerFile;

    private static HashMap<String, List<String>> countyCourtHouses = new HashMap<>();

    public final static String[] countiesRaw = {
            "Lancaster",
            "York",
            "Berks",
            "Dauphin",
            "Lebanon"
    };

    private static List<String> counties;

    static {
        counties = Arrays.asList(countiesRaw);

        pointerFile = new File(POINTER_PATH, "pointer");
    }

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {

//        test to see if we move to York

//        Pointer pointer = new Pointer();
//        pointer.setYear(2022);
//        pointer.setCourthouse("02309");
//        pointer.setCounty("Lancaster");
//        pointer.setSequenceNumberUnformatted(4);
//        savePointer(pointer);

        if (!pointerFile.exists()) {
            //see advancePointer for trick to init
            Pointer pointer = new Pointer();
            pointer.setYear(getCurrentYear());
            pointer.setCounty(counties.get(counties.size() - 1));
            advancePointer(pointer, true);
        }

        commenceScraping(readPointer());
    }

    /**
     * Assumes inbound pointer is already saved to pointer file
     * @param pointer
     */
    public static void commenceScraping(Pointer pointer) {
        System.out.println("**********************");
        System.out.println("Starting with " + pointer);
        System.out.println("**********************");

        int x = 20;
        int misses = 0;
        int missesBeforeGivingUp = 2;
        while (x > 0) {
            try {
                boolean scraped = scrape(pointer);
                if (!scraped) {
                    misses++;
                }
                advancePointer(pointer, misses > missesBeforeGivingUp);
                if (misses > 0) misses = missesBeforeGivingUp;
            } catch (Exception e) {
                System.err.println("scrape fail at " + pointer);
                System.err.println("scraping will stop");
                e.printStackTrace();
                return;
            }

            x--;
        }

        System.out.println("Done scraping for now at " + pointer);
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
    private static void advancePointer (Pointer pointer, boolean nextCourtOffice) throws IOException {
        if (nextCourtOffice) {
            pointer.setSequenceNumberUnformatted(1);

            boolean nextCounty = pointer.getCourthouse() == null;

            int year = pointer.getYear();
            //may just increment year if not in current year
            if (year != getCurrentYear()) {
                pointer.setYear(year + 1);
            }
            else if (!nextCounty){
                pointer.setYear(getStartYear());
                List<String> courtOffices = getCourtOffices(pointer.getCounty());
                int index = courtOffices.indexOf(pointer.getCourthouse());
                if (index == courtOffices.size() - 1) {
                    nextCounty = true;
                }
                else {
                    String advancedCourtOffice = courtOffices.get(index + 1);
                    pointer.setCourthouse(advancedCourtOffice);

                    System.out.println("bumped court office, now at " + pointer.getCourthouse());
                }
            }

            if (nextCounty) {
                pointer.setYear(getStartYear());
                int index = 1 + counties.indexOf(pointer.getCounty());
                if (index == counties.size()) {
                    index = 0;
                }
                pointer.setCounty(counties.get(index));

                pointer.setCourthouse(getCourtOffices(pointer.getCounty()).get(0));

                System.out.println("bumped counties, now at " + pointer.getCounty());
            }
        }
        else {
            pointer.setSequenceNumberUnformatted(1 + pointer.getSequenceNumberUnformatted());
        }

        savePointer(pointer);
    }

    private static List<String> getCourtOffices(String county) throws IOException {
        //ensure we have all the courthouses for target county
        if (!countyCourtHouses.containsKey(county)) {
            countyCourtHouses.put(county, getCourtHouses(county));
        }
        return countyCourtHouses.get(county);
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
        return 2022;
    }

    private static int getCurrentYear() {
        return LocalDateTime.now().getYear();
    }

    public static boolean scrape(Pointer pointer) throws IOException, InterruptedException {
        boolean rescrapeIfActive = true;
        String county = pointer.getCounty();

        String courtOffice = pointer.getCourthouse();
        //initialize
        String year = pointer.getYear() + "";
        String sequenceNumber = buildSequenceNumber(pointer.getSequenceNumberUnformatted());


        File dir = new File(PDF_CACHE_PATH + pointer.getCounty() + "/" + pointer.getYear());
        if (!dir.exists()) dir.mkdirs();

        String pathToFile = getPdfFilePath(pointer);
        File file = new File(pathToFile);

        boolean foundOrRead;
        try {
            if (file.exists()) {
                foundOrRead = true;
                if (rescrapeIfActive) {
                    PdfData oldData = Parser.processFile(file);
                    if (!oldData.isClosed() && !oldData.isInactive()) {
                        String docket = "MJ-" + courtOffice + "-LT-" + sequenceNumber + "-" + year;
                        System.out.println("Will attempt to re-scrape active case " + docket);
                        getDocket(county, year, docket);
                    }
                }
            } else {
                try {
                    Thread.sleep(BETWEEN_CALL_PAUSE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                String docket = "MJ-" + courtOffice + "-LT-" + sequenceNumber + "-" + year;
                foundOrRead = getDocket(county, year, docket);
            }
        } catch (IllegalStateException ise) {
            throw ise;
        } catch (Exception e) {
            String from = file.exists() ? "file" : "remote stream";
            System.err.println("Cannot process " + courtOffice + "_" + sequenceNumber + "_" + year + " from " + from);
            System.err.println("Premature termination of " + courtOffice + " loop");
            e.printStackTrace();
            throw new IllegalStateException("plz start over");
        }

        return foundOrRead;
    }

    public static boolean getDocket(String county, String year, String docket) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet(site);

        CloseableHttpResponse response = httpclient.execute(httpGet);

        int status = response.getStatusLine().getStatusCode();
        if (status != 200) {
            System.err.println("callDocket status: " + response.getStatusLine());
            httpGet.releaseConnection();
            httpclient.close();
            throw new IllegalStateException("plz start over");
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
            //System.out.println("cookie: " + s);
            if (!badCookieChunks.contains(s)) {
                finalCookies.add(s.trim());
            }
        }

        cookies = "";
        for (String s : finalCookies) {
            //System.out.println("finalCookie: " + s);
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
            e.printStackTrace();
        } finally {
            response.close();
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
        int statusCode = response2.getStatusLine().getStatusCode();
        String dnh = null;

        try {
            HttpEntity entity4 = response2.getEntity();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            InputStream inputStream = entity4.getContent();
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

            EntityUtils.consume(entity4);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            response2.close();
            httpPost.releaseConnection();
        }

        String url = "https://ujsportal.pacourts.us/DocketSheets/MDJReport.ashx?" +
                "docketNumber=" + docket + "&" +
                "dnh=" + dnh;

        System.out.println("pdf url: " + url);

        httpGet = new HttpGet(url);

        int attempts = 0;
        boolean success = false;
        CloseableHttpResponse response5 = null;

        response5 = httpclient.execute(httpGet);
        statusCode = response5.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            System.err.println("Pdf call failed with call status: " + response5.getStatusLine());
            return false;
        }

        try {
            HttpEntity entity5 = response5.getEntity();
            InputStream inputStream = entity5.getContent();

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

            EntityUtils.consume(entity5);
            inputStream.close();
            response5.close();
            return true;
        } catch (Exception e) {
            System.err.println("Tripped up when writing pdf to file");
            e.printStackTrace();
            return false;
        } finally {
            if (response5 != null) response5.close();
        }
    }

    static String preCountyFlag = "<option data-aopc-County=\"(";
    static String postCountyFlag = ")\" data-aopc-JudicialDistrict=\"(";
    /**
     *
     * @return List of courtHouses (judge codes)
     * @throws IOException
     */
    public static List<String> getCourtHouses(String county) throws IOException {
        System.out.println("Scraping court offices for " + county);

        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet(site);

        CloseableHttpResponse response = httpclient.execute(httpGet);
        int status = response.getStatusLine().getStatusCode();
        if (status != 200) {
            System.err.println("getCourtHouses status: " + response.getStatusLine());
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
                //System.out.println(next);
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
                            //System.out.println(ret);
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
               pointer.getCourthouse(),
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
}
