package com.lancasterstandsup.evictiondata;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CRAnalysis {

    public static void main (String[] args) throws IOException, ClassNotFoundException {
        String[] years = {"2021"};
        List<CRPdfData> list = ParseAll.get(Scraper.Mode.MDJ_CR, "Lancaster", years);

        //unable(list);
        uniqueOTN(list);
    }

    private static void uniqueOTN(List<CRPdfData> list) {
        Set<String> uniqueOTNs = new HashSet<>();
        for (CRPdfData pdf: list) {
            uniqueOTNs.add(pdf.getOTN());
        }
        System.out.println(uniqueOTNs.size() + " unique OTNs out of " + list.size() + " dockets");
    }

    private static void unable(List<CRPdfData> list) throws IOException {
        List<CRPdfData> unable = new LinkedList<>();
        for (CRPdfData pdf: list) {
            System.out.println(pdf.getDefendantName());
            if (pdf.isUnableToPostBail()) {
                unable.add(pdf);
            }
        }

        int bailThreshhold = 5000;
        int belowThreshholdCount = 0;
        List<CRPdfData> belowPdfs = new LinkedList<>();

        for (CRPdfData pdf: unable) {
            System.out.println("Unable to pay bail: $" + pdf.getBail());
            if (pdf.getBail() <= bailThreshhold) {
                belowThreshholdCount++;
                belowPdfs.add(pdf);
            }
        }

        System.out.println(unable.size() + " of " + list.size() + " pdfs show jailing due to unable to pay bail");
        System.out.println(belowThreshholdCount + " @ $" + bailThreshhold + " or less");

        for (CRPdfData pdf: belowPdfs) {
            System.out.println(pdf.getStoredURL());
        }
    }
}
