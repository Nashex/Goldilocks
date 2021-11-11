package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import main.Permissions;
import moderation.NameChange;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import utils.MemberSearch;
import utils.Utils;
import verification.AddAltRequest;
import verification.VerificationHub;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CommandAddAlt extends Command {
    public CommandAddAlt() {
        setAliases(new String[] {"alt", "addalt", "aa"});
        setEligibleRoles(new String[] {"verified"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.VERIFIED);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        //Check if they are a mod or a raider.
        Member mod = msg.getMember();

        if (Permissions.hasPermission(mod, new String[]{"tSec"})) {
            if (args.length < 2) {
                Utils.errorMessage("Unable to Add Alt", "Please use to command with the following syntax: addalt <name/id/@/tag> <alt name>", msg.getTextChannel(), 15L);
                return;
            }

            Member member = MemberSearch.memberSearch(args[0], msg.getGuild());
            if (member != null) {
                String name = member.getEffectiveName() + " | " + args[1];
                if (name.length() < 32) {
                    Member altMember = MemberSearch.memberSearch(args[1], msg.getGuild());
                    if (altMember != null) msg.getTextChannel().sendMessage("Unable to add alt, the member " + altMember.getAsMention() + " already has the name.").queue();
                    else NameChange.modAddAlt(mod, member, args[1], msg);
                } else {
                    Utils.errorMessage("Unable to add alt", "The new name would be over the discord length limit of 32 characters.", msg.getTextChannel(), 15L);
                }
            }
            else Utils.errorMessage("Unable to add alt", "Could not find the user with the name/id: " + args[0], msg.getTextChannel(), 15L);
            return;
        }

        // Will only go here if they are a raider
        Goldilocks.TIMER.schedule(() -> VerificationHub.addAltRequests.add(new AddAltRequest(msg.getMember())), 0L, TimeUnit.SECONDS);

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Add Alt");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Verified Raider\n";
        commandDescription += "Syntax: ;addalt\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nAdds and alt to a given user. For mods, use the command with the following " +
                "syntax to add an alt to a user, addalt <name/id/@/tag> <alt name>\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
