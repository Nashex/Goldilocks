package commands.miscCommands;

import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import setup.SetupConnector;
import utils.Utils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CommandNoNick extends CommandRuns{
    public CommandNoNick() {
        setAliases(new String[] {"nonick", "fixnicks"});
        setEligibleRoles(new String[] {"officer"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        if (alias.equals("nonick")) {
            Guild guild = msg.getGuild();
            Role verified = guild.getRoleById("514790630816481301");
            List<Member> noNickMembers = guild.getMembers().stream().filter(member -> !member.getAsMention().contains("!") && member.getRoles().contains(verified)).collect(Collectors.toList());
            noNickMembers = noNickMembers.stream().filter(member -> member.getRoles().contains(verified)).collect(Collectors.toList());
            msg.getTextChannel().sendMessage("Total Users with No Nickname: " + noNickMembers.size()).queue();
            if (noNickMembers.size() > 50) {
                int place = 0;
                for (int i = 0; i < noNickMembers.size() / 50; i++) {
                    List<Member> members = noNickMembers.subList(place, place + 49);
                    String noNickString = "";
                    for (Member member : members) {
                        noNickString += member.getAsMention() + "\n";
                    }
                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.setColor(Goldilocks.BLUE);
                    embedBuilder.setTitle("People with no nick name in " + guild.getName());
                    embedBuilder.setDescription(noNickString);
                    embedBuilder.setTimestamp(new Date().toInstant());

                    msg.getTextChannel().sendMessage(embedBuilder.build()).queue();
                    place += 50;
                }
            } else {
                String noNickString = "";
                for (Member member : noNickMembers) {
                    noNickString += member.getAsMention() + "\n";
                }
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setColor(Goldilocks.BLUE);
                embedBuilder.setTitle("People with no nick name in " + guild.getName());
                embedBuilder.setDescription(noNickString);
                embedBuilder.setTimestamp(new Date().toInstant());

                msg.getTextChannel().sendMessage(embedBuilder.build()).queue();
            }
        } else if (alias.equals("fixnicks")) {
            Goldilocks.TIMER.execute(() -> {
                long timeStarted = System.currentTimeMillis();
                Guild guild = msg.getGuild();
                List<Member> noNickName = guild.getMembersWithRoles(Goldilocks.jda.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo", "verifiedRole")))
                        .stream().filter(member -> member.getNickname() == null).collect(Collectors.toList());
                EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("Fixing nicknames for: " + guild.getName())
                        .setColor(Goldilocks.RED)
                        .setDescription("Current Members with no nickname: `" + noNickName.size() + "`\n" +
                                Utils.renderPercentage(0.0f, 10) + " | " + String.format("%.2f", (float) 0 / noNickName.size()) + "%");

                Message progressMessage = msg.getTextChannel().sendMessage(embedBuilder.build()).complete();
                String nicks = "";
                int numChecked = 0;
                for (Member member : noNickName) {
                    List<Guild> userGuilds = member.getUser().getMutualGuilds().stream()
                            .filter(guild1 -> (Database.isPub(guild1) || SetupConnector.getFieldValue(guild1, "guildInfo", "rank").equals("3"))
                                    && guild1.getMember(member.getUser()).getNickname() != null).collect(Collectors.toList());
                    try {
                        if (!userGuilds.isEmpty()) {
                            String nickName = userGuilds.get(0).getMember(member.getUser()).getEffectiveName().replaceAll("[^A-Za-z |]","");
                            nickName = nickName.equals(member.getEffectiveName()) ? nickName.toLowerCase().equals(member.getEffectiveName()) ? StringUtils.capitalize(nickName) : nickName.toLowerCase() : nickName;
                            String log = member.getEffectiveName() + " ⇒ " + nickName + "**:** " + member.getAsMention() + "\n";
                            if (nickName.length() + log.length() < 1014) nicks += log;
                            guild.modifyNickname(member, nickName).complete();
                            if (numChecked % 5 == 0) progressMessage.editMessage(embedBuilder.setDescription("Current Members with no nickname: `" + noNickName.size() + "`\n" +
                                    Utils.renderPercentage((float) numChecked / noNickName.size(), 10) + " | " + String.format("%.2f", (float) (numChecked * 100) / noNickName.size()) + "%").build()).complete();
                        }
                    } catch (Exception ignored) { }
                    numChecked++;
                }

                List<Member> finishedNoNicks = guild.getMembersWithRoles(Goldilocks.jda.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo", "verifiedRole")))
                        .stream().filter(member -> member.getNickname() == null).collect(Collectors.toList());
                progressMessage.editMessage(embedBuilder
                        .setTitle("Finished Fixing nicknames for: " + guild.getName())
                        .setColor(Goldilocks.GREEN)
                        .setDescription("**Names Changed (" + (noNickName.size() - finishedNoNicks.size()) + "):**\n " + (nicks.isEmpty() ? "**None**" : nicks) + "\n" +
                                "**Left Over Members:** `" + finishedNoNicks.size() + "`\n" + Utils.renderPercentage(100.0f, 10) + " | 100%")
                        .setFooter("Time Taken: " + DurationFormatUtils.formatDurationWords(System.currentTimeMillis() - timeStarted, true, true))
                        .build()).queue();
            });
        }
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Ping");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Trial Security\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nChecks if the bot is online." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
