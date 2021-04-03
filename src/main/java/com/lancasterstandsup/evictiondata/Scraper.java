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
import java.time.LocalDateTime;
import java.util.*;

public class Scraper {
    private static int BETWEEN_CALL_PAUSE = 200;
    //millis till restart on non-200 status;
    private static int WAIT_ON_FAIL = 60000 * 15;

    private static int fivehundreds = 0;

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
        //pulling 2015 4/3/21
        scrape("Lancaster", "2015");

        //scrape("Lancaster", "2020");
        //scrape("Lancaster", "2021");

        //scrape("Lancaster", "2016");
        //scrape("Lancaster", "2015");

        //scrape("York", "2021");
        //scrape("York", "2018");

        //scrape("Berks", "2019");
        //scrape("Berks", "2021");

        //scrape("Lebanon", "2019");
//        scrape("Lebanon", "2020");
//        scrape("Lebanon", "2021");

        //scrape("Dauphin", "2019");
        //scrape("Dauphin", "2020");
        //scrape("Dauphin", "2021");

        //scrape("Chester", "2019");
//        scrape("Chester", "2020");
//        scrape("Chester", "2021");

        //scrape("Philadelphia", "2020");
    }

    private static String[] knownFullStopPathSource = {
            //Lancaster 2021
            //"2000_0000001_2021",

            //Lancaster 2020
            "2000_0000001_2020",
            "2101_0000255_2020",
            "2102_0000098_2020",
            "2103_0000124_2020",
            "2201_0000112_2020",
            "2202_0000085_2020",
            "2203_0000149_2020",
            "2204_0000129_2020",
            "2205_0000107_2020",
            "2206_0000079_2020",
            "2207_0000075_2020",
            "2208_0000065_2020",
            "2301_0000115_2020",
            "2302_0000125_2020",
            "2303_0000033_2020",
            "2304_0000086_2020",
            "2305_0000071_2020",
            "2306_0000044_2020",
            "2307_0000029_2020",
            "2309_0000091_2020",
            "2999_0000001_2020",

            //Lancaster 2019
            "2000_0000001_2019",
            "2101_0000507_2019",
            "2102_0000229_2019",
            "2103_0000337_2019",
            "2201_0000303_2019",
            "2202_0000171_2019",
            "2203_0000410_2019",
            "2204_0000307_2019",
            "2205_0000178_2019",
            "2206_0000244_2019",
            "2207_0000195_2019",
            "2208_0000206_2019",
            "2301_0000261_2019",
            "2302_0000242_2019",
            "2303_0000056_2019",
            "2304_0000160_2019",
            "2305_0000150_2019",
            "2306_0000132_2019",
            "2307_0000055_2019",
            "2309_0000258_2019",
            "2999_0000001_2019",

            //Lancaster 2018
            "2000_0000001_2018",
            "2101_0000564_2018", //not manually checked
            "2102_0000207_2018", //not manually checked
            "2103_0000292_2018", //not manually checked
            "2201_0000316_2018", //not manually checked
            "2202_0000175_2018", //not manually checked
            "2203_0000376_2018", //following all not manually checked
            "2204_0000298_2018",
            "2205_0000241_2018",
            "2206_0000287_2018",
            "2207_0000164_2018",
            "2208_0000174_2018",
            "2301_0000246_2018",
            "2302_0000249_2018",
            "2303_0000081_2018",
            "2304_0000149_2018",


            //Lancaster 2017
            "2000_0000001_2017",
            "2101_0000567_2017",
            "2102_0000242_2017",
            "2103_0000289_2017",
            "2201_0000370_2017",
            "2202_0000160_2017",
            "2203_0000351_2017",
            "2204_0000373_2017",
            "2205_0000214_2017",
            "2206_0000211_2017",
            "2207_0000174_2017",
            "2208_0000154_2017",
            "2301_0000346_2017",
            "2302_0000246_2017",
            "2303_0000075_2017",
            "2304_0000179_2017",
            "2305_0000115_2017",
            "2306_0000127_2017",
            "2307_0000055_2017",

            //York 2020
            "19001_0000001_2020",
            "19101_0000365_2020",
            "19102_0000324_2020",
            "19103_0000165_2020",
            "19104_0000386_2020",
            "19105_0000406_2020",
            "19201_0000241_2020",
            "19202_0000303_2020",
            "19203_0000094_2020",
            "19204_0000161_2020",
            "19205_0000119_2020",
            "19301_0000211_2020",
            "19303_0000029_2020",

            //York 2019
            "19001_0000001_2019",
            "19101_0000633_2019",
            "19102_0000596_2019",
            "19103_0000238_2019",
            "19104_0000621_2019",
            "19105_0000679_2019",
            "19201_0000399_2019",
            "19202_0000578_2019",
            "19203_0000214_2019",
            "19204_0000255_2019",
            "19205_0000246_2019",
            "19301_0000370_2019",
            "19303_0000062_2019",
            "19304_0000124_2019",
            "19305_0000201_2019",

            //Berks 2020
            "23001_0000001_2020",
            "23002_0000001_2020",
            "23003_0000001_2020",
            "23101_0000208_2020",
            "23102_0000347_2020",
            "23103_0000326_2020",
            "23105_0000365_2020",
            "23106_0000099_2020",
            "23201_0000292_2020",
            "23202_0000100_2020",
            "23203_0000099_2020",
            "23204_0000037_2020",
            "23301_0000034_2020",
            "23302_0000044_2020",
            "23303_0000049_2020",
            "23304_0000043_2020",
            "23305_0000041_2020",
            "23306_0000042_2020",
            "23307_0000026_2020",

            //Berks 2021
            "23103_0000023_2021",

            //Lebanon 2021
            "52000_0000001_2021",
            "52101_0000019_2021",
            "52201_0000013_2021",
            "52301_0000012_2021",
            "52303_0000002_2021",
            "52304_0000001_2021",
            "52305_0000004_2021",

            //Lebanon 2020
            "52000_0000001_2020",
            "52101_0000202_2020",
            "52201_0000182_2020",
            "52301_0000098_2020",
            "52303_0000116_2020",
            "52304_0000057_2020",
            "52305_0000046_2020",
    };

    private static Set<String> knownFullStops = new HashSet<>();
    private static String suggestedFullStops = "";

    static {
//        for (String s : knownFullStopPathSource) {
//            knownFullStops.add(s);
//        }
    }

    private final static String PDF_CACHE_PATH = "./src/main/resources/pdfCache/";
    private final static String site = "https://ujsportal.pacourts.us/CaseSearch";

    public static void scrape(String county, String year) throws IOException, InterruptedException, ClassNotFoundException {
        scrape(county, year, null);

        System.out.println("Finished " + county + ", " + year);
    }

    public static void scrape(String county, String year, Map<String, List<PdfData>> courtToTargets) throws IOException, InterruptedException {
        String verb = courtToTargets == null ? "Scraping" : "Re-scraping";
        System.out.println("**********************");
        System.out.println(verb + " " + county + " " + year);
        System.out.println("**********************");
        boolean done = false;
        while (!done) {
            try {
                done = start(county, year, courtToTargets);
            }
            catch (Exception e) {
                System.err.println("An error made it up to 'scrape'");
                e.printStackTrace();
                done = false;
            }
            if (!done) {
                System.err.println("'scrape' detects failure somewhere, do the whole thing over");
                int timer = WAIT_ON_FAIL;

                while (timer > 0) {
                    try {
                        System.out.println("Start over in " + timer + " millis");
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    timer -= 60000;
                }
            }
        }
    }

    private static boolean start(String county, String year, Map<String, List<PdfData>> courtToTargets) throws IOException, InterruptedException {
        File dir = new File(PDF_CACHE_PATH + county + "/" + year);
        if (!dir.exists()) dir.mkdirs();

        LocalDateTime now = LocalDateTime.now();
        int y = now.getYear();
        int targetYear = Integer.parseInt(year);
        boolean rescrapeIfActive = targetYear >= y - 1;

        fivehundreds = 0;
        List<String> courtOffices;
        if (courtToTargets == null) {
            courtOffices = getCourtHouses(county);
        }
        else {
            courtOffices = new LinkedList<>();
            courtOffices.addAll(courtToTargets.keySet());
        }
        if (courtOffices == null) {
            System.err.println("no judges available, consider starting the whole thing again");
            return false;
        }

        for (String courtOffice: courtOffices) {
            try {
                callCourtHouse(county, courtOffice, year, courtToTargets, rescrapeIfActive);
            }
            catch (Exception e) {
                e.printStackTrace();
                System.err.println("failed on county " + county + ". Maybe just restart everything.");
                return false;
            }

            System.out.println("sleep for a bit b/n courthouses for " + county + " " + year);
            Thread.sleep(500);
        }

        //System.out.println(suggestedFullStops);
        //String verb = courtToTargets == null ? "scraping" : "re-scraping";

        return true;
    }

    static String preCountyFlag = "<option data-aopc-County=\"(";
    static String postCountyFlag = ")\" data-aopc-JudicialDistrict=\"(";
    /**
     *
     * @return List of courtHouses (judge codes)
     * @throws IOException
     */
    public static List<String> getCourtHouses(String county) throws IOException {
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

    public static boolean getDocket(String county, String year, String docket) throws IOException, InterruptedException {
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

            //System.out.println("dnh: " + dnh);

            EntityUtils.consume(entity4);
        } catch (Exception e) {
            throw e;
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
        while (!success) {
            response5 = httpclient.execute(httpGet);
            statusCode = response5.getStatusLine().getStatusCode();
            if (statusCode != 200 && attempts >= 3) {
                System.err.println("Pdf call status: " + response5.getStatusLine());
                response5.close();
                httpGet.releaseConnection();
                if (statusCode == 500) {
                    System.err.println("****************************");
                    System.err.println(docket + " got 3 500s, so skipping");
                    System.err.println("****************************");
                    fivehundreds++;
                    if (fivehundreds > 5) {
                        System.err.println("Too many 500s, System exit");
                        System.exit(1);
                    }
                    return false;
                }
                else {
                    httpclient.close();
                    System.err.println("Start over (pdf call)");
                    throw new IllegalStateException("plz start over (pdf)");
                }
            }

            if (statusCode == 200) {
                success = true;
            }
            else {
                System.err.println("pdf Pause #" + (attempts + 1) +
                        " because of of call status: " + response5.getStatusLine());
                attempts++;
                Thread.sleep(BETWEEN_CALL_PAUSE);
            }
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
            throw e;
        } finally {
            if (response5 != null) response5.close();
        }
    }

    /**
     * @param county
     * @param courtOffice
     * @param year
     * @return next sequence, or -1 if finished
     * @throws IOException
     */
    private static void callCourtHouse(
            String county,
            String courtOffice,
            String year,
            Map<String, List<PdfData>> courtToTargets,
            boolean rescrapeIfActive)
                    throws IOException {
        int next = courtToTargets == null ? 1 : 0;
        if (courtToTargets == null) {
            int probePastFail = 3;
            int probe = 0;
            boolean done = false;
            while (!done) {
                String sequenceNumber = buildSequenceNumber(next);

                if (knownFullStops.contains(courtOffice + "_" + sequenceNumber + "_" + year)) {
                    System.out.println("Stopping " + courtOffice + " on known full stop " +
                            courtOffice + "_" + sequenceNumber + "_" + year);
                    suggestedFullStops += " \"" + courtOffice + "_" + sequenceNumber + "_" + year + "\",\n";
                    return;
                } else {
                    String pathToFile = getPdfFilePath(county, year, courtOffice, sequenceNumber);
                    File file = new File(pathToFile);

                    boolean foundOrRead = false;
                    try {
                        if (!file.exists()) {
                            //probe 1 to avoid known gaps
                            String probeSequence = buildSequenceNumber(next + 1);
                            String pathToFile2 = getPdfFilePath(county, year, courtOffice, probeSequence);
                            File file2 = new File(pathToFile2);
                            if (file2.exists()) {
                                System.out.println("Skipping to next file, since " + courtOffice + "_" +
                                        sequenceNumber + "_" + year + " is a file gap");
                                next++;
                                sequenceNumber = probeSequence;
                                file = file2;
                                pathToFile = pathToFile2;
                            }
                        }
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

                    if (foundOrRead) {
                        next++;
                        probe = 0;
                    } else {
                        if (probe < probePastFail) {
                            next++;
                            probe++;
                            if (probe == 1) System.out.println("Probing... starting at " + next);
                        } else {
                            System.out.println("Probed " + probePastFail + " times without a hit");
                            String suggestion = courtOffice + "_" + buildSequenceNumber(next - probePastFail) + "_" + year;
                            System.out.println("Suggested knownFullStop: " + suggestion);
                            suggestedFullStops += " \"" + suggestion + "\",\n";
                            return;
                        }
                    }
                }
            }
        }
        else {
            List<PdfData> original = courtToTargets.get(courtOffice);
            if (original.isEmpty()) return;
            List<PdfData> targets = new LinkedList<>();
            for (PdfData pdf: original) {
                targets.add(pdf);
            }
            for (int x = next; x < targets.size(); x++) {
                PdfData pdf = targets.get(x);
                boolean read = false;
                try {
                    try {
                        Thread.sleep(BETWEEN_CALL_PAUSE);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    String docket = "MJ-" + courtOffice + "-LT-" + pdf.getSequenceNumber() + "-" + year;
                    read = getDocket(county, year, docket);

                    next++;
                } catch (IllegalStateException ise) {
                    throw ise;
                } catch (Exception e) {
                    System.err.println("Exception prevents re-scraping " + pdf.getDocketNumber());
                    System.err.println("Premature termination of re-scraping for " + courtOffice + " loop");
                    e.printStackTrace();
                    throw new IllegalStateException("plz start over");
                }
                if (!read) {
                    System.err.println("Cannot re-scrape " + pdf.getDocketNumber());
                    System.err.println("Premature termination of re-scraping for " + courtOffice + " loop");
                    throw new IllegalStateException("plz start over");
                }
                else {
                    original.remove(pdf);
                    System.out.println("Re-scraped " + pdf.getDocketNumber());
                    if (original.isEmpty()) return;
                }
            }
        }

        throw new IllegalStateException("Finished getting one court: " + courtOffice);
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












//public static int callBaseForJudge(String county,
//                                       String year,
//                                       String courtOffice,
//                                       int start,
//                                       Map<String, List<PdfData>> courtToTargets)
//                                            throws IOException {
//        CloseableHttpClient httpclient = HttpClients.createDefault();
//
//        HttpGet httpGet = new HttpGet(site);
//
//        CloseableHttpResponse response = httpclient.execute(httpGet);
//
//        int callBaseForJudgesStatus = response.getStatusLine().getStatusCode();
//        if (callBaseForJudgesStatus != 200) {
//            System.err.println("callBaseForJudge (singular) status: " + response.getStatusLine());
//            httpGet.releaseConnection();
//            httpclient.close();
//            throw new IllegalStateException("plz start over");
//        }
//
//        Header[] headers = response.getAllHeaders();
//
//        //******** Get Cookies from base page return **********
//
//        String cookies = "";
//        for (Header header : headers) {
//            String val = header.getValue();
//            //System.out.println("header... " + header.getName() + ", " + val);
//            if (header.getName().equals("Set-Cookie")) {
//                if (!cookies.equals("")) {
//                    cookies += "; ";
//                }
//                cookies += val;
//            }
//        }
//
//        String pathC = " path=/";
//        String httpC = " HttpOnly";
//        String sameC = " SameSite=Lax";
//        String secureC = " secure";
//        HashSet<String> badCookies = new HashSet<>();
//        badCookies.add(pathC);
//        badCookies.add(httpC);
//        badCookies.add(sameC);
//        badCookies.add(secureC);
//
//        String[] cookiePrint = cookies.split(";");
//        List<String> finalCookies = new ArrayList<>();
//        for (String s : cookiePrint) {
//            //System.out.println("cookie: " + s);
//            if (!badCookies.contains(s)) {
//                finalCookies.add(s.trim());
//            }
//        }
//
//        cookies = "";
//        for (String s : finalCookies) {
//            //System.out.println("finalCookie: " + s);
//            if (!cookies.equals("")) {
//                cookies += "; ";
//            }
//            cookies += s;
//        }
//
////        try {
////            Thread.sleep(BETWEEN_CALL_PAUSE);
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
//
//        String viewStateFromCall1 = null;
//        String captchaAnswerFromCall1 = null;
//        try {
//            HttpEntity entity = response.getEntity();
//
//            ByteArrayOutputStream os = new ByteArrayOutputStream();
//            InputStream inputStream = entity.getContent();
//            byte[] buffer = new byte[8192];
//            int bytesRead;
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                os.write(buffer, 0, bytesRead);
//            }
//
//            String xml = new String(os.toByteArray());
//            inputStream.close();
//            os.close();
//            xml = xml.trim();
//
////            File file = new File("./src/main/resources/htmlfromcall1.html");
////            file.createNewFile();
////
////            Writer targetFileWriter = new FileWriter(file);
////            targetFileWriter.write(xml);
////            targetFileWriter.close();
//
//            String viewStatePrefix = "__VIEWSTATE\" id=\"__VIEWSTATE\" value=\"";
//            int i = xml.indexOf(viewStatePrefix) + viewStatePrefix.length();
//            viewStateFromCall1 = xml.substring(i);
//            viewStateFromCall1 = viewStateFromCall1.substring(0, viewStateFromCall1.indexOf("\""));
//
//            //System.out.println("call 1 __VIEWSTATE: " + viewStateFromCall1);
//
//            String captchaPrefix = "_captchaAnswer' ).value = '";
//            i = xml.indexOf(captchaPrefix) + captchaPrefix.length();
//            captchaAnswerFromCall1 = xml.substring(i);
//            captchaAnswerFromCall1 = captchaAnswerFromCall1.substring(0, captchaAnswerFromCall1.indexOf("'"));
//
//            //System.out.println("captchaAnswerFromCall1: " + captchaAnswerFromCall1);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            response.close();
//            httpGet.releaseConnection();
//        }
//
////        try {
////            Thread.sleep(BETWEEN_CALL_PAUSE);
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
//
////        System.out.println();
////        System.out.println("***********************************");
////        System.out.println("******* CALL #2 (send county) **********");
////        System.out.println("***********************************");
////        System.out.println();
//
//        HttpPost httpPost = new HttpPost(site);
//
//        httpPost.addHeader("Cookie", cookies);
//
//        List<NameValuePair> nvps = new ArrayList<>();
//        nvps.add(new BasicNameValuePair("__EVENTTARGET", "ctl00$ctl00$ctl00$cphMain$cphDynamicContent$cphSearchControls$udsDocketNumber$ddlCounty"));
//        nvps.add(new BasicNameValuePair("__EVENTARGUMENT", ""));
//        nvps.add(new BasicNameValuePair("__LASTFOCUS", ""));
//        nvps.add(new BasicNameValuePair("__VIEWSTATE", viewStateFromCall1));
//        nvps.add(new BasicNameValuePair("__VIEWSTATEGENERATOR", "4AB257F3"));
//        nvps.add(new BasicNameValuePair("__SCROLLPOSITIONX", "0"));
//        nvps.add(new BasicNameValuePair("__SCROLLPOSITIONY", "609"));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$ddlSearchType", "DocketNumber"));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$cphSearchControls$udsDocketNumber$ddlCounty", county));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$ctl07$captchaAnswer", captchaAnswerFromCall1));
//        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
//        CloseableHttpResponse response2 = httpclient.execute(httpPost);
//
//        String viewStateFromCall2 = null;
//        try {
//            int countyCallStatus = response2.getStatusLine().getStatusCode();
//            if (countyCallStatus != 200) {
//                System.err.println("Call with " + county + " county response: " + response2.getStatusLine());
//                response2.close();
//                httpPost.releaseConnection();
//                httpclient.close();
//                throw new IllegalStateException("plz start over");
//            }
//            HttpEntity entity2 = response2.getEntity();
//
//            ByteArrayOutputStream os = new ByteArrayOutputStream();
//            InputStream inputStream = entity2.getContent();
//            byte[] buffer = new byte[8192];
//            int bytesRead;
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                os.write(buffer, 0, bytesRead);
//            }
//
//            String xml = new String(os.toByteArray());
//            xml = xml.trim();
//            inputStream.close();
//            os.close();
//
//            String viewStatePrefix = "__VIEWSTATE\" id=\"__VIEWSTATE\" value=\"";
//            int i = xml.indexOf(viewStatePrefix) + viewStatePrefix.length();
//            viewStateFromCall2 = xml.substring(i);
//            viewStateFromCall2 = viewStateFromCall2.substring(0, viewStateFromCall2.indexOf("\""));
//
//            EntityUtils.consume(entity2);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            response2.close();
//            httpPost.releaseConnection();
//        }
//
////        try {
////            Thread.sleep(BETWEEN_CALL_PAUSE);
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
//
//        try {
//            int ret = callCourtHouse(
//                    httpclient,
//                    cookies,
//                    viewStateFromCall2,
//                    start,
//                    county,
//                    courtOffice,
//                    year,
//                    captchaAnswerFromCall1,
//                    courtToTargets);
//
//            return ret;
//        }
//        catch (IllegalStateException ise) {
//            throw ise;
//        }
//        finally {
//            httpclient.close();
//        }
//    }


//    private static String judgePrefixish =
//            "ctl00_ctl00_ctl00_cphMain_cphDynamicContent_cphSearchControls_udsDocketNumber_ddlCourtOffice\" class=\"standard\">";
//    private static String judgeEnd = "</select>";
//    private static String valueTag = "value=\"";
//
//    private static List<String> extractCourtHouses(String xml) {
//        List<String> ret = new ArrayList<>();
//        int i = xml.indexOf(judgePrefixish) + judgePrefixish.length();
//        xml = xml.substring(i);
//        xml = xml.substring(0, xml.indexOf(judgeEnd));
//
//        boolean loop = true;
//        while (loop) {
//            i = xml.indexOf(valueTag);
//            if (i < 0) {
//                loop = false;
//            }
//            else {
//                xml = xml.substring(i + valueTag.length());
//                String judgeCode = xml.substring(0, xml.indexOf('"'));
//                if (!judgeCode.equals("0")) ret.add(judgeCode);
//            }
//        }
//
//        return ret;
//    }
//
//
//                                       private static boolean getPdf(
//                                               CloseableHttpClient httpclient,
//                                       String cookies,
//                                       String viewStateFromCall3,
//                                       String county,
//                                       String courtOffice,
//                                       String courtOfficePerhapsWithPrecedingZero,
//                                       String sequenceNumber,
//                                       String year,
//                                       String captchaAnswerFromCall1) throws Exception {
//
////        System.out.println();
////        System.out.println("***********************************");
////        System.out.println("******* Call with docket # to get link to pdf **********");
////        System.out.println("***********************************");
////        System.out.println();
//
//        HttpPost httpPost = new HttpPost(site);
//
//        httpPost.addHeader("Cookie", cookies);
//
//        List<NameValuePair> nvps = new ArrayList<>();
//        nvps.add(new BasicNameValuePair("__EVENTTARGET", ""));
//        nvps.add(new BasicNameValuePair("__EVENTARGUMENT", ""));
//        nvps.add(new BasicNameValuePair("__LASTFOCUS", ""));
//        nvps.add(new BasicNameValuePair("__VIEWSTATE", viewStateFromCall3));
//        nvps.add(new BasicNameValuePair("__VIEWSTATEGENERATOR", "4AB257F3"));
//        nvps.add(new BasicNameValuePair("__SCROLLPOSITIONX", "0"));
//        nvps.add(new BasicNameValuePair("__SCROLLPOSITIONY", "609"));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$ddlSearchType", "DocketNumber"));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$cphSearchControls$udsDocketNumber$ddlCounty", county));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$cphSearchControls$udsDocketNumber$ddlCourtOffice", courtOffice));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$cphSearchControls$udsDocketNumber$ddlDocketType", docketType));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$cphSearchControls$udsDocketNumber$txtSequenceNumber", sequenceNumber));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$cphSearchControls$udsDocketNumber$txtYear", year));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$btnSearch", "Search"));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$ctl07$captchaAnswer", captchaAnswerFromCall1));
//        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
//
//        boolean finished = false;
//        int loops = 0;
//        CloseableHttpResponse response4 = null;
//        while (!finished) {
//            response4 = httpclient.execute(httpPost);
//            int statusCode = response4.getStatusLine().getStatusCode();
//            if (statusCode != 200 && loops >= 3) {
//                System.err.println("getDnh for " + courtOffice + "_" + sequenceNumber + "_" + year + " status: " + response4.getStatusLine());
//                response4.close();
//                httpPost.releaseConnection();
//                if (statusCode == 500) {
//                    System.err.println("****************************");
//                    System.err.println("getDnh for " + courtOffice + "_" + sequenceNumber + "_" +
//                            year + " got 3 500s, so skipping");
//                    System.err.println("****************************");
//                    fivehundreds++;
//                    if (fivehundreds > 5) {
//                        System.err.println("Too many 500s, System exit from getDnh");
//                        System.exit(1);
//                    }
//                    return true;
//                }
//                else {
//                    httpclient.close();
//                    System.err.println("Start over (dnh call)");
//                    throw new IllegalStateException("plz start over (dnh)");
//                }
//            }
//
//            if (statusCode == 200) {
//                finished = true;
//            }
//            else {
//                System.err.println("dnh Pause #" + (loops + 1) +
//                        " because of of dnh call status: " + response4.getStatusLine());
//                loops++;
//                Thread.sleep(BETWEEN_CALL_PAUSE);
//            }
//        }
//
//        String dnh = null;
//        try {
//
//            HttpEntity entity4 = response4.getEntity();
//
//            ByteArrayOutputStream os = new ByteArrayOutputStream();
//            InputStream inputStream = entity4.getContent();
//            byte[] buffer = new byte[8192];
//            int bytesRead;
//            while((bytesRead = inputStream.read(buffer)) != -1){
//                os.write(buffer, 0, bytesRead);
//            }
//
//            String xml = new String(os.toByteArray());
//            xml = xml.trim();
//            inputStream.close();
//            os.close();
//
////            File file = new File("./src/main/resources/htmlfromcall4.html");
////            file.createNewFile();
////
////            Writer targetFileWriter = new FileWriter(file);
////            targetFileWriter.write(xml);
////            targetFileWriter.close();
//
//            String dnhPrefix = "dnh=";
//            int i = xml.indexOf(dnhPrefix);
//            //there was no case/docket number
//            if (i < 0) {
//                response4.close();
//                System.out.println("No such sequence #: " + sequenceNumber + " for courtOffice " + courtOffice);
//                return false;
//            }
//            i += dnhPrefix.length();
//            dnh = xml.substring(i);
//            dnh = dnh.substring(0, dnh.indexOf('"'));
//
//            //System.out.println("dnh: " + dnh);
//
//            EntityUtils.consume(entity4);
//        } catch (Exception e) {
//            throw e;
//        } finally {
//            response4.close();
//            httpPost.releaseConnection();
//        }
//
////        System.out.println();
////        System.out.println("****************************");
////        System.out.println("****** CALL #5 (pdf?) *********");
////        System.out.println("****************************");
////        System.out.println();
//
//        String docketNumber = "MJ-" +
//                courtOfficePerhapsWithPrecedingZero +
//                "-" +
//                docketType +
//                "-" +
//                sequenceNumber +
//                "-" +
//                year;
//
//        String url = "https://ujsportal.pacourts.us/DocketSheets/MDJReport.ashx?" +
//                "docketNumber=" + docketNumber + "&" +
//                "dnh=" + dnh;
//
//        System.out.println("pdf url: " + url);
//
//        HttpGet httpGet = new HttpGet(url);
//
//        int attempts = 0;
//        boolean success = false;
//        CloseableHttpResponse response5 = null;
//        while (!success) {
//            response5 = httpclient.execute(httpGet);
//            int statusCode = response5.getStatusLine().getStatusCode();
//            if (statusCode != 200 && attempts >= 3) {
//                System.err.println("Pdf call status: " + response5.getStatusLine());
//                response5.close();
//                httpGet.releaseConnection();
//                if (statusCode == 500) {
//                    System.err.println("****************************");
//                    System.err.println(courtOffice + "_" + sequenceNumber + "_" +
//                            year + " got 3 500s, so skipping");
//                    System.err.println("****************************");
//                    fivehundreds++;
//                    if (fivehundreds > 5) {
//                        System.err.println("Too many 500s, System exit");
//                        System.exit(1);
//                    }
//                    return true;
//                }
//                else {
//                    httpclient.close();
//                    System.err.println("Start over (pdf call)");
//                    throw new IllegalStateException("plz start over (pdf)");
//                }
//            }
//
//            if (statusCode == 200) {
//                success = true;
//            }
//            else {
//                System.err.println("pdf Pause #" + (attempts + 1) +
//                        " because of of call status: " + response5.getStatusLine());
//                attempts++;
//                Thread.sleep(BETWEEN_CALL_PAUSE);
//            }
//        }
//
//        try {
////            Header[] headers = response5.getAllHeaders();
////            for (Header header: headers) {
////                System.out.println("Pdf call response header: " + header.getName() + ", " + header.getValue());
////            }
//            HttpEntity entity5 = response5.getEntity();
//            InputStream inputStream = entity5.getContent();
//
//            ByteArrayOutputStream os = new ByteArrayOutputStream();
//            byte[] buffer = new byte[8192];
//            int bytesRead;
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                os.write(buffer, 0, bytesRead);
//            }
//
//            byte[] bytes = os.toByteArray();
//            os.close();
//
//            //String fileName = courtOffice + "_" + sequenceNumber + "_" + year + ".pdf";
//            String pathToFile = getPdfFilePath(county, year, courtOffice, sequenceNumber);
//            FileOutputStream fos = new FileOutputStream(pathToFile);
//            fos.write(bytes);
//            fos.close();
//
//            EntityUtils.consume(entity5);
//            inputStream.close();
//            response5.close();
//            return true;
//        } catch (Exception e) {
//            throw e;
//        } finally {
//            if (response5 != null) response5.close();
//        }
//    }
////

//    /**
//     *
//     * @return List of courtHouses (judge codes)
//     * @throws IOException
//     */
//    public static List<String> getCourtHouses(String county, String year) throws IOException {
//        CloseableHttpClient httpclient = HttpClients.createDefault();
//
//        HttpGet httpGet = new HttpGet(site);
//
//        CloseableHttpResponse response = httpclient.execute(httpGet);
//        int callBaseForJudgesStatus = response.getStatusLine().getStatusCode();
//        if (callBaseForJudgesStatus != 200) {
//            System.err.println("callBaseForJudges status: " + response.getStatusLine());
//            httpGet.releaseConnection();
//            httpclient.close();
//            throw new IllegalStateException("start over plz");
//        }
//
//        Header[] headers = response.getAllHeaders();
//
//        //******** Get Cookies from base page return **********
//
//        String cookies = "";
//        for (Header header : headers) {
//            String val = header.getValue();
//            if (header.getName().equals("Set-Cookie")) {
//                if (!cookies.equals("")) {
//                    cookies += "; ";
//                }
//                cookies += val;
//            }
//        }
//
//        Set<String> badCookieChunks = new HashSet<>();
//        badCookieChunks.add(" path=/");
//        badCookieChunks.add(" samesite=strict");
//        badCookieChunks.add(" httponly");
//        badCookieChunks.add(" HttpOnly");
//        badCookieChunks.add(" secure");
//
//        String[] cookiePrint = cookies.split(";");
//        List<String> finalCookies = new ArrayList<>();
//        for (String s : cookiePrint) {
//            //System.out.println("cookie: " + s);
//            if (!badCookieChunks.contains(s)) {
//                finalCookies.add(s.trim());
//            }
//        }
//
//        cookies = "";
//        for (String s : finalCookies) {
//            //System.out.println("finalCookie: " + s);
//            if (!cookies.equals("")) {
//                cookies += "; ";
//            }
//            cookies += s;
//        }
//        //System.out.println("call 1 cookies: " + cookies);
//
//        String viewStateFromCall1 = null;
//        String captchaAnswerFromCall1 = null;
//        try {
//            HttpEntity entity = response.getEntity();
//
//            ByteArrayOutputStream os = new ByteArrayOutputStream();
//            InputStream inputStream = entity.getContent();
//            byte[] buffer = new byte[8192];
//            int bytesRead;
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                os.write(buffer, 0, bytesRead);
//            }
//
//            String xml = new String(os.toByteArray());
//            inputStream.close();
//            os.close();
//            xml = xml.trim();
//
////            File file = new File("./src/main/resources/htmlfromcall1.html");
////            file.createNewFile();
////
////            Writer targetFileWriter = new FileWriter(file);
////            targetFileWriter.write(xml);
////            targetFileWriter.close();
//
//            String viewStatePrefix = "__VIEWSTATE\" id=\"__VIEWSTATE\" value=\"";
//            int i = xml.indexOf(viewStatePrefix) + viewStatePrefix.length();
//            viewStateFromCall1 = xml.substring(i);
//            viewStateFromCall1 = viewStateFromCall1.substring(0, viewStateFromCall1.indexOf("\""));
//
//            //System.out.println("call 1 __VIEWSTATE: " + viewStateFromCall1);
//
//            String captchaPrefix = "_captchaAnswer' ).value = '";
//            i = xml.indexOf(captchaPrefix) + captchaPrefix.length();
//            captchaAnswerFromCall1 = xml.substring(i);
//            captchaAnswerFromCall1 = captchaAnswerFromCall1.substring(0, captchaAnswerFromCall1.indexOf("'"));
//
//            //System.out.println("captchaAnswerFromCall1: " + captchaAnswerFromCall1);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            response.close();
//        }
//
////        System.out.println();
////        System.out.println("***********************************");
////        System.out.println("******* CALL #2 (send county) **********");
////        System.out.println("***********************************");
////        System.out.println();
//
////        try {
////            Thread.sleep(BETWEEN_CALL_PAUSE);
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
//
//        HttpPost httpPost = new HttpPost(site);
//
//        httpPost.addHeader("Cookie", cookies);
//
//        List<NameValuePair> nvps = new ArrayList<>();
//        nvps.add(new BasicNameValuePair("__EVENTTARGET", "ctl00$ctl00$ctl00$cphMain$cphDynamicContent$cphSearchControls$udsDocketNumber$ddlCounty"));
//        nvps.add(new BasicNameValuePair("__EVENTARGUMENT", ""));
//        nvps.add(new BasicNameValuePair("__LASTFOCUS", ""));
//        nvps.add(new BasicNameValuePair("__VIEWSTATE", viewStateFromCall1));
//        nvps.add(new BasicNameValuePair("__VIEWSTATEGENERATOR", "4AB257F3"));
//        nvps.add(new BasicNameValuePair("__SCROLLPOSITIONX", "0"));
//        nvps.add(new BasicNameValuePair("__SCROLLPOSITIONY", "609"));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$ddlSearchType", "DocketNumber"));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$cphSearchControls$udsDocketNumber$ddlCounty", county));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$ctl07$captchaAnswer", captchaAnswerFromCall1));
//        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
//        CloseableHttpResponse response2 = httpclient.execute(httpPost);
//
//        //String viewStateFromCall2 = null;
//        List<String> courtHouses = null;
//        try {
//            int countyCallStatus = response2.getStatusLine().getStatusCode();
//            if (countyCallStatus != 200) {
//                System.err.println("Call with " + county + " county response: " + response2.getStatusLine());
//                httpGet.releaseConnection();
//                httpclient.close();
//                response2.close();
//                throw new IllegalStateException("plz start over");
//            }
//
//            HttpEntity entity2 = response2.getEntity();
//
//            ByteArrayOutputStream os = new ByteArrayOutputStream();
//            InputStream inputStream = entity2.getContent();
//            byte[] buffer = new byte[8192];
//            int bytesRead;
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                os.write(buffer, 0, bytesRead);
//            }
//
//            String xml = new String(os.toByteArray());
//            xml = xml.trim();
//            inputStream.close();
//            os.close();
//
//            courtHouses = extractCourtHouses(xml);
//
////            String viewStatePrefix = "__VIEWSTATE\" id=\"__VIEWSTATE\" value=\"";
////            int i = xml.indexOf(viewStatePrefix) + viewStatePrefix.length();
////            viewStateFromCall2 = xml.substring(i);
////            viewStateFromCall2 = viewStateFromCall2.substring(0, viewStateFromCall2.indexOf("\""));
//
//            EntityUtils.consume(entity2);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            response2.close();
//        }
//
//        httpclient.close();
////        try {
////            Thread.sleep(BETWEEN_CALL_PAUSE);
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
//
//        return courtHouses;
//    }
//private static boolean getPdf(
//        CloseableHttpClient httpclient,
//        String cookies,
//        String viewStateFromCall3,
//        String county,
//        String courtOffice,
//        String courtOfficePerhapsWithPrecedingZero,
//        String sequenceNumber,
//        String year,
//        String captchaAnswerFromCall1) throws Exception {
//
////        System.out.println();
////        System.out.println("***********************************");
////        System.out.println("******* Call with docket # to get link to pdf **********");
////        System.out.println("***********************************");
////        System.out.println();
//
//        HttpPost httpPost = new HttpPost(site2);
//
//        httpPost.addHeader("Cookie", cookies);
//
//        List<NameValuePair> nvps = new ArrayList<>();
//        nvps.add(new BasicNameValuePair("__EVENTTARGET", ""));
//        nvps.add(new BasicNameValuePair("__EVENTARGUMENT", ""));
//        nvps.add(new BasicNameValuePair("__LASTFOCUS", ""));
//        nvps.add(new BasicNameValuePair("__VIEWSTATE", viewStateFromCall3));
//        nvps.add(new BasicNameValuePair("__VIEWSTATEGENERATOR", "4AB257F3"));
//        nvps.add(new BasicNameValuePair("__SCROLLPOSITIONX", "0"));
//        nvps.add(new BasicNameValuePair("__SCROLLPOSITIONY", "609"));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$ddlSearchType", "DocketNumber"));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$cphSearchControls$udsDocketNumber$ddlCounty", county));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$cphSearchControls$udsDocketNumber$ddlCourtOffice", courtOffice));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$cphSearchControls$udsDocketNumber$ddlDocketType", docketType));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$cphSearchControls$udsDocketNumber$txtSequenceNumber", sequenceNumber));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$cphSearchControls$udsDocketNumber$txtYear", year));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$btnSearch", "Search"));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$ctl07$captchaAnswer", captchaAnswerFromCall1));
//        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
//
//        boolean finished = false;
//        int loops = 0;
//        CloseableHttpResponse response4 = null;
//        while (!finished) {
//            response4 = httpclient.execute(httpPost);
//            int statusCode = response4.getStatusLine().getStatusCode();
//            if (statusCode != 200 && loops >= 3) {
//                System.err.println("getDnh for " + courtOffice + "_" + sequenceNumber + "_" + year + " status: " + response4.getStatusLine());
//                response4.close();
//                httpPost.releaseConnection();
//                if (statusCode == 500) {
//                    System.err.println("****************************");
//                    System.err.println("getDnh for " + courtOffice + "_" + sequenceNumber + "_" +
//                            year + " got 3 500s, so skipping");
//                    System.err.println("****************************");
//                    fivehundreds++;
//                    if (fivehundreds > 5) {
//                        System.err.println("Too many 500s, System exit from getDnh");
//                        System.exit(1);
//                    }
//                    return true;
//                }
//                else {
//                    httpclient.close();
//                    System.err.println("Start over (dnh call)");
//                    throw new IllegalStateException("plz start over (dnh)");
//                }
//            }
//
//            if (statusCode == 200) {
//                finished = true;
//            }
//            else {
//                System.err.println("dnh Pause #" + (loops + 1) +
//                        " because of of dnh call status: " + response4.getStatusLine());
//                loops++;
//                Thread.sleep(BETWEEN_CALL_PAUSE);
//            }
//        }
//
//        String dnh = null;
//        try {
//
//            HttpEntity entity4 = response4.getEntity();
//
//            ByteArrayOutputStream os = new ByteArrayOutputStream();
//            InputStream inputStream = entity4.getContent();
//            byte[] buffer = new byte[8192];
//            int bytesRead;
//            while((bytesRead = inputStream.read(buffer)) != -1){
//                os.write(buffer, 0, bytesRead);
//            }
//
//            String xml = new String(os.toByteArray());
//            xml = xml.trim();
//            inputStream.close();
//            os.close();
//
////            File file = new File("./src/main/resources/htmlfromcall4.html");
////            file.createNewFile();
////
////            Writer targetFileWriter = new FileWriter(file);
////            targetFileWriter.write(xml);
////            targetFileWriter.close();
//
//            String dnhPrefix = "dnh=";
//            int i = xml.indexOf(dnhPrefix);
//            //there was no case/docket number
//            if (i < 0) {
//                response4.close();
//                System.out.println("No such sequence #: " + sequenceNumber + " for courtOffice " + courtOffice);
//                return false;
//            }
//            i += dnhPrefix.length();
//            dnh = xml.substring(i);
//            dnh = dnh.substring(0, dnh.indexOf('"'));
//
//            //System.out.println("dnh: " + dnh);
//
//            EntityUtils.consume(entity4);
//        } catch (Exception e) {
//            throw e;
//        } finally {
//            response4.close();
//            httpPost.releaseConnection();
//        }
//
////        System.out.println();
////        System.out.println("****************************");
////        System.out.println("****** CALL #5 (pdf?) *********");
////        System.out.println("****************************");
////        System.out.println();
//
//        String docketNumber = "MJ-" +
//                courtOfficePerhapsWithPrecedingZero +
//                "-" +
//                docketType +
//                "-" +
//                sequenceNumber +
//                "-" +
//                year;
//
//        String url = "https://ujsportal.pacourts.us/DocketSheets/MDJReport.ashx?" +
//                "docketNumber=" + docketNumber + "&" +
//                "dnh=" + dnh;
//
//        System.out.println("pdf url: " + url);
//
//        HttpGet httpGet = new HttpGet(url);
//
//        int attempts = 0;
//        boolean success = false;
//        CloseableHttpResponse response5 = null;
//        while (!success) {
//            response5 = httpclient.execute(httpGet);
//            int statusCode = response5.getStatusLine().getStatusCode();
//            if (statusCode != 200 && attempts >= 3) {
//                System.err.println("Pdf call status: " + response5.getStatusLine());
//                response5.close();
//                httpGet.releaseConnection();
//                if (statusCode == 500) {
//                    System.err.println("****************************");
//                    System.err.println(courtOffice + "_" + sequenceNumber + "_" +
//                            year + " got 3 500s, so skipping");
//                    System.err.println("****************************");
//                    fivehundreds++;
//                    if (fivehundreds > 5) {
//                        System.err.println("Too many 500s, System exit");
//                        System.exit(1);
//                    }
//                    return true;
//                }
//                else {
//                    httpclient.close();
//                    System.err.println("Start over (pdf call)");
//                    throw new IllegalStateException("plz start over (pdf)");
//                }
//            }
//
//            if (statusCode == 200) {
//                success = true;
//            }
//            else {
//                System.err.println("pdf Pause #" + (attempts + 1) +
//                        " because of of call status: " + response5.getStatusLine());
//                attempts++;
//                Thread.sleep(BETWEEN_CALL_PAUSE);
//            }
//        }
//
//        try {
////            Header[] headers = response5.getAllHeaders();
////            for (Header header: headers) {
////                System.out.println("Pdf call response header: " + header.getName() + ", " + header.getValue());
////            }
//            HttpEntity entity5 = response5.getEntity();
//            InputStream inputStream = entity5.getContent();
//
//            ByteArrayOutputStream os = new ByteArrayOutputStream();
//            byte[] buffer = new byte[8192];
//            int bytesRead;
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                os.write(buffer, 0, bytesRead);
//            }
//
//            byte[] bytes = os.toByteArray();
//            os.close();
//
//            //String fileName = courtOffice + "_" + sequenceNumber + "_" + year + ".pdf";
//            String pathToFile = getPdfFilePath(county, year, courtOffice, sequenceNumber);
//            FileOutputStream fos = new FileOutputStream(pathToFile);
//            fos.write(bytes);
//            fos.close();
//
//            EntityUtils.consume(entity5);
//            inputStream.close();
//            response5.close();
//            return true;
//        } catch (Exception e) {
//            throw e;
//        } finally {
//            if (response5 != null) response5.close();
//        }
//    }
//    /**
//     *
//     * @param httpclient
//     * @param cookies
//     * @param viewStateFromCall2
//     * @param start
//     * @param county
//     * @param courtOffice
//     * @param year
//     * @param captchaAnswerFromCall1
//     * @return next sequence, or -1 if finished
//     * @throws IOException
//     */
//    private static int callCourtHouse(
//            CloseableHttpClient httpclient,
//            String cookies,
//            String viewStateFromCall2,
//            int start,
//            String county,
//            String courtOffice,
//            String year,
//            String captchaAnswerFromCall1,
//            Map<String, List<PdfData>> courtToTargets)
//            throws IOException {
//        HttpPost httpPost = new HttpPost(site);
//
//        httpPost.addHeader("Cookie", cookies);
//
//        List<NameValuePair> nvps = new ArrayList<>();
//        nvps.add(new BasicNameValuePair("__EVENTTARGET", "ctl00$ctl00$ctl00$cphMain$cphDynamicContent$cphSearchControls$udsDocketNumber$ddlCourtOffice"));
//        nvps.add(new BasicNameValuePair("__EVENTARGUMENT", ""));
//        nvps.add(new BasicNameValuePair("__LASTFOCUS", ""));
//        nvps.add(new BasicNameValuePair("__VIEWSTATE", viewStateFromCall2));
//        nvps.add(new BasicNameValuePair("__VIEWSTATEGENERATOR", "4AB257F3"));
//        nvps.add(new BasicNameValuePair("__SCROLLPOSITIONX", "0"));
//        nvps.add(new BasicNameValuePair("__SCROLLPOSITIONY", "609"));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$ddlSearchType", "DocketNumber"));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$cphSearchControls$udsDocketNumber$ddlCounty", county));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$cphMain$cphDynamicContent$cphSearchControls$udsDocketNumber$ddlCourtOffice", courtOffice));
//        nvps.add(new BasicNameValuePair("ctl00$ctl00$ctl00$ctl07$captchaAnswer", captchaAnswerFromCall1));
//        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
//        CloseableHttpResponse response3 = httpclient.execute(httpPost);
//
//        String viewStateFromCall3 = null;
//        String courtOfficePerhapsWithPrecedingZero = null;
//        String courtOfficePerhapsWithPrecedingZeroPrefix =
//                "ctl00_ctl00_ctl00_cphMain_cphDynamicContent_cphSearchControls_udsDocketNumber_lblCourt\">";
//        try {
//            System.out.println();
//            int callJudgeStatus = response3.getStatusLine().getStatusCode();
//            if (callJudgeStatus != 200) {
//                System.err.println("callJudge " + courtOffice + " status: " + response3.getStatusLine());
//                response3.close();
//                httpPost.releaseConnection();
//                httpclient.close();
//                throw new IllegalStateException("plz start over");
//            }
//            HttpEntity entity3 = response3.getEntity();
//
//            ByteArrayOutputStream os = new ByteArrayOutputStream();
//            InputStream inputStream = entity3.getContent();
//            byte[] buffer = new byte[8192];
//            int bytesRead;
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                os.write(buffer, 0, bytesRead);
//            }
//
//            String xml = new String(os.toByteArray());
//            xml = xml.trim();
//            inputStream.close();
//            os.close();
//
//            String viewStatePrefix = "__VIEWSTATE\" id=\"__VIEWSTATE\" value=\"";
//            int i = xml.indexOf(viewStatePrefix) + viewStatePrefix.length();
//            viewStateFromCall3 = xml.substring(i);
//            viewStateFromCall3 = viewStateFromCall3.substring(0, viewStateFromCall3.indexOf("\""));
//
//            i = xml.indexOf(courtOfficePerhapsWithPrecedingZeroPrefix) +
//                    courtOfficePerhapsWithPrecedingZeroPrefix.length();
//            courtOfficePerhapsWithPrecedingZero = xml.substring(i);
//            courtOfficePerhapsWithPrecedingZero = courtOfficePerhapsWithPrecedingZero.substring(
//                    0, courtOfficePerhapsWithPrecedingZero.indexOf("<"));
//
//            EntityUtils.consume(entity3);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            response3.close();
//        }
//
////        try {
////            Thread.sleep(BETWEEN_CALL_PAUSE);
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
//
//        int remotePulls = 0;
//        int next = start;
//        if (courtToTargets == null) {
//            int probePastFail = 3;
//            int probe = 0;
//            boolean done = false;
//            while (!done) {
//                String sequenceNumber = buildSequenceNumber(next);
//
//                if (knownFullStops.contains(courtOffice + "_" + sequenceNumber + "_" + year)) {
//                    System.out.println("Stopping " + courtOffice + " on known full stop " +
//                            courtOffice + "_" + sequenceNumber + "_" + year);
//                    suggestedFullStops += " \"" + courtOffice + "_" + sequenceNumber + "_" + year + "\",\n";
//                    return -1;
//                } else {
//                    String pathToFile = getPdfFilePath(county, year, courtOffice, sequenceNumber);
//                    File file = new File(pathToFile);
//
//                    boolean foundOrRead = false;
//                    try {
//                        if (!file.exists()) {
//                            //probe 1 to avoid known gaps
//                            String probeSequence = buildSequenceNumber(next + 1);
//                            String pathToFile2 = getPdfFilePath(county, year, courtOffice, probeSequence);
//                            File file2 = new File(pathToFile2);
//                            if (file2.exists()) {
//                                System.out.println("Skipping to next file, since " + courtOffice + "_" +
//                                        sequenceNumber + "_" + year + " is a file gap");
//                                next++;
//                                sequenceNumber = probeSequence;
//                                file = file2;
//                                pathToFile = pathToFile2;
//                            }
//                        }
//                        if (file.exists()) {
//                            foundOrRead = true;
//                        } else {
//                            try {
//                                Thread.sleep(BETWEEN_CALL_PAUSE);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//
//                            foundOrRead = getPdf(httpclient,
//                                    cookies,
//                                    viewStateFromCall3,
//                                    county,
//                                    courtOffice,
//                                    courtOfficePerhapsWithPrecedingZero,
//                                    sequenceNumber,
//                                    year,
//                                    captchaAnswerFromCall1);
//
//                            remotePulls++;
//                        }
//                    } catch (IllegalStateException ise) {
//                        throw ise;
//                    } catch (Exception e) {
//                        String from = file.exists() ? "file" : "remote stream";
//                        System.err.println("Cannot process " + courtOffice + "_" + sequenceNumber + "_" + year + " from " + from);
//                        System.err.println("Premature termination of " + courtOffice + " loop");
//                        e.printStackTrace();
//                        throw new IllegalStateException("plz start over");
//                    }
//
//                    if (foundOrRead) {
//                        next++;
//                        probe = 0;
//                    } else {
//                        if (probe < probePastFail) {
//                            next++;
//                            probe++;
//                            if (probe == 1) System.out.println("Probing... starting at " + next);
//                        } else {
//                            System.out.println("Probed " + probePastFail + " times without a hit");
//                            String suggestion = courtOffice + "_" + buildSequenceNumber(next - probePastFail) + "_" + year;
//                            System.out.println("Suggested knownFullStop: " + suggestion);
//                            suggestedFullStops += " \"" + suggestion + "\",\n";
//                            return -1;
//                        }
//                    }
//                }
//
//                if (probe == 0 && remotePulls >= MAX_REMOTE_PULLS) {
//                    return next;
//                }
//            }
//        }
//        else {
//            List<PdfData> original = courtToTargets.get(courtOffice);
//            if (original.isEmpty()) return -1;
//            List<PdfData> targets = new LinkedList<>();
//            for (PdfData pdf: original) {
//                targets.add(pdf);
//            }
//            for (int x = next; x < targets.size(); x++) {
//                PdfData pdf = targets.get(x);
//                boolean read = false;
//                try {
//                    try {
//                        Thread.sleep(BETWEEN_CALL_PAUSE);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//
//                    read = getPdf(httpclient,
//                            cookies,
//                            viewStateFromCall3,
//                            county,
//                            courtOffice,
//                            courtOfficePerhapsWithPrecedingZero,
//                            pdf.getSequenceNumber(),
//                            year,
//                            captchaAnswerFromCall1);
//
//                    remotePulls++;
//                    next++;
//                } catch (IllegalStateException ise) {
//                    throw ise;
//                } catch (Exception e) {
//                    System.err.println("Exception prevents re-scraping " + pdf.getDocketNumber());
//                    System.err.println("Premature termination of re-scraping for " + courtOffice + " loop");
//                    e.printStackTrace();
//                    throw new IllegalStateException("plz start over");
//                }
//                if (!read) {
//                    System.err.println("Cannot re-scrape " + pdf.getDocketNumber());
//                    System.err.println("Premature termination of re-scraping for " + courtOffice + " loop");
//                    throw new IllegalStateException("plz start over");
//                }
//                else {
//                    original.remove(pdf);
//                    System.out.println("Re-scraped " + pdf.getDocketNumber());
//                    if (original.isEmpty()) return -1;
//                    if (remotePulls >= MAX_REMOTE_PULLS) {
//                        return next;
//                    }
//                }
//            }
//            return -1;
//        }
//
//        throw new IllegalStateException("Should never get here!");
//    }


//    private static void rescrapeActiveRecords(String county, String year) throws IOException, ClassNotFoundException, InterruptedException {
//        //Any case that is from this or prior year and not closed/inactive: re-scrape pdf
//        //Caution: if eviction in progress, we want it even if 'closed'
//        List<PdfData> thisYear = ParseAll.parseAll(county, year);
//        thisYear = Analysis.orderByDocket(thisYear);
//        thisYear = Analysis.filterByClosedOrInactive(thisYear);
//        Map<String, List<PdfData>> byCourtOffice = Analysis.groupByCourtOffice(thisYear);
//        System.out.println("Will re-scrape " + thisYear.size() + " " + year + " non-closed pdfs");
//        Scraper.scrape(county, year, byCourtOffice);
//    }
}
