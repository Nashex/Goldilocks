package commands.setupCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import setup.VerificationSetup;

import java.util.Date;

public class CommandVerificationSetup extends Command {
    public CommandVerificationSetup() {
        setAliases(new String[] {"verifysetup"});
        setEligibleRoles(new String[] {"developer"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.SETUP);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        VerificationSetup.createVerificationSetup(msg);
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Raid Setup");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Owner\n";
        commandDescription += "Syntax: ;alias <command alias>\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nSets up raiding categories for a guild" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
