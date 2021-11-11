package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import utils.RealmeyeRequestsIssuer;

import java.util.Arrays;
import java.util.Date;

public class CommandBackgroundCheck extends Command {
    public CommandBackgroundCheck() {
        setAliases(new String[] {"backgroundcheck" ,"bc", "gc"});
        setEligibleRoles(new String[] {"officer", "vetRl", "headEo"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        TextChannel textChannel = msg.getTextChannel();

        if (args.length == 0) {
            textChannel.sendMessage("Please use the command in the following format: `.bc <name> [additional names...]` or for just guild and name history use the `gc` alias.").queue();
            return;
        }

        RealmeyeRequestsIssuer.ProfileQuickScrape(Arrays.asList(args), msg.getTextChannel(), alias.equalsIgnoreCase("gc") ? "mini" : "compact");

    }


    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Background Check");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Officer or Veteran Raid Leader\n";
        commandDescription += "Syntax: ;bc <name> [additional names...]\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nRqapidly retrieves the realmeye profiles for the specified names" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
