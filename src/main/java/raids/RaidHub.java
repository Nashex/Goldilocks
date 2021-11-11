package raids;

import main.Database;
import main.Goldilocks;
import main.Permissions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdateUserLimitEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import raids.endgame.EndGameRaid;
import setup.SetupConnector;
import shatters.SqlConnector;
import utils.Utils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static main.Goldilocks.eventWaiter;

public class RaidHub extends ListenerAdapter {

    public static ExecutorService RAID_POOL = Executors.newFixedThreadPool(4);
    public static List<Raid> activeRaids = new ArrayList<>();
    public static List<EndGameRaid> activeEndGameRaids = new ArrayList<>();
    public static List<HeadCount> activeHeadcounts = new ArrayList<>();
    public static List<LogPanel> activeLoggingPanels = new ArrayList<>();

    public static void createNewRaid(Member raidLeader, String location ,TextChannel textChannel, int customCap, VoiceChannel voiceChannel) {
        String raidStatusChannelId = Database.getRaidStatusChannel(textChannel.getId());
        TextChannel raidStatusChannel = Goldilocks.jda.getTextChannelById(raidStatusChannelId);
        int dungeonType = Database.getSectionDungeon(textChannel.getId());

        if (dungeonType == -1) {
            dungeonSelection(raidLeader, textChannel, raidStatusChannel, location, customCap, voiceChannel);
        } else if (dungeonType == 5)  {
            defaultDungeonSelection(raidLeader, textChannel, raidStatusChannel, location, customCap, voiceChannel);
        } else {
            activeRaids.add(new Raid(raidLeader, dungeonType ,raidStatusChannel, textChannel, location, false, customCap, voiceChannel));
        }
    }

    public static void createNewEndgameRaid(Member raidLeader, String location, TextChannel textChannel) {
        String raidStatusChannelId = Database.getRaidStatusChannel(textChannel.getId());
        TextChannel raidStatusChannel = Goldilocks.jda.getTextChannelById(raidStatusChannelId);
        new EndGameRaid(raidLeader, 0, raidStatusChannel, textChannel, location);
    }

    public static void createEventRaid(Member raidLeader, String location ,TextChannel textChannel, int customCap, VoiceChannel voiceChannel) {

        String raidStatusChannelId = Database.getRaidStatusChannel(textChannel.getId());
        TextChannel raidStatusChannel = Goldilocks.jda.getTextChannelById(raidStatusChannelId);
        dungeonSelection(raidLeader, textChannel, raidStatusChannel, location, customCap, voiceChannel);

    }

    public static void createNewHeadcount(Member raidLeader, TextChannel raidCommands) {

        String raidStatusChannelId = Database.getRaidStatusChannel(raidCommands.getId());
        TextChannel raidStatusChannel = Goldilocks.jda.getTextChannelById(raidStatusChannelId);
        int dungeonType = Database.getSectionDungeon(raidCommands.getId());

        if (dungeonType == -1) {
            dungeonSelection(raidLeader, raidCommands, raidStatusChannel);
        } else {
            activeHeadcounts.add(new HeadCount(raidLeader, raidCommands, raidStatusChannel, dungeonType));
        }

    }

    public static void createEventHeadcount(Member raidLeader, TextChannel raidCommands) {
        String raidStatusChannelId = Database.getRaidStatusChannel(raidCommands.getId());
        TextChannel raidStatusChannel = Goldilocks.jda.getTextChannelById(raidStatusChannelId);

        dungeonSelection(raidLeader, raidCommands, raidStatusChannel);

    }

    public static void changeRaidType(Member raidLeader, TextChannel textChannel, Raid raid) {
        dungeonSelectionChangeRaid(raidLeader, textChannel, raid);
    }

    public static void endRaid(Raid raid, Member member) {
        activeRaids.remove(raid);
        RAID_POOL.execute(() -> raid.endRaid(member));
    }

    public static void endRaid(Raid raid) {
        activeRaids.remove(raid);
        RAID_POOL.execute(() -> raid.endRaid(raid.getRaidLeader()));
    }

    public static Raid getRaid(Member member) {
        for (Raid raid : activeRaids) {
            if (raid.getRaidLeader().equals(member)) {
                return raid;
            }
        }
        return null;
    }

    public static Raid getRaid(String messageId) {
        for (Raid raid : activeRaids) {
            try {
                if (raid.getRaidMessage().getId().equals(messageId) || raid.getControlPanel().getId().equals(messageId)) {
                    return raid;
                }
            } catch (Exception e) {}
        }
        return null;
    }

    public static Raid getRaidById(String messageId) {
        for (Raid raid : activeRaids) {
            if (raid.getRaidMessageId().equals(messageId) || raid.getControlPanelId().equals(messageId)) {
                return raid;
            }
        }
        return null;
    }

