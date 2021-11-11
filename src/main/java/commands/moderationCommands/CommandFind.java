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
import utils.MemberSearch;
import utils.Utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CommandFind extends Command {
    public CommandFind() {
        setAliases(new String[] {"find"});
        setEligibleRoles(new String[] {"arl","tSec", "eo"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        if (args.length == 0) {
            Utils.errorMessage("Unable to find user", "No name/id/@ was provided", msg.getTextChannel(), 10L);
            return;
        }

        //Guild guild = Goldilocks.jda.getGuildById("514788290809954305");
        TextChannel textChannel = msg.getTextChannel();
        List<Member> memberList = MemberSearch.memberSearch(msg, args);

        for (Member member : memberList) {
            Goldilocks.TIMER.schedule(() -> {
                find(member, textChannel);
            }, 0L, TimeUnit.SECONDS);
        }

    }

    public static void find (Member member, TextChannel textChannel) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        List<Role> suspendedRoles = member.getRoles().stream().filter(role -> role.getName().toLowerCase().contains("suspended")).collect(Collectors.toList());
        embedBuilder.setColor(Goldilocks.BLUE);
        //Todo check for punishment state
        embedBuilder.setDescription("**`ID:`**`" + member.getId() + "` | " + member.getAsMention() + " found with the nickname: "
                + (Arrays.asList(member.getEffectiveName().split(" [|] ")).stream().map(s -> "**[" + s.replaceAll("[^A-Za-z]","")
                + "](https://www.realmeye.com/player/" + s.replaceAll("[^A-Za-z]","") + ")**")).collect(Collectors.joining(" | ")));
        embedBuilder.addField("Highest Role:", member.getRoles().stream().sorted(new Comparator<Role>() {
            @Override
            public int compare(Role o1, Role o2) {
                return o2.getPosition() - o1.getPosition();
            }
        }).collect(Collectors.toList()).get(0).getAsMention() , true);

        embedBuilder.addField("Suspended", suspendedRoles.size() > 0 ? suspendedRoles.get(0).getAsMention() : "❌", true);
        embedBuilder.addField("Voice Channel", member.getVoiceState().inVoiceChannel() ? "<#" + member.getVoiceState().getChannel().getId() + ">" : "None", true);
        embedBuilder.setFooter(member.getId(), member.getUser().getAvatarUrl());
        textChannel.sendMessage(embedBuilder.build()).queue(m -> {
            int[] parses = Database.getParses(member.getEffectiveName().toLowerCase().split(" ")[0].replaceAll("[^A-Za-z]", ""), textChannel.getGuild());
            if (parses[0] != 0) {
                embedBuilder.addField("Crashes", "This user has crashed a total of `" + parses[0] + "` runs. Exactly `" + parses[1] + "` of those crashes were in " + textChannel.getGuild().getName(), false);
                m.editMessage(embedBuilder.build()).queue();
            }
        });
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Find");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Trial Security or Raid Leader\n";
        commandDescription += "Syntax: ;alias <@/id/name>\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nRetrieves a user for a given server." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
