package raids;

import customization.RaidLeaderPrefsConnector;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import shatters.SqlConnector;
import utils.Utils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static main.Goldilocks.eventWaiter;
import static main.Goldilocks.numEmotes;

public class KeyPanel {

    public Map<Member, Integer> keyPoppers = new HashMap<>();
    private int keyPopperIndex;
    private int numSelected = -1;
    private List<Member> operators = new ArrayList<>();
    private Raid raid;
    private TextChannel keyLogChannel = null;
    private boolean minimized = false;
    private int oryxKeys = 0;

    public Message controlPanel = null;

    public KeyPanel(List<Member> operators, Raid raid) {
        this.operators = operators;
        operators.add(raid.getRaidLeader());
        this.raid = raid;

        List<TextChannel> keyPopChannels = raid.getVoiceChannel().getParent().getTextChannels().stream()
                .filter(channel -> channel.getName().contains("key") && channel.getName().contains("logs") && PermissionUtil.checkPermission(raid.getRaidGuild().getSelfMember(), Permission.VIEW_CHANNEL))
                .collect(Collectors.toList());

        if (keyPopChannels.size() > 0) {
            keyLogChannel = keyPopChannels.get(0);
        }

        minimized = !RaidLeaderPrefsConnector.getRlKeyCp(raid.getRaidLeader());

        createControlPanel();

    }

    public KeyPanel(List<Member> operators, Raid raid, Message message) {
        this.operators = operators;
        operators.add(raid.getRaidLeader());
        this.raid = raid;

        List<TextChannel> keyPopChannels = raid.getVoiceChannel().getParent().getTextChannels().stream().filter(channel -> channel.getName().contains("key")).collect(Collectors.toList());

        if (keyPopChannels.size() > 0) {
            keyLogChannel = keyPopChannels.get(0);
        }

        for (Member member : raid.keyReacts) {
            keyPoppers.put(member, 0);
        }

        if (keyPoppers.size() > 0) numSelected = 0;

        minimized = false;

        controlPanel = message;
        updateControlPanel();
        reactionHandler();
    }

    private void createControlPanel() {
        controlPanel = raid.getRaidCommandsChannel().sendMessage(minimized ? miniControlPanel().build() : controlPanel().build()).complete();
        controlPanel.addReaction("⬆").queue();
        controlPanel.addReaction("⬇").queue();
        controlPanel.addReaction(minimized ? "💠" : "🔹").queue();

        reactionHandler();

    }

    public void addKeyPopper(Member member) {
        keyPoppers.put(member, 0);
        controlPanel.addReaction(numEmotes[keyPoppers.size()]).queue();
        if (numSelected == -1) {
            numSelected = 0;
        }
        updateControlPanel();
        //RaidCaching.cacheRaid(raid);
    }

    public void addKeyPopper(Member member, int numKeys) {
        keyPoppers.put(member, numKeys);
        controlPanel.addReaction(numEmotes[keyPoppers.size()]).queue();
        if (numSelected == -1) {
            numSelected = 0;
        }
        updateControlPanel();
    }

    private void updateControlPanel() {
        if (minimized) controlPanel.editMessage(miniControlPanel().build()).queue();
        else controlPanel.editMessage(controlPanel().build()).queue();
    }

    private void reopenControlPanel() {
        controlPanel.editMessage(controlPanel().build()).queue();
        controlPanel.clearReactions("💠").queue();
        controlPanel.addReaction("🔹").queue();
    }

    private void minimizeControlPanel() {
        controlPanel.editMessage(miniControlPanel().build()).queue();
        controlPanel.clearReactions("🔹").queue();
        controlPanel.addReaction("💠").queue();
    }

    public void deleteControlPanel() {
        controlPanel.delete().queue();
        controlPanel = null;
    }

