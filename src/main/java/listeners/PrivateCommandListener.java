package listeners;

import main.Database;
import main.Goldilocks;
import modmail.ModmailHub;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.user.UserTypingEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import verification.AddAltRequest;
import verification.VerificationHub;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class PrivateCommandListener extends ListenerAdapter {

    //String excludedWords[] = {"join", "help"};
    //Scanner systemIn = new Scanner(System.in);

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {

        if (event.getAuthor().isBot()) // if a bot ignore.
            return;

        String[] split = event.getMessage().getContentRaw().split(" ");
        String[] args = Arrays.copyOfRange(split, 1, split.length);


        if (event.getMessage().getContentRaw().toLowerCase().contains("help") && args.length < 2) {
            Goldilocks.commands.getCommand("help").execute(event.getMessage(), "help", args);
            return;
        } if (event.getMessage().getContentRaw().toLowerCase().contains("alt") && args.length < 2) {
            Guild guild = event.getAuthor().getMutualGuilds().stream().filter(guild1 -> Database.getGuildInfo(guild1, "rank").equals("3")).collect(Collectors.toList()).get(0);
            if (guild.getMember(event.getAuthor()).getEffectiveName().contains("|")) {
                event.getAuthor().openPrivateChannel().complete().sendMessage("You already have an alt account added, if you would like to remove this please contact a Security+.").queue();
                return;
            }

            Goldilocks.TIMER.schedule(() -> VerificationHub.addAltRequests.add(new AddAltRequest(guild.getMember(event.getAuthor()))), 0L, TimeUnit.SECONDS);
            return;
        } else {
            System.out.println(event.getAuthor().getName() + "(" + event.getAuthor().getId() + "):");
            System.out.println(event.getMessage().getContentRaw());
            System.out.println();
            //Goldilocks.jda.openPrivateChannelById(event.getAuthor().getId()).complete().sendMessage(systemIn.nextLine()).queue();
        }

        if (event.getMessage().getContentRaw().toLowerCase().contains("modmail") && event.getMessage().getContentRaw().length() < 9) {
            event.getAuthor().openPrivateChannel().complete().sendMessage("What would you like to modmail?").queue();
        }

        if (ModmailHub.hasOpenModmail(event.getAuthor())) {
            ModmailHub.receivedMessage(event.getAuthor(), event.getMessage());
        } else if (args.length > 0) ModmailHub.promptForGuild(event.getAuthor(), event.getMessage());

    }

    @Override
    public void onUserTyping(@Nonnull UserTypingEvent event) {
        if (event.isFromType(ChannelType.PRIVATE) && VerificationHub.generalVerifications.containsKey(event.getUser())) {
            VerificationHub.generalVerifications.get(event.getUser()).sendTyping();
        }
    }
}
