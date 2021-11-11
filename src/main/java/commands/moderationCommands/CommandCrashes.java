package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Date;

public class CommandCrashes extends Command {
    public CommandCrashes() {
        setAliases(new String[] {"crashes"});
        setEligibleRoles(new String[] {"security", "eo", "arl"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        TextChannel textChannel = msg.getTextChannel();
        if (args.length < 1) {
            textChannel.sendMessage("Please use the command with the following syntax: `.crashes <name of crasher>`").queue();
        }

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Crashers");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Security, Almost Raid Leader, Event Organizer\n";
        commandDescription += "Syntax: .crashes\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nLists all of the Crashes for a potential user" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

}
