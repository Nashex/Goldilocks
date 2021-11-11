package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import setup.SetupConnector;
import shatters.SqlConnector;
import utils.MemberSearch;
import utils.Utils;
import verification.VerificationHub;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static main.Goldilocks.eventWaiter;

public class CommandManualVerify extends Command {
    public CommandManualVerify() {
        setAliases(new String[] {"mverify","manualverify", "mv"});
        setEligibleRoles(new String[] {"tSec"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        TextChannel textChannel = msg.getTextChannel();

        if (args.length < 2) {
            Utils.errorMessage("Failed to Manually Verify User", "No name/id/@ or username was provided. Please use the command format: \"mverify <id/@> <realm-ign>\"", msg.getTextChannel(), 10L);
            return;
        }

        Member member = MemberSearch.memberSearch(args[0], msg.getGuild());

        if (member == null) {
            Utils.errorMessage("Failed to Manually Verify User", "No user with the @/id of: " + args[0] + " was found in the server.", msg.getTextChannel(), 10L);
            return;
        }

        if (member.getRoles().stream().map(Role::getId).anyMatch(s -> s.equals(SetupConnector.getFieldValue(msg.getGuild(), "guildInfo","verifiedRole")))) {
            Utils.errorMessage("Failed to Manually Verify User", "The user with the @/id of: " + args[0] + " is already verified.", msg.getTextChannel(), 10L);
            return;
        }

        String explusions = "";
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Successful Manual Verification for " + args[1])
                .setColor(Goldilocks.GREEN)
                .setDescription("**[" + args[1] + "](https://realmeye.com/player/" + args[1] + "):** " + member.getAsMention() + " | `" + member.getId() + "`");

        if (Database.isShatters(msg.getGuild())) {
            if (SqlConnector.unexpelUser(args[0].toLowerCase())) explusions += args[0] + "\n";
            if (SqlConnector.unexpelUser(args[1].toLowerCase())) explusions += args[1] + "\n";
            VerificationHub.verifyUser(member, msg.getMember(), args[1], (!msg.getAttachments().isEmpty() ? msg.getAttachments().get(0).getProxyUrl() : ""));
            msg.getTextChannel().sendMessage(embedBuilder.addField("Expulsions Removed", "```\n" + (explusions.isEmpty() ? "None" : explusions) + "\n```", false).build()).queue();
            return;
        }

        embedBuilder = new EmbedBuilder()
                .setColor(Goldilocks.GREEN)
                .setDescription("✅ Successfully verified " + member.getAsMention() + " under the name `" + args[1] + "`!")
                .setFooter("Verified by " + msg.getMember().getEffectiveName(), msg.getAuthor().getAvatarUrl());

        if (msg.getAttachments().size() == 0) {
            Message controlPanel = textChannel.sendMessage(noImageEmbed(args[1], member).build()).complete();
            controlPanel.addReaction("✅").queue();
            controlPanel.addReaction("❌").queue();
            EmbedBuilder finalEmbedBuilder = embedBuilder;
            eventWaiter.waitForEvent(MessageReactionAddEvent.class, e -> {
                return Objects.equals(e.getUser(), msg.getAuthor()) && ("✅❌").contains(e.getReactionEmote().getEmoji());
            }, e -> {

                String emote = e.getReactionEmote().getEmoji();
                if (("✅").equals(emote)) {
                    controlPanel.delete().queue();
                    String nameExpelled = Database.isExpelled(args[1].toLowerCase(), member.getGuild());
                    String idExpelled = Database.isExpelled(member.getId(), member.getGuild());

                    if (!nameExpelled.isEmpty() || !idExpelled.isEmpty()) {
                        Utils.confirmAction(controlPanel.getTextChannel(), msg.getAuthor(), "This user is currently expelled. Are you sure you would like to verify them?", () -> {
                            Database.expelRemove(member.getId(), member.getGuild());
                            Database.expelRemove(args[1].toLowerCase(), member.getGuild());
                            VerificationHub.verifyUser(member, msg.getMember(), args[1], "");
                            msg.getTextChannel().sendMessage(finalEmbedBuilder.build()).queue();
                        });
                    } else {
                        VerificationHub.verifyUser(member, msg.getMember(), args[1], "");
                        msg.getTextChannel().sendMessage(finalEmbedBuilder.build()).queue();
                    }

                }
                if (("❌").equals(emote)) {
                    controlPanel.delete().queue();
                }

            }, 2L, TimeUnit.MINUTES, () -> {
                controlPanel.delete().queue();
            });
            //Utils.errorMessage("Failed to Manually Verify User", "No verification image was provided.", msg.getTextChannel(), 10L);
            return;
        }

        String nameExpelled = Database.isExpelled(args[1].toLowerCase(), member.getGuild());
        String idExpelled = Database.isExpelled(member.getId(), member.getGuild());

        if (!nameExpelled.isEmpty() || !idExpelled.isEmpty()) {
            EmbedBuilder finalEmbedBuilder1 = embedBuilder;
            Utils.confirmAction(msg.getTextChannel(), msg.getAuthor(), "This user is currently expelled. Are you sure you would like to verify them?", () -> {
                Database.expelRemove(member.getId(), member.getGuild());
                Database.expelRemove(args[1].toLowerCase(), member.getGuild());
                VerificationHub.verifyUser(member, msg.getMember(), args[1], msg.getAttachments().get(0).getProxyUrl());
                msg.getTextChannel().sendMessage(finalEmbedBuilder1.build()).queue();
            });
        } else {
            VerificationHub.verifyUser(member, msg.getMember(), args[1], msg.getAttachments().get(0).getProxyUrl());
            msg.getTextChannel().sendMessage(embedBuilder.build()).queue();
        }


        //msg.delete().queueAfter(2L, TimeUnit.SECONDS);

    }

    private EmbedBuilder noImageEmbed(String username, Member member) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.setTitle("Are you sure you want to verify without an Image?")
                .setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("Player Tag: " + member.getAsMention() + " | In-game Name: " + username);
        embedBuilder.setTimestamp(new Date().toInstant());

        return embedBuilder;
    }


    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Case");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Security or Raid Leader\n";
        commandDescription += "Syntax: ;alias <caseId>\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nRetrieves the case file for a given case" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
