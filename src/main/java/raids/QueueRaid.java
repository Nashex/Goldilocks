package raids;

import com.google.gson.annotations.Expose;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import org.apache.commons.lang3.StringUtils;
import setup.SetupConnector;
import utils.Utils;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static main.Goldilocks.eventWaiter;

public class QueueRaid {

    /* GOLDILOCKS QUEUE RAID SYSTEM
    This is the official queue system for Goldilocks. It has the following
    methods that are used to control the multiple aspects of a queue
    styled AFK check. Here is the general breakdown.

    1. Start of the AFK
        ⇒ Gets the early location reacts to join the voice channel
        ⇒

    1. The initiation of the AFK check.
        ⇒ It will post a control panel to the commands channel along with an
          AFK check to the raid status channel. The raid status Message will
          have 4 buttons.


     */

    @Expose private Member raidLeader;
    @Expose public Guild raidGuild;
    @Expose private TextChannel raidStatusChannel;
    @Expose private TextChannel raidCommandsChannel;

    private TextChannel raidControlChannel = null;
    private Category raidControlCategory = null;
    private Message placeHolderMessage = null;

    private boolean createdVc = false;
    @Expose private VoiceChannel queueVoiceChannel = null;
    @Expose private VoiceChannel confirmationVoiceChannel = null;

    @Expose private Message controlPanel = null;
    private String controlPanelId;
    @Expose private Message raidMessage;
    private String raidMessageId;
    private Message logMessage = null;

    private String[] dungeonInfo;
    private Emote dungeonEmote;
    private Emote keyEmote;
    private List<Emote> earlyLocEmotes = new ArrayList<>();
    private HashMap<Emote, Integer> emoteLimits = new HashMap<>();
    private List<Emote> additionalEmotes = new ArrayList<>();
    private List<Emote> raidLeaderEmotes = new ArrayList<>();
    private Emote[] defaultEmotes = {getEmote("771681007086075964"), getEmote("823935665514610738"), getEmote("771680219382677514"), getEmote("768663241076244480")};
    private List<Emote> debuffEmotes = Arrays.asList(getEmote("768995607905566740"), getEmote("771659502839398411"),
            getEmote("771659502780153877"), getEmote("771659503099445248"), getEmote("771659503094464522"));
    private Emote nitro = Goldilocks.jda.getEmoteById("771683299106095105");
    private Emote assist = Goldilocks.jda.getEmoteById("735943510473310309");

    private boolean defaultRaid;
    @Expose private String dungeonName;
    private Color raidColor;
    private Role raiderRole;
    private int dungeonLimit;
    @Expose private int raidType;
    @Expose private String dungeonImage = null;
    private String dungeonStartImage = null;

    @Expose private boolean isStarted;
    @Expose private Long timeStarted;
    private Long startingTime;
    @Expose private Long timeToStart = 300L;

    @Expose private String meetupLocation;
    @Expose private String location = "";
    private int customCap;
    @Expose private int numChained = 0;
    private String logString = "";

    private boolean earlyLoc = false;
    private boolean feedback = false;
    private boolean quota = false;
    private boolean delete = false;
    private int maxQueueSize = 10;

    @Expose private TextChannel feedbackChannel = null;
    public List<Member> feedbackList = new ArrayList<>();
    @Expose private Message feedbackMessage;

    public LogPanel logPanel;

    HashMap<Member, InteractionHook> raidersInRaid = new HashMap<>();
    HashMap<Emote, List<Member>> earlyLocationMap = new HashMap<>();

