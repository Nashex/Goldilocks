package commands.miscCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import utils.Utils;

import java.util.Date;

public class CommandPing extends Command {
    public CommandPing() {
        setAliases(new String[] {"ping"});
        setEligibleRoles(new String[] {"trl","tSec"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.DEBUG);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        Utils.sendMessage(msg.getTextChannel(), "Latency: `" + Goldilocks.jda.getGatewayPing() + "ms`");
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
