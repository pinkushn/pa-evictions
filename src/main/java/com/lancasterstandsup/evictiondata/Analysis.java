package com.lancasterstandsup.evictiondata;

import org.apache.commons.math3.util.Precision;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.*;

import static java.time.temporal.ChronoUnit.DAYS;

public class Analysis {

    private static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static DateTimeFormatter dateFormatterShortYearSlashes = DateTimeFormatter.ofPattern("MM/dd/yy");
    private static DateTimeFormatter dateFormatterShortYearUnderscores = DateTimeFormatter.ofPattern("MM/dd/yy");
    private static LocalDate covidStart = LocalDate.parse("03/15/2020", dateFormatter);
    private static LocalDate wolfMoratoriumEnds = LocalDate.parse("08/31/2020", dateFormatter);

    public static void main (String [] args) throws IOException, ClassNotFoundException {
        String county = "Lancaster";
        //String[] years = {"2019"};
        //String[] years = {"2020"};
        String[] years = {"2021"};
        //String[] years = {"2019", "2020"};
        //String[] years = {"2020", "2021"};
        //String[] years = {"2019", "2020", "2021"};
        //String[] years = {"2017", "2018", "2019", "2020"};
        //String[] years = {"2017", "2018", "2019", "2020", "2021"};

        List<PdfData> list = (List<PdfData>) ParseAll.get(county, years, false)[2];

        nowHappening(filterMDJ(list, "2309"));
        //nowHappening(filterMDJ(list, "2301"));
        //nowHappening(filterOutLancasterCity(list));
        //nowHappening(list);

        //mostFiled(filterPostWolfMoratoriumEnds(list), true, false);
        //monthly(list);
        //weekly(list);
        //daily(list);
        //mostFiled(list, false, true);
        //mergeCheck(list);
//        int defendantHasRep = countRepresentationForDefendants(list);
//        int plaintiffHasRep = countRepresentationForPlaintiffs(list);
//        System.out.println("defendant repped: " + defendantHasRep + " out of " + list.size());
//        System.out.println("plaintiff repped: " + plaintiffHasRep + " out of " + list.size());
        //impactOfRepresentation(list);
        //evictions(filterOutUnresolved(list));
        //plaintiffWinsByJudge(list);
        //evictionRateByJudge(list);
        //everyJudge(list);
        //judgmentForPlaintiff(list);
        //plaintiffWin(filterOutUnresolved(list));
        //grantWithoutJudgment(list);
        //covidEvictions(list);

        //there are two cases in Lancaster 2020 that had order for possession served
        //in November, 2020, but are still marked as active
        //orderForPossessionServed(notClosedOrInactive(list));

        //countNotClosedOrInactive(county);
        //getNotClosedOrInactive(county);
        //doActiveCasesHaveDecisions(county);

        //whichPlaintiffsHaveActiveCases(county);

        //*** Use frequently, may help identify court electronic update patterns ***
        //whichCourtsHaveCases(list);

        //judgeCovidProtocol(list);

        //projectedBacklog("Lancaster");

        //resolvedButNotJudged(list);

        //resolvedAndPlaintiffWinSplitAnalysis(list);

        //judgeHearingDays(list);

        //evictionsOrderedRecently(list);

        //antwi(list);

        //orgassPercentages(list);

        //mostFiled(ephrata(filterOutNonPandemic(list)), false, false);
        //mostFiled(ephrata(list), false, false);

        //preVersusPostPandemic("Lancaster");
    }

    public static LocalDate now = LocalDate.now();
    public static LocalDate march15_2020 = LocalDate.of(2020, 3, 15);
    public static LocalDate march14_2020 = LocalDate.of(2020, 3, 14);
    public static int pandemicDays = (int) DAYS.between(march15_2020, now);
    public static LocalDate preStart = march15_2020.minusDays(pandemicDays);

    public static String presentName = now.getMonthValue() + "_" + now.getDayOfMonth() + "_" + now.getYear();
    public static String preStartName = preStart.getMonthValue() + "_" + preStart.getDayOfMonth() + "_" + preStart.getYear();
    public static String preEndName = march14_2020.getMonthValue() + "_" + march14_2020.getDayOfMonth() + "_" + march14_2020.getYear();
    public static String pivotName = march15_2020.getMonthValue() + "_" + march15_2020.getDayOfMonth() + "_" + march15_2020.getYear();

    public static String dataPathWithoutDot = "webdata/";
    public static String dataPathWithDot = "./" + dataPathWithoutDot;

