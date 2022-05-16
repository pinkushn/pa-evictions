package com.lancasterstandsup.evictiondata;

import java.time.LocalDate;

public class Person implements Comparable<Person>{
    String first;
    String last;
    LocalDate birthdate;

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }

    private boolean hasBirthdate() {
        return birthdate != null;
    }

    public String toString() {
        String ret = last + ", " + first + " ";
        if (hasBirthdate()) {
            return ret + birthdate.format(PdfData.slashDateFormatter);
        }
        return  ret + " NO Birthdate on MDJ docket";
    }

    @Override
    public int compareTo(Person o) {
        int ret = toString().compareTo(o.toString());
        if (!hasBirthdate() || !o.hasBirthdate()) {
            if (getFirst().equals(o.getFirst()) && getLast().equals(o.getLast())) {
                if (ret != 0) System.err.println(this + " would have matched w/o bday ");
            }
        }
        return ret;
    }
}
