package com.lancasterstandsup.evictiondata;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * When does a case get switched to 'closed'?
 *   That is: could it be closed from court's perspective, but still have pending eviction?
 *
 *   Once we have a closed case that includes order for possession served in 2021, we can try to answer this
 */
public class UpdateActiveRecords {

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        //get year
        LocalDateTime now = LocalDateTime.now();
        int y = now.getYear();
        String year = "" + y;
        String lastYear = "" + (y - 1);

        String county = "Lancaster";

        //Any case that is from this or prior year and not closed/inactive: re-scrape pdf
           //Caution: if eviction in progress, we want it even if 'closed'
        List<PdfData> thisYear = ParseAll.parseAll(county, year);
        thisYear = Analysis.orderByDocket(thisYear);
        thisYear = Analysis.filterByClosedOrInactive(thisYear);
        Map<String, List<PdfData>> byCourtOffice = Analysis.groupByCourtOffice(thisYear);
        System.out.println("Will re-scrape " + thisYear.size() + " " + year + " non-closed pdfs");
        Scraper.scrape(county, year, byCourtOffice);

        List<PdfData> priorYear = ParseAll.parseAll(county, lastYear);
        priorYear = Analysis.orderByDocket(priorYear);
        priorYear = Analysis.filterByClosedOrInactive(priorYear);
        Map<String, List<PdfData>> byCourtOfficePriorYear = Analysis.groupByCourtOffice(priorYear);
        System.out.println("Will re-scrape " + priorYear.size() + " " + lastYear + " non-closed pdfs");
        Scraper.scrape(county, lastYear, byCourtOfficePriorYear);

        //normal scrape of current year
        Scraper.scrape(county, year);
    }
}
