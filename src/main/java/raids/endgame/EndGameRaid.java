package raids.endgame;

import com.google.gson.annotations.Expose;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.requests.ErrorResponse;
import utils.Utils;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static main.Goldilocks.eventWaiter;

public class EndGameRaid {

    @Expose private Member raidLeader;
    @Expose public Guild raidGuild;
    @Expose private TextChannel raidStatusChannel;
    @Expose private TextChannel raidCommandsChannel;

    @Expose private Message controlPanel;
    @Expose public Message raidMessage;
    @Expose private Message locationMessage;

    public TextChannel locationChannel;

    public Emote dungeonEmote;
    public List<Emote> emotes = new ArrayList<>();
    private List<Emote> additionalEmotes = new ArrayList<>();

    private String[] dungeonInfo;
    @Expose private String dungeonName;
    private Color raidColor;
    @Expose private int raidType;
    @Expose private String dungeonImage = null;
    @Expose private String dungeonStartImage = null;

    @Expose private boolean isStarted = false;
    @Expose private Long timeStarted;
    private Long startingTime;

    @Expose private String location;
    public HashMap<Member, InteractionHook> interactionHooks = new HashMap<>();
    public HashMap<Emoji, List<Member>> emoteReacts = new HashMap<>();

    private Timer timer;

    boolean setRealmLocation = false;
    boolean ended = false;

    private final String[] meetupLocation = new String[]{"US West 4", "US West 2", "US West", "US East 4", "US East 3", "US East 2", "US East", "US South", "US South 2", "US South 3", "US Northwest", "Austrailia", "AsiaEast", "EU East", "EU East", "EU East 2", "EU East 3"};

    public EndGameRaid(Member raidLeader, int raidType, TextChannel raidStatusChannel, TextChannel raidCommands, String location) {
        this.raidLeader = raidLeader;
        this.raidGuild = raidLeader.getGuild();
        this.raidStatusChannel = raidStatusChannel;
        this.raidCommandsChannel = raidCommands;
        Random random = new Random();
        this.location = location.isEmpty() ? meetupLocation[random.nextInt(meetupLocation.length - 1)] + (random.nextBoolean() ? " Left" : " Right") : location;
        this.timeStarted = System.currentTimeMillis();
        this.raidType = raidType;
        this.startingTime = System.currentTimeMillis();

        this.dungeonInfo = new String[]{"768186960622649375", "768186911650611300", "768186834127290408 768186895930359888 768186846059954227 749176008082456646", "Event Dungeon", "", String.valueOf((new Color(27, 0, 92)).getRGB()), "50", "",""};
        getDungeonInfo();

        // Create the raid message

        List<Emote> allEmotes = new ArrayList<>();
        raidMessage = raidStatusChannel.sendMessage(raidStatusEmbed().build()).content("@here " + raidLeader.getEffectiveName() + " has started a new `" + dungeonName + "` raid! React if you have any Runes or Decas. The AFK will open soon.").setActionRows(getActionRows()).complete();
        addRaidMessageEmotes();

        // Create the control panel
        controlPanel = raidCommandsChannel.sendMessage(controlPanelEmbed().build()).setActionRows(getControlPanelActions()).complete();

        timer = new Timer();
        timer.schedule(new updateTask(), 0L, 5000L);

        controlButtonHandler();
        generalButtonHandler();
    }

