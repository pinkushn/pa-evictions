package com.lancasterstandsup.evictiondata;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

public class CRAnalysis {

    public static void main (String[] args) throws IOException, ClassNotFoundException, InterruptedException, NoSuchFieldException {
        String[] years = {"2022"};
        List<CRPdfData> list = ParseAll.get(Scraper.CourtMode.MDJ_CR, "Lancaster", years);

        //unable(list);
        //uniqueOTN(list);
        //unableAndCurrentlyJailedInLancaster(list);

        //pretrial(list, LocalDate.now());

        //bailFundOTNs(list);

        forfeitureOTNs(list);

//        for (CRPdfData pdf: list) {
//            System.out.println(pdf.getDefendantName() + " currently jailed");
//        }
//        String[] years = {"2019"};
//        List<CRPdfData> list = ParseAll.get(Scraper.CourtMode.MDJ_CR, "Lancaster", years);
//
//        //20%
//        percentageOfMissingDockets(list);
//
//        String[] years2 = {"2020"};
//        list = ParseAll.get(Scraper.CourtMode.MDJ_CR, "Lancaster", years2);
//
//        //16%
//        percentageOfMissingDockets(list);
//
//        //9%
//        String[] years3 = {"2021"};
//        list = ParseAll.get(Scraper.CourtMode.MDJ_CR, "Lancaster", years3);
//
//        percentageOfMissingDockets(list);
//
//        //1%
//        String[] years4 = {"2022"};
//        list = ParseAll.get(Scraper.CourtMode.MDJ_CR, "Lancaster", years4);
//
//        percentageOfMissingDockets(list);
    }

    //what if cp level pdfs show more?
    public static int pretrial(List<CRPdfData> list, LocalDate date) {
        List<CRPdfData> ret = new ArrayList<>();
        for (CRPdfData pdf: list) {
            if (pdf.wasJailedThisDay(date)) {// && pdf.wasNotSentenced(date)) {
                ret.add(pdf);
            }
        }
        //issues: 1) may have same person recorded 2 or more times
        //2) for recent dates: docket changes not shown yet (has been jailed or released)
        //3) does someone ever get jailed at the cp level, no indicator at the mj level?
        System.out.println(ret.size() + " pretrial " + date);
        return ret.size();
    }

//    public static void pretrialNow(List<CRPdfData> list) {
//        List<CRPdfData> appearJailedNow = new ArrayList<>();
//        for (CRPdfData pdf: list) {
//            if (pdf.isJailedNow()) {
//                appearJailedNow.add(pdf);
//            }
//        }
//
//        //List<CRPdfData>
//
//        /**
//         * pdfs showing current jailed
//         *
//         * what if a person is jailed pre-trial for one case, but sentenced for another case
//         *   so...
//         *   which pdfs appear to show pre-trial status
//         *   which people are in that list
//         *   are there any pdfs that show sentenced
//         */
//    }

    /**
     * Not entirely accurate, as # dockets missing from end of year per MDJ can't
     * be identified
     * @param list
     */
    public static void percentageOfMissingDockets(List<CRPdfData> list) {
        //group by courtofficeyear
        //sort each group by docket number
        //loop through all, tracking nextExpected number
          //record each miss of next expected number
        TreeMap<String, Set<CRPdfData>> groups = new TreeMap<>();
        for (CRPdfData pdf: list) {
            String key = pdf.getCourtOffice() + pdf.getFileYear();
            if (!groups.containsKey(key)) {
                groups.put(key, new TreeSet<>(new Comparator<CRPdfData>() {
                    @Override
                    public int compare(CRPdfData o1, CRPdfData o2) {
                        return o1.getDocketNumberAsString().compareTo(o2.getDocketNumberAsString());
                    }
                }));
            }
            groups.get(key).add(pdf);
        }

        int totalMisses = 0;
        int totalPossible = 0;
        for (String groupKey: groups.keySet()) {
            int expectedNext = 1;
            int misses = 0;
            int last = 1;
            for (CRPdfData pdf: groups.get(groupKey)) {
                while (pdf.getDocketNumberAsInt() != expectedNext) {
                    misses++;
                    expectedNext++;
                }
                expectedNext++;
                last = pdf.getDocketNumberAsInt();
            }
            //System.out.println(misses + "/" + last + " misses for " + groupKey);
            totalMisses += misses;
            totalPossible += last;
        }
        System.out.println(LTAnalysis.getPercentage(totalMisses, totalPossible, true)
                        + " total misses ");
    }