    private static String rootName = "lanco_eviction_cases";
    public static String allName = rootName + "_1_1_2015_to_" + presentName + ".xlsx";
    public static String postName = rootName + "_" + pivotName + "_to_" + presentName + ".xlsx";
    public static String preName = rootName + "_" + preStartName + "_to_" + preEndName + ".xlsx";

    /**
     *
     * open case or 'closed' but may result in eviction or order has been granted but may not have been filled
     *
     * @param list
     */
    public static void nowHappening(List<PdfData> list) {
        List<PdfData> ret = new ArrayList<>();
        int howMany = 0;
        for (PdfData pdf: list) {
            if (!pdf.isResolved()) ret.add(pdf);
            else {
                LocalDate dispositionDate = pdf.getDispositionDate();
                if (dispositionDate == null) {
                    ret.add(pdf);
                }
                else if (pdf.evictionWarning()) {
                    ret.add(pdf);
                    howMany++;
                    //MJ-02208-LT-0000027-2021
                    System.out.println("Eviction may be imminent for " + pdf.getDefendant() + " " + pdf.getDocketNumber());
                }
            }
        }
        System.out.println(howMany + " in danger of imminent eviction");
    }

    /**
     *
     * @param list
     * @param targetMDJ format for Lancaster MDJs is ####, ex: 2309
     * @return
     */
    public static List<PdfData> filterMDJ(List<PdfData> list, String targetMDJ) {
        List<PdfData> ret = new ArrayList<>();
        for (PdfData pdf: list) {
            if (pdf.getCourtOfficeNumberOnly().equals(targetMDJ)) {
                ret.add(pdf);
            }
        }
        return ret;
    }

    /**
     *
     * @param list
     * @param mdjs format for Lancaster MDJs is ####, ex: 2309
     * @return
     */
    public static List<PdfData> filterOutMDJs(List<PdfData> list, Set<String> mdjs) {
        List<PdfData> ret = new ArrayList<>();
        for (PdfData pdf: list) {
            if (!mdjs.contains(pdf.getCourtOfficeNumberOnly())) {
                ret.add(pdf);
            }
        }
        return ret;
    }

    public static List<PdfData> filterOutLancasterCity(List<PdfData> list) {
        Set<String> lanc = new HashSet<>();
        lanc.add("2101");
        lanc.add("2201");
        lanc.add("2202");
        lanc.add("2204");
        return filterOutMDJs(list, lanc);
    }

    public static List<PdfData> filterOutNonPandemic(List<PdfData> list) {
        List<PdfData> ret = new ArrayList<>();
        for (PdfData pdf: list) {
            if (pdf.getFileDate().compareTo(march15_2020) > -1) {
                ret.add(pdf);
            }
        }

        return ret;
    }
    /**PDFs must exist for as many days BEFORE pandemic as since pandemic
     *
     * @param county
     */
    public static void preVersusPostPandemic(String county) throws IOException, ClassNotFoundException {
        //how many days since 3/15/2020?
        //get all PDFs from 3/15/2020 minus days since then
        //split into two groups, pre and post
        //two maps string to count: court to count. ONe for pre, one post

        //result file as csv
        //Name has days since pandemic: PreVersusPost_X_Days.csv
        //headers: Court,Pre,Post,Ratio

        int startYear = preStart.getYear();
        int endYear = now.getYear();
        int len = endYear - startYear + 1;
        String [] years = new String[len];
        for (int x = 0; x < len; x++) {
            years[x] = (startYear + x) + "";
        }

        List<PdfData> list = (List<PdfData>) ParseAll.get(county, years, false)[2];

        Map<String, Integer> pre = new TreeMap<>();
        Map<String, Integer> post = new TreeMap<>();
        Map<String, Integer> courtToRatio = new TreeMap<>();
        Map<String, String> judges = new TreeMap<>();

        for (PdfData pdf: list) {
            Map<String, Integer> useMe;
            useMe = pdf.getFileDate().compareTo(march15_2020) < 0 ? pre : post;
            String court = pdf.getCourtOfficeWithoutMDJ();
            if (!useMe.containsKey(court)) {
                useMe.put(court, 0);
            }
            useMe.put(court, useMe.get(court) + 1);
            judges.put(court, pdf.getJudgeName());
        }

        Map<Double, List<String>> ratios = new TreeMap<>();
        for (String s: pre.keySet()) {
            int a = pre.get(s);
            int b = post.get(s);
            double ratio = Precision.round(((double) b) / a, 2);
            //System.out.println(s + " (" + judges.get(s) + ") pre: " + a + "   post: " + b + "    ratio: " + ratio);
            if (!ratios.containsKey(ratio)) {
                ratios.put(ratio, new ArrayList<>());
            }
            ratios.get(ratio).add(s);
        }

        for (Double d: ratios.keySet()) {
            long l = Math.round(d*100);
            for (String court: ratios.get(d)) {
                //System.out.println(l + "% " + court + " (" + judges.get(court) + ")  pre: " + pre.get(court) + "  post: " + post.get(court));
                courtToRatio.put(court, (int) l);
            }
        }

        //Map to lookup data per court
        String fileName = county.toLowerCase() + "_pre_versus_post.js";
        String filePath = dataPathWithDot + fileName;
        PrintWriter out = new PrintWriter(new FileWriter(filePath));
        out.println("let courtData = new Map([");
        for (String court: pre.keySet()) {
            out.println("\t['" + court + "', " +
                    "{ratio: " + courtToRatio.get(court) + ", " +
                    "pre: " + pre.get(court) + ", " +
                    "post: " + post.get(court) + "}" +
                    "],");
        }

        out.println("]);");
        out.println();
        
        out.println("let pandemicDates = {");

        out.println("\tpandemicDays:" + pandemicDays + ",");
        out.println("\tprePandemicStartSlashes:'" + preStart.format(dateFormatterShortYearSlashes) + "',");
        out.println("\tprePandemicEndSlashes:'3/14/20',");
        out.println("\tpandemicStartSlashes:'3/15/20',");
        out.println("\tpandemicEndSlashes:'" + now.format(dateFormatterShortYearSlashes) + "',");

        out.println("\tallName:'" + allName + "',");
        out.println("\tpostName:'" + postName + "',");
        out.println("\tpreName:'" + preName + "',");
        out.println("}");

        out.flush();
        out.close();
    }

