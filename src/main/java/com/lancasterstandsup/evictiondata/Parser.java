package com.lancasterstandsup.evictiondata;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface Parser {
    PdfData process(InputStream pdfStream, boolean printAll) throws IOException, NoSuchFieldException;

    PdfData processFile(File file) throws IOException, NoSuchFieldException;

}
