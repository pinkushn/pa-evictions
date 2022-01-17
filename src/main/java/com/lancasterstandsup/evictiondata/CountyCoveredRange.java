package com.lancasterstandsup.evictiondata;

import java.io.Serializable;
import java.time.LocalDateTime;

public class CountyCoveredRange implements Serializable {
    LocalDateTime start;
    LocalDateTime end;

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }
}