    private void reactionHandler() {
        if (controlPanel == null) {
            return;
        }
        eventWaiter.waitForEvent(MessageReactionAddEvent.class, e -> {
            return controlPanel != null && e.getMessageId().equals(controlPanel.getId()) && operators.contains(e.getMember()) && e.getReactionEmote().isEmoji() && (("⬆⬇🔹💠").contains(e.getReactionEmote().getEmoji())
            || Arrays.asList(numEmotes).contains(e.getReactionEmote().getEmoji()));
        }, e -> {
            String emoji = e.getReactionEmote().getEmoji();

            if (emoji.equals("⬆")) {
                Member keyPopper = null;
                if (numSelected != -1) {
                    keyPopper = (Member) keyPoppers.keySet().toArray()[numSelected];
                    if (!Database.isShatters(raid.raidGuild)) Database.addKeys(keyPopper, 1);
                    else {
                        if (raid.isDefaultRaid()) {
                            SqlConnector.logFieldForMember(keyPopper, Arrays.asList(new String[]{"shatterspops"}), 1);
                        } else {
                            SqlConnector.logFieldForMember(keyPopper, Arrays.asList(new String[]{"eventpops"}), 1);
                        }
                    }
                    int temp = keyPoppers.get(keyPopper) + 1;
                    keyPoppers.replace(keyPopper, temp);

                    List<Role> eligibleRoles = Database.eligibleKeyRoles(Database.getKeysPopped(keyPopper.getId(), keyPopper.getGuild().getId()), keyPopper.getGuild().getId());
                    if (!eligibleRoles.isEmpty()) {
                        for (Role role : eligibleRoles) {
                            if (!keyPopper.getRoles().contains(role)) {
                                keyPopper.getGuild().addRoleToMember(keyPopper, role).queue();
                                if (!Database.getKeyRoleMessage(role).isEmpty()) {
                                    Utils.sendPM(keyPopper.getUser(), Database.getKeyRoleMessage(role));
                                }
                            }
                        }
                    }
                } else {
                    Database.addKeys(1L, raid.getRaidGuild(), 1);
                    oryxKeys++;
                }

                updateControlPanel();

                if (keyLogChannel != null) {
                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.clear().setTitle("Keys Added by " + e.getMember().getEffectiveName())
                            .setTimestamp(new Date().toInstant())
                            .setColor(Goldilocks.BLUE);
                    embedBuilder.setDescription("1 key has been added to " + (keyPopper == null ? "Oryx" : keyPopper.getAsMention()) + " who now has a total of `" +
                            Database.getKeysPopped((keyPopper == null ? "1" : keyPopper.getId()), raid.getRaidGuild().getId()) + " keys`");

                    try {
                        keyLogChannel.sendMessage(embedBuilder.build()).complete();
                    } catch (Exception e1) {}

                }
            }

            if (emoji.equals("⬇")) {
                Member keyPopper = null;
                if (numSelected != -1) {
                    keyPopper = (Member) keyPoppers.keySet().toArray()[numSelected];
                    if (!Database.isShatters(raid.raidGuild)) Database.addKeys(keyPopper, -1);
                    else {
                        if (raid.isDefaultRaid()) {
                            SqlConnector.logFieldForMember(keyPopper, Arrays.asList(new String[]{"shatterspops"}), -1);
                        } else {
                            SqlConnector.logFieldForMember(keyPopper, Arrays.asList(new String[]{"eventpops"}), -1);
                        }
                    }
                    int temp = keyPoppers.get(keyPopper) - 1;
                    keyPoppers.replace(keyPopper, temp);
                } else {
                    Database.addKeys(1L, raid.getRaidGuild(), -1);
                    oryxKeys--;
                }

                if (keyLogChannel != null) {
                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.clear().setTitle("Keys Added by " + e.getMember().getEffectiveName())
                            .setTimestamp(new Date().toInstant())
                            .setColor(Goldilocks.BLUE);
                    embedBuilder.setDescription("1 key has been removed from " + (keyPopper == null ? "Oryx" : keyPopper.getAsMention()) + " who now has a total of `" +
                            Database.getKeysPopped((keyPopper == null ? "1" : keyPopper.getId()), raid.getRaidGuild().getId()) + " keys`");

                    try {
                        keyLogChannel.sendMessage(embedBuilder.build()).complete();
                    } catch (Exception e1) {}

                }

                updateControlPanel();

            }

            if (emoji.equals("🔹")) {
                minimizeControlPanel();
                minimized = true;
            }

            if (emoji.equals("💠")) {
                reopenControlPanel();
                minimized = false;
            }

            if (Arrays.asList(numEmotes).contains(emoji)) {
                numSelected = Arrays.asList(numEmotes).indexOf(e.getReactionEmote().getEmoji()) - 1;
                updateControlPanel();
            }

            e.getReaction().removeReaction(e.getUser()).queue();
            reactionHandler();

        }, 1, TimeUnit.HOURS, () -> {
            if (controlPanel != null) controlPanel.delete();
        });
    }