    private Timer timer;
    private ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);
    boolean openedVoiceChannel = false;

    private final String[] meetupLocations = new String[]{"US West 4", "US West 2", "US West", "US East 4", "US East 3", "US East 2", "US East", "US South", "US South 2", "US South 3", "US Northwest", "Austrailia", "AsiaEast", "EU East", "EU East", "EU East 2", "EU East 3"};

    public QueueRaid(Member raidLeader, int raidType, TextChannel raidStatusChannel, TextChannel raidCommands, String location, boolean fromCache, int customCap, VoiceChannel voiceChannel) {
        this.raidLeader = raidLeader;
        this.raidGuild = raidLeader.getGuild();
        this.raidStatusChannel = raidStatusChannel;
        this.raidCommandsChannel = raidCommands;
        Random random = new Random();
        this.meetupLocation = meetupLocations[random.nextInt(meetupLocations.length - 1)] + (random.nextBoolean() ? " Left" : " Right");
        this.timeStarted = System.currentTimeMillis();
        this.raiderRole = Goldilocks.jda.getRoleById(Database.getRaiderRole(raidStatusChannel.getId()));
        this.raidType = raidType;
        this.customCap = customCap;
        this.confirmationVoiceChannel = voiceChannel;
        this.startingTime = System.currentTimeMillis();

        this.defaultRaid = Arrays.asList(SetupConnector.getFieldValue(raidGuild, "guildInfo", "quotaString").split(" ")).contains(String.valueOf(raidType));

        //this.raidControlCategory = Database.getRaidSectionCategory(raidStatusChannel);

        this.dungeonInfo = (raidType != -2 ? DungeonInfo.dungeonInfo(raidGuild, raidType).dungeonInfo : DungeonInfo.eventDungeon);
        getDungeonInfo();

        this.feedback = Database.hasFeedback(raidGuild);
        this.earlyLoc = Database.isEarlyLoc(raidGuild);
        this.quota = Database.hasQuota(raidGuild);
        this.delete = Database.deleteMessages(raidGuild);

        createRaid();

    }

    public void createRaid() {
        //if (voiceChannel == null) createRaidVoiceChannel();
        //else voiceChannel.getManager().setUserLimit(dungeonLimit).queue();


        createControls();
        createConfirmationVoiceChannel();
        createQueueVoiceChannel();
        sendRaidMessage();
        //logPanel = new LogPanel(this);
        //if (feedback) createFeedback();

        //logPanel.cache();
        timer = new Timer();
        timer.schedule(new updateTask(), 5000L, 5000L); // TODO Set Length

    }

    public void createQueueVoiceChannel() {
        Category category = raidStatusChannel.getParent();
        queueVoiceChannel = raidGuild.createVoiceChannel(raidLeader.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "") + "'s " + dungeonName + " Queue")
                .setParent(category)
                .setPosition(0).complete();
        queueVoiceChannel.getManager().sync().putPermissionOverride(raiderRole, null, EnumSet.of(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VIEW_CHANNEL))
                .putPermissionOverride(raidLeader, EnumSet.of(Permission.MANAGE_CHANNEL), null).queue();
    }

    public void createConfirmationVoiceChannel() {
        Category category = raidStatusChannel.getParent();
        confirmationVoiceChannel = raidGuild.createVoiceChannel(raidLeader.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "") + "'s " + dungeonName + " Reaction Confirmations")
                .setParent(category)
                .setPosition(0).complete();
        confirmationVoiceChannel.getManager().sync().putPermissionOverride(raiderRole, null, EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK))
                .putPermissionOverride(raidLeader, EnumSet.of(Permission.MANAGE_CHANNEL), null).queue();
    }

    public void joinVoiceChannels() {
        List<Member> members = confirmationVoiceChannel.getMembers();
        members.forEach(member -> {
            try {
                raidGuild.moveVoiceMember(member, queueVoiceChannel).complete();
            } catch (Exception e) {
            }
        });
        confirmationVoiceChannel.delete().queue();
    }

    public void openVC() {
        joinVoiceChannels();
        raidMessage.editMessage(raidStartedEmbed().build()).setActionRows().queue(m -> m.clearReactions().queue());
    }

    /*
    CONTROL SECTION
     */

    // CONTROL ACTION ROWS!
    private final ActionRow CONTROL_PRE = ActionRow.of(net.dv8tion.jda.api.interactions.components.Button.secondary("meetuplocation", "Change Meetup Location").withEmoji(Emoji.fromUnicode("🗺")),
            net.dv8tion.jda.api.interactions.components.Button.success("openvc","Open VC").withDisabled(true),
            Button.danger("abort","Abort Raid").withDisabled(true));

    private final ActionRow CONTROL_START = ActionRow.of(net.dv8tion.jda.api.interactions.components.Button.secondary("meetuplocation", "Change Meetup Location").withEmoji(Emoji.fromUnicode("🗺")),
            net.dv8tion.jda.api.interactions.components.Button.success("openvc","Open VC"),
            Button.danger("abort","Abort Raid"));

    private void createControls() {
        if (raidControlCategory != null) createControlChannel();
        else createControlPanel();
    }

    private void createControlChannel() {
        raidControlChannel = raidControlCategory.createTextChannel(raidLeader.getEffectiveName().split(" ")[0] + "-controls")
                .complete();
        raidControlChannel.getManager().sync()
                .putPermissionOverride(raidLeader, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY), EnumSet.of(Permission.MANAGE_CHANNEL))
                .queue();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(raidLeader.getEffectiveName() + "'s Raid Controls")
                .setColor(Goldilocks.BLUE)
                .setDescription("This control panel is located in " + raidControlChannel.getAsMention())
                .setTimestamp(new Date().toInstant());
        placeHolderMessage = raidCommandsChannel.sendMessage(embedBuilder.build()).complete();

        raidCommandsChannel = raidControlChannel;

        createControlPanel();
    }

    private void createControlPanel() {
        controlPanel = raidCommandsChannel.sendMessage(controlPanelEmbed().build())
                .setActionRows(CONTROL_PRE)
                .complete();
        controlPanel.editMessage(controlPanelEmbed().build())
                .setActionRows(CONTROL_START)
                .queueAfter(5L, TimeUnit.SECONDS);
        controlButtonHandler();
    }

    public EmbedBuilder controlPanelEmbed() {
        String chainString = numChained == 0 ? "" : "**Total Runs:**\nYou have chained `" + numChained + " run" + (numChained == 1 ? "" : "s") + "` in addition to the first run\n";

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Control Panel for " + raidLeader.getEffectiveName() + "'s " + dungeonName + " Raid")
                .setColor(raidColor)
                .setDescription(chainString);
        embedBuilder.addField("Meetup Location", meetupLocation.isEmpty() ? "`Please set a location!`" : "`" + meetupLocation + "`",true);
        embedBuilder.addField("Realm Location", location.isEmpty() ? "`Not Set`" : "`" + location + "`" ,true);

        String keySection = "";
        String runesSections = "";
        String itemsSection = "";
        String dpsSections = "";

        for (Emote emote : earlyLocEmotes) {
            String emoteName = emote.getName().toLowerCase();
            String memberString = earlyLocationMap.get(emote).stream().map(Member::getAsMention).collect(Collectors.joining(", "));
            if (memberString.isEmpty()) memberString = "None";
            if (emoteName.contains("key")) {
                keySection += memberString;
            } else if (emoteName.contains("rune") || emoteName.equals("inc")) {
                runesSections += emote.getAsMention() + " **| `" + earlyLocationMap.get(emote).size() + "/" + getEmoteLimit(emote) + "` |** " + memberString + "\n";
            } else if (emoteName.contains("dps")) {
                dpsSections += emote.getAsMention() + " **| `" + earlyLocationMap.get(emote).size() + "/" + getEmoteLimit(emote) + "` |** " + memberString + "\n";
            } else {
                itemsSection += emote.getAsMention() + " **| `" + earlyLocationMap.get(emote).size() + "/" + getEmoteLimit(emote) + "` |** " + memberString + "\n";
            }
        }

        if (!keySection.isEmpty()) embedBuilder.addField("Keys", keySection, false);
        if (!runesSections.isEmpty()) embedBuilder.addField("Incs and Runes", runesSections, false);
        if (!itemsSection.isEmpty()) embedBuilder.addField("Items and Classes", itemsSection, false);
        if (!dpsSections.isEmpty()) embedBuilder.addField("DPS Classes", dpsSections, false);

        if (Database.isShatters(raidGuild)) embedBuilder.addField("Logs", "```\n" + (numChained == 0 && logString.isEmpty() ? "No runs have been logged." : logString) + "\n```", false);
        return  embedBuilder;
    }

    private int getEmoteLimit(Emote emote) {
        return emoteLimits.getOrDefault(emote, 5);
    }

    private void controlButtonHandler() {
        List<String> controls = Arrays.asList("meetuplocation", "openvc", "abort");

        // End the AFK check
        eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return !e.getUser().isBot() && e.getMessageId().equals(controlPanel.getId()) && controls.contains(e.getComponentId());
        }, e -> {
            String control = e.getComponentId();

            e.deferEdit().queue();
            if (control.equals("meetuplocation")) {
                // Change location
            }

            if (control.equals("openvc")) {
                openVC();
            }
            if (control.equals("abort")) {
                abortRaid();
                return;
            }

            controlButtonHandler();

        }, 1L, TimeUnit.HOURS, () -> {
            // End Raid
        });
    }

    private void updateMessages() {
        controlPanel.editMessage(controlPanelEmbed().build()).queue();
        raidMessage.editMessage(raidMessageStartEmbed().build()).queue();
    }

    /*
    END CONTROL SECTION
     */

    /*
    RAID STATUS SECTION
     */

    public void sendRaidMessage() {
        raidMessage = raidStatusChannel.sendMessage("@ here " + raidLeader.getEffectiveName() + " has started a new " + dungeonName + " " + dungeonEmote.getAsMention() + " queue raid in [VOICE CHANNEL]").complete();
        raidMessage.editMessage(raidMessageStartEmbed().build()).setActionRow(
                Button.primary("joinqueue", "Join Queue"),
                Button.success("earlylocation", "Early Location and Priority Queue"),
                Button.success("classreact", "Gear, Class, and Debuff")
        ).queueAfter(5L, TimeUnit.SECONDS, m -> {
            //addRaidMessageEmotes();
            generalButtonHandler();
            leaveButtonHandler();
        });
    }

    public EmbedBuilder raidMessageStartEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(raidLeader.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "") + "'s " + dungeonName + " Raid", null, raidLeader.getUser().getAvatarUrl())
                .setColor(raidColor);
