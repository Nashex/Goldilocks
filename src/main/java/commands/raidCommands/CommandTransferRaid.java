package commands.raidCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import main.Permissions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import raids.Raid;
import raids.RaidHub;
import utils.Utils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static main.Goldilocks.eventWaiter;

public class CommandTransferRaid extends Command {
    public CommandTransferRaid() {
        setAliases(new String[] {"traid", "transfer", "igivemeraidto"});
        setEligibleRoles(new String[] {"arl","eo"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.RAID);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        Raid raid = RaidHub.getRaid(msg.getMember());

        if (raid == null) {
            Utils.errorMessage("Failed to Transfer Raid", "Unable to find your raid. If you think this is wrong please ping Nashex#6969", msg.getTextChannel(), 10L);
            return;
        }

        if (msg.getMentionedMembers().isEmpty()) {
            Utils.errorMessage("Failed to Transfer Raid", "Please make sure to use the command in the following format: transfer @", msg.getTextChannel(), 10L);
            return;
        }

        TextChannel textChannel = msg.getTextChannel();
        Member owner = msg.getMember();
        Member recipient = msg.getMentionedMembers().get(0);

        if (!Permissions.hasPermission(recipient, new String[] {"arl", "eo"})) {
            Utils.errorMessage("Failed to Transfer Raid", "Please make sure that the recipient has permissions to start raids on their own.", msg.getTextChannel(), 10L);
            return;
        }

        msg.delete().queue();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Would you like to accept ownership of " + raid.getVoiceChannel().getName() + "?")
                .setColor(Goldilocks.GOLD)
                .setFooter("Please use ✅ and ❌ to answer.")
                .setTimestamp(new Date().toInstant());
        Message confirmationMessage = recipient.getUser().openPrivateChannel().complete().sendMessage(embedBuilder.build()).complete();
        confirmationMessage.addReaction("✅").queue();
        confirmationMessage.addReaction("❌").queue();

        eventWaiter.waitForEvent(MessageReactionAddEvent.class, e -> {
            return e.getUser().equals(recipient.getUser()) && confirmationMessage.getId().equals(e.getMessageId()) && ("✅❌").contains(e.getReactionEmote().getEmoji());
        }, e -> {

            String emoji = e.getReactionEmote().getEmoji();
            if (("✅").equals(emoji)) {
                raid.transferRaid(recipient);
            }
            if (("❌").equals(emoji)) {
                owner.getUser().openPrivateChannel().complete().sendMessage(recipient.getEffectiveName() + " has rejected ownership of you raid.");
            }

            confirmationMessage.delete().queue();

        }, 2L, TimeUnit.MINUTES, () -> {
            owner.getUser().openPrivateChannel().complete().sendMessage(recipient.getEffectiveName() + " has not responded to your raid transfer request within 2 minutes.");
        });
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Transfer Raid");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Transfer\n";
        commandDescription += "Syntax: ;transfer\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nTransfers the raid to the designated person." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
