package verification;

import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import shatters.SqlConnector;
import sheets.GoogleSheets;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static verification.VerificationHub.*;

public class newManualVerificationRequest {

    public Message message;
    public String username;
    public Member member;

    public newManualVerificationRequest(Message message, String username, Member member) {
        this.message = message;
        this.username = username;
        this.member = member;
    }

    public void unlockVerification(Member member) {
        message.editMessage(message.getEmbeds().get(0)).setActionRow(
                Button.success("accept", "Accept"),
                Button.danger("deny", "Deny"),
                Button.secondary("lock", "Re-Lock").withEmoji(Emoji.fromUnicode("🔒"))
        ).queue();

        List<String> controls = Arrays.asList("accept", "deny", "lock");

        Goldilocks.eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return e.getMessageId().equals(message.getId()) && e.getUser().equals(member.getUser())
                    && controls.contains(e.getComponentId());
        }, e -> {
            String control = e.getComponentId();
            if (!e.isAcknowledged()) e.deferEdit().queue();

            if (control.equals("accept")) {
                acceptVerification(member);
            }
            if (control.equals("deny")) {
                denyVerification(member);
            }
            if (control.equals("lock")) {
                lockVerification();
            }
        }, 5L, TimeUnit.MINUTES, this::lockVerification);

//        message.clearReactions("🔑").queue(aVoid -> {
//            message.addReaction("✅").queue();
//            message.addReaction("❌").queue();
//            message.addReaction("🔒").queue();
//        });
//
//        Goldilocks.eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
//            return e.getMessageId().equals(message.getId()) && e.getUser().equals(member.getUser())
//                    && e.getReactionEmote().isEmoji() && ("✅❌🔒").contains(e.getReactionEmote().getEmoji());
//        }, e -> {
//            String emoji = e.getReactionEmote().getEmoji();
//
//            if (emoji.equals("✅")) {
//                acceptVerification(member);
//            }
//            if (emoji.equals("❌")) {
//                denyVerification(member);
//            }
//            if (emoji.equals("🔒")) {
//                lockVerification();
//            }
//        }, 5L, TimeUnit.MINUTES, () -> {
//            lockVerification();
//        });

    }

    public void acceptVerification(Member member) {
        MessageEmbed embed = message.getEmbeds().get(0);
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setAuthor(embed.getAuthor().getName())
                .setTitle(embed.getTitle())
                .setColor(Goldilocks.GREEN)
                .setDescription(embed.getDescription())
                .setImage(embed.getImage().getUrl())
                .setFooter("Accepted by " + member.getEffectiveName())
                .setTimestamp(new Date().toInstant());

        for (MessageEmbed.Field f : embed.getFields()) embedBuilder.addField(f.getName(), f.getValue(), f.isInline());
        message.editMessage(embedBuilder.build()).setActionRows().queue();
        //message.editMessage(embedBuilder.build()).queue(m -> message.clearReactions().queue(aVoid -> m.addReaction("💯").queue()));

        this.member.getUser().openPrivateChannel().queue(privateChannel -> {
            privateChannel.sendMessage("You have been successfully verified for " + member.getGuild().getName() + " please read the rules and happy raiding!").queue();
            newManualVerificationRequests.remove(this);
        });

        //Log verification
        //Verify user
        verifyUser(this.member, member, username, embed.getImage().getUrl(), message.getJumpUrl());

    }

    public void denyVerification(Member member) {

        message.editMessage(message.getEmbeds().get(0))
                .setActionRow(Button.secondary("blacklist", "Blacklist").withEmoji(Emoji.fromUnicode("🔨")),
                        Button.secondary("settings", "Incorrect Settings").withEmoji(Emoji.fromUnicode("🗺")),
                        Button.secondary("trash", "Trash").withEmoji(Emoji.fromUnicode("🗑")),
                        Button.secondary("lock", "Re-Lock").withEmoji(Emoji.fromUnicode("🔒")))
                .queue();

        List<String> controls = Arrays.asList("blacklist", "settings", "trash", "lock");

        Goldilocks.eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return e.getMessageId().equals(message.getId()) && e.getUser().equals(member.getUser())
                    && controls.contains(e.getComponentId());
        }, e -> {
            String control = e.getComponentId();
            if (!e.isAcknowledged()) e.deferEdit().queue();

            if (control.equals("lock")) {
                lockVerification();
            } else {
                denyVerification(control, member);
            }
        }, 5L, TimeUnit.MINUTES, this::lockVerification);

