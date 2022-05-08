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

        get(county, year, false);
    }

    public static Map<String, List<PdfData>> get(String county, String year, boolean reverseChronological) throws IOException, ClassNotFoundException {
        List<PdfData> all = parseAll(county, year, false);

        Map<String, List<PdfData>> byJudge = sortByJudge(all);

        byJudge.put("All", sortByDate(all, reverseChronological));

        return byJudge;
    }

    /**
     * every year in local files
     *
     * @param county
     * @return [low year, high year, List<PdfData>]
     */
    public static Object[] get(String county, String[] years, boolean reverseChronological) throws IOException, ClassNotFoundException {
        Object[] ret = new Object[3];
        List<PdfData> list = new LinkedList<>();

        ret[0] = years[0];
        ret[1] = years[years.length - 1];

        for (String year: years) {
            list.addAll(parseAll(county, year, false));
        }

        ret[2] = sortByDate(list, reverseChronological);

        return ret;
    }

    private static Map<String, List<PdfData>> sortByJudge(List<PdfData> pdfs) {
        TreeMap<String, List<PdfData>> map = new TreeMap<>();
        for (PdfData pdf: pdfs) {
            String judgeName = pdf.getJudgeName();
            if (!map.containsKey(judgeName)) {
                map.put(judgeName, new ArrayList<>());
            }
            List<PdfData> list = map.get(judgeName);
            list.add(pdf);
        }

        return map;
    }

    private static List<PdfData> sortByDate(List<PdfData> pdfs, boolean reverseChronology) {
        TreeSet<PdfData> ss = new TreeSet<>();
        ss.addAll(pdfs);

        List<PdfData> ret = new ArrayList<>(ss.size());

        if (!reverseChronology) {
            ret.addAll(ss);
        }
        else {
            Iterator<PdfData> i = ss.descendingIterator();
            while(i.hasNext()) ret.add(i.next());
        }

        return ret;
    }

    public static List<PdfData> parseAll(String county, String year, boolean countExpunged) throws IOException, ClassNotFoundException {
        if (countExpunged) {
            Worksheet.clearPreProcessed(county);
        }

        String dirPath = PDF_CACHE_PATH + county + "/" + year;
        String preProcessedPath = PDF_CACHE_PATH + county + "/" + year + "_preProcessed";

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

        List<PdfData> ret = new ArrayList<>(pdfs.length);
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
                        data = Parser.process(targetStream, false);
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
                    if (data != null) ret.add(data);

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
