package verification;

import net.dv8tion.jda.api.entities.Guild;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class UserDataConnector {
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

    public static void createUserData(String userID, String guildId, boolean alt, PlayerProfile playerProfile) {
        String sql = "INSERT INTO " + (alt ? "realmeyeAltData" : "realmeyeData") + " (guildId,userId,charCount,exaltCount,aliveFame,rank,accFame,firstSeen,lastSeen,guildRank,deadChars,dead400,nameChanges,guildMemberCount,formerMemberCount,guildChangeCount) " +
                "VALUES(" + guildId + "," + userID + ",";

        String playerIgn = playerProfile.getUsername();

        String charCount = Optional.ofNullable(String.valueOf(playerProfile.getCharacters())).orElse("NULL");
        String exaltCount = Optional.ofNullable(String.valueOf(playerProfile.getExaltations())).orElse("NULL");
        String aliveFame = Optional.ofNullable(String.valueOf(playerProfile.getAliveFame())).orElse("NULL");
        String rank = Optional.ofNullable(String.valueOf(playerProfile.getStars())).orElse("NULL");
        String accFame = Optional.ofNullable(String.valueOf(playerProfile.getAccountFame())).orElse("NULL");
        String firstSeen = Optional.ofNullable(String.valueOf(playerProfile.getFirstSeen())).orElse("NULL");
        if (!firstSeen.equals("NULL")) {
            if (firstSeen.equals("hidden")) {
                firstSeen = "NULL";
            } else if (firstSeen.contains("years")) {
                firstSeen = firstSeen.split(" ")[0].replaceAll("[^0-9]", "");
            } else {
                firstSeen = "0";
            }
        }
        String lastSeen = Optional.ofNullable(String.valueOf(playerProfile.isHiddenLocation())).orElse("NULL");
        lastSeen = lastSeen.equals("true") ? "1" : "0";
        String guildRank = Optional.ofNullable(String.valueOf(playerProfile.getGuildRank())).orElse("NULL");

        sql += charCount + "," + exaltCount + "," + aliveFame + "," + rank + "," + accFame + "," + firstSeen + "," + lastSeen + "," + guildRank + ",";

        List<String> graveData = BackgroundCheck.graveScrape(playerIgn);
        String deadChars = (graveData.isEmpty() ? "NULL" : String.valueOf(graveData.get(0)));
        String dead400Chars = (graveData.isEmpty() ? "NULL" : String.valueOf(graveData.get(1)));

        sql += deadChars + "," + dead400Chars + ",";

        String nameScrape = String.join("⇒", BackgroundCheck.nameChangeScrape(playerIgn));
        String nameChanges = nameScrape.contains("hidden") ? "NULL" : String.valueOf(nameScrape.split("\n").length - 1);
        String guildMemberCount = String.valueOf(BackgroundCheck.getGuildMembers(playerProfile.getGuild()).size() == 0 ? "NULL" : BackgroundCheck.getGuildMembers(playerProfile.getGuild()).size());
        String formerMemberCount = String.valueOf(BackgroundCheck.getGuildFormerMembers(playerProfile.getGuild()).size() == 0 ? "NULL" : BackgroundCheck.getGuildFormerMembers(playerProfile.getGuild()).size());

        sql += nameChanges + "," + guildMemberCount + "," + formerMemberCount + ",";

        String guildScrape = String.join(" ⇒ ", BackgroundCheck.guildScrape(playerIgn));
        String guildChangeCount = String.valueOf(guildScrape.contains("hidden") ? "NULL" : guildScrape.split(" ⇒ ").length);

        sql += guildChangeCount + ")";

        System.out.println(sql);

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();) {

            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static double getPlayerPercentage(PlayerProfile playerProfile, boolean alt) {

        int[] mainStats = new int[20];
        String statName[] = new String[14];

        String table = alt ? "realmeyeAltData" : "realmeyeData";

        String sql = "SELECT " +
                getSqlMedian("charCount", table) + ", " +
                getSqlMedian("exaltCount", table) + ", " +
                getSqlMedian("aliveFame", table) + ", " +
                getSqlMedian("rank", table) + ", " +
                getSqlMedian("accFame", table) + ", " +
                getSqlMedian("firstSeen", table) + ", " +
                getSqlMedian("lastSeen", table) + ", " +
                getSqlMedian("guildRank", table) + ", " +
                getSqlMedian("deadChars", table) + ", " +
                getSqlMedian("dead400", table) + ", " +
                getSqlMedian("nameChanges", table) + ", " +
                getSqlMedian("guildMemberCount", table) + ", " +
                getSqlMedian("formerMemberCount", table) + ", " +
                getSqlMedian("guildChangeCount", table) + ", " +
                getSqlMedian("petLevel", table);


        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            ResultSet resultSet = stmt.executeQuery(sql);

            mainStats[0] = resultSet.getInt("charCountMed");
            //mainStats[1] = resultSet.getInt("exaltCountMed");
            mainStats[1] = 1;
            mainStats[2] = resultSet.getInt("aliveFameMed");
            mainStats[3] = resultSet.getInt("rankMed");

            //mainStats[4] = resultSet.getInt("accFameMed");
            mainStats[4] = 1;
            mainStats[5] = resultSet.getInt("firstSeenMed");
            mainStats[6] = resultSet.getInt("lastSeenMed");
            mainStats[7] = resultSet.getInt("guildRankMed");

            mainStats[8] = resultSet.getInt("deadCharsMed");
            mainStats[9] = resultSet.getInt("dead400Med");
            mainStats[10] = resultSet.getInt("nameChangesMed");
            //mainStats[11] = resultSet.getInt("guildMemberCountMed");
            mainStats[11] = 1;

            //mainStats[12] = resultSet.getInt("formerMemberCountMed");
            mainStats[12] = 1;
            mainStats[13] = resultSet.getInt("guildChangeCountMed");
            mainStats[14] = resultSet.getInt("petLevelMed");

            mainStats[15] = mainStats[3];
            mainStats[16] = mainStats[3];
            mainStats[17] = mainStats[0];
            mainStats[18] = mainStats[5];
            mainStats[19] = mainStats[14];

        } catch (SQLException e) {
            e.printStackTrace();
        }

        int[] playerStats = new int[20];

        String playerIgn = playerProfile.getUsername();

        playerStats[0] = Optional.ofNullable(playerProfile.getCharacters()).orElse(0);
        //playerStats[1] = Optional.ofNullable(playerProfile.getExaltations()).orElse(0);
        playerStats[1] = 1;
        playerStats[2] = Optional.ofNullable((int) playerProfile.getAliveFame()).orElse(0);
        playerStats[3] = Optional.ofNullable(playerProfile.getStars()).orElse(0);

        //playerStats[4] = Optional.ofNullable((int) playerProfile.getAccountFame()).orElse(0);
        playerStats[4] = 1;
        String firstSeen = Optional.ofNullable(String.valueOf(playerProfile.getFirstSeen())).orElse("NULL");
        if (!firstSeen.equals("NULL")) {
            if (firstSeen.equals("hidden")) {
                playerStats[5] = 0;
            } else if (firstSeen.contains("years")) {
                playerStats[5] = Integer.parseInt(firstSeen.split(" ")[0].replaceAll("[^0-9]", ""));
            } else {
                playerStats[5] = 0;
            }
        }
        playerStats[6] = Optional.ofNullable(playerProfile.isHiddenLocation() ? 1 : 0).orElse(0);
        playerStats[7] = Optional.ofNullable(playerProfile.getGuildRank()).orElse(0);

        List<String> graveData = BackgroundCheck.graveScrape(playerIgn);
        playerStats[8] = (graveData.isEmpty() ? 0 : Integer.parseInt(graveData.get(0).replaceAll("[^0-9]","")));
        playerStats[9] = (graveData.isEmpty()  ? 0 : Integer.parseInt(graveData.get(1).replaceAll("[^0-9]","")));

        String nameScrape = String.join("⇒", BackgroundCheck.nameChangeScrape(playerIgn));
        playerStats[10] = nameScrape.contains("hidden") ? 0 : nameScrape.split("\n").length - 1;
        //playerStats[11] = BackgroundCheck.getGuildMembers(playerProfile.getGuild()).size();
        //playerStats[12] = BackgroundCheck.getGuildFormerMembers(playerProfile.getGuild()).size();
        playerStats[11] = 1;
        playerStats[12] = 1;

        String guildScrape = String.join(" ⇒ ", BackgroundCheck.guildScrape(playerIgn));
        String petScrape = BackgroundCheck.petScrape(playerIgn);
        playerStats[13] = guildScrape.contains("hidden") ? 0 : guildScrape.split(" ⇒ ").length;
        playerStats[14] = petScrape.replaceAll("[^0-9]", "").isEmpty() ? 0 : Integer.parseInt(petScrape);

        playerStats[15] = playerStats[3];
        playerStats[16] = playerStats[3];
        playerStats[17] = playerStats[0];
        playerStats[18] = playerStats[5];
        playerStats[19] = playerStats[14];

        double percentageTotal = 0.0;
        double percentageMult = 1.0 / mainStats.length;
        for (int i = 0; i < mainStats.length; i++) {
            //System.out.print(playerStats[i] + " " + mainStats[i] + " | ");
            Double localPercent = (double) playerStats[i] / mainStats[i];
            if (localPercent > 1.0 || localPercent < 0.0) localPercent = 1.0;
            else if (localPercent.isNaN()) localPercent = 1.0;

            percentageTotal += localPercent * percentageMult;
            //System.out.println(localPercent);
        }

        //System.out.println(percentageTotal);
        if (playerStats[8] < 10 && playerStats[8] > 0 && alt) return .9999;
        if (alt) return 1.0 - percentageTotal;
        return percentageTotal;

    }

    public static void getPetAbilities(Guild guild, String table, boolean alt) {

        String sql = "SELECT userId FROM " + table;
        Map<String, String> nameToIdMap = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){


            ResultSet resultSet = stmt.executeQuery(sql);

            while (resultSet.next()) {
                String userId = resultSet.getString("userId");
                nameToIdMap.put(guild.getMemberById(userId).getEffectiveName().replace("|", ",").split(",")[(alt ? 1 : 0)].replaceAll("[^A-z]", ""), userId);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        nameToIdMap.forEach((s, s2) -> {
            String petLevel = BackgroundCheck.petScrape(s);

            if (petLevel.contains("hidden") || petLevel.contains("No pets detected.")) {
                petLevel = "NULL";
            }
            updatePet(s2, petLevel, table);
            System.out.println("Added pet level of: " + petLevel + " to " + s);

        });


    }

    public static void updatePet(String userId, String level, String table) {
        String sql = "UPDATE " + table + " SET petLevel = " + level + " WHERE userId = " + userId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){

            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static String getSqlMedian(String field, String table) {
        return "(SELECT " + field + " FROM " + table + " ORDER BY " + field + " LIMIT 1 OFFSET (SELECT COUNT(*) FROM " + table + ") / 2) AS " + field + "Med";
    }

}
