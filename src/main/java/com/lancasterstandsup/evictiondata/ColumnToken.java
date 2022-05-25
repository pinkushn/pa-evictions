package com.lancasterstandsup.evictiondata;

public enum ColumnToken {
    JUDGE("Judge"),
    DOCKET("Docket"),
    OTNS("OTN(s)"),
    FILE_DATE("File Date"),
    UNABLE_TO_PAY_BAIL("\"Unable to Pay Bail\""),
    BAIL("Bail"),
    HAS_BAIL_SECTION("Has Bail Section"),
    BAIL_TYPE("Bail Type"),
    BAIL_PERCENTAGE("Bail Percentage"),
    GRADES("Grades"),
    OTHER_DOCKETS("Other Dockets"),
    MOST_SERIOUS_GRADE("Most Serious Grade");
    String header;

    ColumnToken(String header) {
        this.header = header;
    }

    public String toString() {
        return header;
    }
}
