package moderation.punishments.controlpanels;

import com.cloudinary.utils.ObjectUtils;
import main.Goldilocks;
import moderation.PunishmentConnector;
import moderation.punishments.Mute;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static main.Goldilocks.cloudinary;
import static main.Goldilocks.eventWaiter;
import static moderation.punishments.controlpanels.ControlPanelUtils.*;

public class MuteCP {
    List<Member> recipients;
    Member mod, recipient;

    long timeStarted, timeEnding;
    long duration = -1;
    Mute mute;

    Message controlPanel;

    File evidence = null;
    String reason, evidenceURL = "";
    String[] controls = {"✅", "📸", "⏱", "🗑", "❌"};

    public MuteCP(Message message, String[] args, Member... members) {
        recipients = Arrays.asList(members);
        mod = message.getMember();
        TextChannel textChannel = message.getTextChannel();

        reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        //Check the first two args of the message (Aside from the members). Check if they are indicating time.
        String fArgNums = args[0].replaceAll("[^0-9]", "");
        if (fArgNums.length() > 15) fArgNums = "";
        if (!fArgNums.isEmpty()) {
            // Use the second arg as the time identifier
            if (fArgNums.equals(args[0]) && Arrays.stream(times).anyMatch(s -> args[1].startsWith(s))) {
                duration = getTime(Integer.parseInt(fArgNums), args[1]);
                reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            }
            else if (Arrays.stream(times).anyMatch(s -> args[0].replaceAll("[^A-Za-z]", "").startsWith(s))) {
                duration = getTime(Integer.parseInt(fArgNums), args[0].replaceAll("[^A-Za-z]", ""));
                reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            }
        }

        // Mute the user if they already specified the time
        if (duration != -1) {
            for (Member recipient : recipients) {
                issueMute(recipient, textChannel);
            }
            return;
        }

        recipient = members[0];

        // Otherwise continue to the control panel
        // Download attachment to file
        if (!message.getAttachments().isEmpty()) {
            try {
                evidence = message.getAttachments().get(0).downloadToFile("punishmentEvidence/" + recipient.getEffectiveName() + "_" + message.getId() + ".png").get();
                Map params = ObjectUtils.asMap(
                        "public_id", "punishmentEvidence/" + recipient.getEffectiveName() + "_" + message.getId(),
                        "overwrite", true,
                        "resource_type", "image"
                );
                Map imageDataMap =  cloudinary.uploader().upload(evidence, params);
                evidenceURL = (String) imageDataMap.get("url");
            } catch (Exception e) { e.printStackTrace(); }
        }

        // Set the default mute time to one day
        duration = getTime(1, "d");

        //Create control panel
        mute = new Mute(recipient, mod, reason, timeStarted, duration);

        controlPanel = textChannel.sendMessage(panelEmbed().build()).complete();
        addEmojis(controlPanel, controls);
        reactionHandler();
    }

    public void reactionHandler() {
        Goldilocks.eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(controlPanel.getId()) && e.getUser().equals(mod.getUser()) && e.getReactionEmote().isEmoji()
                    && Arrays.asList(controls).contains(e.getReactionEmote().getEmoji());
        }, e -> {

            String emoji = e.getReactionEmote().getEmoji();
            e.getReaction().removeReaction(e.getUser()).queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            switch (emoji) {
                case "✅":
                    issueMute(recipient, controlPanel.getTextChannel());
                    controlPanel.delete().queue();
                    break;
                case "📸":
                    promptForEvidence();
                    break;
                case "⏱":
                    promptForLength();
                    break;
                case "🗑":
                    promptForMessageDeletes();
                    break;
                case "❌":
                    controlPanel.delete().queue();
                    break;
                default:
                    break;
            }

        }, 2L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    public void issueMute(Member recipient, TextChannel textChannel) {
        mute = new Mute(recipient, mod, reason, System.currentTimeMillis(), duration);
        int result = mute.issueMute();
        if (result == -2) textChannel.sendMessage("Unable to mute user because the mute role is invalid.").queue();
        if (result == -1) {
            overrideMute(recipient, textChannel);
        }
        if (result == 0) textChannel.sendMessage(muteStartEmbed(recipient).build()).queue();

    }

