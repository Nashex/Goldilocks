package lobbies;

import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import utils.Utils;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import static main.Goldilocks.eventWaiter;

public class LobbyManagerHub extends ListenerAdapter {

    public static ScheduledExecutorService TIMER = new ScheduledThreadPoolExecutor(1);
    public static List<LobbyManager> activeLobbyManagers = new CopyOnWriteArrayList<>();
    public static Message controlPanel;

    public static void createLobbyManager(String lobbyName, Emote lobbyEnterEmote, List<Emote> lobbyCreateEmote, TextChannel lobbyChannel, Color color, int lobbyLimit) {
        LobbyManager lobbyManager = new LobbyManager(lobbyName, lobbyEnterEmote, lobbyCreateEmote, lobbyChannel, color, lobbyLimit,false, -1);
        activeLobbyManagers.add(lobbyManager);
        lobbyManager.createLobbyManager();
    }

    public static void deleteLobbyManager(String lobbyManageMessageId, Member executor) {
        LobbyManager lobbyManager = getLobbyManager(lobbyManageMessageId);
        lobbyManager.deleteLobbyManager(executor);
        activeLobbyManagers.remove(lobbyManager);
        LobbyCaching.deleteLobbyManager(Long.parseLong(lobbyManageMessageId));
    }

    public static void retrieveCachedLobbys() {
        int numLobbyManagers = LobbyCaching.getNumLobbyManagers();
        for (int i = 0; i < numLobbyManagers; i++) {
            activeLobbyManagers.add(LobbyCaching.getLobbyManager(i));
            System.out.println("Retrieving Lobby Manager #" + i + " from cache!");
        }
    }

    public static LobbyManager getLobbyManager(String messageId) {
        for (LobbyManager lobbyManager : activeLobbyManagers) {
            if (lobbyManager.getLobbyMessageId().equals(messageId)) {
                return lobbyManager;
            }
        }
        return null;
    }

    public static void createControlPanel(TextChannel textChannel) {
        controlPanel = textChannel.sendMessage(updateControlPanelEmbed().build()).complete();
        TIMER.scheduleWithFixedDelay(() -> {
            updateControlPanel();
        }, 0L, 5L, TimeUnit.SECONDS);
    }

    public static void updateControlPanel() {
        controlPanel.editMessage(updateControlPanelEmbed().build()).queue();
    }

