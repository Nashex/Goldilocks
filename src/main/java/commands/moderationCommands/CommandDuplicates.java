package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import org.ocpsoft.prettytime.PrettyTime;
import setup.SetupConnector;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CommandDuplicates extends Command {
    public CommandDuplicates() {
        setAliases(new String[] {"fixduplicates", "fixdupes", "dupes"});
        setEligibleRoles(new String[] {"security"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        Guild guild = msg.getGuild();
        TextChannel textChannel = msg.getTextChannel();

        HashMap<String, Member> existingNames = new HashMap<>();
        HashMap<Member, Member> duplicateNamesMap = new HashMap<>();

        final int[] numDuplicates = {0};

        guild.getMembers().stream().filter(member -> member.getNickname() != null)
                .forEach(member -> {
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

        List<Map.Entry<Member, Member>> duplicates = new ArrayList<>(duplicateNamesMap.entrySet());
        if (!duplicates.isEmpty()) listener(textChannel, msg.getMember(), duplicates, new ArrayList<>());
        else textChannel.sendMessage("You do not have any duplicate names in this server so you are unable to enter this process!").queue();

    }

    private static void listener(TextChannel textChannel, Member member, List<Map.Entry<Member, Member>> duplicates, List<Member> kicked) {
        Map.Entry<Member, Member> current = duplicates.get(0);

        Message message = textChannel.sendMessage(duplicateNamesEmbed(current.getKey(), current.getValue()).build())
                .setActionRow(Button.primary("one", current.getKey().getEffectiveName() + " (" + current.getKey().getUser().getAsTag() + ")"),
                        Button.primary("two", current.getValue().getEffectiveName() + " (" + current.getValue().getUser().getAsTag() + ")"),
                        Button.danger("skip", "Skip"),
                        Button.danger("cancel", "Cancel"))
                .complete();

        Goldilocks.eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return Objects.equals(e.getMessage(), message) && Objects.equals(e.getMember(), member) &&
                    Arrays.asList("one", "two", "skip", "cancel").contains(e.getComponentId());
        }, e-> {
            e.deferEdit().queue();
            String control = e.getComponentId();

            if (control.equals("cancel")) {
                message.editMessage(finalNamesEmbed(kicked).build()).setActionRows().queue();
                return;
            }

            if (control.equals("one")) {
                kickMember(current.getKey(), member);
                message.editMessage(kickedUser(current.getKey()).build()).setActionRows().queue();
                kicked.add(current.getKey());
            } else if (control.equals("two")) {
                kickMember(current.getValue(), member);
                message.editMessage(kickedUser(current.getValue()).build()).setActionRows().queue();
                kicked.add(current.getValue());
            }  else if (control.equals("skip")) {
                message.delete().queue();
            }


            duplicates.remove(0);
            if (duplicates.size() > 0) {
                Map.Entry<Member, Member> next = duplicates.get(0);
                listener(textChannel, member, duplicates, kicked);
            } else {
                message.editMessage(finalNamesEmbed(kicked).build()).setActionRows().queue();
            }

        }, 10L, TimeUnit.MINUTES, () -> {
            message.editMessage(finalNamesEmbed(kicked).build()).setActionRows().queue();
        });
    }

    private static void kickMember(Member member, Member mod) {
        String reason = "Your account was claimed by someone else, if you believe this to be a mistake please join " +
                mod.getGuild().getName() + " with this link " + (mod.getGuild().getBoostTier().equals(Guild.BoostTier.TIER_3) ?
                mod.getGuild().retrieveVanityInvite().complete().getUrl() : "") + " and message a security+ to help you.";

        if (!SetupConnector.getFieldValue(mod.getGuild(), "guildLogs", "modLogChannelId").equals("0")) {
            TextChannel logChannel = Goldilocks.jda.getTextChannelById(SetupConnector.getFieldValue(mod.getGuild(), "guildLogs", "modLogChannelId"));
            if (logChannel != null) logChannel.sendMessage(new EmbedBuilder().setTitle("User Kicked From Guild")
                    .setColor(Goldilocks.RED)
                    .addField("User Information", member.getAsMention() + " | `" + member.getId() + "`", true)
                    .addField("Mod Information", mod.getAsMention() + " | `" +mod.getId() + "`", true)
                    .addField("Reason", "```\n" + reason + "\n```", false)
                    .setTimestamp(new Date().toInstant()).build()).queue();
        }
        mod.getGuild().kick(member, reason).queue();
    }

    private static EmbedBuilder duplicateNamesEmbed(Member member1, Member member2) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Please Select Which User to Kick")
                .setColor(Goldilocks.LIGHTBLUE)
                .addField(member1.getEffectiveName() + " (" + member1.getUser().getAsTag() + ")",
                        getInfo(member1), true)
                .addField(member2.getEffectiveName()  + " (" + member2.getUser().getAsTag() + ")",
                        getInfo(member2), true);
        return embedBuilder;
    }

    private static EmbedBuilder finalNamesEmbed(List<Member> kickedMembers) {
        String kickedMembersString =  (kickedMembers.isEmpty() ? "None" : kickedMembers.stream().map(Member::getAsMention).collect(Collectors.joining(", ")));
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Duplicate Names Process Finished")
                .setColor(Goldilocks.GREEN)
                .setDescription("**Kicked Members:** " +
                        (kickedMembersString.length() > 950 ? kickedMembersString.substring(950) : kickedMembersString));
        return embedBuilder;
    }

    private static EmbedBuilder kickedUser(Member member) {
        return new EmbedBuilder().setColor(Goldilocks.GREEN)
                .setDescription("Successfully kicked " + member.getAsMention());
    }

    private static String getInfo(Member member) {
        PrettyTime prettyTime = new PrettyTime();
        return "**User Tag: **" + member.getAsMention() + "\n" +
                "\n**Mutual Servers (" + member.getUser().getMutualGuilds().size() + ")**: `"
                + member.getUser().getMutualGuilds().stream().map(Guild::getName).collect(Collectors.joining(", ")) + "`\n" +
                "\n**Member Roles (" + member.getRoles().size() + "):** "
                + member.getRoles().stream().map(Role::getAsMention).collect(Collectors.joining(", ")) + "\n" +
                "\n**Joined Server:** ```\n" + prettyTime.format(member.getTimeJoined()) + "\n```" +
                "\n**Discord Created:** ```\n" + prettyTime.format(member.getUser().getTimeCreated()) + "\n```";
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Duplicates");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Security\n";
        commandDescription += "Syntax: ;duplicates\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nPrompts you through the duplicate users of a server." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

}
