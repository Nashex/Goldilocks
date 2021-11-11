package setup;

import main.Goldilocks;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import quota.QuotaRole;
import utils.InputVerification;
import utils.Utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetupConnector {
    private static Connection c = null;
    private static Statement statement = null;
    private static String dbUrl = "jdbc:sqlite:database.db";

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
            System.out.println("Database connection error.");
        }
    }

    public static void updateField(Guild guild, String tableName, String field, String value) {
        String sql = "UPDATE " + tableName + " SET " + field + " = '" + value + "' WHERE guildId = " + guild.getId();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(sql);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Database update error.");
        }
    }

    public static boolean guildExists(Guild guild, String tableName) {
        String sql = "SELECT * FROM " + tableName + " WHERE guildId = " + guild.getId();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);
            return resultSet.next();
        } catch (Exception e) {
            System.out.println("Database connection error.");
        }
        return false;
    }

    public static void createGuild(Guild guild, String tableName) {
        String sql = "INSERT INTO " + tableName + " (guildId) VALUES ('" + guild.getId() + "')";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            stmt.executeUpdate(sql);
        } catch (Exception e) {
            System.out.println("Database connection error.");
        }
    }

    public static Map<String, Object> getFields(Guild guild, String tableName) {
        if (!guildExists(guild, tableName)) createGuild(guild, tableName);

        Map<String, Object> fields = new HashMap<>();
        String sql = "SELECT * from " + tableName + " WHERE guildId = " + guild.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
            Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);
            ResultSetMetaData rsmd = resultSet.getMetaData();

            int numCols = rsmd.getColumnCount();
            for (int i = 1; i <= numCols; i++) {

                String colName = rsmd.getColumnName(i);
                String visName = StringUtils.capitalize(Utils.splitCamelCase(colName)).replaceAll("(?:[\\s]|^)(Id)(?=[\\s]|$)", "");

                Object classType = String.class;
                if (colName.toLowerCase().contains("role")) classType = Role.class;
                if (colName.toLowerCase().contains("channel")) classType = TextChannel.class;
                if (colName.toLowerCase().contains("category")) classType = Category.class;
                if (colName.toLowerCase().contains("bool") || colName.startsWith("Command") || colName.startsWith("Slash")) classType = Boolean.class;
                if (colName.toLowerCase().contains("requirement")) classType = Integer.class;

                if (!colName.equalsIgnoreCase("guildId") && !classType.equals(String.class)) fields.put(visName, classType);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Database connection error.");
        }
        return fields;
    }

    public static Map<String, String> getValues(Guild guild, String tableName) {
        if (!guildExists(guild, tableName)) createGuild(guild, tableName);

        Map<String, String> values = new HashMap<>();
        String sql = "SELECT * from " + tableName + " WHERE guildId = " + guild.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);
            ResultSetMetaData rsmd = resultSet.getMetaData();

            while(resultSet.next()) {
                int numCols = resultSet.getMetaData().getColumnCount();
                for (int i = 1; i <= numCols; i++) {
                    String colName = rsmd.getColumnName(i);
                    String visName = StringUtils.capitalize(Utils.splitCamelCase(colName)).replaceAll("(?:[\\s]|^)(Id)(?=[\\s]|$)", "");
                    values.put(visName, resultSet.getString(colName));
                }
            }

        } catch (Exception e) {
            System.out.println("Database connection error.");
        }
        return values;
    }

    public static List<TextChannel> getChannels(Guild guild, String tableName) {
        List<TextChannel> textChannels = new ArrayList<>();
        List<String> invalidChannels = new ArrayList<>();

        String sql = "SELECT * from " + tableName + " WHERE guildId = " + guild.getId();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);

            while(resultSet.next()) {
                String textChannelId = resultSet.getString("channelId");
                TextChannel textChannel = InputVerification.getGuildTextChannel(guild, textChannelId);
                if (textChannel != null) textChannels.add(textChannel);
                else invalidChannels.add(textChannelId);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Database connection error.");
        }

        invalidChannels.forEach(s -> executeUpdate("DELETE from " + tableName + " WHERE channelId = " + s));
        return textChannels;
    }

    public static String getFieldValue(Guild guild, String table, String column) {
        String sql = "SELECT " + column + " from " + table + " WHERE guildId = " + guild.getId();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sql);

            if (resultSet.next()) return resultSet.getString(column);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Database connection error.");
        }

        return "";
    }

    public static boolean commandEnabled(Guild guild, Class commandClass) {
        if (!guildExists(guild, "commandConfig")) createGuild(guild, "commandConfig");
        String sql = "SELECT " + commandClass.getSimpleName() + " FROM commandConfig WHERE guildId = '" + guild.getId() + "'";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            ResultSet resultSet = stmt.executeQuery(sql);

            if (resultSet.next()) {
                return Boolean.parseBoolean(resultSet.getString(1).toLowerCase().replace("1", "true").replace("0", "false"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Database connection error.");
        }
        //Todo Create command
        return false;
    }

    public static List<QuotaRole> getQuotaRoles(Guild guild) {
        List<QuotaRole> quotaRoles = new ArrayList<>();
        List<String> invalidRoles = new ArrayList<>();
        // Columns | guildId | roleId | runReq | minRuns | assistReq | parseReq
        String sql = "SELECT * from quotaRoles WHERE guildId = " + guild.getId();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            ResultSet resultSet = stmt.executeQuery(sql);

            while(resultSet.next()) {
                String roleId = resultSet.getString("roleId");
                Role role = Goldilocks.jda.getRoleById(roleId);
                if (role != null) {
                    int runs = resultSet.getInt("runReq");
                    int minRunsForAssists = resultSet.getInt("minRuns");
                    int assists = resultSet.getInt("assistReq");
                    int parses = resultSet.getInt("parseReq");
                    quotaRoles.add(new QuotaRole(role, runs, minRunsForAssists, assists, parses));
                }
                else invalidRoles.add(roleId);
            }
        } catch (Exception e) {
            System.out.println("Database connection error.");
            e.printStackTrace();
        }

        invalidRoles.forEach(s -> executeUpdate("DELETE from quotaRoles WHERE roleId = " + s));
        return quotaRoles;
    }

}

