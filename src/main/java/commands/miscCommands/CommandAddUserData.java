package commands.miscCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import verification.PlayerProfile;
import verification.UserDataConnector;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CommandAddUserData extends Command {

    public CommandAddUserData() {
        setAliases(new String[] {"userdata"});
        setEligibleRoles(new String[] {"developer"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.DEVELOPER);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        Guild guild = Goldilocks.jda.getGuildById("514788290809954305");
        TextChannel textChannel = msg.getTextChannel();

        Random random = new Random();

        if (args.length < 1) return;

        if (msg.isFromGuild()) {
            try {

                DecimalFormat df = new DecimalFormat("0.00");

                double mainPercentage = Math.abs(UserDataConnector.getPlayerPercentage(new PlayerProfile(args[0]), false)) * 100 - .01;
                double altPercentage = Math.abs(UserDataConnector.getPlayerPercentage(new PlayerProfile(args[0]), true)) * 100 + .01;

                textChannel.sendMessage("__**Account Stats for " + args[0] + "**__\n**Main Percentage Chance: **" + df.format(mainPercentage) + "%\n**Alt Chance: **" + df.format(altPercentage) + "%").queue();
            } catch (PlayerProfile.PrivateProfileException e) {
                textChannel.sendMessage(args[0] + " has a private profile.").queue();
            }
            return;
        }

        //List<Member> guildAltMembers = guild.getMembers().stream().filter(member -> member.getEffectiveName().contains(" | ") && member.getAsMention().contains("!")).collect(Collectors.toList());
        List<Member> guildMembers = guild.getMembers().stream().filter(member -> member.getAsMention().contains("!")).collect(Collectors.toList());

        ScheduledExecutorService TIMER = new ScheduledThreadPoolExecutor(1);

        final int[] num = {0};
        TIMER.scheduleWithFixedDelay(() -> {
            try {
                Member member = guildMembers.get(random.nextInt(guildMembers.size() - 1));
                //PlayerProfile playerProfile = new PlayerProfile(member.getEffectiveName().replace("|", ",").split(",")[1].replaceAll("[^A-z]", ""));
                PlayerProfile playerProfile = new PlayerProfile(member.getEffectiveName().split(" ")[0].replaceAll("[^A-z]", ""));
                UserDataConnector.createUserData(member.getId(), guild.getId(), false, playerProfile);

                textChannel.sendMessage("#" + num[0] + " | Added Main Data for: " + member.getEffectiveName()).queue();

            } catch (PlayerProfile.PrivateProfileException e) {
                System.out.println("Private Profile");
            }

            num[0]++;

        }, 0L, 5L, TimeUnit.SECONDS);

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: User Data");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Developer\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nGets user Data." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

}
