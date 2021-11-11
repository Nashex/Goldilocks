package commands.miscCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import pointSystem.PointGui;
import pointSystem.PointProfile;

import java.util.Date;

public class CommandPoints extends Command {
    public CommandPoints() {
        setAliases(new String[] {"points"});
        setEligibleRoles(new String[] {""});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.VERIFIED);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        TextChannel textChannel = msg.getTextChannel();
        Member member = msg.getMember();

        new PointGui(member, member, textChannel, new PointProfile());

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
