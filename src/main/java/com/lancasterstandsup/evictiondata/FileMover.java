package com.lancasterstandsup.evictiondata;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileMover {
    public static void main (String[] args) {
        String root = "./src/main/resources/pdfCache/York";
        File dir = new File(root);
//        for (File file: dir.listFiles()) {
//            if (file.isFile()) {
//                String name = file.getName();
//                String year = name.substring(name.length() - 8, name.length() - 4);
//                try {
//                    Files.copy(file.toPath(), Paths.get(root + "/" + year + "/" + name));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }

//        File child_2020 = new File(root + "/2020");
//        File child_2021 = new File(root + "/2021");
        File child_2019 = new File(root + "/2019");
//        File child_2018 = new File(root + "/2018");
//        File child_2017 = new File(root + "/2017");

//        int total = child_2019.listFiles().length +
//                child_2020.listFiles().length +
//                child_2021.listFiles().length;
//
//        int original = dir.listFiles().length;
//
//        System.out.println(total + " v. " + original);

//        System.out.println("2017: " + child_2017.listFiles().length);
//        System.out.println("2018: " + child_2018.listFiles().length);
        System.out.println("2019: " + child_2019.listFiles().length);
//        System.out.println("2020: " + child_2020.listFiles().length);
//        System.out.println("2021: " + child_2021.listFiles().length);
    }
}
