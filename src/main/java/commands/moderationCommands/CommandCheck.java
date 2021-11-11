package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import moderation.punishments.PunishmentManager;
import modmail.ModmailHub;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import setup.SetupConnector;
import verification.VerificationHub;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class CommandCheck extends Command {
    public CommandCheck() {
        setAliases(new String[] {"check"});
        setEligibleRoles(new String[] {"security", "hrl"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.SETUP);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        long timeStarted = System.currentTimeMillis();
        Guild guild = msg.getGuild();
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Check for " + guild.getName())
                .setColor(Goldilocks.LIGHTBLUE)
                .setTimestamp(new Date().toInstant());

        Message message = msg.getTextChannel().sendMessage(embedBuilder.setDescription("```\nCreating Check Report...\n```").build()).complete();

        //Check duplicate names
        HashMap<String, Member> existingNames = new HashMap<>();
        HashMap<Member, Member> duplicateNamesMap = new HashMap<>();
        //List<Member> invalidNames = new ArrayList<>();
        List<Member> currentMedals = new ArrayList<>();

        final int[] numDuplicates = {0};
        int numNoNicks = 0;

        guild.getMembers().stream().filter(member -> member.getNickname() != null)
                .forEach(member -> {
                    if (!member.getNickname().replaceAll("[^🎖🥇🥈🥉🏅]", "").isEmpty()) currentMedals.add(member);
                    String nickname = member.getNickname().toLowerCase().replaceAll("[^A-Za-z|]","");
                    //if ((nickname.split(" ").length + nickname.split(" ").length * 2) != nickname.split("[|]").length && invalidNames.size() <= 25) invalidNames.add(member);
                    for (String s : nickname.split("[|]")) {
                        if (!existingNames.containsKey(s))
                            existingNames.put(s, member);
                        else {
                            numDuplicates[0]++;
                            if (!duplicateNamesMap.containsValue(member) && duplicateNamesMap.size() <= 10) duplicateNamesMap.put(member, existingNames.get(s));
                        }
                    }
                });

        List<Member> noNickName = guild.getMembersWithRoles(Goldilocks.jda.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo", "verifiedRole")))
                .stream().filter(member -> member.getNickname() == null).collect(Collectors.toList());

        String duplicateNamesString = duplicateNamesMap.entrySet().stream().map(e -> e.getKey().getAsMention() + "⇋" + e.getValue().getAsMention()).collect(Collectors.joining(", "));
        String noNickNamesString = noNickName.stream().map(m -> m.getAsMention()).collect(Collectors.joining(", "));
        //String invalidNamesString = invalidNames.stream().map(m -> m.getAsMention()).collect(Collectors.joining(", "));
        String medalsString = currentMedals.stream().map(m -> m.getAsMention()).collect(Collectors.joining(", "));
        String pendingVerificationModules = VerificationHub.newManualVerificationRequests.stream()
                .filter(r -> r.member.getGuild().equals(guild)).map(r -> "**[Module](" + r.message.getJumpUrl() + ")**").collect(Collectors.joining(", "));
        String openModmailChannels = ModmailHub.activeModmails.entrySet().stream()
                .filter(e -> e.getValue().getGuild().equals(guild))
                .map(e -> e.getValue().getAsMention()).collect(Collectors.joining(", "));
        String pendingAltRequests = VerificationHub.addAltRequests.stream().filter(r -> r.getMember().getGuild().equals(guild))
                .map(r -> "**[Module](" + r.getMessage().getJumpUrl() + ")**").collect(Collectors.joining(", "));
        String pendingManualVerifications = VerificationHub.manualVerificationRequests.stream().filter(r -> r.getMember().getGuild().equals(guild) && r.getVerificationControlPanel() != null)
                .map(r -> "**[Module](" + r.getVerificationControlPanel().getJumpUrl() + ")**").collect(Collectors.joining(", "));


        String suspensions = guild.getMembersWithRoles(guild.getRoles().stream().filter(role -> role.getName().toLowerCase().contains("suspended")).collect(Collectors.toList()))
                .stream().filter(member -> PunishmentManager.getSuspension(member) == null).map(Member::getAsMention).collect(Collectors.joining(", "));
        String mutes = guild.getMembersWithRoles(guild.getRoles().stream().filter(role -> role.getName().toLowerCase().contains("mute")).collect(Collectors.toList()))
                .stream().filter(member -> PunishmentManager.getMute(member) == null).map(Member::getAsMention).collect(Collectors.joining(", "));

        embedBuilder.clear()
                .setTitle("Check for " + guild.getName())
                .setColor(Goldilocks.WHITE)
                .addField("Duplicate Names (" + numDuplicates[0] + ")", duplicateNamesString.isEmpty() ? "None" : duplicateNamesString, false)
                //.addField("Invalid Names", invalidNamesString.isEmpty() ? "None" : invalidNamesString, false)
                .addField("No Nicknames (" + noNickName.size() + ")", noNickNamesString.isEmpty() ? "None" : (noNickNamesString.length() > 1024 ? noNickNamesString.substring(0, 1024) : noNickNamesString), false)
                .addField("Users with Medals", medalsString.isEmpty() ? "None" : medalsString, false)
                .addField("Pending Verification Modules", pendingVerificationModules.isEmpty() ? "None" : pendingVerificationModules, false)
                .addField("Open Modmail Channels", openModmailChannels.isEmpty() ? "None" : openModmailChannels, false)
                .addField("Pending Alt Requests", pendingAltRequests.isEmpty() ? "None" : pendingAltRequests, false)
                .addField("Pending Manual Verification Requests", pendingManualVerifications.isEmpty() ? "None" : pendingManualVerifications, false)
                .setFooter("Checked in " + String.format("%.2f", (System.currentTimeMillis() - timeStarted) / 1000.0) + " seconds")
                .setTimestamp(new Date().toInstant());

        if (!Database.isShatters(guild)) {
            embedBuilder.addField("False Suspensions", suspensions.isEmpty() ? "None" : (suspensions.length() > 1024 ? suspensions.substring(0, 1024) : suspensions), false)
                    .addField("False Mutes", mutes.isEmpty() ? "None" : (mutes.length() > 1024 ? mutes.substring(0, 1024) : mutes) , false);
        }

        message.editMessage(embedBuilder.build()).queue();

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Check");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Security\n";
        commandDescription += "Syntax: ;check <command alias>\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nChecks basic info for the server." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
