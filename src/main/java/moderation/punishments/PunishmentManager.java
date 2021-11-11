package moderation.punishments;

import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PunishmentManager {

    public static ScheduledExecutorService TIMER = new ScheduledThreadPoolExecutor(2);
    public static List<Punishment> activePunishments = new ArrayList<>();

    public static void startPunishmentManager() {
        // Retrieve all of the active suspensions
        List<Guild> guilds = Goldilocks.jda.getGuilds().stream().filter(guild -> Database.getGuildInfo(guild, "rank").equals("3")).collect(Collectors.toList());
        guilds.forEach(PunishmentManager::retrievePunishments);

        TIMER.scheduleWithFixedDelay(() -> {
            List<Punishment> inactivePunishments = new ArrayList<>();
            try { // Try catch to avoid stoppage
                activePunishments.forEach(punishment -> {
                    // Check the suspensions
                    if (punishment instanceof Suspension) {
                        Suspension suspension = (Suspension) punishment;
                        if (System.currentTimeMillis() > suspension.timeEnding) {
                            suspension.unsuspend(null);
                            inactivePunishments.add(suspension);
                        }
                    }
                    // Check the mutes
                    if (punishment instanceof Mute) {
                        Mute mute = (Mute) punishment;
                        if (System.currentTimeMillis() > mute.timeEnding) {
                            mute.unmute(null);
                            inactivePunishments.add(mute);
                        }
                    }
                });

                activePunishments.removeAll(inactivePunishments);
            } catch (Exception e) { e.printStackTrace(); }

        }, 0L, 5L, TimeUnit.SECONDS);
    }

    public static Suspension getSuspension(Member member) {
        for (Punishment punishment : activePunishments) {
            if (punishment instanceof Suspension) {
                if (punishment.recipient != null && punishment.recipient.equals(member)) {
                    return (Suspension) punishment;
                }
            }
        }
        return null;
    }

    public static Mute getMute(Member member) {
        for (Punishment punishment : activePunishments) {
            if (punishment instanceof Mute) {
                if (punishment.recipient != null && punishment.recipient.equals(member)) {
                    return (Mute) punishment;
                }
            }
        }
        return null;
    }

    public static void retrievePunishments(Guild guild) {
        String sql = "SELECT punisherId, userId, strftime('%s',timeStarted) AS timeStarted, strftime('%s',timeEnded) AS timeEnded, caseId, " +
                "logMessageId, nickname, roleIds, reason, punishmentType FROM punishments WHERE (datetime('now') < timeEnded) AND (punishmentType = 'suspension' OR punishmentType = 'mute') AND guildId = " + guild.getId();
        try (Connection conn = DriverManager.getConnection(Database.dbUrl);
             Statement stmt = conn.createStatement()) {
            ResultSet resultSet = stmt.executeQuery(sql);

            while (resultSet.next()) {
                String punishmentType = resultSet.getString("punishmentType");

                String punisherId = resultSet.getString("punisherId");
                String recipientId = resultSet.getString("userId");
                String reason = resultSet.getString("reason");
                long timeStarted = resultSet.getLong("timeStarted") * 1000;
                long timeEnded = resultSet.getLong("timeEnded") * 1000;
                String caseId = resultSet.getString("caseId");
                String roleIds = resultSet.getString("roleIds");
                String messageId = resultSet.getString("logMessageId");
                String nickName = resultSet.getString("nickname");

                // Params (String guildId, String recipientId, String modId, String reason, long timeIssued, long timeEnding, String caseId, List<String> roleIds, String logMessageId, String nickname)
                if (punishmentType.equals("suspension")) {
                    activePunishments.add(new Suspension(guild.getId(), recipientId, punisherId, reason, timeStarted, timeEnded, caseId,
                            Arrays.asList(roleIds.split(" ")), messageId, nickName));
                } else if (punishmentType.equals("mute")) {
                    activePunishments.add(new Mute(guild.getId(), recipientId, punisherId, reason, timeStarted, timeEnded, caseId,
                            Arrays.asList(roleIds.split(" ")), messageId, nickName));
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
