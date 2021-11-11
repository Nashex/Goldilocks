package sheets;

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
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import lombok.AllArgsConstructor;
import main.Goldilocks;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import setup.SetupConnector;
import utils.Utils;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static main.Database.dbUrl;
import static utils.Utils.getHighestRole;
import static utils.Utils.getUnHoistedHighestRole;

public class GoogleSheets {
    private static final String APPLICATION_NAME = "Goldilocks Google Sheets";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "/data/credentials.json";

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws Exception {
        // Load client secrets.
        InputStream in = new FileInputStream(Utils.getJarContainingFolder(Goldilocks.class) + "//credentials.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8890).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getDriveCredentials(final NetHttpTransport HTTP_TRANSPORT) throws Exception {
        // Load client secrets.
        InputStream in = new FileInputStream(Utils.getJarContainingFolder(Goldilocks.class) + "//credentials.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, Collections.singletonList(DriveScopes.DRIVE))
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File("drivetokens")))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8890).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Create the sheet with the corresponding sub-sheets
     */
    private static void createSheet(Guild guild) {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            final String spreadsheetId; //This will be retrieved from the database

            Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            Spreadsheet spreadsheet = new Spreadsheet()
                    .setProperties(new SpreadsheetProperties()
                            .setTitle(guild.getName() + " Goldilocks Log"));
            spreadsheet = service.spreadsheets().create(spreadsheet)
                    .setFields("spreadsheetId")
                    .execute();

            // Create the requests List
            List<Request> requests = new ArrayList<>();

            /*
            We need to make 6 new named ranges
            -> Verifications
                -> Columns are: Mod Name, Mod Id, User Name, User Id, Option (Accept or deny), Time
            -> Runs
                -> Columns are: Rl Name, Rl Id, Raid Name, Raid Type, Time
            -> Parses
                -> Columns are: Mod Name, Mod Id, VC Name, Num Crashers, Time
            -> Punishments
                -> Columns are: Mod Name, Mod Id, User Name, User Id, Reason, Time
            -> Assists
                -> Columns are: Mod Name, Mod Id, VC Name, Time
            -> Commands
                -> Columns are: Mod Name, Mod Id, Channel Id, Alias, Command String, Time
             */

            // Set Verifications Named Range and Sheet
            requests.add(new Request().setUpdateSheetProperties(
                    new UpdateSheetPropertiesRequest().setProperties(new SheetProperties().setTitle("README")).setFields("title")));

            requests.addAll(Arrays.asList(
                    sheetRequest(1, "Verifications", new java.awt.Color(255, 122, 122)),
                    sheetRequest(2, "Raids", new java.awt.Color(52, 255, 0)),
                    sheetRequest(3, "Parses", new java.awt.Color(75, 146, 255)),
                    sheetRequest(4, "Punishments", new java.awt.Color(227, 102, 255)),
                    sheetRequest(5, "Assists", new java.awt.Color(255, 246, 76)),
                    sheetRequest(6, "Commands", new java.awt.Color(255, 126, 75)),
                    sheetRequest(7, "Current Staff", new java.awt.Color(108, 75, 255))
                    //sheetRequest(7, "On Leave", new java.awt.Color(255, 20, 20))
            ));

            requests.addAll(Arrays.asList(
                    namedRangeRequest(1, 10, "verifications"),
                    namedRangeRequest(2, 10, "raids"),
                    namedRangeRequest(3, 10, "parses"),
                    namedRangeRequest(4, 10, "punishments"),
                    namedRangeRequest(5, 10, "assists"),
                    namedRangeRequest(6, 10, "commands"),
                    namedRangeRequest(7, 10, "staff")
                    //namedRangeRequest(8, 10, "onleave")
            ));

            requests.addAll(Arrays.asList(
                    new Request().setAddProtectedRange(new AddProtectedRangeRequest().setProtectedRange(new ProtectedRange().setWarningOnly(true).setRange(new GridRange().setSheetId(SheetsLogType.VERIFICATIONS.sheetId)))),
                    new Request().setAddProtectedRange(new AddProtectedRangeRequest().setProtectedRange(new ProtectedRange().setWarningOnly(true).setRange(new GridRange().setSheetId(SheetsLogType.RAIDS.sheetId)))),
                    new Request().setAddProtectedRange(new AddProtectedRangeRequest().setProtectedRange(new ProtectedRange().setWarningOnly(true).setRange(new GridRange().setSheetId(SheetsLogType.PARSES.sheetId)))),
                    new Request().setAddProtectedRange(new AddProtectedRangeRequest().setProtectedRange(new ProtectedRange().setWarningOnly(true).setRange(new GridRange().setSheetId(SheetsLogType.PUNISHMENTS.sheetId)))),
                    new Request().setAddProtectedRange(new AddProtectedRangeRequest().setProtectedRange(new ProtectedRange().setWarningOnly(true).setRange(new GridRange().setSheetId(SheetsLogType.ASSISTS.sheetId)))),
                    new Request().setAddProtectedRange(new AddProtectedRangeRequest().setProtectedRange(new ProtectedRange().setWarningOnly(true).setRange(new GridRange().setSheetId(SheetsLogType.COMMANDS.sheetId)))),
                    new Request().setAddProtectedRange(new AddProtectedRangeRequest().setProtectedRange(new ProtectedRange().setWarningOnly(true).setRange(new GridRange().setStartColumnIndex(0).setEndColumnIndex(2).setSheetId(SheetsLogType.STAFF.sheetId))))
            ));

            BatchUpdateSpreadsheetRequest body =
                    new BatchUpdateSpreadsheetRequest().setRequests(requests);
            spreadsheetId = spreadsheet.getSpreadsheetId();
            service.spreadsheets().batchUpdate(spreadsheetId, body).execute();


            List<List<Object>> verifications = Collections.singletonList(Arrays.asList("Mod Name", "Mod Id", "User Name", "User Id", "Decision", "Time"));
            List<List<Object>> raids = Collections.singletonList(Arrays.asList("Leader Name", "Leader Id", "Raid Name", "Raid Type", "Time"));
            List<List<Object>> parses = Collections.singletonList(Arrays.asList("Mod Name", "Mod Id", "Voicechannel Name", "Num Crashers" ,"Time")); // TODO Add number of people in the parse
            List<List<Object>> punishments = Collections.singletonList(Arrays.asList("Mod Name", "Mod Id", "User Name", "User Id", "Type", "Reason", "Time"));
            List<List<Object>> assists = Collections.singletonList(Arrays.asList("Mod Name", "Mod Id", "Leader Name", "Leader Id", "Time"));
            List<List<Object>> commands = Collections.singletonList(Arrays.asList("User Name", "User Id", "Channel Id", "Alias", "Command String", "Time"));
            List<List<Object>> staff = Collections.singletonList(Arrays.asList("Name", "Id", "Highest Rank", "On Leave", "Started"));
            //List<List<Object>> onLeave = Collections.singletonList(Arrays.asList("Name", "Id", "Started"));

            service.spreadsheets().values().append(spreadsheetId, "verifications", new ValueRange().setValues(verifications))
                    .setValueInputOption("USER_ENTERED").execute();
            service.spreadsheets().values().append(spreadsheetId, "raids", new ValueRange().setValues(raids))
                    .setValueInputOption("USER_ENTERED").execute();
            service.spreadsheets().values().append(spreadsheetId, "parses", new ValueRange().setValues(parses))
                    .setValueInputOption("USER_ENTERED").execute();
            service.spreadsheets().values().append(spreadsheetId, "punishments", new ValueRange().setValues(punishments))
                    .setValueInputOption("USER_ENTERED").execute();
            service.spreadsheets().values().append(spreadsheetId, "assists", new ValueRange().setValues(assists))
                    .setValueInputOption("USER_ENTERED").execute();
            service.spreadsheets().values().append(spreadsheetId, "commands", new ValueRange().setValues(commands))
                    .setValueInputOption("USER_ENTERED").execute();
            service.spreadsheets().values().append(spreadsheetId, "staff", new ValueRange().setValues(staff))
                    .setValueInputOption("USER_ENTERED").execute();
//            service.spreadsheets().values().append(spreadsheetId, "onleave", new ValueRange().setValues(staff))
//                    .setValueInputOption("USER_ENTERED").execute();


            String sql = "INSERT INTO spreadsheetIds VALUES (" + guild.getId() + ", '" + StringEscapeUtils.escapeSql(spreadsheet.getSpreadsheetId()) + "')";
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 Statement stmt = conn.createStatement()){
                stmt.executeUpdate(sql);
            } catch (Exception e) {
                System.out.println("SPREADSHEET KEY INSERT | Database connection error. " + guild.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Request sheetRequest(int sheetId, String title, java.awt.Color color) {
        return new Request().setAddSheet(new AddSheetRequest()
                .setProperties(new SheetProperties()
                        .setTitle(title)
                        .setTabColor(new Color().setBlue(color.getBlue() + 0.0f)
                                .setGreen(color.getGreen() + 0.0f)
                                .setRed(color.getRed() + 0.0f)
                                .setAlpha(color.getAlpha() + 0.0f))
                        .setSheetId(sheetId)
                )
        );
    }

    private static Request namedRangeRequest(int sheetId, int numColumns, String title) {
        return new Request().setAddNamedRange(new AddNamedRangeRequest()
                .setNamedRange(
                        new NamedRange()
                                .setRange(new GridRange().setSheetId(sheetId).setStartColumnIndex(0).setEndColumnIndex(numColumns).setStartRowIndex(0).setEndRowIndex(1))
                                .setName(title)
                ));
    }

    public static void updateGuildStaff(Guild guild) {
        String spreadSheetId;
        if ((spreadSheetId = getSpreadsheetId(guild.getId())).isEmpty()) {
            System.out.println("Creating sheet for " + guild.getName());
            createSheet(guild);
            return;
        }

        Goldilocks.TIMER.execute(() -> {
            try {
                final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

                Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                        .setApplicationName(APPLICATION_NAME)
                        .build();

                String range = "'Current Staff'!A2:E1000";

                ClearValuesRequest requestBody = new ClearValuesRequest();

                Sheets.Spreadsheets.Values.Clear request =
                        service.spreadsheets().values().clear(spreadSheetId, range, requestBody);
                request.execute();

                String onLeaveId = SetupConnector.getFieldValue(guild, "guildInfo","onLeaveRole");
                Role onLeaveRole;
                if (!onLeaveId.equals("0")) {
                    onLeaveRole = Goldilocks.jda.getRoleById(onLeaveId);
                    if (onLeaveRole == null) return;
                } else return;

                List<Member> staff = guild.getMembers().stream().filter(member -> member.getRoles().stream().filter(Role::isHoisted).count() > 1).filter(m -> getUnHoistedHighestRole(m).getPosition() >= onLeaveRole.getPosition())
                        .sorted((m1, m2) -> getHighestRole(m2).getPosition() - getHighestRole(m1).getPosition()).collect(Collectors.toList());
                List<List<Object>> values = staff.stream().map(m -> Arrays.asList((Object) m.getEffectiveName(), m.getId(), getHighestRole(m).getName(), m.getRoles().contains(onLeaveRole) ? "Y" : "N")).collect(Collectors.toList());

                AppendValuesResponse response = service.spreadsheets().values()
                        .append(spreadSheetId, "staff", new ValueRange().setValues(values))
                        .setValueInputOption("RAW")
                        .execute();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void addEmail(TextChannel textChannel, String email) {
        Guild guild = textChannel.getGuild();
        String spreadSheetId;
        if ((spreadSheetId = getSpreadsheetId(guild.getId())).isEmpty()) {
            System.out.println("Creating sheet for " + guild.getName());
            createSheet(guild);
            return;
        }

        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getDriveCredentials(HTTP_TRANSPORT))
                    .setApplicationName("Goldilocks Google Drives")
                    .build();

            Permission permission = service.permissions().create(spreadSheetId, new Permission().setType("user").setRole("writer").setEmailAddress(email)).execute();
            textChannel.sendMessage("Successfully added " + email + " to the Spreadsheet!").queue();

        } catch (Exception e) {
            textChannel.sendMessage("Unable to add " + email + " to the Spreadsheet, please make sure it is a valid email.").queue();
        }
    }

    public static void logEvent(Guild guild, SheetsLogType sheetsLogType, String... fields) {
        String spreadSheetId;
        if ((spreadSheetId = getSpreadsheetId(guild.getId())).isEmpty()) {
            System.out.println("Creating sheet for " + guild.getName());
            createSheet(guild);
            return;
        }

        Goldilocks.TIMER.execute(() -> {
            try {
                final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

                Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                        .setApplicationName(APPLICATION_NAME)
                        .build();

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                //Escape the rl name
                fields[0] = "'" + fields[0];

                List<String> fieldList = new ArrayList<>(Arrays.asList(fields));
                fieldList.add(format.format(new Date(System.currentTimeMillis())));

                List<List<Object>> values = Collections.singletonList(
                        Arrays.asList(fieldList.toArray())
                );

                AppendValuesResponse response = service.spreadsheets().values()
                        .append(spreadSheetId, sheetsLogType.title, new ValueRange().setValues(values))
                        .setValueInputOption("USER_ENTERED")
                        .execute();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static String logEvent(String guildId, SheetsLogType sheetsLogType, String... fields) {
        String spreadSheetId;
        if ((spreadSheetId = getSpreadsheetId(guildId)).isEmpty()) return "Google Sheet Does not Exist";

        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            //Escape the rl name
            fields[0] = "'" + fields[0];

            List<String> fieldList = new ArrayList<>(Arrays.asList(fields));
            fieldList.add(format.format(new Date(System.currentTimeMillis())));

            List<List<Object>> values = Collections.singletonList(
                    Arrays.asList(fieldList.toArray())
            );

            AppendValuesResponse response = service.spreadsheets().values()
                    .append(spreadSheetId, sheetsLogType.title, new ValueRange().setValues(values))
                    .setValueInputOption("USER_ENTERED")
                    .execute();

        } catch (Exception e) {
            return "Failed to Log Event";
        }

        return "Successfully Logged Raid!";
    }

    public static String getSpreadsheetId(String guildId) {
        String sql = "SELECT * FROM spreadsheetIds WHERE guildId = " + guildId;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);
            if (resultSet.next()) {
                return resultSet.getString("spreadsheetId");
            }
        } catch (Exception e) {
            System.out.println("SPREADSHEET | Database connection error.");
        }
        return "";
    }

    public static void main(String... args) throws Exception {

    }

    @AllArgsConstructor
    public enum SheetsLogType {
        VERIFICATIONS("verifications", 1),
        RAIDS("raids", 2),
        PARSES("parses", 3),
        PUNISHMENTS("punishments", 4),
        ASSISTS("assists", 5),
        COMMANDS("commands", 6),
        STAFF("staff", 7),
        ONLEAVE("onleave", 8);

        public String title;
        public int sheetId;
    }
}