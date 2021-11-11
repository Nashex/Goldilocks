package commands.raidCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import raids.Raid;
import raids.RaidHub;
import utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CommandCreateRaid extends Command {
    public CommandCreateRaid() {
        setAliases(new String[] {"raid","afk"});
        setEligibleRoles(new String[] {"arl","eo"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.RAID);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        Guild guild = msg.getGuild();
        TextChannel textChannel = msg.getTextChannel();
        List<String> commandChannelIds = Database.getGuildRaidCommandChannels(guild.getId());
        if (!commandChannelIds.contains(textChannel.getId())) {
            msg.delete().queue();
            Utils.errorMessage("Failed to Start Raid", "This is not a valid command channel for raids.", textChannel, 10L);
            return;
        }

        if (Database.isEndGame(guild)) {
            RaidHub.createNewEndgameRaid(msg.getMember(), String.join(" ", args), textChannel);
            return;
        }

        int customCap = -1;
        if (args.length > 0) {
            for (String potCap : args) {
                if (Utils.isNumeric(potCap)) {
                    customCap = Integer.parseInt(potCap);
                    if (customCap < 0) customCap = 1;
                    if (customCap > 85) customCap = 85;
                }
            }
        }
        if (Database.hasStaticChannels(guild)) getRaidingVc(msg, args);
        else createRaid(msg, args, customCap, null, String.join(" ", args).replace(customCap + " ", ""));

    }

    private void getRaidingVc(Message msg, String[] args) {
        int customCap = -1;
        if (args.length > 0) {
            for (String potCap : args) {
                if (Utils.isNumeric(potCap)) {
                    customCap = Integer.parseInt(potCap);
                    if (customCap < 0) customCap = 1;
                    if (customCap > 85) customCap = 85;
                }
            }
        }

        List<VoiceChannel> raidingChannels = msg.getCategory().getVoiceChannels()
                .stream().filter(voiceChannel ->
                        !voiceChannel.getName().replaceAll("[^0-9]", "").isEmpty()
                                && (voiceChannel.getName().toLowerCase().contains("raiding")
                        || voiceChannel.getName().toLowerCase().contains("event"))
                ).collect(Collectors.toList());
        HashMap<String, VoiceChannel> raidingChannelNames = new HashMap<>();
        raidingChannels.forEach(voiceChannel -> raidingChannelNames.put(voiceChannel.getName().replaceAll("[a-z. ]", "").toLowerCase(), voiceChannel));

        VoiceChannel voiceChannel;
        if (args.length > 0 && raidingChannelNames.containsKey(args[0].toLowerCase())) {
            voiceChannel = raidingChannelNames.get(args[0].toLowerCase());
            createRaid(msg, args, customCap, voiceChannel, String.join(" ", args).replace(args[0], " ").replace(String.valueOf(customCap), ""));
        } else if (msg.getMember().getVoiceState().inVoiceChannel() && !Database.isFungal(msg.getGuild())) {
            voiceChannel = msg.getMember().getVoiceState().getChannel();
            createRaid(msg, args, customCap, voiceChannel, String.join(" ", args).replace(customCap + " ", ""));
        } else {
            List<String> emotes = new ArrayList<>();
            emotes.add("❌");

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Please Select a Raiding VC for Your Raid")
                    .setColor(Goldilocks.WHITE)
                    .setDescription("Please react to the number emote that corresponds to the raiding VC you would like to start a raid in.")
                    .setFooter("React with ❌ to cancel");

            String vcString = "";
            int index = 1;
            for (VoiceChannel voiceChannel1 : raidingChannels) {
                if (index <= 10) {
                    String emote = Goldilocks.numEmotes[index++];
                    emotes.add(emote);
                    vcString += emote + ": `" + String.format("%-25s", voiceChannel1.getName()) + "`\n";
                }
            }
            embedBuilder.addField("Raiding Channels", vcString, false);

            Message promptMessage = msg.getTextChannel().sendMessage(embedBuilder.build()).complete();
            for (String s : emotes) promptMessage.addReaction(s).queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));

            int finalCustomCap = customCap;
            Goldilocks.eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
                return e.getMessageId().equals(promptMessage.getId()) && e.getUser().equals(msg.getAuthor()) && e.getReactionEmote().isEmoji() &&
                        emotes.contains(e.getReactionEmote().getEmoji());
            }, e -> {
                String emoji = e.getReactionEmote().getEmoji();
                if (!("❌").equals(emoji)) {
                    VoiceChannel voiceChannel1 = raidingChannels.get(emotes.indexOf(emoji) - 1);
                    createRaid(msg, args, finalCustomCap, voiceChannel1, String.join(" ", args).replace(String.valueOf(finalCustomCap), ""));
                }
                promptMessage.delete().queue();
            }, 5L, TimeUnit.MINUTES, () -> {
                promptMessage.delete().queue();
            });
        }

    }

    private void createRaid(Message msg, String[] args, int customCap, VoiceChannel voiceChannel, String location) {
        if (RaidHub.getRaid(msg.getMember()) == null) {
            RaidHub.createNewRaid(msg.getMember(), location.replace(String.valueOf(customCap), "").trim().replace("  ", " "), msg.getTextChannel(), customCap, voiceChannel);
            if (args.length > 1 && (Utils.isNumeric(args[0]) || Utils.isNumeric(args[1]))) {
                Raid raid = RaidHub.getRaid(msg.getMember());
                if (raid != null) {
                    if (Utils.isNumeric(args[0])) {
                        raid.setDungeonLimit(Integer.parseInt(args[0]));
                    } else {
                        raid.setDungeonLimit(Integer.parseInt(args[1]));
                    }
                }
            }
            if (Database.deleteMessages(msg.getGuild())) msg.delete().queue();
        } else {
            Raid raid = RaidHub.getRaid(msg.getMember());
            RaidHub.RAID_POOL.execute(raid::reopenRaid);
            if (Database.deleteMessages(msg.getGuild())) msg.delete().queue();
        }
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Create Raid");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Raid Leader or Event Organizer\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nStarts a raid" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
