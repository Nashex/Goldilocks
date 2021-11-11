package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import main.Permissions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import moderation.NameChange;
import utils.MemberSearch;
import utils.Utils;

import java.util.Date;

public class CommandChangeName extends Command {
    public CommandChangeName() {
        setAliases(new String[] {"cn", "changename"});
        setEligibleRoles(new String[] {"verified"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.VERIFIED);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        Member mod = msg.getMember();

        if (mod == null) {
            Utils.errorMessage("Unable to Change Name", "The member was found to be null", msg.getTextChannel(), 15L);
            return;
        }

        //Check if they are a mod or a raider.
        if (args.length > 1) {
            if (Permissions.hasPermission(msg.getMember(), new String[]{"tSec"})) {
                Member member = MemberSearch.memberSearch(args[0], msg.getGuild());
                if (member != null) NameChange.modNameChange(mod, member, args[1], msg);
                else Utils.errorMessage("Unable to Change Name", "Could not find the user with the name/id " + args[0], msg.getTextChannel(), 15L);
                return;
            }
        }

        if (args.length > 0) {
            NameChange.changeName(msg.getMember(),  args[0], msg.getTextChannel());
        } else {
            msg.getTextChannel().sendMessage("Unable to change your name, please use the command as follows: `" + Database.getGuildPrefix(msg.getGuild().getId()) + "cn <new name>`").queue();
        }

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
        commandDescription += "\nAdds and alt to a given user." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
