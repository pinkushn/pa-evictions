package com.lancasterstandsup.evictiondata;

import com.itextpdf.text.exceptions.InvalidPdfException;

import java.io.*;
import java.util.*;

public class ParseAll {

    private final static String PDF_CACHE_PATH = Scraper.PDF_CACHE_PATH;

    //county --> cities in county
    private static Map<String, Set<String>> cities = new HashMap<>();

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        String county = "Lancaster";
        String year = "2022";

        get(Scraper.CourtMode.MDJ_CR, county, year, false);
    }

    public static Map<String, List<PdfData>> get(Scraper.CourtMode courtMode, String county, String year, boolean reverseChronological) throws IOException, ClassNotFoundException {
        List<PdfData> all = parseAll(courtMode, county, year, false);

        Map<String, List<PdfData>> byJudge = sortByJudge(all);

        byJudge.put("All", sortByDate(all, reverseChronological));

        return byJudge;
    }

    /**
     * every year in local files
     *
     * @param county
     */
    public static <T extends PdfData> List<T> get(Scraper.CourtMode courtMode, String county, String[] years) throws IOException, ClassNotFoundException {
        List<T> list = new LinkedList<>();

        for (String year: years) {
            list.addAll(parseAll(courtMode, county, year, false));
        }

        return list;
    }

    private static <T extends PdfData> Map<String, List<T>> sortByJudge(List<T> pdfs) {
        TreeMap<String, List<T>> map = new TreeMap<>();
        for (PdfData pdf: pdfs) {
            String judgeName = pdf.getJudgeName();
            if (!map.containsKey(judgeName)) {
                map.put(judgeName, new ArrayList<>());
            }
            List<T> list = map.get(judgeName);
            list.add((T) pdf);
        }

        return map;
    }

    private static <T extends PdfData> List<T> sortByDate(List<T> pdfs, boolean reverseChronology) {
        TreeSet<T> ss = new TreeSet<>();
        ss.addAll(pdfs);

        List<T> ret = new ArrayList<>(ss.size());

        if (!reverseChronology) {
            ret.addAll(ss);
        }
        else {
            Iterator<T> i = ss.descendingIterator();
            while(i.hasNext()) ret.add(i.next());
        }

        return ret;
    }

    public static <T extends PdfData> List<T> parseAll(Scraper.CourtMode courtMode, String county, String year, boolean countExpunged) throws IOException, ClassNotFoundException {
        if (countExpunged) {
            Worksheet.clearPreProcessed(county);
        }

        String dirPath = PDF_CACHE_PATH + courtMode.getFolderName() + "/" + county + "/" + year;
        String preProcessedPath = PDF_CACHE_PATH + courtMode.getFolderName() + "/" + county + "/" + year + "_preProcessed";

        File dir = new File(dirPath);
        File[] pdfs = dir.listFiles();

        File preProcessedDir = new File(preProcessedPath);
        if (!preProcessedDir.exists()) preProcessedDir.mkdir();
        File[] pres = preProcessedDir.listFiles();
        HashMap<String, File> mapOfPreprocessed = new HashMap<>();
        if (pres != null) {
            for (File f : pres) {
                mapOfPreprocessed.put(f.getName(), f);
            }
        }
        Set<String> setOfPreprocessed = mapOfPreprocessed.keySet();

        System.out.println("Parsing " + pdfs.length + " pdfs for " + county + ", " + year);

        List<T> ret = new ArrayList<>(pdfs.length);
        int expunged = 0;
        int malformed = 0;
        PdfData previous = null;
        if (countExpunged) {
            TreeMap<String, File> ordered = new TreeMap<>();
            for (File f: pdfs) {
                ordered.put(f.getName(), f);
            }
            ordered.values().toArray(pdfs);
        }
        for (File pdf: pdfs) {
            try {
                PdfData data;
                int pdfI = pdf.getName().indexOf(".pdf");
                if (pdfI > -1) {
                    String stripPdf = pdf.getName().substring(0, pdfI);
                    if (setOfPreprocessed.contains(stripPdf)) {
                        FileInputStream fin = new FileInputStream(preProcessedPath + "/" + stripPdf);
                        ObjectInputStream ois = new ObjectInputStream(fin);
                        data = (PdfData) ois.readObject();
                        ois.close();
                        fin.close();
                    } else {
                        //System.out.println("Processing " + pdf);
                        InputStream targetStream = new FileInputStream(pdf);
//                        data = courtMode == Scraper.CourtMode.MDJ_LT ?
//                                LTParser.process(targetStream, false) :
//                                CRParser.process(targetStream, false);
                        data = getParser(courtMode).process(targetStream, false);
                        if (data != null && data.isClosed()) {
                            FileOutputStream fout = new FileOutputStream(preProcessedPath + "/" + stripPdf, false);
                            ObjectOutputStream oos = new ObjectOutputStream(fout);
                            oos.writeObject(data);
                            oos.close();
                        }
                        else if (data == null) {
                            System.err.println("****** Unable to parse " + pdf.getName() + " ******");
                        }
                        targetStream.close();
                    }
                    if (data != null) ret.add((T) data);

                    if (countExpunged) {
                        if (previous != null) {
                            if (previous.getCourtOffice().equals(data.getCourtOffice())) {
                                int docketDiff = data.getDocketNumberAsInt() - previous.getDocketNumberAsInt();
                                expunged += (docketDiff - 1);
                            }
                        }
                        //if it's the first pdf for a court office, and it is numbered 2 or more
                        //then there's at least one expunged file
                        else {
                            expunged += (data.getDocketNumberAsInt() - 1);
                        }
                    }

                    if (data != null) previous = data;
                }
                else {
                    System.out.println("Found a non-pdf file: " + pdf.getName());
                }
            }
            /**
             * This was an attempt to deal with malformed pdfs, which in the end
             * just reloads malformed pdfs!
             */
            catch (InvalidPdfException ipe) {
//                System.err.println("processAll snarled on pdf " + pdf.getName());
//                System.err.println("Will attempt to delete and force reload");
//                Pointer pointer = Scraper2.getPointerFromPdfFileName(county, pdf.getName());
//                try {
//                    Scraper2.deleteAndReloadPdf(pointer);
//                } catch (InterruptedException e) {
//                    System.err.println("fatal error in ParseAll, System will exit");
//                    e.printStackTrace();
//                    System.exit(1);
//                }
                System.err.println("malformed pdf cannot be processed: " + pdf.getName());
                malformed++;
            } catch (Exception e) {
                System.err.println("processAll cannot process " + pdf.getName());
                e.printStackTrace();
                malformed++;
                //throw e;
            }
        }

        System.out.println("parseAll successfully completed");
        if (countExpunged) {
            System.out.println(county + " " + year + "  Expunged: " + expunged + "  Malformed: " + malformed);
        }

        return ret;
    }

    public static <T extends PdfData> List<T> parseFromDockets(List<String> dockets, String county) throws NoSuchFieldException, IOException, ClassNotFoundException {
        List<T> ret = new ArrayList<>();
        for (String docket: dockets) {
            T t = parseFromDocket(docket, county);
            if (t != null) ret.add(t);
        }
        return ret;
    }

    public static <T extends PdfData> T parseFromDocket(String docket, String county) throws IOException, ClassNotFoundException, NoSuchFieldException {
        Scraper.CourtMode courtMode = PdfData.getCourtModeFromDocket(docket);
        String year = PdfData.getYearFromDocket(docket);
        String dirPath = PDF_CACHE_PATH + courtMode.getFolderName() + "/" + county + "/" + year;
        String preProcessedPath = PDF_CACHE_PATH + courtMode.getFolderName() + "/" + county + "/" + year + "_preProcessed";

        try {
            String fileName = PdfData.getJBFileName(docket);
            File file = new File(dirPath + "/" + fileName + ".pdf");
            if (!file.exists()) {
                System.err.println("No such file: " + file);
                return null;
            }
            InputStream targetStream = new FileInputStream(file);
            PdfData data = getParser(courtMode).process(targetStream, false);
            if (data != null && data.isClosed()) {
                FileOutputStream fout = new FileOutputStream(preProcessedPath + "/" + fileName, false);
                ObjectOutputStream oos = new ObjectOutputStream(fout);
                oos.writeObject(data);
                oos.close();
            }
            else if (data == null) {
                System.err.println("****** Unable to parse docket " + docket + " ******");
            }
            targetStream.close();

            return (T) data;
        }
        catch (InvalidPdfException ipe) {
            System.err.println("malformed docket cannot be processed: " + docket);
            throw ipe;
        } catch (Exception e) {
            System.err.println("Cannot process docket " + docket);
            e.printStackTrace();
            throw e;
        }
    }

    public static Parser getParser(Scraper.CourtMode courtMode) {
        if (courtMode == Scraper.CourtMode.MDJ_LT) {
            return LTParser.getSingleton();
        }
        else if (courtMode == Scraper.CourtMode.MDJ_CR) {
            return CRParser.getSingleton();
        }
        else if (courtMode == Scraper.CourtMode.CP_CR) {
            return CPParser.getSingleton();
        }
        throw new IllegalStateException("Mode " + courtMode + " not supported");
    }
