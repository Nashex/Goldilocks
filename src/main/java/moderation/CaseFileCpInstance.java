package moderation;

import com.cloudinary.utils.ObjectUtils;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static main.Goldilocks.cloudinary;
import static main.Goldilocks.eventWaiter;

public class CaseFileCpInstance {

    private Message controlPanel;
    private Member executor;
    private String caseId;
    private List<String> caseImages;
    private List<String> caseNotes;
    private EmbedBuilder embedBuilder;

    public CaseFileCpInstance(String caseId, TextChannel textChannel, Member member) {

        this.executor = member;
        this.caseId = caseId;

        List<String> caseEvidence = PunishmentConnector.getCaseEvidence(caseId, member.getGuild().getId());
        this.caseImages = caseEvidence.stream().filter(s -> s.contains("res.cloudinary.com")).collect(Collectors.toList());
        this.caseNotes = caseEvidence.stream().filter(s -> !s.contains("res.cloudinary.com")).collect(Collectors.toList());

        String caseNotesString = "";
        for (String string : caseNotes) {
            caseNotesString += string + "\n\n";
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Goldilocks.GOLD);
        embedBuilder.setTitle("Case File for: " + caseId);
        embedBuilder.setDescription("**Controls:**" +
                (caseImages.size() > 1 ? "\nUse ◀ and ▶ to scroll through image evidence" : "") +
                "\nUse 📸 to add image evidence to this case file" +
                "\nUse ✏ to add textual evidence or notes to this case file" +
                "\nUse ❌ to exit this menu" +
                "\n\n**Case Notes:**" +
                "```\n" + (caseNotesString.isEmpty() ? "None" : caseNotesString) + "\n```" +
                "\n**Case Images:**" +
                (caseImages.isEmpty() ? "```\nNone\n```" : ""));
        if (caseImages.size() > 1) {
            embedBuilder.setFooter("Page " + 1 + " of " + caseImages.size());
        }


        if (!caseImages.isEmpty()) {
            embedBuilder.setImage(caseImages.get(0));
        }
        controlPanel = textChannel.sendMessage(embedBuilder.build()).complete();
        if (caseImages.size() > 1) {
            controlPanel.addReaction("◀").queue();
            controlPanel.addReaction("▶").queue();
        }
        controlPanel.addReaction("📸").queue();
        controlPanel.addReaction("✏").queue();
        controlPanel.addReaction("❌").queue();
        this.embedBuilder = embedBuilder;
        reactionHandler(0);
    }

    public void addControlPanelEmotes() {
        if (caseImages.size() > 1) {
            controlPanel.addReaction("◀").queue();
            controlPanel.addReaction("▶").queue();
        }
        controlPanel.addReaction("📸").queue();
        controlPanel.addReaction("✏").queue();
        controlPanel.addReaction("❌").queue();
    }

