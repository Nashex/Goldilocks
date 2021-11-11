package giveaways;

import lombok.AllArgsConstructor;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import org.apache.commons.lang3.time.DurationFormatUtils;
import utils.MemberSearch;
import utils.Utils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Giveaway {

    public Member host;
    public int numWinners;
    public String prize;
    public TextChannel textChannel;
    public long length;
    public long startingTime;

    private Message startingMessage;
    public Member creator;
    public TextChannel creationChannel;

    public Message giveawayMessage;

    public Giveaway(Member creator, TextChannel creationChannel, Message message) {

        this.creator = creator;
        this.creationChannel = creationChannel;
        this.startingTime = System.currentTimeMillis();
        this.startingMessage = message;

        getFields(Field.HOSTING);
    }

    public Giveaway(Member host, Member creator, int numWinners, String prize, TextChannel textChannel, long startingTime, long length, Message giveawayMessage) {
        this.host = host;
        this.creator = creator;
        this.numWinners = numWinners;
        this.prize = prize;
        this.textChannel = textChannel;
        this.startingTime = startingTime;
        this.length = length;
        this.giveawayMessage = giveawayMessage;
    }

    private void getFields(Field field) {
        //Giveaway prompt progression
        //    Who is hosting, Number of Winners, Prize, Channel, How long?

        creationChannel.sendMessage(field.message).complete();
        Goldilocks.eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(creator.getUser()) && !e.getMessage().equals(startingMessage) && ((e.getMessage().getContentRaw().contains(" ") && e.getMessage().getContentRaw().split(" ").length <= field.maxArgs)
                    || e.getMessage().getContentRaw().equalsIgnoreCase("close") || field.maxArgs == 1 || (!e.getMessage().getMentionedChannels().isEmpty() && field.equals(Field.CHANNEL)));
        }, e -> {

            String messageContent = e.getMessage().getContentRaw();

            if (messageContent.equalsIgnoreCase("close")) {
                creationChannel.sendMessage("Aw you decided not to make a giveaway.").queue();
                return;
            }

            switch (field) {
                case HOSTING:
                    getHost(messageContent, e.getMessage());
                    break;
                case WINNERS:
                    getWinners(messageContent);
                    break;
                case PRIZE:
                    getPrize(messageContent);
                    break;
                case CHANNEL:
                    getChannel(messageContent, e.getMessage());
                    break;
                case TIME:
                    getTime(messageContent);
                    break;
            }

        }, 5L, TimeUnit.MINUTES, () -> {
            creationChannel.sendMessage("Unfortunately this giveaway creation has timed out.").queue();
        });
    }

    private void getHost(String content, Message message) {
        List<Member> memberList = MemberSearch.memberSearch(message, content.split(" "));
        if (memberList.isEmpty()) {
            creationChannel.sendMessage("Unfortunately I could not find that user. Please re-enter another name/@/id/tag or type close to stop the giveaway creation.").queue();
            getFields(Field.HOSTING);
        } else {
            this.host = memberList.get(0);
            getFields(Field.WINNERS);
        }
    }

    private void getWinners(String content) {
        if (!Utils.isNumeric(content) || Integer.parseInt(content) < 1 || Integer.parseInt(content) > 25) {
            creationChannel.sendMessage("Unfortunately that is not a valid number of users. Please re-enter a number between 1 and 25.").queue();
            getFields(Field.WINNERS);
        } else {
            this.numWinners = Integer.parseInt(content);
            getFields(Field.PRIZE);
        }
    }

    private void getPrize(String content) {
        //Confirm the prize
        Message confirmationMessage = creationChannel.sendMessage("Are you sure you would like: `" + content + "` to be your prize?").complete();
        confirmationMessage.addReaction("✅").queue();
        confirmationMessage.addReaction("❌").queue();

        Goldilocks.eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e2 -> {
            return e2.getMessageId().equals(confirmationMessage.getId()) && e2.getMember().equals(this.creator) && e2.getReactionEmote().isEmoji()
                    && ("✅❌").contains(e2.getReactionEmote().getEmoji());
        }, e2 -> {
            if (("✅").equalsIgnoreCase(e2.getReactionEmote().getEmoji())) {
                this.prize = content;
                getFields(Field.CHANNEL);
            } else {
                creationChannel.sendMessage(Field.PRIZE.message).queue();
                getFields(Field.PRIZE);
            }
            confirmationMessage.delete().queue();
        }, 5L, TimeUnit.MINUTES, () -> {
            confirmationMessage.delete().queue();
        });
    }

    private void getChannel(String content, Message message) {
        if (message.getMentionedChannels().isEmpty()) {
            try {
                this.textChannel = creationChannel.getGuild().getTextChannelsByName(content, true).get(0);
                getFields(Field.TIME);
            } catch (Exception e1) {
                creationChannel.sendMessage("Unfortunately I was unable to find a channel with that name. Please re-enter the name or mention of the channel.").queue();
                getFields(Field.CHANNEL);
            }
        } else {
            this.textChannel = message.getMentionedChannels().get(0);
            getFields(Field.TIME);
        }
    }

    private void getTime(String content) {
        String[] args = content.split(" ");
        String[] times = {"w", "d", "mo", "h", "mi", "s"};
        if (Utils.isNumeric(args[0]) && Arrays.asList(times).stream().anyMatch(s -> args[1].startsWith(s))) {
            int multiplier = Integer.parseInt(args[0]);
            String prefix = Arrays.asList(times).stream().filter(s -> args[1].startsWith(s)).collect(Collectors.toList()).get(0);
            switch (prefix) {
                case "w":
                    this.length = multiplier * 604800;
                    break;
                case "mo":
                    this.length = multiplier * 2628000;
                    break;
                case "h":
                    this.length = multiplier * 3600;
                    break;
                case "d":
                    this.length = multiplier * 86400;
                    break;
                case "mi":
                    this.length = multiplier * 60;
                    break;
                default: //Seconds
                    this.length = multiplier;
                    break;
            }
            startGiveaway();
        } else {
            creationChannel.sendMessage("Unfortunately I was unable to recognize a time period. Please enter a time period with the following format: <# of> <mo/w/d/mi/h/s>").queue();
            getFields(Field.TIME);
        }
    }

    private void startGiveaway() {
        String messageContext = "🎊 **Giveaway Hosted by " + host.getAsMention() + "!** 🎊";
        giveawayMessage = textChannel.sendMessage(messageContext).embed(onGoingEmbed().build()).complete();
        giveawayMessage.addReaction("🎉").queue();
        creationChannel.sendMessage("You successfully created a giveaway in " + textChannel.getAsMention() + "!").queue();
        GiveawayHub.giveaways.add(this);

        Database.addGiveaway(this);

    }

    public void endGiveaway() {
        //Pick winners
        Database.endGiveaway(giveawayMessage.getId());
        GiveawayHub.giveaways.remove(this);

        List<User> users = giveawayMessage.retrieveReactionUsers("🎉").complete();
        List<Member> winners = getWinners(users);

        giveawayMessage.editMessage("🎊 **Giveaway Hosted by " + host.getAsMention() + " has Ended!** 🎊").embed(endedEmbed(winners).build()).queue();
        giveawayMessage.reply("Congratulations " + String.join(" ", winners.stream().map(member1 -> member1.getAsMention()).collect(Collectors.toList())) +
                "! You won the giveaway for **" + prize + "**!").queue();
    }

    private List<Member> getWinners(List<User> users) {
        List<Member> winners = new ArrayList<>();
        Random random = new Random();

        while (winners.size() < numWinners && winners.size() < users.size() - 1) {
            try {
                Member member = giveawayMessage.getGuild().getMember(users.get(random.nextInt(users.size())));
                if (!winners.contains(member) && !member.equals(this.host) && !member.equals(giveawayMessage.getGuild().getSelfMember())) winners.add(member);
            } catch (Exception e) {} // User is no longer in the server
        }
        return winners;
    }

    public void updateGiveaway() {
        if (System.currentTimeMillis() > startingTime + length * 1000) endGiveaway();
        else giveawayMessage.editMessage(onGoingEmbed().build()).queue();
    }

    private EmbedBuilder onGoingEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(prize)
                .setColor(Goldilocks.GOLD)
                .setDescription("To enter the giveaway react with 🎉" +
                        "\nTime Remaining: " + DurationFormatUtils.formatDuration((startingTime + length * 1000) - System.currentTimeMillis(), "'**'d'** days **'H'** hours **'m'** minutes **'s'** seconds'", true) +
                        "\nNumber of Winners: **" + numWinners + "**")
                .setFooter("Created by: " + creator.getEffectiveName() + " | Ending")
                .setTimestamp(new Date(System.currentTimeMillis() + length * 1000).toInstant());
        return embedBuilder;
    }

    private EmbedBuilder endedEmbed(List<Member> winners) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(prize)
                .setColor(Goldilocks.WHITE)
                .setDescription("__**Winners**__\n" +
                        String.join("\n", winners.stream().map(member1 -> member1.getAsMention()).collect(Collectors.toList())))
                .setFooter(winners.size() + " Winners | Ended")
                .setTimestamp(new Date().toInstant());

        return embedBuilder;
    }

    @AllArgsConstructor
    private enum Field {
        HOSTING("A giveaway? Awesome! Say \"close\" at anytime if you want to cancel this process.\nWho is hosting this giveaway? Enter someone's __name, @, id, or tag.__", 1),
        WINNERS("How many winners will this giveaway have? Enter a number __between 1 and 25__.", 1),
        PRIZE("What are you giving away? If you have multiple winners be sure to account for what they get! Enter __up to 10 words__.", 10),
        CHANNEL("What is the __name or mention__ of the channel you want to host this giveaway in?", 10),
        TIME("How long will this giveaway last? Enter in this format: `1 week` for one week, `2 days` for two days, etc.", 2);

        public String message;
        public int maxArgs;
    }

}
