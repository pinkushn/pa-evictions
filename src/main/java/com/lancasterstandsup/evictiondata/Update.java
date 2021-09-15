package com.lancasterstandsup.evictiondata;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class Update {

    public static void main (String [] args) {
//        for (String county: Website.counties) {
//            //scrape(county);
//            excel(county);
//        }
        excel("Lancaster");

        try {
            Website.main(null);
        } catch (IOException e) {
            System.err.println("Failed to create site, abandoning update");
            System.exit(1);
            e.printStackTrace();
        }

//        try {
//            Sheet.main(null);
//        } catch (IOException | GeneralSecurityException e) {
//            System.err.println("Failed to update Google Sheet, abandoning update");
//            System.exit(1);
//            e.printStackTrace();
//        }

        System.out.println("Successfully updated. Now push to git.");
    }

    private static void scrape(String county) {
        try {
            Scraper.scrape(county, "2021");
        } catch (IOException e) {
            System.err.println("Failed to scrape " + county + ", abandon update");
            System.exit(1);
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("Failed to scrape " + county + ", abandon update");
            System.exit(1);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to scrape " + county + ", abandon update");
            System.exit(1);
            e.printStackTrace();
        }
    }

    private static void excel(String county) {
        try {
            Worksheet.webRefresh(county);
        } catch (IOException e) {
            System.err.println("Failed to webRefresh " + county + ", abandon update");
            System.exit(1);
            e.printStackTrace();
        }
    }
}
