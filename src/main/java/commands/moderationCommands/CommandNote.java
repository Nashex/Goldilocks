package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import moderation.punishments.controlpanels.NoteCP;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import utils.MemberSearch;
import utils.Utils;

import java.util.Arrays;
import java.util.List;

public class CommandNote extends Command {
    public CommandNote() {
        setAliases(new String[] {"note", "addnote", "newt"});
        setEligibleRoles(new String[] {"tSec"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        if (args.length < 2) {
            msg.delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            Utils.errorMessage("Failed to Add Note", "Not enough arguments to note!\nFormat: note <@/id/name> <reason>", msg.getTextChannel(), 10L);
            return;
        }

        String[] memberArgs = {args[0]};
        List<Member> memberSearch = MemberSearch.memberSearch(msg, memberArgs);
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (!memberSearch.isEmpty()) {
            new NoteCP(memberSearch.get(0), msg.getMember(), reason, msg.getTextChannel());
        } else  Utils.errorMessage("Failed to Add Note", "Could not find the provided user!", msg.getTextChannel(), 10L);
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Note");
        embedBuilder.addField("Required rank", "Almost Raid Leader or Trial Security", false);
        embedBuilder.addField("Syntax", "-note [@user], <reason>", false);
        embedBuilder.addField("Aliases", aliases, false);
        embedBuilder.addField("Information", "Adds a note to a discord user", false);
        return embedBuilder;
    }
}