    public static List<PdfData> ephrata(List<PdfData> list) {
        List<PdfData> eph = new LinkedList();
        String target = "17522";
        for (PdfData pdf: list) {
            if (pdf.defendantZip(target)) {
                eph.add(pdf);
            }
        }
        return eph;
    }

//    public static void evictionsOrderedRecently(List<PdfData> list) {
//        for (PdfData pdf: list) {
//            if (pdf.isOrderForPossessionServed()) {
//                if ()
//            }
//        }
//    }

    public static void orgassPercentages(List<PdfData> list) {
        plaintiffWin(list);
        evictions(list);

        impactOfRepresentation(list);

        List<PdfData> james = new LinkedList<>();
        for (PdfData pdf: list) {
            String notes = pdf.getNotes();
            if (notes != null && notes.toLowerCase().indexOf("orgass") > -1) {
                james.add(pdf);
            }
        }

        plaintiffWin(james);
        evictions(james);
    }

    public static void monthly(List<PdfData> data) {
        Map<LocalDate, Integer> map = new TreeMap<>();
        for (PdfData d: data) {
            LocalDate date = d.getFileDate();
            LocalDate month = LocalDate.of(date.getYear(), date.getMonth(), 1);
            if (!map.containsKey(month)) map.put(month, 0);
            map.put(month, map.get(month) + 1);
        }
        for (LocalDate month: map.keySet()) {
            System.out.println(month.getMonth() + " " + month.getYear() + ": " + map.get(month));
        }
    }

    public static void weekly(List<PdfData> data) {
        Map<LocalDate, Integer> map = new TreeMap<>();
        TemporalField woy = WeekFields.of(Locale.US).weekOfWeekBasedYear();
        TemporalField hlep = WeekFields.of(Locale.US).dayOfWeek();
        for (PdfData d: data) {
            LocalDate date = d.getFileDate();
            LocalDate key = date.with(hlep, 1L);
            if (!map.containsKey(key)) map.put(key, 0);
            map.put(key, map.get(key) + 1);
        }
        for (LocalDate key: map.keySet()) {
            System.out.println(key + "\t" + map.get(key));
        }
    }

    public static void daily(List<PdfData> data) {
        Map<LocalDate, Integer> map = new TreeMap<>();
        for (PdfData d: data) {
            LocalDate key = d.getFileDate();
            if (!map.containsKey(key)) map.put(key, 0);
            map.put(key, map.get(key) + 1);
        }
        for (LocalDate key: map.keySet()) {
            System.out.println(key + "\t" + map.get(key));
        }
    }

