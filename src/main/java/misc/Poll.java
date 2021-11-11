package misc;

import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.Component;
import utils.Utils;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Poll {

    Member member;
    TextChannel textChannel;
    TextChannel pollChannel;
    Message controlPanel;
    List<String> options = new ArrayList<>();
    String error = "";
    String title = "";
    Message pollMessage = null;
    int totalVotes = 0;

    long lastReaction = 0L;
    final long MIN_DELAY = 500L;
    int MAX_CHAR = 30;
    HashMap<String, Integer> numVotes = new HashMap<>();
    HashMap<String, List<User>> votes = new HashMap<>();
    HashMap<User, InteractionHook> interactionHooks = new HashMap<>();

    public Poll() {
    }

    public Poll (Message message) {
        member = message.getMember();
        textChannel = message.getTextChannel();
        title = member.getEffectiveName() + "'s Poll";
        pollChannel = message.getMentionedChannels().get(0);

        pollCreation();
    }

    protected void pollCreation() {
        controlPanel = textChannel.sendMessage(pollCreationEmbed().build()).complete();
        controlPanel.addReaction("✅").queue();
        controlPanel.addReaction("❌").queue();
        controlPanel.addReaction("🏷").queue();
        controlPanel.addReaction("✏").queue();
        reactionListener();
    }

    protected void reactionListener() {
        Goldilocks.eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(controlPanel.getId()) && e.getUser().equals(member.getUser()) && e.getReactionEmote().isEmoji()
                    && ("✅❌🏷✏").contains(e.getReactionEmote().getEmoji());
        }, e -> {
            String emote = e.getReactionEmote().getEmoji();
            if (("❌").equals(emote)) {
                controlPanel.delete().queue();
                return;
            }
            e.getReaction().removeReaction(member.getUser()).complete();

            if (("✅").equals(emote)) {
                if (options.size() < 2) {
                    error = "Please make sure you have a minimum of 2 options before finalizing the poll.";
                    controlPanel.editMessage(pollCreationEmbed().build()).complete().editMessage(pollCreationEmbed().build()).queueAfter(5L, TimeUnit.SECONDS);
                    reactionListener();
                } else {
                    controlPanel.delete().queue();
                    startPoll();
                }
                return;
            }
            if (("✏").equals(emote)) {
                if (options.size() > 7) {
                    error = "There is a max of 8 options allowed.";
                    controlPanel.editMessage(pollCreationEmbed().build()).complete().editMessage(pollCreationEmbed().build()).queueAfter(5L, TimeUnit.SECONDS);
                } else promptForOption(false);

            }
            if (("🏷").equals(emote)) {
                promptForOption(true);
            }

        }, 5L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    protected void promptForOption(boolean title) {
        Message promptMessage = textChannel.sendMessage(optionPromptEmbed(title).build()).complete();

        Goldilocks.eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(member.getUser()) && e.getChannel().equals(textChannel)
                    && (title ? (e.getMessage().getContentRaw().length() < 257) : (e.getMessage().getContentRaw().length() < MAX_CHAR + 1));
        }, e -> {
            String content = e.getMessage().getContentRaw();

            if (!content.equalsIgnoreCase("close")) {
                if (!title) options.add(content);
                else this.title = content;
            }

            e.getMessage().delete().queue();
            promptMessage.delete().queue();
            controlPanel.editMessage(pollCreationEmbed().build()).queue();
            reactionListener();

        }, 5L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
            promptMessage.delete().queue();
        });

    }

    protected void startPoll() {
        List<ActionRow> actionRows = new ArrayList<>();
        List<Component> curComponents = new ArrayList<>();
        for (String option : options) {
            if (curComponents.size() < 5) {
                curComponents.add(Button.of(ButtonStyle.PRIMARY, option.replaceAll("^[A-Za-z]", ""), option));
            } else {
                actionRows.add(ActionRow.of(curComponents));
                curComponents = new ArrayList<>();
                curComponents.add(Button.of(ButtonStyle.PRIMARY, option.replaceAll("^[A-Za-z]", ""), option));
            }
        }
        if (!curComponents.isEmpty()) actionRows.add(ActionRow.of(curComponents));
        actionRows.add(ActionRow.of(Button.of(ButtonStyle.DANGER, "endpoll", "Click Here to End this Poll!")));
        pollMessage = pollChannel.sendMessage(pollEmbed().build()).setActionRows(actionRows).complete();
        //for (int i = 1; i <= options.size(); i++) pollMessage.addReaction(Goldilocks.numEmotes[i]).queue();
        //pollMessage.addReaction("❌").queue();
        options.forEach(o -> votes.put(o.replaceAll("^[A-Za-z]", ""), new ArrayList<>()));
        pollListener();
    }

    protected void endPoll() {
        pollMessage.editMessage(pollEndEmbed().build()).setActionRows().queue();
        pollMessage.clearReactions().queue();
    }

    protected void pollListener() {
        Goldilocks.eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return e.getMessageId().equals(pollMessage.getId());
        }, e -> {
            //String emote = e.getReactionEmote().getEmoji();
            String compId = e.getComponentId();

            if (("endpoll").equals(compId) && e.getMember().equals(member)) {
                endPoll();
                return;
            }

            if (!hasVoted(e.getUser())) {
                votes.get(compId).add(e.getUser());
                e.reply("Thanks for voting!").setEphemeral(true).queue(i -> interactionHooks.put(e.getUser(), i));
            } else {
                e.deferEdit().queue();
                if (interactionHooks.containsKey(e.getUser())) interactionHooks.get(e.getUser()).editOriginal("You already voted!").queue();
            }

            if (System.currentTimeMillis() - lastReaction > MIN_DELAY) {
                pollMessage.editMessage(pollEmbed().build()).queue();
                lastReaction = System.currentTimeMillis();
            }

            pollListener();
        }, 30L, TimeUnit.MINUTES, this::endPoll);
    }

    protected boolean hasVoted(User user) {
        for (Map.Entry<String, List<User>> e : votes.entrySet()) {
            if (e.getValue().contains(user)) return true;
        }
        return false;
    }

    protected void getVotes() {
        numVotes = new HashMap<>();
        totalVotes = 0;
        List<User> reactedUsers = new ArrayList<>();

        for (String s : options) {
            List<User> emoteUsers = votes.get(s.replaceAll("^[A-Za-z]", ""));
            int numVotes = (int) emoteUsers.stream().filter(user -> !user.isBot() && !reactedUsers.contains(user)).count();
            totalVotes += numVotes;
            reactedUsers.addAll(emoteUsers);
            this.numVotes.put(s, numVotes);
        }

    }

    protected EmbedBuilder pollEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        String optionsString = "";
        if (pollMessage != null) getVotes();
        for (int i = 1; i <= options.size(); i++) {
            Float percentage = 0.0f;
            if (pollMessage != null) percentage = (float) numVotes.get(options.get(i - 1)) / totalVotes;
            if (percentage < 0 || percentage.isNaN()) percentage = 0f;
            if (percentage > 1) percentage = 1f;
            String bar;
            if (options.size() > 5) bar = Utils.renderPercentage(percentage, 13);
            else bar = Utils.renderPercentage(percentage, 10);

            optionsString += "`" + String.format("%-" + MAX_CHAR + "s", options.get(i - 1)) + "` **|** " + bar + " **" + String.format("%.2f", percentage * 100) + "%**\n";
        }

        embedBuilder.setTitle("📊 Poll: " + title)
                .setColor(Goldilocks.GREEN)
                .setDescription("Please react to the emote that corresponds with the option you would like to vote for, " +
                        " you can only vote for one option.\n\n**Options:**\n" + optionsString)
                .setFooter("To end this poll react with ❌")
                .setTimestamp(new Date().toInstant());

        return embedBuilder;
    }

    protected EmbedBuilder pollEndEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        String optionsString = "";
        String winner = options.get(0);
        if (pollMessage != null) getVotes();
        for (int i = 1; i <= options.size(); i++) {
            Float percentage = 0.0f;
            if (pollMessage != null) percentage = (float) numVotes.get(options.get(i - 1)) / totalVotes;
            if (percentage < 0 || percentage.isNaN()) percentage = 0f;
            if (percentage > 1) percentage = 1f;
            if (numVotes.get(winner) < numVotes.get(options.get(i - 1))) winner = options.get(i - 1);
            String bar;
            if (options.size() > 5) bar = Utils.renderPercentage(percentage, 13);
            else bar = Utils.renderPercentage(percentage, 10);
            optionsString += Goldilocks.numEmotes[i] + ": `" + String.format("%-" + MAX_CHAR + "s", options.get(i - 1)) + "` **|** " + bar + " **" + String.format("%.2f", percentage * 100) + "%**\n";
        }

        embedBuilder.setTitle("📊 Poll Ended: " + title)
                .setColor(Goldilocks.GREEN)
                .setDescription("**Winner: ** " + winner + " with " + numVotes.get(winner) + " votes!\n\n**Options:**\n" + optionsString)
                .setFooter("This poll ended at")
                .setTimestamp(new Date().toInstant());

        return embedBuilder;
    }

    protected EmbedBuilder optionPromptEmbed(boolean title) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.setTitle((title ? "Title " : "New Option ") + "for " + member.getEffectiveName() + "'s Poll")
                .setColor(Goldilocks.GREEN)
                .setFooter("To exit this process react type close")
                .setTimestamp(new Date().toInstant());

        if (!title) embedBuilder.setDescription("To add an option for your poll, type it below. Options **must be** under " + MAX_CHAR + " characters.");
        else embedBuilder.setDescription("To set the title for your poll, type it below. The title **must be** under 256 characters.");

        return embedBuilder;
    }

    protected EmbedBuilder pollCreationEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        String optionsString = "";
        for (int i = 1; i <= options.size(); i++) optionsString += Goldilocks.numEmotes[i] + ": " + options.get(i - 1) + "\n";
        if (options.isEmpty()) optionsString = "You currently have no options for your poll.";

        embedBuilder.setTitle("📊 Poll Creation for " + member.getEffectiveName())
                .setColor(Goldilocks.WHITE)
                .setDescription((!error.isEmpty() ? "```\n" + error + "\n```\n" : "") +
                        "**Controls**\n" +
                        "To add an option react with ✏\n" +
                        "To change the title react with 🏷\n" +
                        "To start this poll react with ✅\n" +
                        "To cancel this poll react with ❌")
                .addField("Poll Title", title, false)
                .addField("Poll Options", optionsString, false)
                .setTimestamp(new Date().toInstant());
        error = "";
        return embedBuilder;
    }

}
