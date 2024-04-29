package com.brittonresidenceinvestigation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CallData {
    static String FIFTH = "04/05/2024";
    static String SIXTH = "04/06/2024";

    public static void main(String[] args) {



        // read each line from file named 'calldata'
        List<String> list = new LinkedList<>();
        try (BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader("calldata.csv"))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < 2) {
                System.out.println(line);
                count++;
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
//
//        // write all map key value pairs to file named dates
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("dates"))) {
//            for (String s: list) {
//                writer.write(s);
//                writer.newLine();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//



//        Map<String, String> map = new TreeMap<>();
//        try (BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader("dates"))) {
//            String line;
//            int count = 0;
//            while ((line = reader.readLine()) != null) {
//                // split each line by comma
//                String[] parts = line.split(",");
//                for (String part: parts) {
//                    if (part.indexOf(FIFTH) > -1) {
//                        int i = part.indexOf(":");
//                        String hour = part.substring(i - 2, i);
//                        if (hour.equals("11") && part.indexOf("PM") > -1) {
//                            map.put(part, line);
//                        }
//                    }
//                    else if (part.indexOf(SIXTH) > -1) {
//                        int i = part.indexOf(":");
//                        String hour = part.substring(i - 2, i);
//                        if (hour.equals("12") && part.indexOf("AM") > -1) {
//                            map.put(part, line);
//                        }
//                    }
//                }
//                count++;
//            }
//        } catch (java.io.IOException e) {
//            e.printStackTrace();
//        }
//
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("datesOrdered"))) {
//            for (String s: map.keySet()) {
//                writer.write(s + ", " + map.get(s));
//                writer.newLine();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }



    }
}
