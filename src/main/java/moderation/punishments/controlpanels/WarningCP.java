package moderation;

import com.cloudinary.utils.ObjectUtils;
import main.Goldilocks;
import main.Permissions;
import moderation.punishments.Warning;
import moderation.punishments.controlpanels.ControlPanelUtils;
import moderation.punishments.controlpanels.SuspensionCP;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static main.Goldilocks.cloudinary;
import static main.Goldilocks.eventWaiter;

public class WarningCP {
    Member recipient, mod;
    boolean strict = false;
    String reason, evidenceURL = "";
    Message controlPanel;
    File evidence = null;

    public WarningCP(Member recipient, Member mod, String reason, TextChannel textChannel) {
        this(recipient, mod, reason, textChannel, null);
    }

    public WarningCP(Member recipient, Member mod, String reason, TextChannel textChannel, Message message) {
        this.recipient = recipient;
        this.mod = mod;
        this.reason = reason;
        if (message != null) {
            controlPanel = message;
            controlPanel.editMessage(panelEmbed().build()).queue();
        }
        else controlPanel = textChannel.sendMessage(panelEmbed().build()).complete();

        ControlPanelUtils.addEmojis(controlPanel, "✅", "↕", "📸");
        if (Permissions.hasPermission(mod, new String[]{"rl","security"}))
            controlPanel.addReaction("🔀").queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
        controlPanel.addReaction("❌").queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));

        reactionHandler();
    }

    public void reactionHandler() {
        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(controlPanel.getId()) && e.getUser().equals(mod.getUser())
                    && e.getReactionEmote().isEmoji() && ("✅↕📸🔀❌").contains(e.getReactionEmote().getEmoji());
        }, e -> {

            String emoji = e.getReactionEmote().getEmoji();
            e.getReaction().removeReaction(mod.getUser()).queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));

            if (emoji.equals("❌")) {
                controlPanel.delete().queue();
            }

            if (emoji.equals("✅")) {
                Warning warning = new Warning(recipient, mod, reason, strict).issue(controlPanel.getTextChannel());
                if (!evidenceURL.isEmpty()) PunishmentConnector.addCase(warning.getCaseId(), mod.getGuild().getId(), evidenceURL);
                controlPanel.delete().queue();
            }

            if (emoji.equals("↕")) {
                strict = !strict;
                controlPanel.editMessage(panelEmbed().build()).queue();
                reactionHandler();
            }

            if (emoji.equals("📸")) {
                promptForEvidence();
            }

            if (emoji.equals("🔀") && Permissions.hasPermission(mod, new String[]{"rl","security"})) {
                controlPanel.clearReactions().complete();
                new SuspensionCP(recipient, mod, reason, controlPanel);
            }

        }, 2, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
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
        String warnHistory = "";
        List<Warning> memberWarnings = PunishmentConnector.getWarnings(recipient);
        if (memberWarnings.isEmpty()) warnHistory = "This user has no previous warnings.";
        else warnHistory = memberWarnings.stream().map(Warning::toString).collect(Collectors.joining("\n"));

        if (warnHistory.length() > 1000) warnHistory = warnHistory.substring(0, 1000);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("You are about to issue a" + (strict ? " STRICT" : "") + " warning to " + recipient.getEffectiveName() + " for the following reason:")
                .setDescription("```\n" + reason + "\n```")
                .setColor(Goldilocks.YELLOW)
                .setThumbnail(recipient.getUser().getAvatarUrl())
                .addField("Previous Warning History", "```\n" + warnHistory + "\n```", false)
                .addField("Controls:", "**`Initiate `** ✅ Confirm the warning\n" +
                        "**`Evidence `** 📸 Add evidence to the warning\n" +
                        "**`Intensity`** ↕ Make this warning " + (strict ? "normal" : "strict") + "\n" +
                        "**`Convert  `** 🔀 Convert this warning to a suspension\n" +
                        "**`Cancel   `** ❌ Cancel this warning.", false);
        embedBuilder.setFooter("This Warning is Being Issued by " + mod.getEffectiveName(), recipient.getGuild().getIconUrl())
                .setTimestamp(new Date().toInstant());

        if (!evidenceURL.isEmpty()) {
            embedBuilder.addField("Evidence", "Below is the evidence you provided:", false);
            embedBuilder.setImage(evidenceURL);
        }
        return embedBuilder;
    }
}