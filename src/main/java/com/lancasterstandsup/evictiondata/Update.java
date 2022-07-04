package com.lancasterstandsup.evictiondata;

import java.io.IOException;
import java.util.TreeSet;

public class Update {

    public static void main(String [] args) throws IOException, ClassNotFoundException, InterruptedException {
        //update(Scraper.CourtMode.MDJ_LT);
        Worksheet.csvAllLT(true, 2017);
    }

    public static void update (Scraper.CourtMode courtMode) throws IOException, ClassNotFoundException, InterruptedException {
        TreeSet<String> countiesWithData = new TreeSet<>();
        for (String county: Website.counties) {
            try {
                if (Scraper.getCountyStartAndEnd(county, courtMode) != null) {
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
                totalPdfs += Worksheet.createExcelLT(county);
            } catch (IOException e) {
                System.err.println("Failed to create spreadsheet for " + county + ", abandoning update");
                e.printStackTrace();
                return;
            } catch (ClassNotFoundException e) {
                System.err.println("Failed to create spreadsheet for " + county + ", abandoning update");
                e.printStackTrace();
                return;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            Website.buildWebsite(countiesWithData);
        } catch (IOException e) {
            System.err.println("Failed to create site, abandoning update");
            e.printStackTrace();
            return;
        }

//        boolean isWindows = System.getProperty("os.name")
//                .toLowerCase().startsWith("windows");
//
//        String commands = "cd ~/git/pa-evictions; git add *; git commit -m \"update\"; git push";
//        if (isWindows) commands = "cmd.exe " + commands;
//
//        Runtime.getRuntime().exec(commands);

        Worksheet.csvAllLT(false, null);
//        Worksheet.csvAllLT(true, 2017);
//        Worksheet.csvAllLT(true, 2018);
//        Worksheet.csvAllLT(true, 2019);
//        Worksheet.csvAllLT(true, 2020);
//        Worksheet.csvAllLT(true, 2021);
//        Worksheet.csvAllLT(true, 2022);

        System.out.println("\n*** Successfully updated " + totalPdfs + " pdfs. Now push to git.");
    }
}