    public static void mostFiled(List<PdfData> data, boolean hidePercentageWins, boolean filterOutUnresolved) {
        Map<String, List<PdfData>> grouped = groupByPlaintiff(data);

//        TreeMap<String, Integer> map = new TreeMap<>();
//        for (PdfData pdf: data) {
//            String plaintiff = pdf.getPlaintiff();
//            map.put(plaintiff, map.getOrDefault(plaintiff, 0) + 1);
//        }
//

        if (filterOutUnresolved) {
            Map<String, List<PdfData>> temp = new TreeMap<>();
            for (String groupId: grouped.keySet()) {
                temp.put(groupId, filterOutUnresolved(grouped.get(groupId)));
            }
            grouped = temp;
        }

        Map<String, Double> winPercentage = new TreeMap<>();
        for (String c: grouped.keySet()) {
            double wins = 0;
            for (PdfData pdf: grouped.get(c)) {
                wins += pdf.isPlaintiffWin() ? 1 : 0;
            }
            double per = 100 * ((double) wins) / grouped.get(c).size();
            winPercentage.put(c, per);
        }

        TreeMap<Integer, Set<String>> highs = new TreeMap<>();
        for (String c: grouped.keySet()) {
            Integer count = grouped.get(c).size();
            if (!highs.containsKey(count)) {
                highs.put(count, new TreeSet<>());
            }
            highs.get(count).add(c);
        }

        for (Integer i: highs.keySet()) {
            if (i >= 2) {
                Set<String> set = highs.get(i);
                for (String c: set) {
                    String per = " " + Math.round(winPercentage.get(c)) + "% wins";
                    if (hidePercentageWins) per = "";
                    System.out.println(i + " cases" + per + ": " + grouped.get(c).get(0).getPlaintiff());
                }
            }
        }
    }

    //plaintiff 'core' to Pdfs
    private static Map<String, List<PdfData>> groupByPlaintiff(List<PdfData> list) {
        Map<String, List<PdfData>> map = new TreeMap<>();
        for (PdfData pdf: list) {
            String core = plaintiffCore(pdf.getPlaintiff());
            if (!map.containsKey(core)) {
                map.put(core, new ArrayList<>());
            }
            //if (pdf.getFileDate().compareTo(wolfMoratoriumEnds) > 0) {
                map.get(core).add(pdf);
            //}
        }

        return map;
    }

    private static void mergeCheck(List<PdfData> list) {
        Map<String, Set<String>> cores = new TreeMap<>();
        for (PdfData pdf: list) {
            String p = pdf.getPlaintiff();
            String core = plaintiffCore(p);
            if (!cores.containsKey(core)) {
                cores.put(core, new TreeSet<>());
            }
            cores.get(core).add(p);
        }

        for (String c: cores.keySet()) {
            Set<String> set = cores.get(c);
            if (set.size() > 1) {
                for (String s: set) {
                    System.out.println(c + " to " + s);
                }
                System.out.println();
            }
        }
    }

    //************
    //WARNING
    //This yields a few fake matches
    //************
    private static String plaintiffCore(String full) {
        full = full.toLowerCase().replace(" ", "").replace(",", "").replace(".", "");
        return full.substring(0, Math.min(10, full.length()));
    }

    private static int countRepresentationForDefendants(List<PdfData> data) {
        int ret = 0;
        for (PdfData pdf: data) {
            ret += pdf.isDefendantAttorney() ? 1 : 0;
        }
        return ret;
    }

    private static int countRepresentationForPlaintiffs(List<PdfData> data) {
        int ret = 0;
        for (PdfData pdf: data) {
            ret += pdf.isPlaintiffAttorney() ? 1 : 0;
        }
        return ret;
    }

