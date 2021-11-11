package misc;

import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import utils.Utils;

import java.util.Arrays;
import java.util.Date;

public class Rate extends Poll {

    public Rate(Message message) {
        member = message.getMember();
        textChannel = message.getTextChannel();
        String content = message.getContentRaw();
        pollChannel = message.getMentionedChannels().get(0);
        title = content.substring(content.indexOf(" ")).replace(pollChannel.getAsMention(), "").replaceFirst(" ", "");
        MAX_CHAR = 5;

        options = Arrays.asList(new String[]{"1/10", "2/10", "3/10", "4/10", "5/10", "6/10", "7/10", "8/10", "9/10", "10/10"});

        startPoll();
    }

    @Override
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

            optionsString += Goldilocks.numEmotes[i] + ": `" + String.format("%-" + MAX_CHAR + "s", options.get(i - 1)) + "` **|** " + bar + " **" + String.format("%.2f", percentage * 100) + "%**\n";
        }

        embedBuilder.setTitle("📊 Poll: " + title)
                .setColor(Goldilocks.GREEN)
                .setDescription("**Options:**\n" + optionsString)
                .setFooter("To end this poll react with ❌")
                .setTimestamp(new Date().toInstant());

        return embedBuilder;
    }

    @Override
    protected EmbedBuilder pollEndEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        String optionsString = "";
        String winner = options.get(0);
        float averageScore = 0.0f;
        if (pollMessage != null) getVotes();
        for (int i = 1; i <= options.size(); i++) {
            Float percentage = 0.0f;
            averageScore += numVotes.get(options.get(i - 1)) * i;
            if (pollMessage != null) percentage = (float) numVotes.get(options.get(i - 1)) / totalVotes;
            if (percentage < 0 || percentage.isNaN()) percentage = 0f;
            if (percentage > 1) percentage = 1f;
            if (numVotes.get(winner) < numVotes.get(options.get(i - 1))) winner = options.get(i - 1);
            String bar;
            if (options.size() > 5) bar = Utils.renderPercentage(percentage, 13);
            else bar = Utils.renderPercentage(percentage, 10);
            optionsString += Goldilocks.numEmotes[i] + ": `" + String.format("%-" + MAX_CHAR + "s", options.get(i - 1)) + "` **|** " + bar + " **" + String.format("%.2f", percentage * 100) + "%**\n";
        }
        averageScore /= totalVotes;

        embedBuilder.setTitle("📊 Poll Ended: " + title)
                .setColor(Goldilocks.GREEN)
                .setDescription("**Average Score: ** " + String.format("%.1f", averageScore) + " with " + totalVotes + " total votes!\n\n**Options:**\n" + optionsString)
                .setFooter("This poll ended at")
                .setTimestamp(new Date().toInstant());

        return embedBuilder;
    }

}
