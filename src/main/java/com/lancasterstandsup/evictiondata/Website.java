package com.lancasterstandsup.evictiondata;

import j2html.tags.ContainerTag;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static j2html.TagCreator.*;

public class Website {

    public final static String[] countiesRaw = {
            "Lancaster",
    };

//    public final static String[] countiesRaw = {
//            "Lancaster",
//            "York",
//            "Berks",
//            "Dauphin",
//            "Lebanon",
//            "Adams",
//            "Allegheny",
//            //"Philadelphia",
//            "Armstrong",
//            "Beaver",
//            "Bedford",
//            "Blair",
//            "Bradford",
//            "Bucks",
//            "Butler",
//            "Cambria",
//            "Cameron",
//            "Carbon",
//            "Centre",
//            "Chester",
//            "Clarion",
//            "Clearfield",
//            "Clinton",
//            "Columbia",
//            "Crawford",
//            "Cumberland",
//            "Delaware",
//            "Elk",
//            "Erie",
//            "Fayette",
//            "Forest",
//            "Franklin",
//            "Fulton",
//            "Greene",
//            "Huntingdon",
//            "Indiana",
//            "Jefferson",
//            "Juniata",
//            "Lackawanna",
//            "Lawrence",
//            "Lehigh",
//            "Luzerne",
//            "Lycoming",
//            "McKean",
//            "Mercer",
//            "Mifflin",
//            "Monroe",
//            "Montgomery",
//            "Montour",
//            "Northampton",
//            "Northumberland",
//            "Perry",
//            "Pike",
//            "Potter",
//            "Schuylkill",
//            "Snyder",
//            "Somerset",
//            "Sullivan",
//            "Susquehanna",
//            "Tioga",
//            "Union",
//            "Venango",
//            "Warren",
//            "Washington",
//            "Wayne",
//            "Westmoreland",
//            "Wyoming",
//    };

    public static List<String> counties;

    static {
        counties = Arrays.asList(countiesRaw);
    }

    public static void main (String[] args) throws IOException {
        Collection<String> empty = new LinkedList<>();
        buildWebsite(counties);
    }

    public static void buildWebsite (Collection<String> counties) throws IOException {
        TreeSet<String> ordered = new TreeSet<>();
        ordered.addAll(counties);
        counties = ordered;

        String html = html(
                head(
                        meta().attr("charset","UTF-8"),
                        meta().attr("name","viewport")
                        .attr("content", "width=device-width, initial-scale=1, shrink-to-fit=no"),
                        link().withHref("https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css")
                            .withRel("stylesheet")
                            .attr("integrity", "sha384-EVSTQN3/azprG1Anm3QDgpJLIm9Nao0Yz1ztcQTwFspd3yD65VohhpuuCOmLASjC")
                            .attr("crossorigin", "anonymous"),
                        script().withSrc("https://cdn.jsdelivr.net/npm/jquery@3.5.1/dist/jquery.slim.min.js")
                                .attr("integrity", "sha384-DfXdz2htPH0lsSSs5nCTpuj/zy4C+OGpamoFVy38MVBnE+IbbVYUew+OrCXaRkfj")
                                .attr("crossorigin", "anonymous"),
                        script().withSrc("https://cdn.jsdelivr.net/npm/bootstrap@4.6.1/dist/js/bootstrap.bundle.min.js")
                            .attr("integrity", "sha384-fQybjgWLrvvRgtW6bFlB7jaZrFsaBXjsOMm/tB9LTS58ONXgqbR9W8oWht/amnpF")
                            .attr("crossorigin", "anonymous")
                ),
                body(
                        //google studio data report iframe
//                        iframe()
//                            .attr("width", "600")
//                            .attr("height", "220")
//                            .withSrc("https://datastudio.google.com/embed/reporting/ad9a4d86-a85f-493e-b41e-e25ca17b481c/page/eoPWC")
//                            .attr("frameborder", "0")
//                            .withStyle("border:0")
//                            .attr("allowfullscreen"),

                        //Tabs. Got too crowded.
//                        ul(
//                                each(counties, county ->
//                                        countyTabHeader(county)
//                                )
//                        ).withClasses("nav", "nav-tabs")
//                        .withRole("tablist")
//                        .withId("myTab"),
//
//                        div(
//                            each(counties, county ->
//                                    countyTab(county))
//                        ).withClass("tab-content")

                        //drop-down
                        div (h3("PA eviction data 2019 to near-present ").withStyle("font-weight:bold")

                        //).withStyle("margin-left:10px; margin-top:5px"),
                        ).withStyle("text-align:center"),
                        div("(all counties except Philadelphia)")
                                .withStyle("text-align:center; font-size:small; margin-bottom: 5px"),
                        div ("Choose a county to download an eviction data spreadsheet.")
                                //.withStyle("margin-left:10px; margin-bottom:5px"),
                        .withStyle("text-align:center; margin-bottom: 10px"),
                        div(
                            a(
                            "Choose County"
                            ).withClasses("btn btn-secondary dropdown-toggle")
                                .withHref("#")
                                .withRole("button")
                                .withId("dropdownMenuLink")
                                .attr("data-toggle", "dropdown")
                                .attr("aria-haspopup", "true")
                                .attr("aria-expanded", "false")
                                .withStyle("font-size: large"),

                            div(
                                    each(counties, county ->
                                        countyA(county)
                                    )
                            )
                            .withClass("dropdown-menu")
                            .attr("aria-labelledby", "dropdownMenuLink")
                        ).withClasses("dropdown", "show")
                                //.withStyle("margin-left:10px; "),
                        .withStyle("text-align:center"),
                        br(),
                        div(
                            span("Data pulled from public "),
                            a("PA court records")
                                    .withHref("https://ujsportal.pacourts.us/CaseSearch")
                        ).withStyle("text-align:center; margin-bottom: 10px"),
                        div (span("Records near the end cut-off dates may be incomplete."),
                                br(),
                                span("Not all courts upload cases or changes same-day.")

                        ).withStyle("text-align:center; margin-bottom: 10px")
                )
        ).renderFormatted();

        BufferedWriter writer = new BufferedWriter(new FileWriter("index.html"));
        writer.write(html);
        writer.close();

        System.out.println("Done");
    }

