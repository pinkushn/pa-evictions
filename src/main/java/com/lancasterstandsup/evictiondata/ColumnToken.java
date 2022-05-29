package com.lancasterstandsup.evictiondata;

public enum ColumnToken {
    JUDGE("Judge"),
    COURT("Court"),
    DOCKET("Docket"),
    OTNS("OTN(s)"),
    FILE_DATE("File Date"),
    UNABLE_TO_PAY_BAIL("\"Unable to Pay Bail\""),
    BAIL("Bail"),
    HAS_BAIL_SECTION("Has Bail Section"),
    BAIL_TYPE("Bail Type"),
    BAIL_PERCENTAGE("Bail Percentage"),
    GRADES("Grades", 30),
    OTHER_DOCKETS("Other Dockets", 50),
    MOST_SERIOUS_GRADE("Most Serious Grade"),
    
    STATUS("Case Status"),
    PLAINTIFF("Plaintiff", 80),
    PLAINTIFF_ZIP("Plaintiff ZIP"),
    DEFENDANT("Defendant", 80),
    DEFENDANT_ZIP("Defendant ZIP"),
    HEARING_DATE("Hearing Date"),
    HEARING_TIME("Hearing Time"),
    CLAIM_AMOUNT("Claim Amt"),
    JUDGMENT_AMOUNT("Judgment Amount"),
    RENT_IN_ARREARS("Rent in Arrears"),
    FILING_FEES("Filing Fees"),
    COSTS("Costs"),
    SERVER_FEES("Server Fees"),
    DAMAGES("Damages"),
    ATTORNEY_FEES("Attorney Fees"),
    RENT_RESERVED_AND_DUE("Rent Reserved and Due"),
    INTEREST("Interest"),
    MONTHLY_RENT("Monthly Rent"),
    WITHDRAWN("Withdrawn"),
    DISMISSED("Dismissed"),
    GRANT_POSS("Grant Poss"),
    GRANT_POSS_IF_JUDGE_NOT_SATISFIED("Grant Pos if Judge Not Satisfied"),
    ORDER_FOR_POSS_REQ("Order for Poss Req"),
    ORDER_FOR_POSS_SERVED("Order for Poss Served"),
    JUDGMENT_FOR_PLAINTIFF("Judgment for Plaintiff"),
    JUDGMENT_FOR_DEFENDANT("Judgment for Defendant"),
    SETTLED("Settled"),
    STAYED("Stayed"),
    APPEALED("Appealed"),
    PLAINTIFF_ATTORNEY("Plaintiff Attorney", 60),
    DEFENDANT_ATTORNEY("Defendant Attorney", 60),
    NOTES("Notes"),
    PAIRED_MDJS_SAME_COUNTY("Paired MDJs Same County");
    String header;
    int maxWidth;

    ColumnToken(String header, int maxWidth) {
        this.header = header;
        this.maxWidth = maxWidth;
    }

    ColumnToken(String header) {
        this(header, 0);
    }

    public String toString() {
        return header;
    }

    public boolean hasMaxWidth() {
        return maxWidth != 0;
    }

    public int getMaxWidth() {
        return maxWidth;
    }
}