    private static void impactOfRepresentation(List<PdfData> list) {
        int casesDefendantRep = 0;
        int pWinDefendantRep = 0;
        int casesPRepButNoDefendantRep = 0;
        int pWinPRepNoDefendantRep = 0;
        int casesNoRep = 0;
        int pWinNoRep = 0;
        String target = "grant/order";
        //String target = "eviction";
        int defendantRepped = 0;
        for (PdfData pdf: list) {
            boolean pWin = pdf.isGrantPossessionOrOrderForEvictionServed();
            //boolean pWin = pdf.isEviction();
            if (pdf.isDefendantAttorney()) {
                casesDefendantRep++;
                pWinDefendantRep += pWin ? 1 : 0;
                defendantRepped++;
            }
            else if (pdf.isPlaintiffAttorney()) {
                casesPRepButNoDefendantRep++;
                pWinPRepNoDefendantRep += pWin ? 1 : 0;
            }
            else {
                casesNoRep++;
                pWinNoRep += pWin ? 1 : 0;
            }
        }
        double per = 100 * ((double) pWinDefendantRep) / casesDefendantRep;
        System.out.println("Defendant repped, but plaintiff gets " + target + ": " + per +
                "% (" + pWinDefendantRep + " of " + casesDefendantRep + ")");
        per = 100 * ((double) pWinPRepNoDefendantRep) / casesPRepButNoDefendantRep;
        System.out.println("No defense rep, but plaintiff rep, and plaintiff gets " + target + ": " + per +
                "% (" + pWinPRepNoDefendantRep + " of " + casesPRepButNoDefendantRep + ")");
        per = 100 * ((double) pWinNoRep) / casesNoRep;
        System.out.println("No rep either party, plaintiff gets " + target + ": " + per +
                "% (" + pWinNoRep + " of " + casesNoRep + ")");
        per = 100 * ((double) defendantRepped) / list.size();
        System.out.println("Defendant repped: " + per +
                "% (" + defendantRepped + " of " + list.size() + ")");
    }

//    private static void covidEvictions(List<PdfData> list) {
//        List<PdfData> onlyCovid = filterCovid(list);
//        List<PdfData> closedOrInactive = filterClosedOrInactive(onlyCovid);
//        String per = evictions(closedOrInactive);
//        System.out.println("Evictions from closed/inactive cases filed after 3/15/20: " + per);
//    }

    //USE filterOutUnresolved
//    /**
//     * returns all cases closed or inactive
//     */
//    private static List<PdfData> filterClosedOrInactive(List<PdfData> list) {
//        List<PdfData> ret = new LinkedList<>();
//        for (PdfData pdf: list) {
//            if (pdf.isClosed() || pdf.isInactive()) {
//                ret.add(pdf);
//            }
//        }
//        return ret;
//    }

    /**
     * returns all cases filed 3/15/20 or later
     */
    private static List<PdfData> filterCovid(List<PdfData> list) {
        List<PdfData> ret = new LinkedList<>();
        for (PdfData pdf: list) {
            LocalDate ld = pdf.getFileDate();
            if (ld.compareTo(covidStart) > -1) {
                ret.add(pdf);
            }
        }
        return ret;
    }

    /**
     * returns all cases filed 9/1/20 or later
     */
    private static List<PdfData> filterPostWolfMoratoriumEnds(List<PdfData> list) {
        List<PdfData> ret = new LinkedList<>();
        for (PdfData pdf: list) {
            LocalDate ld = pdf.getFileDate();
            if (ld.compareTo(wolfMoratoriumEnds) > -1) {
                ret.add(pdf);
            }
        }
        return ret;
    }

    private static List<PdfData> covidCases(String county) throws IOException, ClassNotFoundException {
        String[] years = {"2020", "2021"};
        List<PdfData> list = (List<PdfData>) ParseAll.get(county, years, false)[2];
        return filterCovid(list);
    }

    private static String evictions(List<PdfData> list) {
        int ret = 0;
        for(PdfData pdf: list) {
            if (pdf.isEviction()) {
                ret++;
            }
        }

        String retS = getPer(ret, list.size(), true);
        System.out.println("Evictions: " + retS);
        return retS;
    }

    private static String plaintiffWins(List<PdfData> list) {
        int ret = 0;
        for(PdfData pdf: list) {
            if (pdf.isPlaintiffWin()) {
                ret++;
            }
        }

//        double per = Math.round(100 * ((double) ret)/list.size());
//        return per + "%";

        return getPer(ret, list.size(), true);
    }

    public static Map<String, List<PdfData>> groupByJudge(List<PdfData> list) {
        Map<String, List<PdfData>> ret = new TreeMap<>();
        for (PdfData pdf: list) {
            if (!ret.containsKey(pdf.getJudgeName())) {
                ret.put(pdf.getJudgeName(), new LinkedList<>());
            }
            ret.get(pdf.getJudgeName()).add(pdf);
        }
        return ret;
    }

    public static Map<String, List<PdfData>> groupByCourtOffice(List<PdfData> list) {
        Map<String, List<PdfData>> ret = new TreeMap<>();
        for (PdfData pdf: list) {
            if (!ret.containsKey(pdf.getCourtOfficeNumberOnly())) {
                ret.put(pdf.getCourtOfficeNumberOnly(), new LinkedList<>());
            }
            ret.get(pdf.getCourtOfficeNumberOnly()).add(pdf);
        }
        return ret;
    }

