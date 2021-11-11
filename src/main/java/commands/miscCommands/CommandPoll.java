package commands.miscCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import misc.Poll;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import utils.Utils;

import java.util.Date;

public class CommandPoll extends Command {
    public CommandPoll() {
        setAliases(new String[] {"poll"});
        setEligibleRoles(new String[] {"officer", "hrl", "eo"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.GAME);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        if (msg.getMentionedChannels().isEmpty()) {
            Utils.errorMessage("Unable to Create Poll", "Please mention a channel you would like the poll to be in.", msg.getTextChannel(), 10L);
            return;
        }
        new Poll(msg);
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
