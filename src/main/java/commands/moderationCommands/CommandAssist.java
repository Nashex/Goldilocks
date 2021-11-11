package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import sheets.GoogleSheets;
import utils.MemberSearch;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CommandAssist extends Command {
    public CommandAssist() {
        setAliases(new String[] {"assist", "addassist"});
        setEligibleRoles(new String[] {"arl", "eo", "tSec"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        TextChannel textChannel = msg.getTextChannel();

        if (args.length == 0) {
            textChannel.sendMessage("Please use the command with the following syntax: `.assist <name> [additional names...]`").queue();
            return;
        }

        int numAssists = 1;
        List<Member> members = new ArrayList<>();
        for (String s : args) {
            if (s.replaceAll("[^0-9]", "").length() == 1) numAssists = Integer.parseInt(s.replaceAll("[^0-9]", ""));
            else {
                Member member = MemberSearch.memberSearch(s, msg.getGuild());
                if (member != null && !members.contains(member)) members.add(member);
                else textChannel.sendMessage("Unfortunately I could not find a member with the following name/id `" + s + "`").queue();
            }
        }

        if (members.isEmpty()) return;

        Message prompt = textChannel.sendMessage("Are you sure you would like to add "
                + (numAssists == 1 ? "an assist " : numAssists + " assists ") + "to " + members.stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")) + "?")
                .setActionRow(Button.success("yesassist", "Yes"),
                        Button.danger("noassist", "No")).complete();

        Goldilocks.eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return prompt.equals(e.getMessage()) && Arrays.asList("yesassist", "noassist").contains(e.getComponentId()) && Objects.equals(e.getMember(), msg.getMember());
        }, e -> {

            String control = e.getComponentId();
            e.deferEdit().queue();

            if (control.equals("yesassist")) {
                Database.addAssists(members);
                members.forEach(a -> {
                    GoogleSheets.logEvent(msg.getGuild(), GoogleSheets.SheetsLogType.ASSISTS, a.getEffectiveName(), a.getId(), Objects.requireNonNull(msg.getMember()).getEffectiveName(), msg.getMember().getId());
                    Database.logEvent(a, Database.EventType.ASSIST, System.currentTimeMillis() / 1000, textChannel, "assist");
                    textChannel.sendMessage("Successfully added an assist to " + a.getEffectiveName() + "!").queue();
                });
            }
            prompt.delete().queue();

        }, 2L, TimeUnit.MINUTES, () -> prompt.delete().queue());
    }


    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Add Assist");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Helper, Eveont Organizer, or Almost Raid Leader\n";
        commandDescription += "Syntax: .assist <name> [additional names...]\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nAdds an assist to the given users" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

}
