package moderation;

import main.Database;
import main.Goldilocks;
import moderation.punishments.Note;
import moderation.punishments.Suspension;
import moderation.punishments.Warning;
import net.dv8tion.jda.api.entities.*;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class PunishmentConnector {
    private static String dbUrl = "jdbc:sqlite:database.db";

    public static void testConnection() {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()){
            System.out.println("Database successfully connected.");
        } catch (Exception e) {
            System.out.println("Database connection error.");
        }
    }

    public static String getCaseId(Member member, String punishmentType) {
        String recipientId = member.getId();
        String caseId = "";
        int infractionNumber = 0;

        String sql = "SELECT COUNT(*) FROM punishments WHERE userId = " + member.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement statement = conn.createStatement();) {

            ResultSet resultSet = statement.executeQuery(sql);
            if (resultSet.next()) {
                infractionNumber = resultSet.getInt(1);
                String memberNamePartition = member.getEffectiveName().replaceAll("[^A-Za-z]", "").length() > 3 ?
                        member.getEffectiveName().replaceAll("[^A-Za-z]", "").toUpperCase().substring(0, 4) : member.getEffectiveName().replaceAll("[^A-Za-z]", "").toUpperCase();
                caseId = memberNamePartition + recipientId.substring(recipientId.length() - 4,
                        recipientId.length() - 1) + infractionNumber + punishmentType.substring(0 , 1);

                return caseId;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    return caseId;
    }

    public static void addPunishment(Member member, Member punisher, String reason, Long timeStarted, Long timeEnded, String punishmentType, List<Role> roles, String messageId) {

        String recipientId = member.getId();
        String guildId = member.getGuild().getId();
        String punisherId = punisher.getId();
        int infractionNumber = 0;
        StringBuilder roleStringBuilder = new StringBuilder();
        roles.forEach(role -> roleStringBuilder.append(role.getId() + " "));
        String roleString = roleStringBuilder.toString().trim();

        String sql = "SELECT COUNT(*) FROM punishments WHERE userId = " + recipientId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement statement = conn.createStatement();) {

            ResultSet resultSet = statement.executeQuery(sql);
            if (resultSet.next()) {
                infractionNumber = resultSet.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        sql = "INSERT INTO punishments(guildId,userId,punisherId,reason,timeStarted,timeEnded,punishmentType,caseId,roleIds,logMessageId) VALUES(?,?,?,?,(SELECT DATETIME('" + timeStarted
                + "','unixepoch')),(SELECT DATETIME('" + timeEnded + "','unixepoch')),?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {
            String memberNamePartition = member.getEffectiveName().replaceAll("[^A-Za-z]", "").length() > 3 ?
                    member.getEffectiveName().replaceAll("[^A-Za-z]", "").toUpperCase().substring(0, 4) : member.getEffectiveName().replaceAll("[^A-Za-z]", "").toUpperCase();
            String caseNumber = memberNamePartition + recipientId.substring(recipientId.length() - 4,
                    recipientId.length() - 1) + infractionNumber + punishmentType.substring(0 , 1);

            pstmt.setString(1, guildId);
            pstmt.setString(2, recipientId);
            pstmt.setString(3, punisherId);
            pstmt.setString(4, reason);
            pstmt.setString(5, punishmentType);
            pstmt.setString(6, caseNumber);
            pstmt.setString(7, roleString);
            pstmt.setString(8, messageId);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addPunishment(Member member, Member punisher, String reason, Long timeStarted, Long timeEnded, String punishmentType) {

        String recipientId = member.getId();
        String guildId = member.getGuild().getId();
        String punisherId = punisher.getId();
        int infractionNumber = 0;

        String sql = "SELECT COUNT(*) FROM punishments WHERE userId = " + recipientId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement statement = conn.createStatement();) {

            ResultSet resultSet = statement.executeQuery(sql);
            if (resultSet.next()) {
                infractionNumber = resultSet.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        sql = "INSERT INTO punishments(guildId,userId,punisherId,reason,timeStarted,timeEnded,punishmentType,caseId,roleIds) VALUES(?,?,?,?,(SELECT DATETIME('" + timeStarted
                + "','unixepoch')),(SELECT DATETIME('" + timeEnded + "','unixepoch')),?,?,0)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {
            String memberNamePartition = member.getEffectiveName().replaceAll("[^A-Za-z]", "").length() > 3 ?
                    member.getEffectiveName().replaceAll("[^A-Za-z]", "").toUpperCase().substring(0, 4) : member.getEffectiveName().replaceAll("[^A-Za-z]", "").toUpperCase();
            String caseNumber = memberNamePartition + recipientId.substring(recipientId.length() - 4,
                    recipientId.length() - 1) + infractionNumber + punishmentType.substring(0 , 1);

            pstmt.setString(1, guildId);
            pstmt.setString(2, recipientId);
            pstmt.setString(3, punisherId);
            pstmt.setString(4, reason);
            pstmt.setString(5, punishmentType);
            pstmt.setString(6, caseNumber);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTime(Member member, Long timeEnded, Long oldTime) {
        String sql = "Update punishments SET timeEnded = (SELECT DATETIME('" + timeEnded + "','unixepoch')) WHERE userId = " + member.getId() + " AND guildId = " + member.getGuild().getId() + " AND strftime('%s',timeEnded) > " + oldTime;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement statement = conn.createStatement();) {
            statement.executeUpdate(sql);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addPunishment(Member punisher, Long timeStarted, String caseNumber) {

        String guildId = punisher.getGuild().getId();
        String punisherId = punisher.getId();

        String sql = "INSERT INTO punishments(guildId,userId,punisherId,reason,timeStarted,timeEnded,punishmentType,caseId,roleIds) VALUES(?,?,?,?,(SELECT DATETIME('" + timeStarted
                + "','unixepoch')),(SELECT DATETIME('" + timeStarted + "','unixepoch')),?,?,0)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, guildId);
            pstmt.setString(2, "0");
            pstmt.setString(3, punisherId);
            pstmt.setString(4, "");
            pstmt.setString(5, "note");
            pstmt.setString(6, caseNumber);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static boolean caseExists(String caseId, String guildId) {
        String sql = "SELECT caseId FROM punishments WHERE caseId = ? AND guildId = " + guildId;

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, caseId);

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) {
                return true;
            } else return false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getPunishmentInfo(String caseId, String guildId) {
        Guild guild = Goldilocks.jda.getGuildById(guildId);

        String sql = "SELECT userId,punisherId,reason,strftime('%s',timeStarted),strftime('%s',timeEnded),caseId,roleIds,punishmentType,logMessageId FROM punishments WHERE caseId = ? AND guildId = " + guildId;

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, caseId);

            ResultSet resultSet = pstmt.executeQuery();

            String type;
            if (resultSet.next()) {
                 try {
                     type = resultSet.getString("punishmentType");

                     if (type.equals("suspension")) {
                         String punisherId = resultSet.getString("punisherId");
                         String recipient = resultSet.getString("userId");
                         String reason = resultSet.getString("reason");
                         Long timeStarted = resultSet.getLong("strftime('%s',timeStarted)");
                         Long timeEnded = resultSet.getLong("strftime('%s',timeEnded)");
                         String roleIds = resultSet.getString("roleIds");
                         String[] roleArr = roleIds.split(" ");

                         // (String guildId, String recipientId, String modId, String reason, long timeIssued, long timeEnding, String caseId)
                         Suspension suspension = new Suspension(guildId, recipient, punisherId, reason,timeStarted * 1000, timeEnded * 1000, caseId);

                         return suspension.toString();

                     } else if (type.contains("warning")) {
                         String recipient = resultSet.getString("userId");
                         String punisherId = resultSet.getString("punisherId");
                         String reason = resultSet.getString("reason");
                         Long timeStarted = resultSet.getLong("strftime('%s',timeStarted)");
                         boolean strict = resultSet.getString("punishmentType").equals("strict warning");
                         Warning warning = new Warning(guild.getMemberById(recipient), guild.getMemberById(punisherId), timeStarted * 1000, reason, caseId, strict);

                         return warning.toString();

                     } else {
                         //Add statement for note
                     }
                 } catch (Exception e) {
                     System.out.println("Failed to retrieve suspension for: " + resultSet.getString("userId"));
                 }

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean deletePunishment(String caseId, String guildId) {
        String sql = "DELETE FROM punishments WHERE caseId = ? AND guildId = " + guildId;

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, caseId);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void addCase(String caseId, String guildId, String evidence) {
        String sql = "INSERT INTO caseEvidence (guildId,caseId,evidence) VALUES(?,?,?)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, guildId);
            pstmt.setString(2, caseId);
            pstmt.setString(3, evidence);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getCaseEvidence(String caseId, String guildId) {
        List<String> caseEvidence = new ArrayList<>();
        String sql = "SELECT evidence FROM caseEvidence WHERE caseId = ? AND guildId = " + guildId;

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, caseId);

            ResultSet resultSet = pstmt.executeQuery();


            while (resultSet.next()) {
                caseEvidence.add(resultSet.getString("evidence"));
            }
            return caseEvidence;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return caseEvidence;
    }

    public static List<Suspension> getSuspensions(Member member) {

        List<Suspension> suspensions = new ArrayList<>();
        String sql = "SELECT userId,punisherId,reason,strftime('%s',timeStarted),strftime('%s',timeEnded),caseId,roleIds FROM punishments WHERE userId = " + member.getId() + " AND (punishmentType = 'suspension') AND guildId = " + member.getGuild().getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            ResultSet resultSet = pstmt.executeQuery();

            while (resultSet.next()) {
                String punisherId = resultSet.getString("punisherId");
                String reason = resultSet.getString("reason");
                Long timeStarted = resultSet.getLong("strftime('%s',timeStarted)");
                Long timeEnded = resultSet.getLong("strftime('%s',timeEnded)");
                String caseId = resultSet.getString("caseId");
                // (String guildId, String recipientId, String modId, String reason, long timeIssued, long timeEnding, String caseId)
                Suspension suspension = new Suspension(member.getGuild().getId(), member.getId(), punisherId, reason,timeStarted * 1000, timeEnded * 1000, caseId);
                suspensions.add(suspension);
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }
        return suspensions;
    }

    public static int getNumPunishments(Member member, String punishmentType) {

        String sql = "SELECT COUNT(caseId) WHERE userId = " + member.getId() + " AND (punishmentType = '" + punishmentType + "') AND guildId = " + member.getGuild().getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            ResultSet resultSet = pstmt.executeQuery();

            return resultSet.getInt("count(userId)");

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static HashMap<TextChannel, List<Message>> getMessages(Member member, long timeAgo) {
        HashMap<String, List<String>> idMap = new HashMap<>();
        HashMap<TextChannel, List<Message>> messageMap = new HashMap<>();
        String sql = "SELECT messageId, textChannelId FROM messageLog WHERE userId = " + member.getId() + " AND guildId = " + member.getGuild().getId() + " AND time > " + (System.currentTimeMillis() - timeAgo);
        try (Connection conn = DriverManager.getConnection(Database.messageLogdbUrl);
             Statement stmt = conn.createStatement()) {
            ResultSet resultSet = stmt.executeQuery(sql);

            while (resultSet.next()) {
                if (idMap.containsKey(resultSet.getString("textChannelId"))) idMap.get(resultSet.getString("textChannelId")).add(resultSet.getString("messageId"));
                else idMap.put(resultSet.getString("textChannelId"), new ArrayList<>(Collections.singletonList(resultSet.getString("messageId"))));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Iterate through the id map
        for (Map.Entry<String, List<String>> entry : idMap.entrySet()) {
            TextChannel textChannel = Goldilocks.jda.getTextChannelById(entry.getKey());
            if (textChannel != null) {
                messageMap.put(textChannel, new ArrayList<>());
                List<Message> messages = textChannel.getHistory().retrievePast(100).complete();
                messages = messages.stream().filter(message -> entry.getValue().contains(message.getId())).collect(Collectors.toList());
                // Add all of the valid messages to the array
                messageMap.get(textChannel).addAll(messages);
            }
        }
        return messageMap;
    }

    public static List<Warning> getWarnings(Member member) {

        List<Warning> warnings = new ArrayList<>();
        String sql = "SELECT userId,punisherId,reason,strftime('%s',timeStarted),caseId,punishmentType FROM punishments WHERE userId = " + member.getId() +
                " AND ((punishmentType = 'warning') OR (punishmentType = 'strict warning')) AND guildId = " + member.getGuild().getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            ResultSet resultSet = pstmt.executeQuery();

            while (resultSet.next()) {
                String punisherId = resultSet.getString("punisherId");
                String reason = resultSet.getString("reason");
                Long timeStarted = resultSet.getLong("strftime('%s',timeStarted)");
                String caseId = resultSet.getString("caseId");
                boolean strict = resultSet.getString("punishmentType").equals("strict warning");
                // (Member recipient, Member mod, long timePunished, String reason, String caseId, boolean strict)
                warnings.add(new Warning(member, member.getGuild().getMemberById(punisherId), timeStarted * 1000, reason, caseId, strict));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return warnings;
    }

    public static List<Note> getNotes(Member member) {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT userId, punisherId, reason, strftime('%s',timeStarted), caseId FROM punishments WHERE userId = " + member.getId() +
                " AND punishmentType = 'note' AND guildId = " + member.getGuild().getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            ResultSet resultSet = pstmt.executeQuery();
            while (resultSet.next()) {
                String punisherId = resultSet.getString("punisherId");
                String reason = resultSet.getString("reason");
                Long timeStarted = resultSet.getLong("strftime('%s',timeStarted)");
                String caseId = resultSet.getString("caseId");
                notes.add(new Note(member, member.getGuild().getMemberById(punisherId), timeStarted * 1000, reason, caseId));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notes;
    }

}

