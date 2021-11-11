package quota;

import main.Config;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import shatters.SqlConnector;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DataCollector {

    private static String dbUrl = "jdbc:sqlite:database.db";

    public DataCollector() {
        //Collect data for each guild ranked 3

        //Get list of rank 3 guilds
        List<Guild> guilds = Goldilocks.jda.getGuilds().stream().filter(guild -> Database.getGuildInfo(guild, "rank").equals("3") || guild.getId().equals("343704644712923138")).collect(Collectors.toList());

        //Collect Data
        Goldilocks.TIMER.scheduleAtFixedRate(() -> {
            for (Guild guild : guilds) {
                List<LogField> guildFields = new ArrayList<>();

                //Log members in role
                for (Role r : guild.getRoles()) {
                    guildFields.add(new LogField(r.getName(), guild.getMembersWithRoles(r).size()));
                }

                if (Database.isShatters(guild)) {
                    guildFields.addAll(SqlConnector.executeLogQueries("jdbc:mysql://" + Config.get("SHATTERS_DB_IP") + "shatters"));
                } else if (guild.getId().equals("343704644712923138")) {
                    guildFields.addAll(SqlConnector.executeLogQueries("jdbc:mysql://" + Config.get("SHATTERS_DB_IP") + "halls"));
                } else {
                    guildFields.addAll(Database.executeLogQueries(guild));
                }

                logData(guild, guildFields);
            }

            System.out.println("Logged Data for Guilds");
        }, 1L, 15L, TimeUnit.MINUTES);
    }

    public static void logData(Guild guild, List<LogField> fields) {
        long time = System.currentTimeMillis();
        String sql = "INSERT INTO logData (guildId, time, field, value)" +
                " VALUES " + fields.stream().map(lf -> "(" + guild.getId() + ", " + "?, '" + lf.name.replaceAll("[^A-Za-z0-9]", "").toLowerCase() + "', '" + lf.value + "')").collect(Collectors.joining(","));

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            Time time1 = new Time(time);
            for (int i = 1; i <= fields.size(); i++) pstmt.setTime(i, time1);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Database logging error.\n" + e.getLocalizedMessage());
        }
    }

    public static List<LogField> getData(Guild guild, String field, long time) {
        return getData(guild.getId(), field, time);
    }

    public static List<LogField> getData(String guildId, String field, long time) {

        List<LogField> dataPoints = new ArrayList<>();
        String sql = "SELECT * from logData WHERE guildId = " + guildId + " AND field = '" + field + "' AND time > " + time;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();) {
            stmt.executeUpdate(sql);

            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                dataPoints.add(new LogField(field, rs.getInt("value"), rs.getTime("time").getTime()));
            }

        } catch (SQLException e) {
            System.out.println("Database logging error.\n" + e.getLocalizedMessage());
        }

        return dataPoints;
    }

    public static List<LogField> getPingData() {
        List<LogField> dataPoints = new ArrayList<>();
        String sql = "SELECT * from eventlog WHERE type = 'ping' AND time > " + ((System.currentTimeMillis() - 86400000) / 1000);

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();) {
            stmt.executeUpdate(sql);

            SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            timeFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));

            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {

                Date date = timeFormat.parse(rs.getString("time"));
                int value = rs.getInt("alias");
                if (value < 250) dataPoints.add(new LogField("ping", value, date.getTime()));
            }

        } catch (SQLException | ParseException e) {
            e.printStackTrace();
        }

        return dataPoints;
    }

}
