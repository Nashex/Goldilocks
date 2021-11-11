package moderation.punishments.controlpanels;

import com.cloudinary.utils.ObjectUtils;
import main.Goldilocks;
import moderation.PunishmentConnector;
import moderation.WarningCP;
import moderation.punishments.Suspension;
import moderation.punishments.Warning;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static main.Goldilocks.cloudinary;
import static main.Goldilocks.eventWaiter;
import static moderation.punishments.controlpanels.ControlPanelUtils.*;

public class SuspensionCP {

    List<Member> recipients;
    Member mod, recipient;

    long timeStarted;
    long duration = -1;
    Suspension suspension;

    Message controlPanel;

    File evidence = null;
    String reason, evidenceURL = "";
    String[] controls = {"✅", "📸", "⏱", "🔀", "❌"};

    public SuspensionCP(Message message, String[] args, Member... members) {
        recipients = Arrays.asList(members);
        mod = message.getMember();
        TextChannel textChannel = message.getTextChannel();

        reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        //Check the first two args of the message (Aside from the members). Check if they are indicating time.
        String fArgNums = args[0].replaceAll("[^0-9]", "");
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

        // Suspend the user if they already specified the time
        if (duration != -1) {
            for (Member recipient : recipients) {
                issueSuspension(recipient, textChannel);
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

        // Set the default suspension time to one day
        duration = getTime(1, "d");

        //Create control panel
        suspension = new Suspension(recipient, mod, reason, timeStarted, duration);

        controlPanel = textChannel.sendMessage(panelEmbed().build()).complete();
        addEmojis(controlPanel, controls);
        reactionHandler();
    }

    public SuspensionCP(Member recipient, Member mod, String reason, Message message) {
        duration = getTime(1, "d");

        //Create control panel
        suspension = new Suspension(recipient, mod, reason, timeStarted, duration);

        this.recipient = recipient;
        this.mod = mod;
        this.reason = reason;

        controlPanel = message;
        message.editMessage(panelEmbed().build()).queue();
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
                    issueSuspension(recipient, controlPanel.getTextChannel());
                    controlPanel.delete().queue();
                    break;
                case "📸":
                    promptForEvidence();
                    break;
                case "⏱":
                    promptForLength();
                    break;
                case "🔀":
                    controlPanel.clearReactions().complete();
                    new WarningCP(recipient, mod, reason, controlPanel.getTextChannel(), controlPanel);
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

    public void issueSuspension(Member recipient, TextChannel textChannel) {
        suspension = new Suspension(recipient, mod, reason, System.currentTimeMillis(), duration);
        int result = suspension.issueSuspension();
        if (result == -3) textChannel.sendMessage("Unable to suspend user because the user has a role equal to or higher than you.").queue();
        if (result == -2) textChannel.sendMessage("Unable to suspend user because the suspended role is invalid.").queue();
        if (result == -1) {
            overrideSuspension(recipient, textChannel);
        }
        if (result == 0) {
            textChannel.sendMessage(suspensionStartEmbed(recipient).build()).queue();
            if (!evidenceURL.isEmpty()) PunishmentConnector.addCase(suspension.getCaseId(), mod.getGuild().getId(), evidenceURL);
        }

    }

    public void overrideSuspension(Member recipient, TextChannel textChannel) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(recipient.getEffectiveName() + " is already suspended, would you like to override the suspension?")
                .setColor(Goldilocks.WHITE);

        Message overrideMessage = textChannel.sendMessage(embedBuilder.build()).complete();
        addEmojis(overrideMessage, "✅", "❌");

        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(overrideMessage.getId()) && e.getUser().equals(mod.getUser())
                    && e.getReactionEmote().isEmoji() && ("✅❌").contains(e.getReactionEmote().getEmoji());
        }, e -> {
            String emoji = e.getReactionEmote().getEmoji();

            if (emoji.equals("✅")) {
                suspension = new Suspension(recipient, mod, reason, System.currentTimeMillis(), duration);
                suspension.overrideSuspension();
                textChannel.sendMessage(new EmbedBuilder().setColor(Goldilocks.GREEN)
                        .setDescription("Successfully overrode suspension for " + recipient.getAsMention()
                                + (suspension.logMessage == null ? "" : " view the suspension log **[🔗](" + suspension.logMessage.getJumpUrl() + ")**"))
                        .build()).queue();
            }
            overrideMessage.delete().queue();
        }, 2, TimeUnit.MINUTES, () -> {
            overrideMessage.delete().queue();
            controlPanel.delete().queue();
        });

    }