    private static void evictionRateByJudge(List<PdfData> list) {
        Map<String, List<PdfData>> byJudge = groupByJudge(list);

        //judge name --> eviction percentage
        Map<String, String> results = new TreeMap<>();for (String j: byJudge.keySet()) {
            results.put(j, evictions(filterOutUnresolved(byJudge.get(j))));
        }

        //percentage String --> List of judges
        TreeMap<String, List<String>> ordered = new TreeMap<>();

        for (String j: results.keySet()) {
            String per = results.get(j);
            if (!ordered.containsKey(per)) {
                ordered.put(per, new LinkedList<>());
            }
            ordered.get(per).add(j);
        }

        for (String per: ordered.keySet()) {
            for (String j: ordered.get(per)) {
                System.out.println(per + ": " + j);
            }
        }
    }

    private static void plaintiffWinsByJudge(List<PdfData> list) {
        Map<String, List<PdfData>> byJudge = groupByJudge(list);

        //judge name --> plaintiff win percentage
        Map<String, String> results = new TreeMap<>();

        for (String j: byJudge.keySet()) {
            results.put(j, plaintiffWins(filterOutUnresolved(byJudge.get(j))));
        }

        //percentage String --> List of judges
        TreeMap<String, List<String>> ordered = new TreeMap<>();

        for (String j: results.keySet()) {
            String per = results.get(j);
            if (!ordered.containsKey(per)) {
                ordered.put(per, new LinkedList<>());
            }
            ordered.get(per).add(j);
        }

        for (String per: ordered.keySet()) {
            for (String j: ordered.get(per)) {
                System.out.println(per + ": " + j);
            }
        }
    }

    private static void everyJudge(List<PdfData> list) {
        Set<String> used = new HashSet<>();
        for (PdfData pdf: list) {
            if (!used.contains(pdf.getJudgeName())) {
                System.out.println(pdf.getJudgeName());
                used.add(pdf.getJudgeName());
            }
        }
    }

    /**
     * ignores unresolved cases
     * @param list
     */
    private static void plaintiffWin(List<PdfData> list) {
        list = filterOutUnresolved(list);
        int forP = 0;
        for (PdfData pdf: list) {
            forP += pdf.isPlaintiffWin() ? 1 : 0;
        }

        System.out.println("plaintiff win: " +
                getPer(forP, list.size(), true));
    }

    private static void judgmentForPlaintiff(List<PdfData> list) {
        int forP = 0;
        int base = 0;
        for (PdfData pdf: list) {
            forP += pdf.isJudgmentForPlaintiff() ? 1 : 0;
//            if (pdf.isJudgmentForPlaintiff()) {
//                if (!pdf.isGrantPossession() && !pdf.isGrantPossessionIf()) {
//                    System.out.println(pdf.getDocketNumber());
//                }
//            }
            if (pdf.isClosed() || pdf.isInactive() || pdf.isJudgmentForPlaintiff() || pdf.isJudgmentForDefendant()) {
                base++;
            }
        }

        System.out.println("Judgment for Plaintiff: " +
                getPer(forP, base, true));
    }

    private static void grantWithoutJudgment(List<PdfData> list) {
        for (PdfData pdf: list) {
            if (!pdf.isJudgmentForPlaintiff() && pdf.isGrantPossessionOrOrderForEvictionServed()) {
                System.out.println(pdf.getDocketNumber());
            }
        }
    }

    public static List<PdfData> filterByClosedOrInactive(List<PdfData> list) {
        List<PdfData> ret = new LinkedList<>();
        for (PdfData pdf: list) {
            if (!pdf.isClosed() && !pdf.isInactive()) {
                ret.add(pdf);
            }
        }
        return ret;
    }

    private static List<PdfData> filterOutUnresolved(List<PdfData> list) {
        List<PdfData> ret = new LinkedList<>();
        for (PdfData pdf: list) {
            if (pdf.isResolved()) {
                ret.add(pdf);
            }
        }
        return ret;
    }

    public static List<PdfData> orderForPossessionServed(List<PdfData> list) {
        List<PdfData> ret = new LinkedList<>();
        for (PdfData pdf: list) {
            if (pdf.isOrderForPossessionServed()) {
                ret.add(pdf);
                System.out.println("order for possession served: " + pdf.getDocketNumber());
            }
            else if (pdf.isGrantPossession()) {
                System.out.println("grant possession: " + pdf.getDocketNumber());
            }
            else if (pdf.isJudgmentForPlaintiff()) {
                System.out.println("judgment for plaintiff: " + pdf.getDocketNumber());
            }
        }
        return ret;
    }