    public void overrideMute(Member recipient, TextChannel textChannel) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(recipient.getEffectiveName() + " is already muted, would you like to override the mute?")
                .setColor(Goldilocks.WHITE);

        Message overrideMessage = textChannel.sendMessage(embedBuilder.build()).complete();
        addEmojis(overrideMessage, "✅", "❌");

        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(overrideMessage.getId()) && e.getUser().equals(mod.getUser())
                    && e.getReactionEmote().isEmoji() && ("✅❌").contains(e.getReactionEmote().getEmoji());
        }, e -> {
            String emoji = e.getReactionEmote().getEmoji();

            if (emoji.equals("✅")) {
                mute = new Mute(recipient, mod, reason, System.currentTimeMillis(), duration);
                mute.overrideMute();
                textChannel.sendMessage(new EmbedBuilder().setColor(Goldilocks.GREEN)
                        .setDescription("Successfully overrode mute for " + recipient.getAsMention()
                                + (mute.logMessage == null ? "" : " view the mute log **[🔗](" + mute.logMessage.getJumpUrl() + ")**"))
                        .build()).queue();
            }
            overrideMessage.delete().queue();
        }, 2, TimeUnit.MINUTES, () -> {
            overrideMessage.delete().queue();
            controlPanel.delete().queue();
        });

    }

    public EmbedBuilder muteStartEmbed(Member recipient) {
        return new EmbedBuilder()
                .setColor(Goldilocks.GREEN)
                .setDescription("Successfully muted " + recipient.getAsMention()
                        + (mute.logMessage == null ? "" : " view the mute log **[🔗](" + mute.logMessage.getJumpUrl() + ")**"));
    }

    public void promptForLength() {
        EmbedBuilder muteLengthCP = new EmbedBuilder();
        muteLengthCP.setTitle("Please select the period of time you would like to mute " + recipient.getEffectiveName() + " for:")
                .setDescription("```\n" + reason + "\n```")
                .setColor(Goldilocks.GOLD)
                .setThumbnail(recipient.getUser().getAvatarUrl())
                .addField("Length Options:", "1️⃣: 1 day | 2️⃣: 2 days | 3️⃣: 3 days | 7️⃣: 1 week | 📆: 2 weeks", true)
                .setFooter("Mute Control Panel for " + mod.getEffectiveName(), mod.getUser().getAvatarUrl())
                .setTimestamp(new Date().toInstant());

        String[] timeOptions = {"1️⃣", "2️⃣", "3️⃣", "7️⃣", "📆"};
        Message lengthMessage = controlPanel.getTextChannel().sendMessage(muteLengthCP.build()).complete();
        addEmojis(lengthMessage, timeOptions);


        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(lengthMessage.getId()) && e.getUser().equals(mod.getUser())
                    && e.getReactionEmote().isEmoji() && Arrays.asList(timeOptions).contains(e.getReactionEmote().getEmoji());
        }, e -> {

            String emoji = e.getReactionEmote().getEmoji();
            switch (emoji) {
                case "1️⃣":
                    duration = getTime(1, "d");
                    break;
                case "2️⃣":
                    duration = getTime(2, "d");
                    break;
                case "3️⃣":
                    duration = getTime(3, "d");
                    break;
                case "7️⃣":
                    duration = getTime(7, "d");
                    break;
                case "📆":
                    duration = getTime(2, "w");
                    break;
            }
            lengthMessage.delete().queue();
            controlPanel.editMessage(panelEmbed().build()).queue();
            reactionHandler();

        }, 2, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
            lengthMessage.delete().queue();
        });
    }

    public void promptForMessageDeletes() {
        HashMap<TextChannel, List<Message>> messageMap = PunishmentConnector.getMessages(recipient, 86400000);

        String description = messageMap.entrySet().stream().filter(e -> e.getValue().size() > 0)
                .map(e -> e.getKey().getAsMention() + " **|** `"
                        + e.getValue().size() + "` Messages").collect(Collectors.joining("\n"));

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("Would you like to delete these Messages?")
                .setColor(Goldilocks.LIGHTBLUE)
                .setDescription("These messages were sent by " + recipient.getAsMention() + " over the last `1 hour`\n" + (description.isEmpty() ? "None" : description));
        Message prompt = controlPanel.getTextChannel().sendMessage(embedBuilder.build()).complete();
        addEmojis(prompt, "✅", "❌");

        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(prompt.getId()) && e.getUser().equals(mod.getUser())
                    && e.getReactionEmote().isEmoji() && ("✅❌").contains(e.getReactionEmote().getEmoji());
        }, e -> {
            String emoji = e.getReactionEmote().getEmoji();

            if (emoji.equals("✅")) {
                for (Map.Entry<TextChannel, List<Message>> entry : messageMap.entrySet()) {
                    entry.getKey().purgeMessages(entry.getValue());
                }
            }
            prompt.delete().queue();
            reactionHandler();
        }, 2, TimeUnit.MINUTES, () -> {
            prompt.delete().queue();
            controlPanel.delete().queue();
        });

    }

    public void promptForEvidence() {
        EmbedBuilder muteEvidenceCp = new EmbedBuilder();
        muteEvidenceCp.setTitle("Please send your evidence for " + recipient.getEffectiveName() + "'s mute")
                .setDescription("Please send the image of your evidence in the chat below.")
                .setColor(Goldilocks.GOLD)
                .setFooter("Type close at anytime to exit this process")
                .setTimestamp(new Date().toInstant());

        controlPanel.editMessage(muteEvidenceCp.build()).queue();

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(mod.getUser()) && (e.getMessage().getAttachments().size() >= 1 || e.getMessage().getContentRaw().toLowerCase().equals("close"));
        }, e -> {
            if (e.getMessage().getContentRaw().equalsIgnoreCase("close")) {
                e.getMessage().delete().queue();
                reactionHandler();
                return;
            }
            try {
                evidence = e.getMessage().getAttachments().get(0).downloadToFile("punishmentEvidence/" + recipient.getEffectiveName() + "_" + e.getMessage().getId() + ".png").get();
                Map params = ObjectUtils.asMap(
                        "public_id", "punishmentEvidence/" + recipient.getEffectiveName() + "_" + e.getMessage().getId(),
                        "overwrite", true,
                        "resource_type", "image"
                );
                Map imageDataMap =  cloudinary.uploader().upload(evidence, params);
                evidenceURL = (String) imageDataMap.get("url");
            } catch (Exception ignored) { }
            e.getMessage().delete().queue();
            controlPanel.editMessage(panelEmbed().build()).queue();
            reactionHandler();

        }, 2L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    public EmbedBuilder panelEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        // Todo previous mutes

        embedBuilder.setTitle("You are about to mute " + recipient.getEffectiveName() + " for " + DurationFormatUtils.formatDurationWords(duration, true, true) + " for the following reason:")
                .setDescription("```\n" + reason + "\n```")
                .setColor(Goldilocks.BLUE);

        embedBuilder.addField("Controls:", "**`Initiate       `** ✅ Confirm the mute\n" +
                "**`Evidence       `** 📸 Add evidence to the mute\n" +
                "**`Length         `** ⏱ Change the length of the mute\n" +
                "**`Delete Messages`** 🗑 Open a GUI to delete recent messages.\n" +
                "**`Cancel         `** ❌ Cancel this mute.", false)
                .setFooter("Mute Control Panel for " + mod.getEffectiveName(), mod.getUser().getAvatarUrl())
                .setTimestamp(new Date().toInstant());

        if (!evidenceURL.isEmpty()) {
            embedBuilder.addField("Evidence", "Below is the evidence you provided:", false);
            embedBuilder.setImage(evidenceURL);
        }
        return embedBuilder;
    }
}
