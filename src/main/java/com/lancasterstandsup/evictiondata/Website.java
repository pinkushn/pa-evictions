package com.lancasterstandsup.evictiondata;

import j2html.tags.ContainerTag;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static j2html.TagCreator.*;

public class Website {

    public static String html = "";

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
                                //county("Lancaster")
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

        System.out.println(html);

        BufferedWriter writer = new BufferedWriter(new FileWriter("tabbedIndex.html"));
        writer.write(html);
        writer.close();
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
            county + " region"
        ).withClasses("tab-pane", "fade", iff(active, "show"), iff(active, "active"))
                .withId(county)
                .withRole("tabpanel")
                .attr("aria-labelledby", county + "_tab");
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
