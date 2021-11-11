package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import setup.SetupConnector;
import utils.MemberSearch;
import utils.Utils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static main.Goldilocks.eventWaiter;

public class CommandAddRole extends Command {
    public CommandAddRole() {
        setAliases(new String[] {"addrole", "ar", "exverify", "eventverify"});
        setEligibleRoles(new String[] {"security", "hrl", "headEo"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        TextChannel textChannel = msg.getTextChannel();

        if (args.length == 0) {
            Utils.errorMessage("Failed to Add Role", "Please use the command in the following format: addrole <name/@/id>", textChannel, 10L);
            return;
        }

        List<Member> members = MemberSearch.memberSearch(msg, args);
        Guild guild = msg.getGuild();
        Member punisher = msg.getMember();

        msg.delete().queue();

        if (members.isEmpty()) {
            return;
        }

        Member member = members.get(0);

        List<Role> roleList = Database.getRaiderRoles(guild).stream()
                .filter(role -> !member.getRoles().contains(role)).distinct().collect(Collectors.toList());
        if (msg.getMember().getRoles().contains(Goldilocks.jda.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo", "headEoRole")))) roleList.add(Goldilocks.jda.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","eoRole")));

        if (roleList.isEmpty()) {
            Utils.errorMessage("Failed to Add Role", "User already has all of the roles you can currently add.", textChannel, 10L);
            return;
        }

        String roleDescription = "";
        int index = 0;
        for (Role role : roleList) {
            roleDescription += Goldilocks.numEmotes[index + 1] + ": " + role.getName() + "\n";
            index++;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Add Role to " + member.getEffectiveName())
                .setColor(Goldilocks.WHITE)
                .setDescription("Please select one of the following roles by reacting to it's corresponding emote:\n" + roleDescription)
                .setFooter(punisher.getEffectiveName() + " is adding roles to " + member.getEffectiveName());

        Message controlPanel = textChannel.sendMessage(embedBuilder.build()).complete();
        roleList.forEach(role -> controlPanel.addReaction(Goldilocks.numEmotes[roleList.indexOf(role) + 1]).queue());
        controlPanel.addReaction("❌").queue();

        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(controlPanel.getId()) && e.getReactionEmote().isEmoji() && (Arrays.asList(Goldilocks.numEmotes).contains(e.getReactionEmote().getEmoji()) ||
                    ("❌").contains(e.getReactionEmote().getEmoji())) && e.getMember().equals(punisher);
        }, e -> {
            String emoji = e.getReactionEmote().getEmoji();

            if (("❌").equals(emoji)) {
                controlPanel.delete().queue();
                return;
            }

            int choice = Arrays.asList(Goldilocks.numEmotes).indexOf(emoji) - 1;
            Role role = roleList.get(choice);
            guild.addRoleToMember(member, role).queue();

            EmbedBuilder embedBuilder1 = new EmbedBuilder();
            embedBuilder1.setTitle("Successfully added role to " + member.getEffectiveName())
                    .setDescription("**Role Added: ** " + role.getAsMention() + " | **User Tag:** " + member.getAsMention())
                    .setColor(Goldilocks.WHITE)
                    .setFooter("Role added by: " + punisher.getEffectiveName())
                    .setTimestamp(new Date().toInstant());

            controlPanel.clearReactions().queue();
            controlPanel.editMessage(embedBuilder1.build()).queue();

            TextChannel logChannel;
            try {
                logChannel = guild.getTextChannelsByName("verification-logs", true).get(0);
                logChannel.sendMessage(embedBuilder1.build()).queue();
            } catch (Exception e2) {
                return;
            }


        }, 2L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
        });

    }


    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Add Role");
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
