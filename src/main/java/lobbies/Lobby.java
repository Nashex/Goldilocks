package lobbies;

import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import utils.Utils;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Lobby {

    private String lobbyName;
    private int lobbyLimit;
    private Member lobbyMaster;
    private Message lobbyManagerMessage;
    private List<User> lobbyParticipants;
    private String location;
    private VoiceChannel voiceChannel;
    private TextChannel textChannel;
    private boolean full = false;
    private Message infoMessage;
    private HashMap<User, Message> limboUsers = new HashMap<>();
    private long refreshTime;
    private Message controlPanel;
    private ScheduledExecutorService lobbyTimer = new ScheduledThreadPoolExecutor(1);

    private Guild guild;

    public Lobby(Member lobbyMaster, List<User> lobbyParticipants, String location, VoiceChannel voiceChannel, TextChannel textChannel, String lobbyName, int lobbyLimit, Message lobbyManagerMessage, Message controlPanel, boolean cacheRevival) {

        this.refreshTime = System.currentTimeMillis() + 600000;


        if (location.equals("None Set") && !lobbyName.equals("Among Us")) {
            Random random = new Random();
            String[] rotmgLocations = {"USWest4","USWest2","USWest","USSouthWest","USSouth3","USSouth2","USSouth","USNorthWest","USMidWest2",
                    "USMidWest","USEast4","USEast3","USEast2","USEast","EUWest2","EUWest","EUSouthWest","EUSouth","EUNorth2","EUNorth","EUEast2",
                    "EUEast","Australia","AsiaSouthEast","AsiaEast"};
            String[] leftOrRight = {" Left", " Right"};
            this.location = rotmgLocations[random.nextInt(24)] + leftOrRight[random.nextInt(2)];
        } else {
            this.location = location;
        }

        this.guild = voiceChannel.getGuild();
        this.textChannel = textChannel;
        this.voiceChannel = voiceChannel;
        this.lobbyMaster = lobbyMaster;
        this.lobbyParticipants = lobbyParticipants;
        for (Member member : voiceChannel.getMembers()) {
            if (!lobbyParticipants.contains(member.getUser())) {
                lobbyParticipants.add(member.getUser());
            }
        }
        this.lobbyName = lobbyName;
        this.lobbyLimit = lobbyLimit;
        this.controlPanel = controlPanel;
        this.lobbyManagerMessage = lobbyManagerMessage;
        infoMessage = textChannel.sendMessage(joinEmbed().build()).complete();

        if (cacheRevival) {
            retrieveFromCache();
            createLobbyChannels(true);
        } else {
            createLobbyChannels(false);
        }

        if (!cacheRevival) {

            LobbyCaching.createLobby(lobbyManagerMessage.getIdLong(), lobbyMaster.getIdLong(), voiceChannel.getIdLong(), textChannel.getIdLong(), this.location, infoMessage.getIdLong(), controlPanel.getIdLong());
            LobbyCaching.cacheLobbyPlayers(lobbyParticipants, voiceChannel.getIdLong());
        }

        updateLobbyControlPanel();
        startMessageTimer();

    }

    public void startMessageTimer() {
        if (!isStarted()) {
            lobbyTimer.scheduleWithFixedDelay(() -> {
                if (((((refreshTime - System.currentTimeMillis()) / 1000) / 5) * 5) == 0) {
                    infoMessage.editMessage(joinEmbed().setFooter("The lobby is now closed!").build()).queue();
                    lobbyTimer.shutdown();
                    return;
                }
                infoMessage.editMessage(joinEmbed().setFooter("⌛ Time Left till new players can't join: " + Utils.formatTimeFromNow(refreshTime / 5 * 5 + 1000) + "").build()).queue();
            }, 0L, 5L, TimeUnit.SECONDS);
        }
    }

    public void retrieveFromCache() {
        //Will add more informative features for retrieved raids.
        this.refreshTime = System.currentTimeMillis();
        //Make sure player perms are kept
        //updateLobbyControlPanel();
    }

    public void createLobbyChannels(boolean fromCache) {

        voiceChannel.getManager().sync().queue();
        textChannel.getManager().sync().queue();

        for (User user : lobbyParticipants) {
            if (!user.equals(lobbyMaster.getUser())) {
                Member member = guild.getMember(user);
                voiceChannel.putPermissionOverride(member).setAllow(EnumSet.of(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VIEW_CHANNEL)).queue();
                textChannel.putPermissionOverride(member).setAllow(EnumSet.of(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.VIEW_CHANNEL, Permission.MESSAGE_ADD_REACTION)).queue();
                if (!fromCache) {
                    Message userMessage = user.openPrivateChannel().complete().sendMessage(limboUserEmbed((((refreshTime - System.currentTimeMillis()) / 1000) / 5) * 5).build()).complete();
                    limboUsers.put(user, userMessage);
                }
            }
        }
        //Add perms to lobby master
        if (!lobbyParticipants.contains(lobbyMaster.getUser())) {
            lobbyParticipants.add(lobbyMaster.getUser());
        }
        voiceChannel.putPermissionOverride(lobbyMaster).setAllow(EnumSet.of(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VIEW_CHANNEL)).queue();
        textChannel.putPermissionOverride(lobbyMaster).setAllow(EnumSet.of(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.VIEW_CHANNEL, Permission.MESSAGE_ADD_REACTION)).queue();
    }

    public void removeUser(User user) {
        if (limboUsers.containsKey(user)) {
            limboUsers.get(user).delete().queue();
            limboUsers.remove(user);
        }
        lobbyParticipants.remove(user);
        voiceChannel.upsertPermissionOverride(guild.getMember(user)).setDeny(Permission.ALL_PERMISSIONS).queue();
        textChannel.upsertPermissionOverride(guild.getMember(user)).setDeny(Permission.ALL_PERMISSIONS).queue();
        LobbyCaching.cacheLobbyPlayers(lobbyParticipants, voiceChannel.getIdLong());
        updateLobbyInfo();
        updateLobbyControlPanel();
    }

    public void addUser(User user) {
        this.lobbyParticipants.add(user);
        //addChannelPerms();
        Member member = guild.getMember(user);
        voiceChannel.putPermissionOverride(member).setAllow(EnumSet.of(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VIEW_CHANNEL)).queue();
        textChannel.putPermissionOverride(member).setAllow(EnumSet.of(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.VIEW_CHANNEL, Permission.MESSAGE_ADD_REACTION)).queue();
        Message userMessage = user.openPrivateChannel().complete().sendMessage(limboUserEmbed((((refreshTime - System.currentTimeMillis()) / 1000) / 5) * 5).build()).complete();
        if (!user.equals(lobbyMaster.getUser())) {
            limboUsers.put(user, userMessage);
        }
        updateLobbyInfo();
        updateLobbyControlPanel();
        LobbyCaching.cacheLobbyPlayers(lobbyParticipants, voiceChannel.getIdLong());
    }

    public void updateLobbyInfo() {
        infoMessage.editMessage(joinEmbed().build()).queue();
    }

    public EmbedBuilder limboUserEmbed(long timeleft) {
        EmbedBuilder joinEmbed = new EmbedBuilder();
        joinEmbed.setTitle("You have been added to a lobby!");
        joinEmbed.setThumbnail(guild.getIconUrl());
        joinEmbed.setColor(Goldilocks.BLUE);
        //joinEmbed.setColor(new Color(160 - ((int) timeleft * 2), 100 + ((int) timeleft * 2), 0));
        joinEmbed.setDescription("Please join the voice channel `" + voiceChannel.getName() + "` within the next minute to preserve your spot in the lobby!");
        joinEmbed.setFooter("Time left to join voice channel: " + timeleft + " seconds");
        return joinEmbed;
    }

    public void updateLimboUsers() {
        if (refreshTime < System.currentTimeMillis()) {
            limboUsers.forEach((key, msg) -> {
                System.out.println("here");
                removeUser(key);
                msg.delete().queue();
                limboUsers.remove(key);
            });
            limboUsers.clear();
            //Caching.cacheLobbyPlayers(lobbyParticipants, voiceChannel.getIdLong());
        }
        if (!limboUsers.isEmpty()) {
            limboUsers.forEach((key, msg) -> {
                if (!this.voiceChannel.getMembers().contains(guild.getMember(msg.getPrivateChannel().getUser()))) {
                    long niceTime = (((refreshTime - System.currentTimeMillis()) / 1000) / 5) * 5;
                    msg.editMessage(limboUserEmbed(niceTime).build()).queue();
                } else {
                    limboUsers.get(key).editMessage(limboUserEmbed(0).setFooter("This message will delete in 5 seconds")
                            .setDescription("Thank you for joining the voice channel!").setTimestamp(new Date().toInstant()).build())
                            .complete().delete().submitAfter(5L, TimeUnit.SECONDS);
                    limboUsers.remove(key);
                }
            });
        }
    }

    public EmbedBuilder joinEmbed() {

        String lobbyNames = "";
        for (User user : lobbyParticipants) {
            lobbyNames += guild.getMember(user).getEffectiveName() + "\n";
        }

        EmbedBuilder joinEmbed = new EmbedBuilder();
        joinEmbed.setTitle("Lobby info for `" + voiceChannel.getName() + "`");
        joinEmbed.setThumbnail(guild.getIconUrl());
        joinEmbed.setDescription("Below is a brief description of your current lobby. Thank you for being a member of " + lobbyMaster.getGuild().getName() + "!");
        joinEmbed.setColor(new Color(158, 119, 0));
        joinEmbed.addField("Lobby Key", "`" + lobbyMaster.getEffectiveName() + "`", false);
        joinEmbed.addField("Current Location: ", "`" + location + "`", false);
        joinEmbed.addField("Players Raiding with you: ", "```fix\n" + (lobbyNames.isEmpty() ? "You're all alone" : lobbyNames) + "\n```", false);
        joinEmbed.setFooter("Thank you for raiding with " + guild.getName() + "!");
        return joinEmbed;
    }

    public void updateLobbyControlPanel() {

        int numInVc = voiceChannel.getMembers().size();

        Guild guild = lobbyMaster.getGuild();
        String lobbyNames = "";
        for (User user : lobbyParticipants) {
            lobbyNames += guild.getMember(user).getEffectiveName() + "\n";
        }

        EmbedBuilder controlPanelEmbed = new EmbedBuilder();
        controlPanelEmbed.setTitle(lobbyMaster.getEffectiveName() + "'s " + lobbyName + " Control Panel");
        controlPanelEmbed.setDescription("Below are the controls for the lobby that you have created! Thank you for being a member of " + lobbyMaster.getGuild().getName() + "!");
        controlPanelEmbed.setColor(new Color(158, 119, 0));
        controlPanelEmbed.addField("Current Location: ", "`" + location + "`", false);
        controlPanelEmbed.addField("Players Raiding with you: ", "```fix\n" + (lobbyNames.isEmpty() ? "You're all alone" : lobbyNames) + "\n```", false);
        controlPanelEmbed.addField("Set Location 🗺", "Please react with 🗺 to change the location for your lobby.", true);
        controlPanelEmbed.addField("Close Lobby ❌", "Please react with ❌ if you would like to close this vc.", true);
        controlPanelEmbed.setFooter("Current members in voice channel: " + numInVc + "/" + lobbyLimit);

        controlPanel.editMessage(controlPanelEmbed.build()).queue();

    }

    public void setLocation(String location) {
        this.location = location;
        updateLobbyInfo();
        updateLobbyControlPanel();
    }

    public boolean isStarted() {
        if ((refreshTime) < System.currentTimeMillis()) {
            return true;
        }
        return false;
    }

    public HashMap<User, Message> getLimboUsers () {
        return limboUsers;
    }

    public void setFull() {
        full = true;
    }

    public boolean isFull() {
        return full;
    }

    public VoiceChannel getVoiceChannel() {
        return voiceChannel;
    }

    public TextChannel getTextChannel() {
        return textChannel;
    }

    public Member getLobbyMaster() {
        return lobbyMaster;
    }

    public List<User> getLobbyParticipants() {
        return lobbyParticipants;
    }

    public String getLocation() {
        return location;
    }


    public Message getControlPanel() {
        return controlPanel;
    }

    public void shutDownTimer() {
        lobbyTimer.shutdown();
    }

}
