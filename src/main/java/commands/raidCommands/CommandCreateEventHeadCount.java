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

import java.util.Date;
import java.util.List;

public class CommandCreateEventHeadCount extends Command {
    public CommandCreateEventHeadCount() {
        setAliases(new String[] {"ehc","hc","eventhc"});
        setEligibleRoles(new String[] {"vetRl","vetEo"});
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


        HeadCount headCount = RaidHub.getHeadCount(msg.getMember());
        if (headCount == null) {
            RaidHub.createEventHeadcount(msg.getMember(), msg.getTextChannel());
        } else {
            headCount.deleteHeadCount();
            RaidHub.createEventHeadcount(msg.getMember(), msg.getTextChannel());
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
        embedBuilder.setTitle("Command: Create Event Headcount");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Veteran Raid Leader\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nA command to create an event hc." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
