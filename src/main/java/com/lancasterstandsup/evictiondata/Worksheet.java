package com.lancasterstandsup.evictiondata;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public class Worksheet {

    public static String webDataPath = "./webdata/";
    public static String localDataPath = "./localdata/";

    static {
        File localData = new File(localDataPath);
        if (!localData.exists()) localData.mkdir();
        File webData = new File(webDataPath);
        if (!webData.exists()) webData.mkdir();
    }

    public static int createExcelLT(String county) throws IOException, ClassNotFoundException {
        CountyCoveredRange ccr = Scraper.getCountyStartAndEnd(county, Scraper.CourtMode.MDJ_LT);
        int startYear = ccr.getStart().getYear();
        int endYear = ccr.getEnd().getYear();
        int distinctYears = endYear - startYear + 1;
        String [] years = new String[distinctYears];
        for (int x = 0; x < distinctYears; x++) {
            years[x] = "" + (startYear + x);
        }

        LocalDateTime end = ccr.getEnd();
        int month = end.getMonthValue();
        int day = end.getDayOfMonth();
        int year = end.getYear();

        String excelFileName = county + "_eviction_cases_" +
                "1_1_" + years[0] + "_" +
                "to_" +
                month + "_" + day + "_" + year +
                ".xlsx";

        String countyPath = webDataPath + county;
        File countyDir = new File(countyPath);

        if (countyDir.exists() && countyDir.listFiles().length > 0) {
            String worksheetIndicator = county + "_eviction_cases_";
            File worksheet = null;
            String worksheetFileName;

            for (File file : countyDir.listFiles()) {
                worksheetFileName = file.getName();
                if (worksheetFileName.indexOf(worksheetIndicator) > -1) {
                    worksheet = file;
                    break;
                }
            }

            if (worksheet != null && worksheet.getName().equals(excelFileName)) {
                System.out.println(excelFileName + " already exists");
                return 0;
            }
        }

        List<PdfData> pdfs = null;

        try {
            pdfs = ParseAll.get(Scraper.CourtMode.MDJ_LT, county, years);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("parser failed, abandoning createExcelLT");
            e.printStackTrace();
            System.exit(1);
        }

        File dir = new File(webDataPath + county);
        if (!dir.exists()) dir.mkdir();
        else {
            File [] files = dir.listFiles();
            for (File file: files) {
                if (file.getName().indexOf(".xlsx") > -1) file.delete();
            }
        }

        writeExcel(webDataPath + county + "/" + excelFileName, pdfs, null, null);

        return pdfs.size();
    }

    public static int createExcelCP(String county, String[] years) throws IOException, ClassNotFoundException {
        String excelFileName = county + "_common_pleas_criminal_dockets_" +
                years[0] +
                (years.length == 1 ? "" :
                "_to_" +
                years[years.length - 1]) +
                ".xlsx";

        List<PdfData> pdfs = null;

        try {
            pdfs = ParseAll.get(Scraper.CourtMode.CP_CR, county, years);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("parser failed, abandoning createExcelCP");
            e.printStackTrace();
            System.exit(1);
        }

        File dir = new File(localDataPath + county);
        if (!dir.exists()) {
            dir.mkdir();
        }
        else {
            File [] files = dir.listFiles();
            for (File file: files) {
                if (file.getName().indexOf(".xlsx") > -1) file.delete();
            }
        }

        writeExcel(localDataPath + county + "/" + excelFileName, pdfs, null, null);

        return pdfs.size();
    }

    public static int createExcelMJ_CR(String county, String[] years) throws IOException, ClassNotFoundException {
        String excelFileName = county + "_mdj_criminal_dockets_" +
                years[0] +
                (years.length == 1 ? "" :
                        "_to_" +
                                years[years.length - 1]) +
                ".xlsx";

        List<PdfData> pdfs = null;

        try {
            pdfs = ParseAll.get(Scraper.CourtMode.MDJ_CR, county, years);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("parser failed, abandoning createExcelCP");
            e.printStackTrace();
            System.exit(1);
        }

        File dir = new File(localDataPath + county);
        if (!dir.exists()) {
            dir.mkdir();
        }
        else {
            File [] files = dir.listFiles();
            for (File file: files) {
                if (file.getName().indexOf(".xlsx") > -1) file.delete();
            }
        }

        TreeSet<PdfData> ordered = new TreeSet<>();
        ordered.addAll(pdfs);

        writeExcel(localDataPath + county + "/" + excelFileName, ordered, null, null);

        return pdfs.size();
    }

    /**
     *
     * @param filePath
     * @param list
     * @param start inclusive. Use null to turn off date filtering.
     * @param end inclusive
     * @throws IOException
     */
    private static void writeExcel(String filePath, Collection<PdfData> list, LocalDate start, LocalDate end) throws IOException {
        System.out.println("building excel file " + filePath);
        XSSFWorkbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet("Eviction Cases");
        int rowNum = 0;

        sheet.setDisplayRowColHeadings(true);
        sheet.setPrintRowAndColumnHeadings(true);
        sheet.createFreezePane(0, 1);

        Row header = sheet.createRow(rowNum);
        rowNum++;
        int col = 0;
        PdfData aPdfForHeaders = list.iterator().next();
        for (ColumnToken headerToken: aPdfForHeaders.getColumnHeaders()) {
            Cell headerCell = header.createCell(col);
            headerCell.setCellValue(headerToken.toString());
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

    public static void main (String [] args) throws IOException, ClassNotFoundException {
        String[] years = {"2022"};
        createExcelMJ_CR("Lancaster", years);

        //        clearAllPreProcessed();
//        csvAllLT();

//        boolean isWindows = System.getProperty("os.name")
//                .toLowerCase().startsWith("windows");
//
////        String commands = "cd ~/git/pa-evictions; git add *; git commit -m \"update\"; git push";
////        if (isWindows) commands = "cmd.exe " + commands;

        //Runtime.getRuntime().exec("networksetup -setairportnetwork en0 Hodad garbagio");
    }

    public static void clearAllPreProcessed() {
        for (String county: Website.counties) {
            clearPreProcessed(county);
        }
    }

    public static void clearPreProcessed(String county) {
        for (Scraper.CourtMode caseType: Scraper.CourtMode.values()) {
            File dir = new File(Scraper.PDF_CACHE_PATH + caseType.getCaseType() + "/" + county);
            if (!dir.exists()) {
                return;
            }

            for (File yearFile : dir.listFiles()) {
                if (yearFile.getName().indexOf("preProcessed") > -1) {
                    File[] files = yearFile.listFiles();
                    for (File file : files) {
                        file.delete();
                    }
                    if (!yearFile.delete()) {
                        System.err.println("Failed to delete " + yearFile);
                    }
                }
            }
        }
    }

    public static void csvAllLT() throws IOException, ClassNotFoundException {
        File file = new File("./LT_All.csv");
        FileOutputStream fos = new FileOutputStream(file);
        PrintWriter pw = new PrintWriter(fos);

        System.out.println("building csv of all LT pdfs");

        pw.print("County\t");
        for (String h: LTParser.colHeaders) {
            pw.print(h + "\t");
        }
        pw.println();

        for (String county: Website.counties) {
            System.out.println("Next county for csv: " + county);
            CountyCoveredRange ccr = Scraper.getCountyStartAndEnd(county, Scraper.CourtMode.MDJ_LT);
            int startYear = ccr.getStart().getYear();
            int endYear = ccr.getEnd().getYear();
            int distinctYears = endYear - startYear + 1;
            String [] years = new String[distinctYears];
            for (int x = 0; x < distinctYears; x++) {
                years[x] = "" + (startYear + x);
            }

            Object list = null;

            try {
                list = ParseAll.get(Scraper.CourtMode.MDJ_LT, county, years);
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("parser failed, abandoning allYears");
                e.printStackTrace();
                System.exit(1);
            }

            for (LTPdfData pdf: (List<LTPdfData>) list) {
                pw.print(county + "\t");
                String[] rowData = pdf.getRow();
                for (int c = 0; c < rowData.length; c++) {
                    String cellValue = rowData[c];
                    if (cellValue == null) cellValue = "";
                    pw.print(cellValue + "\t");
                }
                pw.println();
            }
        }

        pw.close();
        fos.close();
    }
}
