package com.lancasterstandsup.evictiondata;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * Trying to write straight to Google Sheet named 'evictions'
 */
public class Sheet {
    private static final String APPLICATION_NAME = "pa-evictions";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    private static HttpRequestInitializer setHttpTimeout(final HttpRequestInitializer requestInitializer) {
        return new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest httpRequest) throws IOException {
                requestInitializer.initialize(httpRequest);
                httpRequest.setConnectTimeout(3 * 60000);  // 3 minutes connect timeout
                httpRequest.setReadTimeout(3 * 60000);  // 3 minutes read timeout
            }
        };

        //final Analytics analytics = Analytics.builder(new NetHttpTransport(), jsonFactory, setHttpTimeout(credential)).build();
    }

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = Sheet.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Prints the names and majors of students in a sample spreadsheet:
     * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
     */
    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        final String spreadsheetId = "1lDBW0R9-DzNbURzVOCf82kQbp37m7RKVlcnn03-57Gc";

        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, setHttpTimeout(getCredentials(HTTP_TRANSPORT)))
                .setApplicationName(APPLICATION_NAME)
                .build();

        String[] lancoYears =  {"2015", "2016", "2017", "2018", "2019", "2020", "2021"};
        String[] otherCountyYears =  {"2019", "2020", "2021"};

        List<List<Object>> rows =  new ArrayList<>();
        ArrayList<Object> headers = new ArrayList<>();
        headers.add(0, "County");
        for (String header: LTParser.colHeaders) {
            headers.add(header);
        }
        rows.add(headers);

        try {
            List<LTPdfData> pdfs = ParseAll.get(Scraper.Mode.MDJ_LT, "Lancaster", lancoYears);
            rows.addAll(build(pdfs, "Lancaster"));

            for (String county: Website.counties) {
                if (!county.equals("Lancaster")) {
                    rows.addAll(build(ParseAll.get(Scraper.Mode.MDJ_LT, county, otherCountyYears), county));
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("parser failed, abandoning allYears");
            e.printStackTrace();
            System.exit(1);
        }

        ValueRange body = new ValueRange();
        body.setValues(rows);

        System.out.println("Commencing upload of spreadsheet. This may take a while.");

        UpdateValuesResponse result = service.spreadsheets().values()
        .update(spreadsheetId, "A1", body)
        .setValueInputOption("USER_ENTERED")
        .execute();

        System.out.println("Finished upload of spreadsheet.");
    }

    public static List<List<Object>> build(List<LTPdfData> list, String county) {
        List<List<Object>> ret = new ArrayList<>();

        System.out.println("Starting build of Sheets ValueRange for " + county);
        for (LTPdfData pdf: list) {
            String[] rowData = pdf.getRow();
            List<Object> row = new ArrayList();
            row.add(county);
            for (int c = 0; c < rowData.length; c++) {
                String cellValue = rowData[c] == null ? "" : rowData[c];
                row.add(cellValue);
            }
            ret.add(row);
        }

        return ret;
    }
}
