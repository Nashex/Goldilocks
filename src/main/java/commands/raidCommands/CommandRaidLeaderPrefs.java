package commands.raidCommands;

import commands.Command;
import commands.CommandHub;
import customization.RaidLeaderPrefsCP;
import main.Config;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Date;

public class CommandRaidLeaderPrefs extends Command {
    public CommandRaidLeaderPrefs() {
        setAliases(new String[] {"raidprefs","rprefs","rlprefs","color","emotes"});
        setEligibleRoles(new String[] {"rl"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.CUSTOMIZATION);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

//        if (!msg.getAuthor().isBot()) {
//            msg.getTextChannel().sendMessage("This command is disabled while Goldilocks gets verified!");
//            return;
//        }

        if (args.length > 0 && msg.getAuthor().getId().equals(Config.get("INSTANCE_OWNER")) && !msg.getMentionedMembers().isEmpty()) {
            RaidLeaderPrefsCP raidLeaderPrefsCP = new RaidLeaderPrefsCP(msg.getMentionedMembers().get(0), msg.getTextChannel());
            raidLeaderPrefsCP.createControlPanel();
            msg.delete().queue();
            return;
        }

        Member member = msg.getMember();
        TextChannel textChannel = msg.getTextChannel();
        RaidLeaderPrefsCP raidLeaderPrefsCP = new RaidLeaderPrefsCP(member, textChannel);
        raidLeaderPrefsCP.createControlPanel();
        if (Database.deleteMessages(msg.getGuild()))  msg.delete().queue();

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Raid Leader Preferences");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Raid Leader\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nAllows you to customize you AFK checks!" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
