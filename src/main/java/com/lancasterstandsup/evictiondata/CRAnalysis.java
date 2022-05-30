package com.lancasterstandsup.evictiondata;

import javax.print.attribute.standard.PDLOverrideSupported;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

public class CRAnalysis {

    public static void main (String[] args) throws IOException, ClassNotFoundException, InterruptedException, NoSuchFieldException {
        String[] years = {"2022"};
        List<CRPdfData> list = ParseAll.get(Scraper.CourtMode.MDJ_CR, "Lancaster", years);

        //estimateHidden(list);
        //estimateUniqueCases(list);
        m3OrLower(list);

        //unable(list);
        //uniqueOTN(list);
        //unableAndCurrentlyJailedInLancaster(list);

        //pretrial(list, LocalDate.now());

        //bailFundOTNs(list);

        //forfeitureOTNs(list);

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

    private static <T extends PdfData> int estimateUniqueCases(List<T> list) {
        Map<String, Set<T>> doubles = getDocketsByOTN(list);
        Set<T> doNotCount = new TreeSet<>();
        for (Set<T> val: doubles.values()) {
            boolean first = true;
            for (T t: val) {
                if (!first) {
                    doNotCount.add(t);
                }
                first = false;
            }
        }

        int ret = list.size() - doNotCount.size();

        System.out.println("unique otns: " + getOTNs(list).size());
        System.out.println("dockets with more than one otn: " + getDocketsWithMoreThanOneOTN(list).size());
        System.out.println("estimated count of unique cases: " + ret);
        return ret;
    }

    public static <T extends PdfData> List<T> getDocketsWithMoreThanOneOTN(List<T> list) {
        List<T> ret = new ArrayList<>();
        for (T t: list) {
            if (t.getOTNs().size() > 1) {
                ret.add(t);
            }
        }
        return ret;
    }

    //raw missing
    //but some of the missing are two per otn
    //so raw missing - percentThatAreDoubles(rawMissing)
    //or rather: percentThanAren'tDoubles(rawMissing)
    //then you have predictions of missing at end of run... not caluclate for now
    public static <T extends PdfData> int estimateHidden(List<T> list) {
        //year --> judge --> numbers
        Map<String, Map<String, Set<String>>> map = new HashMap<>();
        for (T t: list) {
            String docket = t.getDocket();
            int i = docket.length() - 4;
            String year = docket.substring(i);
            docket = docket.substring(0, i -1);
            i = docket.lastIndexOf('-');
            String number = docket.substring(i + 1);
            String judge = docket.substring(0 , i);

            if (!map.containsKey(year)) {
                map.put(year, new HashMap<>());
            }
            Map<String, Set<String>> judges = map.get(year);
            if (!judges.containsKey(judge)) {
                judges.put(judge, new TreeSet<>());
            }
            judges.get(judge).add(number);
        }

        int ret = 0;
        for (Map<String, Set<String>> judges: map.values()) {
            for (Set<String> judge: judges.values()) {
                int expected = 1;
                int totalMisses = 0;
                for (String number: judge) {
                    int num = Integer.parseInt(number);
                    int misses = 0;
                    while (num != expected) {
                        expected++;
                        misses++;
                    }
                    expected++;
                    totalMisses += misses;
                }
                ret += totalMisses;
            }
        }

        Map<String, Set<T>> doubles = getDocketsByOTN(list);

//        int uniqueSize = list.size() - doNotCount.size();
        double percentThatAreNotDoubles = 1.0 - ((double) doubles.size()) / list.size();

        //System.out.println(ret);
        ret = (int) (percentThatAreNotDoubles * ret);

        //System.out.println(ret);

        return ret;
    }

    public static <T extends PdfData> Set<String> getOTNs(List<T> list) {
        Set<String> ret = new HashSet<>();
        for (PdfData pdf: list) {
            ret.addAll(pdf.getOTNs());
        }
        return ret;
    }

    /**
     * how many misdeamenors M3 or lower (lower = ungraded?) have secured cash bail
     *
     * smaller issues:
     * how many dockets are missing?
     * how many dockets are linked to other dockets?
     *    so need to only record the set from the 'first' docket
     */
    public static void m3OrLower (List<CRPdfData> list) {
        int i = 0;
        int unspecified = 0;
        Set<PdfData> usedDockets = new HashSet<>();
        Map<String, Set<CRPdfData>> otnGroups = getDocketsByOTN(list);
        for (CRPdfData pdf: list) {
            boolean inc;
            if (pdf.isSecuredCashBail() &&
                    pdf.isMostSeriousGradeAtOrBelow(CRPdfData.GRADE.M3)) {
                if (pdf.hasGrade(CRPdfData.GRADE.NOT_SPECIFIED)) {
                    unspecified++;
                }
                else {
                    inc = true;
                    if (pdf.hasOTNs()) {
                        for (String otn : pdf.getOTNs()) {
                            Set<CRPdfData> dockets = otnGroups.get(otn);
                            if (dockets != null) {
                                for (CRPdfData member : dockets) {
                                    if (member.equals(pdf)) {
                                        break;
                                    } else if (member.isSecuredCashBail() &&
                                            !member.hasGrade(CRPdfData.GRADE.NOT_SPECIFIED) &&
                                            pdf.isMostSeriousGradeAtOrBelow(CRPdfData.GRADE.M3)) {
                                        inc = false;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (inc && !usedDockets.contains(pdf)) {
                        i++;
                        usedDockets.add(pdf);
                    }
                }
            }
        }
        System.out.println(i + " of estimated unique cases: " + estimateUniqueCases(list));
        System.out.println("Note skipped " + unspecified + " dockets with cash bail BUT at least one unspecified charge");
    }

    /**
     *
     * @param list
     * @return NO SINGLES!!!
     */
    public static <T extends PdfData> Map<String, Set<T>> getDocketsByOTN(List<T> list) {
        TreeMap<String, Set<T>> ret = new TreeMap<>();

        for (T pdf: list) {
            if (pdf.hasOTNs()) {
                for (String otn: pdf.getOTNs()) {
                    if (!ret.containsKey(otn)) {
                        ret.put(otn, new TreeSet<>(PdfData.DATE_COMPARATOR));
                    }
                    ret.get(otn).add(pdf);
                }
            }
        }

        TreeMap <String, Set<T>> noSingles = new TreeMap<>();
        for (String otn: ret.keySet()) {
            Set<T> set = ret.get(otn);
            if (set.size() > 1) {
                noSingles.put(otn, set);
            }
        }

        return noSingles;
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
