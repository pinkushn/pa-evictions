package com.lancasterstandsup.evictiondata;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
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
//        final String range = "Sheet1!A1:B";
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
//        ValueRange response = service.spreadsheets().values()
//                .get(spreadsheetId, range)
//                .execute();
//        List<List<Object>> values = response.getValues();
//        if (values == null || values.isEmpty()) {
//            System.out.println("No data found.");
//        } else {
//            System.out.println("Name, Major");
//            for (List row : values) {
//                // Print columns A and E, which correspond to indices 0 and 4.
//                System.out.printf("%s, %s\n", row.get(0), row.get(1));
//            }
//        }



//        ValueRange body = new ValueRange()
//                .setValues(Arrays.asList(
//                        Arrays.asList("Expenses January"),
//                        Arrays.asList("books", "30"),
//                        Arrays.asList("pens", "10"),
//                        Arrays.asList("Expenses February"),
//                        Arrays.asList("clothes", "20"),
//                        Arrays.asList("shoes", "5")));
//        UpdateValuesResponse result = service.spreadsheets().values()
//                .update(spreadsheetId, "A1", body)
//                .setValueInputOption("RAW")
//                .execute();



        ValueRange body = build(pdfs);

        UpdateValuesResponse result = service.spreadsheets().values()
        .update(spreadsheetId, "A1", body)
        .setValueInputOption("RAW")
        .execute();

//        Spreadsheet spreadsheet = new Spreadsheet()
//                .setProperties(new SpreadsheetProperties()
//                        .setTitle("delete me plz"));
//        spreadsheet = service.spreadsheets().create(spreadsheet)
//                .setFields("spreadsheetId")
//                .execute();
//        System.out.println("Spreadsheet ID: " + spreadsheet.getSpreadsheetId());
    }

    public static ValueRange build(List<PdfData> list) {
        ValueRange valueRange = new ValueRange();
        List<List<Object>> values = new ArrayList<>();
        values.add(Arrays.asList(Parser.colHeaders));

        for (PdfData pdf: list) {
            String[] rowData = pdf.getRow();
            List<Object> row = new ArrayList();
            for (int c = 0; c < rowData.length; c++) {
                String cellValue = rowData[c] == null ? "" : rowData[c];
                row.add(cellValue);
            }
            values.add(row);
        }

        valueRange.setValues(values);

        return valueRange;
    }
}