    public static ContainerTag countyA(String county) {
        String countyPath = LTAnalysis.dataPathWithDot + county;
        File countyDir = new File(countyPath);

        if (!countyDir.exists() || countyDir.listFiles().length == 0) {
            return div(
                    a(county + " (no data)")
                            .withHref("#")
                            .withClass("dropdown-item")
            );
        }

        String worksheetIndicator = county + "_eviction_cases_";
        File worksheet = null;
        String worksheetFileName = null;

        for (File file: countyDir.listFiles()) {
            worksheetFileName = file.getName();
            if (worksheetFileName.indexOf(worksheetIndicator) > -1) {
                worksheet = file;
                break;
            }
        }

        if (worksheet == null) {
            return div(
                a(county + " (no data)")
                        .withHref("#")
                        .withClass("dropdown-item")
            );
        }
//        String dateRangePresentable = getDateRangePresentable(worksheetFileName.substring(worksheetIndicator.length()));
//        //trim .xslx
//        dateRangePresentable = dateRangePresentable.substring(0, dateRangePresentable.length() - 5);

        String endDate = getFinalDatePresentable(worksheetFileName.substring(worksheetIndicator.length()));
        //trim .xslx
        endDate = endDate.substring(0, endDate.length() - 5);

        return
                div(
                    a(county + " to " + endDate)
                            .withHref(countyPath.substring(2) + "/" + worksheetFileName)
                            .withClass("dropdown-item")
                );
    }

    public static ContainerTag countyTabHeader(String county) {
        boolean active = county.equals("Lancaster");
        return li(
                button(
                        county
                ).withClasses("nav-link", iff(active, "active"))
                        .withId(county + "_tab")
                        .attr("data-bs-toggle", "tab")
                        .attr("data-bs-target", "#" + county)
                        .withType("button")
                        .withRole("tab")
                        .attr("aria-controls", county)
                        .attr("aria-selected", active)
        ).withClass("nav-item")
                .withRole("presentation");
    }

    public static ContainerTag countyTab(String county) {
        boolean active = county.equals("Lancaster");
        return div(
                countyReport(county), countyMDJs(county)
        ).withClasses("tab-pane", "fade", iff(active, "show"), iff(active, "active"))
                .withId(county)
                .withRole("tabpanel")
                .attr("aria-labelledby", county + "_tab");
    }

    public static ContainerTag countyReport(String county) {
        String countyPath = LTAnalysis.dataPathWithDot + county;
        File countyDir = new File(countyPath);

        if (!countyDir.exists()) {
            return div(county + " eviction data not currently available");
        }

        String worksheetIndicator = county + "_eviction_cases_";
        File worksheet = null;
        String worksheetFileName = null;

        for (File file: countyDir.listFiles()) {
            worksheetFileName = file.getName();
            if (worksheetFileName.indexOf(worksheetIndicator) > -1) {
                worksheet = file;
                break;
            }
        }

        if (worksheet == null) return div(county + " eviction data not available").withStyle("margin: 10px");

        String dateRangePresentable = getDateRangePresentable(worksheetFileName.substring(worksheetIndicator.length()));
        //trim .xslx
        dateRangePresentable = dateRangePresentable.substring(0, dateRangePresentable.length() - 5);

        return div(
                a(county + " eviction cases")
                    .withHref(countyPath.substring(2) + "/" + worksheetFileName),
                span(dateRangePresentable),
                br(),
                br(),
                span("Data pulled from public "),
                a("PA court records")
                    .withHref("https://ujsportal.pacourts.us/CaseSearch")
        ).withStyle("margin: 10px");
    }

    public static String getDateRangePresentable(String underscored) {
        String[] parts = underscored.split("_");
        return parts[0] + "/" + parts[1] + "/" + parts[2] + " " + parts[3] + " " +
                parts[4] + "/" + parts[5] + "/" + parts[6];
    }

    public static String getFinalDatePresentable(String underscored) {
        String[] parts = underscored.split("_");
        return parts[4] + "/" + parts[5] + "/" + parts[6];
    }

    public static ContainerTag countyMDJs(String county) {
        return div(

        );
    }
}
