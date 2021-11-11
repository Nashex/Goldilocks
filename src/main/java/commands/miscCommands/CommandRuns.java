package commands.miscCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.lang3.StringUtils;
import utils.Utils;

import java.util.Date;

public class CommandRuns extends Command {
    public CommandRuns() {
        setAliases(new String[] {"runs"});
        setEligibleRoles(new String[] {"verified"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.VERIFIED);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        String userId = msg.getMember().getId();
        if (args.length == 0) {
            msg.delete().queue();
            return;
        }

        try {
            if (args.length == 1 || Utils.isNumeric(args[0])) {
                userId = StringUtils.getDigits(args[0]);
            }
        } catch (Exception e) {
            msg.delete().queue();
            return;
        }

        if (!Database.userExists(userId, msg.getGuild().getId())){
            msg.getTextChannel().sendMessage("**User " + args[0] + " doesn't have any runs yet!**").queue();
            return;
        }

        msg.getTextChannel().sendMessage("**User " + args[0] + " has " + Database.getRunsCompleted(userId, msg.getGuild().getId()) + " completed runs!**").queue();

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Runs");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Verified\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nShows how many runs a user has participated in." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
