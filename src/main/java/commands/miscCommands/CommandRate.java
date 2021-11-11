package commands.miscCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import misc.Rate;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import utils.Utils;

import java.util.Date;

public class CommandRate extends Command {
    public CommandRate() {
        setAliases(new String[] {"rate"});
        setEligibleRoles(new String[] {"officer", "hrl", "headEo"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.GAME);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        if (msg.getMentionedChannels().isEmpty()) {
            Utils.errorMessage("Unable to Create Rating Pole", "Please use the command in the following format: rate <channel tag> <rating title>", msg.getTextChannel(), 10L);
            return;
        }
        new Rate(msg);
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Ping");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Trial Security\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nChecks if the bot is online." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
