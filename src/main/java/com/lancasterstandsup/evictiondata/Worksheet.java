package com.lancasterstandsup.evictiondata;

import com.github.jferard.fastods.*;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

public class Worksheet {

    public static void main (String[] args) {

//        oneYear("Lancaster", "2018", false);
//        oneYear("Lancaster", "2019", false);
//        oneYear("Lancaster", "2020", false);
//        oneYear("Lancaster", "2021", false);

        String[] approvedYears = {"2017", "2018", "2019", "2020", "2021"};
        allYears("Lancaster", approvedYears);
    }

    /**
     * No scraping occurs: relies on files
     *
     * @param county
     */
    public static void allYears(String county, String[] years) {
        Object[] data = null;

        try {
            data = ParseAll.get(county, years, true);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("parser failed, abandoning allYears");
            e.printStackTrace();
            System.exit(1);
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM_dd_yyyy");
        LocalDateTime now = LocalDateTime.now();
        String nowS = dtf.format(now);

        Map<String, List<PdfData>> map = new TreeMap<>();
        List<PdfData> list = (List<PdfData>) data[2];
        map.put("All", list);

        String yearRange = data[0].toString();
        if (!yearRange.equals(data[1])) {
            yearRange += " to " + data[1].toString();
        }

        write("ALL " + county + " Eviction Cases (" + yearRange + ") auto-created " + nowS, map);
    }

//    public static void oneYear(String county, String year, boolean scrape) {
//        if (scrape) {
//            try {
//                Scraper.scrape(county, year);
//            } catch (IOException | InterruptedException e) {
//                System.err.println("scraper failed, abandoning process");
//                e.printStackTrace();
//                System.exit(1);
//            }
//        }
//
//        Map<String, List<PdfData>> data = null;
//
//        try {
//            data = ParseAll.get(county, year, false);
//        } catch (IOException | ClassNotFoundException e) {
//            System.err.println("parser failed, abandoning process");
//            e.printStackTrace();
//            System.exit(1);
//        }
//
//        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM_dd_yyyy");
//        LocalDateTime now = LocalDateTime.now();
//        String nowS = dtf.format(now);
//
//        write(county + " Eviction Cases " + year + " auto-created " + nowS, data);
//    }

    public static void write(String fileName, Map<String, List<PdfData>> worksheets) {
        OdsFactory odsFactory = OdsFactory.create(Logger.getLogger("Worksheet"), Locale.US);
        AnonymousOdsFileWriter writer = odsFactory.createWriter();
        OdsDocument document = writer.document();

        try {
            for (String tabName: worksheets.keySet()) {
                List<PdfData> worksheet = worksheets.get(tabName);
                if (!worksheet.isEmpty()) {
                    Table table = document.addTable(tabName);
                    TableRowImpl row = table.getRow(0);
                    for (int col = 0; col < Parser.colHeaders.length; col++) {
                        row.getOrCreateCell(col).setStringValue(Parser.colHeaders[col]);
                    }

                    for (int rowNumber = 1; rowNumber <= worksheet.size(); rowNumber++) {
                        row = table.getRow(rowNumber);
                        PdfData data = worksheet.get(rowNumber - 1);
                        String[] rowData = data.getRow();
                        for (int col = 0; col < rowData.length; col++) {
                            String cell = rowData[col];
                            if (cell != null) {
                                row.getOrCreateCell(col).setStringValue(cell);
                            }
                        }
                    }
                }
            }
            writer.saveAs(new File("./src/main/resources", fileName + ".ods"));
        }
        catch (Exception e) {
            System.err.println("Failure to create spreadsheet");
            e.printStackTrace();
            return;
        }

        System.out.println("Wrote spreadsheet " + fileName);
    }
}
