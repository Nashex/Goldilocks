package commands.raidCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import raids.HeadCount;
import raids.RaidHub;
import utils.Utils;

import java.awt.*;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CommandStartHeadCount extends Command {
    public CommandStartHeadCount() {
        setAliases(new String[] {"headcount","hc"});
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
            Utils.errorMessage("Failed to Start Headcount", "This is not a valid command channel for headcount.", textChannel, 10L);
            return;
        }

        if (Database.isEndGame(guild)) {
            List<TextChannel> hcChannels = guild.getTextChannels().stream().filter(t -> t.getName().contains("runes")).collect(Collectors.toList());
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setColor(new Color(57, 0, 132))
                    .setAuthor(msg.getMember().getEffectiveName() + " has started a new Rune Count", null, msg.getAuthor().getAvatarUrl())
                    .setDescription("If you have any of the following please react to them so that we can organize a run soon!")
                    .setTimestamp(new Date().toInstant());
            if (!hcChannels.isEmpty()) hcChannels.get(0).sendMessage(embedBuilder.build()).content("@here " +
                    msg.getMember().getEffectiveName() + " has started a new Rune Count").queue(m -> {
                m.addReaction(Goldilocks.jda.getEmoteById("768186911650611300")).queue();
                m.addReaction(Goldilocks.jda.getEmoteById("768186834127290408")).queue();
                m.addReaction(Goldilocks.jda.getEmoteById("768186895930359888")).queue();
                m.addReaction(Goldilocks.jda.getEmoteById("768186846059954227")).queue();
                m.addReaction(Goldilocks.jda.getEmoteById("749176008082456646")).queue();
                m.editMessage(msg.getMember().getEffectiveName() + " has started a new Rune Count").queue();
                msg.reply(new EmbedBuilder().setDescription("Headcount created **[here](" + m.getJumpUrl() + ")**").setColor(Goldilocks.GREEN).build())
                        .mentionRepliedUser(false).queue();
            });

            return;
        }

        HeadCount headCount = RaidHub.getHeadCount(msg.getMember());
        if (headCount == null) {
            RaidHub.createNewHeadcount(msg.getMember(), msg.getTextChannel());
        } else {
            headCount.deleteHeadCount();
            RaidHub.createNewHeadcount(msg.getMember(), msg.getTextChannel());
            //Todo make it so that rls can delete an "existing" headcount
        }
        msg.delete().queue();
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Create Lobby Manager");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Lobby Master\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nOpens up the lobby creation menu." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
