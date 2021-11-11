package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import moderation.punishments.PunishmentManager;
import moderation.punishments.Suspension;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import utils.MemberSearch;
import utils.Utils;

import java.util.Date;
import java.util.List;

public class CommandUnsuspend extends Command {
    public CommandUnsuspend() {
        setAliases(new String[] {"unsuspend"});
        setEligibleRoles(new String[] {"rl","security"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        if (args.length < 1) {
            msg.delete().queue();
            Utils.errorMessage("Failed to Unsuspend User", "Not enough arguments to unsuspend!", msg.getTextChannel(), 10L);
            return;
        }
        String memberArgs[] = {args[0]};
        List<Member> memberSearch = MemberSearch.memberSearch(msg, memberArgs);

        if (memberSearch.isEmpty()) return;
        Suspension suspension = PunishmentManager.getSuspension(memberSearch.get(0));
        EmbedBuilder embedBuilder = new EmbedBuilder();

        if (suspension == null) {
            String suspendedRoleId = Database.getGuildInfo(msg.getGuild(), "suspendedRole");
            Role suspendedRole = msg.getGuild().getRoleById(suspendedRoleId);
            if (suspendedRole == null) return;
            if (memberSearch.get(0).getRoles().contains(suspendedRole)) {
                Utils.confirmAction(msg.getTextChannel(), msg.getAuthor(), "This user was not suspended by me, are you sure you would like to unsuspend them?", () -> {
                    msg.getGuild().removeRoleFromMember(memberSearch.get(0), suspendedRole).queue(m -> {
                        msg.getTextChannel().sendMessage(embedBuilder.setTitle("Successfully unsuspended " + memberSearch.get(0).getEffectiveName() + ", they will have to re-verify.").setColor(Goldilocks.BLUE).build()).queue();
                    });
                });
            } else msg.getTextChannel().sendMessage("This person is not currently suspended.").queue();
            return;
        };

        suspension.unsuspend(msg.getMember());
        embedBuilder.setTitle("Successfully unsuspended " + suspension.recipient.getEffectiveName());
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("**Initial Reason:**\n>>> " + suspension.reason);

        msg.getTextChannel().sendMessage(embedBuilder.build()).queue();

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: UnSuspend");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Raid Leader or Security\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nUnSuspends a given user.\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

}
