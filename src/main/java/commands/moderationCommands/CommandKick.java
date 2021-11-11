package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import setup.SetupConnector;
import utils.MemberSearch;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommandKick extends Command {
    public CommandKick() {
        setAliases(new String[] {"kick"});
        setEligibleRoles(new String[] {"officer","hrl"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        Guild guild = msg.getGuild();
        TextChannel textChannel = msg.getTextChannel();
        if (!guild.getSelfMember().getPermissions().contains(Permission.KICK_MEMBERS)) {
            textChannel.sendMessage("I do not have permission to kick members.").queue();
            return;
        }

        if (args.length < 1) {
            textChannel.sendMessage("Please use the command with the following format: " + Database.getGuildPrefix(guild.getId()) + "kick <@/id/tag/name> <reason>").queue();
            return;
        }

        List<Member> members = MemberSearch.memberSearch(msg, new String[]{args[0]});
        if (members.isEmpty()) return;

        Member member = members.get(0);
        //Prompt for kick
        if (getHighestRolePos(member) >= getHighestRolePos(msg.getMember())) {
            textChannel.sendMessage("You do not have permission to kick this member.").queue();
            return;
        }
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (args.length > 1 && args[1].toLowerCase().equals("duplicate"))
            reason = "Your account was claimed by someone else, if you believe this to be a mistake please join " + guild.getName() + " with this link " + guild.retrieveVanityInvite().complete().getUrl() + " and message a security+ to help you.";

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setColor(Goldilocks.LIGHTBLUE)
                .setTitle("Are you sure you would like to kick this user?")
                .setDescription("**User Tag: **" + member.getAsMention() + "\n```\n" + (reason.isEmpty() ? "None Provided" : reason) + "\n```");
        String finalReason = reason;
        textChannel.sendMessage(embedBuilder.build()).queue(m -> {
            m.addReaction("✅").queue();
            m.addReaction("❌").queue();

            Goldilocks.eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
                return e.getMessageId().equals(m.getId()) && e.getUser().equals(msg.getAuthor()) && e.getReactionEmote().isEmoji() &&
                        ("✅❌").contains(e.getReactionEmote().getEmoji());
            }, e -> {
                String emote = e.getReactionEmote().getEmoji();
                if (("✅").equals(emote)) {
                    if (!SetupConnector.getFieldValue(guild, "guildLogs", "modLogChannelId").equals("0")) {
                        TextChannel logChannel = Goldilocks.jda.getTextChannelById(SetupConnector.getFieldValue(guild, "guildLogs", "modLogChannelId"));
                        if (logChannel != null) logChannel.sendMessage(new EmbedBuilder().setTitle("User Kicked From Guild")
                                .setColor(Goldilocks.RED)
                                .addField("User Information", member.getAsMention() + " | `" + member.getId() + "`", true)
                                .addField("Mod Information", msg.getMember().getAsMention() + " | `" + msg.getAuthor().getId() + "`", true)
                                .addField("Reason", "```\n" + (finalReason.isEmpty() ? "None Provided" : finalReason) + "\n```", false)
                                .setTimestamp(new Date().toInstant()).build()).queue();
                    }
                    guild.kick(member, finalReason).queue(a -> m.editMessage(embedBuilder.setTitle("User Successfully Kicked").build()).queue());
                } else {
                    m.editMessage(embedBuilder.setTitle("Kick request Cancelled").build()).queue();
                }
                m.clearReactions().queue();

            }, 2L, TimeUnit.MINUTES, () -> m.delete().queue());

        });
    }

    private int getHighestRolePos(Member member) {
        int position = -1;
        for (Role role : member.getRoles()) {
            if (role.getPosition() > position) position = role.getPosition();
        }
        return position;
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Warn");
        embedBuilder.addField("Required rank", "Raid Leader or Trial Security", false);
        embedBuilder.addField("Syntax", "-warn [@user], <reason>", false);
        embedBuilder.addField("Aliases", aliases, false);
        embedBuilder.addField("Information", "Warns a discord user", false);
        return embedBuilder;
    }
}
