package misc;

import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import raids.RaidHub;
import setup.SetupConnector;
import utils.Charts;
import verification.VerificationHub;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class StatusHub {
    public StatusHub() {
        Goldilocks.TIMER.scheduleWithFixedDelay(() -> {

            List<Guild> guilds = Goldilocks.jda.getGuilds().stream()
                    .filter(g -> (SetupConnector.getFieldValue(g, "guildInfo", "rank").equals("3") || Database.isPub(g)) &&
                            !SetupConnector.getFieldValue(g, "guildLogs", "statusChannelId").isEmpty())
                    .collect(Collectors.toList());

            for (Guild g : guilds) {
                String textchannelId = SetupConnector.getFieldValue(g, "guildLogs", "statusChannelId");
                TextChannel textChannel = Goldilocks.jda.getTextChannelById(textchannelId);
                if (textChannel != null) {
                    // Retrieve status message from db table
                }
            }

        }, 0L, 5L, TimeUnit.MINUTES);
    }

    public static EmbedBuilder statusEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Goldilocks.BLUE)
                .setAuthor("Goldilocks Status", null, Goldilocks.jda.getSelfUser().getAvatarUrl())
                .addField("API Latencies", "```ini\n" +
                        String.format("[Discord]: %1$4dms  [Google]: %2$4dms", Goldilocks.jda.getGatewayPing(), 110) + "\n```", false)
                .addField("Database Latencies", "```ini\n" +
                        String.format("[ViBot]: %1$4dms    [Raid]: %2$4dms    [Gen]: %3$4dms", 407, 121, 1) + "\n```", true)
                .addField("Active Processes", "```ini\n" +
                        String.format("[Raids]: %1$2d        [Veris]: %2$2d", RaidHub.activeRaids.size(), VerificationHub.newManualVerificationRequests.size()) + "\n```", false)
                .setImage(Charts.createPingChart());
        return embedBuilder;
    }

}
