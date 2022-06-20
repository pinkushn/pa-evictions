package com.lancasterstandsup.evictiondata;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public class Worksheet {

    public static String webDataPath = "./webdata/";

    static {
        File localData = new File(Scraper.LOCAL_DATA_PATH);
        if (!localData.exists()) localData.mkdir();
        File webData = new File(webDataPath);
        if (!webData.exists()) webData.mkdir();
    }

    public static int createExcelLT(String county) throws IOException, ClassNotFoundException, InterruptedException {
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

    public static int createExcelCP(String county, String[] years) throws IOException, ClassNotFoundException, InterruptedException {
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

        File dir = new File(Scraper.LOCAL_DATA_PATH + county);
        if (!dir.exists()) {
            dir.mkdir();
        }
        else {
            File [] files = dir.listFiles();
            for (File file: files) {
                if (file.getName().indexOf(".xlsx") > -1) file.delete();
            }
        }

        writeExcel(Scraper.LOCAL_DATA_PATH + county + "/" + excelFileName, pdfs, null, null);

        return pdfs.size();
    }

    public static int createExcelMJ_CR(String county, String[] years) throws IOException, ClassNotFoundException, InterruptedException {
        String excelFileName = county + "_mdj_criminal_dockets_" +
                years[0] +
                (years.length == 1 ?
                        "" :
                        "_to_" + years[years.length - 1]) +
                ".xlsx";

        List<PdfData> pdfs = null;

        try {
            pdfs = ParseAll.get(Scraper.CourtMode.MDJ_CR, county, years);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("parser failed, abandoning createExcelCP");
            e.printStackTrace();
            System.exit(1);
        }

//        List<PdfData> debug = new ArrayList<>();
//        for (PdfData pdf: pdfs) {
////            if (pdf.getDocket().equals("MJ-02101-CR-0000213-2021")) {
////                debug.add(pdf);
////            }
//            if (pdf.getOtherDockets().length() < 1) {
//                debug.add(pdf);
//            }
//        }
//
//        pdfs = debug;

        File dir = new File(Scraper.LOCAL_DATA_PATH + county);
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

        writeExcel(Scraper.LOCAL_DATA_PATH + county + "/" + excelFileName, ordered, null, null);

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
    private static void writeExcel(String filePath, Collection<PdfData> list, LocalDate start, LocalDate end) throws IOException, InterruptedException {
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
            if (headerToken == ColumnToken.GRADES) {
                sheet.setColumnWidth(col, 50);
            }
            else if (headerToken == ColumnToken.OTHER_DOCKETS) {
                sheet.setColumnWidth(col, 50 * 256);
            }
            col++;
        }

        int cols = 0;
        for (PdfData pdf: list) {
            if (pdf.hasNoColumns()) {
                throw new IllegalStateException("NO COLUMNS ???? " + pdf.getDocket());
            }
            LocalDate date = pdf.getFileDate();
            boolean use = start == null || (date.compareTo(start) >= 0 && date.compareTo(end) < 1);
            if (use) {
                Row row = sheet.createRow(rowNum);
                rowNum++;
                RowValues rowValues = pdf.getRowValues();
                String[] rowCellStrings = rowValues.getRow();
                cols = rowCellStrings.length;
                for (int c = 0; c < rowCellStrings.length; c++) {
                    String cellValue = rowCellStrings[c];
                    if (cellValue != null) {
                        Cell cell = row.createCell(c);
                        cell.setCellValue(cellValue);

                        final String docketURL = rowValues.getHyperlink(c);
                        if (docketURL != null) {
                            final int hRow = rowNum;
                            final int hCol = c;
                            Hyperlink hl = (new Hyperlink() {
                                @Override
                                public int getFirstRow() {
                                    return hRow;
                                }

                                @Override
                                public void setFirstRow(int row) {

                                }

                                @Override
                                public int getLastRow() {
                                    return hRow;
                                }

                                @Override
                                public void setLastRow(int row) {

                                }

                                @Override
                                public int getFirstColumn() {
                                    return hCol;
                                }

                                @Override
                                public void setFirstColumn(int col) {

                                }

                                @Override
                                public int getLastColumn() {
                                    return hCol;
                                }

                                @Override
                                public void setLastColumn(int col) {

                                }

                                @Override
                                public String getAddress() {
                                    return docketURL;
                                }

                                @Override
                                public void setAddress(String address) {

                                }

                                @Override
                                public String getLabel() {
                                    return null;
                                }

                                @Override
                                public void setLabel(String label) {

                                }

                                @Override
                                public int getType() {
                                    return 0;
                                }

                                @Override
                                public HyperlinkType getTypeEnum() {
                                    return null;
                                }
                            });
                            org.apache.poi.xssf.usermodel.XSSFHyperlink h =
                                    new org.apache.poi.xssf.usermodel.XSSFHyperlink (hl);

                            cell.setHyperlink(h);
                        }
                    }
                }
            }
        }

        for (int x = 0; x < cols; x++) {
            sheet.autoSizeColumn(x);
        }

        col = 0;
        for (ColumnToken headerToken: aPdfForHeaders.getColumnHeaders()) {
            if (headerToken.hasMaxWidth()) {
                sheet.setColumnWidth(col, headerToken.getMaxWidth() * 256);
            }
            col++;
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

    public static void main (String [] args) throws IOException, ClassNotFoundException, InterruptedException {
//        String[] years = {"2021"};
//        createExcelMJ_CR("Lancaster", years);

        //deleteBlankOTNS();

        clearAllPreProcessed();
//        csvAllLT();

//        File file = new File(Scraper.PDF_CACHE_PATH);
//        for (File child: file.listFiles()) {
//            System.out.println("child: " + child.getName());
//        }

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
            File dir = new File(caseType.getPdfCachePath() + county);
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

    public static void deleteBlankOTNS() throws IOException {
        String path = Scraper.PDF_CACHE_PATH + "OTN";
        File dir = new File(path);
        File[] otnFiles = dir.listFiles();
        int deleted = 0;
        for (File file: otnFiles) {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String s = br.readLine();
            if (s == null || s.trim().length() == 0) {
                file.delete();
                deleted++;
            }
        }
        System.out.println("Deleted " + deleted + " empties");
    }

    public static void csvAllLT(boolean showDefendantNames) throws IOException, ClassNotFoundException, InterruptedException {
        String path = showDefendantNames ? "./webdata/csvs/evictionlab/PA_Evictions.csv" :
                "./webdata/csvs/LT_All.csv";
        File file = new File(path);
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
                if (showDefendantNames) {
                    pdf.setUseFullDefendantName();
                }
                String[] rowData = pdf.getRowValues().getRow();
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
