package raids;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;
import main.Config;
import main.Database;
import main.Goldilocks;
import quota.QuotaManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.apache.commons.lang3.StringUtils;
import raids.caching.RaidCaching;
import raids.queue.Queue;
import raids.queue.QueueHub;
import setup.SetupConnector;
import shatters.SqlConnector;
import sheets.GoogleSheets;
import utils.ClearVc;
import utils.Utils;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static main.Goldilocks.eventWaiter;

@Getter
@Setter
public class Raid {

    @Expose private Member raidLeader;
    @Expose public Guild raidGuild;
    @Expose private TextChannel raidStatusChannel;
    @Expose private TextChannel raidCommandsChannel;

    private TextChannel raidControlChannel = null;
    private Category raidControlCategory = null;
    private Message placeHolderMessage = null;

    private boolean createdVc = false;
    @Expose private VoiceChannel voiceChannel = null;

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

    public List<Member> keyReacts = new CopyOnWriteArrayList<>();
    private HashMap<User, Emote> earlyLocationMap = new HashMap<>();
    private List<Member> earlyLocReacts = new CopyOnWriteArrayList<>();private List<Member> assistReactions = new ArrayList<>();
    @Expose private List<Member> draggableMembers = new ArrayList<>();

    @Expose private boolean isStarted;
    @Expose private Long timeStarted;
    private Long startingTime;
    private ScheduledExecutorService raidTimeManager = new ScheduledThreadPoolExecutor(1);
    @Expose private Long timeToStart = 300L;
    private ScheduledFuture<?> scheduledFuture;

    @Expose private String location;
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

    @Expose public HashMap<Member, Long> memberTimeHashMap = new HashMap<>();

    public int numLocUsers = 0;

    public LogPanel logPanel;

    public Raid(Member raidLeader, int raidType, TextChannel raidStatusChannel, TextChannel raidCommands, String location, boolean fromCache, int customCap, VoiceChannel voiceChannel) {
        this.raidLeader = raidLeader;
        this.raidGuild = raidLeader.getGuild();
        this.raidStatusChannel = raidStatusChannel;
        this.raidCommandsChannel = raidCommands;
        this.location = location;
        this.timeStarted = System.currentTimeMillis();
        this.raiderRole = Goldilocks.jda.getRoleById(Database.getRaiderRole(raidStatusChannel.getId()));
        this.raidType = raidType;
        this.customCap = customCap;
        this.voiceChannel = voiceChannel;
        this.startingTime = System.currentTimeMillis();

        this.defaultRaid = raidType != -2 && Arrays.asList(SetupConnector.getFieldValue(raidGuild, "guildInfo", "quotaString").split(" ")).contains((DungeonInfo.dungeonInfo(raidGuild, raidType).dungeonIndex) + "");

        this.raidControlCategory = Database.getRaidSectionCategory(raidStatusChannel);

        this.dungeonInfo = (raidType != -2 ? DungeonInfo.dungeonInfo(raidGuild, raidType).dungeonInfo : DungeonInfo.eventDungeon);
        getDungeonInfo();

        this.feedback = Database.hasFeedback(raidGuild);
        this.earlyLoc = Database.isEarlyLoc(raidGuild);
        this.quota = Database.hasQuota(raidGuild);
        this.delete = Database.deleteMessages(raidGuild);

        if (!fromCache) RaidHub.RAID_POOL.execute(this::createRaid);
    }

    public void retrieveRaid(VoiceChannel voiceChannel, Message raidMessage, Message controlPanel, boolean isStarted, TextChannel feedbackChannel, Message feedbackMessage, LogPanel logPanel) {
        this.voiceChannel = voiceChannel;
        this.raidMessage = raidMessage;
        this.raidGuild = raidMessage.getGuild();
        raidMessageId = raidMessage.getId();
        this.controlPanel = controlPanel;
        controlPanelId = controlPanel.getId();
        this.isStarted = isStarted;
        this.feedbackChannel = feedbackChannel;
        this.feedbackMessage = feedbackMessage;
        this.dungeonLimit = voiceChannel.getUserLimit();
        this.startingTime = System.currentTimeMillis();

        assistReactions = controlPanel.retrieveReactionUsers(assist).complete().stream().map(user -> raidGuild.getMember(user)).collect(Collectors.toList());
        assistReactions.remove(raidGuild.getSelfMember());

        keyReacts = raidMessage.retrieveReactionUsers(keyEmote).complete().stream().map(user -> raidGuild.getMember(user)).collect(Collectors.toList());
        keyReacts.remove(raidGuild.getSelfMember());

        this.feedback = Database.hasFeedback(raidGuild);
        this.earlyLoc = Database.isEarlyLoc(raidGuild);
        this.quota = Database.hasQuota(raidGuild);
        this.delete = Database.deleteMessages(raidGuild);
        createdVc = voiceChannel.getName().replaceAll("[^0-9]", "").isEmpty() || voiceChannel.getName().contains("Oryx 3");

        if (logPanel != null) this.logPanel = logPanel;
        else this.logPanel = new LogPanel(this);

        if (!isStarted) {
            if (defaultRaid && !voiceChannel.getParent().getName().contains("Veteran") && raidType != 0) {
                timeToStart = 300L;
                startMessageTimer();
            }
        }
    }

