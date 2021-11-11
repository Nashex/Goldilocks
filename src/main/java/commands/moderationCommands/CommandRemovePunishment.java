package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import moderation.PunishmentConnector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import utils.Utils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static main.Goldilocks.eventWaiter;

public class CommandRemovePunishment extends Command {
    public CommandRemovePunishment() {
        setAliases(new String[] {"remove", "removepunishment"});
        setEligibleRoles(new String[] {"mod"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        TextChannel textChannel = msg.getTextChannel();
        Member member = msg.getMember();

        if (args.length == 0) {
            Utils.errorMessage("Failed to Remove Punishment", "No case id provided", textChannel, 10L);
            msg.delete().queue();
            return;
        }

        if (!PunishmentConnector.caseExists(args[0], msg.getGuild().getId())) {
            Utils.errorMessage("Failed to Remove Punishment", "A case does not exist for the following case id: " + args[0], textChannel, 10L);
            msg.delete().queue();
            return;
        }

        msg.delete().queue();

        String punishmentInfo = PunishmentConnector.getPunishmentInfo(args[0], msg.getGuild().getId());

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Are you sure you want to remove this punishment?")
                .setDescription("```\n" + punishmentInfo + "\n```")
                .setFooter("Use ✅ or ❌ to select your answer")
                .setTimestamp(new Date().toInstant());
        embedBuilder.setColor(Goldilocks.BLUE);

        Message confirmationMessage = textChannel.sendMessage(embedBuilder.build()).complete();
        confirmationMessage.addReaction("✅").queue();
        confirmationMessage.addReaction("❌").queue();

        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getReactionEmote().isEmoji() && ("✅❌").contains(e.getReactionEmote().getEmoji()) && e.getMember().equals(member)
                    && e.getMessageId().equals(confirmationMessage.getId());
        }, e -> {
            String emote = e.getReactionEmote().getEmoji();

            if (("✅").equals(emote)) {
                PunishmentConnector.deletePunishment(args[0], msg.getGuild().getId());
                confirmationMessage.clearReactions().queue();
                confirmationMessage.editMessage(embedBuilder.setTitle("Successfully Removed Punishment with CaseId: " + args[0])
                        .setFooter("This punishment was removed by " + member.getEffectiveName()).build()).queue();
            }

            if (("❌").equals(emote)) {
                confirmationMessage.delete().queue();
            }

        }, 2L, TimeUnit.MINUTES, () -> {
            confirmationMessage.delete().queue();
        });




    }


    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Remove Punishment");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Mod\n";
        commandDescription += "Syntax: ;alias <caseId>\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nRemoves the punishment for a given caseid" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
