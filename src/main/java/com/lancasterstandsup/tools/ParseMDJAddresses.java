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
//                i = next.indexOf(br) + br.length();
//                next = next.substring(i);
//                i = next.indexOf(br) + br.length();
//                next = next.substring(i);
//                i = next.indexOf(br) + br.length();
//                next = next.substring(i);
//                i = next.indexOf(br);
                i += endStrong.length();
                next = next.substring(i);
                i = next.indexOf(em);
                String street = next.substring(0, i);
                street = street.replace(br, " ");
                street = street.replace(",", "");
                street = street.replace("\t", " ");
                while (street.indexOf("  ") > -1) {
                    street = street.replace("  ", " ");
                }
                street = street.trim();

                System.out.println(county + ", " + court + ", " + judgeName + ", " + street);

                out.println(street + "," + county + "," + court + "," + judgeName);
            }
        }

        out.flush();
        out.close();
    }
}