    public void createRaid() {
        if (voiceChannel == null) createRaidVoiceChannel();
        else voiceChannel.getManager().setUserLimit(dungeonLimit).queue(null, (m) -> System.out.println(raidGuild.getName() + " | MISSING PERMISSIONS"));
        raidMessage = pingMessage();
        raidMessageId = raidMessage.getId();

        createControls();
        logPanel = new LogPanel(this);
        if (feedback) createFeedback();

        Goldilocks.TIMER.schedule(() -> {
            Queue queue = QueueHub.getQueue(raidStatusChannel);
            if (Database.queueEnabled(raidGuild) && queue != null && defaultRaid) queuePhase(queue);
            else {
                raidMessage.editMessage(raidMessageEmbed().build()).queue(message -> raidMessage = message);
                openVoiceChannel();
                addRaidMessageEmotes();
                if (defaultRaid) {
                    timeToStart = 300L;
                    startMessageTimer();
                } else {
                    timeToStart = -1L;
                    raidMessage.editMessage("" + raidLeader.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "") + " has started a new "
                            + dungeonName + " raid in <#" + voiceChannel.getId() + ">, now located above the drag channel." + " This raid will begin when " + raidLeader.getEffectiveName() + " is ready.").queue();
                    raidTimeManager.schedule(() -> updateControlPanel(), 6L, TimeUnit.SECONDS);
                }
            }
        }, 5L, TimeUnit.SECONDS);
        //RaidCaching.cacheRaid(this);
        RaidCaching.createRaid(raidGuild.getIdLong(), raidLeader.getIdLong(), voiceChannel.getIdLong(), raidCommandsChannel.getIdLong(),
                raidStatusChannel.getIdLong(), location, raidType, raidMessage.getIdLong(), controlPanel.getIdLong(), false,
                feedback ? feedbackChannel.getIdLong() : 0L , feedback ? feedbackMessage.getIdLong() : 0L);
        logPanel.cache();

    }

    public void queuePhase(Queue queue) {
        Goldilocks.TIMER.schedule(() -> {
            raidMessage.editMessage(queueEmbed(queue.queueChannel, Utils.renderPercentage(0.0f, 10), "0.00").build()).queue();
            List<Member> attemptedMembers = new ArrayList<>();
            List<Member> queuedMembers = queue.getAllQueuedMembers();
            final int[] draggedMembers = {0};
            final int[] index = {0};
            while (draggedMembers[0] < maxQueueSize && index[0] < queuedMembers.size()) {
                Member member = queuedMembers.get(index[0]);
                if (member.getVoiceState().inVoiceChannel()) {
                    raidGuild.moveVoiceMember(member, voiceChannel).queue(aVoid -> {
                        System.out.println(draggedMembers + " | " + member);
                        draggedMembers[0]++;
                        if (draggedMembers[0] % 2 == 0) {
                            raidMessage.editMessage(queueEmbed(queue.queueChannel, Utils.renderPercentage((float) draggedMembers[0] / maxQueueSize, 10),
                                    String.format("%.2f", ((float) draggedMembers[0] / maxQueueSize) * 100)).build()).queue();
                        }
                    }, new ErrorHandler().ignore(ErrorResponse.USER_NOT_CONNECTED));
                }
                attemptedMembers.add(member);
                index[0]++;
            }
            raidMessage.editMessage(queueEmbed(queue.queueChannel, Utils.renderPercentage(100.0f, 10),
                    "100.00").build()).queue();
            queue.removeMembers(attemptedMembers);
            openVoiceChannel();
            raidMessage.editMessage(raidMessageEmbed().build()).queue();
            addRaidMessageEmotes();
            timeToStart = 300L;
            startMessageTimer();
        }, 0L, TimeUnit.MILLISECONDS);
    }

    public void startRaid() {
        for (User user : controlPanel.retrieveReactionUsers("🗺")) {
            controlPanel.removeReaction("🗺", user).queue();
        }
        if (defaultRaid) {
            controlPanel.addReaction("📥").queue();
        }

        controlPanel.addReaction("🆕").queue();
        controlPanel.addReaction("♻").queue();
        controlPanel.addReaction("❌").queue();
        this.timeStarted = System.currentTimeMillis();
        this.isStarted = true;
        RaidCaching.setRaidActive(voiceChannel.getIdLong(), true);
        updateControlPanel();
        closeVoiceChannel();
        if (defaultRaid) {
            if (!scheduledFuture.isCancelled()) scheduledFuture.cancel(true);
        }
        controlPanel.editMessage(controlPanelEmbed().setFooter(timeStarted == 0 ? "This raid has been started automatically by " + raidGuild.getSelfMember().getEffectiveName() : "This raid has been started by " + raidLeader.getEffectiveName()).build()).complete();
        raidMessage.editMessage("" + raidLeader.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "") + " has started a new "
                + dungeonName + " raid in <#" + voiceChannel.getId() + ">, now located above the drag channel.").queue();
        raidMessage.editMessage(raidStartEmbed().build()).queue();
    }

    public void reopenRaid() {
        feedbackList.forEach(member -> feedbackChannel.upsertPermissionOverride(member).setAllow(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE).queue());
        feedbackList = new ArrayList<>();
        logPanel.deleteControlPanel();

        earlyLocationMap = new HashMap<>();
        numChained = 0;
        draggableMembers = new ArrayList<>();
        RaidCaching.deleteRaid(voiceChannel.getIdLong());
        this.isStarted = false;
        logRaid();
        logString = "";
        this.keyReacts = new ArrayList<>();
        this.raidLeaderEmotes = customization.RaidLeaderPrefsConnector.getRlEmotes(raidLeader.getId(), raidGuild.getId());
        try {
            voiceChannel.getName();
        } catch (Exception e) {voiceChannel = raidLeader.getVoiceState().getChannel();}

        Category category = voiceChannel.getParent();
        voiceChannel.getManager().putPermissionOverride(raiderRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_CONNECT)).submit().exceptionally(throwable -> {voiceChannel = raidLeader.getVoiceState().getChannel(); return null;});
        if (createdVc) voiceChannel.getManager().setPosition(category.getTextChannels().size() - 1).queue();
        if (delete) {
            raidMessage.delete().queue();
            controlPanel.delete().queue();
        }

        raidMessage = pingMessage();
        raidMessageId = raidMessage.getId();

        controlPanel.clearReactions().queue(null, new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, aVoid -> System.out.println("Failed to clear reactions on CP")));
        controlPanel = null;
        controlPanelId = null;
        raidMessage.editMessage(raidMessageEmbed().build()).queueAfter(5L, TimeUnit.SECONDS, message -> {
            openVoiceChannel();
            raidMessage = message;
            raidMessageId = raidMessage.getId();
            addRaidMessageEmotes();
            if (defaultRaid && !voiceChannel.getName().contains("Veteran")) {
                timeToStart = 300L;
                startMessageTimer();
            } else {
                raidMessage.editMessage("" + raidLeader.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "") + " has started a new "
                        + dungeonName + " raid in <#" + voiceChannel.getId() + ">, now located above the drag channel." + " This raid will begin when " + raidLeader.getEffectiveName() + " is ready.").queue();
                raidTimeManager.schedule(this::updateControlPanel, 6L, TimeUnit.SECONDS);
                timeToStart = -1L;
            }
        });

        createControlPanel();
        logPanel = new LogPanel(this);

        if (Database.isFungal(raidGuild)) new AssistLogger(raidCommandsChannel, raidLeader, assistReactions.stream().filter(m -> !m.equals(raidLeader)).collect(Collectors.toList()));

        try {
            RaidCaching.createRaid(raidGuild.getIdLong(), raidLeader.getIdLong(), voiceChannel.getIdLong(), raidCommandsChannel.getIdLong(),
                    raidStatusChannel.getIdLong(), location, raidType, raidMessage.getIdLong(), controlPanel.getIdLong(), false,
                    feedbackChannel.getIdLong(),feedbackMessage.getIdLong());
            logPanel.cache();
        } catch (Exception e) {
            RaidCaching.createRaid(raidGuild.getIdLong(), raidLeader.getIdLong(), voiceChannel.getIdLong(), raidCommandsChannel.getIdLong(),
                    raidStatusChannel.getIdLong(), location, raidType, raidMessage.getIdLong(), controlPanel.getIdLong(), false,
                    0L, 0L);
            logPanel.cache();
        }
    }

    public void transferRaid(Member member) {
        if (!defaultRaid) Database.addRlEventTime(raidLeader, (System.currentTimeMillis() - startingTime) / 1000);
        else Database.addRlQuota(raidLeader);
        raidLeader = member;

        String feedbackLogId = SetupConnector.getFieldValue(raidGuild, "guildLogs","feedbackLogChannelId");
        if (!feedbackLogId.equals("0")) raidTimeManager.schedule(() -> raidGuild.getTextChannelById(feedbackLogId).sendMessage(feedbackLogEmbed().build()).queue(), 115L, TimeUnit.SECONDS);

        String feedbackCategoryId = SetupConnector.getFieldValue(raidGuild, "guildLogs","feedbackCategoryId");
        if (!feedbackCategoryId.equals("0")) feedbackChannel.delete().queueAfter(2L, TimeUnit.MINUTES);

        createFeedback();

        voiceChannel.getManager().setName(raidLeader.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "") + "'s " + dungeonName + " Raid").queue();
        updateControlPanel();

        if (!Database.getGuildRaidCommandChannels(raidGuild.getId()).contains(raidCommandsChannel.getId())) {
            raidCommandsChannel.getManager().setName(raidLeader.getEffectiveName().split(" ")[0] + "-controls").queue();
        }

        if (isStarted) raidMessage.editMessage(raidStartEmbed().build()).queue();
        else raidMessage.editMessage(raidMessageEmbed().build()).queue();

        RaidCaching.updateFeedback(feedbackChannel, feedbackMessage, raidLeader);
    }

    /*
    Voice Channel Section
     */

    public void createRaidVoiceChannel() {
        createdVc = true;
        Category category = raidStatusChannel.getParent();
        voiceChannel = raidGuild.createVoiceChannel(raidLeader.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "") + "'s " + dungeonName + " Raid")
                .setParent(category)
                .setPosition(category.getTextChannels().size() - 1).complete();
        voiceChannel.getManager().sync().putPermissionOverride(raiderRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK))
                .putPermissionOverride(raidLeader, EnumSet.of(Permission.MANAGE_CHANNEL), null).queue();
    }

    public void openVoiceChannel() {
        Category category = voiceChannel.getParent();
        voiceChannel.getManager().putPermissionOverride(raiderRole, EnumSet.of(Permission.VOICE_CONNECT, Permission.VOICE_CONNECT, Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_SPEAK)).queue();
        if (createdVc) voiceChannel.getManager().setUserLimit(dungeonLimit).setPosition(category.getTextChannels().size() - 1).queue();
    }

    public void closeVoiceChannel() {
        voiceChannel.getManager().putPermissionOverride(raiderRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_CONNECT)).queue();
        if (createdVc) voiceChannel.getManager().setPosition(99).queue();
        List<User> dungeonReactions = raidMessage.retrieveReactionUsers(dungeonEmote).complete();
        if (Database.moveMembers(raidStatusChannel.getId())) {
            for (Member member : voiceChannel.getMembers()) {
                if (!dungeonReactions.contains(member.getUser())) {
                    if (!member.getPermissions(voiceChannel).contains(Permission.VOICE_SPEAK)) {
                        //Todo add drag channel
                        try {
                            raidGuild.kickVoiceMember(member).queue();
                        } catch (Exception e) {}
                        draggableMembers.add(member);
                    }
                }
            }
        }
    }

    public void clearVc() {
        controlPanel.removeReaction("♻", raidLeader.getUser()).queue();

        if (!Database.isFungal(raidGuild)) {
            VoiceChannel tempVc = voiceChannel.createCopy().setPosition(voiceChannel.getPosition()).complete();
            List<Member> members = voiceChannel.getMembers().stream().filter(member -> member.hasPermission(voiceChannel, Permission.VOICE_SPEAK)).collect(Collectors.toList());
            members.forEach(member -> {
                try {
                    raidGuild.moveVoiceMember(member, tempVc).complete();
                } catch (Exception e) {
                }
            });
            System.out.println(members.size());
            voiceChannel.delete().queueAfter(500 * members.size(), TimeUnit.MILLISECONDS);
            voiceChannel = tempVc;

            RaidCaching.updateVoiceChannel(voiceChannel.getIdLong(), raidLeader.getIdLong());
        } else {
            ClearVc.clearVc(raidCommandsChannel, voiceChannel);
        }
    }

    /*
    Raid Message Section
     */

    public Message pingMessage() {
        return raidStatusChannel.sendMessage((raidLeader.getId().equals(Config.get("INSTANCE_OWNER").toString()) ? "TEST RAID " : "@here ") + raidLeader.getEffectiveName() + " has started a new " + dungeonName + " raid in <#" + voiceChannel.getId() + ">, now located above the drag channel." +
                " The voice channel will open in `5 seconds.`").complete();
    }

    public EmbedBuilder raidMessageEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(raidLeader.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "") + "'s " + dungeonName + " Raid", null, raidLeader.getUser().getAvatarUrl())
                .setColor(raidColor);
        if (dungeonImage != null) embedBuilder.setThumbnail(dungeonImage);
        else embedBuilder.setThumbnail(dungeonEmote.getImageUrl());

        String debuffString = "\n\nPlease **select the debuffs your character has** by reacting with the following emotes » ";
        String raidDescription = "";
        if (raidType == -2 || this.dungeonName.contains("Alien")) {
            String keyEmotes = earlyLocEmotes.stream().map(emote -> emote.getAsMention()).collect(Collectors.joining());
            raidDescription += "**Click on** this channel, **<#" + voiceChannel.getId() + ">**, to join the raid.\n" +
                    "\nAfter you do so please **react with " + dungeonEmote.getAsMention() + "** so you do not get moved out." +
                    "\nIf you have any of the following keys, please react with " + keyEmotes +
                    " **OR** if you have multiple types of the listed keys please react with " + keyEmote.getAsMention() + " and check your dms." +
                    "\n\nPlease **indicate your class and gear choices** by reacting with the following emotes » ";
        } else {
            raidDescription += "**Click on** this channel, **<#" + voiceChannel.getId() + ">**, to join the raid." +
                    "\nAfter you do so please **react with " + dungeonEmote.getAsMention() + "** so you do not get moved out.\n" +
                    "\nIf you have a " + dungeonName + " key, please react with " + keyEmote.getAsMention() + " and check your dms." +
                    "\n\nPlease **indicate your class and gear choices** by reacting with the following emotes » ";
        }

        raidDescription += Arrays.asList(defaultEmotes).stream().filter(emote -> !additionalEmotes.contains(emote) && !earlyLocEmotes.contains(emote)).map(emote -> emote.getAsMention()).collect(Collectors.joining());

        if (!additionalEmotes.isEmpty()) debuffString += debuffEmotes.stream().filter(emote -> additionalEmotes.contains(emote)).map(emote -> emote.getAsMention()).collect(Collectors.joining());
        else debuffString += debuffEmotes.stream().map(emote -> emote.getAsMention()).collect(Collectors.joining());

        String finalDebuffString = debuffString;
        raidDescription += additionalEmotes.stream().filter(emote -> !finalDebuffString.contains(emote.getAsMention()) && !raidLeaderEmotes.contains(emote)).map(emote -> emote.getAsMention()).collect(Collectors.joining());
        raidDescription += raidLeaderEmotes.stream().filter(emote -> !finalDebuffString.contains(emote.getAsMention()) && !earlyLocEmotes.contains(emote)).map(emote -> emote.getAsMention()).collect(Collectors.joining());
        raidDescription += debuffString + "\n";

        String emoteString = "";
        for (Emote emote : earlyLocEmotes) {
            switch (emote.getId()) {
                case "768782625783283743":
                    raidDescription += "\nIf you intend to rush, please react with " + emote.getAsMention() + " and check your dms.";
                    break;
                case "822247326600134686":
                    raidDescription += "\nIf you intend to rush, please react with " + emote.getAsMention();
                    break;
                case "822247348084015114":
                    raidDescription += emote.getAsMention();
                    break;
                case "822247366463717417":
                    raidDescription += emote.getAsMention() + " and check your dms.";
                    break;
                default:
                    if (!emote.getName().toLowerCase().contains("key")) emoteString += emote.getAsMention();
                    break;
            }
        }

        if (!emoteString.isEmpty())
            raidDescription += "\n\nIf you are bringing any of the following " + emoteString + ", please react with the corresponding emote and check your dms.\n";

        raidDescription += "\nIf you are boosting this server or Goldilocks is in your server, react with " + nitro.getAsMention() + " to receive early location." + (earlyLoc ? " If you would like to get **free early location**" +
                " every 2 hours invite Goldilocks to your server [here](http://goldi.tech)!" : "");
        embedBuilder.setDescription(raidDescription);
        embedBuilder.setFooter("This raid will begin automatically or when the raid leader is ready");
        embedBuilder.setTimestamp(new Date().toInstant());
        return  embedBuilder;
    }

    public EmbedBuilder raidStartEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(raidLeader.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "") + "'s " + dungeonName + " Raid", null, raidLeader.getUser().getAvatarUrl())
                .setColor(raidColor);
        if (dungeonImage != null) embedBuilder.setThumbnail(dungeonStartImage);
        else embedBuilder.setThumbnail(dungeonEmote.getImageUrl());
        embedBuilder.setDescription("This afk check has already ended please wait until another leader begins a raid! If you were moved out" +
                " please ***join the drag channel*** to get automatically dragged back in! This raid has started with " + voiceChannel.getMembers().size() + " raiders!" +
                "\n" +
                (feedback ? "\nIf you would like to leave feedback for " + raidLeader.getAsMention() + " feel free to type it in " + feedbackChannel.getAsMention() + "!" : "") +
                (earlyLoc ? "\n\nIf you would like to get **free early location** every 2 hours invite Goldilocks to your server [here](http://goldi.tech)!" : ""));
        embedBuilder.setFooter((timeToStart == 0 ? "This raid was started automatically by " + raidGuild.getSelfMember().getEffectiveName() : "This raid was started by "
                + raidLeader.getEffectiveName()));
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    private EmbedBuilder queueEmbed(TextChannel queueChannel, String percentageBar, String percent) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(raidLeader.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "") + "'s " + dungeonName + " Raid", null, raidLeader.getUser().getAvatarUrl())
                .setColor(raidColor);
        if (dungeonImage != null) embedBuilder.setThumbnail(dungeonStartImage);
        else embedBuilder.setThumbnail(dungeonEmote.getImageUrl());
        embedBuilder.setDescription("This AFK check is currently dragging members from the queue located in " + queueChannel.getAsMention() + "\n **Dragging Members:** "
                + percentageBar + " | " + percent);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    public EmbedBuilder raidAbortEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(raidLeader.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "") + "'s " + dungeonName + " Raid", null, raidLeader.getUser().getAvatarUrl())
                .setColor(raidColor);
        if (dungeonImage != null) embedBuilder.setThumbnail(dungeonStartImage);
        else embedBuilder.setThumbnail(dungeonEmote.getImageUrl());
        embedBuilder.setDescription("This afk check has has been aborted by the raid leader. We are sorry for the inconvenience." +
                (feedback ? "\nIf you would like to leave feedback for " + raidLeader.getAsMention() + " feel free to type it in " + feedbackChannel.getAsMention() + "!" : "") +
                (earlyLoc ? "\n\nIf you would like to get **free early location** every 2 hours invite Goldilocks to your server [here](http://goldi.tech)!" : ""));
        embedBuilder.setFooter("This raid was aborted at: ");
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    public void addRaidMessageEmotes() {
        raidMessage.addReaction(dungeonEmote).queue();
        raidMessage.addReaction(keyEmote).queue();
        List<Emote> raidEarlyLocEmotes = earlyLocEmotes;

        List<String> defaultEmoteNames = raidEarlyLocEmotes.stream().map(emote -> emote.getName()).collect(Collectors.toList());

        for (Emote emote : earlyLocEmotes) raidMessage.addReaction(emote).queue();

        if (raidType == 4) raidMessage.addReaction(getEmote("822247366463717417")).queue();

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
            if (!defaultEmoteNames.contains(emote.getName())) {
                raidMessage.addReaction(emote).queue();
            }
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

        raidMessage.addReaction(nitro).queue();
    }

    /*
    Raid Ending and Logging Section
     */

    public void endRaid(Member raidMember) {
        System.out.println("Raid ending | Guild Info: " + raidGuild);
        if (!defaultRaid) {
            if (!Database.isShatters(raidGuild)) {
                memberTimeHashMap.forEach((member, aLong) -> Database.addEventTime(member, (System.currentTimeMillis() - aLong) / 1000));
                Database.addRlEventTime(raidLeader, (System.currentTimeMillis() - startingTime) / 1000);
            } else {
                int rlEvents = ((int) (System.currentTimeMillis() - startingTime) / 600000);
                memberTimeHashMap.forEach(((member, aLong) -> SqlConnector.logFieldForMember(member, Arrays.asList(new String[]{"eventruns"}), (int) (System.currentTimeMillis() - aLong) / 600000)));
                //ShattersConnector.logFieldForMember(raidLeader, Arrays.asList(new String[]{"eventruns", "currentweekEvents", "eventslead"}), rlEvents);
                logString = "Logging has been disabled. Please log via ViBot.";
                //logString = "Logged " + rlEvents + " Event" + (rlEvents != 1 ? "s" : "") + " for " + raidLeader.getEffectiveName() + " for a total of " + (Integer.parseInt(ShattersConnector.shattersStats(raidLeader.getUser())[8]) + rlEvents) + " this week.";
            }
        } else {
            logString = (numChained == 0 ? "1" : (numChained + 1)) + " Shatters " + (numChained == 0 ? "has" : "have")
                    + " been logged for " + raidLeader.getEffectiveName() + ". You have " + /*(Integer.parseInt(SqlConnector.shattersStats(raidLeader.getUser())[6]) + 1) +*/ " runs.";
        }
        updateControlPanel();
        RaidCaching.deleteRaid(voiceChannel.getIdLong());
        try {
            logRaid();
        } catch (Exception e) {}//Raid message may have been deleted causing it to not log
        try {
            controlPanel.clearReactions().complete();

        } catch (Exception e) {} //Control Panel may be null
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Control Panel for " + raidLeader.getEffectiveName() + "'s " + dungeonName + " Raid")
                .setColor(raidColor);

        //if (!scheduledFuture.isCancelled()) scheduledFuture.cancel(true);
        try {
            if (delete)  {
                raidMessage.delete().complete();
                controlPanel.editMessage(embedBuilder.setDescription("```\nThis raid was ended by " + raidMember.getEffectiveName() + "\n```")
                        .setFooter("This message will delete automatically").build()).complete().delete().submitAfter(15L, TimeUnit.SECONDS).exceptionally(throwable -> {return null;});
            }
        } catch (Exception e) {} //Try catch for potential deleted messages.
        logPanel.deleteControlPanel();

        if (createdVc) voiceChannel.delete().queue();
        else {
            if (!Database.isFungal(raidGuild)) {
                Message message = raidCommandsChannel.sendMessage("Clearing `" + voiceChannel.getName() + "`").complete();
                try {
                    int position = voiceChannel.getPosition();
                    VoiceChannel tempVc = voiceChannel.createCopy().setPosition(position).setUserlimit(50).complete();
                    System.out.println("Cleared VC: " + voiceChannel.getName());
                    voiceChannel.delete().queueAfter(0L, TimeUnit.MILLISECONDS, aVoid -> message.editMessage("`" + tempVc.getName() + "` Successfully cleared!").queue());
                    voiceChannel = tempVc;
                    if (voiceChannel.getPosition() != position) voiceChannel.getManager().setPosition(position).queue();
                } catch (Exception e) {
                    message.editMessage("Failed to clear VC").queue();
                }
            } else {
                new AssistLogger(raidCommandsChannel, raidLeader, assistReactions.stream().filter(m -> !m.equals(raidLeader)).collect(Collectors.toList()));
                ClearVc.clearVc(raidCommandsChannel, voiceChannel);
            }
        }
        if (raidControlCategory != null && !Database.getGuildRaidCommandChannels(raidGuild.getId()).contains(raidCommandsChannel.getId())) {
            raidCommandsChannel.delete().queueAfter(20L, TimeUnit.SECONDS);
            if (placeHolderMessage != null) placeHolderMessage.delete().queue();
        }

        String feedbackLogId = SetupConnector.getFieldValue(raidGuild, "guildLogs","feedbackLogChannelId");
        if (!feedbackLogId.equals("0")) raidTimeManager.schedule(() -> raidGuild.getTextChannelById(feedbackLogId).sendMessage(feedbackLogEmbed().build()).queue(), 115L, TimeUnit.SECONDS);

        if (feedback) feedbackChannel.delete().queueAfter(2L, TimeUnit.MINUTES);

    }

    public void abortRaid(Member raidMember) {
        System.out.println("Raid Aborting | Guild Info: " + raidGuild);

        draggableMembers = new ArrayList<>();

        RaidCaching.deleteRaid(voiceChannel.getIdLong());
        if (Database.isShatters(raidGuild) && isStarted) {
            SqlConnector.logFieldForMember(raidLeader, Arrays.asList(new String[]{"currentweekfailed","failruns"}), 1);
            logString = "Logged 1 failed run for " + raidLeader.getEffectiveName() + " for a total of " + (Integer.parseInt(SqlConnector.shattersStats(raidLeader.getUser())[9]) + 1) + " this week.";
            updateControlPanel();
        }
        controlPanel.clearReactions().submit().exceptionally(throwable -> {return null;});

        //if (!scheduledFuture.isCancelled()) scheduledFuture.cancel(true);
        try {
            if (delete)  {
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Control Panel for " + raidLeader.getEffectiveName() + "'s " + dungeonName + " Raid")
                        .setColor(raidColor);

                raidMessage.delete().complete();
                controlPanel.editMessage(embedBuilder.setDescription("```\nThis raid was aborted by " + raidMember.getEffectiveName() + "\n```")
                        .setFooter("This message will delete automatically").build()).complete().delete().submitAfter(15L, TimeUnit.SECONDS).exceptionally(throwable -> {return null;});
            } else {
                raidMessage.editMessage(raidAbortEmbed().build()).queue();
            }
        } catch (Exception e) { } //Try catch for potential deleted messages.

        logPanel.deleteControlPanel();
        if (createdVc) voiceChannel.delete().queue();

        String feedbackLogId =  SetupConnector.getFieldValue(raidGuild, "guildLogs","feedbackLogChannelId");
        if (!feedbackLogId.equals("0")) raidTimeManager.schedule(() -> raidGuild.getTextChannelById(feedbackLogId).sendMessage(feedbackLogEmbed().build()).queue(), 115L, TimeUnit.SECONDS);
        if (feedback) feedbackChannel.delete().queueAfter(2L, TimeUnit.MINUTES);
        if (raidControlCategory != null && !Database.getGuildRaidCommandChannels(raidGuild.getId()).contains(raidCommandsChannel.getId())) {
            raidCommandsChannel.delete().queueAfter(15L, TimeUnit.SECONDS);
            if (placeHolderMessage != null) placeHolderMessage.delete().queue();
        };

    }

    public void logRaid() {
        Goldilocks.TIMER.schedule(() -> SqlConnector.logRaid(this), 0L, TimeUnit.SECONDS);

        GoogleSheets.logEvent(raidGuild, GoogleSheets.SheetsLogType.RAIDS, raidLeader.getEffectiveName(), raidLeader.getId(), dungeonName, raidType + "");

        if (Database.isShatters(raidGuild)) {
            if (!defaultRaid) {
                int rlEvents = ((int) (System.currentTimeMillis() - startingTime) / 600000);
                //logString = "Logged " + rlEvents + " Event" + (rlEvents != 1 ? "s" : "") + " for " + raidLeader.getEffectiveName() + " for a total of " + (Integer.parseInt(ShattersConnector.shattersStats(raidLeader.getUser())[8]) + rlEvents) + " this week.";
            } else {
                logString = (numChained == 0 ? "1" : (numChained + 1)) + " Shatters " + (numChained == 0 ? "has" : "have")
                        + " been logged for " + raidLeader.getEffectiveName() + ". You have " + (Integer.parseInt(SqlConnector.shattersStats(raidLeader.getUser())[6]) + 1) + " runs.";
            }
            updateControlPanel();
        }
        if (quota) QuotaManager.updateQuotaMessage(raidGuild);
        //Todo Shatts connector
        Database.logEvent(raidLeader, Database.EventType.RAID, System.currentTimeMillis() / 1000, raidCommandsChannel, "raid");

        if (defaultRaid) {
            assistReactions = assistReactions.stream().filter(member -> voiceChannel.getMembers().contains(member) && !member.equals(raidLeader)).distinct().collect(Collectors.toList());
            if (!Database.isShatters(raidGuild)) {
                Database.addPlayerRuns(voiceChannel.getMembers());
                if (!Database.isFungal(raidGuild)) {
                    Database.addAssists(assistReactions);
                    assistReactions.forEach(a ->
                            GoogleSheets.logEvent(raidGuild, GoogleSheets.SheetsLogType.ASSISTS, a.getEffectiveName(), a.getId(), raidLeader.getEffectiveName(), raidLeader.getId()));
                    assistReactions.forEach(member -> Database.logEvent(member, Database.EventType.ASSIST, System.currentTimeMillis() / 1000, raidCommandsChannel, "assist"));
                }
                Database.addRlQuota(raidLeader);
            }
            else {
                List<Member> members = raidMessage.retrieveReactionUsers(dungeonEmote).complete().stream().map(user -> raidGuild.getMember(user)).collect(Collectors.toList());
                if (!members.isEmpty()) SqlConnector.logFieldForMembers(members, Arrays.asList(new String[]{"runs"}), 1);
                //if (!assistReactions.isEmpty()) ShattersConnector.logFieldForMembers(assistReactions, Arrays.asList(new String[]{"currentweekassists","assists"}), 1);
                SqlConnector.logFieldForMember(raidLeader, Arrays.asList(new String[]{"successruns","currentweek"}), 1);
            }
            //Check the voice channel to make sure that the people who reacted to assist are actually in the run.
        }

        String raidLogId = SetupConnector.getFieldValue(raidGuild, "guildLogs","raidLogChannelId");
        if (!raidLogId.equals("0")) {
            logMessage = raidGuild.getTextChannelById(raidLogId).sendMessage(logRaidEmbed().build()).complete();
        }
    }

    public EmbedBuilder logRaidEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(raidLeader.getEffectiveName() + " completed a raid!");
        String playerNames = "";
        List<Member> members = voiceChannel.getMembers();
        if (Database.isShatters(raidGuild)) members = raidMessage.retrieveReactionUsers(dungeonEmote).complete().stream().map(user -> raidGuild.getMember(user)).collect(Collectors.toList());
        for (Member member : members){
            try {
                playerNames += ", " + member.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "");
            } catch (Exception e) { }

        }
        playerNames = playerNames.replaceFirst(", ", "");
        embedBuilder.setDescription("Players in this raid (" + members.size() + ") : " + playerNames);
        String raidMembers = "";
        List<Member> raidMembersList = members;
        for (Member member : raidMembersList) {
            try {
                if (!member.getUser().isBot()) {
                    raidMembers += member.getAsMention() + " ";
                    if ((raidMembers.length() + raidLeader.getAsMention().length()) >= 950){
                        raidMembers += "And " + (raidMembersList.size() - raidMembersList.indexOf(member) - 1) + " others...";
                        break;
                    }
                }
            } catch (Exception e) { }
        }
        embedBuilder.addField("Raid Member Tags: ", raidMembers.isEmpty() ? "None" : raidMembers, false);
        String keyReacts = getMemberReactionString(keyEmote, raidMessage);
        embedBuilder.addField( dungeonName + " key reacts", keyReacts.isEmpty() ? "None" : keyReacts, true);
        for (Emote emote : earlyLocEmotes) {
            if (!emote.equals(keyEmote)) {
                String emoteReacts = getMemberReactionString(emote, raidMessage);
                embedBuilder.addField(StringUtils.capitalize(emote.getName()) + " reacts:", emoteReacts.isEmpty() ? "None" : emoteReacts, true);
            }
        }
        String nitroReacts = getMemberReactionString(nitro, raidMessage);
        embedBuilder.addField("Nitro reacts: ", nitroReacts.isEmpty() ? "None" : nitroReacts, true);
        if (controlPanel != null) {
            String assistReacts = "";
            for (Member member : assistReactions.stream().distinct().collect(Collectors.toList())) {
                assistReacts += member.getAsMention() + "\n";
                if ((raidMembers.length() + raidLeader.getAsMention().length()) >= 1024){
                    assistReacts += "And " + (raidMembersList.size() - raidMembersList.indexOf(member) - 1) + " others...";
                    break;
                }
            }
            embedBuilder.addField("People Assisting This Run: ", assistReacts.isEmpty() ? "None" : assistReacts, false);
        };
        embedBuilder.setColor(raidColor);
        embedBuilder.setThumbnail(dungeonEmote.getImageUrl());
        embedBuilder.setFooter("This raid lasted a total of " + getTimeString((System.currentTimeMillis() - timeStarted) / 1000));
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    public EmbedBuilder feedbackLogEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(raidLeader.getEffectiveName() + "'s Feedback Log")
                .setColor(Goldilocks.GOLD)
                .setThumbnail(raidLeader.getUser().getAvatarUrl());
        int positiveVotes = feedbackMessage.retrieveReactionUsers("👍").complete().size() - 1;
        int negativeVotes = feedbackMessage.retrieveReactionUsers("👎").complete().size() - 1;
        final String[] comments = {""};
        HashMap<Member, String> commentMap = new HashMap<>();
        feedbackChannel.getHistory().retrievePast(100).complete().stream().filter(message -> !message.getAuthor().isBot())
                .forEach(message -> {
                    if (!commentMap.containsKey(message.getMember()) || commentMap.get(message.getMember()).length() < message.getContentRaw().length()) {
                        commentMap.put(message.getMember(), message.getContentRaw());
                    }
                });
        commentMap.forEach((member, s) -> {
            if (comments[0].length() + s.length() < 1950) {
                comments[0] += "\n" + member.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "") + " commented:\n" + s.replaceAll("[^A-Za-z0-9- ]", "") + "\n";
            }
        });
        embedBuilder.setDescription("**__Votes:__**:\n```css\nPositive Votes: " + positiveVotes + "  Negative Votes: " + negativeVotes + "\n```\n**__Comments:__**" + (comments[0].isEmpty()
                ? "```yaml\nNo comments were left\n```" : "```yaml\n" + comments[0] + "\n```"));
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    /*
    Control Section
     */

    private void createControls() {
        if (raidControlCategory != null) createControlChannel();
        else createControlPanel();
    }

    public void createControlPanel() {
        controlPanel = raidCommandsChannel.sendMessage(controlPanelEmbed().build()).complete();
        controlPanelId = controlPanel.getId();
        controlPanel.addReaction("🗺").queue();
        controlPanel.addReaction(assist).queue();
        controlPanel.addReaction("▶").queueAfter(5L, TimeUnit.SECONDS);

    }

    private void createControlChannel() {
        raidControlChannel = raidControlCategory.createTextChannel(raidLeader.getEffectiveName().split(" ")[0] + "-controls")
                .complete();
        raidControlChannel.getManager().sync()
                .putPermissionOverride(raidLeader, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY), null)
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

    public void updateControlPanel() {
        controlPanel.editMessage(controlPanelEmbed().build()).complete();
    }

    public EmbedBuilder controlPanelEmbed() {
        String chainString = numChained == 0 ? "" : "**Total Runs:**\nYou have chained `" + numChained + " run" + (numChained == 1 ? "" : "s") + "` in addition to the first run\n";
        String commandDescription = chainString + "**Controls: **" +
                (!isStarted ? "\nReact with 🗺 to change the location" : "") +
                "\nIf you are assisting react with " + assist.getAsMention();
        try {
            controlPanel.retrieveReactionUsers("▶").complete();
            commandDescription += "\nReact with ▶ to end your afk check" +
                    (!isStarted ? "\nType `.abort` to abort this run" : "");
        } catch (Exception e) { }
        commandDescription += (isStarted ? "\nIf you are chaining, react with 📥 to add a run" +
                "\nReact with 🆕 to begin a new raid" +
                "\nReact with ♻ to clear out the voice channel" +
                "\nReact with ❌ to **log** this raid and close the channel" +
                (Database.isShatters(raidGuild) ? "\nTo log this run as a fail type `.fail`": "") : "");

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Control Panel for " + raidLeader.getEffectiveName() + "'s " + dungeonName + " Raid")
                .setColor(raidColor)
                .setDescription(commandDescription);
        embedBuilder.addField("Location", location.isEmpty() ? "`Please set a location!`" : "`" + location + "`",false);

        float vcPercentage = (float) voiceChannel.getMembers().size() / voiceChannel.getUserLimit();
        if (defaultRaid) embedBuilder.addField("Raiders Accounted For (" + voiceChannel.getMembers().size() + "/" + voiceChannel.getUserLimit() +"):", "" + Utils.renderPercentage(vcPercentage, 10) + " **" + String.format("%.2f", vcPercentage * 100) + "%**", false);

        final String[] additionalKeyReacts = {""};
        if (dungeonName.equals("Random Epic Dungeons") || dungeonName.equals("Random Court Dungeons") || dungeonName.contains("Alien") || raidType == -2) {
            earlyLocationMap.forEach(((user, emote) -> {
                additionalKeyReacts[0] += ", " + raidGuild.getMember(user).getAsMention() + "[" + emote.getAsMention() + "]";
            }));
            additionalKeyReacts[0] = StringUtils.replaceOnce(additionalKeyReacts[0], ", ", "");
            embedBuilder.addField("Key Reactions: ", additionalKeyReacts[0].isEmpty() ? "None" : additionalKeyReacts[0], false);
        } else {
            String keyReacts = "";
            for (Member member : this.keyReacts) keyReacts += ", " + member.getAsMention();
            keyReacts = StringUtils.replaceOnce(keyReacts, ", ", "");

            if (raidType != 0) embedBuilder.addField( "Key reacts", keyReacts.isEmpty() ? "None" : keyReacts, false);
            else embedBuilder.addField( "Incantation reacts", keyReacts.isEmpty() ? "None" : keyReacts, false);

            for (Emote emote : earlyLocEmotes) {
                if (!emote.equals(keyEmote)) {
                    final String[] currentString = {""};
                    earlyLocationMap.forEach(((user, emote1) -> {
                        if (emote1 == emote) {
                            currentString[0] += ", " + raidGuild.getMember(user).getAsMention();
                        }
                    }));
                    currentString[0] = StringUtils.replaceOnce(currentString[0], ", ", "");
                    final String[] displayName = {""};
                    String s = emote.getName().replace("of", "Of").replace("the", "The");
                    Arrays.stream(s.split("(?=\\p{Upper})")).forEach(s1 -> displayName[0] += s1 + " ");
                    embedBuilder.addField(StringUtils.capitalize(displayName[0]).trim() + " reacts:", currentString[0].isEmpty() ? "None" : currentString[0], false);
                }
            }
        }

        String nitroReacts = getMemberReactionString(nitro, raidMessage);

        embedBuilder.addField("Nitro reacts: ", nitroReacts.isEmpty() ? "None" : nitroReacts, false);

        final String[] potentialFakeReactions = {""};
        earlyLocationMap.forEach(((user, emote) -> {
            List<User> reactionUsers = raidMessage.retrieveReactionUsers(emote).complete();
            if (!reactionUsers.contains(user)) {
                try {
                    potentialFakeReactions[0] += ", " + raidGuild.getMember(user).getAsMention() + "[" + emote.getAsMention() + "]";
                } catch (Exception e) {} //User may have left the server
            }
        }));
        potentialFakeReactions[0] = StringUtils.replaceOnce(potentialFakeReactions[0], ", ", "");
        if (!potentialFakeReactions[0].isEmpty()) embedBuilder.addField("Potential Fake Reactions: ", potentialFakeReactions[0], false);

        if (controlPanel != null) {
            String assistReacts = getMemberReactionString(assist, controlPanel);
            embedBuilder.addField("People Assisting This Run: ", assistReacts.isEmpty() ? "None" : assistReacts, false);
        };
        if (timeToStart == 300L) {
            embedBuilder.setFooter("Your voice channel will open in 5 seconds.");
        } else if (isStarted) {
            embedBuilder.setFooter("Run added by " + raidLeader.getEffectiveName());
        } else if (timeToStart == -1) {
            embedBuilder.setFooter("This afk check will end when " + raidLeader.getEffectiveName() + " is ready");
        } else {
            embedBuilder.setFooter("Time until your afk check automatically ends: " + getTimeString(timeToStart));
        }

        if (Database.isShatters(raidGuild)) embedBuilder.addField("Logs", "```\n" + (numChained == 0 && logString.isEmpty() ? "No runs have been logged." : logString) + "\n```", false);
        return  embedBuilder;
    }

    /*
    Feedback Section
     */

    public void createFeedback() {
        Category feedbackCategory = null;

        String feedbackCategoryId = SetupConnector.getFieldValue(raidGuild, "guildLogs","feedbackCategoryId");
        if (!feedbackCategoryId.equals("0")) feedbackCategory = raidGuild.getCategoryById(feedbackCategoryId);

        if (feedbackCategory == null)
            return;

        feedbackChannel = feedbackCategory.createTextChannel(raidLeader.getEffectiveName().replace("|", ",").split(",")[0] + "-feedback")
                .complete();
        feedbackChannel.getManager().sync().complete();
        feedbackChannel.putPermissionOverride(raidGuild.getPublicRole()).setDeny(Permission.VIEW_CHANNEL).complete();
        feedbackChannel.putPermissionOverride(raiderRole).setDeny(Permission.VIEW_CHANNEL).complete();
        feedbackMessage = feedbackChannel.sendMessage(feedbackEmbed().build()).complete();
        feedbackMessage.addReaction("👍").queue();
        feedbackMessage.addReaction("👎").queue();
    }

    public EmbedBuilder feedbackEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(raidLeader.getEffectiveName() + " feedback!");
        embedBuilder.setThumbnail(raidLeader.getUser().getAvatarUrl());
        embedBuilder.setColor(Goldilocks.GOLD);
        embedBuilder.setDescription("**__Instructions__**\n```css\nPlease react with a 👍 or 👎, after you do so, leave a comment below!\n\n" +
                "You are allotted up to 2 messages per raid to prevent spam.\n```\n**Please consider:**\n Raid leaders often read this feedback after their raid so please keep it genuine, if you are " +
                "caught continually clogging the feedback channel with spam-like comments your ability to comment on future raids will be revoked.");
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    /*
    Util Section
     */

    public void startMessageTimer() {
        Runnable messageTimeUpdate = () -> {
            try {
                raidMessage.editMessage("" + raidLeader.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "") + " has started a new "
                        + dungeonName + " raid in <#" + voiceChannel.getId() + ">, now located above the drag channel." + " Time until this raid begins: `" + getTimeString(timeToStart)  + "`").complete();
            } catch (Exception e) {
                System.out.println("Skipped time interval " + timeToStart);
            }
            controlPanel.editMessage(controlPanelEmbed().setFooter("Time until your afk check automatically ends: " + getTimeString(timeToStart)).build()).queue();
            timeToStart = timeToStart - 5;
            if (timeToStart == 0) {
                startRaid();
                scheduledFuture.cancel(true);
            }
        };
        scheduledFuture = raidTimeManager.scheduleWithFixedDelay(messageTimeUpdate, 0L, 5L, TimeUnit.SECONDS);
    }

    public String getTimeString(long time) {
        String timeString = "";
        if (time >= 60) timeString += (time / 60) + " minutes and ";
        timeString += time % 60 + " seconds";
        return timeString;
    }

    public String getMemberReactionString(Emote emote, Message message) {
        List<User> reactions = message.retrieveReactionUsers(emote).complete();
        String reacts = reactions.stream().filter(user -> !user.isBot()).map(user -> user.getAsMention()).collect(Collectors.joining(", "));
        return reacts;
    }

    public void getDungeonInfo() {
        this.dungeonEmote = getEmote(dungeonInfo[0]);
        this.keyEmote = getEmote(dungeonInfo[1]);
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
                    Emote emote = getEmote(emoteId.trim());;
                    earlyLocEmotes.add(emote);
                    emoteLimits.put(emote, 5);
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

        this.raidColor = new Color(Integer.parseInt(dungeonInfo[5]));
        if (customization.RaidLeaderPrefsConnector.getRlColor(raidLeader.getId(), raidGuild.getId()) != null)
            this.raidColor = customization.RaidLeaderPrefsConnector.getRlColor(raidLeader.getId(), raidGuild.getId());

        this.dungeonLimit = customCap == -1 ? (dungeonInfo[6].isEmpty() ? 50 : Integer.parseInt(dungeonInfo[6])) : customCap;
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
    }

    public Emote getEmote(String emoteId) {
        return Goldilocks.jda.getEmoteById(emoteId);
    }

    public void addToAssistReacts(Member member) {
        if (!assistReactions.contains(member)) {
            assistReactions.add(member);
        }
    }

    public void addKeyReact(Member member) {
        keyReacts.add(member);
    }

    public void addEarlyLocReact(User user, Emote emote) {
        earlyLocationMap.put(user, emote);
    }

    public boolean isStarted() {
        return isStarted;
    }

    public void promptForLocation(Member member) {
        if (!isStarted) {
            String raidLocations = "";
            for (Raid raid : RaidHub.activeRaids) {
                if (!raid.equals(this) && raid.raidGuild.equals(this.raidGuild)) {
                    raidLocations += "`" + String.format("%-12s", raid.getRaidLeader().getEffectiveName().split(" ")[0]) + ":` " + raid.getLocation() + "\n";
                }
            }

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
                location = e.getMessage().getContentRaw();
                earlyLocationMap.forEach(((user, emote) -> {
                    sendLocationUpdate(user);
                }));
                for (User user : raidMessage.retrieveReactionUsers(nitro).complete()) {
                    if (!user.isBot() && !earlyLocationMap.containsKey(user)) {
                        sendLocationUpdate(user);
                    }
                }
                locationPrompt.delete().queue();
                e.getMessage().delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
                updateControlPanel();
            }, 2L, TimeUnit.MINUTES, () -> {
                Utils.errorMessage("Location change failed", "Raid leader did not enter a location", locationPrompt, 15L);
            });
        } else {
            Utils.errorMessage("Location change failed", "You cannot change the raid location after it has started.", raidCommandsChannel, 5L);
        }

    }

    public void sendLocationUpdate(User user) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Location Change for " + raidLeader.getEffectiveName() + "'s Raid");
        embedBuilder.setColor(raidColor);
        embedBuilder.setDescription("The new location for this raid is: `" + location + "`");
        embedBuilder.setFooter("The location was updated by " + raidLeader.getEffectiveName());

        user.openPrivateChannel().complete().sendMessage(embedBuilder.build()).queue();

    }
}
