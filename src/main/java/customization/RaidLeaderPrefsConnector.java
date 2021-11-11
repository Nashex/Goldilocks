package customization;

import main.Goldilocks;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Member;

import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class RaidLeaderPrefsConnector {
    private static ScheduledExecutorService TIMER = new ScheduledThreadPoolExecutor(1);
    private static String dbUrl = "jdbc:sqlite:database.db";

    public static boolean staffExists(String userID, String guildId) {
        String sql = "SELECT userId FROM rlPrefs WHERE userID = ? AND guildId = ?";

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
        String sql = "INSERT INTO rlPrefs (guildId,userId,color,emoteOneId,emoteTwoId,emoteThreeId,keyCp) VALUES(?,?,0,0,0,0,1)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {
            pstmt.setString(1, guildId);
            pstmt.setString(2, userID);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<Emote> getRlEmotes(String userId, String guildId) {
        List<Emote> rlEmotes = new ArrayList<>();

        String sql = "SELECT emoteOneId,emoteTwoId,emoteThreeId FROM rlPrefs WHERE userID = ? AND guildId = ?";

        if (!staffExists(userId, guildId)) {
            createStaff(userId, guildId);
        }

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, userId);
            pstmt.setString(2, guildId);
            ResultSet resultSet = pstmt.executeQuery();

            String emoteOneId = resultSet.getString("emoteOneId");
            String emoteTwoId = resultSet.getString("emoteTwoId");
            String emoteThreeId = resultSet.getString("emoteThreeId");

            if (!emoteOneId.equals("0")) {
                Emote emote = Goldilocks.jda.getEmoteById(emoteOneId);
                rlEmotes.add(emote);
            }
            if (!emoteTwoId.equals("0")) {
                Emote emote = Goldilocks.jda.getEmoteById(emoteTwoId);
                rlEmotes.add(emote);
            }
            if (!emoteThreeId.equals("0")) {
                Emote emote = Goldilocks.jda.getEmoteById(emoteThreeId);
                rlEmotes.add(emote);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rlEmotes;
    }

    public static Color getRlColor(String userId, String guildId) {
        Color rlColor = null;

        String sql = "SELECT color FROM rlPrefs WHERE userID = ? AND guildId = ?";

        if (!staffExists(userId, guildId)) {
            createStaff(userId, guildId);
        }

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, userId);
            pstmt.setString(2, guildId);
            ResultSet resultSet = pstmt.executeQuery();

            String colorIntValue = resultSet.getString("color");

            if (!colorIntValue.equals("0")) {
                rlColor = new Color(Integer.valueOf(colorIntValue));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rlColor;
    }

    public static boolean getRlKeyCp(Member member) {
        boolean rlKeyCp = false;

        String sql = "SELECT keyCp FROM rlPrefs WHERE userID = ? AND guildId = ?";

        if (!staffExists(member.getId(), member.getGuild().getId())) {
            createStaff(member.getId(), member.getGuild().getId());
        }

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, member.getId());
            pstmt.setString(2, member.getGuild().getId());
            ResultSet resultSet = pstmt.executeQuery();

            String booleanValue = resultSet.getString("keyCp");

            rlKeyCp = booleanValue.equals("1") ? true : false;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rlKeyCp;
    }

    public static void setRlEmote(String guildId, String userId, String emoteId, String emoteNum) {

        String sql = "UPDATE rlPrefs SET " + emoteNum + " = " + emoteId + " WHERE guildId = " + guildId + " AND userId = " + userId;

        //System.out.println(sql);
        try (Connection conn = DriverManager.getConnection(dbUrl);

             Statement stmt = conn.createStatement()){
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static void setRlColor(String userId, String guildId, Color color) {

        String sql = "UPDATE rlPrefs SET color = " + color.getRGB() + " WHERE guildId = " + guildId + " AND userId = " + userId;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static void toggleKeyControlPanel(Member member) {

        String sql = "UPDATE rlPrefs SET keyCp = ((keyCp | 1) - (keyCp & 1)) WHERE guildId = " + member.getGuild().getId() + " AND userId = " + member.getId();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static void resetPreferences(String guildId, String userId) {

        String sql = "UPDATE rlPrefs SET color = 0, emoteOneId = 0, emoteTwoId = 0, emoteThreeId = 0 WHERE guildId = " + guildId + " AND userId = " + userId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

}
