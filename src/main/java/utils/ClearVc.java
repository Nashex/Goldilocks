package utils;

import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import setup.SetupConnector;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ClearVc {

    public static void clearVc(TextChannel textChannel, VoiceChannel voiceChannel) {

        if (Database.isFungal(textChannel.getGuild())) {

            Message message = textChannel.sendMessage(queueEmbed(voiceChannel, Utils.renderPercentage(0.0f, 8), "0.0").setTitle("Clearing Voicechannel").build()).complete();

            Goldilocks.TIMER.execute(() -> {
                String loungeChannelId = SetupConnector.getFieldValue(textChannel.getGuild(), "guildlogs", "loungeChannelId");
                VoiceChannel loungeChannel = null;
                if (!loungeChannelId.isEmpty()) loungeChannel = Goldilocks.jda.getVoiceChannelById(loungeChannelId);

                message.editMessage(queueEmbed(voiceChannel, Utils.renderPercentage(0.0f, 8), "0.00").build()).queue();
                int movedMembers = 0;
                int numMembers = voiceChannel.getMembers().size();
                for (Member member : voiceChannel.getMembers()) {
                    try {
                        textChannel.getGuild().moveVoiceMember(member, loungeChannel).complete();
                        if (movedMembers % 5 == 0) {
                            message.editMessage(queueEmbed(voiceChannel, Utils.renderPercentage((float) movedMembers / numMembers, 8),
                                    String.format("%.2f", ((float) movedMembers / numMembers) * 100)).build()).queue();
                        }
                        movedMembers++;
                    } catch (Exception e) { }
                }
                message.editMessage(queueEmbed(voiceChannel, Utils.renderPercentage(100.0f, 8),
                        "100.00").setColor(Goldilocks.GREEN).setTitle("Finished Clearing VoiceChannel").build()).queue();
            });
        } else {
            VoiceChannel tempVc = voiceChannel.createCopy().setPosition(voiceChannel.getPosition()).complete();

            Message message = textChannel.sendMessage("Clearing `" + voiceChannel.getName() + "`").complete();

            VoiceChannel finalVoiceChannel = voiceChannel;
            List<Member> members = voiceChannel.getMembers().stream().filter(member -> member.hasPermission(finalVoiceChannel, Permission.VOICE_SPEAK)).collect(Collectors.toList());
            members.forEach(member -> {
                try {
                    textChannel.getGuild().moveVoiceMember(member, tempVc).complete();
                } catch (Exception e) {
                }
            });
            System.out.println("Cleared VC with: " + members.size() + " members");
            voiceChannel.delete().queueAfter(500 * members.size(), TimeUnit.MILLISECONDS, aVoid -> message.editMessage("`" + voiceChannel.getName() + "` Successfully cleared!").queue());
        }
    }

    private static EmbedBuilder queueEmbed(VoiceChannel voiceChannel, String percentageBar, String percent) {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setColor(Goldilocks.YELLOW);
        embedBuilder.setDescription("\n **Moving Members from " + voiceChannel.getName() + ":**\n"
                + percentageBar + " | **" + percent +"%**");
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

}
