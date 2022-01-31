package com.lancasterstandsup.evictiondata;

import java.io.IOException;
import java.util.TreeSet;

public class Update {

    public static void main (String [] args) {
        TreeSet<String> countiesWithData = new TreeSet<>();
        for (String county: Website.counties) {
            try {
                if (Scraper.getCountyStartAndEnd(county) != null) {
                    countiesWithData.add(county);
                }
            } catch (IOException e) {
                System.err.println("Failed to build update counties list, abandoning update");
                e.printStackTrace();
                return;
            } catch (ClassNotFoundException e) {
                System.err.println("Failed to build update counties list, abandoning update");
                e.printStackTrace();
                return;
            }
        }

        System.out.println("Counties with data: " + countiesWithData);

        int totalPdfs = 0;

        for (String county: countiesWithData) {
            try {
                totalPdfs += Worksheet.createExcel(county);
            } catch (IOException e) {
                System.err.println("Failed to create spreadsheet for " + county + ", abandoning update");
                e.printStackTrace();
                return;
            } catch (ClassNotFoundException e) {
                System.err.println("Failed to create spreadsheet for " + county + ", abandoning update");
                e.printStackTrace();
                return;
            }
        }

        try {
            Website.buildWebsite(countiesWithData);
        } catch (IOException e) {
            System.err.println("Failed to create site, abandoning update");
            e.printStackTrace();
            return;
        }

        System.out.println("\n*** Successfully updated " + totalPdfs + " pdfs. Now push to git.");
    }
}
