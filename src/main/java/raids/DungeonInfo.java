package raids;

import main.Database;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DungeonInfo {

    public static String[] eventDungeon = {"771201102464942101", "771201091840901122", "", "Event Dungeon", "", String.valueOf((new Color(255, 70, 70)).getRGB()), "50", "",""};
    private static String dbUrl = "jdbc:sqlite:database.db";

    public static String[][] oldDungeonInfo() {

        String[][] dungeonInfo = new String[100][];

        String sql = "SELECT * FROM dungeonInfo WHERE guildId = 0 ORDER BY dungeonId ASC";
        try (Connection conn = DriverManager.getConnection(dbUrl);
            Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery("SELECT count(dungeonID) FROM dungeonInfo WHERE userID = 0");
            dungeonInfo = new String[resultSet.getInt(1)][];
            resultSet = stmt.executeQuery(sql);
            int index = 0;

            while(resultSet.next()) {
                String[] dungeonInfoS = {
                        resultSet.getString("portalEmoteID"),
                        resultSet.getString("keyEmoteId"),
                        resultSet.getString("earlyLocEmoteIds"),
                        resultSet.getString("dungeonName"),
                        resultSet.getString("additionalEmoteIds"),
                        resultSet.getString("embedColor"),
                        resultSet.getString("dungeonVCcap"),
                        resultSet.getString("startingImageURL"),
                        resultSet.getString("onGoingImageURL")
                };
                dungeonInfo[index] = dungeonInfoS;
                index++;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dungeonInfo;
    }

    public static List<Dungeon> unOrderedDungeonInfo(Guild guild) {
        List<Dungeon> dungeons = new ArrayList<>();

        String sql = "SELECT * FROM dungeonInfo WHERE userID = 0 AND (guildId = " + guild.getId() + " OR guildId = 0) ORDER BY dungeonId ASC, guildId DESC";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);
            while(resultSet.next()) {
                String dungeonName = resultSet.getString("dungeonName");
                String dungeonCategory = resultSet.getString("category");
                int dungeonIndex = resultSet.getInt("dungeonId");

                String[] dungeonInfoS = {
                        resultSet.getString("portalEmoteID"),
                        resultSet.getString("keyEmoteId"),
                        resultSet.getString("earlyLocEmoteIds"),
                        dungeonName,
                        resultSet.getString("additionalEmoteIds"),
                        resultSet.getString("embedColor"),
                        resultSet.getString("dungeonVCcap"),
                        resultSet.getString("startingImageURL"),
                        resultSet.getString("onGoingImageURL")
                };
                if (dungeons.stream().noneMatch(dungeon -> dungeon.dungeonIndex == dungeonIndex)) dungeons.add(new Dungeon(dungeonIndex, dungeonName, dungeonCategory, dungeonInfoS));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return dungeons;
    }

    public static List<Dungeon> nDungeonInfo(Guild guild) {
        List<Dungeon> dungeons = unOrderedDungeonInfo(guild);

        String[] dungeonTypes = {"legendary", "court", "epic", "highlands", "random", "randomLobby", "none"};
        List<Dungeon> sortedDungeons = new ArrayList<>();

        for (String s : dungeonTypes) sortedDungeons.addAll(dungeons.stream().filter(d -> d.dungeonCategory.equals(s)).collect(Collectors.toList()));
        return sortedDungeons;
    }

    public static Dungeon dungeonInfo(Guild guild, int id) {
        return nDungeonInfo(guild).get(id);
    }

    public static List<Dungeon> userDungeonInfo(Member member) {

        List<Dungeon> dungeons = new ArrayList<>();

        String sql = "SELECT * FROM dungeonInfo WHERE userID = 0 ORDER BY dungeonId ASC";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);
            while(resultSet.next()) {
                String dungeonName = resultSet.getString("dungeonName");
                String dungeonCategory = resultSet.getString("category");

                dungeons.add(new Dungeon(0, dungeonName, dungeonCategory, new String[]{""}));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (!Database.exists("rlLeadingSettings", member.getId())) Database.createRlSettings(member);

        sql = "SELECT * FROM rlLeadingSettings WHERE userID = " + member.getId();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);
            while(resultSet.next()) {

                for (int i = 0; i < dungeons.size(); i++) {
                    dungeons.get(i).setEnabled(resultSet.getBoolean(i));
                }

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return dungeons;
    }

    public static boolean hasCustomDungeon(Guild guild, int dungeonId) {
        String sql = "SELECT * FROM dungeonInfo WHERE dungeonId = ? AND guildId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setInt(1, dungeonId);
            pstmt.setString(2, guild.getId());

            ResultSet resultSet = pstmt.executeQuery();

            return resultSet.next();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /*
     DUNGEON ARRAY INFORMATION
         [0] portal emote
         [1] key emote
         [2] early loc emotes
         [3] dungeon name
         [4] additional emotes
         [5] embed color
         [6] dungeon vc cap
         [7] starting image url
         [8] on going image url
     */

    public static void updateDungeon(Guild guild, Dungeon dungeon) {
        if (!hasCustomDungeon(guild, dungeon.dungeonIndex)) {
            addDungeon(guild, dungeon); // Insert a new dungeon if no custom dungeon
            return;
        }

        String sql = "UPDATE dungeonInfo SET portalEmoteId = ?, keyEmoteId = ?, earlyLocEmoteIds = ?, dungeonName = ?, additionalEmoteIds = ?, " +
                "embedColor = ?, dungeonVCcap = ?, category = ? WHERE dungeonId = " + dungeon.dungeonIndex + " AND guildId = " + guild.getId();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, dungeon.dungeonInfo[0]);
            pstmt.setString(2, dungeon.dungeonInfo[1]);
            pstmt.setString(3, dungeon.dungeonInfo[2]);
            pstmt.setString(4, dungeon.dungeonInfo[3]);
            pstmt.setString(5, dungeon.dungeonInfo[4]);
            pstmt.setString(6, dungeon.dungeonInfo[5]);
            pstmt.setString(7, dungeon.dungeonInfo[6]);
            pstmt.setString(8, dungeon.dungeonCategory);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeDungeon(Guild guild, Dungeon dungeon) {
        String sql = "DELETE FROM dungeonInfo WHERE dungeonId = " + dungeon.dungeonIndex + " AND guildId = " + guild.getId();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void addDungeon(Guild guild, Dungeon dungeon) {
        String sql = "INSERT INTO dungeonInfo VALUES(?, 0, ?, ?, ?, ?, ?, ?, ?, ?, '', '', ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, guild.getId());
            pstmt.setInt(2, dungeon.dungeonIndex);
            pstmt.setString(3, dungeon.dungeonInfo[0]);
            pstmt.setString(4, dungeon.dungeonInfo[1]);
            pstmt.setString(5, dungeon.dungeonInfo[2]);
            pstmt.setString(6, dungeon.dungeonInfo[3]);
            pstmt.setString(7, dungeon.dungeonInfo[4]);
            pstmt.setString(8, dungeon.dungeonInfo[5]);
            pstmt.setString(9, dungeon.dungeonInfo[6]);
            pstmt.setString(10, dungeon.dungeonCategory);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


//    public static String[][] dungeonInfo() {
//        return dungeonInfo(Goldilocks.jda.getSelfUser().getId(), false);
//    }
//
//    public static String[][] dungeonInfo(Guild guild) {
//        return dungeonInfo(guild.getId(), true);
//    }

}
