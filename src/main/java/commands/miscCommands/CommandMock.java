package commands.miscCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import utils.MemberSearch;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommandMock extends Command {

    public static List<Member> mockedMembers = new ArrayList<>();

    public CommandMock() {
        setAliases(new String[] {"mock"});
        setEligibleRoles(new String[] {"mod", "hrl"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.GAME);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        Member member;
        List<Member> memberList = MemberSearch.memberSearch(msg, args);
        msg.delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
        if (!memberList.isEmpty()) member = memberList.get(0);
        else {
            msg.getTextChannel().sendMessage("Unfortunately I cannot find the user specified.").queue();
            return;
        }

        if (member.getUser().isBot()) {
            msg.getTextChannel().sendMessage("Unfortunately I cannot mock bots.").queue();
            return;
        }

        if (mockedMembers.contains(member)) {
            mockedMembers.remove(member);
            msg.getTextChannel().sendMessage(member.getEffectiveName() + " is no longer being mocked.").queue();
        } else {
            mockedMembers.add(member);
            msg.getTextChannel().sendMessage(member.getEffectiveName() + " is now being mocked!").queue();
        }
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Mock");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Security, Almost Raid Leader, Event Organizer\n";
        commandDescription += "Syntax: ;word\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nMocks a given user. Use it to see what it does!" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
