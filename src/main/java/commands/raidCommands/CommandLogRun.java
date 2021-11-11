package commands.raidCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import sheets.GoogleSheets;
import utils.MemberSearch;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class CommandLogRun extends Command {
    public CommandLogRun() {
        setAliases(new String[] {"logrun", "addrun"});
        setEligibleRoles(new String[] {"arl", "eo"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        TextChannel textChannel = msg.getTextChannel();
        if (args.length == 0) {
            textChannel.sendMessage("Please use the command with the following syntax: `.log <# of runs> [Optional assists]`").queue();
            return;
        }

        int numAssists = 1;
        List<Member> members = new ArrayList<>();
        for (String s : args) {
            if (s.replaceAll("[^0-9]", "").length() <= 2) numAssists = Integer.parseInt(s.replaceAll("[^0-9]", ""));
            else {
                Member member = MemberSearch.memberSearch(s, msg.getGuild());
                if (member != null && !members.contains(member)) members.add(member);
                else textChannel.sendMessage("Unfortunately I could not find a member with the following name/id `" + s + "`").queue();
            }
        }

        if (!members.isEmpty()) {
            Database.addAssists(members);
            members.forEach(a -> {
                GoogleSheets.logEvent(msg.getGuild(), GoogleSheets.SheetsLogType.ASSISTS, a.getEffectiveName(), a.getId(), Objects.requireNonNull(msg.getMember()).getEffectiveName(), msg.getMember().getId());
                Database.logEvent(a, Database.EventType.ASSIST, System.currentTimeMillis() / 1000, textChannel, "assist");
                textChannel.sendMessage("Successfully added an assist to " + a.getEffectiveName() + "!").queue();
            });
        }



    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Log Run");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Raid Leader\n";
        commandDescription += "Syntax: ;lock <command alias>\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nUnlocks a Voice channel" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