    public void reactionHandler(int casePosition) {
        List<String> caseEvidence = PunishmentConnector.getCaseEvidence(caseId, executor.getGuild().getId());
        this.caseImages = caseEvidence.stream().filter(s -> s.contains("res.cloudinary.com")).collect(Collectors.toList());
        this.caseNotes = caseEvidence.stream().filter(s -> !s.contains("res.cloudinary.com")).collect(Collectors.toList());

        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(controlPanel.getId()) && e.getUser().equals(executor.getUser()) && e.getReaction().getReactionEmote().isEmoji()
                    && ("📸✏❌◀▶").contains(e.getReactionEmote().getEmoji()) && (("▶").equals(e.getReactionEmote().getEmoji()) ? (caseImages.size() > casePosition + 1) : true)
                    && (("◀").equals(e.getReactionEmote().getEmoji()) ? (casePosition > 0) : true);
        }, e -> {
            String emote = e.getReactionEmote().getEmoji();
            if (emote.equals("▶") && (caseImages.size() > casePosition + 1)) {
                e.getReaction().removeReaction(e.getUser()).queue();
                embedBuilder.setImage(caseImages.get(casePosition + 1));
                if (caseImages.size() > 1) {
                    embedBuilder.setFooter("Page " + (casePosition + 2) + " of " + caseImages.size());
                }
                controlPanel.editMessage(embedBuilder.build()).queue();
                reactionHandler(casePosition + 1);
            }
            if (emote.equals("◀") && casePosition >= 1) {
                e.getReaction().removeReaction(e.getUser()).queue();
                embedBuilder.setImage(caseImages.get(casePosition - 1));
                if (caseImages.size() > 1) {
                    embedBuilder.setFooter("Page " + (casePosition) + " of " + caseImages.size());
                }
                controlPanel.editMessage(embedBuilder.build()).queue();
                reactionHandler(casePosition - 1);
            }
            if (emote.equals("📸")) {
                e.getReaction().removeReaction(e.getUser()).queue();
                controlPanel.clearReactions().queue();
                promptForEvidence(casePosition);
            }
            if (emote.equals("✏")) {
                e.getReaction().removeReaction(e.getUser()).queue();
                controlPanel.clearReactions().queue();
                promptForCaseNote(casePosition);
            }
            if (emote.equals("❌")) {
                controlPanel.delete().queue();
            }
        }, 2L, TimeUnit.MINUTES, () -> {controlPanel.delete().queue();});
    }

    public void promptForCaseNote(int casePosition) {
        EmbedBuilder evidenceCp = new EmbedBuilder();
        evidenceCp.setTitle("Please send your note for " + caseId)
                .setDescription("Please send the note you would like to add in the chat below.")
                .setColor(Goldilocks.GOLD)
                //.setThumbnail(recipient.getUser().getAvatarUrl())
                .setFooter("Type close at anytime to exit this process")
                .setTimestamp(new Date().toInstant());

        controlPanel.editMessage(evidenceCp.build()).queue();

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(executor.getUser());
        }, e -> {
            if (e.getMessage().getContentRaw().toLowerCase().equals("close")) {
                controlPanel.editMessage(embedBuilder.build()).queue();
                addControlPanelEmotes();
                e.getMessage().delete().queue();
                reactionHandler(casePosition);
                return;
            }
            PunishmentConnector.addCase(caseId, executor.getGuild().getId(),"Note added by: " + executor.getEffectiveName() + "\n" + e.getMessage().getContentRaw());
            this.caseNotes = PunishmentConnector.getCaseEvidence(caseId, executor.getGuild().getId()).stream().filter(s -> !s.contains("res.cloudinary.com")).collect(Collectors.toList());
            String caseNotesString = "";
            for (String string : caseNotes) {
                caseNotesString += string + "\n\n";
            }
            embedBuilder.setDescription("**Controls:**" +
                    (caseImages.size() > 1 ? "\nUse ◀ and ▶ to scroll through image evidence" : "") +
                    "\nUse 📸 to add image evidence to this case file" +
                    "\nUse ✏ to add textual evidence or notes to this case file" +
                    "\nUse ❌ to exit this menu" +
                    "\n\n**Case Notes:**" +
                    "```\n" + (caseNotesString.isEmpty() ? "None" : caseNotesString) + "\n```" +
                    "\n**Case Images:**" +
                    (caseImages.isEmpty() ? "```\nNone\n```" : ""));
            if (caseImages.size() > 1) {
                embedBuilder.setFooter("Page " + casePosition + " of " + caseImages.size());
            }
            e.getMessage().delete().queue();
            controlPanel.editMessage(embedBuilder.build()).queue();
            addControlPanelEmotes();
            reactionHandler(casePosition);

        }, 2L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    public void promptForEvidence(int casePosition) {
        EmbedBuilder suspensionEvidenceCp = new EmbedBuilder();
        suspensionEvidenceCp.setTitle("Please send your evidence for " + caseId)
                .setDescription("Please send the image of your evidence in the chat below.")
                .setColor(Goldilocks.GOLD)
                .setFooter("Type close at anytime to exit this process")
                .setTimestamp(new Date().toInstant());

        controlPanel.editMessage(suspensionEvidenceCp.build()).queue();

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(executor.getUser()) && (e.getMessage().getAttachments().size() >= 1 || e.getMessage().getContentRaw().toLowerCase().equals("close"));
        }, e -> {
            if (e.getMessage().getContentRaw().toLowerCase().equals("close")) {
                e.getMessage().delete().queue();
                reactionHandler(casePosition);
                return;
            }
            if (e.getMessage().getAttachments().size() >= 1) {
                File file;
                e.getMessage().getAttachments().get(0).downloadToFile(file = new File("punishmentEvidence/" + controlPanel.getId() + ".png")).thenAccept(file1 -> {
                    try {
                        Map params = ObjectUtils.asMap(
                                "public_id", "punishmentEvidence/" + controlPanel.getId(),
                                "overwrite", true,
                                "resource_type", "image"
                        );
                        Map imageDataMap =  cloudinary.uploader().upload(new File(file.getAbsolutePath()), params);
                        String imageUrl = (String) imageDataMap.get("url");
                        caseImages.add(imageUrl);
                        PunishmentConnector.addCase(caseId, executor.getGuild().getId(), imageUrl);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }).thenAccept(file3 -> {
                    if (embedBuilder.build().getImage() == null) embedBuilder.setImage(caseImages.get(0));
                    this.caseNotes = PunishmentConnector.getCaseEvidence(caseId, executor.getGuild().getId()).stream().filter(s -> !s.contains("res.cloudinary.com")).collect(Collectors.toList());
                    String caseNotesString = "";
                    for (String string : caseNotes) {
                        caseNotesString += string + "\n\n";
                    }
                    embedBuilder.setDescription("**Controls:**" +
                            (caseImages.size() > 1 ? "\nUse ◀ and ▶ to scroll through image evidence" : "") +
                            "\nUse 📸 to add image evidence to this case file" +
                            "\nUse ✏ to add textual evidence or notes to this case file" +
                            "\nUse ❌ to exit this menu" +
                            "\n\n**Case Notes:**" +
                            "```\n" + (caseNotesString.isEmpty() ? "None" : caseNotesString) + "\n```" +
                            "\n**Case Images:**" +
                            (caseImages.isEmpty() ? "```\nNone\n```" : ""));
                    if (caseImages.size() > 1) {
                        embedBuilder.setFooter("Page " + casePosition + " of " + caseImages.size());
                    }
                    controlPanel.editMessage(embedBuilder.build()).queue();
                    addControlPanelEmotes();
                    reactionHandler(casePosition);
                    e.getMessage().delete().queue();
                });
            }
        }, 2L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

}