    private static void uniqueOTN(List<CRPdfData> list) {
        Set<String> uniqueOTNs = new HashSet<>();
        for (CRPdfData pdf: list) {
            uniqueOTNs.addAll(pdf.getOTNs());
        }
        System.out.println(uniqueOTNs.size() + " unique OTNs out of " + list.size() + " dockets");
    }

    private static Set<Person> uniquePeople(List<CRPdfData> list) {
        Set<Person> ret = new HashSet<>();
        for (CRPdfData pdf: list) {
            ret.add(pdf.getPerson());
        }
        return ret;
    }

    private static List<Person> unableAndCurrentlyJailedInLancaster(List<CRPdfData> list) throws IOException, InterruptedException {
        List<CRPdfData> unable = unable(list);
        Set<Person> uniquePeople = uniquePeople(unable);
        System.out.println(uniquePeople.size() + " unique people unable to pay bail");

        int i = 0;
        List<Person> ret = new ArrayList<>();
        for (Person person: uniquePeople) {
            i++;
            LocalDate jailedDate = Scraper.getDateOfIncarcerationIfCurrentlyJailedInLancaster(person);
            if (jailedDate != null) {
                ret.add(person);
                System.out.println(ret.size() + " of " + i + " " + person + " jailed " + jailedDate);
            }
        }

        return ret;
    }

    private static List<CRPdfData> unable(List<CRPdfData> list) throws IOException {
        List<CRPdfData> unable = new LinkedList<>();
        for (CRPdfData pdf: list) {
            //System.out.println(pdf.getDefendantName());
            if (pdf.isUnableToPostBail()) {
                unable.add(pdf);
            }
        }

//        int bailThreshhold = 5000;
//        int belowThreshholdCount = 0;
//        List<CRPdfData> belowPdfs = new LinkedList<>();

        //for (CRPdfData pdf: unable) {
            //System.out.println("Unable to pay bail: $" + pdf.getBail());
//            if (pdf.getBail() <= bailThreshhold) {
//                belowThreshholdCount++;
//                belowPdfs.add(pdf);
//            }
        //}

        //System.out.println(unable.size() + " of " + list.size() + " pdfs show jailing due to unable to pay bail");
        //System.out.println(belowThreshholdCount + " @ $" + bailThreshhold + " or less");

//        for (CRPdfData pdf: belowPdfs) {
//            System.out.println(pdf.getStoredURL());
//        }

        return unable;
    }

    public static void forfeitureOTNs(List<CRPdfData> list) throws InterruptedException, NoSuchFieldException, IOException, ClassNotFoundException {
        Set<String> otns = new HashSet<>();
        for (CRPdfData pdf: list) {
            if (pdf.isForfeiture()) {
                otns.addAll(pdf.getOTNs());
            }
        }
        List<String> sList = new ArrayList<>();
        sList.addAll(otns);

        List<String> dockets = Scraper.scrapeOTNListForDocketNames(sList, false);

        Scraper.scrapeDockets(dockets);

        List<PdfData> pdfs = ParseAll.parseFromDockets(dockets, "Lancaster");

        for (PdfData pdf: pdfs) {
            if (pdf instanceof CPPdfData) {
                CPPdfData cp = (CPPdfData) pdf;
                if (cp.isLancasterBailFund()) {
                    System.out.println("Lancaster Bail Fund: " + cp.getDocket());
                }
            }
        }
    }
}