    public static List<PdfData> orderByDocket(List<PdfData> thisYear) {
        TreeSet<PdfData> set = new TreeSet<>(new DocketComparator());
        set.addAll(thisYear);
        List<PdfData> ret = new LinkedList<>();
        ret.addAll(set);
        return ret;
    }

    private static class DocketComparator implements Comparator<PdfData> {

        @Override
        public int compare(PdfData a, PdfData b) {
            return a.getDocketNumber().compareTo(b.getDocketNumber());
        }
    }

    private static void countNotClosedOrInactive(String county) throws IOException, ClassNotFoundException {
        List<PdfData> active = getNotClosedOrInactive(county);

        System.out.println(active.size() + " not closed/inactive");
    }

    private static List<PdfData> getNotClosedOrInactive(String county) throws IOException, ClassNotFoundException {
        LocalDateTime now = LocalDateTime.now();
        int y = now.getYear();
        String year = "" + y;
        String lastYear = "" + (y - 1);

        List<PdfData> ret = ParseAll.parseAll(county, year);
        ret.addAll(ParseAll.parseAll(county, lastYear));
        ret = filterByClosedOrInactive(ret);

        return ret;
    }

    private static void doActiveCasesHaveDecisions(String county) throws IOException, ClassNotFoundException {
        List<PdfData> list = getNotClosedOrInactive(county);
        for (PdfData pdf: list) {
            if (pdf.isJudgmentForPlaintiff()) {
                System.out.println("Found an 'active' status with 'Judgment for Plaintiff': " + pdf.getDocketNumber());
            }
        }
    }

    private static void whichPlaintiffsHaveActiveCases(String county) throws IOException, ClassNotFoundException {
        List<PdfData> list = getNotClosedOrInactive(county);
        mostFiled(list, true, false);
    }

    private static void whichCourtsHaveCases(List<PdfData> list) {
        Map<String, List<PdfData>> byCourt = groupByCourtOffice(list);
        for (String s: byCourt.keySet()) {
            List<PdfData> court = byCourt.get(s);
            System.out.println(s + " most recent filing " + court.get(court.size() - 1).getFileDate() +
                    "  earliest: " + court.get(0).getFileDate());
        }
    }

    private static void judgeCovidProtocol(List<PdfData> data) {
        String total = ratioPostVersusPreCovid(data);
        System.out.println("Total: " + total);

        Map<String, List<PdfData>> byJudge = groupByJudge(data);
        TreeMap<String, List<String>> ordered = new TreeMap<>();
        for (String j: byJudge.keySet()) {
            String per = ratioPostVersusPreCovid(byJudge.get(j));
            if (!ordered.containsKey(per)) {
                ordered.put(per, new LinkedList<>());
            }
            ordered.get(per).add(j);
        }
        for (String p: ordered.keySet()) {
            for (String j: ordered.get(p)) {
                System.out.println(j + ": " + p);
            }
        }
    }

    /**
     * pre: 8/31/19 to 12/31/19
     * post: 8/31/20 to 12/31/20
     */
    private static final LocalDate preStarter = LocalDate.parse("09/01/2019", dateFormatter);
    private static final LocalDate preEnd = LocalDate.parse("12/31/2019", dateFormatter);
    private static final LocalDate postStart = LocalDate.parse("09/01/2020", dateFormatter);
    private static final LocalDate postEnd = LocalDate.parse("12/31/2020", dateFormatter);

    private static String ratioPostVersusPreCovid(List<PdfData> data) {
        int pre = 0;
        int post = 0;
        for (PdfData pdf: data) {
            LocalDate ld = pdf.getFileDate();
            if (ld.compareTo(preStarter) > -1 && ld.compareTo(preEnd) < 1) {
                pre++;
            }
            else if (ld.compareTo(postStart) > -1 && ld.compareTo(postEnd) < 1) {
                post++;
            }
        }
        return getPer(post, pre, false) + " (pre: " + pre + " v. during: " + post + ")";
    }

