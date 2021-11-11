package main;

import giveaways.Giveaway;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.ocpsoft.prettytime.PrettyTime;
import quota.LogField;
import raids.LogPanel;
import setup.KeySection;
import verification.AddAltRequest;
import verification.ExaltVerificationRequest;
import verification.ManualVerificationRequest;
import verification.VerificationHub;

import javax.annotation.Nullable;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;
import java.util.stream.Collectors;

import static verification.VerificationHub.manualVerificationRequests;

public class Database {
    private static Connection c = null;
    private static Statement statement = null;
    public static String dbUrl = "jdbc:sqlite:database.db";
    public static String messageLogdbUrl = "jdbc:sqlite:messageLogs.db";

    public static void testConnection() {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            System.out.println("Database successfully connected.");
        } catch (Exception e) {
            System.out.println("Database connection error.");
        }
    }

    public static void executeUpdate(String string) {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            stmt.executeUpdate(string);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Database connection error.");
        }
    }

    public static List<String> getDistinctFields(Guild guild) {
        String sql = "SELECT DISTINCT field from logData WHERE guildId = " + guild.getId();
        List<String> fields = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            ResultSet resultSet = stmt.executeQuery(sql);

            while (resultSet.next()) {
                fields.add(resultSet.getString("field"));
            }

        } catch (Exception e) {
            System.out.println("Database connection error.");
        }

        return fields;

    }

    public static List<LogField> executeLogQueries(Guild guild) {
        List<LogField> fields = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery("SELECT * from userStats WHERE guildId = " + guild.getId());
            ResultSetMetaData rsmd = resultSet.getMetaData();

            int numCols = rsmd.getColumnCount();
            List<String> colNames = new ArrayList<>();
            for (int i = 3; i <= numCols; i++) {
                colNames.add(rsmd.getColumnName(i));
            }

            for (String s : colNames) {
                resultSet = stmt.executeQuery("SELECT SUM(" + s + ") from userStats WHERE guildId = " + guild.getId());
                if (resultSet.next()) {
                    fields.add(new LogField(s, resultSet.getInt(1)));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Database connection error.");
        }

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery("SELECT * from rlStats WHERE guildId = " + guild.getId());
            ResultSetMetaData rsmd = resultSet.getMetaData();

            int numCols = rsmd.getColumnCount();
            List<String> colNames = new ArrayList<>();
            for (int i = 3; i <= numCols; i++) {
                colNames.add(rsmd.getColumnName(i));
            }

            for (String s : colNames) {
                resultSet = stmt.executeQuery("SELECT SUM(" + s + ") from rlStats WHERE guildId =" + guild.getId());
                if (resultSet.next()) {
                    fields.add(new LogField(s, resultSet.getInt(1)));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Database connection error.");
        }
        return fields;
    }


    public static boolean guildExists(String guildId) {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            ResultSet resultSet = stmt.executeQuery("SELECT commandPrefix FROM guildInfo WHERE guildId = " + guildId);

            if (resultSet.next()) {
                return true;
            } else {
                return false;
            }

        } catch (Exception e) {
            System.out.println("Database connection error.");
        }
        return false;
    }

    public static String getLockdown(TextChannel textChannel) {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            ResultSet resultSet = stmt.executeQuery("SELECT permissionString FROM lockdowns WHERE channelId = " + textChannel.getId());

            if (resultSet.next()) {
                return resultSet.getString("permissionString");
            }

        } catch (Exception e) {
            System.out.println("Database connection error.");
        }
        return "";
    }

    public static void logLockdown(TextChannel textChannel, String perms) {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            stmt.executeUpdate("INSERT INTO lockdowns (channelId, permissionString) VALUES (" + textChannel.getId() + ", '" + perms + "')") ;

        } catch (Exception e) {
            System.out.println("Database connection error.");
        }
    }

    public static void removeLockdown(TextChannel textChannel) {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            stmt.executeUpdate("DELETE FROM lockdowns where channelId = " + textChannel.getId()) ;

        } catch (Exception e) {
            System.out.println("Database connection error.");
        }
    }

    public static void createGuild(String guildId) {
        String sql = "INSERT INTO guildInfo (guildId) VALUES (?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, guildId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getGuildPrefix(String guildId) {

        if (!guildExists(guildId)) {
            createGuild(guildId);
        }

        String sql = "SELECT commandPrefix FROM guildInfo WHERE guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, guildId);
            ResultSet resultSet = pstmt.executeQuery();

            return resultSet.getString("commandPrefix");

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ";";
    }

    public static Category getModmailCategory(Guild guild) {

        if (!guildExists(guild.getId())) {
            createGuild(guild.getId());
        }

        String sql = "SELECT modmailCategoryId FROM guildLogs WHERE guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, guild.getId());
            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) {
                String categoryId = resultSet.getString("modmailCategoryId");
                try {
                    if (!categoryId.equals("0")) return Goldilocks.jda.getCategoryById(resultSet.getLong("modmailCategoryId"));
                } catch (Exception e) {
                    conn.close();
                    executeUpdate("UPDATE guildLogs SET modmailCategoryId = 0 WHERE guildId = " + guild.getId());
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static TextChannel getModmailLogChannel(Guild guild) {

        if (!guildExists(guild.getId())) {
            createGuild(guild.getId());
        }

        String sql = "SELECT modmailLogChannelId FROM guildLogs WHERE guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, guild.getId());
            ResultSet resultSet = pstmt.executeQuery();

            long categoryId = resultSet.getLong("modmailLogChannelId");
            try {
                if (categoryId != 0L) return Goldilocks.jda.getTextChannelById(resultSet.getLong("modmailLogChannelId"));
            } catch (Exception e) {
                conn.close();
                executeUpdate("UPDATE guildLogs SET modmailLogChannelId = 0 WHERE guildId = " + guild.getId());
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void updateGuildInfo(String guildId, String field, String value, boolean add) {
        if (!guildExists(guildId)) {
            createGuild(guildId);
        }

        //System.out.println(value);
        if (add) {
            value += getGuildInfo(guildId).get(4);
        }

        String sql = "UPDATE guildInfo SET " + field + " = '" + value + "' WHERE guildId = " + guildId;


        //System.out.println(sql);

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.executeUpdate();
            //System.out.println(field + " " + value);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getGuildInfo(Guild guild, String field) {
        if (!guildExists(guild.getId())) {
            createGuild(guild.getId());
        }

        String sql = "SELECT " + field + " from guildInfo WHERE guildId = " + guild.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();) {

            ResultSet rs = stmt.executeQuery(sql);
            return rs.getString(1);
            //System.out.println(field + " " + value);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "";
    }

    //Todo add the suspended role to the setup panel
    public static List<String> getGuildInfo(String guildId) {

        List<String> guildInfo = new ArrayList<>();

        if (!guildExists(guildId)) {
            createGuild(guildId);
        }

        String sql = "SELECT * FROM guildInfo WHERE guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, guildId);
            ResultSet resultSet = pstmt.executeQuery();

            guildInfo.add(resultSet.getString("commandPrefix"));

            guildInfo.add(resultSet.getString("mmCategory"));

            guildInfo.add(resultSet.getString("commandChannels"));

            //Roles
            guildInfo.add(resultSet.getString("verifiedRole"));
            guildInfo.add(resultSet.getString("veteranRole"));

            guildInfo.add(resultSet.getString("headRlRole"));
            guildInfo.add(resultSet.getString("vetRlRole"));
            guildInfo.add(resultSet.getString("rlRole"));
            guildInfo.add(resultSet.getString("arlRole"));
            guildInfo.add(resultSet.getString("trlRole"));

            guildInfo.add(resultSet.getString("modRole"));
            guildInfo.add(resultSet.getString("officerRole"));
            guildInfo.add(resultSet.getString("securityRole"));
            guildInfo.add(resultSet.getString("tSecRole"));

            guildInfo.add(resultSet.getString("headEoRole"));
            guildInfo.add(resultSet.getString("vetEoRole"));
            guildInfo.add(resultSet.getString("eoRole"));

            guildInfo.add(resultSet.getString("verificationChannel"));
            return guildInfo;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return guildInfo;
    }

    public static Role getSuspendedRole(String guildId) {
        Role suspendedRole = null;

        String sql = "SELECT suspendedRoleId FROM guildInfo WHERE guildId = " + guildId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);
            String suspendedRoleId = resultSet.getString("suspendedRoleId");
            if (Long.parseLong(suspendedRoleId) != 0) {
                suspendedRole = Goldilocks.jda.getRoleById(suspendedRoleId);
            }
            return suspendedRole;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return suspendedRole;
    }

    public static List<Role> getEligibleRoles(String guildId, String[] roles) {

        List<String> eligibleRoleNames = new ArrayList<>();
        List<Role> eligibleRoles = new ArrayList<>();

        if (!guildExists(guildId)) createGuild(guildId);

        String sql = "SELECT * FROM guildInfo WHERE guildId = ?";

        for (String role : roles) {
            if (role.equals("eo")) eligibleRoleNames.addAll(Arrays.asList("eo", "vetEo", "headEo", "rl", "vetRl", "headRl", "developer"));

            if (role.equals("vetEo")) eligibleRoleNames.addAll(Arrays.asList("vetEo", "headEo", "headRl"));

            if (role.equals("headEo")) eligibleRoleNames.addAll(Arrays.asList("headEo", "headRl"));

            if (role.equals("verified")) eligibleRoleNames.addAll(Arrays.asList("verified", "trl", "arl", "rl", "vetRl", "headRl", "tSec", "security", "officer", "mod", "eo", "vetEo", "headEo","developer"));

            if (role.equals("trl")) eligibleRoleNames.addAll(Arrays.asList("trl", "arl", "rl", "vetRl", "headRl", "developer"));

            if (role.equals("arl")) eligibleRoleNames.addAll(Arrays.asList("arl", "rl", "vetRl", "headRl", "developer"));

            if (role.equals("rl")) eligibleRoleNames.addAll(Arrays.asList("rl", "vetRl", "headRl", "developer"));

            if (role.equals("vetRl")) eligibleRoleNames.addAll(Arrays.asList("vetRl", "headRl", "developer"));

            if (role.equals("hrl")) eligibleRoleNames.addAll(Arrays.asList( "headRl", "developer"));

            if (role.equals("tSec")) eligibleRoleNames.addAll(Arrays.asList("tSec", "security", "officer", "mod", "developer"));

            if (role.equals("security")) eligibleRoleNames.addAll(Arrays.asList("security", "officer", "mod", "developer"));

            if (role.equals("officer")) eligibleRoleNames.addAll(Arrays.asList("officer", "mod", "developer"));

            if (role.equals("mod")) eligibleRoleNames.add("mod");

            if (role.equals("game")) eligibleRoleNames.add("game");
        }
        eligibleRoleNames.add("admin");

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, guildId);
            ResultSet resultSet = pstmt.executeQuery();

            eligibleRoles = eligibleRoleNames.stream().map(s -> {
                try {
                    if (resultSet.getLong(s + "Role") != 0) {
                       Role currentRole;
                       if ((currentRole = Goldilocks.jda.getRoleById(resultSet.getLong(s + "Role"))) != null) return currentRole;
                    }
                } catch (Exception ignored) { }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return eligibleRoles;
    }

    public static List<Role> getRaiderRoles(Guild guild) {
        List<Role> raiderRoles = new ArrayList<>();

        String sql = "SELECT raidRoleId FROM raidSections WHERE guildId = " + guild.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);

            while (resultSet.next()) {
                raiderRoles.add(Goldilocks.jda.getRoleById(resultSet.getString("raidRoleId")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return raiderRoles;
    }

    public static void addRaidCategory(String guildId, String raidCategoryId, String raidRoleId, String raidCommandChannelId, String raidStatusChannel, String defaultRaidId) {

        String sql = "INSERT INTO raidSections (guildId,raidCategoryId,raidRoleId,raidCommandChannelId,raidStatusChannel,defaultRaidId,raidControlCategory) VALUES (?,?,?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, guildId);
            pstmt.setString(2, raidCategoryId);
            pstmt.setString(3, raidRoleId);
            pstmt.setString(4, raidCommandChannelId);
            pstmt.setString(5, raidStatusChannel);
            pstmt.setString(6, defaultRaidId);
            pstmt.setString(7, "0");

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void expelMember(Member member, String username, Member mod, String reason) {
        expelAdd(member.getId(), username, mod, reason);
    }

    public static boolean expelAdd(String id, String username, Member mod, String reason) {

        String sql = "INSERT INTO expelledList (guildId,userId,name,modId,reason,timeDone) VALUES (?,?,?,?,?, dateTime('now', '-6 hours'))";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, mod.getGuild().getId());
            pstmt.setString(2, id);
            pstmt.setString(3, username);
            pstmt.setString(4, mod.getId());
            pstmt.setString(5, reason);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean expelRemove(String string, Guild guild) {
        String sql = "DELETE FROM expelledList WHERE name = ? OR userId = ? AND guildId = " + guild.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, string);
            pstmt.setString(2, string);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String isExpelled(String string, Guild guild) {
        String sql = "SELECT * FROM expelledList WHERE (name = ? OR userID = ?) AND guildId = '" + guild.getId() + "'";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, string.toLowerCase());
            pstmt.setString(2, string.toLowerCase());

            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) return "";

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String time = rs.getString("timeDone");
            Date date = sdf.parse(time);

            SimpleDateFormat timeFormat = new SimpleDateFormat("MM-dd-yyyy");

            return "This user was expelled by <@" + rs.getString("modId") + "> " + new PrettyTime().format(date) + " (" + timeFormat.format(date) + ") for \n> " + rs.getString("reason") ;

        } catch (SQLException | ParseException e) {
            e.printStackTrace();
            return "Error Retrieving Expulsion";
        }
    }

    public static void modMailBlacklist(User user, Member mod, String reason) {

        String sql = "INSERT INTO modMailBlacklist (guildId,userId,modId,reason) VALUES (?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, mod.getGuild().getId());
            pstmt.setString(2, user.getId());
            pstmt.setString(3, mod.getId());
            pstmt.setString(4, reason);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean isModmailBlacklisted(Member member) {
        String sql = "SELECT * FROM modMailBlacklist WHERE userId = " + member.getId() + " AND guildId = " + member.getGuild().getId();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();) {
            ResultSet rs = stmt.executeQuery(sql);
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String[] getExpel(String name, Guild guild) {
        String[] expel = new String[3];
        String sql = "SELECT * FROM expelledList WHERE name = '" + name + "' OR userID = '" + name + "' AND guildId = " + guild.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();) {

            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                expel[0] = rs.getString("name");
                expel[1] = rs.getString("userId");
                expel[2] = rs.getString("modId");
                return expel;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new String[]{};
    }

    public static String isExpelled(Member member) {
        String sql = "SELECT * FROM expelledList WHERE userId = " + member.getId() + " AND guildId = " + member.getGuild().getId();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();) {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) return rs.getString("modId");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void addKeySection(KeySection keySection) {

        String sql = "INSERT INTO keySections (guildId,roleId,keyRequirement,leaderboardRole,uniqueMessage) VALUES (?,?,?,?,?)";

        if (keySectionExists(keySection.getKeyRole()))
            sql = "UPDATE keySections SET guildId = ?, roleId = ?, keyRequirement = ?, leaderboardRole = ?, uniqueMessage = ? WHERE roleId = " + keySection.getKeyRole().getId();


        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, keySection.getGuild().getId());
            pstmt.setString(2, keySection.getKeyRole().getId());
            pstmt.setString(3, String.valueOf(keySection.getKeyAmount()));
            pstmt.setString(4, String.valueOf(keySection.isLeaderboardRole()));
            pstmt.setString(5, keySection.getUniqueMessage());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean keySectionExists(Role keyRole) {
        return !(getKeySection(keyRole.getId()).getKeyAmount() == -1);
    }

    public static KeySection getKeySection(String roleId) {
        KeySection keySection = new KeySection();

        String sql = "SELECT * FROM keySections WHERE roleId = " + roleId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);

            while (resultSet.next()) {

                keySection.setGuild(Goldilocks.jda.getGuildById(resultSet.getString("guildId")));
                keySection.setKeyRole(Goldilocks.jda.getRoleById(resultSet.getString("roleId")));
                keySection.setKeyAmount(Integer.parseInt(resultSet.getString("keyRequirement")));
                keySection.setLeaderboardRole(Boolean.parseBoolean(resultSet.getString("leaderboardRole")));
                keySection.setUniqueMessage(resultSet.getString("uniqueMessage"));

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return keySection;
    }

    public static List<Role> eligibleKeyRoles(int keyAmount, String guildId) {
        List<Role> roleList = new ArrayList<>();

        String sql = "SELECT * FROM keySections WHERE keyRequirement < " + keyAmount + " AND guildId = " + guildId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);

            while (resultSet.next()) {

                roleList.add(Goldilocks.jda.getRoleById(resultSet.getString("roleId")));

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return roleList;
    }

    public static String getKeyRoleMessage(Role role) {
        String keyRoleMessage = "";

        String sql = "SELECT uniqueMessage FROM keySections WHERE roleId = " + role.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);

            while (resultSet.next()) {

                keyRoleMessage += resultSet.getString("uniqueMessage");

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return keyRoleMessage;
    }

    public static void addVerificationSection(String guildId, String verificationChannelId, String verificationRoleId, int starRequirement, int fameRequirement, int statsRequirement, int runsRequirement) {

        String sql = "INSERT INTO verificationSections (guildId,verificationChannelId,verificationRoleId,starRequirement,fameRequirement,statsRequirement,runsRequirement) VALUES (?,?,?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, guildId);
            pstmt.setString(2, verificationChannelId);
            pstmt.setString(3, verificationRoleId);
            pstmt.setInt(4, starRequirement);
            pstmt.setInt(5, fameRequirement);
            pstmt.setInt(6, statsRequirement);
            pstmt.setInt(7, runsRequirement);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void logEvent(Member member, EventType eventType, Long time, TextChannel textChannel, String commandString) {
        String sql = "INSERT INTO eventLog(guildId,userId,type,time,channelId,alias) VALUES(?,?,?,(SELECT DATETIME('" + time
                + "','unixepoch')),?,?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, member.getGuild().getId());
            pstmt.setString(2, member.getId());
            pstmt.setString(3, eventType.type);
            pstmt.setString(4, textChannel == null ? "0" : textChannel.getId());
            pstmt.setString(5, commandString);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void logPing() {
        String sql = "INSERT INTO eventLog(guildId,userId,type,time,channelId,alias) VALUES(?,?,?,(SELECT DATETIME('" +
                (System.currentTimeMillis() / 1000) + "','unixepoch')),?,?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1,"0");
            pstmt.setString(2, Goldilocks.jda.getSelfUser().getId());
            pstmt.setString(3, "ping");
            pstmt.setString(4, "0");
            pstmt.setString(5, Goldilocks.jda.getGatewayPing() + "");
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void logMessage(Message message) {
        String sql = "INSERT INTO messageLog(guildId,userId,messageId,textChannelId,content,time) VALUES(?,?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(messageLogdbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, message.getGuild().getId());
            pstmt.setString(2, message.getAuthor().getId());
            pstmt.setString(3, message.getId());
            pstmt.setString(4, message.getTextChannel().getId());
            pstmt.setString(5, message.getContentRaw());
            pstmt.setTime(6, new Time(message.getTimeCreated().toInstant().toEpochMilli() + 7200000));
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void logMessages(List<Message> messages) {
        String valuesString = messages.stream()
                .map(m -> "(" + m.getGuild().getId() + "," + m.getAuthor().getId() + "," + m.getId() + "," + m.getTextChannel().getId() + ",'" + StringEscapeUtils.escapeSql(m.getContentRaw()) + "'," + m.getTimeCreated().toInstant().toEpochMilli() + 7200000 + ")")
                .collect(Collectors.joining(","));
        String sql = "INSERT INTO messageLog(guildId,userId,messageId,textChannelId,content,time) VALUES " + valuesString;

        try (Connection conn = DriverManager.getConnection(messageLogdbUrl);
             Statement stmt = conn.createStatement();) {

            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateMessage(Message message) {
        String sql = "UPDATE messageLog SET content = '" + StringEscapeUtils.escapeSql(message.getContentRaw()) + "' WHERE messageId = " + message.getId();

        try (Connection conn = DriverManager.getConnection(messageLogdbUrl);
             Statement stmt = conn.createStatement();) {
            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getMessageContent(String messageId) {
        String sql = "Select content FROM messageLog WHERE messageId = " + messageId;

        try (Connection conn = DriverManager.getConnection(messageLogdbUrl);
             Statement stmt = conn.createStatement();) {

            ResultSet resultSet = stmt.executeQuery(sql);
            if (resultSet.next()) return resultSet.getString(1);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String[] getMessageInfo(String messageId) {
        String sql = "Select userId,content,textChannelId FROM messageLog WHERE messageId = " + messageId;

        try (Connection conn = DriverManager.getConnection(messageLogdbUrl);
             Statement stmt = conn.createStatement();) {

            ResultSet resultSet = stmt.executeQuery(sql);
            if (resultSet.next()) {
                return new String[]{resultSet.getString("userId"),
                        resultSet.getString("content"),
                        resultSet.getString("textChannelId")};
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new String[]{};
    }

    public static String getData(EventType eventType, Guild guild) {
        String sql = "SELECT * FROM" +
                   " (SELECT count(userId) AS '1' FROM eventLog WHERE date(time) = date('now', '-6 days') AND type = '" + eventType.type + "' AND guildId = " + guild.getId() + ")," +
                   " (SELECT count(userId) AS '2' FROM eventLog WHERE date(time) = date('now', '-5 days') AND type = '" + eventType.type + "' AND guildId = " + guild.getId() + ")," +
                   " (SELECT count(userId) AS '3' FROM eventLog WHERE date(time) = date('now', '-4 days') AND type = '" + eventType.type + "' AND guildId = " + guild.getId() + ")," +
                   " (SELECT count(userId) AS '4' FROM eventLog WHERE date(time) = date('now', '-3 days') AND type = '" + eventType.type + "' AND guildId = " + guild.getId() + ")," +
                   " (SELECT count(userId) AS '5' FROM eventLog WHERE date(time) = date('now', '-2 days') AND type = '" + eventType.type + "' AND guildId = " + guild.getId() + ")," +
                   " (SELECT count(userId) AS '6' FROM eventLog WHERE date(time) = date('now', '-1 days') AND type = '" + eventType.type + "' AND guildId = " + guild.getId() + ")," +
                   " (SELECT count(userId) AS '7' FROM eventLog WHERE date(time) = date('now') AND type = '" + eventType.type + "' AND guildId = " + guild.getId() + ")";
        String dataString = "";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();) {

            ResultSet resultSet = stmt.executeQuery(sql);
            dataString += "[" + resultSet.getInt(1) + "," +
                    resultSet.getInt(2) + "," +
                    resultSet.getInt(3) + "," +
                    resultSet.getInt(4) + "," +
                    resultSet.getInt(5) + "," +
                    resultSet.getInt(6) + "," +
                    resultSet.getInt(7) + "]";

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return dataString;
    }

    public static String getData(EventType eventType, Member member) {
        Guild guild = member.getGuild();
        String sql = "SELECT * FROM" +
                " (SELECT count(userId) AS '1' FROM eventLog WHERE date(time) = date('now', '-7 days') AND type = '" + eventType.type + "' AND guildId = " + guild.getId() + " AND userId = " + member.getId() + ")," +
                " (SELECT count(userId) AS '2' FROM eventLog WHERE date(time) = date('now', '-6 days') AND type = '" + eventType.type + "' AND guildId = " + guild.getId() + " AND userId = " + member.getId() + ")," +
                " (SELECT count(userId) AS '3' FROM eventLog WHERE date(time) = date('now', '-5 days') AND type = '" + eventType.type + "' AND guildId = " + guild.getId() + " AND userId = " + member.getId() + ")," +
                " (SELECT count(userId) AS '4' FROM eventLog WHERE date(time) = date('now', '-4 days') AND type = '" + eventType.type + "' AND guildId = " + guild.getId() + " AND userId = " + member.getId() + ")," +
                " (SELECT count(userId) AS '5' FROM eventLog WHERE date(time) = date('now', '-3 days') AND type = '" + eventType.type + "' AND guildId = " + guild.getId() + " AND userId = " + member.getId() + ")," +
                " (SELECT count(userId) AS '6' FROM eventLog WHERE date(time) = date('now', '-2 days') AND type = '" + eventType.type + "' AND guildId = " + guild.getId() + " AND userId = " + member.getId() + ")," +
                " (SELECT count(userId) AS '7' FROM eventLog WHERE date(time) = date('now', '-1 days') AND type = '" + eventType.type + "' AND guildId = " + guild.getId() + " AND userId = " + member.getId() + ")";
        String dataString = "";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();) {

            ResultSet resultSet = stmt.executeQuery(sql);
            dataString += "[" + resultSet.getInt(1) + "," +
                    resultSet.getInt(2) + "," +
                    resultSet.getInt(3) + "," +
                    resultSet.getInt(4) + "," +
                    resultSet.getInt(5) + "," +
                    resultSet.getInt(6) + "," +
                    resultSet.getInt(7) + "]";

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dataString;
    }

    @AllArgsConstructor
    public enum EventType {
        PING("ping"),
        PARSE("parse"),
        RAID("raid"),
        COMMAND("command"),
        ADDALT("addalt"),
        NAMECHANGE("namechange"),
        VERIFICATION("verification"),
        MODMAIL("modmail"),
        ASSIST("assist");
        private String type;
    }

    public static List<String[]> getGuildVerificationSections(String guildId) {
        List<String[]> raidCategoryInfo = new ArrayList<>();

        String sql = "SELECT * FROM verificationSections WHERE guildId = " + guildId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);

            while (resultSet.next()) {
                String[] currentRaidCategory = new String[6];
                currentRaidCategory[0] = resultSet.getString("verificationChannelId");
                currentRaidCategory[1] = resultSet.getString("verificationRoleId");
                currentRaidCategory[2] = resultSet.getString("starRequirement");
                currentRaidCategory[3] = resultSet.getString("fameRequirement");
                currentRaidCategory[4] = resultSet.getString("statsRequirement");
                currentRaidCategory[5] = resultSet.getString("runsRequirement");

                raidCategoryInfo.add(currentRaidCategory);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return raidCategoryInfo;
    }

    public static int getVerificationRuns(String verificationChannelId) {
        int runRequirement = 0;

        String sql = "SELECT * FROM verificationSections WHERE verificationChannelId = " + verificationChannelId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);

            while (resultSet.next()) {

                runRequirement = resultSet.getInt("runsRequirement");

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return runRequirement;
    }

    public static List<Integer> getVerificationReqs(String verificationChannelId) {
        List<Integer> verificationReqs = new ArrayList<>();

        String sql = "SELECT * FROM verificationSections WHERE verificationChannelId = " + verificationChannelId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);
            verificationReqs.add(resultSet.getInt("starRequirement"));
            verificationReqs.add(resultSet.getInt("fameRequirement"));
            verificationReqs.add(resultSet.getInt("statsRequirement"));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return verificationReqs;
    }

    public static HashMap<String, Integer> getVetVerificationReqs(Guild guild) {
        HashMap<String, Integer> verificationReqs = new HashMap<>();
        String sql = "SELECT veteranRequirements FROM guildInfo WHERE guildId = " + guild.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);

            String reqString = resultSet.getString("veteranRequirements");

            if (reqString.equalsIgnoreCase("0")) return verificationReqs;

            String[] reqs = reqString.split(" ");

            for (String r : reqs) {
                String[] req = r.split(":");
                verificationReqs.put(req[0], Integer.parseInt(req[1]));
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }
        return verificationReqs;
    }

    public static List<Integer> getVerificationReqs(Guild guild) {
        List<Integer> verificationReqs = new ArrayList<>();

        String sql = "SELECT starRequirement,aliveFameRequirement,classRequirement FROM guildInfo WHERE guildId = " + guild.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);
            verificationReqs.add(resultSet.getInt("starRequirement"));
            verificationReqs.add(resultSet.getInt("aliveFameRequirement"));
            verificationReqs.add(resultSet.getInt("classRequirement"));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return verificationReqs;
    }

    public static Category getActiveVerificationCategory(Guild guild) {

        if (!guildExists(guild.getId())) return null;

        String sql = "SELECT activeVerificationCategoryId FROM guildInfo WHERE guildId = " + guild.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);
            String categoryId = resultSet.getString("activeVerificationCategoryId");
            if (categoryId.equals("0")) {
                return null;
            } else {
                try {
                    return Goldilocks.jda.getCategoryById(categoryId);
                } catch (Exception e) {
                    return null;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getVerifiedRole(String verificationChannelId) {
        String verifiedRole = "";

        String sql = "SELECT verificationRoleId FROM verificationSections WHERE verificationChannelId = " + verificationChannelId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);
            return resultSet.getString("verificationRoleId");

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return verifiedRole;
    }

    public static String getVerificationChannel(String guildId) {
        String verificationChannel = "";

        String sql = "SELECT verificationChannelId FROM guildInfo WHERE guildId = " + guildId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);
            return resultSet.getString("verificationChannelId");

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return verificationChannel;
    }

    public static void retrieveVerificationRequests() {
        String verificationChannelId = "";

        String sql = "SELECT guildId,verificationChannelId FROM guildInfo WHERE verificationChannelId > 0";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);

            while (resultSet.next()) {

                verificationChannelId = resultSet.getString("verificationChannelId");
                String guildId = resultSet.getString("guildId");

                TextChannel verificationChannel = Goldilocks.jda.getTextChannelById(verificationChannelId);

                List<Message> requests = verificationChannel.getHistory().retrievePast(99).complete().stream()
                        .filter(message -> message.getAuthor().equals(Goldilocks.jda.getSelfUser())).collect(Collectors.toList());

                for (Message message : requests) {
                    try {
                        if (message.getEmbeds().size() > 0 && message.getEmbeds().get(0).getFooter().getText().contains(" - ")) {
                            String footer = message.getEmbeds().get(0).getFooter().getText();
                            manualVerificationRequests.add(new ManualVerificationRequest(Goldilocks.jda.getGuildById(guildId).getMemberById(footer.split(" - ")[0]), footer.split(" - ")[1], message));
                            System.out.println("Manual Verification Retrieved for: " + footer);
                        }
                        if (message.getEmbeds().size() > 0 && message.getEmbeds().get(0).getFooter().getText().contains(" : ")) {
                            String footer = message.getEmbeds().get(0).getFooter().getText();
                            VerificationHub.addAltRequests.add(new AddAltRequest(Goldilocks.jda.getGuildById(guildId).getMemberById(footer.split(" : ")[0]), footer.split(" : ")[1], message));
                            System.out.println("Alt Addition Request Retrieved for: " + footer);
                        }
                        if (message.getEmbeds().size() > 0 && message.getEmbeds().get(0).getFooter().getText().contains(" ~ ")) {
                            String footer = message.getEmbeds().get(0).getFooter().getText();
                            VerificationHub.vetVerificationRequests.add(new ExaltVerificationRequest(Goldilocks.jda.getGuildById(guildId).getMemberById(footer.split(" ~ ")[0]), footer.split(" ~ ")[1], message));
                            System.out.println("Veteran Verification Request Retrieved for: " + footer);
                        }
                    } catch (Exception e) {}
                }

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<String[]> getGuildRaidCategories(String guildId) {
        List<String[]> raidCategoryInfo = new ArrayList<>();

        String sql = "SELECT * FROM raidSections WHERE guildId = " + guildId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);

            while (resultSet.next()) {
                String[] currentRaidCategory = new String[5];
                currentRaidCategory[0] = resultSet.getString("raidCategoryId");
                currentRaidCategory[1] = resultSet.getString("raidRoleId");
                currentRaidCategory[2] = resultSet.getString("raidCommandChannelId");
                currentRaidCategory[3] = resultSet.getString("raidStatusChannel");
                currentRaidCategory[4] = resultSet.getString("defaultRaidId");

                raidCategoryInfo.add(currentRaidCategory);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return raidCategoryInfo;

    }

    public static Category getRaidSectionCategory(TextChannel raidStatusChannel) {
        String sql = "SELECT raidControlCategory FROM raidSections WHERE raidControlCategory > 0 AND raidStatusChannel = " + raidStatusChannel.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);
            while (resultSet.next()) {
                return Goldilocks.jda.getCategoryById(resultSet.getString("raidControlCategory"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String[]> getGuildKeySections(String guildId) {
        List<String[]> raidCategoryInfo = new ArrayList<>();

        String sql = "SELECT * FROM keySections WHERE guildId = " + guildId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);

            while (resultSet.next()) {
                String[] currentRaidCategory = new String[5];
                currentRaidCategory[0] = resultSet.getString("roleId");
                currentRaidCategory[1] = resultSet.getString("roleId");
                currentRaidCategory[2] = resultSet.getString("keyRequirement");
                currentRaidCategory[3] = resultSet.getString("leaderboardRole");
                currentRaidCategory[4] = resultSet.getString("uniqueMessage");

                raidCategoryInfo.add(currentRaidCategory);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return raidCategoryInfo;

    }

    public static List<String> getGuildVerificationChannels(String guildId) {
        List<String> verificationChannels = new ArrayList<>();

        String sql = "SELECT verificationChannelId FROM verificationSections WHERE guildId = " + guildId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);

            while (resultSet.next()) {
                String currentRaidChannel;
                currentRaidChannel = resultSet.getString("verificationChannelId");

                verificationChannels.add(currentRaidChannel);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return verificationChannels;
    }

    public static List<String> getBannedItems(Guild guild) {
        List<String> bannedItems = new ArrayList<>();
        String sql = "SELECT * FROM bannedItems WHERE guildId = " + guild.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            ResultSet resultSet = stmt.executeQuery(sql);
            while (resultSet.next()) bannedItems.add(resultSet.getString("itemName").toLowerCase());

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bannedItems;
    }

    public static List<String> getGuildRaidCommandChannels(String guildId) {
        List<String> raidCommandChannels = new ArrayList<>();

        String sql = "SELECT raidCommandChannelId FROM raidSections WHERE guildId = " + guildId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);

            while (resultSet.next()) {
                String currentRaidChannel;
                currentRaidChannel = resultSet.getString("raidCommandChannelId");

                raidCommandChannels.add(currentRaidChannel);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return raidCommandChannels;

    }

    public static String getRaidStatusChannel(String raidCommandChannelId) {
        String raidStatusChannel = "";

        String sql = "SELECT raidStatusChannel FROM raidSections WHERE raidCommandChannelId = " + raidCommandChannelId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);

            raidStatusChannel = resultSet.getString("raidStatusChannel");

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return raidStatusChannel;

    }

    public static String getRaiderRole(String raidStatusChannelId) {
        String raiderRole = "";

        String sql = "SELECT raidRoleId FROM raidSections WHERE raidStatusChannel = " + raidStatusChannelId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            ResultSet resultSet = stmt.executeQuery(sql);
            raiderRole = resultSet.getString("raidRoleId");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return raiderRole;
    }

    public static boolean moveMembers(String raidStatusChannelId) {
        String raiderRole = "";

        String sql = "SELECT moveMembers FROM raidSections WHERE raidStatusChannel = " + raidStatusChannelId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            ResultSet resultSet = stmt.executeQuery(sql);
            return resultSet.getString("moveMembers").equals("1");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static int getSectionDungeon(String raidCommandChannelId) {
        int dungeonNumber = -1;

        String sql = "SELECT defaultRaidId FROM raidSections WHERE raidCommandChannelId = " + raidCommandChannelId + " OR raidStatusChannel = " + raidCommandChannelId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);

            dungeonNumber = resultSet.getInt("defaultRaidId");

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return dungeonNumber;

    }

    public static boolean userExists(String userID, String guildId) {
        String sql = "SELECT userId FROM userStats WHERE userId = ? AND guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, userID);
            pstmt.setString(2, guildId);

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) {
                return true;
            } else return false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int getRunsCompleted(String userID, String guildId) {
        String sql = "SELECT runsCompleted FROM userStats WHERE userID = ? AND guildId = ?";

        if (!userExists(userID,guildId)) {
            createUser(userID, guildId);
        }

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, userID);
            pstmt.setString(2, guildId);

            ResultSet resultSet = pstmt.executeQuery();

            return Integer.parseInt(resultSet.getString("runsCompleted"));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static long getEventTime(String userID, String guildId) {
        String sql = "SELECT eventTime FROM userStats WHERE userID = ? AND guildId = ?";

        if (!userExists(userID,guildId)) {
            createUser(userID, guildId);
        }

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, userID);
            pstmt.setString(2, guildId);

            ResultSet resultSet = pstmt.executeQuery();

            return Long.parseLong(resultSet.getString("eventTime")) * 1000;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String[][] getTopRuns(String guildId) {
        String[][] topruns = new String[10][2];
        String sql = "SELECT userId, runsCompleted FROM userStats WHERE (runsCompleted > 0) AND (userId != 724025002856546315) AND guildId = " + guildId + " ORDER BY runsCompleted DESC LIMIT 10";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             Statement stmt = conn.createStatement()){


            ResultSet resultSet = stmt.executeQuery(sql);
            int i = 0;
            while (resultSet.next()) {
                topruns[i][0] = resultSet.getString(1);
                topruns[i][1] = resultSet.getString(2);
                //System.out.println(topruns[i][0] + " with " + topruns[i][1]);
                i++;
            }
            return topruns;


        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String[] get2048Hs() {
        String[] highScore = new String[2];
        String sql = "SELECT * FROM games ORDER BY score DESC LIMIT 1";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);
            highScore[0] = resultSet.getString(1);
            highScore[1] = resultSet.getString(2);
            return highScore;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String get2048board(User user) {
        String boardData = "";
        String sql = "SELECT boardInfo FROM games WHERE userId = " + user.getId() + " AND gameOver = '0'";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);

            if (resultSet.next()) boardData = resultSet.getString(1);
            if (!boardData.isEmpty()) {
                conn.close();
                delete2048Board(user);
            }

            return boardData;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void delete2048Board(User user) {
        String sql = "DELETE FROM games WHERE userId = " + user.getId() + " AND gameOver = '0'";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void add2048Score(String userID, int score, boolean gameOver, String boardData) {
        String sql = "INSERT INTO games (userId,score,gameOver, boardInfo) VALUES(?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, userID);
            pstmt.setInt(2, score);
            pstmt.setBoolean(3, gameOver);
            pstmt.setString(4, boardData);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String[][] getRunsPlace(String userId, String guildId) {
        String[][] runsPlace = new String[3][3];
        String sql = "Select rank,guildId,userId,runsCompleted FROM (SELECT row_number() OVER (ORDER BY runsCompleted DESC) AS rank,guildId,userId,runsCompleted FROM userStats) WHERE userId = ? AND guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, userId);
            pstmt.setString(2, guildId);

            ResultSet resultSet = pstmt.executeQuery();
            int i = 1;
            while (resultSet.next()) {
                runsPlace[1][0] = resultSet.getString(1);
                runsPlace[1][1] = resultSet.getString(3);
                runsPlace[1][2] = resultSet.getString(4);
                //System.out.println(runsPlace[i][0] + " with " + runsPlace[i][1]);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        sql = "Select rank,guildId,userId,runsCompleted FROM (SELECT row_number() OVER (ORDER BY runsCompleted DESC) AS rank,guildId,userId,runsCompleted FROM userStats) WHERE (rank BETWEEN "
                + (Integer.parseInt(runsPlace[1][0]) - 1) + " and " + (Integer.parseInt(runsPlace[1][0]) + 1) + ") AND guildId = " + guildId;

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            //pstmt.setString(1, String.valueOf(Integer.parseInt(runsPlace[1][0]) - 1));
            //System.out.println(String.valueOf(Integer.parseInt(runsPlace[1][0]) - 1));
            //pstmt.setString(2, String.valueOf(Integer.parseInt(runsPlace[1][0]) + 1));
            //System.out.println(String.valueOf(Integer.parseInt(runsPlace[1][0]) + 1));

            ResultSet resultSet = pstmt.executeQuery();

            int i = 0;
            while (resultSet.next()) {
                runsPlace[i][0] = resultSet.getString(1);
                runsPlace[i][1] = resultSet.getString(3);
                runsPlace[i][2] = resultSet.getString(4);
                i++;
                //System.out.println(runsPlace[i][0] + " with " + runsPlace[i][1]);
            }

            return runsPlace;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean staffExists(String userID, String guildId) {
        String sql = "SELECT userId FROM rlStats WHERE userID = ? AND guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, userID);
            pstmt.setString(2, guildId);

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) {
                return true;
            } else return false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void createStaff(String userID, String guildId) {
        String sql = "INSERT INTO rlStats (guildId,userId,quotaRuns,quotaAssists,quotaParses,totalRunsLed,totalAssists,totalParses,quotaEventTime,totalEventTime) VALUES(?,?,0,0,0,0,0,0,0,0)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, guildId);
            pstmt.setString(2, userID);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createUser(String userID, String guildId) {
        String sql = "INSERT INTO userStats (guildId,userId,keysPopped,runsCompleted,eventTime,keysPopped,vialsPopped,runesPopped) VALUES(?,?,0,0,0,0,0,0)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, guildId);
            pstmt.setString(2, userID);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createStickyRoleMessage(Message message, Role role) {
        String sql = "INSERT INTO stickyRoles (guildId,textchannelId,messageId,roleId) VALUES(?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, message.getGuild().getId());
            pstmt.setString(2, message.getChannel().getId());
            pstmt.setString(3, message.getId());
            pstmt.setString(4, role.getId());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean isEarlyLoc(Guild guild) {
        String sql = "SELECT earlyLocBool FROM guildInfo WHERE guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, guild.getId());

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) return resultSet.getBoolean(1);
            else return false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isShatters(Guild guild) {
        String sql = "SELECT shatters FROM guildInfo WHERE guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, guild.getId());

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) return resultSet.getBoolean(1);
            else return false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isFungal(Guild guild) {
        String sql = "SELECT isFungal FROM guildInfo WHERE guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, guild.getId());

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) return resultSet.getBoolean(1);
            else return false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isOSanc(Guild guild) {
        String sql = "SELECT isOSanc FROM guildInfo WHERE guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, guild.getId());

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) return resultSet.getBoolean(1);
            else return false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isPub(Guild guild) {
        String sql = "SELECT pubhalls FROM guildInfo WHERE guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, guild.getId());

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) return resultSet.getInt(1) == 1;
            else return false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isEndGame(Guild guild) {
        String sql = "SELECT endgame FROM guildInfo WHERE guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, guild.getId());

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) return resultSet.getInt(1) == 1;
            else return false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean queueEnabled(Guild guild) {
        String sql = "SELECT queueBool FROM guildInfo WHERE guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, guild.getId());

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) return resultSet.getBoolean(1);
            else return false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean hasQuota(Guild guild) {
        String sql = "SELECT quotaBool FROM guildInfo WHERE guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, guild.getId());

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) return resultSet.getBoolean(1);
            else return false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean hasStaticChannels(Guild guild) {
        String sql = "SELECT staticAfksBool FROM guildInfo WHERE guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, guild.getId());

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) return resultSet.getBoolean(1);
            else return false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteMessages(Guild guild) {
        String sql = "SELECT deleteMessagesBool FROM guildInfo WHERE guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, guild.getId());

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) return resultSet.getBoolean(1);
            else return false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean hasFeedback(Guild guild) {
        String sql = "SELECT feedbackBool FROM guildInfo WHERE guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, guild.getId());

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) return resultSet.getBoolean(1);
            else return false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Role isStickyRoleMessage(String messageId) {

        Role role = null;
        String sql = "SELECT * FROM stickyRoles WHERE messageId = " + messageId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);

            if (!resultSet.isClosed()) {
                String roleId = resultSet.getString("roleId");
                return Goldilocks.jda.getRoleById(roleId);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

        return role;

    }

    public static void addPlayerRuns(List<Member> memberList) {

        if (memberList.size() != 0) {
            String guildId = memberList.get(0).getGuild().getId();
            String updateString = "(";
            String sql = "UPDATE userStats SET runsCompleted = runsCompleted + 1 WHERE guildId = " + guildId + " AND ";

            for (Member member : memberList) {
                if (!userExists(member.getId(), guildId)) {
                    createUser(member.getId(), guildId);
                }
                updateString+=" OR userId = " + member.getId();
            }
            updateString = StringUtils.replaceOnce(updateString, " OR ", "");
            sql += updateString + ")";
            //System.out.println(sql);
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 Statement stmt = conn.createStatement()){
                stmt.executeUpdate(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void addEventTime(Member member, Long time) {

        String guildId = member.getGuild().getId();
        String sql = "UPDATE userStats SET eventTime = eventTime + " + time + " WHERE guildId = " + guildId + " AND userId = " + member.getId();

        if (!userExists(member.getId(), guildId)) {
            createUser(member.getId(), guildId);
        }
        //System.out.println(sql);
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static void addRlQuota(Member member) {

        String guildId = member.getGuild().getId();
        String sql = "UPDATE rlStats SET quotaRuns = quotaRuns + 1, totalRunsLed = totalRunsLed + 1 WHERE guildId = " + guildId + " AND userId = " + member.getId();

        if (!staffExists(member.getId(), guildId)) {
            createStaff(member.getId(), guildId);
        }
        //System.out.println(sql);
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static void addRlEventTime(Member member, Long time) {

        String guildId = member.getGuild().getId();
        String sql = "UPDATE rlStats SET quotaEventTime = quotaEventTime + " + time + ", totalEventTime = totalEventTime + " + time + " WHERE guildId = " + guildId + " AND userId = " + member.getId();

        if (!staffExists(member.getId(), guildId)) {
            createStaff(member.getId(), guildId);
        }
        //System.out.println(sql);
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static void addKeys(Member member, int keyAmount) {

        String guildId = member.getGuild().getId();
        String sql = "UPDATE userStats SET keysPopped = keysPopped + " + keyAmount + " WHERE guildId = " + guildId + " AND userId = " + member.getId();

        if (!userExists(member.getId(), guildId)) {
            userExists(member.getId(), guildId);
        }
        //System.out.println(sql);
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static void addKeys(Long userId, Guild guild ,int keyAmount) {

        String sql = "UPDATE userStats SET keysPopped = keysPopped + " + keyAmount + " WHERE userId = " + userId + " AND guildId = " + guild.getId();

        if (!userExists(userId.toString(), guild.getId())) {
            userExists(userId.toString(), guild.getId());
        }
        //System.out.println(sql);
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static int getKeysPopped(String userID, String guildId) {
        String sql = "SELECT keysPopped FROM userStats WHERE userID = ? AND guildId = ?";

        if (!userExists(userID,guildId)) {
            createUser(userID, guildId);
        }

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, userID);
            pstmt.setString(2, guildId);

            ResultSet resultSet = pstmt.executeQuery();

            return Integer.parseInt(resultSet.getString("keysPopped"));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void addParse(Member member) {

        String guildId = member.getGuild().getId();
        String sql = "UPDATE rlStats SET quotaParses = quotaParses + 1, totalParses = totalParses + 1 WHERE guildId = " + guildId + " AND userId = " + member.getId();

        if (!staffExists(member.getId(), guildId)) {
            createStaff(member.getId(), guildId);
        }
        //System.out.println(sql);
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static void incrementField(Member member, String field, String totalField) {

        String guildId = member.getGuild().getId();
        String sql = "UPDATE rlStats SET " + field + " = " + field + " + 1, " + totalField + " = " + totalField + " + 1 WHERE guildId = " + guildId + " AND userId = " + member.getId();

        if (!staffExists(member.getId(), guildId)) {
            createStaff(member.getId(), guildId);
        }
        //System.out.println(sql);
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static void addAssists(List<Member> memberList) {
        if (memberList.size() != 0) {
            String guildId = memberList.get(0).getGuild().getId();
            String updateString = "(";
            String sql = "UPDATE rlStats SET quotaAssists = quotaAssists + 1, totalAssists = totalAssists + 1 WHERE guildId = " + guildId + " AND ";

            for (Member member : memberList) {
                if (!staffExists(member.getId(), guildId)) {
                    createStaff(member.getId(), guildId);
                }
                updateString+=" OR userId = " + member.getId();
            }
            updateString = StringUtils.replaceOnce(updateString, " OR ", "");
            sql += updateString + ")";
            //System.out.println(sql);
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 Statement stmt = conn.createStatement()){
                stmt.executeUpdate(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    public static String[][] getTopQuota(String sqlField, String guildId) {

        String[][] topQuota = new String[3][2];
        String sql = "SELECT userId, " + sqlField + " FROM rlStats WHERE guildId = " + guildId + " ORDER BY " + sqlField + " DESC LIMIT 3";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);
            int i = 0;
            while (resultSet.next()) {
                topQuota[i][0] = resultSet.getString(1);
                topQuota[i][1] = resultSet.getString(2);
                //System.out.println(topruns[i][0] + " with " + topruns[i][1]);
                i++;
            }
            return topQuota;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static HashMap<String, Integer> getTopPoints(String guildId) {

        HashMap<String, Integer> topQuota = new HashMap<>();
        String sql = "SELECT userId, quotaParses, quotaVerifications, quotaAlts, quotaNameChanges, quotaModMail FROM rlStats WHERE guildId = " + guildId + ";";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);
            int i = 0;
            while (resultSet.next()) {
                int numPoints = (resultSet.getInt("quotaParses") * 5 + resultSet.getInt("quotaVerifications") * 3 +  resultSet.getInt("quotaAlts") * 2
                        + resultSet.getInt("quotaNameChanges") + resultSet.getInt("quotaModMail"));
                topQuota.put(resultSet.getString(1), numPoints);
            }
            return topQuota;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String[] getRunsAssists(String guildId, String userId) {

        String[] runsAssists = new String[2];
        String sql = "SELECT userId, quotaAssists, quotaRuns FROM rlStats WHERE guildId = " + guildId + " AND userId = " + userId;

        if (!staffExists(userId, guildId)) {
            createStaff(userId, guildId);
        }

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);
            runsAssists[0] = resultSet.getString("quotaRuns");
            runsAssists[1] = resultSet.getString("quotaAssists");
            return runsAssists;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int[] getAllQuota(String guildId, String userId) {

        int[] quota = new int[8];
        String sql = "SELECT userId, quotaRuns, quotaAssists, quotaParses, quotaVerifications, quotaNameChanges, quotaAlts, quotaModmail FROM rlStats WHERE guildId = " + guildId + " AND userId = " + userId;

        if (!staffExists(userId, guildId)) {
            createStaff(userId, guildId);
        }

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);
            quota[1] = resultSet.getInt("quotaRuns");
            quota[2] = resultSet.getInt("quotaAssists");
            quota[3] = resultSet.getInt("quotaParses");
            quota[4] = resultSet.getInt("quotaVerifications");
            quota[5] = resultSet.getInt("quotaNameChanges");
            quota[6] = resultSet.getInt("quotaAlts");
            quota[7] = resultSet.getInt("quotaModmail");

            // Fungal Points
            quota[0] = (quota[3] * 5) + (quota[4] * 3) + (quota[7] * 1) + (quota[6] * 2) + (quota[5] * 1);
            return quota;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getStaffData(Member member, String field) {

        String runsAssists = "";
        String sql = "SELECT " + field + " FROM rlStats WHERE guildId = " + member.getGuild().getId() + " AND userId = " + member.getId();

        if (!staffExists(member.getId(), member.getGuild().getId())) {
            createStaff(member.getId(), member.getGuild().getId());
        }

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);
            return resultSet.getString(field);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String[] getAssistsParses(String guildId, String userId) {

        String[] runsAssists = new String[2];
        String sql = "SELECT userId, quotaAssists, quotaParses FROM rlStats WHERE guildId = " + guildId + " AND userId = " + userId;

        if (!staffExists(userId, guildId)) {
            createStaff(userId, guildId);
        }

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);
            runsAssists[0] = resultSet.getString("quotaAssists");
            runsAssists[1] = resultSet.getString("quotaParses");
            return runsAssists;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String[] getQuota(String guildId, String userId) {
        String[] runsAssists = new String[3];
        String sql = "SELECT userId, quotaRuns, quotaAssists, quotaParses FROM rlStats WHERE guildId = " + guildId + " AND userId = " + userId;

        if (!staffExists(userId, guildId)) {
            createStaff(userId, guildId);
        }

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);
            runsAssists[0] = resultSet.getString("quotaRuns");
            runsAssists[1] = resultSet.getString("quotaAssists");
            runsAssists[2] = resultSet.getString("quotaParses");
            return runsAssists;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return runsAssists;
    }

    public static String[] getEventAssists(String guildId, String userId) {

        String[] runsAssists = new String[2];
        String sql = "SELECT userId, quotaAssists, quotaEventTime FROM rlStats WHERE guildId = " + guildId + " AND userId = " + userId;

        if (!staffExists(userId, guildId)) {
            createStaff(userId, guildId);
        }

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);
            runsAssists[0] = resultSet.getString("quotaAssists");
            runsAssists[1] = String.valueOf(resultSet.getInt("quotaEventTime"));
            return runsAssists;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void resetQuota(String guildId) {
        String sql = "UPDATE rlStats SET quotaRuns = 0, quotaAssists = 0, quotaParses = 0, quotaEventTime = 0, quotaVerifications = 0, quotaNameChanges = 0, quotaAlts = 0, quotaModmail = 0 WHERE guildId = " + guildId;
        //System.out.println(sql);
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<Giveaway> retrieveGiveaways() {
        List<Giveaway> giveaways = new ArrayList<>();
        List<String> failedGiveaways = new ArrayList<>();
        String sql = "SELECT * FROM giveaways WHERE ended = '0'";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);
            while (resultSet.next()) {
                try {
                    Guild guild = Goldilocks.jda.getGuildById(resultSet.getString("guildId"));
                    Member host = guild.getMemberById(resultSet.getString("hostId"));
                    Member creator = guild.getMemberById(resultSet.getString("creatorId"));
                    int numWinners = resultSet.getInt("numWinners");
                    String prize = resultSet.getString("prize");
                    TextChannel textChannel = guild.getTextChannelById(resultSet.getString("textChannelId"));
                    long startingTime = resultSet.getTime("startingTime").getTime();
                    long length = resultSet.getLong("length");
                    Message message = textChannel.retrieveMessageById(resultSet.getString("messageId")).complete();

                    giveaways.add(new Giveaway(host, creator, numWinners, prize, textChannel, startingTime, length, message));
                    System.out.println("Retrieved Giveaway for: " + guild.getId() + " hosted by " + host.getId());
                } catch (Exception e) {
                    System.out.println("Failed to retrieve giveaway for: " + resultSet.getString("guildId") + " | " + resultSet.getString("hostId"));
                    failedGiveaways.add(resultSet.getString("messageId"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for (String s : failedGiveaways)
            endGiveaway(s);

        return giveaways;
    }

    public static void addGiveaway(Giveaway giveaway) {
        //public Giveaway(Member host, Member creator, int numWinners, String prize, TextChannel textChannel, long startingTime, long length, Message giveawayMessage)
        String sql = "INSERT INTO giveaways (guildId,hostId,creatorId,numWinners,prize,textChannelId,startingTime,length,messageId,ended)" +
                " VALUES (?,?,?,?,?,?,?,?,?,0)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, giveaway.host.getGuild().getId());
            pstmt.setString(2, giveaway.host.getId());
            pstmt.setString(3, giveaway.creator.getId());
            pstmt.setInt(4, giveaway.numWinners);
            pstmt.setString(5, giveaway.prize);
            pstmt.setString(6, giveaway.textChannel.getId());
            pstmt.setTime(7, new Time(giveaway.startingTime));
            pstmt.setLong(8, giveaway.length);
            pstmt.setString(9, giveaway.giveawayMessage.getId());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void endGiveaway(String messageId) {
        String sql = "UPDATE giveaways SET ended = 1 WHERE messageId = " + messageId;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String query(String column, String table, String conditions) {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            return stmt.executeQuery("SELECT " + column + " FROM " + table + " WHERE " + conditions).getString(column);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Nullable
    public static Emote getEmote(String name) {
        String id = Database.query("id", "emotes", "name = '" + name + "'");
        if (id.isEmpty()) return null;
        return Goldilocks.jda.getEmoteById(id);
    }

    public static void retrieveActiveLogPanels() {
        List<String> jsonObjects = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            ResultSet resultSet = stmt.executeQuery("SELECT * FROM activeLogPanels");

            while(resultSet.next()) {
                jsonObjects.add(resultSet.getString("json"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Database connection error.");
        }
        jsonObjects.forEach(s -> new LogPanel(s, null));
    }

    public static boolean exists(String table, String id) {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            ResultSet resultSet = stmt.executeQuery("SELECT " + table + " FROM guildInfo WHERE guildId = " + id);

            return resultSet.next();

        } catch (Exception e) {
            System.out.println("Database connection error.");
        }
        return false;
    }

    public static void createRlSettings(Member member) {
        String sql = "INSERT INTO guildInfo (guildId, userId) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, member.getGuild().getId());
            pstmt.setString(2, member.getId());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Log a parse into the DB table
    public static void addParseLog(Member mod, String crashers, String playersInVc, String who) {
        String sql = "INSERT INTO parseTable (guildId,modId,crashers,playersInVc,who, timeDone)" +
                " VALUES (?,?,?,?,?, datetime('now'))";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, mod.getGuild().getId());
            pstmt.setString(2, mod.getId());
            pstmt.setString(3, crashers);
            pstmt.setString(4, playersInVc);
            pstmt.setString(5, who);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        logParseAffiliations(crashers, playersInVc);
    }

    public static void logParseAffiliations(String crashers, String playersInVc) {
        String sql = "INSERT INTO parseAffiliations (parseId, crasher, playerInVc)" +
                " VALUES ";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            String parseId = stmt.executeQuery("SELECT count(ParseID) FROM parseTable").getString(1);

            sql += Arrays.stream(crashers.split(", ")).map(c ->
                    Arrays.stream(playersInVc.split(", "))
                            .map(p -> "(" + parseId + ", '" + StringEscapeUtils.escapeSql(c) + "', '" + StringEscapeUtils.escapeSql(p) + "')")
                            .collect(Collectors.joining(", "))
            ).collect(Collectors.joining(", "));
            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getAffiliations(String crasherName, List<String> leakers) {
        String sql = "SELECT * FROM parseRelations WHERE crasher = '" + crasherName + "'";

        List<CrasherRelation> relations = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);

            while (resultSet.next()) {
                String crasher = resultSet.getString("crasher").replaceAll("[^A-Za-z]", "");

                CrasherRelation relation = new CrasherRelation(
                        crasher,
                        resultSet.getString("playerInVc").split(" ")[0].replaceAll("[^A-Za-z]", ""),
                        resultSet.getInt("numCrashes"),
                        resultSet.getInt("numOccurences")
                );

                relations.add(relation);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        relations.sort((r1, r2) -> (int) ((r2.coefficient * r2.numOccurences) * 100 - (r1.coefficient * r1.numOccurences) * 100));
        CrasherRelation foundRelation = relations.stream().filter(r -> r.numOccurences > 3 && r.coefficient >= .75).filter(r -> leakers.stream().anyMatch(r::leakerEquals)).findAny().orElse(null);

        if (foundRelation == null) return "";

        else {
            double confidence = 1.0 / ((relations.stream().filter(r -> r.numOccurences == foundRelation.numOccurences)).count());
            return foundRelation.leaker + " ⇒ " + crasherName + " (Correlation: (" + foundRelation.numOccurences + " / " + foundRelation.numCrashes + ") | Conf: " + (confidence >= 1.0 ? "99%" : String.format("%.2f", confidence * 100) + "%") + ")";
        }
    }

    private static class CrasherRelation {

        public String crasher;
        public String leaker;
        public int numCrashes;
        public int numOccurences;
        public double coefficient;

        public CrasherRelation(String crasher, String leaker, int numCrashes, int numOccurences) {
            this.crasher = crasher;
            this.leaker = leaker;
            this.numCrashes = numCrashes;
            this.numOccurences = numOccurences;
            this.coefficient = (double) numOccurences / numCrashes;
        }

        @Override
        public String toString() {
            return String.format("Relation: %s → %s | # Occurrences: %04d Coefficient %.2f", leaker, crasher, numOccurences, coefficient);
        }

        public boolean leakerEquals(String leaker2) {
            return leaker.split(" ")[0].replaceAll("[^A-Za-z]", "").equalsIgnoreCase(leaker2.split(" ")[0].replaceAll("[^A-Za-z]", ""));
        }
    }

    public static int[] getParses(String crasherName, Guild guild) {
        String sql = "SELECT * FROM guildParseAffiliations WHERE crasher = '" + crasherName + "' ORDER BY parseId ASC";

        int[] numCrashes = new int[2];
        List<Integer> accountedForParses = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);

            while (resultSet.next()) {
                int parseId = resultSet.getInt("parseId");
                String leaker = resultSet.getString("playerInVc");
                String guildId = resultSet.getString("guildId");

                if (!accountedForParses.contains(parseId)) {
                    numCrashes[0]++;
                    if (guildId.equals(guild.getId())) numCrashes[1]++;
                    accountedForParses.add(parseId);
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return numCrashes;
    }

}