//        if (dungeonImage != null) embedBuilder.setThumbnail(dungeonImage);
//        else embedBuilder.setThumbnail(dungeonEmote.getImageUrl());

        String raidDescription = "This AFK is currently in the queue phase, to join the raid please react with the Join Queue button and then voice the voicechannel it links you to.\n\n" +
                "**GEAR, CLASS, AND DEBUFF ITEMS |** ";

        raidDescription += Arrays.stream(defaultEmotes).filter(emote -> !additionalEmotes.contains(emote) && !earlyLocEmotes.contains(emote)).map(emote -> emote.getAsMention()).collect(Collectors.joining(" "));

        if (!additionalEmotes.isEmpty()) raidDescription += debuffEmotes.stream().filter(emote -> additionalEmotes.contains(emote)).map(emote -> emote.getAsMention()).collect(Collectors.joining(" "));
        else raidDescription += debuffEmotes.stream().map(Emote::getAsMention).collect(Collectors.joining(" "));

        String finalRaidDescription = raidDescription;
        raidDescription += additionalEmotes.stream().filter(emote -> !finalRaidDescription.contains(emote.getAsMention()) && !raidLeaderEmotes.contains(emote)).map(Emote::getAsMention).collect(Collectors.joining());
        raidDescription += raidLeaderEmotes.stream().filter(emote -> !finalRaidDescription.contains(emote.getAsMention()) && !earlyLocEmotes.contains(emote)).map(Emote::getAsMention).collect(Collectors.joining());

        raidDescription += "\nPlease react to the Gear, Class, Debuff button if you have any of the above items.";

        raidDescription += "\n\n**EARLY LOCATION ITEMS | **" + earlyLocEmotes.stream().map(Emote::getAsMention).collect(Collectors.joining(" "));
        if (!earlyLocEmotes.isEmpty())
            raidDescription += "\nIf you are bringing any of the above emotes please press the early location button.\n";

        for (Emote emote : earlyLocEmotes) {
            switch (emote.getId()) {
                case "768782625783283743":
                case "822247326600134686":
                    raidDescription += "\nIf you intend to rush, please press the early location button";
                default:
                    break;
            }
        }

        DecimalFormat df = new DecimalFormat("00.00");
        String earlyLocationInfo = "";
        String curLine = "";

        String keySection = "";
        String runesSections = "";
        String itemsSection = "";
        String dpsSections = "";
        int[] numSections = { 0, 0, 0, 0};
        for (Emote emote : earlyLocEmotes) {
            String emoteName = emote.getName().toLowerCase();
            float percent = earlyLocationMap.get(emote).size() / (float) getEmoteLimit(emote);
            String emoteString = emote.getAsMention() + " | " +
                    Utils.renderPercentage(percent, 100 / getEmoteLimit(emote)) + " | `" + df.format(percent * 100) + "%`";

            if (emoteName.contains("key")) {
                keySection += emoteString;
            } else if (emoteName.contains("rune") || emoteName.equals("inc")) {
                if (numSections[1] >= 4) {
                    runesSections += "\n";
                    numSections[1] = 0;
                }
                emoteString = emote.getAsMention() + " | " +
                        Utils.renderPercentage(percent, 50) + " | `" + df.format(percent * 100) + "%`";
                //runesSections += emoteString + " | `" + earlyLocationMap.get(emote).size() + " / " + getEmoteLimit(emote) + "` | "; TODO Dont hard code
                runesSections += emoteString + " | `" + earlyLocationMap.get(emote).size() + " / " + 2 + "` | ";
                numSections[1] += 2;
            } else if (emoteName.contains("dps")) {
                dpsSections += emoteString + "\n";
            } else {
                if (numSections[3] >= 10) {
                    itemsSection += "\n";
                    numSections[3] = 0;
                }
                itemsSection += emoteString + " | `" + earlyLocationMap.get(emote).size() + " / " + getEmoteLimit(emote) + "` | ";
                numSections[3] += getEmoteLimit(emote);
            }
        }

        if (!runesSections.isEmpty()) embedBuilder.addField("Incs and Runes", runesSections, false);
        if (!itemsSection.isEmpty()) embedBuilder.addField("Early Location Classes and Gear", itemsSection, false);

        embedBuilder.addField("Queue Status", Utils.renderPercentage(raidersInRaid.size() / (float) customCap, 5)
                + " | `" + df.format(raidersInRaid.size() / (float) customCap) + "%` | `" + raidersInRaid.size() + " / " + customCap + "` |", false);

        raidDescription += "\n" + nitro.getAsMention() + "If you are boosting this server press the early location button.";
        embedBuilder.setDescription(raidDescription)
                .setFooter("This raid will begin automatically or when the raid leader is ready")
                .setTimestamp(new Date().toInstant());
        return  embedBuilder;
    }

    public EmbedBuilder raidStartedEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(raidLeader.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "") + "'s " + dungeonName + " Raid", null, raidLeader.getUser().getAvatarUrl())
                .setColor(raidColor);
        if (dungeonImage != null) embedBuilder.setThumbnail(dungeonImage);
        else embedBuilder.setThumbnail(dungeonEmote.getImageUrl());

        embedBuilder.setDescription("This AFK check has now ended. We are currently running with **" + queueVoiceChannel.getMembers().size() + "** members.\n\n" +
                "If at anytime you are disconnected, join lounge to be dragged back in. If you not like to be dragged back in, please press the leave" +
                " raid button.");
        embedBuilder.setFooter("This has ended, please wait for a new one to start.");
        embedBuilder.setTimestamp(new Date().toInstant());
        return  embedBuilder;
    }

    public void addRaidMessageEmotes() {
        for (Emote emote : defaultEmotes) {
            if (!earlyLocEmotes.contains(emote)) {
                raidMessage.addReaction(emote).queue();
            }
        }

        for (Emote emote : additionalEmotes) {
            if (!debuffEmotes.contains(emote)) {
                raidMessage.addReaction(emote).queue();
            }
        }

        for (Emote emote : raidLeaderEmotes) {
            raidMessage.addReaction(emote).queue();
        }

        for (Emote emote : debuffEmotes) {
            if (!additionalEmotes.isEmpty()) {
                if (additionalEmotes.contains(emote)) {
                    raidMessage.addReaction(emote).queue();
                }
            } else {
                raidMessage.addReaction(emote).queue();
            }
        }
    }

    /*
    GENERAL BUTTON HANDLING
     */

    public void generalButtonHandler() {
        List<String> controls = Arrays.asList("joinqueue", "earlylocation");

        eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return e.getMessageId().equals(raidMessage.getId()) && controls.contains(e.getComponentId());
        }, e -> {

            String control = e.getComponentId().replaceAll("[^A-Za-z]", "");

            if (control.equals("joinqueue")) {
                joinQueue(e);
            }
            if (control.equals("earlylocation")) {
                getEarlyLocReaction(e);
            }

            generalButtonHandler();

        }, 30L, TimeUnit.MINUTES, () -> { });
    }

    public void leaveButtonHandler() {
        eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return raidersInRaid.containsKey(e.getMember()) && e.getComponentId().equals("leave");
        }, e -> {

            Member member = e.getMember();
            if (member == null) return;
            if (member.getVoiceState().inVoiceChannel()) {
                VoiceChannel channel = member.getVoiceState().getChannel();
                if (channel.equals(queueVoiceChannel)) {
                    raidGuild.kickVoiceMember(member).queue();
                }
            }
            raidersInRaid.get(member).editOriginal("You have successfully left this raid. Feel free to rejoin while it is still in the queue phase.").setActionRows().queue();
            raidersInRaid.remove(member);

            leaveButtonHandler();
        }, 2L, TimeUnit.MINUTES, () -> { });
    }

    public void joinQueue(ButtonClickEvent event) {
        if (event.getMember() == null) return;
        event.reply("You have successfully joined this raid's queue. Please connect to this VC: " + queueVoiceChannel.getAsMention())
                .addActionRow(Button.danger("leave","Leave Queue"))
                .setEphemeral(true)
                .queue(e -> raidersInRaid.put(event.getMember(), e));
        queueVoiceChannel.getManager().putPermissionOverride(event.getMember(), EnumSet.of(Permission.VOICE_CONNECT, Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_SPEAK)).queue();
        // Add user to the voice channel
    }

    public void getEarlyLocReaction(ButtonClickEvent event) {
        event.reply("Please select the item you are bringing to the raid. If you are found not to have this item you will be suspended.")
                .addActionRows(earlyLocActionRows())
                .setEphemeral(true)
                .queue();

        eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return e.getUser().equals(event.getUser()) && (e.getComponentId().contains("earlyloc") || e.getComponentId().equals("mistake"));
        }, e -> {

            if (e.getComponentId().equals("mistake")) {
                e.deferEdit().setActionRows().setContent("Bummer. Feel free to join the queue.").queue();
                return;
            }

            int choice = Integer.parseInt(e.getComponentId().replaceAll("[^0-9]", ""));

            confirmationVoiceChannel.getManager().putPermissionOverride(e.getMember(), EnumSet.of(Permission.VOICE_CONNECT, Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_SPEAK)).queue();
            e.editMessage("Thank you for bringing a " + earlyLocEmotes.get(choice).getAsMention() + " !\n" +
                    "The meetup location for this raid is `" + meetupLocation + "`. Please join this VC: " + confirmationVoiceChannel.getAsMention() + " and then head to location.")
                    .setActionRows()
                    .queue();
            if (!earlyLocationMap.get(earlyLocEmotes.get(choice)).contains(e.getMember())) earlyLocationMap.get(earlyLocEmotes.get(choice)).add(e.getMember());

        }, 2L, TimeUnit.MINUTES, () -> { event.editMessage("You did not confirm in time.").setActionRows().queue(); });
    }

    public List<ActionRow> earlyLocActionRows() {
        List<ActionRow> actionRows = new ArrayList<>();
        List<Button> curRow = new ArrayList<>();

        int index = 0;
        for (Emote emote : earlyLocEmotes) {
            Button button = Button.secondary("earlyloc" + index, StringUtils.capitalize(Utils.splitCamelCase(emote.getName()))).withEmoji(Emoji.fromEmote(emote));
            if (curRow.size() < 5) {
                curRow.add(button);
            } else {
                actionRows.add(ActionRow.of(curRow));
                curRow = new ArrayList<>(Arrays.asList(button));
            }
            index++;
        }
        if (curRow.size() < 5) {
            curRow.add(Button.danger("mistake", "I Don't Have These"));
            actionRows.add(ActionRow.of(curRow));
        }
        else {
            actionRows.add(ActionRow.of(curRow));
            actionRows.add(ActionRow.of(Button.danger("mistake", "I Don't Have These")));
        }

        return actionRows;
    }

    /*
    ABORT SECTION
     */

    public void abortRaid() {
        if (delete) {
            raidMessage.delete().queue();
            controlPanel.delete().queue();
        } else {
            controlPanel.editMessage(controlPanelEmbed().build()).setActionRows().queue();
        }
        //if () raidControlChannel.delete().queue();
        raidersInRaid.values().forEach(v -> v.editOriginal("This raid has been aborted.").setActionRows().queue());
        queueVoiceChannel.delete().queue();
        confirmationVoiceChannel.delete().queue();
    }

    public void getDungeonInfo() {
        this.dungeonEmote = getEmote(dungeonInfo[0]);
        this.keyEmote = getEmote(dungeonInfo[1]);

        if (raidType == 0) earlyLocEmotes.add(keyEmote); // Oryx 3

        this.raidLeaderEmotes = customization.RaidLeaderPrefsConnector.getRlEmotes(raidLeader.getId(), raidGuild.getId());
        //Create a list of early location emotes
        if (!dungeonInfo[2].isEmpty()) {
            for (String emoteId : dungeonInfo[2].split(" ")) {
                if (emoteId.contains(",")) {
                    String emoteString[] = emoteId.split(",");
                    Emote emote = getEmote(emoteString[0].trim());;
                    earlyLocEmotes.add(emote);
                    emoteLimits.put(emote, Integer.parseInt(emoteString[1].trim()));
                } else {
                    earlyLocEmotes.add(getEmote(emoteId.trim()));
                }
            }
        }
        Guild earlyLocGuild = Goldilocks.jda.getGuildById("794814328082530364");
        List<String> earlyLocEmoteNames = earlyLocEmotes.stream().map(emote -> emote.getName()).collect(Collectors.toList());
        for (Emote emote : raidLeaderEmotes)
            if (earlyLocGuild.getEmotes().contains(emote) && !earlyLocEmoteNames.contains(emote.getName())) earlyLocEmotes.add(emote);
        this.dungeonName = dungeonInfo[3];
        if (!dungeonInfo[4].isEmpty())
            for (String emoteId : dungeonInfo[4].split(" ")) additionalEmotes.add(getEmote(emoteId.trim()));

        this.raidColor = new Color(Integer.valueOf(dungeonInfo[5]));
        if (customization.RaidLeaderPrefsConnector.getRlColor(raidLeader.getId(), raidGuild.getId()) != null)
            this.raidColor = customization.RaidLeaderPrefsConnector.getRlColor(raidLeader.getId(), raidGuild.getId());

        this.dungeonLimit = customCap == -1 ? (dungeonInfo[6].isEmpty() ? 50 : Integer.valueOf(dungeonInfo[6])) : customCap;
        if (!dungeonInfo[7].isEmpty()) {
            try {
                dungeonImage = String.valueOf(new URL(dungeonInfo[7]));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        if (!dungeonInfo[8].isEmpty()) {
            try {
                dungeonStartImage = String.valueOf(new URL(dungeonInfo[8]));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        earlyLocEmotes.forEach(e -> earlyLocationMap.put(e, new ArrayList<>()));

    }

    public Emote getEmote(String emoteId) {
        return Goldilocks.jda.getEmoteById(emoteId.split(":")[0]);
    }

    private class updateTask extends TimerTask {
        public void run() {
            if (!openedVoiceChannel) updateMessages();
            else timer.cancel();
        }
    }

}
