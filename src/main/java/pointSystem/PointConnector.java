package pointSystem;

import net.dv8tion.jda.api.entities.Member;
import org.apache.commons.lang3.StringUtils;

import java.sql.*;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class PointConnector {

    private static ScheduledExecutorService TIMER = new ScheduledThreadPoolExecutor(1);
    private static String dbUrl = "jdbc:sqlite:database.db";

    public static boolean userExists(Member member) {
        String sql = "SELECT userId FROM rlPrefs WHERE userID = ? AND guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, member.getId());
            pstmt.setString(2, member.getGuild().getId());

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) {
                return true;
            } else return false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void createUser(Member member) {
        String sql = "INSERT INTO userPoints (guildId,userId,points,dailyRuns,dailyKeys,dailyRunStreak,questRunStreak,keyStreak,metDailyRuns,metQuotaRuns,metKeys) VALUES(?,?,0,0,0,0,0,0,0,0,0)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {
            pstmt.setString(1, member.getId());
            pstmt.setString(2, member.getGuild().getId());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addPlayerPointRuns(List<Member> memberList) {
        if (memberList.size() != 0) {
            String guildId = memberList.get(0).getGuild().getId();
            String updateString = "(";
            String sql = "UPDATE userPoints SET dailyRuns = dailyRuns + 1 WHERE guildId = " + guildId + " AND ";

            for (Member member : memberList) {
                if (!userExists(member)) {
                    createUser(member);
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

    public static void addDailyRunPoints() {
        int pointValue = 5;
        String sql = "UPDATE userPoints SET points = points + (" + pointValue + " * (1 + dailyRunStreak / 10) WHERE dailyRuns = 1 AND metDailyRuns = 0";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
