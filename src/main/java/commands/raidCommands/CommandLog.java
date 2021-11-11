package commands.raidCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import shatters.SqlConnector;
import utils.MemberSearch;
import utils.Utils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CommandLog extends Command {
    public CommandLog() {
        setAliases(new String[] {"log"});
        setEligibleRoles(new String[] {"arl","tSec"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.RAID);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        if (args.length == 0) {
            Utils.errorMessage("Unable to Add Assist to Member", "Please make sure to specify the user/users you would like to log an assist for.", msg.getTextChannel(), 10L);
            return;
        }
        if (args[0] == "s") {
            msg.getTextChannel().sendMessage("If you used Goldilocks for your afk check, your run was automatically logged. For more info type `.faq`");
            return;
        }

        int inc = 1;
        for (String s : args) {
            if (!s.replaceAll("[^0-9]", "").isEmpty() && s.length() < 5) inc = Integer.parseInt(s.replaceAll("[^0-9]", ""));
        }

        List<Member> memberList = MemberSearch.memberSearch(msg, args);
        if (memberList.isEmpty()) return;

        SqlConnector.logFieldForMembers(memberList, Arrays.asList(new String[]{"currentweekassists", "assists"}), inc);
        for (Member member : memberList) msg.getTextChannel().sendMessage(inc + " assist" + (inc == 1 ? "" : "s") + " logged for " + member.getEffectiveName() + ". Total assists: `" + (Integer.parseInt(SqlConnector.shattersStats(member.getUser())[7]) + inc) + "`.").queue();
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Log");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Trial Security or Almost Raid Leader\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nManually adds an assist to a user." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
