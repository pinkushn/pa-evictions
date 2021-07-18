package com.lancasterstandsup.evictiondata;

import j2html.tags.ContainerTag;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static j2html.TagCreator.*;

public class Website {

    public final static String[] countiesRaw = {
            "Lancaster",
            "York",
            "Berks",
            "Dauphin",
            "Lebanon"
    };

    public static List<String> counties;

    static{
        counties = Arrays.asList(countiesRaw);
    }

    public static void main (String [] args) throws IOException {
        String html = html(
                head(
                        link().withHref("https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css")
                            .withRel("stylesheet")
                            .attr("integrity", "sha384-EVSTQN3/azprG1Anm3QDgpJLIm9Nao0Yz1ztcQTwFspd3yD65VohhpuuCOmLASjC")
                            .attr("crossorigin", "anonymous"),
                        script().withSrc("https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/js/bootstrap.bundle.min.js")
                            .attr("integrity", "sha384-MrcW6ZMFYlzcLA8Nl+NtUVF0sA7MsXsP1UyJoMp4YLEuNSfAP+JcXn/tWtIaxVXM")
                            .attr("crossorigin", "anonymous")
                ),
                body(
                        ul(
                                each(counties, county ->
                                        countyTabHeader(county)
                                )
                        ).withClasses("nav", "nav-tabs")
                        .withRole("tablist")
                        .withId("myTab"),

                        div(
                            each(counties, county ->
                                    countyTab(county))
                        ).withClass("tab-content")
                )
        ).renderFormatted();

        //System.out.println(html);

        BufferedWriter writer = new BufferedWriter(new FileWriter("index.html"));
        writer.write(html);
        writer.close();

        System.out.println("Done");
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
        String countyPath = Analysis.dataPathWithDot + county;
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
                span(dateRangePresentable)
        ).withStyle("margin: 10px");
    }

    public static String getDateRangePresentable(String underscored) {
        String[] parts = underscored.split("_");
        return parts[0] + "/" + parts[1] + "/" + parts[2] + " " + parts[3] + " " +
                parts[4] + "/" + parts[5] + "/" + parts[6];
    }

    public static ContainerTag countyMDJs(String county) {
        return div(

        );
    }

//    public static void main (String [] args) {
//        System.out.println(test());
//
//        body(
//                ul(
//                        li("first list item"),
//                        li("second list item")
//                )
//        ).render();
//
//        System.out.println(body(
//                h1("Hello, World!"),
//                img().withSrc("/img/hello.png")
//        ).renderFormatted());
//    }
//
//    //create tab per county
//    public static void countyTab(String county) {
//
//    }
//
//    public static String test() {
//        return //document(
//                html(
//                        head(
//                                title("This is the title")
//                        ),
//                        body(
//                                ul(
//                                        li("first list item"),
//                                        li("second list item")
//                                )
//                        )
//                ).renderFormatted();
//        //);
//    }


}
