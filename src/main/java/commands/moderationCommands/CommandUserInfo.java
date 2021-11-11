package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import moderation.PunishmentConnector;
import moderation.punishments.Note;
import moderation.punishments.Suspension;
import moderation.punishments.Warning;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import shatters.SqlConnector;
import utils.MemberSearch;
import utils.Utils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CommandUserInfo extends Command {
    public CommandUserInfo() {
        setAliases(new String[] {"uinfo","userinfo", "ui"});
        setEligibleRoles(new String[] {"arl","tSec", "eo"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        if (args.length == 0) {
            Utils.errorMessage("Failed to retrieve User Info", "No name/id/@ was provided", msg.getTextChannel(), 10L);
            return;
        }

        //Guild guild = Goldilocks.jda.getGuildById("514788290809954305");
        TextChannel textChannel = msg.getTextChannel();
        List<Member> memberList = MemberSearch.memberSearch(msg, args);

        if (memberList.isEmpty()) {
            return;
        }

        for (Member member : memberList) {
            Goldilocks.TIMER.schedule(() -> {
                textChannel.sendMessage(userInfoEmbed(member, msg.getMember()).build()).queue();
            }, 0L, TimeUnit.SECONDS);
        }

    }

    public static EmbedBuilder userInfoEmbed (Member member, Member executer) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        String suspensionString = "";
        String warningString = "";
        String noteString = "";

        List<Suspension> suspensionList = PunishmentConnector.getSuspensions(member);
        if (Database.isShatters(member.getGuild())) suspensionList.addAll(SqlConnector.getSuspensions(member));
        for (Suspension suspension : suspensionList) suspensionString += suspension.toString() + "\n";
        if (suspensionString.length() > 1000) suspensionString = suspensionString.substring(0, 1000);

        List<Warning> warningList = PunishmentConnector.getWarnings(member);
        for (Warning warning : warningList) warningString += warning.toString() + "\n";
        if (warningString.length() > 1000) warningString = warningString.substring(0, 1000);
        if (Database.isShatters(member.getGuild())) warningList.addAll(SqlConnector.getWarnings(member));

        List<Note> memberNotes = PunishmentConnector.getNotes(member);
        if (memberNotes.isEmpty()) noteString = "";
        else noteString = memberNotes.stream().map(Note::toString).collect(Collectors.joining("\n\n"));
        if (noteString.length() > 1000) noteString = noteString.substring(0, 1000);

        Role suspendedRole = member.getGuild().getRolesByName("suspended", true).get(0);
        String onGoingPunishments = "";
        if (member.getRoles().contains(suspendedRole)) {
            onGoingPunishments += " Suspended";
        }

        String[] stats = {"0", "0"};
        if (Database.isShatters(member.getGuild())) stats = SqlConnector.shattersStats(member.getUser());

        onGoingPunishments.replaceFirst(" ", "");

        embedBuilder.setTitle("User info for " + member.getEffectiveName())
                .setThumbnail(member.getUser().getAvatarUrl())
                .setColor(Goldilocks.BLUE)
                .setTimestamp(new Date().toInstant());
        //Todo check for punishment state
        embedBuilder.setDescription("```\n" + (onGoingPunishments.isEmpty() ? "This user has no ongoing punishments" : "Active Punishments:" + onGoingPunishments) + "\n```");
        embedBuilder.addField("Name:", "```\n" + member.getEffectiveName() + "\n```", true);
        //embedBuilder.addField("ID:", "```\n" + member.getId() + "\n```", true);
        embedBuilder.addField("Voice State", member.getVoiceState().inVoiceChannel() ? "```\n" + member.getVoiceState().getChannel().getName() + "\n```" : "```\nNot in a VC \n```", true);
        embedBuilder.addField("Mention:", "```md\n<@" + member.getId() + ">\n```", false);
        String runCompletes = String.valueOf(Database.getRunsCompleted(member.getId(), member.getGuild().getId()));
        if (Database.isShatters(member.getGuild())) runCompletes = stats[0];
        embedBuilder.addField("Runs Completed:", "```\n" + runCompletes + "\n```", true);
        String keysPopped = String.valueOf(Database.getKeysPopped(member.getId(), member.getGuild().getId()));
        if (Database.isShatters(member.getGuild())) keysPopped = stats[1];
        embedBuilder.addField("Keys Popped:", "```\n" + keysPopped + "\n```", true);
        embedBuilder.addField("Time Joined", "```\n" + member.getTimeJoined().getYear() + "-" + member.getTimeJoined().getMonth() + "-" + member.getTimeJoined().getDayOfMonth() + "\n```", true);
        String memberRoles = "";
        for (Role role : member.getRoles()) memberRoles += ", " + role.getName();
        memberRoles = memberRoles.replaceFirst(", ","");
        embedBuilder.addField("Roles", "```\n" + (memberRoles.isEmpty() ? "This user is not verified" : memberRoles) + "\n```", false);
        //Todo link to punishment db
        embedBuilder.addField("Warnings:", "```\n" + (warningList.isEmpty() ? "None" : warningString) + "\n```", false);
        embedBuilder.addField("Suspensions:", "```\n" + (suspensionList.isEmpty() ? "None" : suspensionString) + "\n```", false);
        //Todo show bans if they were banned
        embedBuilder.addField("Staff Notes:", "```\n" + (noteString.isEmpty() ? "None" : noteString) + "\n```", false);
        embedBuilder.setFooter("Searched by " + executer.getEffectiveName(), executer.getUser().getAvatarUrl());
        return embedBuilder;
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: User Info");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Trial Security or Raid Leader\n";
        commandDescription += "Syntax: ;alias <@/id/name>\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nRetrieves the info of a user for a given server." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

}
