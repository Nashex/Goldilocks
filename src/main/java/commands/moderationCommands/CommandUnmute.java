package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import moderation.punishments.Mute;
import moderation.punishments.PunishmentManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import utils.MemberSearch;
import utils.Utils;

import java.util.Date;
import java.util.List;

public class CommandUnmute extends Command {
    public CommandUnmute() {
        setAliases(new String[] {"unmute"});
        setEligibleRoles(new String[] {"rl","security"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        if (args.length < 1) {
            msg.delete().queue();
            Utils.errorMessage("Failed to Unmute User", "Not enough arguments to unmute!", msg.getTextChannel(), 10L);
            return;
        }
        String[] memberArgs = {args[0]};
        List<Member> memberSearch = MemberSearch.memberSearch(msg, memberArgs);

        if (memberSearch.isEmpty()) return;
        Mute mute = PunishmentManager.getMute(memberSearch.get(0));
        EmbedBuilder embedBuilder = new EmbedBuilder();

        if (mute == null) {
            String mutedRoleId = Database.getGuildInfo(msg.getGuild(), "mutedRole");
            Role mutedRole = msg.getGuild().getRoleById(mutedRoleId);
            if (mutedRole == null) return;
            if (memberSearch.get(0).getRoles().contains(mutedRole)) {
                Utils.confirmAction(msg.getTextChannel(), msg.getAuthor(), "This user was not muted by me, are you sure you would like to unmute them?", () -> {
                    msg.getGuild().removeRoleFromMember(memberSearch.get(0), mutedRole).queue(m -> {
                        msg.getTextChannel().sendMessage(embedBuilder.setTitle("Successfully unmuted " + memberSearch.get(0).getEffectiveName()).setColor(Goldilocks.BLUE).build()).queue();
                    });
                });
            }
            else msg.getTextChannel().sendMessage("This person is not currently muted.").queue();
            return;
        };

        mute.unmute(msg.getMember());
        embedBuilder.setTitle("Successfully unmuted " + mute.recipient.getEffectiveName());
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("**Initial Reason:**\n>>> " + mute.reason + "");

        msg.getTextChannel().sendMessage(embedBuilder.build()).queue();

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Unmute");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Raid Leader or Security\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nUnmutes a given user.\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
