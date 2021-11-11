package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import moderation.punishments.controlpanels.SuspensionCP;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import utils.MemberSearch;
import utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CommandSuspend extends Command {
    public CommandSuspend() {
        setAliases(new String[] {"suspend"});
        setEligibleRoles(new String[] {"rl","security"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        Guild guild = msg.getGuild();
        if (args.length < 2) {
            msg.delete().queue();
            Utils.errorMessage("Failed to Issue Suspension", "Not enough arguments to suspend!", msg.getTextChannel(), 10L);
            return;
        }

        List<Member> members = new ArrayList<>();
        int max = Math.min(args.length, 6);

        if (Arrays.stream(Arrays.copyOfRange(args, 0, max)).anyMatch(s -> !s.replaceAll("[^0-9]", "").isEmpty() && s.replaceAll("[^0-9]", "").length() < 3)) {
            for (int i = 0; i < max; i++) {
                if (!args[i].replaceAll("[^0-9]", "").isEmpty() && args[i].replaceAll("[^0-9]", "").length() < 3) {
                    args = Arrays.copyOfRange(args, i, args.length);
                    break;
                }
                Member m = MemberSearch.memberSearch(args[i], guild);
                if (m != null) members.add(m);
            }
        } else {
            Member m = MemberSearch.memberSearch(args[0], guild);
            if (m != null) members.add(m);
        }

        if (members.isEmpty()) {
            Utils.errorMessage("Failed to Issue Suspension", "Unable to find user", msg.getTextChannel(), 10L);
            return;
        }

        new SuspensionCP(msg, args, members.toArray(new Member[0]));

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Suspend");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Raid Leader or Security\n";
        commandDescription += "Syntax: ;alias <@/id/name/tag> [#(s/h/d)] <reason>\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nSuspends a given user. The time field is optional as it can be" +
                "changed after executing the command via the GUI.\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

}
