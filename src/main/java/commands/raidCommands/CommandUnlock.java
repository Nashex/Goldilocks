package commands.raidCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import utils.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class CommandUnlock extends Command {
    public CommandUnlock() {
        setAliases(new String[] {"lock", "unlock"});
        setEligibleRoles(new String[] {"arl", "security", "eo"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.DEVELOPER);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        List<String> commandChannelIds = Database.getGuildRaidCommandChannels(msg.getGuild().getId());
        if (!commandChannelIds.contains(msg.getTextChannel().getId())) {
            msg.delete().queue();
            Utils.errorMessage("Failed to Unlock VC", "This is not a valid command channel for locking and unlocking vcs.", msg.getTextChannel(), 10L);
            return;
        }

        List<VoiceChannel> raidingChannels = msg.getCategory().getVoiceChannels()
                .stream().filter(voiceChannel -> !voiceChannel.getName().replaceAll("[^0-9]", "").isEmpty() && !voiceChannel.getName().toLowerCase().contains("drag")).collect(Collectors.toList());
        HashMap<String, VoiceChannel> raidingChannelNames = new HashMap<>();
        raidingChannels.forEach(voiceChannel -> raidingChannelNames.put(voiceChannel.getName().replaceAll("[a-z. ]", "").toLowerCase(), voiceChannel));

        String raidStatusChannelId = Database.getRaidStatusChannel(msg.getTextChannel().getId());
        Role raiderRole = Goldilocks.jda.getRoleById(Database.getRaiderRole(raidStatusChannelId));
        if (raiderRole == null) return;

        if (msg.getMember().getVoiceState().inVoiceChannel() && args.length == 0) {

            VoiceChannel voiceChannel = msg.getMember().getVoiceState().getChannel();
            lockVoiceChannel(voiceChannel, msg.getTextChannel(), raiderRole);

        } else if (raidingChannelNames.containsKey(args[0].toLowerCase())){
            VoiceChannel voiceChannel = raidingChannelNames.get(args[0].toLowerCase());
            lockVoiceChannel(voiceChannel, msg.getTextChannel(), raiderRole);
        } else {
            msg.getTextChannel().sendMessage("Unable to " + alias.toLowerCase() + " voice channel. Please use the command as follows: `.lock <r1,r2...>` or just `.lock` if you are in a VC and want to lock it.").queue();
        }

    }

    public static void lockVoiceChannel(VoiceChannel voiceChannel, TextChannel textChannel, Role raiderRole) {
        if (voiceChannel.getPermissionOverride(raiderRole) != null && voiceChannel.getPermissionOverride(raiderRole).getDenied().contains(Permission.VOICE_CONNECT)) {
            try {
                voiceChannel.getManager().putPermissionOverride(raiderRole, EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT), null).complete();
                textChannel.sendMessage("🔓 Successfully unlocked `" + voiceChannel.getName() + "`!").queue();
            } catch (Exception e) { textChannel.sendMessage("🔓 Error unlocking `" + voiceChannel.getName() + "`. I may not have permission to do so.").queue(); }
        } else {
            try {
                voiceChannel.getManager().putPermissionOverride(raiderRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_CONNECT)).complete();
                textChannel.sendMessage("🔒 Successfully locked `" + voiceChannel.getName() + "`!").queue();
            } catch (Exception e) { textChannel.sendMessage("🔒 Error locking `" + voiceChannel.getName() + "`. I may not have permission to do so.").queue(); }
        }
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Lock");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Raid Leader\n";
        commandDescription += "Syntax: ;lock <command alias>\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nUnlocks a Voice channel" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