    public EmbedBuilder suspensionStartEmbed(Member recipient) {
        return new EmbedBuilder()
                .setColor(Goldilocks.GREEN)
                .setDescription("Successfully suspended " + recipient.getAsMention()
                        + (suspension.logMessage == null ? "" : " view the suspension log **[🔗](" + suspension.logMessage.getJumpUrl() + ")**"));
    }

    public void promptForLength() {
        EmbedBuilder suspensionLengthCP = new EmbedBuilder();
        suspensionLengthCP.setTitle("Please select the period of time you would like to suspend " + recipient.getEffectiveName() + " for:")
                .setDescription("```\n" + reason + "\n```")
                .setColor(Goldilocks.GOLD)
                .setThumbnail(recipient.getUser().getAvatarUrl())
                .addField("Length Options:", "1️⃣: 1 day | 2️⃣: 2 days | 3️⃣: 3 days | 7️⃣: 1 week | 📆: 2 weeks", true)
                .setFooter("Suspension Control Panel for " + mod.getEffectiveName(), mod.getUser().getAvatarUrl())
                .setTimestamp(new Date().toInstant());

        String[] timeOptions = {"1️⃣", "2️⃣", "3️⃣", "7️⃣", "📆"};
        Message lengthMessage = controlPanel.getTextChannel().sendMessage(suspensionLengthCP.build()).complete();
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

    public void promptForEvidence() {
        EmbedBuilder suspensionEvidenceCp = new EmbedBuilder();
        suspensionEvidenceCp.setTitle("Please send your evidence for " + recipient.getEffectiveName() + "'s suspension")
                .setDescription("Please send the image of your evidence in the chat below.")
                .setColor(Goldilocks.GOLD)
                .setFooter("Type close at anytime to exit this process")
                .setTimestamp(new Date().toInstant());

        controlPanel.editMessage(suspensionEvidenceCp.build()).queue();

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
        String warnHistory = "";
        String suspensionHistory = "";

        List<Suspension> suspensionList = PunishmentConnector.getSuspensions(recipient);
        suspensionHistory = suspensionList.stream().map(Suspension::toString).collect(Collectors.joining("\n"));
        if (suspensionHistory.length() > 1000) suspensionHistory = suspensionHistory.substring(0, 1000);

        List<Warning> warningList = PunishmentConnector.getWarnings(recipient);
        warnHistory = warningList.stream().map(Warning::toString).collect(Collectors.joining("\n"));
        if (warnHistory.length() > 1000) warnHistory = warnHistory.substring(0, 1000);

        embedBuilder.setTitle("You are about to suspend " + recipient.getEffectiveName() + " for " + DurationFormatUtils.formatDurationWords(duration, true, true) + " for the following reason:")
                .setDescription("```\n" + reason + "\n```")
                .setColor(Goldilocks.BLUE);
        try {
            embedBuilder.addField("__Previous Warning History__", "```\n" + (warnHistory.isEmpty() ? "This user has no previous warnings." : warnHistory) + "\n```", false);
        } catch (Exception e) {
            embedBuilder.addField("__Previous Warning History__", "Warning history too long to display.", false);
        }

        try {
            embedBuilder.addField("Previous Suspension History", "```\n" + (suspensionHistory.isEmpty() ? "This user has no previous suspensions." : suspensionHistory) + "\n```", false);
        } catch (Exception e) {
            embedBuilder.addField("Previous Suspension History", "Suspension history too long to display", false);
        }

        embedBuilder.addField("Controls:", "**`Initiate`** ✅ Confirm the suspension\n" +
                "**`Evidence`** 📸 Add evidence to the suspension\n" +
                "**`Length  `** ⏱ Change the length of the suspension\n" +
                "**`Convert `** 🔀 Convert this suspension to a warning\n" +
                "**`Cancel  `** ❌ Cancel this suspension.", false)
                .setFooter("Suspension Control Panel for " + mod.getEffectiveName(), mod.getUser().getAvatarUrl())
                .setTimestamp(new Date().toInstant());

        if (!evidenceURL.isEmpty()) {
            embedBuilder.addField("Evidence", "Below is the evidence you provided:", false);
            embedBuilder.setImage(evidenceURL);
        }
        return embedBuilder;
    }
}