    public static EndGameRaid getEndGameRaid(String messageId) {
        for (EndGameRaid endGameRaid : activeEndGameRaids) {
            if (endGameRaid.raidMessage.getId().equals(messageId)) {
                return endGameRaid;
            }
        }
        return null;
    }

    public static Raid getRaid(VoiceChannel voiceChannel) {
        for (Raid raid : activeRaids) {
            if (raid.getVoiceChannel().equals(voiceChannel)) {
                return raid;
            }
        }
        return null;
    }

    public static HeadCount getHeadCount(Member member) {
        for (HeadCount headCount : activeHeadcounts) {
            if (headCount.getRaidLeader().equals(member)) {
                return headCount;
            }
        }
        return null;
    }

    public static HeadCount getHeadCount(String messageId) {
        for (HeadCount headCount : activeHeadcounts) {
            if (headCount.getHeadCountMessage().getId().equals(messageId) || headCount.getControlPanel().getId().equals(messageId)) {
                return headCount;
            }
        }
        return null;
    }

    public static HeadCount getHeadCountById(String messageId) {
        for (HeadCount headCount : activeHeadcounts) {
            if (headCount.getHeadCountMessageId().equals(messageId) || headCount.getControlPanelId().equals(messageId)) {
                return headCount;
            }
        }
        return null;
    }

    public static void endHeadCount(Member member) {
        HeadCount headCount = getHeadCount(member);
        activeHeadcounts.remove(headCount);
        headCount.deleteHeadCount();
    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        Member member = event.getMember();
        for (Raid raid : activeRaids) {
            try {
                if (raid.getFeedbackChannel().equals(event.getChannel())) {
                    if (raid.feedbackList.contains(member)) {
                        raid.getFeedbackChannel().upsertPermissionOverride(member).setAllow(Permission.MESSAGE_READ).setDeny(Permission.MESSAGE_WRITE).queue();
                    } else {
                        raid.feedbackList.add(member);
                    }
                }
            } catch (Exception e) {}
        }
    }

    @Override
    public void onVoiceChannelUpdateUserLimit(@Nonnull VoiceChannelUpdateUserLimitEvent event) {
        Guild guild = event.getGuild();
        VoiceChannel voiceChannel = event.getChannel();

        Raid raid = getRaid(voiceChannel);
        if (raid != null) {
            raid.setDungeonLimit(voiceChannel.getUserLimit());
        }
    }

    @Override
    public void onGuildVoiceMove(@Nonnull GuildVoiceMoveEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();

        for (Raid raid : activeRaids) {
            if (event.getChannelJoined().equals(raid.getVoiceChannel())) {
                RAID_POOL.execute(() -> {
                    if (raid.isFeedback()) raid.getFeedbackChannel().upsertPermissionOverride(member).setAllow(Permission.VIEW_CHANNEL, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE).queue();
                });
                raid.memberTimeHashMap.put(member, System.currentTimeMillis());
            } else if (event.getChannelLeft().equals(raid.getVoiceChannel())) {
                if (!raid.isDefaultRaid()) {
                    try {
                        if (!Database.isShatters(guild)) Database.addEventTime(member, (System.currentTimeMillis() - raid.memberTimeHashMap.get(member)) / 1000);
                        else SqlConnector.logFieldForMember(member, Arrays.asList(new String[]{"eventruns"}), (int) (System.currentTimeMillis() - raid.memberTimeHashMap.get(member)) / 600000);
                        raid.memberTimeHashMap.remove(member);
                    } catch (Exception e) {
                        if (!Database.isShatters(guild)) Database.addEventTime(member, (System.currentTimeMillis() - raid.getStartingTime()) / 1000);
                        else SqlConnector.logFieldForMember(member, Arrays.asList(new String[]{"eventruns"}), (int) (System.currentTimeMillis() - raid.getStartingTime()) / 600000);
                    }
                }
            }
        }
    }

    @Override
    public void onGuildVoiceJoin(@Nonnull GuildVoiceJoinEvent event) {

        Guild guild = event.getGuild();
        Member member = event.getMember();

        for (Raid raid : activeRaids) {
            if (raid.getDraggableMembers().contains(member)) {
                try {
                    guild.moveVoiceMember(member, raid.getVoiceChannel()).queue();
                } catch (Exception e) {}
                raid.getDraggableMembers().remove(member);
            }
            if (event.getChannelJoined().equals(raid.getVoiceChannel())) {
                if (!raid.isDefaultRaid()) {
                    raid.memberTimeHashMap.put(member, System.currentTimeMillis());
                }
                try {
                    raid.getFeedbackChannel().upsertPermissionOverride(member).setAllow(Permission.VIEW_CHANNEL, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE).queue();
                } catch (Exception e) {}
            }
        }

    }

