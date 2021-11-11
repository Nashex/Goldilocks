package listeners;

import giveaways.GiveawayHub;
import lobbies.LobbyManagerHub;
import main.Config;
import main.Database;
import main.Goldilocks;
import misc.NewsHub;
import net.dv8tion.jda.api.entities.*;
import quota.QuotaManager;
import moderation.punishments.PunishmentManager;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;
import quota.DataCollector;
import raids.caching.RaidCaching;
import shatters.SqlConnector;
import verification.VerificationHub;
import verification.VerificationRequest;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Calendar.*;
import static main.Goldilocks.jda;

public class ReadyListener extends ListenerAdapter {


    @Override
    public void onReady(ReadyEvent e) {

        System.out.println(1);

        String activity = ((String) Config.get("ACTIVITY")).split(" ")[0];
        String activityMessage = StringUtils.split((String) Config.get("ACTIVITY"), " ", 2)[1];

        switch (activity.toLowerCase()){
            case "listening":
                jda.getPresence().setPresence(Activity.listening(activityMessage), false);
                break;
            case "streaming":
                String streamUrl = ((String) Config.get("ACTIVITY")).split(" ")[1];
                activityMessage = StringUtils.split((String) Config.get("ACTIVITY"), " ", 3)[2];
                jda.getPresence().setPresence(Activity.streaming(activityMessage, streamUrl), false);
                break;
            case "watching":
                jda.getPresence().setPresence(Activity.watching(activityMessage), false);
                break;
            case "competing":
                jda.getPresence().setPresence(Activity.competing(activityMessage), false);
                break;
            case "playing":
                //Goldilocks.jda.getPresence().setPresence(Activity.playing(activityMessage), false);
                break;
        }

        String serverlist = "\nThis Bot is running on " + e.getJDA().getGuilds().size() + " servers: \n";

        for (Guild g : e.getJDA().getGuilds()) {
            serverlist += "    - " + g.getName() + " {ServerID: " + g.getId() + ", Members: " + g.getMembers().size() + "} \n";
        }

        System.out.println(serverlist);

        RaidCaching.retrieveRaids();
        scheduleTimer();

        if (!jda.getSelfUser().getId().equals("770776162677817384")) {
            new DataCollector();

            LobbyManagerHub.retrieveCachedLobbys();
            RaidCaching.retrieveHeadcounts();
            Goldilocks.TIMER.schedule(Database::retrieveVerificationRequests, 0L , TimeUnit.SECONDS);
            new GiveawayHub();
            new NewsHub();

        }

        Goldilocks.registerSlashCommands();

        for (Guild guild : jda.getGuilds()) {
            QuotaManager.updateQuotaMessage(guild);
        }

        try {
            //Goldilocks.TIMER.schedule(() -> PunishmentHub.retrieveSuspensions(), 0L , TimeUnit.SECONDS);
        } catch (Exception e2) {
            System.out.println("Failed to retrieve Suspensions");
        }
        //PunishmentHub.startPunishmentManager();
        PunishmentManager.startPunishmentManager();

        //Goldilocks.emoteCache.cacheServer = Goldilocks.jda.getGuildById(Config.get("CACHE_SERVER").toString());
        //PunishmentConnector.applyRolesToUsers(Goldilocks.jda.getGuildById("514788290809954305"));

        SqlConnector.testConnection();
        VerificationHub.retrieveVerifications();
        Database.retrieveActiveLogPanels();

        for (Guild guild : jda.getGuilds()) {
            try {
                Category activeVeriCategory = Database.getActiveVerificationCategory(guild);
                if (activeVeriCategory != null) {
                    List<TextChannel> activeVeris = activeVeriCategory.getTextChannels().stream().collect(Collectors.toList());
                    activeVeris.forEach(textChannel -> {
                        try {
                            if (Optional.ofNullable(textChannel.getTopic()).orElse(null) != null) {
                                String[] veriInfo = textChannel.getTopic().replace("|", ",").split(",");
                                VerificationRequest verificationRequest = new VerificationRequest(guild.getMemberById(veriInfo[1].split(":")[1].replaceAll(" ", "")),
                                        veriInfo[2].split(":")[1].replaceAll(" ", ""), jda.getTextChannelById(veriInfo[6].split(":")[1].replaceAll(" ", "")),
                                        (!veriInfo[4].contains("N/A") ? textChannel.retrieveMessageById(veriInfo[4].split(":")[1].replaceAll(" ", "")).complete() : null),
                                        (!veriInfo[5].contains("N/A") ? textChannel.retrieveMessageById(veriInfo[5].split(":")[1].replaceAll(" ", "")).complete() : null), textChannel);
                                verificationRequest.setPlayerName(veriInfo[3].split(":")[1].replaceAll(" ", ""));
                                //if (!veriInfo[4].contains("N/A")) verificationRequest.setInitialMessage(textChannel.retrieveMessageById(veriInfo[4].split(":")[1].replaceAll(" ", "")).complete());
                                //if (!veriInfo[5].contains("N/A")) verificationRequest.setNameRequestMessage(textChannel.retrieveMessageById(veriInfo[5].split(":")[1].replaceAll(" ", "")).complete());
                                VerificationHub.generalVerifications.put(jda.getUserById(veriInfo[1].split(":")[1].replaceAll(" ", "")), verificationRequest);
                                System.out.println("Retrieved Verification for: " + textChannel.getTopic());
                            }
                        } catch (Exception e2) {
                            System.out.println("Failed to retrieve active verification for: " + textChannel.getTopic());
                        }
                    });

                }
            } catch (Exception e1) {
                System.out.println("Failed to retrieve active verifications for: " + guild);
            }
        }
        //SlashCommandHub.updateCommands();
    }

    public static void scheduleTimer() {

        Calendar calendar = Calendar.getInstance();
        int milliDelay = calendar.get(MILLISECOND);
        milliDelay += calendar.get(SECOND)*1000;

        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeZone(TimeZone.getTimeZone("America/New_York"));
                int day = calendar.get(DAY_OF_WEEK);
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int min = calendar.get(MINUTE);
                //System.out.println("Day: " + day + " Hour: " + hour + " Min: " + min + " Sec: " + calendar.get(SECOND));

                //Resets the quota every Sunday at 12AM EST
                if (day == SATURDAY && hour == 23 && min == 59) {
                        QuotaManager.resetQuota();
                }

                if (day == WEDNESDAY && hour == 0 && min == 0) {

                    //End the weekly vote
                }

                //Updates the quotaembed every minute
                String resetDay = ((7 - day) == 1) ? (7 - day) + " Day " : (7 - day) + " Days ";
                String resetHour = ((23 - hour) == 1) ? (23 - hour) + " Hour " : (23 - hour) + " Hours ";
                String resetMin = ((59 - min) < 2) ? (59 - min) + " Minute" : (59 - min) + " Minutes";
                String resetTime = (7 - day) == 0 ? resetHour + resetMin : resetDay + resetHour + resetMin;

                if (System.currentTimeMillis() - Goldilocks.timeStarted > 60000) QuotaManager.updateQuotaTimes(resetTime);

                if (min % 5 == 0) Database.logPing();
            }
        };
        timer.schedule(timerTask, 60000-milliDelay, 60000);
    }
}