    public static EmbedBuilder updateControlPanelEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Lobby Manager Information");
        embedBuilder.setDescription("Below is a brief description of all of the current lobby managers running under this control panel.");
        embedBuilder.setColor(new Color(158, 119, 0));
        if (!activeLobbyManagers.isEmpty() && activeLobbyManagers.size() < 20) {
            for (LobbyManager lobbyManager : activeLobbyManagers) {
                int totalPlayersInLobby = 0;
                for (Lobby lobby : lobbyManager.getLobbies()) {
                    totalPlayersInLobby += lobby.getLobbyParticipants().size();
                }
                embedBuilder.addField(lobbyManager.getLobbyName(), "Id: `" + lobbyManager.getLobbyMessageId() + "`\n" + "Currently has a total of `" + (lobbyManager.getLobbyPositions().size() + totalPlayersInLobby) + " players` and `"
                        + lobbyManager.getLobbies().size() + " lobbies`\n" + "Player breakdown: `" + lobbyManager.getLobbyPositions().size() + " players` in queue and `"
                        + totalPlayersInLobby + " players` in lobbies", false);
            }
        } else if (activeLobbyManagers.size() > 20) {
            embedBuilder.addField("Active Lobby Managers: ", "You currently have more than 20 active lobby managers", false);
        } else {
            embedBuilder.addField("Active Lobby Managers: ", "You currently do not have any active lobby managers", false);
        }
        embedBuilder.setFooter("Currently handling " + activeLobbyManagers.size() + ((activeLobbyManagers.size() == 1) ? " lobby manager" : " lobby managers"));
        return embedBuilder;
    }

    public void onGuildMessageReactionAdd(@Nonnull GuildMessageReactionAddEvent event) {

        if (getLobbyManager(event.getMessageId()) == null) {
            return;
        }

        if(event.getUser().isBot()) {
            return;
        }

        LobbyManager lobbyManager = getLobbyManager(event.getMessageId());

        List<Lobby> openLobbies = new ArrayList<>();
        for (Lobby lobby : lobbyManager.getLobbies()) {
            if (!lobby.isFull() && !lobby.isStarted()) {
                openLobbies.add(lobby);
            }
        }

        List<User> playerPlaces = lobbyManager.getLobbyPositions();
        HashMap<User,Message> userMessages = lobbyManager.getLobbyUsers();
        Emote lobbyEnter = lobbyManager.getLobbyEnter();
        List<Emote> lobbyCreate = lobbyManager.getLobbyCreate();
        String lobbyName = lobbyManager.getLobbyName();

        Member member = event.getMember();
        User user = event.getUser();

        if (!event.getUser().isBot()) {
            if (event.getReactionEmote().isEmote()) {
                Emote emote = event.getReactionEmote().getEmote();
                if (!lobbyManager.userInLobby(user)) {
                    if (emote.equals(lobbyEnter)) {
                        if (!openLobbies.isEmpty()) {
                            for (Lobby lobby : openLobbies) {
                                if (!lobby.isStarted()) {
                                    lobby.addUser(user);
                                    lobbyManager.removeUserReaction(user, lobbyEnter);
                                    break;
                                }
                            }
                            lobbyManager.removeUserReaction(user, lobbyEnter);
                        } else {
                            lobbyManager.addUser(user);
                        }
                    } else if (lobbyCreate.contains(emote)) {
                        if (!openLobbies.isEmpty()) {
                            for (Lobby lobby : openLobbies) {
                                if (!lobby.isStarted()) {
                                    lobby.addUser(user);
                                    lobbyManager.removeUserReaction(user, event.getReactionEmote().getEmote());
                                    break;
                                }
                            }
                        } else {
                            if (!lobbyName.equals("Among Us")) {
                                Message keyPromptMessage = user.openPrivateChannel().complete().sendMessage(promptForKey(lobbyName).build()).complete();
                                keyPromptMessage.addReaction("✅").queue();
                                keyPromptMessage.addReaction("❌").queue();

                                TIMER.schedule(() -> {
                                    eventWaiter.waitForEvent(PrivateMessageReactionAddEvent.class, e -> {
                                        return e.getUser().equals(user) && (e.getReactionEmote().getEmoji().equals("❌") || e.getReactionEmote().getEmoji().equals("✅"));
                                    }, e -> {

                                        if (e.getReactionEmote().getEmoji().equals("✅")) {
                                            lobbyManager.createLobby(event.getMember());
                                            keyPromptMessage.delete().queue();
                                        }
                                        if (e.getReactionEmote().getEmoji().equals("❌")) {
                                            lobbyManager.removeUserReaction(user, event.getReactionEmote().getEmote());
                                            keyPromptMessage.delete().queue();
                                        }
                                    }, 1L, TimeUnit.MINUTES, () -> {
                                        lobbyManager.removeUserReaction(user, event.getReactionEmote().getEmote());
                                        keyPromptMessage.clearReactions().queue();
                                        Utils.errorMessage("Lobby Creation", "User did not react in time", keyPromptMessage, 10L);
                                    });
                                }, 0L, TimeUnit.SECONDS);
                            } else {
                                lobbyManager.createLobby(event.getMember());
                            }
                        }
                        //lobbyManager.removeUser(event.getUser());
                    }
                } else {
                    lobbyManager.removeUserReaction(user, event.getReactionEmote().getEmote());
                }

            }
        }

    }

    public void onGuildMessageReactionRemove(@Nonnull GuildMessageReactionRemoveEvent event) {
        if (getLobbyManager(event.getMessageId()) == null) {
            return;
        }

        LobbyManager lobbyManager = getLobbyManager(event.getMessageId());
        User user = event.getUser();
        lobbyManager.removeUser(user);
    }

    @Override
    public void onPrivateMessageReactionAdd(@Nonnull PrivateMessageReactionAddEvent event) {

        if (event.getUser().isBot()) {
            return;
        }

        LobbyManager lobbyManager = null;
        for (LobbyManager lManager : activeLobbyManagers) {
            if (lManager.getLobbyMasters().containsKey(event.getUser())) {
                lobbyManager = lManager;
                break;
            }
            if (lManager.getLobbyPositions().contains(event.getUser())) {
                lobbyManager = lManager;
                break;
            }
        }

        if (lobbyManager == null) {
            return;
        }

        User user = event.getUser();

        if (event.getReactionEmote().isEmoji()) {
            String emote = event.getReactionEmote().getEmoji();
            if (emote.equals("❌")) {
                if (lobbyManager.getLobby(user) == null) {
                    lobbyManager.removeUser(user);
                } else if (lobbyManager.getLobby(user) != null) {
                    lobbyManager.removeLobby(lobbyManager.getLobby(user));
                }
            }
            if (emote.equals("🗺")) {
                Lobby lobby = lobbyManager.getLobby(user);
                Message locationChangeMessage = user.openPrivateChannel().complete().sendMessage(changeLobbyLocation().build()).complete();
                TIMER.schedule(() -> {
                    eventWaiter.waitForEvent(PrivateMessageReceivedEvent.class, e -> {
                        return e.getAuthor().equals(user) && !e.getMessage().getContentRaw().isEmpty();
                    }, e -> {
                        String newLocation = e.getMessage().getContentRaw();
                        lobby.setLocation(newLocation);
                        locationChangeMessage.editMessage(changeLobbyLocation().setDescription("Location successfully changed to `" + newLocation + "`").build()).complete()
                        .delete().submitAfter(5L, TimeUnit.SECONDS);
                        lobby.updateLobbyInfo();
                    }, 1L, TimeUnit.MINUTES, () -> {
                        Utils.errorMessage("Lobby Location Change", " No location was given", locationChangeMessage, 10L);
                    });
                }, 0L, TimeUnit.SECONDS);
            }

        }

    }

    @Override
    public void onGuildVoiceLeave(@Nonnull GuildVoiceLeaveEvent event) {
        User user = event.getMember().getUser();

        for (LobbyManager lobbyManager : activeLobbyManagers) {
            for (Lobby lobby : lobbyManager.getLobbies()) {
                if (lobby.getLobbyParticipants().contains(user)) {
                    lobby.removeUser(user);
                    return;
                }
            }
        }

    }

    public static EmbedBuilder changeLobbyLocation() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Lobby Location Change");
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setThumbnail(Goldilocks.jda.getSelfUser().getAvatarUrl());
        embedBuilder.setDescription("```\nPlease enter a new location for your lobby below, you have one minute to do so.\n```");
        embedBuilder.setTimestamp(new Date().toInstant());

        return embedBuilder;
    }
    public EmbedBuilder promptForKey(String lobbyName) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Lobby Creation for " + lobbyName);
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setThumbnail(Goldilocks.jda.getSelfUser().getAvatarUrl());
        embedBuilder.setDescription("```\nPlease react with ✅ if you have a key and would like to create a lobby." +
                " If you do not have a key, react with ❌.\n" +
                "Fake reactions are HIGHLY discouraged as they will result in a lengthy suspension.\n```");
        embedBuilder.setTimestamp(new Date().toInstant());

        return embedBuilder;
    }

}
