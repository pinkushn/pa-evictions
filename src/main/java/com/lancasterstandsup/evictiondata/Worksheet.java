package com.lancasterstandsup.evictiondata;

import com.github.jferard.fastods.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import static java.time.temporal.ChronoUnit.DAYS;

public class Worksheet {

    public static void main (String[] args) throws IOException, ClassNotFoundException {

//        oneYear("Lancaster", "2018", false);
//        oneYear("Lancaster", "2019", false);
//        oneYear("Lancaster", "2020", false);
//        oneYear("Lancaster", "2021", false);

        //String[] approvedYears = {"2017", "2018", "2019", "2020", "2021"};
        //allYears("Lancaster", approvedYears);

        //webRefresh("Lancaster");
        webRefreshSurroundingCounty("York");
    }

    /**
     * rebuild all artifacts used in web presentation
     */

    public static void webRefresh(String county) throws IOException, ClassNotFoundException {
        //writes 'pre_versus_post.js' to webdata
        Analysis.preVersusPostPandemic(county);

        String[] years = {"2015", "2016", "2017", "2018", "2019", "2020", "2021"};
        Object[] data = null;

        try {
            data = ParseAll.get(county, years, true);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("parser failed, abandoning allYears");
            e.printStackTrace();
            System.exit(1);
        }

        List<PdfData> list = (List<PdfData>) data[2];

        writeExcel(Analysis.dirPath + Analysis.allName, list, null, null);

        //write 'lanco_eviction_cases_3_15_2020_to_X_X_X.xls'
        writeExcel(Analysis.dirPath + Analysis.postName, list, Analysis.march15_2020, Analysis.now);

        //write 'lanco_eviction_cases_Y_Y_Y_to_3_15_2020.xls'
        writeExcel(Analysis.dirPath + Analysis.preName, list, Analysis.preStart, Analysis.march14_2020);

        //someday, maybe also write json object to use for table of 'processed source data'
    }

    public static void webRefreshSurroundingCounty(String county) throws IOException, ClassNotFoundException {
        String[] years = {"2019", "2020", "2021"};
        Object[] data = null;

        try {
            data = ParseAll.get(county, years, true);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("parser failed, abandoning allYears");
            e.printStackTrace();
            System.exit(1);
        }

        List<PdfData> list = (List<PdfData>) data[2];

        writeExcel(Analysis.dirPath + county, list, null, null);
    }

    /**
     *
     * @param filePath
     * @param list
     * @param start inclusive. Use null to turn off date filtering.
     * @param end inclusive
     * @throws IOException
     */
    private static void writeExcel(String filePath, List<PdfData> list, LocalDate start, LocalDate end) throws IOException {
        System.out.println("building excel file " + filePath);
        XSSFWorkbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet("Eviction Cases");
        int rowNum = 0;

        //sheet.setColumnWidth();
        sheet.setDisplayRowColHeadings(true);
        sheet.setPrintRowAndColumnHeadings(true);
        sheet.createFreezePane(0, 1);

        Row header = sheet.createRow(rowNum);
        rowNum++;
        int col = 0;
        for (String h: Parser.colHeaders) {
            Cell headerCell = header.createCell(col);
            headerCell.setCellValue(h);
            col++;
        }

        int cols = 0;
        for (PdfData pdf: list) {
            LocalDate date = pdf.getFileDate();
            boolean use = start == null || (date.compareTo(start) >= 0 && date.compareTo(end) < 1);
            if (use) {
                Row row = sheet.createRow(rowNum);
                rowNum++;
                String[] rowData = pdf.getRow();
                cols = rowData.length;
                for (int c = 0; c < rowData.length; c++) {
                    String cellValue = rowData[c];
                    if (cellValue != null) {
                        Cell cell = row.createCell(c);
                        cell.setCellValue(cellValue);
                    }
                }
            }
        }

        for (int x = 0; x < cols; x++) {
            sheet.autoSizeColumn(x);
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             FileOutputStream fw = new FileOutputStream(filePath);) {
            workbook.write(out);
            workbook.close();
            fw.write(out.toByteArray());
        }
        catch (IOException ioe) {
            throw ioe;
        }
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

        writeODS("ALL " + county + " Eviction Cases (" + yearRange + ") auto-created " + nowS, map);
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

    public static void writeODS(String fileName, Map<String, List<PdfData>> worksheets) {
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
