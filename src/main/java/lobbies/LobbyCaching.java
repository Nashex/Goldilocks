package lobbies;

import main.Goldilocks;
import net.dv8tion.jda.api.entities.*;

import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LobbyCaching {
    private static String dbUrl = "jdbc:sqlite:database.db";

    public static void createLobbyManager(Long lobbyMessageId, String lobbyName, Long textChannelId, int lobbyLimit, Color messageColor, Long lobbyEnterEmoteId, String lobbyCreateEmoteId) {
        String sql = "INSERT INTO lobbyManagers (lobbyManagerId,lobbyMessageId,lobbyName,textChannelId,lobbyLimit,messageColor,lobbyEnterEmoteId,lobbyCreateEmoteId) VALUES ((SELECT Count(lobbyManagerId) FROM lobbyManagers),?,?,?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setLong(1, lobbyMessageId);
            pstmt.setString(2, lobbyName);
            pstmt.setLong(3, textChannelId);
            pstmt.setInt(4, lobbyLimit);
            pstmt.setString(5, String.valueOf(messageColor.getRGB()));
            pstmt.setLong(6, lobbyEnterEmoteId);
            pstmt.setString(7, lobbyCreateEmoteId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteLobbyManager(Long lobbyManagerMessageId) {
        String sql = "DELETE FROM lobbyManagers WHERE lobbyMessageId = " + lobbyManagerMessageId;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int getNumLobbyManagers() {
        String sql = "SELECT count(lobbyManagerId) FROM lobbyManagers";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement statement = conn.createStatement();) {

            ResultSet resultSet = statement.executeQuery(sql);

            return resultSet.getInt(1);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static HashMap<User, Message> getQueuePlayers(int lobbyManagerId) {
        HashMap<User, Message> playerMessageMap = new HashMap<>();

        String sql = "SELECT * FROM queuePlayers WHERE lobbyManagerId = " + lobbyManagerId + " ORDER BY playerQueueIndex ASC";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement statement = conn.createStatement();) {

            ResultSet resultSet = statement.executeQuery(sql);

            while (resultSet.next()) {
                Long userId = resultSet.getLong("playerId");
                User user = Goldilocks.jda.getUserById(userId);
                Long messageId = resultSet.getLong("playerMessageId");
                Message message = user.openPrivateChannel().complete().retrieveMessageById(messageId).complete();

                playerMessageMap.put(user, message);
            }

            return playerMessageMap;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static LobbyManager getLobbyManager(int lobbyManagerId) {
        LobbyManager lobbyManager;
        String sql = "SELECT * FROM lobbyManagers WHERE lobbyManagerId = " + lobbyManagerId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement statement = conn.createStatement();) {

            ResultSet resultSet = statement.executeQuery(sql);

            String lobbyName = resultSet.getString("lobbyName");
            TextChannel textChannel = Goldilocks.jda.getTextChannelById(resultSet.getLong("textChannelId"));
            //Message lobbyMessage = textChannel.getHistory().getMessageById(resultSet.getLong("lobbyMessageId"));
            int lobbyLimit = resultSet.getInt("lobbyLimit");
            Color color = new Color(Integer.valueOf(resultSet.getString("messageColor")));
            Emote enterEmote = Goldilocks.jda.getEmoteById(resultSet.getLong("lobbyEnterEmoteId"));
            List<Emote> createEmote = new ArrayList<>();
            String[] createEmotes = resultSet.getString("lobbyCreateEmoteId").split(" ");
            for (String string : createEmotes) {
                createEmote.add(Goldilocks.jda.getEmoteById(string.trim()));
            }

            lobbyManager = new LobbyManager(lobbyName, enterEmote, createEmote, textChannel, color, lobbyLimit, true, lobbyManagerId);

            return lobbyManager;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Message getLobbyManagerMessage(int lobbyManagerId) {
        Message lobbyManagerMessage;
        String sql = "SELECT textChannelId,lobbyMessageId FROM lobbyManagers WHERE lobbyManagerId = " + lobbyManagerId;

        //System.out.println(sql);

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement statement = conn.createStatement();) {

            ResultSet resultSet = statement.executeQuery(sql);

            TextChannel textChannel = Goldilocks.jda.getTextChannelById(resultSet.getLong("textChannelId"));
            Long messageId = resultSet.getLong("lobbyMessageId");
            Message lobbyMessage = textChannel.retrieveMessageById(messageId).complete();

            lobbyManagerMessage = lobbyMessage;

            return lobbyManagerMessage;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void createLobby(Long lobbyManagerMessageId, Long lobbyMasterId, Long voiceChannelId,Long textChannelId, String location, Long infoMessageId, Long controlPanelMessageId) {
        String sql = "INSERT INTO activeLobbies (lobbyManagerId,lobbyId,lobbyMasterId,voiceChannelId,textChannelId,location,infoMessageId,controlPanelMessageId) VALUES (?,(SELECT Count(lobbyId) FROM activeLobbies),?,?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setLong(1, getLobbyManagerId(lobbyManagerMessageId));
            pstmt.setLong(2, lobbyMasterId);
            pstmt.setLong(3, voiceChannelId);
            pstmt.setLong(4, textChannelId);
            pstmt.setString(5, location);
            pstmt.setLong(6, infoMessageId);
            pstmt.setLong(7, controlPanelMessageId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Message getLobbyControlPanel(Member lobbyMaster, int lobbyId) {
        Message lobbyManagerMessage;
        String sql = "SELECT controlPanelMessageId FROM activeLobbies WHERE lobbyId = " + lobbyId;

        //System.out.println(sql);

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement statement = conn.createStatement();) {

            ResultSet resultSet = statement.executeQuery(sql);

            PrivateChannel privateChannel = lobbyMaster.getUser().openPrivateChannel().complete();
            Long messageId = resultSet.getLong("controlPanelMessageId");
            Message lobbyMessage = privateChannel.retrieveMessageById(messageId).complete();

            lobbyManagerMessage = lobbyMessage;

            return lobbyManagerMessage;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static List<User> getLobbyParticipants(int lobbyId) {
        List<User> lobbyParticipants = new ArrayList<>();
        String sql = "SELECT * FROM lobbyPlayers WHERE lobbyId = " + lobbyId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement statement = conn.createStatement();) {

            ResultSet resultSet = statement.executeQuery(sql);

            while (resultSet.next()) {
               Long playerId = resultSet.getLong("playerId");
               User user = Goldilocks.jda.getUserById(playerId);

               lobbyParticipants.add(user);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lobbyParticipants;

    }

    public static List<Lobby> getLobbies(int lobbyManagerId) {
        List<Lobby> lobbyList = new ArrayList<>();
        String sql = "SELECT * FROM activeLobbies WHERE lobbyManagerId = " + lobbyManagerId;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement statement = conn.createStatement();) {

            ResultSet resultSet = statement.executeQuery(sql);

            while (resultSet.next()) {
                int lobbyId = resultSet.getInt("lobbyId");
                List<User> lobbyParticipants = getLobbyParticipants(lobbyId);
                TextChannel textChannel = Goldilocks.jda.getTextChannelById(resultSet.getLong("textChannelId"));
                Guild guild = textChannel.getGuild();
                String location = resultSet.getString("location");
                Member lobbyMaster = guild.getMemberById(resultSet.getLong("lobbyMasterId"));
                VoiceChannel voiceChannel = Goldilocks.jda.getVoiceChannelById(resultSet.getLong("voiceChannelId"));
                Message lobbyManagerMessage = getLobbyManagerMessage(lobbyManagerId);
                Message controlPanelMessage = getLobbyControlPanel(lobbyMaster, lobbyId);

                lobbyList.add(new Lobby(lobbyMaster, lobbyParticipants, location, voiceChannel, textChannel, "G.O.L.D. Lobby", 15, lobbyManagerMessage, controlPanelMessage, true));
            }

            return lobbyList;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lobbyList;
    }

    public static int getLobbyId(Long voiceChannelId) {
        int lobbyId = -1;

        String sql = "SELECT lobbyId FROM activeLobbies WHERE voiceChannelId = " + voiceChannelId;

        //System.out.println(sql);

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            ResultSet resultSet = pstmt.executeQuery();

            lobbyId = resultSet.getInt(1);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lobbyId;
    }

    public static void deleteLobby(Long lobbyVoiceChannelId) {
        String sql = "DELETE FROM activeLobbies WHERE voiceChannelId = " + lobbyVoiceChannelId;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createLobbyPlayer(int lobbyId, Long playerId, String playerName) {
        String sql = "INSERT INTO lobbyPlayers (lobbyId,playerId,playerName) VALUES (?,?,?)";

        //System.out.println(sql);

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setLong(1, lobbyId);
            pstmt.setLong(2, playerId);
            pstmt.setString(3, playerName);
            pstmt.executeUpdate();


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void cacheLobbyPlayers(List<User> lobbyParticipants, Long voiceChannelId) {
        List<User> currentCachedPlayers = getCurrentPlayerCache("lobbyPlayers");
        int lobbyId = getLobbyId(voiceChannelId);
        for (User user : currentCachedPlayers) {

            Long playerId = user.getIdLong();
            String sql = "DELETE FROM lobbyPlayers WHERE lobbyId = " + getLobbyId(voiceChannelId);

            //System.out.println(sql);

            if (!lobbyParticipants.contains(currentCachedPlayers)) {
                try (Connection conn = DriverManager.getConnection(dbUrl);
                     PreparedStatement pstmt = conn.prepareStatement(sql);) {

                    pstmt.executeUpdate();

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        for (User user : lobbyParticipants) {
            Long playerId = user.getIdLong();
            if (!playerExistsInTable(user, "lobbyPlayers")) {
                createLobbyPlayer(lobbyId, playerId, user.getName());
            }

        }

    }

    public static void deleteLobbyPlayers(Long voiceChannelId) {

        int lobbyId = getLobbyId(voiceChannelId);

        String sql = "DELETE FROM lobbyPlayers WHERE lobbyId = " + lobbyId;

        //System.out.println(sql);

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean playerExistsInTable(User user, String tableName) {
        boolean playerExists = false;

        String sql = "SELECT count(playerId) FROM " + tableName + " WHERE playerId = " + user.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.getLong(1) >= 1) {
                //System.out.println(playerExists);

                playerExists = true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        //System.out.println(playerExists);

        return playerExists;
    }

    public static void createPlayerQueueCache (int lobbyManagerId, int playerIndex, Long playerId, Long messageId) {
        String sql = "INSERT INTO queuePlayers (lobbyManagerId,playerQueueIndex,playerId,playerMessageId) VALUES (?,?,?,?)";

        //System.out.println(sql);

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setLong(1, lobbyManagerId);
            pstmt.setInt(2, playerIndex);
            pstmt.setLong(3, playerId);
            pstmt.setLong(4, messageId);
            pstmt.executeUpdate();


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<User> getCurrentPlayerCache(String table) {
        List<User> cachedPlayers = new ArrayList<>();

        String sql = "SELECT playerId FROM " + table;

        //System.out.println(sql);

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            ResultSet resultSet = pstmt.executeQuery();

            while (resultSet.next()) {
                cachedPlayers.add(Goldilocks.jda.getUserById(resultSet.getString(1)));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return cachedPlayers;
    }

    public static int getLobbyManagerId(Long messageId) {
        int lobbyManagerId = -1;

        String sql = "SELECT lobbyManagerId FROM lobbyManagers WHERE lobbyMessageId = " + messageId;

        //System.out.println(sql);

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            ResultSet resultSet = pstmt.executeQuery();

            lobbyManagerId = resultSet.getInt(1);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lobbyManagerId;
    }

    public static void cacheQueuePlayers(List<User> playerQueue, HashMap<User, Message> userMessageHashMap, Long lobbyManagerMessageId) {
        List<User> currentCachedPlayers = getCurrentPlayerCache("queuePlayers");
        for (User user : currentCachedPlayers) {

            Long playerId = user.getIdLong();
            String sql = "DELETE FROM queuePlayers WHERE playerId = " + playerId + " AND lobbyManagerId = " + getLobbyManagerId(lobbyManagerMessageId);

            //System.out.println(sql);

            if (!playerQueue.contains(currentCachedPlayers)) {
                try (Connection conn = DriverManager.getConnection(dbUrl);
                     PreparedStatement pstmt = conn.prepareStatement(sql);) {

                    pstmt.executeUpdate();

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        for (User user : playerQueue) {
             Long playerId = user.getIdLong();
             int playerIndex = playerQueue.indexOf(user);
             Long messageId = userMessageHashMap.get(user).getIdLong();
             int lobbyManagerId = getLobbyManagerId(lobbyManagerMessageId);

             if (!playerExistsInTable(user, "queuePlayers")) {
                createPlayerQueueCache(lobbyManagerId, playerIndex, playerId, messageId);
             }

            String sql = "UPDATE queuePlayers SET playerQueueIndex = ? WHERE playerId = " + playerId + " AND lobbyManagerId = " + getLobbyManagerId(lobbyManagerMessageId);

            //System.out.println(sql);

            try (Connection conn = DriverManager.getConnection(dbUrl);
                 PreparedStatement pstmt = conn.prepareStatement(sql);) {

                pstmt.setLong(1, playerIndex);
                pstmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }

        }

    }

}
