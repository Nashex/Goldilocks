package raids;

import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import sheets.GoogleSheets;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AssistLogger {

    private Member raidLeader;
    private TextChannel textChannel;
    private List<Member> assistors;
    private HashMap<Member, Boolean> assistLog = new HashMap<>();

    public AssistLogger(TextChannel textChannel, Member raidLeader, List<Member> assistors) {
        if (assistors.isEmpty()) return;

        this.textChannel = textChannel;
        this.raidLeader = raidLeader;
        this.assistors = assistors.stream().distinct().collect(Collectors.toList());

        confirmAssist(null, assistors.get(0));

    }

    private void confirmAssist(Message message, Member member) {
        if (message == null) {
            message = textChannel.sendMessage(assistEmbed(member).build()).setActionRow(
                    Button.success("yes", "Yes"),
                    Button.danger("no", "No")
            ).complete();
        } else {
            message.editMessage(assistEmbed(member).build()).queue();
        }

        Message finalMessage = message;
        Goldilocks.eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return Objects.equals(e.getMember(), raidLeader) && Arrays.asList("yes", "no").contains(e.getComponentId()) && Objects.equals(e.getMessage(), finalMessage);
        }, e -> {

            String controlId = e.getComponentId();

            e.deferEdit().queue();

            if (controlId.equals("yes")) {
                Database.addAssists(Collections.singletonList(member));
                GoogleSheets.logEvent(member.getGuild(), GoogleSheets.SheetsLogType.ASSISTS, member.getEffectiveName(), member.getId(), raidLeader.getEffectiveName(), raidLeader.getId());
                Database.logEvent(member, Database.EventType.ASSIST, System.currentTimeMillis() / 1000, textChannel, "assistPanel");
                assistLog.put(member, true);
            } else {
                assistLog.put(member, false);
            }

            if (assistors.indexOf(member) < (assistors.size() - 1)) {
                confirmAssist(finalMessage, assistors.get(assistors.indexOf(member) + 1));
            } else {
                finalMessage.editMessage(finalAssistEmbed().build()).setActionRows().queue();
            }

        }, 2L, TimeUnit.MINUTES, () -> {
            finalMessage.delete().queue();
        });
    }

    private static EmbedBuilder assistEmbed(Member member) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Would you like to add an assist to " + member.getEffectiveName() + "?")
                .setColor(Goldilocks.YELLOW)
                .setDescription("Please select yes if you believe " + member.getAsMention() + " adequately helped with your run." +
                        " If they did not please select no.");
        return embedBuilder;
    }

    private EmbedBuilder finalAssistEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Thank you for logging you Assists!")
                .setColor(Goldilocks.GREEN)
                .setDescription("You have awarded assists to: " + assistLog.entrySet().stream().filter(Map.Entry::getValue)
                        .map(e -> e.getKey().getAsMention()).collect(Collectors.joining(", "))
                + "\n If there are any additional people you would like to add assists to please use the `.assist [name(s)]` command to do so!");
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

}
