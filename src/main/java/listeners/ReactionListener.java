package listeners;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import utils.Fun;
import verification.VerificationHub;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class ReactionListener extends ListenerAdapter {

    private EventWaiter eventWaiter;
    private Member lastVerified = null;

    public ReactionListener(EventWaiter eventWaiter) {
        this.eventWaiter = eventWaiter;

        Goldilocks.TIMER.schedule(() -> lastVerified = null, 5L, TimeUnit.MINUTES);

    }

    @Override
    public void onGuildMessageReactionAdd(@Nonnull GuildMessageReactionAddEvent event) {

        Member member = event.getMember();
        Guild guild = event.getGuild();
        User user = event.getUser();
        String messageId = event.getMessageId();
        TextChannel textChannel = event.getChannel();
        MessageReaction.ReactionEmote reactionEmote = event.getReactionEmote();

        if (event.getUser().isBot() || Database.isPub(guild)) { // Exit if it is pub
            return;
        }

        if (reactionEmote.isEmoji()) {
            if (reactionEmote.getEmoji().equals("✅")) {
                Role stickyRole = Database.isStickyRoleMessage(messageId);
                if (stickyRole != null) {
                    if (lastVerified != member && !member.getRoles().contains(stickyRole)) {
                        guild.addRoleToMember(member, stickyRole).queue();
                        lastVerified = member;
                    }
                }
            }
        }

        if (reactionEmote.isEmote()) {
            if (reactionEmote.getEmote().getId().equals("812134374559449088")) {
                Message message = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
                message.clearReactions(event.getReactionEmote().getEmote()).queue();
                if (!message.getContentRaw().isEmpty()) {
                    message.reply(Fun.clownMessage(message.getContentRaw().length() < 2000 ? message.getContentRaw() : message.getContentRaw().substring(0, 2000)).build()).queue();
                }
            }
            if (reactionEmote.getEmote().getId().equals("814357721230737459")) {
                Message message = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
                message.clearReactions(event.getReactionEmote().getEmote()).queue();
                if (!message.getContentRaw().isEmpty()) {
                    try {
                        message.reply("Don't worry boys I'll translate.").queue(message1 -> {
                            try {
                                message1.editMessage("**Here's a translation: **\n>>> " + Fun.translateText(message.getContentRaw().length() < 2000 ? message.getContentRaw() : message.getContentRaw().substring(0, 2000))).queue();
                            } catch (Exception e) {message1.editMessage("Nvm I don't got this.").queue();}
                        });
                    } catch (Exception e) {e.printStackTrace();}
                }
            }
            if (reactionEmote.getEmote().getId().equals("839367945359982612")) {
                Message message = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
                message.clearReactions(event.getReactionEmote().getEmote()).queue();
                if (!message.getContentRaw().isEmpty()) {
                    try {
                        String[] letters = {"🇦","🇧","🇨","🇩","🇪","🇫","🇬","🇭","🇮","🇯","🇰","🇱","🇲","🇳","🇴","🇵","🇶","🇷","🇸","🇹","🇺","🇻","🇼","🇽","🇾","🇿"};
                        StringBuilder newContent = new StringBuilder().append("*");
                        for (char c : message.getContentRaw().toLowerCase().replaceAll("[^a-z ]", "").toCharArray()) newContent.append(c == ' ' ? "⬛" : letters[c - 97]).append(c == ' ' ? "" : "*");
                        if (newContent.length() < 2000) message.reply(newContent.toString()).queue();
                    } catch (Exception e) {e.printStackTrace();}
                }
            }
        }

        if (guild.getId().equals("762883845925109781")) {
            if (event.getChannel().getId().equals("767942353784012840")) {

                Emote voids = Goldilocks.jda.getEmoteById(767811845947654204L);
                Emote cults = Goldilocks.jda.getEmoteById(766907072607682560L);
                Emote shatters = Goldilocks.jda.getEmoteById(723001214865899532L);
                Emote nest = Goldilocks.jda.getEmoteById(723001215407095899L);
                Emote fungal = Goldilocks.jda.getEmoteById(723001215696240660L);
                Emote amongUs = Goldilocks.jda.getEmoteById(767656425740304385L);

                if (event.getReactionEmote().getEmote().equals(voids) && !VerificationHub.dungeonVerifications.contains(member)) {
                    VerificationHub.requestVerificationDungeon(member, "voids");
                    return;
                } else if (event.getReactionEmote().getEmote().equals(cults) && !VerificationHub.dungeonVerifications.contains(member)) {
                    VerificationHub.requestVerificationDungeon(member, "cults");
                    return;
                } else if (event.getReactionEmote().getEmote().equals(shatters) && !VerificationHub.dungeonVerifications.contains(member)) {
                    VerificationHub.requestVerificationDungeon(member, "shatters");
                    return;
                } else if (event.getReactionEmote().getEmote().equals(nest) && !VerificationHub.dungeonVerifications.contains(member)) {
                    VerificationHub.requestVerificationDungeon(member, "nests");
                    return;
                } else if (event.getReactionEmote().getEmote().equals(fungal) && !VerificationHub.dungeonVerifications.contains(member)) {
                    VerificationHub.requestVerificationDungeon(member, "fungals");
                    return;
                } else if (event.getReactionEmote().getEmote().equals(amongUs) && !VerificationHub.dungeonVerifications.contains(member)) {
                    VerificationHub.requestVerificationDungeon(member, "amongus");
                    return;
                }
            }
        }

        if (Database.getGuildVerificationChannels(guild.getId()).contains(textChannel.getId())) {
            if (Database.getVerificationRuns(textChannel.getId()) != -1) {
                int runsRequirement = Database.getVerificationRuns(textChannel.getId());
                Role verifiedRole = guild.getRoleById(Database.getVerifiedRole(textChannel.getId()));
                if (!member.getRoles().contains(verifiedRole)) {
                    int memberRuns = Database.getRunsCompleted(member.getId(), guild.getId());
                    if (memberRuns >= runsRequirement) {
                        guild.addRoleToMember(member, verifiedRole).queue();
                        VerificationHub.veteranVerificationSuccess(verifiedRole, member, guild, memberRuns, true);
                        return;
                    } else {
                        VerificationHub.veteranVerificationSuccess(verifiedRole, member, guild, memberRuns, false);
                        event.getReaction().removeReaction().queue();
                        return;
                    }
                }
            }
        }

        if (!VerificationHub.generalVerifications.containsKey(user)) {
            List<String> verificationChannels = Database.getGuildVerificationChannels(event.getGuild().getId());
            if (event.getReactionEmote().isEmoji()) {
                if (verificationChannels.contains(event.getChannel().getId()) && event.getReactionEmote().getEmoji().equals("✅")) {
                    Role verifiedRole = guild.getRoleById(Database.getVerifiedRole(event.getChannel().getId()));
                    if (!member.getRoles().contains(verifiedRole) && !VerificationHub.hasOpenVerification(member)) {
                        VerificationHub.requestVerificationUser(event.getMember(), event.getChannel());
                    }
                }
            }
        }

//        try {
//            Message message = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
//            if (!message.getEmbeds().isEmpty()) {
//                if (message.getEmbeds().get(0).getFooter() != null && message.getEmbeds().get(0).getFooter().getText().contains("ARL-VOTE: ")) {
//                    message.editMessage(CommandTrlArlVote.renderEmbedBuilder(message).build()).queue();
//                }
//            }
//        } catch (Exception e) {}

    }

    @Override
    public void onGuildMessageReactionRemove(@Nonnull GuildMessageReactionRemoveEvent event) {

        Member member = event.getMember();
        Guild guild = event.getGuild();
        User user = event.getUser();
        String messageId = event.getMessageId();
        TextChannel textChannel = event.getChannel();

        Role stickyRole = Database.isStickyRoleMessage(messageId);

        if (stickyRole != null && member.getRoles().contains(stickyRole)) {
            guild.removeRoleFromMember(member, stickyRole).queue();
        }

//        try {
//            Message message = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
//            if (!message.getEmbeds().isEmpty()) {
//                if (message.getEmbeds().get(0).getFooter() != null && message.getEmbeds().get(0).getFooter().getText().contains("ARL-VOTE: ")) {
//                    message.editMessage(CommandTrlArlVote.renderEmbedBuilder(message).build()).queue();
//                }
//            }
//        } catch (Exception e) {}

    }
}
