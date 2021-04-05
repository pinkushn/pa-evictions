package com.lancasterstandsup.tools;

import java.io.*;
import java.util.Scanner;

/**
 * A one-time hacky thing to get a list of addresses of all mdjs
 */
public class ParseMDJAddresses {
    public static void main (String [] args) throws IOException {
        //got this file 4/4/21 from a copy paste of table element from http://www.pacourts.us/courts/minor-courts/magisterial-district-judges/
        String sourceFileName = "mdj_addresses_html.txt";
        String outFileName = "mdj_addresses_normalized.txt";
        String addressFile = "mdj_addresses.csv";
        String sourceFilePath = "./src/main/resources/" + sourceFileName;
        String outFilePath = "./src/main/resources/" + outFileName;
        String addressFilePath = "./src/main/resources/" + addressFile;

//        BufferedReader in = new BufferedReader(new FileReader(sourceFilePath));
//
//        PrintWriter out = new PrintWriter(new FileWriter(outFilePath));
//
//        String next;
//        while ((next = in.readLine()) != null) {
//            if (next.indexOf("result_row") > -1) {
//                out.println();
//            }
//            out.print(next);
//        }
//
//        out.flush();
//        out.close();

        BufferedReader in = new BufferedReader(new FileReader(outFilePath));

        PrintWriter out = new PrintWriter(new FileWriter(addressFilePath));

        out.println("Address,County,Court,JudgeName");

        String next;
        String first = "result-col\">";
        String endTd = "</td>";
        String closeDiv = "</div>";
        String openDiv = "<div>";
        String strong = "<strong>";
        String endStrong = "</strong>";
        String br = "<br>";
        String brbr = br + br;
        String em = "<em>";
        while ((next = in.readLine()) != null) {
            int i = next.indexOf(first);
            if (i != -1) {
                //County
                i += first.length();
                next = next.substring(i);
                i = next.indexOf(endTd);
                String county = next.substring(0, i).trim();

                //Court
                i = next.indexOf(first) + first.length();
                next = next.substring(i);
                i = next.indexOf(endTd);
                String court = next.substring(0, i).trim();

                //Judge name
                i = next.indexOf(strong) + strong.length();
                next = next.substring(i);
                i = next.indexOf(endStrong);
                String judgeName = next.substring(0, i).trim();

                //address
                i += endStrong.length();
                next = next.substring(i);
                i = next.indexOf(em);
                String address = next.substring(0, i);
                address.replace(brbr, br);
                address = address.replace("\t", " ");
                while (address.indexOf("  ") > -1) {
                    address = address.replace("  ", " ");
                }
                address = address.trim();
                String [] split = address.split(br);
                String cityPlus = split[split.length - 1].trim();
                cityPlus = cityPlus.replace(", ", " ");
                String street = split[split.length - 2];
                if (street.indexOf("PO Box") > -1) {
                    street = split[split.length - 3];
                }

                street = street.replace(",", "").trim();

                address = street + " " + cityPlus;

                //address = scrubExtraInfo(address);

                System.out.println(county + ", " + court + ", " + judgeName + ", " + address);

                //if (county.equals("Lancaster")) {
                    out.println(address + "," + county + "," + court + "," + judgeName);
                //}
            }
        }

        out.flush();
        out.close();
    }

    private static String scrubExtraInfo(String s) {
        int i = s.indexOf('(');
        if (i < 0) return s;

        int j = s.indexOf(')');

        //also remove first space;
        s = s.substring(0, i - 2) + s.substring(j + 1);
        return s;
    }
}
