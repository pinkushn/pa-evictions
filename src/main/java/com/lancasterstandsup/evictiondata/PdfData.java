package com.lancasterstandsup.evictiondata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

public abstract class PdfData implements Comparable<PdfData>{

    static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private static final Comparator<PdfData> COMPARATOR =
            Comparator.comparing(PdfData::getFileDate)
                    .thenComparing(PdfData::getJudgeName)
                    .thenComparing(PdfData::getDocketNumber);

    abstract boolean rescrape(LocalDateTime lastCheck);
    abstract boolean isClosed();
    abstract String getDocketNumber();
    abstract String getCourtOffice();
    abstract String getJudgeName();
    abstract LocalDate getFileDate();

    public int getDocketNumberAsInt() {
        String s = getDocketNumber();
        int i = s.lastIndexOf('-');
        s = s.substring(0, i);
        i = s.lastIndexOf('-');
        s = s.substring(i + 1);
        return Integer.parseInt(s);
    }
    public int compareTo(PdfData o) {
        return COMPARATOR.compare(this, o);
    }
}