//
//    private static boolean hasCitiesFile(String county) {
//        String pathToFile = "./src/main/resources/pdfCache/" +
//                county + "/cities.txt";
//        File file = new File(pathToFile);
//        return file.exists();
//    }
//
//    private static Set<String> getCities(String county) {
//        if (!cities.containsKey(county)) {
//            cities.put(county, readCities(county));
//        }
//
//        return cities.get(county);
//    }
//
//    private static Set<String> readCities(String county) {
//        String pathToFile = "./src/main/resources/pdfCache/" +
//                county + "/cities.txt";
//        HashSet ret = new HashSet();
//        try {
//            File file = new File(pathToFile);
//            if (file.exists()) {
//                BufferedReader in = new BufferedReader(new FileReader(file));
//                String next;
//                while ((next = in.readLine()) != null) {
//                    ret.add(next);
//                }
//                in.close();
//            }
//        } catch (Exception e) {
//            System.err.println("failure during getCities for " + county);
//        }
//        return ret;
//    }
//
//    private static void writeCities(String county, Set<String> set) {
//        cities.put(county, set);
//
//        String pathToFile = "./src/main/resources/pdfCache/" +
//                county + "/cities.txt";
//        File file = new File(pathToFile);
//
//        try {
//            PrintWriter out = new PrintWriter(new FileWriter(file));
//            for (String city: set) {
//                out.println(city);
//            }
//            out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
