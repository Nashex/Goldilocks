package raids.caching;

import main.Goldilocks;
import net.dv8tion.jda.api.entities.*;
import raids.LogPanel;
import raids.Raid;

import java.util.Arrays;

import static raids.RaidHub.activeRaids;

public class RaidCache {
    String guildId, leaderId, raidStatusChannelId, raidCommandsChannelId, location, voiceChannelId, raidMessageId,
            controlPanelId, feedbackChannelId, feedbackMessageId, keyPanelId, logPanelJson;
    int raidType, customCap;
    boolean isActive;

    public RaidCache(String guildId, String leaderId, int raidType, String raidStatusChannelId, String raidCommandsChannelId, String location,
                     int customCap, String voiceChannelId, String raidMessageId, String controlPanelId, boolean isActive, String feedbackChannelId,
                     String feedbackMessageId, String keyPanelId, String logPanelJson) {
        this.guildId = guildId;
        this.leaderId = leaderId;
        this.raidType = raidType;
        this.raidStatusChannelId = raidStatusChannelId;
        this.raidCommandsChannelId = raidCommandsChannelId;
        this.location = location;
        this.customCap = customCap;
        this.voiceChannelId = voiceChannelId;
        this.raidMessageId = raidMessageId;
        this.controlPanelId = controlPanelId;
        this.isActive = isActive;
        this.feedbackChannelId = feedbackChannelId;
        this.feedbackMessageId = feedbackMessageId;
        this.keyPanelId = keyPanelId;
        this.logPanelJson = logPanelJson;

    }

    public boolean retrieve() throws RaidCacheException {
        // Testing bot and Testing servers
        if (Goldilocks.jda.getSelfUser().getId().equals("770776162677817384") &&
                !Arrays.asList(new String[]{"733482900841824318", "799161165871316992", "822132404197785600", "842131103019040788"}).contains(guildId)) return false;

        System.out.println("Retrieving Raid #" + activeRaids.size() + " from Cache!");

        Guild guild = Goldilocks.jda.getGuildById(guildId);
        if (guild == null) throw new RaidCacheException("Invalid Guild Id from Raid Cache: " + guildId);

        Member raidLeader = guild.getMemberById(leaderId);
        if (raidLeader == null) throw new RaidCacheException(guild.getName() + " | Invalid Raid Leader Id from Raid Cache: " + leaderId);

        TextChannel raidStatusChannel = guild.getTextChannelById(raidStatusChannelId);
        if (raidStatusChannel == null) throw new RaidCacheException(guild.getName() + " | Invalid Raid Status Channel Id from Raid Cache: " + raidStatusChannelId);

        TextChannel raidCommandsChannel = guild.getTextChannelById(raidCommandsChannelId);
        if (raidCommandsChannel == null) throw new RaidCacheException(guild.getName() + " | Invalid Raid Commands Channel Id from Raid Cache: " + raidCommandsChannelId);

        VoiceChannel voiceChannel = guild.getVoiceChannelById(voiceChannelId);
        if (voiceChannel == null) throw new RaidCacheException(guild.getName() + " | Invalid Voice Channel Id from Raid Cache: " + voiceChannelId);

        Message raidMessage, controlPanel, feedbackMessage = null;
        try {
            raidMessage = raidStatusChannel.retrieveMessageById(raidMessageId).complete();
        } catch (Exception e) { throw new RaidCacheException(guild.getName() + " | Invalid Raid Message Id from Raid Cache: " + raidMessageId); }

        try {
            controlPanel = raidCommandsChannel.retrieveMessageById(controlPanelId).complete();
        } catch (Exception e) { throw new RaidCacheException(guild.getName() + " | Invalid Control Panel Message Id from Raid Cache: " + controlPanelId); }

        // Can be null
        TextChannel feedbackChannel = Goldilocks.jda.getTextChannelById(feedbackChannelId);

        if (feedbackChannel != null) feedbackMessage = feedbackChannel.retrieveMessageById(feedbackMessageId).complete();

        Raid raid = new Raid(raidLeader, raidType, raidStatusChannel, raidCommandsChannel, location, true, customCap, voiceChannel);

        // Retrieve the log panel from cache and set it to the raid.
        LogPanel logPanel = null;
        if (!logPanelJson.equals(" ")) logPanel = new LogPanel(logPanelJson, raid);

        raid.retrieveRaid(voiceChannel, raidMessage, controlPanel, isActive, feedbackChannel, feedbackMessage, logPanel);



        System.out.println(guild.getName() + " | Successfully Retrieved Raid #" + activeRaids.size() + " from Cache!");
        return activeRaids.add(raid);
    }
}
