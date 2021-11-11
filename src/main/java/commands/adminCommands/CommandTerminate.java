package commands.adminCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.Date;

public class CommandTerminate extends Command {
    public CommandTerminate() {
        setAliases(new String[] {"terminate", "exit"});
        setEligibleRoles(new String[] {"admin"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.DEVELOPER);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        System.exit(0);
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Terminate");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Verified\n";
        commandDescription += "Syntax: -alias <command alias>\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nShows information for all Goldilocks' commands" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
