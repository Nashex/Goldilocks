package commands.adminCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import quota.QuotaManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.Date;

public class CommandQuotaEmbed extends Command {
    public CommandQuotaEmbed() {
        setAliases(new String[] {"quota"});
        setEligibleRoles(new String[] {"rl"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.DEVELOPER);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        QuotaManager.updateQuotaMessage(msg.getGuild());
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
