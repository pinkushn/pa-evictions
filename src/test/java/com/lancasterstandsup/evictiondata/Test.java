package com.lancasterstandsup.evictiondata;

import org.junit.Assert;

public class Test {

    private static final String TEST_PATH = "./src/test/resources/testPdfs/";

//    @org.junit.Test
//    public void test_2201_0000047_2020() {
//        String fileName = "2201_0000047_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-2-01", "Jodie E. Richardson", "MJ-02201-LT-0000047-2020", "02/28/2020", "Closed", "Turnkey Investment Solutions LLC.", "17112", "Virola, Mirabel", "17602", "03/09/2020", "11:30 am", "$2,110.00", "$3,508.60", "$3,001.85", null, "$306.75", "$800.00", null, "TRUE", "No", "Yes", "TRUE", "TRUE", "TRUE", null, null, "Jerome Allen Taylor, Esq. representing plaintiff Turnkey Investment Solutions LLC.; Attorney fees: $200.00"};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //processParticipantChunk anomaly:
//    //only one line, but no comma in name
//    //"Quail Run Southeastern, PA"
//    @org.junit.Test
//    public void test_2203_0000079_2020() {
//        String fileName = "2203_0000079_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-2-03", "Mary Mongiovi Sponaugle", "MJ-02203-LT-0000079-2020", "03/16/2020", "Closed", "Quail Run", "19399", "Dickinson, Wanda", "17540", null, null, "$931.78", null, null, null, null, null, "TRUE", null, "No", "No", null, null, null, null, null, null};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //processParticipantChunk anomaly:
//    //name place zip
//    //more name
////    CASE PARTICIPANTS
////    Address
////    Participant Type Participant Name
////    Plaintiff Trademark Property Management, LLC,  Lancaster, PA 17603
////    Lancaster
////    Defendant Lancaster, PA 17603
////    Santos, Junaito Deaza
//    @org.junit.Test
//    public void test_2101_0000035_2021() {
//        String fileName = "2101_0000035_2021.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-1-01", "Adam J. Witkonis", "MJ-02101-LT-0000035-2021", "03/09/2021", "Active", "Trademark Property Management, LLC, Lancaster", "17603", "Santos, Junaito Deaza", "17603", null, null, "$6,100.00", null, null, null, null, null, null, null, "No", "No", null, null, null, null, null, null};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //Judgment for plaintiff but no other indicator of tenant loss
//    @org.junit.Test
//    public void test_2208_0000069_2018() {
//        String fileName = "2208_0000069_2018.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-2-08", "Edward A. Tobin", "MJ-02208-LT-0000069-2018", "06/08/2018", "Closed", "United Zion Retirement Community, Lititz", "17543", "Wilson, Kelly & Doll, Brian", "17543", "06/18/2018", "2:00 pm", "$2,105.40", "$0.00", "$0.00", "$0.00", "$0.00", "$407.00", null, null, "No", "No", null, null, null, "TRUE", null, "Server fees: $0.00"};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //'Stayed'
//    @org.junit.Test
//    public void test_2103_0000275_2018() {
//        String fileName = "2103_0000275_2018.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-1-03", "Miles K. Bixler", "MJ-02103-LT-0000275-2018", "12/07/2018", "Inactive", "Stofflet, David", "17520", "Stofflet, Tonya", "17543", "12/21/2018", "9:15 am", "$6,547.00", null, null, null, null, null, null, null, "No", "No", null, null, null, "TRUE", null, "Rule 513 - Stay of Proceedings"};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //missing claim
//    @org.junit.Test
//    public void test_2304_0000026_2018() {
//        String fileName = "2304_0000026_2018.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-3-04", "Stuart J. Mylin", "MJ-02304-LT-0000026-2018", "02/21/2018", "Closed", "Taggart, Ron", "19320", "Kilhefner, Mark & Blantz, Denise", "17584", "03/02/2018", "9:15 am", "MISSING CLAIM", "$2,489.50", "$2,125.00", "$136.75", "$180", "$1,000.00", null, null, "No", "Yes", "TRUE", "TRUE", null, "TRUE", null, "Server fees: $47.75"};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //Truncated plaintiff name in attorneys section required more PdfData logic
//    @org.junit.Test
//    public void test_2201_0000068_2019() {
//        String fileName = "2201_0000068_2019.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-2-01", "Jodie E. Richardson", "MJ-02201-LT-0000068-2019", "03/25/2019", "Closed", "Hillrise Mutual Housing Association Inc, 455 Rockland Street Lancaster Pa 17602", "17602", "Semprit, Yaceila", "17602", "04/03/2019", "10:00 am", "$2,387.25", "$1,409.25", "$1,042.00", "$267.25", null, "$176.00", null, null, "No", "Yes", "TRUE", "TRUE", null, "TRUE", null, "Angelo Joseph Fiorentino, Esq. representing plaintiff Hillrise Mutual Housing Association Inc, 455 Roc; Damages: $100.00"};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //Truncated plaintiff name in attorneys section required more PdfData logic
//    @org.junit.Test
//    public void test_2208_0000101_2019() {
//        String fileName = "2208_0000101_2019.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-2-08", "Edward A. Tobin", "MJ-02208-LT-0000101-2019", "07/15/2019", "Inactive", "Hasson, Craig & Hasson, Mary", "17543", "Stockton, Jeffery A. & Stockton, Shelby", "17545", null, null, "$3,319.00", null, null, null, null, null, null, null, "No", "No", null, null, null, "TRUE", null, "Bankruptcy Petition Filed"};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //Attorney is for plaintiff despite double space
//    @org.junit.Test
//    public void test2102_0000041() {
//        String fileName = "2102_0000041_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-1-02", "David P. Miller", "MJ-02102-LT-0000041-2020", "03/14/2020", "Closed", "Federal Realty Investment Trust, Rockville", "208524041", "The Solid Wood Cabinet Company LLC", "17601", "06/12/2020", "9:45 am", "$12,000.00", "$342.75", "$0.00", "$177.25", "$5.00", "$6,805.59", null, null, "Yes", "No", "TRUE", "TRUE", null, "TRUE", null, "Continued 3 time(s); Bianca Alexis Roberto, Esq. representing plaintiff Federal Realty Investment Trust, Rockville; Server fees: $160.50"};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //appealed
//    @org.junit.Test
//    public void test2201_0000083() {
//        String fileName = "2201_0000083_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-2-01", "Jodie E. Richardson", "MJ-02201-LT-0000083-2020", "10/26/2020", "Inactive", "Dejesus, Javier", "17603", "Suarez, Sonia", "17602", "11/04/2020", "1:30 pm", "$0.00", "$142.75", null, null, "$142.75", "$0.00", null, null, "Yes", "No", null, null, null, "TRUE", null, "Landlord/Tenant Possession Appeal Filed"};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //One attorney for two plaintiffs
//    @org.junit.Test
//    public void test2305_0000063() {
//        String fileName = "2305_0000063_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-3-05", "Raymond S. Sheller", "MJ-02305-LT-0000063-2020", "12/01/2020", "Closed", "Ford, James R. Jr. & Ford, Kimberly", "19344", "Syphard, Kristina & Syphard, Mike", "17529", "12/16/2020", "12:15 pm", "$0.00", null, null, null, null, null, "TRUE", null, "No", "No", null, null, null, null, null, "Timothy A. Lanza, Esq. representing plaintiff Ford, James R. Jr.; Timothy A. Lanza, Esq. representing plaintiff Ford, Kimberly"};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    @org.junit.Test
//    public void test2306_0000001() {
//        String fileName = "2306_0000001_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-3-06", "Jonathan W. Heisse", "MJ-02306-LT-0000001-2020", "01/03/2020", "Closed", "KS Rentals LLC", "17557", "Greist, Robin", "17557", "01/15/2020", "2:30 pm", "$3,360.00", "$3,548.25", "$3,360.00", null, "$5.00", "$750.00", null, null, "No", "Yes", "TRUE", "TRUE", null, "TRUE", null, "Server fees: $21.25"};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //filing fees
//    @org.junit.Test
//    public void test2306_0000002() {
//        String fileName = "2306_0000002_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-3-06", "Jonathan W. Heisse", "MJ-02306-LT-0000002-2020", "01/06/2020", "Closed", "Zimmerman, Paul D.", "17557", "Bouder, Charles", "17557", "01/16/2020", "1:45 pm", "$0.00", "$169.75", null, "$122.25", "$5.00", "$500.00", null, null, "Yes", "No", "TRUE", "TRUE", null, null, null, "Server fees: $21.25"};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //withdrawn
//    @org.junit.Test
//    public void test2306_0000003() {
//        String fileName = "2306_0000003_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-3-06", "Jonathan W. Heisse", "MJ-02306-LT-0000003-2020", "01/29/2020", "Closed", "Trademark Property Manangement", "17602", "Jennings, Patrick", "17557", "02/06/2020", "10:00 am", "$1,300.00", null, null, null, null, null, "TRUE", null, "No", "No", null, null, null, "TRUE", null, null};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //settled
//    @org.junit.Test
//    public void test2306_0000004() {
//        String fileName = "2306_0000004_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-3-06", "Jonathan W. Heisse", "MJ-02306-LT-0000004-2020", "01/29/2020", "Closed", "Trademark Property Manangement", "17602", "LaCastia Boriken Restaurant", "17557", "02/06/2020", "10:15 am", "$1,000.00", null, null, null, null, null, null, null, "No", "No", null, null, null, "TRUE", null, "Case was settled 02/05/2020" };
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //settled with each (of 2) defendants
//    @org.junit.Test
//    public void test2306_0000007() {
//        String fileName = "2306_0000007_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-3-06", "Jonathan W. Heisse", "MJ-02306-LT-0000007-2020", "02/06/2020", "Closed", "Mill Creek Estates", "17557", "Guyer, Chris & Guyer, Jane", "17557", "02/18/2020", "9:30 am", "$1,360.52", null, null, null, null, null, null, null, "No", "No", null, null, null, "TRUE", null, "Case was settled 02/18/2020" };
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //'Disposition Summary' has 'grant if' fields instead of CIVIL_DISPOSITION_JUDGMENT_DETAILS
//    @org.junit.Test
//    public void test2306_0000029() {
//        String fileName = "2306_0000029_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-3-06", "Jonathan W. Heisse", "MJ-02306-LT-0000029-2020", "09/04/2020", "Closed", "American Heritage Property Management", "17603", "Felizzi, Gabrielle & Felizzi, Luann", "17557", null, null, "$6,190.00", null, null, null, null, null, null, null, "No", "No", null, null, null, "TRUE", null, "Continued 1 time(s); Case was settled 10/16/2020" };
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //Disposition Summary section doesn't have monthly rent
//    //One defendant from NY
//    @org.junit.Test
//    public void test2306_0000030() {
//        String fileName = "2306_0000030_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-3-06", "Jonathan W. Heisse", "MJ-02306-LT-0000030-2020", "09/09/2020", "Active", "Horning Farm Agency", "17555", "Ritchey, Jessica & McFadden, Thomas", "17555 & 13827", "02/16/2021", "2:15 pm", "$3,395.50", null, null, null, null, null, null, null, "No", "No", null, null, null, "TRUE", null, "Continued 2 time(s)" };
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //Dismissed without prejudice --> tenant win
//    @org.junit.Test
//    public void test2306_0000031() {
//        String fileName = "2306_0000031_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-3-06", "Jonathan W. Heisse", "MJ-02306-LT-0000031-2020", "09/10/2020", "Closed", "Rowles, Leif", "17522", "Root, Miranda & Root, Greg & Cambell, Zachary & Arron, Corry", "17522", "09/23/2020", "1:45 pm", "$0.00", null, null, null, null, null, null, "TRUE", "No", "No", null, null, "TRUE", "TRUE", null, null};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //fees preceded by '*' to indicate joint/several liability
//    //also, both defendants listed twice in PARTICIPANTS
//    @org.junit.Test
//    public void test2306_0000034() {
//        String fileName = "2306_0000034_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-3-06", "Jonathan W. Heisse", "MJ-02306-LT-0000034-2020", "09/23/2020", "Closed", "Jake Beiler in C/O Slatehouse Group", "17606", "Meuer, Michael & Montoro, Deana", "17557", "10/01/2020", "1:30 pm", "$1,153.88", "$1,347.22", "$1,098.50", "$122.25", "$20.00", "$424.36", null, null, "No", "Yes", "TRUE", "TRUE", null, null, null, "Server fees: $106.47"};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //attorney for plaintiff
//    @org.junit.Test
//    public void test2306_0000035() {
//        String fileName = "2306_0000035_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-3-06", "Jonathan W. Heisse", "MJ-02306-LT-0000035-2020", "10/09/2020", "Closed", "Terry, June I.", "17517", "Fisher, Guy & Fisher, Susan", "17517", "12/08/2020", "10:30 am", "$0.00", "$7,672.97", "$7,500.00", "$122.25", "$10.00", "$900.00", null, null, "Yes", "No", null, null, null, "TRUE", null, "Continued 3 time(s); Ashley Ann Glick, Esq. representing plaintiff Terry, June I.; Server fees: $40.72"};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    @org.junit.Test
//    public void test2306_0000037() {
//        String fileName = "2306_0000037_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-3-06", "Jonathan W. Heisse", "MJ-02306-LT-0000037-2020", "11/04/2020", "Closed", "King, Marlin S.", "17562", "Lewis, Savannah", "17557", null, null, "$3,080.00", null, null, null, null, null, null, null, "No", "No", null, null, null, "TRUE", null, "Continued 1 time(s); Case was settled 11/23/2020" };
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //'Judgment for Defendant' in Disposition Summary --> tenant win
//    @org.junit.Test
//    public void test2306_0000040() {
//        String fileName = "2306_0000040_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-3-06", "Jonathan W. Heisse", "MJ-02306-LT-0000040-2020", "12/01/2020", "Closed", "Trademark Property Management", "17602", "Lucas, Curtis", "17557", "12/10/2020", "10:30 am", "$0.00", null, null, null, null, null, null, null, "No", "No", null, null, "TRUE", null, null, null};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //different layout of CASE INFORMATION
//    @org.junit.Test
//    public void test2102_000001() {
//        String fileName = "2102_0000001_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-1-02", "David P. Miller", "MJ-02102-LT-0000001-2020", "01/02/2020", "Closed", "Hykes, Nathan", "17538", "King, Wanda", "17603", "01/14/2020", "9:45 am", "$1,800.00", "$2,248.35", "$1,800.00", "$122.25", "$5.00", "$850.00", null, null, "No", "Yes", "TRUE", "TRUE", null, "TRUE", null, "Server fees: $187.60"};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    @org.junit.Test
//    public void test2101_0000004() {
//        String fileName = "2101_0000004_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-1-01", "Adam J. Witkonis", "MJ-02101-LT-0000004-2020", "01/03/2020", "Closed", "New DAE Ventures, LLC, Lancaster", "17603", "Johnson, Maurice & Johnson, Shiretha", "17603", "01/14/2020", "2:15 pm", "$1,490.00", null, null, null, null, null, null, null, "No", "No", null, null, "TRUE", "TRUE", null, "James Orgass, Esq. representing defendant Johnson, Maurice"};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //different pattern for PARTICIPANTS
//    @org.junit.Test
//    public void test2101_0000040() {
//        String fileName = "2101_0000040_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-1-01", "Adam J. Witkonis", "MJ-02101-LT-0000040-2020", "01/22/2020", "Closed", "Royersford Gardens LTD, Southeastern", "19399", "Perez Lozada, Ismael", "17603", null, null, "$511.50", null, null, null, null, null, "TRUE", null, "No", "No", null, null, null, null, null, null};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //another different pattern for PARTICIPANTS
//    @org.junit.Test
//    public void test2101_0000045() {
//        String fileName = "2101_0000045_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-1-01", "Adam J. Witkonis", "MJ-02101-LT-0000045-2020", "01/24/2020", "Closed", "Ben Zee Group in care of Rick Wennerstrom's Property Management, Lancaster", "17603", "Wilson, Domouniq", "17603", "02/06/2020", "2:15 pm", "$789.25", "$2,081.00", "$1,830.25", "$250.75", null, "$810.00", null, null, "No", "Yes", "TRUE", "TRUE", null, "TRUE", null, null};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //moved (hearing schedule was moved)
//    @org.junit.Test
//    public void test2101_0000158() {
//        String fileName = "2101_0000158_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-1-01", "Adam J. Witkonis", "MJ-02101-LT-0000158-2020", "09/04/2020", "Closed", "Christopher Mejia D/B/A CRM Real Estate in care of Rick Wennerstrom's Property Mgmt, Lancaster", "17603", "Acosta, Janelia & Larue, Kywan", "17603", "09/22/2020", "10:15 am", "$2,578.63", "$2,752.38", "$2,578.63", "$173.75", null, "$955.00", null, null, "No", "Yes", null, null, null, "TRUE", null, null};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //Defendant missing zip on pdf!
    @org.junit.Test
    public void test2201_0000002() {
        String fileName = "2201_0000002_2020.pdf";
        LTPdfData data = LTParser.processFile(TEST_PATH + fileName);
        String[] targetData = {"MDJ 02-2-01", "Jodie E. Richardson", "MJ-02201-LT-0000002-2020", "01/06/2020", "Closed", "Hostetter, Jeffrey", "MISSING ZIP", "V., R.", "17602", "01/16/2020", "3:00 pm", "$1,850.00", "$1,992.75", "$100.00", "$142.75", null, null, null, null, null, null, "$875.00", null, null, "No", "Yes", null, null, "TRUE", null, null, null, null, null, null, null};
        Assert.assertArrayEquals(data.getRow(), targetData);
    }
//
//    //case transferred
//    @org.junit.Test
//    public void test2101_0000194() {
//        String fileName = "2101_0000194_2020.pdf";
//        PdfData data = Parser.processFile(TEST_PATH + fileName);
//        String[] targetData = {"MDJ 02-1-01", "Adam J. Witkonis", "MJ-02101-LT-0000194-2020", "10/14/2020", "Closed", "City Limits Realty", "17602", "Rutter, Raymond William", "17602", "10/22/2020", "10:45 am", "$0.00", null, null, null, null, null, null, null, "No", "No", null, null, null, "TRUE", null, "Case transferred"};
//        Assert.assertArrayEquals(data.getRow(), targetData);
//    }
//
//    //case transferred
    @org.junit.Test
    public void test2201_0000089() {
        String fileName = "2201_0000089_2020.pdf";
        LTPdfData data = LTParser.processFile(TEST_PATH + fileName);
        String[] targetData = {"MDJ 02-2-01", "Jodie E. Richardson", "MJ-02201-LT-0000089-2020", "11/23/2020", "Closed", "Redbud Investments LLC", "17557", "S., K. &. S., D.", "17602", "12/03/2020", "11:00 am", "$1,336.43", "$1,761.70", "$1,501.45", null, "$155.25", null, "$105.00", null, null, null, "$1,501.45", null, null, "No", "Yes", null, null, "TRUE", null, null, null, null, null, null, null};
        Assert.assertArrayEquals(data.getRow(), targetData);
    }

//    //hand delivery
    @org.junit.Test
    public void test2203_0000100() {
        String fileName = "2203_0000100_2020.pdf";
        LTPdfData data = LTParser.processFile(TEST_PATH + fileName);
        String[] targetData = {"MDJ 02-2-03", "Mary Mongiovi Sponaugle", "MJ-02203-LT-0000100-2020", "10/07/2020", "Closed", "American Heritage Property Management", "17603", "C., P. &. L., O.", "17603", "10/19/2020", "10:30 am", "$1,983.21", "$1,703.79", "$1,581.54", null, "$122.25", null, null, null, null, null, "$1,300.00", null, null, "Yes", "No", null, null, "TRUE", null, null, null, null, null, null, null};
        Assert.assertArrayEquals(data.getRow(), targetData);
    }

//    //attorney fees
    @org.junit.Test
    public void test2204_0000100() {
        String fileName = "2204_0000100_2020.pdf";
        LTPdfData data = LTParser.processFile(TEST_PATH + fileName);
        String[] targetData = {"MDJ 02-2-04", "Andrew T. LeFever", "MJ-02204-LT-0000100-2020", "10/16/2020", "Closed", "Rent Period Inc", "17402", "W., J.", "17602", "10/28/2020", "10:00 am", "$250.00", "$500.75", "$0.00", null, "$250.75", null, null, "$250.00", null, null, "$1,095.00", null, null, "Yes", "No", "01/12/2021", "01/12/2021", "TRUE", null, null, null, "TRUE", "Kurt A. Blake, Esq.", null, null};
        Assert.assertArrayEquals(data.getRow(), targetData);
    }

//    //'Judge' doesn't have 'magisterial' prefix
    @org.junit.Test
    public void test2207_0000001() {
        String fileName = "2207_0000001_2020.pdf";
        LTPdfData data = LTParser.processFile(TEST_PATH + fileName);
        String[] targetData = {"MDJ 02-2-07", "Tony S. Russell", "MJ-02207-LT-0000001-2020", "01/02/2020", "Closed", "Clover Property Management Inc. Agent for Owner, Lancaster", "17601", "V., E.", "17522", "01/09/2020", "10:30 am", "$1,763.00", "$1,920.13", "$1,763.00", "$122.25", "$5.00", "$29.88", null, null, null, null, "$695.00", null, null, "No", "Yes", null, null, "TRUE", null, null, null, null, null, null, null};
        Assert.assertArrayEquals(data.getRow(), targetData);
    }

//    //home and property abandoned
    @org.junit.Test
    public void test2304_0000056() {
        String fileName = "2304_0000056_2020.pdf";
        LTPdfData data = LTParser.processFile(TEST_PATH + fileName);
        String[] targetData = {"MDJ 02-3-04", "Stuart J. Mylin", "MJ-02304-LT-0000056-2020", "09/02/2020", "Closed", "PCS CHADAGA C/O PROPERTY MANAGEMENT, INC.", "17043", "R., R.", "17560", "09/15/2020", "10:30 am", "$3,378.11", "$3,997.61", "$3,810.11", "$140.75", "$5.00", "$41.75", null, null, null, null, "$432.00", null, null, "Yes", "No", null, null, "TRUE", null, null, null, null, null, null, "Home and Property Abandoned"};
        Assert.assertArrayEquals(data.getRow(), targetData);
    }