    private EmbedBuilder controlPanel() {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        String keyPopperString = "";
        int index = 0;
        Iterator keyPopIterator = keyPoppers.entrySet().iterator();
        while (keyPopIterator.hasNext() && index < 10) {
            Map.Entry keyPopperInfo = (Map.Entry) keyPopIterator.next();
            Member keyPopper = (Member) keyPopperInfo.getKey();
            int totalPops = 0;
            try {
                totalPops = Integer.parseInt(SqlConnector.shattersStats(keyPopper.getUser())[raid.isDefaultRaid() ? 1 : 2]);
            } catch (Exception e) {}
            keyPopperString += (index == numSelected ? "**" : "") + numEmotes[index + 1] + ": " + keyPopper.getAsMention() +
                    " | Keys Added: " + keyPoppers.get(keyPopper) + (Database.isShatters(raid.raidGuild) ? " | Total Pops: " + totalPops : "")
                    + (index == numSelected ? "**\n" : "\n");
            index++;
        }

        Member curKeyPopper = numSelected == -1 ? null : (Member) keyPoppers.keySet().toArray()[numSelected];

        embedBuilder.setTitle("Key Control Panel for " + raid.getVoiceChannel().getName())
                .setColor(raid.getRaidColor())
                .setDescription("To add a key to " + (numSelected == -1 ? "Oryx" : curKeyPopper.getAsMention()) + " please make sure you are reacted with " + raid.getAssist().getAsMention() + " on `" + raid.getVoiceChannel().getName() + "`. " +
                        "Use ⬆ and ⬇ to add and remove keys. Use the number emotes to select who to add keys to." +
                        (keyPopperString.length() == 0 ? "" : "\n\n**Key Poppers:**\n" + keyPopperString) + (numSelected == -1 ? "\n" : "") +
                        "\nYou are currently controlling keys for: " + (numSelected == -1 ? "**Oryx** | Keys Added: " + oryxKeys : curKeyPopper.getAsMention()));
        embedBuilder.setFooter("Minimize this control panel by reacting with 🔹")
                .setTimestamp(new Date().toInstant());

        return embedBuilder;
    }

    private EmbedBuilder miniControlPanel() {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        String keyPopperString = "";
        int index = 0;
        Iterator keyPopIterator = keyPoppers.entrySet().iterator();
        while (keyPopIterator.hasNext()) {
            Map.Entry keyPopperInfo = (Map.Entry) keyPopIterator.next();
            Member keyPopper = (Member) keyPopperInfo.getKey();
            keyPopperString += (index == numSelected ? "**" : "") + numEmotes[index + 1] + ": " + keyPopper.getAsMention() + (index == numSelected ? " ⇐ **\n" : "\n");
            index++;
        }
        Member curKeyPopper = numSelected == -1 ? null : (Member) keyPoppers.keySet().toArray()[numSelected];

        embedBuilder.setTitle("Keys added for " + (curKeyPopper == null ? "Oryx" : curKeyPopper.getEffectiveName()) + ": " + (curKeyPopper == null ? oryxKeys : keyPoppers.get(curKeyPopper)))
                .setColor(raid.getRaidColor());
        if (keyPopperString.length() > 0) embedBuilder.setDescription("\n\n**Key Poppers:**\n" + keyPopperString);
        embedBuilder.setFooter("To maximize this control panel this react with 💠")
                .setTimestamp(new Date().toInstant());

        return embedBuilder;
    }

}
