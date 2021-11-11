package commands.miscCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import main.Permissions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.apache.commons.lang3.time.DurationFormatUtils;
import quota.LogField;
import shatters.SqlConnector;
import utils.Charts;
import utils.MemberSearch;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommandStats extends Command {
    public CommandStats() {
        setAliases(new String[] {"stats"});
        setEligibleRoles(new String[] {"verified"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.VERIFIED);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        Member member = msg.getMember();
        if (Permissions.hasPermission(msg.getMember(), new String[] {"arl", "tSec", "headEo"}) && args.length > 0) {
            List<Member> memberList = MemberSearch.memberSearch(msg, args);
            if (!memberList.isEmpty()) member = memberList.get(0);
            else return;
        }

        if (!Database.isShatters(msg.getGuild())) {
            msg.getAuthor().openPrivateChannel().complete().sendMessage(statsEmbed(msg, member).build()).queue(message -> msg.addReaction("✅").submit().exceptionally(t -> {
                msg.addReaction("❓").queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
                return null;
            }));
        } else {
            Member finalMember = member;
            Goldilocks.TIMER.schedule(() -> msg.getAuthor().openPrivateChannel().complete().sendMessage(shattersStatsEmbed(msg, finalMember).build()).queue(message -> msg.addReaction("✅").queue()), 0L, TimeUnit.SECONDS);
        }
        //textChannel.sendMessage(statsEmbed.build()).queue();

    }

    private EmbedBuilder statsEmbed(Message msg, Member member) {
        Guild guild = msg.getGuild();
        String memberName = member.getEffectiveName().replaceAll("[^A-Za-z]", "");
        EmbedBuilder statsEmbed = new EmbedBuilder();
        statsEmbed.setTitle(memberName + "'s Stats in " + guild.getName())
                .setColor(Goldilocks.BLUE);

        int runCompletes = Database.getRunsCompleted(member.getId(), guild.getId());
        int keysPopped = Database.getKeysPopped(member.getId(), guild.getId());
        String eventTime = DurationFormatUtils.formatDuration(Database.getEventTime(member.getId(), msg.getGuild().getId()), "H' hours 'm' mins'", false);
        final String[] roleString = {""};
        member.getRoles().forEach(role -> roleString[0] += ", " + role.getName());
        roleString[0] = roleString[0].replaceFirst(", ", "");

        statsEmbed.addField("Stats", "Below are the statistics for the amount of runs you have partaken in.", false)
                .addField("Run Completes", "```\n" + runCompletes + "\n```", true)
                .addField("Key Pops", "```\n" + keysPopped + "\n```", true)
                .addField("Event Time", "```\n" + eventTime + "\n```", true);

        if (Database.staffExists(msg.getAuthor().getId(), msg.getGuild().getId())) {
            statsEmbed.addField("Overall Stats", "Below are the statistics for your time as staff.", false)
                    .addField("Runs Lead", "```\n" + Database.getStaffData(member, "totalRunsLed") + "\n```", true)
                    .addField("Assists", "```\n" + Database.getStaffData(member, "totalAssists") + "\n```", true)
                    .addField("Parses", "```\n" + Database.getStaffData(member, "totalParses") + "\n```", true)
                    .addField("Weekly Stats", "Below are your statistics for this week.", false)
                    .addField("Runs", "```\n" + Database.getStaffData(member, "quotaRuns") + "\n```", true)
                    .addField("Assists", "```\n" + Database.getStaffData(member, "quotaAssists") + "\n```", true)
                    .addField("Parses", "```\n" + Database.getStaffData(member, "quotaParses") + "\n```", true);
                    //.setImage(Charts.createUserChart(member));
        }

        try {
            List<List<LogField>> fields = SqlConnector.getUserRuns(member.getId(), member.getGuild().getId());
            if (fields != null) statsEmbed.setImage(Charts.createBarChart(fields, false));
        } catch (Exception e) { }

        statsEmbed.setFooter("Stats for " + guild.getName() + " as of ", member.getUser().getAvatarUrl());
        statsEmbed.setTimestamp(new Date().toInstant());
        return statsEmbed;
    }

    private EmbedBuilder shattersStatsEmbed(Message msg, Member member) {
        Guild guild = msg.getGuild();
        String memberName = member.getEffectiveName().replaceAll("[^A-Za-z]", "");
        EmbedBuilder statsEmbed = new EmbedBuilder();
        statsEmbed.setTitle(memberName + "'s Stats in " + guild.getName())
                .setColor(Goldilocks.BLUE);

        String[] stats = SqlConnector.shattersStats(member.getUser());

        long eventTimeLong = Integer.parseInt(stats[3]) * 600000L;
        String eventTime = DurationFormatUtils.formatDuration(eventTimeLong, "H' hours 'm' mins'", false);

        statsEmbed.addField("Stats", "Below are the statistics for the amount of runs you have partaken in.", false)
                .addField("Run Completes", "```\n" + stats[0] + "\n```", true)
                .addField("Key Pops", "```\n" + stats[1] + "(S) " + stats[2] + "(E)" + "\n```", true)
                .addField("Event Time", "```\n" + eventTime + "\n```", true);

        if (Permissions.hasPermission(member, new String[] {"arl", "eo", "tSec"})) {
            statsEmbed.addField("Overall Stats", "Below are the statistics for your time as staff.", false)
                    .addField("Runs Lead", "```\n" + stats[4] + "\n```", true)
                    .addField("Assists", "```\n" +  stats[5] + "\n```", true)
                    .addField("Parses", "```\n" + Database.getStaffData(member, "totalParses") + "\n```", true)
                    .addField("Weekly Stats", "Below are your statistics for this week.", false)
                    .addField("Runs", "```\n" + stats[6] + "\n```", true)
                    .addField("Assists", "```\n" + stats[7] + "\n```", true)
                    .addField("Parses", "```\n" + Database.getStaffData(member, "quotaParses") + "\n```", true);
        }

        try {
            List<List<LogField>> fields = SqlConnector.getUserRuns(member.getId(), member.getGuild().getId());
            if (fields != null) statsEmbed.setImage(Charts.createBarChart(fields, false));
        } catch (Exception e) { }

        statsEmbed.setFooter("Stats for " + guild.getName() + " as of ", member.getUser().getAvatarUrl());
        statsEmbed.setTimestamp(new Date().toInstant());
        return statsEmbed;
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Runs");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Verified\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nShows how many runs a user has participated in." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
