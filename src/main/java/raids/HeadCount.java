package raids;

import lombok.Getter;
import lombok.Setter;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.apache.commons.lang3.StringUtils;
import raids.caching.RaidCaching;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static raids.RaidHub.activeRaids;


@Getter
@Setter
public class HeadCount {

    private String dungeonInfo[];
    private Member raidLeader;
    private Guild raidGuild;
    private TextChannel raidCommandsChannel;
    private TextChannel raidStatusChannel;
    private int raidType;
    private Emote dungeonEmote;
    private Emote keyEmote;
    private List<Emote> earlyLocEmotes = new ArrayList<>();
    private String dungeonName;
    private List<Emote> additionalEmotes = new ArrayList<>();
    private Color raidColor;
    private String dungeonImage;
    private Message headCountMessage;
    private String headCountMessageId;
    private Message controlPanel;
    private String controlPanelId;
    private HashMap<User, Emote> earlyLocationMap = new HashMap<>();

    public long timeStarted = System.currentTimeMillis();

    public HeadCount(Member raidLeader, TextChannel raidCommandsChannel, TextChannel raidStatusChannel, int raidType) {
        this.dungeonInfo = (raidType != -2 ? DungeonInfo.dungeonInfo(raidLeader.getGuild(), raidType).dungeonInfo : DungeonInfo.eventDungeon);
        this.raidLeader = raidLeader;
        this.raidGuild = raidLeader.getGuild();
        this.raidCommandsChannel = raidCommandsChannel;
        this.raidStatusChannel = raidStatusChannel;
        this.raidType = raidType;

        this.dungeonEmote = getEmote(dungeonInfo[0]);

        //Get key emote of the raid
        this.keyEmote = getEmote(dungeonInfo[1]);

        //Create a list of early location emotes
        if (!dungeonInfo[2].isEmpty()) {
            for (String emoteId : dungeonInfo[2].split(" ")) {
                if (emoteId.contains(",")) {
                    String emoteString[] = emoteId.split(",");
                    Emote emote = getEmote(emoteString[0].trim());;
                    earlyLocEmotes.add(emote);
                } else {
                    earlyLocEmotes.add(getEmote(emoteId.trim()));
                }
            }
        }
        this.dungeonName = dungeonInfo[3];
        //Create a list of additional raid emotes
        if (!dungeonInfo[4].isEmpty()) {
            for (String emoteId : dungeonInfo[4].split(" ")) {
                additionalEmotes.add(getEmote(emoteId.trim()));
            }
        }
        //Todo add color grabbing for rls.
        this.raidColor = new Color(Integer.valueOf(dungeonInfo[5]));

        if (!dungeonInfo[7].isEmpty()) {
            try {
                dungeonImage = String.valueOf(new URL(dungeonInfo[7]));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        createHeadCount();
    }

    public HeadCount(Member raidLeader, TextChannel raidCommandsChannel, TextChannel raidStatusChannel, int raidType, Message controlPanel, Message headCountMessage) {
        this.dungeonInfo = (raidType != -2 ? DungeonInfo.dungeonInfo(raidGuild, raidType).dungeonInfo : DungeonInfo.eventDungeon);
        this.raidLeader = raidLeader;
        this.raidGuild = raidLeader.getGuild();
        this.raidCommandsChannel = raidCommandsChannel;
        this.raidStatusChannel = raidStatusChannel;
        this.raidType = raidType;

        this.dungeonEmote = getEmote(dungeonInfo[0]);

        //Get key emote of the raid
        this.keyEmote = getEmote(dungeonInfo[1]);

        //Create a list of early location emotes
        if (!dungeonInfo[2].isEmpty()) {
            for (String emoteId : dungeonInfo[2].split(" ")) {
                earlyLocEmotes.add(getEmote(emoteId.trim()));
            }
        }
        this.dungeonName = dungeonInfo[3];
        //Create a list of additional raid emotes
        if (!dungeonInfo[4].isEmpty()) {
            for (String emoteId : dungeonInfo[4].split(" ")) {
                additionalEmotes.add(getEmote(emoteId.trim()));
            }
        }
        //Todo add color grabbing for rls.
        this.raidColor = new Color(Integer.valueOf(dungeonInfo[5]));

        if (!dungeonInfo[7].isEmpty()) {
            try {
                dungeonImage = String.valueOf(new URL(dungeonInfo[7]));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        this.headCountMessage = headCountMessage;
        headCountMessageId = headCountMessage.getId();
        this.controlPanel = controlPanel;
        controlPanelId = controlPanel.getId();

        controlListener();
    }


    public void createHeadCount() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(raidLeader.getEffectiveName().replaceAll("[^A-Za-z0-9]", "") + "'s " + dungeonName + " Headcount")
                .setDescription(raidLeader.getEffectiveName() + " has started a " + dungeonName + " headcount! If you have a key or are bringing any of the below items be sure to react with the corresponding emotes. ");
        embedBuilder.setThumbnail(dungeonImage);
        embedBuilder.setColor(raidColor);
        embedBuilder.setFooter("This headcount will end once the raid leader gets their required classes");
        headCountMessage = raidStatusChannel.sendMessage("@here " + raidLeader.getEffectiveName() + " has started a new " + dungeonName + " headcount. Please react with your respective class so that the raid leader can start as quickly as possible.").complete();
        headCountMessageId = headCountMessage.getId();
        headCountMessage.editMessage(embedBuilder.build()).queue();
        headCountMessage.addReaction(dungeonEmote).queue();
        headCountMessage.addReaction(keyEmote).queue();
        for (Emote emote : earlyLocEmotes) {
            headCountMessage.addReaction(emote).queue();
        }
        if (raidType == 4) headCountMessage.addReaction(getEmote("822247366463717417")).queue();
        for (Emote emote : additionalEmotes) {
            headCountMessage.addReaction(emote).queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
        }
        createControlPanel();
    }

    public void deleteHeadCount() {
        RaidHub.activeHeadcounts.remove(this);

        if (Database.deleteMessages(raidGuild)) {
            headCountMessage.delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            controlPanel.delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
        } else {
            headCountMessage.clearReactions().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            headCountMessage.editMessage("This headcount has now ended.").queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));

            controlPanel.editMessage(controlPanel().build())
                    .setActionRows()
                    .queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
        }

        RaidCaching.deleteHeadCount(headCountMessage.getId());
    }

    public void createControlPanel() {
        controlPanel = raidCommandsChannel.sendMessage(controlPanel().build())
                .setActionRow(
                        Button.primary("convert", "Convert to Raid"),
                        Button.danger("stop", "End Headcount")
                        )
                .complete();
        controlPanelId = controlPanel.getId();

        RaidCaching.createHeadCount(this);
        controlListener();
    }

    public void controlListener() {
        List<String> controls = Arrays.asList("convert", "stop");

        Goldilocks.eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return controls.contains(e.getComponentId()) && e.getUser().equals(raidLeader.getUser())
                    && Objects.equals(controlPanel, e.getMessage());
        }, e -> {
            String control = e.getComponentId();

            if (control.equals("convert")) {
                // Check to see if the raid guild has static channels
                if (Database.hasStaticChannels(raidGuild)) {
                    if (e.getMember().getVoiceState().inVoiceChannel()) {
                        activeRaids.add(new Raid(raidLeader, raidType , raidStatusChannel, raidCommandsChannel, "", false, -1, e.getMember().getVoiceState().getChannel()));
                    } else {
                        e.reply("You are not currently in a voice channel please join one and then re-click the button").setEphemeral(true).queue();
                        controlListener();
                        return;
                    }
                } else {
                    activeRaids.add(new Raid(raidLeader, raidType , raidStatusChannel, raidCommandsChannel, "", false, -1, null));
                }
            }

            e.deferEdit().queue();
            deleteHeadCount();

        }, 1L, TimeUnit.HOURS, this::deleteHeadCount);
    }

    public void updateControlPanel() {
        controlPanel.editMessage(controlPanel().build()).queue();
    }

    public EmbedBuilder controlPanel() {
        String commandDescription = "**Controls: **" +
                "\nPress the convert button to turn this headcount into a raid";
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Control Panel for " + raidLeader.getEffectiveName() + "'s " + dungeonName + " Headcount")
                .setColor(raidColor)
                .setDescription(commandDescription);
        final String[] additionalKeyReacts = {""};
        if (dungeonName.equals("Random Epic Dungeons") || dungeonName.equals("Random Court Dungeons") || dungeonName.contains("Alien") || raidType == -2) {
            earlyLocationMap.forEach(((user, emote) -> {
                additionalKeyReacts[0] += ", " + raidGuild.getMember(user).getAsMention() + "[" + emote.getAsMention() + "]";
            }));
            additionalKeyReacts[0] = StringUtils.replaceOnce(additionalKeyReacts[0], ", ", "");
            embedBuilder.addField("Key Reactions: ", additionalKeyReacts[0].isEmpty() ? "None" : additionalKeyReacts[0], false);
        } else {
            String keyReacts = getMemberReactionString(keyEmote, headCountMessage);
            if (raidType != 0) {
                embedBuilder.addField( dungeonName + " key reacts", keyReacts.isEmpty() ? "None" : keyReacts, false);
            } else {
                embedBuilder.addField( "Incantation reacts", keyReacts.isEmpty() ? "None" : keyReacts, false);
            }
            for (Emote emote : earlyLocEmotes) {
                if (!emote.equals(keyEmote)) {
                    final String[] currentString = {""};
                    earlyLocationMap.forEach(((user, emote1) -> {
                        if (emote1 == emote) {
                            currentString[0] += ", " + raidGuild.getMember(user).getAsMention();
                        }
                    }));
                    currentString[0] = StringUtils.replaceOnce(currentString[0], ", ", "");
                    final String[] displayName = {""};
                    String s = emote.getName().replace("of", "Of").replace("the", "The");
                    Arrays.stream(s.split("(?=\\p{Upper})")).forEach(s1 -> displayName[0] += s1 + " ");
                    embedBuilder.addField(StringUtils.capitalize(displayName[0]).trim() + " reacts:", currentString[0].isEmpty() ? "None" : currentString[0], false);
                }
            }
        }
        embedBuilder.setFooter("Press the stop button to end this headcount");
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    public Emote getEmote(String emoteId) {
        return Goldilocks.jda.getEmoteById(emoteId);
    }

    public String getMemberReactionString(Emote emote, Message message) {
        List<User> reactions = message.retrieveReactionUsers(emote).complete();
        String reacts = "";
        for (User user : reactions) {
            if (!user.isBot()) {
                reacts += raidGuild.getMember(user).getAsMention();
                if (reactions.indexOf(user) != reactions.size() - 2) {
                    reacts += ", ";
                }
            }
        }
        return reacts;
    }

    public void addEarlyLocReact(User user, Emote emote) {
        earlyLocationMap.put(user, emote);
    }

    public void removeEarlyLocReact(User user, Emote emote) {
        earlyLocationMap.remove(user, emote);
    }

}
