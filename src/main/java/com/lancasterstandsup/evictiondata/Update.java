package com.lancasterstandsup.evictiondata;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Update {

    public static void main (String [] args) {
        List<String> countiesWithData = new LinkedList<>();
        for (String county: Website.counties) {
            try {
                if (Scraper2.getCountyStartAndEnd(county) != null) {
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

        for (String county: countiesWithData) {
            try {
                Worksheet.createExcel(county);
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

        System.out.println("\n*** Successfully updated. Now push to git.");
    }
}