    //rent reserved and due
    @org.junit.Test
    public void test2307_0000022() {
        String fileName = "2307_0000022_2020.pdf";
        LTPdfData data = LTParser.processFile(TEST_PATH + fileName);
        String[] targetData = {"MDJ 02-3-07", "Nancy G. Hamill", "MJ-02307-LT-0000022-2020", "09/15/2020", "Closed", "American Heritage Property Management", "17603", "K., M.", "17517", "09/29/2020", "9:00 am", "$2,640.00", "$1,667.00", null, null, "$167.00", null, null, null, "$1,500.00", null, "$825.00", null, null, "Yes", "No", null, null, "TRUE", null, null, null, null, null, null, null};
        Assert.assertArrayEquals(data.getRow(), targetData);
    }

    //plaintiff name partially on second line
    @org.junit.Test
    public void test2201_0000090_2021() {
        String fileName = "2201_0000090_2021.pdf";
        LTPdfData data = LTParser.processFile(TEST_PATH + fileName);
        String[] targetData = {"MDJ 02-2-01", "Jodie E. Richardson", "MJ-02201-LT-0000090-2021", "08/03/2021", "Active", "Rick Wennerstrom", "17603", "R., L.", "17602", null, null, "$0.00", null, null, null, null, null, null, null, null, null, null, null, null, "No", "No", null, null, null, null, null, null, null, null, null, null};
        Assert.assertArrayEquals(data.getRow(), targetData);
    }

    @org.junit.Test
    public void addMoney() {
        Assert.assertEquals("$50", LTPdfData.addMoneyStrings("$25", "25"));
        Assert.assertEquals("$50.01", LTPdfData.addMoneyStrings("$25.01", "25"));
        Assert.assertEquals("$51.02", LTPdfData.addMoneyStrings("$25.51", "25.51"));
    }
}
