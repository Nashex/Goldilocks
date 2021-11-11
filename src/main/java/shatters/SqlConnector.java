package shatters;

import main.Config;
import main.Goldilocks;
import moderation.punishments.Suspension;
import moderation.punishments.Warning;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang.StringEscapeUtils;
import quota.LogField;
import raids.Raid;
import raids.caching.RaidCaching;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SqlConnector {
    private static String dbUrl = "jdbc:mysql://" + Config.get("SHATTERS_DB_IP") + "/shatters";
    private static String userName = Config.get("SHATTERS_DB_USERNAME").toString();
    private static String password = Config.get("SHATTERS_DB_PASSWORD").toString();

    private static String raidLogDb = "jdbc:mysql://" + Config.get("DATABASE_IP").toString();
    private static String raidLogUsername = Config.get("RAID_DB_USERNAME").toString();
    private static String raidLogPassword = Config.get("RAID_DB_PASSWORD").toString();

    private static Connection conn = null;
    private static Statement stmt = null;

    public static void testConnection() {
        try (Connection conn = DriverManager.getConnection(raidLogDb, raidLogUsername, raidLogPassword);
             Statement stmt = conn.createStatement()) {
            System.out.println("Database successfully connected.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Database connection error.");
        }
    }

    public static void openConnection() {
        try {
            conn = DriverManager.getConnection(raidLogDb, raidLogUsername, raidLogPassword);
            stmt = conn.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Database connection error.");
        }
    }

    public static String isExpelled(String name) {
        String sql = "SELECT * FROM veriblacklist WHERE id = '" + name + "'";
        try (Connection conn = DriverManager.getConnection(dbUrl, userName, password);
             Statement stmt = conn.createStatement();) {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) return rs.getString("modId");

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String expelUser(String name, String id, String modId) {
        String sql = "INSERT INTO veriblacklist (id, modid) VALUES ( '" + name + "','" + modId + "'),('" + id + "','" + modId + "');";
        try (Connection conn = DriverManager.getConnection(dbUrl, userName, password);
             Statement stmt = conn.createStatement();) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean unexpelUser(String string) {
        String sql = "DELETE FROM veriblacklist WHERE id = '" + string.toLowerCase() + "'";
        try (Connection conn = DriverManager.getConnection(dbUrl, userName, password);
             Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void logRaid(Raid raid) {
        if (conn == null) openConnection();
        try {
            if (stmt == null) conn.createStatement();
            String guildId = raid.getRaidGuild().getId();
            String leaderId = raid.getRaidLeader().getId();
            String raidType = "" + raid.getRaidType();
            String raidName = raid.getDungeonName();
            String raidObject = RaidCaching.cacheRaid(raid);

            stmt.execute("INSERT INTO raidLogs (guildId, leaderId, raidType, raidName, raidObject) VALUES (" +
                    guildId + ", " + leaderId + ", " + raidType + ", '" +
                    raidName + "', '" + StringEscapeUtils.escapeSql(raidObject) + "')");

            //ResultSet resultSet = stmt.executeQuery("");
            //int raidId = resultSet.getInt(1);

            List<Member> raidMembers = raid.getVoiceChannel().getMembers();
            if (!raidMembers.isEmpty()) stmt.execute("INSERT INTO userRaidLogs (raidId, userId, guildId, leaderId, raidType, raidName) VALUES " +
                    raidMembers.stream().map(m -> "((SELECT MAX(raidId) from raidLogs), " + m.getId() + ", " + guildId + ", " + leaderId + ", " + raidType + ", '" + raidName + "')").collect(Collectors.joining(",")));
        } catch (Exception e) {
            System.out.println("Failed to log raid.");
            //e.printStackTrace();
        }
    }

    public static String query(String s) {
        try (Connection conn = DriverManager.getConnection(dbUrl, userName, password);
             Statement stmt = conn.createStatement()){
            ResultSet resultSet = stmt.executeQuery(s);
            if (resultSet.next()) return resultSet.getString(1);
        } catch (Exception e) {
            System.out.println("Database connection error.");
        }
        return "";
    }

    public static List<List<LogField>> getUserRuns(String memberId, String guildId) {
        try (Connection conn = DriverManager.getConnection(raidLogDb, raidLogUsername, raidLogPassword);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery("SELECT DISTINCT raidType from userraidlogs WHERE userId = " + memberId + " AND guildId = " + guildId);
            HashMap<String, List<LogField>> dataPoints = new HashMap<>();
            while (resultSet.next()) dataPoints.put(resultSet.getString("raidType"), new ArrayList<>());

            resultSet = stmt.executeQuery("SELECT raidType, CAST(time AS DATE) AS 'date', COUNT(raidName) AS 'numRuns' from userraidlogs where userId = "
                    + memberId + " AND guildId = " + guildId + " GROUP BY CAST(time AS DATE), raidType");
            while (resultSet.next()) {
                String raidName = resultSet.getString("raidType");
                long raidTime = resultSet.getDate("date").getTime();
                int numRuns = resultSet.getInt("numRuns");
                dataPoints.get(raidName).add(new LogField(raidName, numRuns, raidTime));
            }

            return new ArrayList<>(dataPoints.values());

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Database connection error.");
        }
        return null;
    }

    public static List<LogField> executeLogQueries(String dbUrl) {
        List<LogField> fields = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl, userName, password);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery("SELECT * from users");
            ResultSetMetaData rsmd = resultSet.getMetaData();
            int numCols = rsmd.getColumnCount();
            for (int i = 2; i <= numCols; i++) {
                if (!rsmd.getColumnName(i).contains("nitro")) {
                    resultSet = stmt.executeQuery("SELECT SUM(" + rsmd.getColumnName(i) + ") from users");
                    if (resultSet.next()) {
                        fields.add(new LogField(rsmd.getColumnName(i), resultSet.getInt(1)));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Database connection error.");
        }
        return fields;
    }

    public static String[] shattersStats(User user) {
        return shattersStats(user.getId());
    }

    public static String[] shattersStats(String userId) {
        String[] stats = new String[10];
        String sql = "SELECT * FROM users WHERE id = " + userId;

        try (Connection conn = DriverManager.getConnection(dbUrl, userName, password);
             Statement stmt = conn.createStatement()){
            ResultSet resultSet = stmt.executeQuery(sql);
            if (resultSet.next()) {
                stats[0] = resultSet.getString("runs");
                stats[1] = resultSet.getString("shatterspops");
                stats[2] = resultSet.getString("eventpops");
                stats[3] = resultSet.getString("eventruns");
                stats[4] = resultSet.getString("successruns");
                stats[5] = resultSet.getString("assists");
                stats[6] = resultSet.getString("currentweek");
                stats[7] = resultSet.getString("currentweekassists");
                stats[8] = resultSet.getString("currentweekEvents");
                stats[9] = resultSet.getString("currentweekfailed");
                return stats;
            }
        } catch (Exception e) {
            System.out.println("Database connection error.");
        }
        return stats;
    }

    public static LinkedHashMap<String, String[]> getStatsSortedField(String field, String subField, String subFieldMult) {
        String[] stats = new String[4];
        LinkedHashMap<String, String[]> players = new LinkedHashMap<>();
        String sql = "SELECT * FROM users ORDER BY (" + field + " + " + subFieldMult + (subField.isEmpty() ? "" : " * ") + subField + ") DESC";

        long timeStarted = System.currentTimeMillis();

        String id = "";
        try (Connection conn = DriverManager.getConnection(dbUrl, userName, password);
             Statement stmt = conn.createStatement()){
            ResultSet resultSet = stmt.executeQuery(sql);
            while (resultSet.next()) {
                players.put(resultSet.getString("id"),
                        new String[]{resultSet.getString("currentweek"),
                                resultSet.getString("currentweekassists"),
                                resultSet.getString("currentweekfailed"),
                                resultSet.getString("currentweekEvents")});

            }
        } catch (Exception e) {
            System.out.println("Database connection error.");
        }

        //System.out.println(System.currentTimeMillis() - timeStarted);

        return players;
    }



    public static void logFieldForMembers(List<Member> members, List<String> fields, int increment) {
        Goldilocks.TIMER.schedule(() -> {
            try (Connection conn = DriverManager.getConnection(dbUrl, userName, password);
                 Statement stmt = conn.createStatement()){

                String sql = "INSERT INTO users (id, " + String.join(",", fields) + ") VALUES ";
                sql += members.stream().map(member -> "('" + member.getId() + "', " + fields.stream().map(s -> String.valueOf(increment)).collect(Collectors.joining(",")) + ")").collect(Collectors.joining(","));
                sql += " ON DUPLICATE KEY UPDATE " + fields.stream().map(s -> s + " = " + s + " + " + increment).collect(Collectors.joining(","));
                stmt.execute(sql);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0L, TimeUnit.SECONDS);
    }

    public static void logFieldForMember(Member member, List<String> fields, int increment) {

        Goldilocks.TIMER.schedule(() -> {
            try (Connection conn = DriverManager.getConnection(dbUrl, userName, password);
                 Statement stmt = conn.createStatement()){

                String sql = "INSERT INTO users (id, " + String.join(",", fields) + ") VALUES ";
                sql += "('" + member.getId() + "', " + fields.stream().map(s -> String.valueOf(increment)).collect(Collectors.joining(",")) + ")";
                sql += " ON DUPLICATE KEY UPDATE " + fields.stream().map(s -> s + " = " + s + " + " + increment).collect(Collectors.joining(","));
                stmt.execute(sql);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0L, TimeUnit.SECONDS);

    }

    public static void logFieldForMember(Member member, List<String> fields, int increment, String schema) {

        Goldilocks.TIMER.schedule(() -> {
            try (Connection conn = DriverManager.getConnection("jdbc:mysql://" + Config.get("SHATTERS_DATABASE_IP") + schema, userName, password);
                 Statement stmt = conn.createStatement()){

                String sql = "INSERT INTO users (id, " + String.join(",", fields) + ") VALUES ";
                sql += "('" + member.getId() + "', " + fields.stream().map(s -> String.valueOf((increment < 0 ? 0 : increment))).collect(Collectors.joining(",")) + ")";
                sql += " ON DUPLICATE KEY UPDATE " + fields.stream().map(s -> s + " = " + s + " + " + increment).collect(Collectors.joining(","));
                stmt.execute(sql);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0L, TimeUnit.SECONDS);

    }

    public static List<Suspension> getSuspensions(Member member) {

        List<Suspension> suspensions = new ArrayList<>();
        String sql = "SELECT * FROM suspensions WHERE id = " + member.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl, userName, password);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            ResultSet resultSet = pstmt.executeQuery();

            while (resultSet.next()) {
                String punisherId = resultSet.getString("modid");
                String reason = resultSet.getString("reason");
                Long timeStarted = 0L;
                Long timeEnded = resultSet.getLong("uTime");
                String caseId = "";

                // (String guildId, String recipientId, String modId, String reason, long timeIssued, long timeEnding, String caseId)
                Suspension suspension = new Suspension(member.getGuild().getId(), member.getId(), punisherId, reason, timeStarted * 1000, timeEnded * 1000, caseId);
                suspensions.add(suspension);
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }
        return suspensions;
    }

    public static List<Warning> getWarnings(Member member) {

        List<Warning> warnings = new ArrayList<>();
        String sql = "SELECT * FROM warns WHERE id = " + member.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl, userName, password);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            ResultSet resultSet = pstmt.executeQuery();

            while (resultSet.next()) {
                String punisherId = resultSet.getString("modId");
                Member punisher;
                if ((punisher = member.getGuild().getMemberById(punisherId)) == null) {
                    punisher = member.getGuild().getSelfMember();
                }
                String reason = resultSet.getString("reason");
                Long timeStarted = 0L;
                String caseId = "";
                Warning warning = new Warning(member, punisher, timeStarted * 1000, reason, caseId, false);
                warnings.add(warning);
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }
        return warnings;
    }
}
