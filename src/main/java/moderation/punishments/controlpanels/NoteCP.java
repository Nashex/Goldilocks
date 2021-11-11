package moderation.punishments.controlpanels;

import com.cloudinary.utils.ObjectUtils;
import main.Goldilocks;
import moderation.PunishmentConnector;
import moderation.punishments.Note;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static main.Goldilocks.cloudinary;
import static main.Goldilocks.eventWaiter;

public class NoteCP {
    Member recipient, mod;
    boolean strict = false;
    String reason, evidenceURL = "";
    Message controlPanel;
    File evidence = null;

    public NoteCP(Member recipient, Member mod, String reason, TextChannel textChannel) {
        this(recipient, mod, reason, textChannel, null);
    }

    public NoteCP(Member recipient, Member mod, String reason, TextChannel textChannel, Message message) {
        this.recipient = recipient;
        this.mod = mod;
        this.reason = reason;
        if (message != null) {
            controlPanel = message;
            controlPanel.editMessage(panelEmbed().build()).queue();
        }
        else controlPanel = textChannel.sendMessage(panelEmbed().build()).complete();
        ControlPanelUtils.addEmojis(controlPanel, "✅", "📸", "❌");

        reactionHandler();
    }

    public void reactionHandler() {
        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(controlPanel.getId()) && e.getUser().equals(mod.getUser())
                    && e.getReactionEmote().isEmoji() && ("✅📸❌").contains(e.getReactionEmote().getEmoji());
        }, e -> {
            String emoji = e.getReactionEmote().getEmoji();
            e.getReaction().removeReaction(mod.getUser()).queue();

            if (emoji.equals("❌")) {
                controlPanel.delete().queue();
            }

            if (emoji.equals("✅")) {
                Note note = new Note(recipient, mod, reason).issue(controlPanel.getTextChannel());
                if (!evidenceURL.isEmpty()) PunishmentConnector.addCase(note.getCaseId(), mod.getGuild().getId(), evidenceURL);
                controlPanel.delete().queue();
            }

            if (emoji.equals("📸")) {
                promptForEvidence();
            }
        }, 2, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    public void promptForEvidence() {
        EmbedBuilder suspensionEvidenceCp = new EmbedBuilder();
        suspensionEvidenceCp.setTitle("Please send your evidence for " + recipient.getEffectiveName() + "'s note")
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
        String noteHistory = "";

        List<Note> memberNotes = PunishmentConnector.getNotes(recipient);
        if (memberNotes.isEmpty()) noteHistory = "This user has no other notes.";
        else noteHistory = memberNotes.stream().map(Note::toString).collect(Collectors.joining("\n"));
        if (noteHistory.length() > 1000) noteHistory = noteHistory.substring(0, 1000);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("You are about to add a note to " + recipient.getEffectiveName() + " with the following content:")
                .setDescription("```\n" + reason + "\n```")
                .setColor(Goldilocks.LIGHTBLUE)
                .setThumbnail(recipient.getUser().getAvatarUrl())
                .addField("Previous Notes", "```\n" + noteHistory + "\n```", false)
                .addField("Controls:", "**`Confirm  `** ✅ Confirm the note\n" +
                        "**`Evidence `** 📸 Add evidence to the note\n" +
                        "**`Cancel   `** ❌ Cancel this note.", false);
        embedBuilder.setFooter("This Note is Being Added by " + mod.getEffectiveName(), recipient.getGuild().getIconUrl())
                .setTimestamp(new Date().toInstant());

        if (!evidenceURL.isEmpty()) {
            embedBuilder.addField("Evidence", "Below is the evidence you provided:", false);
            embedBuilder.setImage(evidenceURL);
        }
        return embedBuilder;
    }
}