    public void controlButtonHandler() {
        List<String> controls = Arrays.asList("location", "startafk", "endafk");

        // End the AFK check
        eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return !e.getUser().isBot() && e.getMessageId().equals(controlPanel.getId()) && controls.contains(e.getComponentId());
        }, e -> {
            String control = e.getComponentId();

            e.deferEdit().queue();
            if (control.equals("endafk")) {
                raidMessage.editMessage(raidEndEmbed().build()).setActionRows().queue();
                raidMessage.clearReactions().queue();
                controlPanel.editMessage(controlPanelEmbed().build()).setActionRows().queue();
                ended = true;
                return;
            }

            if (control.equals("startafk")) {
                startRaid();
            }
            if (control.equals("location")) {
                promptForLocation(e.getMember());
            }

            controlButtonHandler();

        }, 1L, TimeUnit.HOURS, () -> ended = true);
    }

    public void generalButtonHandler() {
        List<String> controls = Arrays.asList("early", "join");

        eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return !e.getUser().isBot() && e.getMessageId().equals(raidMessage.getId()) && controls.contains(e.getComponentId().replaceAll("[^A-Za-z]", ""));
        }, e -> {

            String control = e.getComponentId().replaceAll("[^A-Za-z]", "");

            if (control.equals("early")) {
                e.reply("Are you sure you have a " + Objects.requireNonNull(e.getButton()).getEmoji().getAsMention() + "?")
                        .setEphemeral(true).addActionRows(ActionRow.of(Button.success("yes", "Yes"), Button.danger("no", "No"))).queue(i -> confirmationHandler(i, e.getButton().getEmoji()));
            }
            if (control.equals("join")) {
                if (!interactionHooks.containsKey(e.getMember())) {
                    e.reply("The location for this raid is `" + location + "`").setEphemeral(true).queue();
                    interactionHooks.put(e.getMember(), e.getHook());
                } else {
                    e.deferEdit().queue();
                }
            }

            generalButtonHandler();

        }, 30L, TimeUnit.MINUTES, () -> {ended = true;});
    }

    public void confirmationHandler(InteractionHook interactionHook, Emoji emoji) {
        List<String> controls = Arrays.asList("yes", "no");

        eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return !e.getUser().isBot() && e.getUser().equals(interactionHook.getInteraction().getUser()) && controls.contains(e.getComponentId());
        }, e -> {
            String control = e.getComponentId();
            if (control.equals("yes")) {
                interactionHook.editOriginal("The " + (isStarted ? "" : "meetup ") + "location for this raid is `" + (location.isEmpty() ? "Not set, pay attention to the afk for a new location." : location) + "`")
                        .setActionRows().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
                if (!emoteReacts.get(emoji).contains(e.getMember())) emoteReacts.get(emoji).add(e.getMember());
            } else {
                interactionHook.deleteOriginal().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            }

        }, 2L, TimeUnit.MINUTES, () -> {});
    }

    public void getDungeonInfo() {
        this.dungeonEmote = getEmote(dungeonInfo[0]);

        emotes.add(getEmote(dungeonInfo[1]));
        emotes.addAll(Arrays.stream(dungeonInfo[2].split(" ")).map(this::getEmote).collect(Collectors.toList()));
        additionalEmotes.addAll(Arrays.stream(dungeonInfo[4].split(" ")).filter(s -> !s.isEmpty()).map(this::getEmote).collect(Collectors.toList()));

        this.dungeonName = dungeonInfo[3];

        this.raidColor = new Color(Integer.parseInt(dungeonInfo[5]));
        if (customization.RaidLeaderPrefsConnector.getRlColor(raidLeader.getId(), raidGuild.getId()) != null)
            this.raidColor = customization.RaidLeaderPrefsConnector.getRlColor(raidLeader.getId(), raidGuild.getId());

        if (!dungeonInfo[7].isEmpty()) {
            try {
                dungeonImage = String.valueOf(new URL(dungeonInfo[7]));
            } catch (MalformedURLException e) { }
        }
        if (!dungeonInfo[8].isEmpty()) {
            try {
                dungeonStartImage = String.valueOf(new URL(dungeonInfo[8]));
            } catch (MalformedURLException e) { }
        }
    }

    public void addRaidMessageEmotes() {
//        raidMessage.addReaction(dungeonEmote).queue();
        for (Emote emote : emotes) {
            //raidMessage.addReaction(emote).queue();
            emoteReacts.put(Emoji.fromEmote(emote), new ArrayList<>());
        }
        for (Emote emote : additionalEmotes.stream().filter(Objects::nonNull).collect(Collectors.toList())) raidMessage.addReaction(emote).queue();
    }

    public List<ActionRow> getActionRows () {
        List<ActionRow> actionRows = new ArrayList<>();

        List<Component> curComps = new ArrayList<>();
        curComps.add(Button.of(ButtonStyle.PRIMARY, "join", "Join").withEmoji(Emoji.fromEmote(dungeonEmote)).withDisabled(!isStarted));

        for (int i = 1; i < emotes.size(); i++) {
            if (i % 5 == 0) {
                actionRows.add(ActionRow.of(curComps));
                curComps = new ArrayList<>();
            }
            curComps.add(Button.of(ButtonStyle.SUCCESS, "early" + i, Utils.splitCamelCase(emotes.get(i - 1).getName())).withEmoji(Emoji.fromEmote(emotes.get(i - 1))));
        }
        if (!curComps.isEmpty()) actionRows.add(ActionRow.of(curComps));

        return actionRows;
    }

    public ActionRow getControlPanelActions() {
        return ActionRow.of(Button.primary("location","Set Realm Location").withEmoji(Emoji.fromUnicode("🗺")).withDisabled(isStarted),
                Button.success("startafk","Distribute Realm Location").withEmoji(Emoji.fromUnicode("📤")).withDisabled(!setRealmLocation || isStarted),
                Button.danger("endafk","Stop distributing Location").withEmoji(Emoji.fromUnicode("❌")).withDisabled(!isStarted));
    }

    public void updateControlPanel() {
        if (controlPanel != null) controlPanel.editMessage(controlPanelEmbed().build()).setActionRows(getControlPanelActions()).queue();
    }

    public void startRaid() {
        // Start the timer
        isStarted = true;
        raidStatusChannel.sendMessage("@here This raid is now open, please press the join button to be given location.").queue(m -> m.delete().queueAfter(30L, TimeUnit.SECONDS, null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)));
        controlPanel.clearReactions().queue();
        updateControlPanel();
        raidMessage.editMessage(raidStartEmbed().build()).content(raidLeader.getEffectiveName() + " has started a new `" + dungeonName + "` raid!").setActionRows(getActionRows()).queue();
        interactionHooks.values().forEach(i -> {
            i.editOriginal("The location of this raid has been set to `" + location + "`").queue();
        });
    }

    public void promptForLocation(Member member) {
        String raidLocations = "";

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Please select a " + (location.equals("") ? "" : "new") + " location for your raid")
                .setColor(raidColor)
                .setDescription("Current Raid Locations:\n" + raidLocations + "Please type your location in the chat below")
                .addField("Current Location: ", "`" + (location.equals("") ? "None Set" : location) + "`", false);
        embedBuilder.setFooter("You have two minutes to set your location");
        embedBuilder.setTimestamp(new Date().toInstant());

        Message locationPrompt = raidCommandsChannel.sendMessage(embedBuilder.build()).complete();

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(member.getUser()) && e.getChannel().equals(raidCommandsChannel);
        }, e -> {
            setRealmLocation = true;

            final int[] delay = {0};
            for (List<Member> members : emoteReacts.values()) {
                members.stream().map(Member::getUser)
                        .forEach(user -> user.openPrivateChannel().queueAfter(delay[0], TimeUnit.SECONDS,
                                p -> p.sendMessage("The realm location for this raid is `" + location + "`")
                                        .queueAfter(delay[0], TimeUnit.SECONDS, m -> delay[0]++)));
            }

            location = e.getMessage().getContentRaw();
            locationPrompt.delete().queue();
            e.getMessage().delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            updateControlPanel();
        }, 2L, TimeUnit.MINUTES, () -> {
            Utils.errorMessage("Location change failed", "Raid leader did not enter a location", locationPrompt, 15L);
        });
    }

    private EmbedBuilder controlPanelEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        String reactString = emotes.stream()
                .map(e -> e.getAsMention() + ": " + emoteReacts.get(Emoji.fromEmote(e)).stream().map(Member::getAsMention).collect(Collectors.joining(", ")) + (emoteReacts.get(Emoji.fromEmote(e)).isEmpty() ? "None" : ""))
                .collect(Collectors.joining("\n"));
        String memberString = interactionHooks.keySet().stream().map(Member::getAsMention).collect(Collectors.joining(", "));

        embedBuilder.setAuthor(raidLeader.getEffectiveName() + "'s Control Panel", null, raidLeader.getUser().getAvatarUrl())
                .setColor(raidColor)
                .addField("Current Status",  "```\n" + (isStarted ? "Distributing Location" : "Runes " + (setRealmLocation ? "now have the realm location - Press the 'Distribute' button to open this raid" : "Meetup")) + "\n```", false)
                .addField((setRealmLocation ? "Realm " : "Headcount ") + "Location", "```\n" + (location.isEmpty() ? "None Set Yet" : location) + "\n```", false)
                .addField(interactionHooks.size() + " Current People with Location", (memberString.isEmpty() ? "None" : memberString), false)
                .addField("Current Runes, Decas, and Incs", (reactString.isEmpty() ? "None" : reactString), false);

        if (!setRealmLocation) embedBuilder.setDescription("```\nPlease set a Realm Location to continue this raid.\n```\n");

        return embedBuilder;
    }

    private EmbedBuilder raidStatusEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(raidLeader.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "") + "'s " + dungeonName + " Raid", null, raidLeader.getUser().getAvatarUrl())
                .setColor(raidColor);
        if (dungeonImage != null) embedBuilder.setThumbnail(dungeonImage);
        else embedBuilder.setThumbnail(dungeonEmote.getImageUrl());

        embedBuilder.setDescription("```diff\n-THIS AFK CHECK IS IN THE RUNE PHASE\n```\n" +
                "We are currently looking for runes and incs. If you have one, please click the corresponding one to be given location. Otherwise please wait for this afk to open.");

        embedBuilder.setFooter("Afk check started: ")
                .setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    private EmbedBuilder raidStartEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(raidLeader.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "") + "'s " + dungeonName + " Raid", null, raidLeader.getUser().getAvatarUrl())
                .setColor(raidColor);
        if (dungeonImage != null) embedBuilder.setThumbnail(dungeonImage);
        else embedBuilder.setThumbnail(dungeonEmote.getImageUrl());

        embedBuilder.setDescription("```diff\n+THIS AFK CHECK IS OPEN\n```\n" +
                "This AFK check is now open! Please react with join to be given location. " +
                "**Thank you** to the following people for bringing items to the runs: " +
                emoteReacts.entrySet().stream().filter(e -> !e.getValue().isEmpty()).map(e -> e.getValue().stream().map(m -> m.getAsMention() + "(" + e.getKey().getAsMention() + ")").collect(Collectors.joining(", "))).collect(Collectors.joining(", ")));

        embedBuilder.setFooter("Afk check started: ")
                .setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    private EmbedBuilder raidEndEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(raidLeader.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "") + "'s " + dungeonName + " Raid", null, raidLeader.getUser().getAvatarUrl())
                .setColor(raidColor);
        if (dungeonImage != null) embedBuilder.setThumbnail(dungeonImage);
        else embedBuilder.setThumbnail(dungeonEmote.getImageUrl());

        embedBuilder.setDescription("This AFK check is now over! Another run will be organized soon. " +
                "**Thank you** to the following people for bringing items to the runs: " +
                emoteReacts.entrySet().stream().filter(e -> !e.getValue().isEmpty()).map(e -> e.getValue().stream().map(m -> m.getAsMention() + "(" + e.getKey().getAsMention() + ")").collect(Collectors.joining(", "))).collect(Collectors.joining(", ")));

        embedBuilder.setFooter("Afk check ended: ")
                .setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    public Emote getEmote(String emoteId) {
        return Goldilocks.jda.getEmoteById(emoteId);
    }

    private class updateTask extends TimerTask {
        public void run() {
            if (!ended) updateControlPanel();
            else timer.cancel();
        }
    }

}
