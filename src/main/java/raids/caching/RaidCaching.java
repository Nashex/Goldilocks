package raids.caching;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import main.Goldilocks;
import net.dv8tion.jda.api.entities.*;
import raids.HeadCount;
import raids.KeyPanel;
import raids.Raid;
import raids.RaidHub;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RaidCaching {
    private static String dbUrl = "jdbc:sqlite:database.db";

    public static void createRaid(Long guildId, Long leaderId, Long voiceChannelId, Long raidCommandsChannelId, Long raidStatusChannelId,
                                          String location, int raidType, Long raidMessageId, Long controlPanelId, boolean raidActive, Long feedbackChannelId, Long feedbackMessageId) {
        String sql = "INSERT INTO activeRaids (guildId,leaderId,voiceChannelId,raidCommandsChannelId,raidStatusChannelId,location,raidType,raidMessageId,controlPanelId,raidActive,feedbackChannelId,feedbackMessageId) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setLong(1, guildId);
            pstmt.setLong(2, leaderId);
            pstmt.setLong(3, voiceChannelId);
            pstmt.setLong(4, raidCommandsChannelId);
            pstmt.setLong(5, raidStatusChannelId);
            pstmt.setString(6, location);
            pstmt.setInt(7, raidType);
            pstmt.setLong(8, raidMessageId);
            pstmt.setLong(9, controlPanelId);
            pstmt.setBoolean(10, raidActive);
            pstmt.setLong(11, feedbackChannelId);
            pstmt.setLong(12, feedbackMessageId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String cacheRaid(Raid raid) {
        try {
            JsonSerializer<Member> memberJsonSerializer = new JsonSerializer<Member>() {
                @Override
                public JsonElement serialize(Member src, Type typeOfSrc, JsonSerializationContext context) {
                    JsonObject jsonMember = new JsonObject();
                    jsonMember.addProperty("id", src.getId());
                    jsonMember.addProperty("name", src.getEffectiveName());
                    jsonMember.addProperty("avatarUrl", src.getUser().getAvatarUrl());
                    return jsonMember;
                }
            };

            JsonSerializer<Guild> guildJsonSerializer = new JsonSerializer<Guild>() {
                @Override
                public JsonElement serialize(Guild src, Type typeOfSrc, JsonSerializationContext context) {
                    JsonObject jsonMember = new JsonObject();
                    jsonMember.addProperty("id", src.getId());
                    jsonMember.addProperty("name", src.getName());
                    jsonMember.addProperty("iconUrl", src.getIconUrl());
                    return jsonMember;
                }
            };

            JsonSerializer<TextChannel> textChannelJsonSerializer = new JsonSerializer<TextChannel>() {
                @Override
                public JsonElement serialize(TextChannel src, Type typeOfSrc, JsonSerializationContext context) {
                    JsonObject jsonMember = new JsonObject();
                    jsonMember.addProperty("id", src.getId());
                    jsonMember.addProperty("name", src.getName());
                    return jsonMember;
                }
            };

            JsonSerializer<List<Member>> memberListJsonSerializer = new JsonSerializer<List<Member>>() {
                @Override
                public JsonElement serialize(List<Member> src, Type typeOfSrc, JsonSerializationContext context) {
                    JsonArray jsonMember = new JsonArray();
                    for (Member m : src) jsonMember.add(memberJsonSerializer.serialize(m, typeOfSrc, context));
                    return jsonMember;
                }
            };

            JsonSerializer<VoiceChannel> voiceChannelJsonSerializer = new JsonSerializer<VoiceChannel>() {
                @Override
                public JsonElement serialize(VoiceChannel src, Type typeOfSrc, JsonSerializationContext context) {
                    JsonObject jsonMember = new JsonObject();
                    jsonMember.addProperty("id", src.getId());
                    jsonMember.addProperty("name", src.getName());
                    jsonMember.add("members", memberListJsonSerializer.serialize(src.getMembers(), typeOfSrc, context));
                    return jsonMember;
                }
            };

            JsonSerializer<Message> messageJsonSerializer = new JsonSerializer<Message>() {
                @Override
                public JsonElement serialize(Message src, Type typeOfSrc, JsonSerializationContext context) {
                    JsonObject jsonMember = new JsonObject();
                    if (src != null) {
                        jsonMember.addProperty("id", src.getId());
                        jsonMember.addProperty("channelId", src.getTextChannel().getId());
                    } else {
                        jsonMember.addProperty("id", "null");
                        jsonMember.addProperty("channelId", "null");
                    }
                    return jsonMember;
                }
            };

            JsonSerializer<KeyPanel> keyPanelJsonSerializer = new JsonSerializer<KeyPanel>() {
                @Override
                public JsonElement serialize(KeyPanel src, Type typeOfSrc, JsonSerializationContext context) {
                    JsonObject jsonPanel = new JsonObject();

                    JsonArray keyPoppers = new JsonArray();
                    for (Map.Entry<Member, Integer> e : src.keyPoppers.entrySet()) {
                        JsonObject keyPopper = new JsonObject();
                        keyPopper.add("member", memberJsonSerializer.serialize(e.getKey(), typeOfSrc, context));
                        keyPopper.addProperty("numPops", "" + e.getValue());
                        keyPoppers.add(keyPopper);
                    }

                    jsonPanel.add("controlPanel", messageJsonSerializer.serialize(src.controlPanel, typeOfSrc, context));
                    jsonPanel.add("keyPoppers", keyPoppers);

                    return jsonPanel;
                }
            };

            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .registerTypeAdapter(Member.class, memberJsonSerializer)
                    .registerTypeAdapter(Guild.class, guildJsonSerializer)
                    .registerTypeAdapter(TextChannel.class, textChannelJsonSerializer)
                    .registerTypeAdapter(VoiceChannel.class, voiceChannelJsonSerializer)
                    .registerTypeAdapter(Message.class, messageJsonSerializer)
                    .registerTypeAdapter(KeyPanel.class, keyPanelJsonSerializer)
                    .registerTypeAdapter(new TypeToken<List<Member>>() {}.getType(), memberListJsonSerializer)
                    .setPrettyPrinting()
                    .create();
            String json = gson.toJson(raid);
            return json;
        } catch (Exception e) { e.printStackTrace(); }
        return "";
    }

    public static void retrieveRaids() {
        String sql = "SELECT * FROM activeRaids";
        List<RaidCache> raidCaches = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();) {

            ResultSet resultSet = stmt.executeQuery(sql);
            while (resultSet.next()) {
                String guildId = resultSet.getString("guildId");
                String leaderId = resultSet.getString("leaderId");
                int raidType = resultSet.getInt("raidType");
                String raidStatusChannelId = resultSet.getString("raidStatusChannelId");
                String raidCommandsChannelId = resultSet.getString("raidCommandsChannelId");
                String location = resultSet.getString("location");
                String voiceChannelId = resultSet.getString("voiceChannelId");
                String raidMessageId = resultSet.getString("raidMessageId");
                String controlPanelId = resultSet.getString("controlPanelId");
                String keyPanelId = resultSet.getString("keyPanelId");
                String feedbackChannelId = resultSet.getString("feedbackChannelId");
                String feedbackMessageId = resultSet.getString("feedbackMessageId");
                boolean isStarted = resultSet.getBoolean("raidActive");
                String logPanelJson = resultSet.getString("logPanel");

                raidCaches.add(new RaidCache(guildId, leaderId, raidType, raidStatusChannelId, raidCommandsChannelId, location, -1,
                        voiceChannelId, raidMessageId, controlPanelId, isStarted, feedbackChannelId, feedbackMessageId, keyPanelId, logPanelJson));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for (RaidCache cache : raidCaches) {
            try {
                cache.retrieve();
            } catch (RaidCacheException e) {
                System.out.println(e.getLocalizedMessage());
            }
        }

    }

    public static void setRaidActive(Long voiceChannelId, boolean raidActive) {
        String sql = "UPDATE activeRaids SET raidActive = ? WHERE voiceChannelId = " + voiceChannelId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {
            pstmt.setBoolean(1, raidActive);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateVoiceChannel(Long voiceChannelId, Long raidLeaderId) {
        String sql = "UPDATE activeRaids SET voiceChannelId = ? WHERE leaderId = " + raidLeaderId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {
            pstmt.setLong(1, voiceChannelId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateFeedback(TextChannel feedbackChannel, Message feedbackMessage, Member raidLeader) {
        String sql = "UPDATE activeRaids SET feedbackChannelId = ?, feedbackMessageId = ? WHERE leaderId = " + raidLeader.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {
            pstmt.setString(1, feedbackChannel.getId());
            pstmt.setString(2, feedbackMessage.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteRaid(Long voiceChannelId) {
        String sql = "DELETE FROM activeRaids WHERE voiceChannelId = " + voiceChannelId;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void retrieveHeadcounts() {
        String sql = "SELECT * FROM activeHeadCounts";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();) {

            ResultSet resultSet = stmt.executeQuery(sql);
            while (resultSet.next()) {
                try {
                    Guild guild = Goldilocks.jda.getGuildById(resultSet.getString("guildId"));
                    Member leader = guild.getMemberById(resultSet.getString("leaderId"));
                    int raidType = resultSet.getInt("raidType");
                    TextChannel controlChannel = guild.getTextChannelById(resultSet.getString("controlChannelId"));
                    Message controlPanel = controlChannel.retrieveMessageById(resultSet.getString("controlPanelId")).complete();
                    TextChannel statusChannel = guild.getTextChannelById(resultSet.getString("statusChannelId"));
                    Message headCountMessage = statusChannel.retrieveMessageById(resultSet.getString("headcountId")).complete();

                    HeadCount headCount = new HeadCount(leader, controlChannel, statusChannel, raidType, controlPanel, headCountMessage);
                    RaidHub.activeHeadcounts.add(headCount);
                    System.out.println("Successfully retrieved headcount for: " + resultSet.getString("guildId") + " | RL: " + resultSet.getString("leaderId"));
                } catch (Exception e) {
                    System.out.println("Failed to retrieve headcount for: " + resultSet.getString("guildId") + " | RL: " + resultSet.getString("leaderId"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createHeadCount(HeadCount headCount) {
        String sql = "INSERT INTO activeHeadcounts (guildId,leaderId,raidType,controlChannelId,controlPanelId,statusChannelId,headcountId) VALUES (?,?,?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, headCount.getControlPanel().getGuild().getId());
            pstmt.setString(2, headCount.getRaidLeader().getId());
            pstmt.setInt(3, headCount.getRaidType());
            pstmt.setString(4, headCount.getControlPanel().getTextChannel().getId());
            pstmt.setString(5, headCount.getControlPanel().getId());
            pstmt.setString(6, headCount.getHeadCountMessage().getTextChannel().getId());
            pstmt.setString(7, headCount.getHeadCountMessage().getId());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteHeadCount(String messageId) {
        String sql = "DELETE FROM activeHeadcounts WHERE headcountId = " + messageId;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