//        message.clearReactions().queue(aVoid -> {
//            message.addReaction("🔨").queue();
//            message.addReaction("🗺").queue();
//            message.addReaction("🗑").queue();
//            message.addReaction("🔒").queue();
//        });
//
//        Goldilocks.eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
//            return e.getMessageId().equals(message.getId()) && e.getUser().equals(member.getUser())
//                    && e.getReactionEmote().isEmoji() && ("🔨🗺🗑🔒").contains(e.getReactionEmote().getEmoji());
//        }, e -> {
//            String emoji = e.getReactionEmote().getEmoji();
//
//            if (emoji.equals("🔒")) {
//                lockVerification();
//            } else {
//                denyVerification(emoji, member);
//            }
//        }, 5L, TimeUnit.MINUTES, () -> {
//            lockVerification();
//        });
    }

    public void lockVerification() {
        message.editMessage(message.getEmbeds().get(0))
                .setActionRow(Button.success("unlock", "Unlock this Verification Panel")).queue();
//        message.clearReactions().queue(aVoid -> {
//            message.addReaction("🔑").queue();
//        });
    }

    public void denyVerification(String emoji, Member denier) {
        String messageText = "";
        switch (emoji) {
            case "blacklist":
            case "🔨":
                if (Database.isShatters(denier.getGuild())) SqlConnector.expelUser(username.toLowerCase(), member.getId(), denier.getId());
                Database.expelMember(member, username.toLowerCase(), denier, "Blacklisted through pending verification.");
                messageText = "You have been blacklisted from verification by " + denier.getAsMention() + " for being a suspected alt account. " +
                        "If you believe this to be an issue, please feel free to message them.";
                break;
            case "settings":
            case "🗺":
                messageText = "You have been denied verification by " + denier.getAsMention() + " due to your Realm Eye privacy settings being incorrect. " +
                        "Please re-enter the verification process and follow the instructions closely.";
                break;
            case "trash":
            case "🗑":
                break;
        }

        MessageEmbed embed = message.getEmbeds().get(0);
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setAuthor(embed.getAuthor().getName())
                .setColor(Goldilocks.RED)
                .setDescription(embed.getDescription())
                .setImage(embed.getImage().getUrl())
                .setFooter("Denied by " + denier.getEffectiveName() + " using " + emoji)
                .setTimestamp(new Date().toInstant());

        for (MessageEmbed.Field f : embed.getFields()) embedBuilder.addField(f.getName(), f.getValue(), f.isInline());
        message.editMessage(embedBuilder.build()).setActionRows().queue();
        //message.editMessage(embedBuilder.build()).queue(m -> message.clearReactions().queue(aVoid -> m.addReaction("👋").queue()));
        logVerificationDenial(member, denier, username, "Denied by " + denier.getEffectiveName() + " using " + emoji, message.getJumpUrl());

        GoogleSheets.logEvent(member.getGuild(), GoogleSheets.SheetsLogType.VERIFICATIONS, denier.getEffectiveName(), denier.getId(), member.getEffectiveName(), member.getId(), "Denied with " + emoji);
        if (!denier.getUser().equals(Goldilocks.jda.getSelfUser())) {
            Database.incrementField(denier, "quotaVerifications", "totalVerifications");
            Database.logEvent(denier, Database.EventType.VERIFICATION, System.currentTimeMillis() / 1000, message.getTextChannel(), message.getContentRaw());
        }

        if (!messageText.isEmpty()) {
            String finalMessageText = messageText;
            member.getUser().openPrivateChannel().queue(privateChannel -> {
                privateChannel.sendMessage(finalMessageText).queue();
            });
        }
        newManualVerificationRequests.remove(this);

        //Log verification

    }

}
