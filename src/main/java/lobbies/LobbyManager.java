package lobbies;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import utils.Utils;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LobbyManager {

    private ScheduledExecutorService TIMER = new ScheduledThreadPoolExecutor(1);

    private String lobbyName = "";
    private Message lobbyMsg;
    private Emote lobbyEnter;
    private List<Emote> lobbyCreate;
    private TextChannel textChannel;
    private Color color;
    private int lobbyLimit;

    private List<Lobby> lobbies = new CopyOnWriteArrayList<>();
    public HashMap<User,Message> lobbyMasters = new HashMap<>();
    public HashMap<User,Message> userMessages = new HashMap<>();
    public List<User> playerPlaces = new ArrayList<>();

    public LobbyManager(String lobbyName, Emote lobbyEnterEmote, List<Emote> lobbyCreateEmotes, TextChannel textChannel, Color color, int lobbyLimit, boolean fromCache, int lobbyManagerId) {

        this.color = color;
        this.lobbyName = lobbyName;
        this.lobbyEnter = lobbyEnterEmote;
        this.lobbyCreate = lobbyCreateEmotes;
        this.textChannel = textChannel;
        this.lobbyLimit = lobbyLimit;

        if (fromCache) {
            retrieveLobbyManager(lobbyManagerId);
            startUserMessageManager();
        }
    }

    public void retrieveLobbyManager(int lobbyManagerId) {
        if (LobbyCaching.getQueuePlayers(lobbyManagerId) != null) {
            userMessages = LobbyCaching.getQueuePlayers(lobbyManagerId);
            playerPlaces = new ArrayList<>();
            playerPlaces.addAll(userMessages.keySet());
        }
        lobbyMsg = LobbyCaching.getLobbyManagerMessage(lobbyManagerId);
        lobbies.addAll(LobbyCaching.getLobbies(lobbyManagerId));

        for (Lobby lobby : lobbies) {
            lobbyMasters.put(lobby.getLobbyMaster().getUser(), lobby.getControlPanel());
            System.out.println(lobby.getLobbyMaster().getEffectiveName() + " " + lobby.getControlPanel().getId());
        }

    }

    public void startUserMessageManager() {
        TIMER.scheduleWithFixedDelay(() -> {
            if (!userMessages.isEmpty()) {
                userMessages.forEach(((member, message) -> {
                    int userPlace = playerPlaces.indexOf(member);
                    updateUserMessage(userPlace, message);
                }));
            }

            if (!lobbies.isEmpty()) {
                for (Lobby lobby : lobbies) {
                    if (!lobby.isStarted()) {
                        lobby.updateLimboUsers();
                    } else {
                        if (lobby.getVoiceChannel().getMembers().isEmpty()) {
                            removeLobby(lobby);
                        }
                    }
                }
            }
        }, 0L, 5L, TimeUnit.SECONDS);
    }

    public void createLobbyManager() {
        EmbedBuilder lobbyMessage = new EmbedBuilder();
        lobbyMessage.setTitle(lobbyName);
        lobbyMessage.setColor(color);
        lobbyMessage.setThumbnail(lobbyEnter.getImageUrl());
        lobbyMessage.setDescription("In order to join the queue please react with " + lobbyEnter.getAsMention() + " and the bot will message you when a lobby is open!"
                + " If you are in the Queue voice channel when a spot opens, you will be automatically moved into the open lobby!");
        lobbyMessage.addField("Players in Queue", "There are currently `" + playerPlaces.size() + " players` looking an open lobby", true);
        lobbyMessage.addField("Lobbies in Progress", "There are currently `" + lobbies.size() + " lobbies` raiding " + lobbyName + "!", true);
        lobbyMessage.setFooter("If you have a key please react with the key emote to open a new lobby!");
        lobbyMsg = textChannel.sendMessage(lobbyMessage.build()).complete();
        lobbyMsg.addReaction(lobbyEnter).queue();
        String createEmotes = "";
        for (Emote emote : lobbyCreate) {
            lobbyMsg.addReaction(emote).queue();
            createEmotes += emote.getId() + (lobbyCreate.indexOf(emote) != (lobbyCreate.size() - 1) ? " " : "");
        }

        LobbyCaching.createLobbyManager(lobbyMsg.getIdLong(), lobbyName, textChannel.getIdLong(), lobbyLimit, color, lobbyEnter.getIdLong(), createEmotes);

        startUserMessageManager();
    }

    public void deleteLobbyManager(Member executor) {
        for (Lobby lobby : lobbies) {
            removeLobby(lobby);
        }

        userMessages.forEach((user,message) -> Utils.errorMessage("You have been removed from the queue", "Lobby mangager no longer exists", message, 10L));

        playerPlaces.clear();
        userMessages.clear();
        lobbyMasters.clear();

        EmbedBuilder endedLobbyMessage = new EmbedBuilder();
        endedLobbyMessage.setTitle(lobbyName);
        endedLobbyMessage.setThumbnail(lobbyEnter.getImageUrl());
        endedLobbyMessage.setColor(color);
        endedLobbyMessage.setDescription("```\nThis lobby manager and all of its corresponding lobbies have been deleted by: " + executor.getEffectiveName() + "\n```");
        endedLobbyMessage.setFooter("This message will be deleted in 10 seconds.");

        lobbyMsg.clearReactions().queue();
        lobbyMsg.editMessage(endedLobbyMessage.build()).queue();
        lobbyMsg.delete().submitAfter(10L, TimeUnit.SECONDS);
    }

    public void updateLobbyManager() {
        EmbedBuilder lobbyMessage = new EmbedBuilder();
        lobbyMessage.setTitle(lobbyName);
        lobbyMessage.setThumbnail(lobbyEnter.getImageUrl());
        lobbyMessage.setColor(color);
        lobbyMessage.setDescription("In order to join the queue please react with " + lobbyEnter.getAsMention() + " and the bot will message you when a lobby is open!"
                + " If you are in the Queue voice channel when a spot opens, you will be automatically moved into the open lobby!");
        lobbyMessage.addField("Players in Queue", "There are currently `" + playerPlaces.size() + " players` looking an open lobby", true);
        lobbyMessage.addField("Lobbies in Progress", "There are currently `" + lobbies.size() + " lobbies` raiding " + lobbyName + "!", true);
        lobbyMessage.setFooter("If you have a key please react with the key emote to open a new lobby!");

        lobbyMsg.editMessage(lobbyMessage.build()).queue();
    }

    public EmbedBuilder lobbyControlPanel(Member lobbyMaster, List<User> lobbyParticipants, int numInVc) {

        Guild guild = lobbyMaster.getGuild();
        String lobbyNames = "";
        for (User user : lobbyParticipants) {
            lobbyNames += guild.getMember(user).getEffectiveName() + "\n";
        }

        EmbedBuilder controlPanel = new EmbedBuilder();
        controlPanel.setTitle(lobbyMaster.getEffectiveName() + "'s " + lobbyName + " Control Panel");
        controlPanel.setDescription("Below are the controls for the lobby that you have created! Thank you for being a member of " + lobbyMaster.getGuild().getName() + "!");
        controlPanel.setColor(new Color(158, 119, 0));
        controlPanel.addField("Current Location: ", "`None set`", false);
        controlPanel.addField("Players Raiding with you: ", "```fix\n" + (lobbyNames.isEmpty() ? "You're all alone" : lobbyNames) + "\n```", false);
        controlPanel.addField("Set Location 🗺", "Please react with 🗺 to change the location for your lobby.", true);
        controlPanel.addField("Close Lobby ❌", "Please react with ❌ if you would like to close this vc.", true);
        controlPanel.setFooter("Current members in voice channel: " + numInVc + "/" + lobbyLimit);
        return controlPanel;
    }

    public EmbedBuilder endedLobbyMessage() {
        EmbedBuilder endedMessage = new EmbedBuilder();
        endedMessage.setTitle(lobbyName + " Lobby Closed Successfully!");
        endedMessage.setColor(new Color(158, 119, 0));
        endedMessage.setDescription("Thank you for raiding with Goldilocks!");
        return endedMessage;
    }

    public Lobby getLobby(User lobbyMaster) {
        for (Lobby lobby : lobbies) {
            if (lobby.getLobbyMaster().getUser().equals(lobbyMaster)) {
                return lobby;
            }
        }
        return null;
    }

    public boolean userInLobby(User user) {
        for (Lobby lobby : lobbies) {
            if (lobby.getLobbyParticipants().contains(user)) {
                return true;
            }
        }
        return false;
    }

    public void createLobby(Member lobbyMaster) {

        Guild guild = lobbyMaster.getGuild();

        List<User> lobbyParticipants = new CopyOnWriteArrayList<>();
        while (lobbyParticipants.size() <= lobbyLimit && !playerPlaces.isEmpty()) {
            if (!lobbyParticipants.contains(playerPlaces.get(0))) {
                lobbyParticipants.add(playerPlaces.get(0));
                removeUserReaction(playerPlaces.get(0), lobbyEnter);
                playerPlaces.remove(0);
            }
        }

        VoiceChannel lobbyChannel = guild.createVoiceChannel(lobbyName + " | " + (lobbies.size() + 1)).setParent(textChannel.getParent()).setUserlimit(lobbyLimit).setPosition(99)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL)).complete();

        TextChannel lobbyTextChannel = guild.createTextChannel(lobbyName + "-" + (lobbies.size() + 1)).setParent(textChannel.getParent()).setPosition(99)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT)).complete();

        Message controlPanel = lobbyMaster.getUser().openPrivateChannel().complete().sendMessage(lobbyControlPanel(lobbyMaster, lobbyParticipants, 0).build()).complete();
        controlPanel.addReaction("🗺").queue();
        controlPanel.addReaction("❌").queue();

        lobbyMasters.put(lobbyMaster.getUser(), controlPanel);
        Lobby lobby = new Lobby(lobbyMaster, lobbyParticipants, "None Set",lobbyChannel, lobbyTextChannel, lobbyName, lobbyLimit, lobbyMsg, controlPanel, false);
        lobbies.add(lobby);
        for (Emote emote : lobbyCreate) {
            try {
                lobbyMsg.removeReaction(emote, lobbyMaster.getUser()).queue();
            } catch (Exception e) {

            }
        }
        updateLobbyManager();
    }

    public void removeLobby(Lobby lobby) throws ErrorResponseException {

        lobby.shutDownTimer();

        LobbyCaching.deleteLobbyPlayers(lobby.getVoiceChannel().getIdLong());
        LobbyCaching.deleteLobby(lobby.getVoiceChannel().getIdLong());

        try {
            TextChannel lobbyLogs = lobby.getLobbyMaster().getGuild().getTextChannelsByName("lobby-logs",true).get(0);
            String players = "";
            Guild guild = lobby.getLobbyMaster().getGuild();
            if (!lobby.getLobbyParticipants().contains(lobby.getLobbyMaster().getUser())) {
                players += lobby.getLobbyMaster().getEffectiveName() + (lobby.getLobbyParticipants().size() == 1 ? "" : ", ");
            }
            for (User user : lobby.getLobbyParticipants()) {
                if (lobby.getLobbyParticipants().indexOf(user) == lobby.getLobbyParticipants().size() - 1) {
                    players += guild.getMember(user).getEffectiveName();
                    break;
                }
                players += guild.getMember(user).getEffectiveName() + ", ";
            }
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Lobby for " + lobbyName + " Successfully Closed!");
            embedBuilder.setDescription("Players(" + lobby.getLobbyParticipants().size() + "): " + players);
            embedBuilder.addField("Lobby Master:", lobby.getLobbyMaster().getAsMention(), false);
            embedBuilder.setThumbnail(lobbyEnter.getImageUrl());
            embedBuilder.setColor(color);
            embedBuilder.setTimestamp(new Date().toInstant());
            lobbyLogs.sendMessage(embedBuilder.build()).queue();
        } catch (Exception e) {
            //Guild does not have a lobby-logs channel
        }

        lobby.getLimboUsers().forEach((user, message) -> Utils.errorMessage("You have been removed from the lobby", "The lobby you were in was deleted", message, 10L));
        lobby.getVoiceChannel().delete().queue();
        lobby.getTextChannel().delete().queue();
        lobbyMasters.get(lobby.getLobbyMaster().getUser()).delete().queue();
        lobby.getLobbyMaster().getUser().openPrivateChannel().complete().sendMessage(endedLobbyMessage().build()).complete().delete().submitAfter(5L, TimeUnit.SECONDS);
        lobbyMasters.remove(lobby.getLobbyMaster());
        lobbies.remove(lobby);
        updateLobbyManager();

    }

    public EmbedBuilder userEmbed(int userPlace) {
        EmbedBuilder userEmbed = new EmbedBuilder();
        userEmbed.setTitle("You are now in queue for " + lobbyName + "!");
        userEmbed.setDescription("You are currently `#" + userPlace + "` in queue. React with ❌ to leave the queue.");
        userEmbed.setFooter("Nitro boost this server for priority queuing!", "https://pbs.twimg.com/media/EWdeUeHXkAQgJh7.png");
        return userEmbed;
    }

    public void updateUserMessage(int userPlace, Message message) {
        message.editMessage(userEmbed(userPlace).build()).queue();
    }

    public void addUser(User user) {
        Message userMessage;
        userMessage = user.openPrivateChannel().complete().sendMessage(userEmbed(playerPlaces.size() + 1).build()).complete();
        userMessage.addReaction("❌").queue();
        playerPlaces.add(user);
        userMessages.put(user, userMessage);
        updateLobbyManager();

        LobbyCaching.cacheQueuePlayers(playerPlaces, userMessages, lobbyMsg.getIdLong());
    }

    public void removeUser(User user) {
        try {
            playerPlaces.remove(user);
            userMessages.get(user).editMessage(userEmbed(0)
                    .setDescription("You have been removed from the queue.").setFooter("Thank you for raiding with Goldilocks!").build()).queue();
            userMessages.get(user).delete().submitAfter(5L, TimeUnit.SECONDS);
            userMessages.remove(user);
            lobbyMsg.removeReaction(lobbyEnter, user).queue();
            updateLobbyManager();

            LobbyCaching.cacheQueuePlayers(playerPlaces, userMessages, lobbyMsg.getIdLong());

        } catch (NullPointerException e) {

        }
    }

    public void removeUserReaction(User user, Emote emote) {
        try {
            lobbyMsg.removeReaction(emote, user).queue();
        } catch (Exception e) {

        }
    }

    public String getLobbyMessageId() {
        return lobbyMsg.getId();
    }
    public HashMap getLobbyUsers() {
        return userMessages;
    }
    public List getLobbyPositions() {
        return playerPlaces;
    }
    public Emote getLobbyEnter() {
        return lobbyEnter;
    }
    public List<Emote> getLobbyCreate() {
        return lobbyCreate;
    }
    public String getLobbyName() {
        return lobbyName;
    }
    public HashMap getLobbyMasters(){
        return lobbyMasters;
    }
    public List<Lobby> getLobbies() {
        return lobbies;
    }
}
