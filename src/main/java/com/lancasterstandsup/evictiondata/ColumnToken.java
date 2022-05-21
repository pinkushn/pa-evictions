package com.lancasterstandsup.evictiondata;

public enum ColumnToken {
    JUDGE("Judge"),
    DOCKET("Docket"),
    OTN("otn"),
    FILE_DATE("File Date"),
    UNABLE_TO_PAY_BAIL("Unable to Pay Bail"),
    BAIL("Bail");
    String header;

    ColumnToken(String header) {
        this.header = header;
    }

    public String toString() {
        return header;
    }
}