    @Override
    public void onGuildVoiceLeave(@Nonnull GuildVoiceLeaveEvent event) {
        Member member = event.getMember();

        for (Raid raid : activeRaids) {
            if (event.getChannelLeft().equals(raid.getVoiceChannel())) {
                try {
                    raid.getFeedbackChannel().upsertPermissionOverride(member).setDeny(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE).queueAfter(2L, TimeUnit.MINUTES, null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_CHANNEL));
                } catch (Exception e) { }
                if (!raid.isDefaultRaid()) {
                    try {
                        if (!Database.isShatters(event.getGuild())) Database.addEventTime(member, (System.currentTimeMillis() - raid.memberTimeHashMap.get(member)) / 1000);
                        else SqlConnector.logFieldForMember(member, Arrays.asList(new String[]{"eventruns"}), (int) (System.currentTimeMillis() - raid.memberTimeHashMap.get(member)) / 600000);
                        raid.memberTimeHashMap.remove(member);
                    } catch (Exception e) {
                        if (!Database.isShatters(event.getGuild())) Database.addEventTime(member, (System.currentTimeMillis() - raid.getStartingTime()) / 1000);
                        else SqlConnector.logFieldForMember(member, Arrays.asList(new String[]{"eventruns"}), (int) (System.currentTimeMillis() - raid.getStartingTime()) / 600000);
                    }
                }
            }
        }
    }

    @Override
    public void onGuildMessageReactionAdd(@Nonnull GuildMessageReactionAddEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        User user = event.getUser();

        if (user.isBot()) {
            return;
        }

        if (event.getReactionEmote().isEmote()) {
            Emote emote = event.getReactionEmote().getEmote();
            Raid raid = getRaid(event.getMessageId());
            HeadCount headCount = getHeadCount(event.getMessageId());
            if (raid != null) {
                List<Emote> earlyLocEmotes = raid.getEarlyLocEmotes();
                //earlyLocEmotes.add();
                Emote assist = Goldilocks.jda.getEmoteById("735943510473310309");
                if (earlyLocEmotes.contains(emote) || emote.equals(raid.getKeyEmote())) {
                    if (!raid.getEarlyLocationMap().containsKey(user) && !raid.isStarted()) {
                        if (raid.getEarlyLocationMap().values().stream().filter(emote1 -> emote1.equals(emote)).count() > (raid.getRaidType() == 0 ? 2 : 4) && (!emote.equals(raid.getKeyEmote()) || raid.getRaidType() == 0)) {
                            user.openPrivateChannel().complete().sendMessage("We have reached the maximum amount of reacts for " + emote.getAsMention() + ".").queue();
                            return;
                        }
                        //Todo added emote limits
                        if (raid.getEmoteLimits().containsKey(emote) && raid.getEarlyLocationMap().values().stream().filter(emote1 -> emote1.equals(emote)).count() > raid.getEmoteLimits().get(emote)) {
                            user.openPrivateChannel().complete().sendMessage("We have reached the maximum amount of reacts for " + emote.getAsMention() + ".").queue();
                            return;
                        }
                        if (!raid.getVoiceChannel().getMembers().contains(member) && (!emote.equals(raid.getKeyEmote()) || raid.getRaidType() == 0)) {
                            user.openPrivateChannel().complete().sendMessage("Please make sure that you join VC prior to reacting with " + emote.getAsMention() + ".").queue();
                            event.getReaction().removeReaction(user).queue();
                            return;
                        }
                        if (Database.isShatters(guild)) {
                            if (emote.getName().toLowerCase().contains("mystic") && member.getRoles().stream().noneMatch(role -> role.getName().toLowerCase().contains("stasis"))) return;
                            if ((emote.getName().toLowerCase().equals("switch1") || (emote.getName().toLowerCase().equals("switch2"))) && member.getRoles().stream().noneMatch(role -> role.getName().toLowerCase().contains("rusher"))) return;
                            if (emote.getName().toLowerCase().equals("switchs") && member.getRoles().stream().noneMatch(role -> role.getName().toLowerCase().contains("trickster"))) return;
                        }

                        confirmEarlyLocReaction(member, raid, emote);

                    }
                }
                if (emote.equals(assist)) {
                    if (!member.equals(raid.getRaidLeader())) {
                        raid.addToAssistReacts(member);
                    } else {
                        event.getReaction().removeReaction(user).queue();
                    }
                }
                if (emote.equals(raid.getNitro()) && !raid.isStarted()) {
                    if ((member.getTimeBoosted() != null || guild.getBoosters().contains(member))) {
                        user.openPrivateChannel().complete().sendMessage(confirmedReaction(raid, emote).build()).queue();
                    } else {
                        event.getReaction().removeReaction(user).queue();
                    }
                }
            } else if (headCount != null) {
                List<Emote> earlyLocEmotes = headCount.getEarlyLocEmotes();
                if (earlyLocEmotes.contains(emote) || emote.equals(headCount.getKeyEmote())) {
                    if (!headCount.getEarlyLocationMap().containsKey(user)) {
                        headCount.addEarlyLocReact(user, emote);
                        headCount.updateControlPanel();
                    }
                }
            }
        }

        if (event.getReactionEmote().isEmoji()) {
            HeadCount headCount = getHeadCount(member);
            Raid raid = getRaid(member);
            String emoji = event.getReactionEmote().getEmoji();
            if (raid != null) {
                if (event.getMessageId().equals(raid.getControlPanel().getId())) {
                    if (emoji.equals("🗺")) {
                        raid.promptForLocation(raid.getRaidLeader());
                    }
                    if (emoji.equals("❌")) {
                        endRaid(raid);
                    }
                    if (emoji.equals("▶")) {
                        raid.startRaid();
                    }
                    if (emoji.equals("📥") && raid.isDefaultRaid()) {
                        raid.logRaid();
                        raid.setNumChained(raid.getNumChained() + 1);
                        raid.setLogString((raid.getNumChained()) + " Shatters " + (raid.getNumChained() == 1 ? "has" : "have") + " been logged for " + raid.getRaidLeader().getEffectiveName() + ". You have " + (SqlConnector.shattersStats(user)[6]) + " runs.");
                        raid.updateControlPanel();
                        event.getReaction().removeReaction(user).queue();
                    }
                    if (emoji.equals("🆕")) {
                        RaidHub.RAID_POOL.execute(raid::reopenRaid);
                    }
                    if (emoji.equals("♻")) {
                        raid.clearVc();
                    }
                }
            } else {
                raid = getRaid(event.getMessageId());
                if (raid != null) {
                    if (emoji.equals("❌")) {
                        confirmAction(member, true, raid, null);
                    }
                    if (emoji.equals("▶") && !raid.isStarted()) {
                        confirmAction(member, false, raid, null);
                    }
                    if (emoji.equals("🗺") && raid.getAssistReactions().contains(member)) {
                        raid.promptForLocation(member);
                    }
                    if (emoji.equals("📥") && raid.getAssistReactions().contains(member) && raid.isDefaultRaid()) {
                        raid.logRaid();
                        raid.setNumChained(raid.getNumChained() + 1);
                        raid.setLogString((raid.getNumChained()) + " Shatters " + (raid.getNumChained() == 1 ? "has" : "have") + " been logged for " + raid.getRaidLeader().getEffectiveName() + ". You have " + (SqlConnector.shattersStats(user)[6]) + " runs.");
                        raid.updateControlPanel();
                        event.getReaction().removeReaction(user).queue();
                    }
                }
            }
        }
    }

    public static void confirmEarlyLocReaction(Member member, Raid raid, Emote emote) {
        User user = member.getUser();
        PrivateChannel privateChannel = user.openPrivateChannel().complete();
        Message confirmationMessage = privateChannel
                .sendMessage(confirmReaction(raid, emote).build())
                .setActionRow(Button.success("confirmyes", "Yes"),
                        Button.danger("confirmno", "No"))
                .complete();
        eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return e.getMessage().equals(confirmationMessage) && Arrays.asList("confirmyes", "confirmno").contains(e.getComponentId());
        }, e -> {

            String control = e.getComponentId();

            if (control.equals("confirmyes")) {
                if (emote.equals(raid.getKeyEmote())) {
                    if (Database.isShatters(raid.getRaidGuild()) && raid.getKeyReacts().size() > 2) {
                        privateChannel.sendMessage("We have reached the maximum amount of reacts for " + emote.getAsMention() + ".").queue();
                        return;
                    }
                    raid.addKeyReact(member);
                    //raid.keyPanels.add(new KeyPanel(member, raid.getAssistReactions(), raid));
                }

                if (raid.getEarlyLocationMap().values().stream().filter(emote1 -> emote1.equals(emote)).collect(Collectors.toList()).size() > 3 && (!emote.equals(raid.getKeyEmote()) || raid.getRaidType() == 0)) {
                    privateChannel.sendMessage("We have reached the maximum amount of reacts for " + emote.getAsMention() + ".").queue();
                    return;
                }
                raid.addEarlyLocReact(user, emote);
                if (emote.getName().toLowerCase().contains("key") || emote.getName().toLowerCase().contains("rune") || emote.getName().toLowerCase().contains("vial")) raid.logPanel.addPopper(member, raid, emote.getName());
                raid.updateControlPanel();
                confirmationMessage.editMessage(confirmedReaction(raid, emote).build()).setActionRows().queue();
            }
            else {
                e.deferEdit().queue();
                confirmationMessage.delete().queue();
                raid.getRaidMessage().removeReaction(emote, user).queue();
            }
        }, 2L, TimeUnit.MINUTES, () -> {
            Utils.errorMessage("Could not confirm reaction", "User did not react in time", confirmationMessage, 15L);
            raid.getRaidMessage().removeReaction(emote, user).queue();
        });
    }

    @Override
    public void onGuildMessageReactionRemove(@Nonnull GuildMessageReactionRemoveEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        User user = event.getUser();

        if (user == null || user.isBot()) {
            return;
        }

        if (event.getReactionEmote().isEmote()) {
            Emote emote = event.getReactionEmote().getEmote();
            HeadCount headCount = getHeadCount(event.getMessageId());
            if (headCount != null) {
                List<Emote> earlyLocEmotes = headCount.getEarlyLocEmotes();
                if (earlyLocEmotes.contains(emote) || emote.equals(headCount.getKeyEmote())) {
                    headCount.removeEarlyLocReact(user, emote);
                    headCount.updateControlPanel();
                }
            }
        }
    }

    public static EmbedBuilder confirmReaction(Raid raid, Emote emote) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Reaction Confirmation for " + raid.getVoiceChannel().getName());
        embedBuilder.setColor(raid.getRaidColor());
        embedBuilder.setDescription("You have reacted with " + emote.getAsMention() + " on `" + raid.getRaidMessage().getEmbeds().get(0).getAuthor().getName() + "`. " +
                "If you intended to react with this please click yes, otherwise click no for it to be ignored.");
        embedBuilder.setFooter("Please note that if you fake react you will be suspended");
        return embedBuilder;
    }

    public static EmbedBuilder confirmedReaction(Raid raid, Emote emote) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Thank you for confirming your reaction!");
        embedBuilder.setColor(raid.getRaidColor());
        embedBuilder.setDescription("Thank you for reacting with " + emote.getAsMention() + " please make sure to get to the location as soon as you can.");
        embedBuilder.addField("Raid Location", raid.getLocation().isEmpty() ? "`None Set`" : raid.getLocation(), false);
        embedBuilder.setFooter("Reaction for " + raid.getRaidMessage().getEmbeds().get(0).getAuthor().getName());
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    public static void dungeonSelection(Member member, TextChannel raidCommandsChannel, TextChannel raidStatusChannel, String location, int customCap, VoiceChannel voiceChannel) {
        List<Dungeon> dungeons = DungeonInfo.nDungeonInfo(member.getGuild());
        Message controlPanel = raidCommandsChannel.sendMessage(nDungeonSelectionScreen(member.getGuild()).build()).complete();

        eventWaiter.waitForEvent(MessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(member.getUser()) && (Utils.isNumeric(e.getMessage().getContentRaw().replace(" ", ""))
                || e.getMessage().getContentRaw().toLowerCase().equals("close") || e.getMessage().getContentRaw().toLowerCase().equals("all"));}, e -> {
            if (e.getMessage().getContentRaw().toLowerCase().equals("close")) {
                controlPanel.delete().queue();
                e.getMessage().delete().queue();
                return;
            }

            if (e.getMessage().getContentRaw().split(" ").length > 1) {
                String args[] = e.getMessage().getContentRaw().split(" ");
                DungeonInfo.eventDungeon[2] = "";
                for (int i = 0; i < args.length; i++) {
                    int choice = Integer.parseInt(args[i]) - 1;
                    DungeonInfo.eventDungeon[2] += dungeons.get(choice).dungeonInfo[1] + " ";
                }
                e.getMessage().delete().queue();
                controlPanel.delete().queue();
                DungeonInfo.eventDungeon[2] = DungeonInfo.eventDungeon[2].trim();
                activeRaids.add(new Raid(member, -2 ,raidStatusChannel, raidCommandsChannel, location, false, customCap, voiceChannel));
                return;
            }

            int choice = Integer.parseInt(e.getMessage().getContentRaw()) - 1;

            if (!(choice <= dungeons.size() - 1)) {
                e.getMessage().delete().queue();
                Utils.errorMessage("Failed to Start Raid", "Dungeon type is invalid, please try again.", controlPanel, 5L);
                return;
            }
            controlPanel.delete().queue();
            e.getMessage().delete().queue();
            activeRaids.add(new Raid(member, choice ,raidStatusChannel, raidCommandsChannel, location, false, customCap, voiceChannel));

        }, 3L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    public static void dungeonSelectionChangeRaid(Member member, TextChannel commandChannel, Raid raid) {
        List<Dungeon> dungeonInfo = DungeonInfo.nDungeonInfo(member.getGuild());
        Message controlPanel;

        int dungeonType = 0;
        int amountOfChoices = dungeonInfo.size() - 1;
        if (raid.getRaidType() >= 5 && dungeonType <= 7) {
            amountOfChoices = 3;
            dungeonType = 5;
            controlPanel = commandChannel.sendMessage(defaultDungeonSelectionScreen(member.getGuild()).build()).complete();
        } else {
            controlPanel = commandChannel.sendMessage(nDungeonSelectionScreen(member.getGuild()).build()).complete();
        }

        int finalAmountOfChoices = amountOfChoices;
        int finalDungeonType = dungeonType;
        eventWaiter.waitForEvent(MessageReceivedEvent.class, e -> {return e.getAuthor().equals(member.getUser()) && (Utils.isNumeric(e.getMessage().getContentRaw().replace(" ", ""))
                || e.getMessage().getContentRaw().toLowerCase().equals("close") || e.getMessage().getContentRaw().toLowerCase().equals("all"));}, e -> {

            if (e.getMessage().getContentRaw().toLowerCase().equals("close")) {
                controlPanel.delete().queue();
                e.getMessage().delete().queue();
                return;
            }

            if (e.getMessage().getContentRaw().split(" ").length > 1) {
                String args[] = e.getMessage().getContentRaw().split(" ");
                DungeonInfo.eventDungeon[2] = "";
                for (int i = 0; i < args.length; i++) {
                    int choice = Integer.parseInt(args[i]) - 1;
                    DungeonInfo.eventDungeon[2] += dungeonInfo.get(choice).dungeonInfo[1] + " ";
                }
                e.getMessage().delete().queue();
                controlPanel.delete().queue();
                DungeonInfo.eventDungeon[2] = DungeonInfo.eventDungeon[2].trim();
                Raid newRaid = new Raid(raid.getRaidLeader(), -2, raid.getRaidStatusChannel(), raid.getRaidCommandsChannel(), raid.getLocation(), true, raid.getDungeonLimit(), null);
                newRaid.retrieveRaid(raid.getVoiceChannel(), raid.getRaidMessage(), raid.getControlPanel(), raid.isStarted(), raid.getFeedbackChannel(), raid.getFeedbackMessage(), null);
                //newRaid.updateVoiceChannelName();
                newRaid.setPlaceHolderMessage(raid.getPlaceHolderMessage());
                raid.getRaidTimeManager().shutdown();
                activeRaids.remove(raid);
                activeRaids.add(newRaid);
                return;
            }

            int choice = Integer.parseInt(e.getMessage().getContentRaw()) - 1;

            if (!(choice <= finalAmountOfChoices)) {
                e.getMessage().delete().queue();
                Utils.errorMessage("Failed to Start Raid", "Dungeon type is invalid, please try again.", controlPanel, 5L);
                return;
            }
            controlPanel.delete().queue();
            e.getMessage().delete().queue();
            Raid newRaid = new Raid(raid.getRaidLeader(), finalDungeonType + choice, raid.getRaidStatusChannel(), raid.getRaidCommandsChannel(), raid.getLocation(), true, raid.getDungeonLimit(), null);
            newRaid.retrieveRaid(raid.getVoiceChannel(), raid.getRaidMessage(), raid.getControlPanel(), raid.isStarted(), raid.getFeedbackChannel(), raid.getFeedbackMessage(), null);
            //newRaid.updateVoiceChannelName();
            newRaid.setPlaceHolderMessage(raid.getPlaceHolderMessage());
            raid.getRaidTimeManager().shutdown();
            activeRaids.remove(raid);
            activeRaids.add(newRaid);

        }, 3L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    public static void defaultDungeonSelection(Member member, TextChannel raidCommandsChannel, TextChannel raidStatusChannel, String location, int customCap, VoiceChannel voiceChannel) {
        Message controlPanel = raidCommandsChannel.sendMessage(defaultDungeonSelectionScreen(member.getGuild()).build()).complete();
        String[] quotaString = SetupConnector.getFieldValue(member.getGuild(), "guildInfo", "quotaString").trim().split(" ");
        int amountOfChoices = quotaString.length;

        eventWaiter.waitForEvent(MessageReceivedEvent.class, e -> {return e.getAuthor().equals(member.getUser()) && (Utils.isNumeric(e.getMessage().getContentRaw())
                || e.getMessage().getContentRaw().toLowerCase().equals("close") || e.getMessage().getContentRaw().toLowerCase().equals("all"));}, e -> {

            if (e.getMessage().getContentRaw().toLowerCase().equals("close")) {
                controlPanel.delete().queue();
                e.getMessage().delete().queue();
                return;
            }

            int choice = Integer.parseInt(e.getMessage().getContentRaw()) - 1;

            if (choice > amountOfChoices - 1) {
                e.getMessage().delete().queue();
                Utils.errorMessage("Failed to Start Raid", "Dungeon type is invalid, please try again.", controlPanel, 5L);
                return;
            }

            controlPanel.delete().queue();
            e.getMessage().delete().queue();
            activeRaids.add(new Raid(member, Integer.parseInt(quotaString[choice]) ,raidStatusChannel, raidCommandsChannel, location, false, customCap, voiceChannel));

        }, 3L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    public static void dungeonSelection(Member member, TextChannel raidCommandsChannel, TextChannel raidStatusChannel) {
        List<Dungeon> dungeons = DungeonInfo.nDungeonInfo(member.getGuild());
        Message controlPanel = raidCommandsChannel.sendMessage(nDungeonSelectionScreen(member.getGuild()).build()).complete();

        eventWaiter.waitForEvent(MessageReceivedEvent.class, e -> {return e.getAuthor().equals(member.getUser()) && (Utils.isNumeric(e.getMessage().getContentRaw().replace(" ", ""))
                || e.getMessage().getContentRaw().toLowerCase().equals("close") || e.getMessage().getContentRaw().toLowerCase().equals("all"));}, e -> {

            if (e.getMessage().getContentRaw().toLowerCase().equals("close")) {
                controlPanel.delete().queue();
                e.getMessage().delete().queue();
                return;
            }

            if (e.getMessage().getContentRaw().split(" ").length > 1) {
                String args[] = e.getMessage().getContentRaw().split(" ");
                DungeonInfo.eventDungeon[2] = "";
                for (int i = 0; i < args.length; i++) {
                    int choice = Integer.parseInt(args[i]) - 1;
                    DungeonInfo.eventDungeon[2] += dungeons.get(choice).dungeonInfo[1] + " ";
                }
                e.getMessage().delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
                controlPanel.delete().queue();
                DungeonInfo.eventDungeon[2] = DungeonInfo.eventDungeon[2].trim();
                activeHeadcounts.add(new HeadCount(member, raidCommandsChannel, raidStatusChannel, -2));
                return;
            }

            int choice = Integer.parseInt(e.getMessage().getContentRaw().replaceAll("[^0-9]", "")) - 1;

            if (!(choice <= dungeons.size() - 1)) {
                e.getMessage().delete().queue();
                Utils.errorMessage("Failed to Start Raid", "Dungeon type is invalid, please try again.", controlPanel, 5L);
                return;
            }
            e.getMessage().delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            controlPanel.delete().queue();
            activeHeadcounts.add(new HeadCount(member, raidCommandsChannel, raidStatusChannel, choice));

        }, 3L, TimeUnit.MINUTES, () -> {
            controlPanel.editMessage(nDungeonSelectionScreen(member.getGuild()).clearFields().setDescription("```\n" +
                    "This Raid Selection Screen has been closed due to inactivity. \n```")
                    .setFooter("This message will delete in 10 seconds.").build()).queue();
            controlPanel.delete().submitAfter(10L, TimeUnit.SECONDS);
        });
    }

//    public static EmbedBuilder dungeonSelectionScreen() {
//        int i = 0;
//        String[][] dungeonInfo = DungeonInfo.olddungeonInfo();
//
//        EmbedBuilder embedBuilder = new EmbedBuilder();
//        embedBuilder.setTitle("Raid Type Selection");
//        embedBuilder.setColor(Goldilocks.BLUE);
//        embedBuilder.setDescription("Please select a type for your raid by typing the number of the corresponding type below.");
//
//        String legendaryDungeons = "";
//        String courtDungeons = "";
//        String epicDungeons = "";
//        String highlandsDungeons = "";
//        String randomDungeons = "";
//        String randomLobbyTypes = "";
//        for (i = i; i < 9; i++) {
//            legendaryDungeons += "\n**" + (i + 1) + ".** " + Goldilocks.jda.getEmoteById(dungeonInfo[i][0]).getAsMention() + " " + dungeonInfo[i][3];
//        }
//        for (i = i; i < 19; i++) {
//            courtDungeons += "\n**" + (i + 1) + ".** " + Goldilocks.jda.getEmoteById(dungeonInfo[i][0]).getAsMention() + " " + dungeonInfo[i][3];
//        }
//        for (i = i; i < 29; i++) {
//            epicDungeons += "\n**" + (i + 1) + ".** " + Goldilocks.jda.getEmoteById(dungeonInfo[i][0]).getAsMention() + " " + dungeonInfo[i][3];
//        }
//        for (i = i; i < 39; i++) {
//            highlandsDungeons += "\n**" + (i + 1) + ".** " + Goldilocks.jda.getEmoteById(dungeonInfo[i][0]).getAsMention() + " " + dungeonInfo[i][3];
//        }
//        for (i = i; i < 51; i++) {
//            randomDungeons += "\n**" + (i + 1) + ".** " + Goldilocks.jda.getEmoteById(dungeonInfo[i][0]).getAsMention() + " " + dungeonInfo[i][3];
//        }
//        for (i = i; i < 55; i++) {
//            randomLobbyTypes += "\n**" + (i + 1) + ".** " + Goldilocks.jda.getEmoteById(dungeonInfo[i][0]).getAsMention() + " " + dungeonInfo[i][3];
//        }
//        embedBuilder.addField("Legendary Dungeons", legendaryDungeons, true);
//        embedBuilder.addField("Court Dungeons and Aliens", courtDungeons, true);
//        embedBuilder.addField("Epic Dungeons", epicDungeons, true);
//        embedBuilder.addField("Highlands Dungeons", highlandsDungeons, true);
//        embedBuilder.addField("Random Dungeons", randomDungeons, true);
//        embedBuilder.addField("Random Lobbies", randomLobbyTypes, true);
//        embedBuilder.addField("Multiple Dungeons", "If you would like your raid or headcount to have multiple dungeon options please enter the corresponding dungeon numbers in the following format: `# # #`" +
//                "\n\n**📝Example:**" +
//                "\nFor Ddocks, Wlab, and Cdepths enter:" +
//                "\n```" +
//                "\n22 23 24" +
//                "\n```", false);
//        embedBuilder.setFooter("To exit the raid selection gui type: close");
//        return embedBuilder;
//    }

    public static EmbedBuilder nDungeonSelectionScreen(Guild guild) {
        List<Dungeon> dungeons = DungeonInfo.nDungeonInfo(guild);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Raid Type Selection");
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("Please select a type for your raid by typing the number of the corresponding type below.");

        String[] dungeonTypes = {"legendary", "court", "epic", "highlands", "random", "randomLobby"};
        String[] dungeonVisNames = {"Legendary Dungeons", "Court Dungeons and Aliens", "Epic Dungeons", "Highlands Dungeons", "Random Dungeons", "Random Lobbies"};

        for (int i = 0; i < dungeonTypes.length; i++) {
            int finalI = i;
            embedBuilder.addField(dungeonVisNames[i], dungeons.stream()
                    .filter(n -> n.dungeonCategory.equals(dungeonTypes[finalI]))
                    .map(n -> "**" + (dungeons.indexOf(n) + 1) + ".** " + Goldilocks.jda.getEmoteById(n.dungeonInfo[0]).getAsMention() + " " + n.dungeonName)
                    .collect(Collectors.joining("\n")), true);
        }

        embedBuilder.addField("Multiple Dungeons", "If you would like your raid or headcount to have multiple dungeon options please enter the corresponding dungeon numbers in the following format: `# # #`" +
                "\n\n**📝Example:**" +
                "\nFor Ddocks, Wlab, and Cdepths enter:" +
                "\n```" +
                "\n22 23 24" +
                "\n```", false);
        embedBuilder.setFooter("To exit the raid selection gui type: close");
        return embedBuilder;
    }

    public static EmbedBuilder defaultDungeonSelectionScreen(Guild guild) {
        List<Dungeon> dungeonInfo = DungeonInfo.nDungeonInfo(guild);
        EmbedBuilder embedBuilder = new EmbedBuilder();

        String[] quotaString = SetupConnector.getFieldValue(guild, "guildInfo", "quotaString").split(" ");

        embedBuilder.setTitle("Raid Type Selection");
        embedBuilder.setDescription("Please select the type of raid by typing the number of the corresponding type below." +
                "\n\nThe raid type you pick will last the duration of your voice channel.\n");
        embedBuilder.setColor(Goldilocks.LIGHTBLUE);

        String raidTypes = "";
        for (int i = 0; i < quotaString.length; i++) {
            if (!quotaString[i].isEmpty()) {
                raidTypes += "\n**" + (i + 1) + ".** "
                        + Goldilocks.jda.getEmoteById(dungeonInfo.get(Integer.parseInt(quotaString[i])).dungeonInfo[0]).getAsMention()
                        + " " + dungeonInfo.get(Integer.parseInt(quotaString[i])).dungeonInfo[3];
            }
        }
        embedBuilder.addField("Nest Raid Types", raidTypes, true);
        embedBuilder.setFooter("To exit the raid selection gui type: close");
        return embedBuilder;
    }

    public static void confirmAction(Member member, boolean end, Raid raid, HeadCount headCount) {
        if (!Permissions.hasPermission(member, new String[]{"hrl","mod", "headEo"})) {
            return;
        }
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Are you sure you want to " + (end ? "end " : "start ") + (raid != null ? raid.getVoiceChannel().getName() : headCount.getRaidLeader().getEffectiveName() + "'s Headcount") + "?");
        embedBuilder.setFooter("Use ✅ or ❌ to select your answer")
                .setTimestamp(new Date().toInstant());
        embedBuilder.setColor(Goldilocks.GOLD);

        Message confirmationMessage;
        if (raid != null) confirmationMessage = raid.getRaidCommandsChannel().sendMessage(embedBuilder.build()).complete();
        else confirmationMessage = headCount.getRaidCommandsChannel().sendMessage(embedBuilder.build()).complete();
        confirmationMessage.addReaction("✅").queue();
        confirmationMessage.addReaction("❌").queue();

        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getReactionEmote().isEmoji() && ("✅❌").contains(e.getReactionEmote().getEmoji()) && e.getMember().equals(member)
                    && e.getMessageId().equals(confirmationMessage.getId());
        }, e -> {
            String emote = e.getReactionEmote().getEmoji();

            if (("✅").equals(emote)) {
                if (end && raid != null) endRaid(raid, member);
                if (!end && raid != null) raid.startRaid();
                if (end && headCount != null) headCount.deleteHeadCount();
            }

            if (("❌").equals(emote)) {
                //Do something?
            }

            confirmationMessage.delete().queue();

        }, 2L, TimeUnit.MINUTES, () -> {
            confirmationMessage.delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
        });

    }

}