    private static String getPer(int a, int b, boolean showRaw) {
        double per = Math.round(100 * ((double) a/b));
        String ret = per + "";
        ret = ret.substring(0, ret.indexOf('.')) + "%";
        if (showRaw) {
            ret += " (" + a + " of " + b + ")";
        }
        return ret;
    }

//(2019 cases)*(years of moratoriums, calculated 3/15/20 to present) - (cases filed, 3/15/20 to present)
    private static void projectedBacklog(String county) throws IOException, ClassNotFoundException {
        int oneYear = ParseAll.parseAll(county, "2019").size();
        double covid = percentYearSinceCovid();
        int filedSince = covidCases(county).size();
        int backlog = (int) (oneYear * covid - filedSince);
        System.out.println("Minimal backlog: " + backlog);
    }

    private static double percentYearSinceCovid() {
        LocalDateTime now = LocalDateTime.now();
        //double days = (double) DAYS.between(now, covidStart);
        double days = covidStart.until(now, DAYS);
        return days/365;
    }

    private static void judgeHearingDays(List<PdfData> list) {
        Map<String, List<PdfData>> judges = groupByJudge(list);
        for (String j: judges.keySet()) {
            List<PdfData> jl = judges.get(j);
            Map<DayOfWeek, TreeMap<LocalDate, Integer>> days = whatDaysAreScheduledHearings(jl);
            System.out.println("**** " + j);
            for (DayOfWeek dow: days.keySet()) {
                TreeMap<LocalDate, Integer> map = days.get(dow);
                System.out.print(dow);
                for (Integer i: map.values()) {
                    System.out.print(" " + i);
                }
                System.out.println();
            }
        }
    }

    private static Map<DayOfWeek, TreeMap<LocalDate, Integer>> whatDaysAreScheduledHearings(List<PdfData> list) {
        Map<DayOfWeek, TreeMap<LocalDate, Integer>> ret = new TreeMap<>();
        for (PdfData pdf: list) {
            if (pdf.hasHearingDate()) {
                LocalDate ld = pdf.getHearingDate();
                DayOfWeek dayOfWeek = ld.getDayOfWeek();
                if (!ret.containsKey(dayOfWeek)) {
                    ret.put(dayOfWeek, new TreeMap<>());
                }
                TreeMap<LocalDate, Integer> map = ret.get(dayOfWeek);
                if (!map.containsKey(ld)) {
                    map.put(ld, 0);
                }
                map.put(ld, map.get(ld) + 1);
            }
        }

        return ret;
    }

    //how many 'resolved' cases are neither judgment for p or d?
    private static void resolvedButNotJudged(List<PdfData> data) {
        int not = 0;
        for (PdfData pdf: data) {
            if (pdf.isResolved()) {
                if (!pdf.isPlaintiffWin()) {
                    if (!pdf.isJudgmentForDefendant()) {
                        not++;
                    }
                }
            }
        }
        System.out.println(getPer(not, data.size(), true));
    }

    private static void resolvedAndPlaintiffWinSplitAnalysis(List<PdfData> list) {
        int grantAlone = 0;
        int judgeAlone = 0;
        int judgmentForD = 0;
        int win = 0;
        for (PdfData pdf: list) {
            if (pdf.isResolved()) {
                boolean judgmentForP = pdf.isJudgmentForPlaintiff();
                boolean grantEither = pdf.isEitherGrant();
                boolean eviction = pdf.isOrderForPossessionServed();
                if (judgmentForP || grantEither || eviction) {
                    win++;
                    if (judgmentForP && !(grantEither || eviction)) judgeAlone++;
                    if (grantEither && !(judgmentForP || eviction)) {
                        grantAlone++;
                        System.out.println(pdf.getDocketNumber());
                        judgmentForD += pdf.isJudgmentForDefendant() ? 1 : 0;
                    }
                }
            }
        }
        System.out.println("Judgment alone: " + judgeAlone + " of " + win + " wins");
        System.out.println("Grant alone: " + grantAlone + " of " + win + " wins");
        System.out.println("grantEither but 'judgment for tenant': " + judgmentForD);
    }

    /**
     * RETIRED 6/25/21 cuz 'resolved' may have occurred but eviction hasn't happened yet
    private static void evictionWarning(List<PdfData> data) {
        List<PdfData> danger = new LinkedList<>();
        for (PdfData pdf: data) {
            if (pdf.isEvictionWarning()) {
                danger.add(pdf);
                System.out.println(pdf);
            }
        }
    }
     **/

    private static void antwi(List<PdfData> data) {
        List<PdfData> list = new LinkedList<>();
        for (PdfData pdf: data) {
            if (pdf.getDefendant().indexOf("Dixon, Antwi") > -1) {
                list.add(pdf);
            }
        }
        for (PdfData pdf: list) {
            System.out.println("Antwi: " + pdf.isPlaintiffWin());
        }
    }
}
