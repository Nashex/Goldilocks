package raids.queue;

import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.*;
import java.util.stream.Collectors;

public class Queue {

    final private long MAX_TIME = 600000;
    final private long MIN_REFRESH = 5000;
    private LinkedHashMap<Member, Long> queueMap = new LinkedHashMap<>();
    public TextChannel rsChannel;
    public TextChannel queueChannel;
    private List<Message> queueMessages = new ArrayList<>();
    private long lastRefresh;

    public Queue(TextChannel rsChannel) {
        this.rsChannel = rsChannel;
        //if ((queueChannel = Goldilocks.jda.getTextChannelById(SetupConnector.getFieldValue(guild, "guildInfo", "queueChannelId"))) == null) return;
        queueChannel = rsChannel.createCopy().syncPermissionOverrides().setPosition(rsChannel.getPosition() - 1).setName("queue-status").complete();
        List<EmbedBuilder> embedBuilders = queueEmbeds();
        embedBuilders.forEach(e -> queueMessages.add(queueChannel.sendMessage(e.build()).complete()));
        lastRefresh = System.currentTimeMillis();
    }

    public void updateQueueMessages() {
        if (queueChannel == null) return;
        int i;
        List<EmbedBuilder> embedBuilders = queueEmbeds();
        if (embedBuilders.size() == queueMessages.size()) {
            for (i = 0; i < queueMessages.size(); i++) queueMessages.get(i).editMessage(embedBuilders.get(i).build()).queue();
        }
        if (embedBuilders.size() < queueMessages.size()) {
            for (i = 0; i < embedBuilders.size(); i++) queueMessages.get(i).editMessage(embedBuilders.get(i).build()).queue();
            for (; i < queueMessages.size(); i++) queueMessages.get(i).delete().queue();
        }
        if (embedBuilders.size() > queueMessages.size()) {
            for (i = 0; i < queueMessages.size(); i++) queueMessages.get(i).editMessage(embedBuilders.get(i).build()).queue();
            for (; i < embedBuilders.size(); i++) queueMessages.add(queueChannel.sendMessage(embedBuilders.get(i).build()).complete());
        }
        lastRefresh = System.currentTimeMillis();
    }

    /*
    GUI and Display
     */

    public List<EmbedBuilder> queueEmbeds() {
        List<EmbedBuilder> embeds = new ArrayList<>();
        int embedIdx = 0;
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setColor(Goldilocks.LIGHTBLUE)
                .setTitle("Queue for " + queueChannel.getParent().getName())
                .setDescription("The following message contains everyone in the queue for this raiding category. " +
                        "You are automatically entered into the queue you reacted to an afk-check but did not make " +
                        "it into the run.");
        embeds.add(embedBuilder);

        String memberPosString = this.toString();
        String[] memberPosEntries = memberPosString.split("\n");
        String currentField = "";
        int fieldIdx = 0;
        for (String mPos : memberPosEntries) {
            if (currentField.length() + mPos.length() < 1000 && embeds.get(embedIdx).length() < 4900) {
                currentField += mPos + "\n";
            } else {
                if (embeds.get(embedIdx).length() >= 4900) {
                    embeds.add(new EmbedBuilder().setColor(Goldilocks.LIGHTBLUE));
                    embedIdx++;
                }
                if (currentField.length() + mPos.length() >= 1000) {
                    embeds.get(embedIdx).addField(fieldIdx == 0 ? "Members in Queue" : " ", (fieldIdx++ != 0 ? "" : "**`Pos` |  `Time Left`  | `Member`**\n") + currentField, false);
                    currentField = mPos + "\n";
                }
            }
        }

        if (!currentField.isEmpty()) embeds.get(embedIdx).addField(fieldIdx == 0 ? "Members in Queue" : "", (fieldIdx != 0 ? "" : "**`Pos` |  `Time Left`  | `Member`**\n") + currentField, false);
        else if (fieldIdx == 0) embeds.get(embedIdx).addField("Members in Queue", "None", false);
        embeds.get(embedIdx).setFooter("Last Updated").setTimestamp(new Date().toInstant());
        return embeds;
    }

    /*
    Member Management
     */

    public void addMember(Member member) {
        if (!queueMap.containsKey(member)) {
            queueMap.put(member, System.currentTimeMillis());
            if (System.currentTimeMillis() - lastRefresh > MIN_REFRESH) updateQueueMessages();
        }
        cleanQueue();
    }

    public void addMembers(List<Member> members) {
        members.forEach(m -> {
            if (!queueMap.containsKey(m)) queueMap.put(m, System.currentTimeMillis());
        });
        if (System.currentTimeMillis() - lastRefresh > MIN_REFRESH) updateQueueMessages();
        cleanQueue();
    }

    public void removeMembers(List<Member> members) {
        members.forEach(m -> {
            queueMap.remove(m);
        });
        updateQueueMessages();
        cleanQueue();
    }

    public void cleanQueue() {
        List<Member> invalidEntries = new ArrayList<>();
        queueMap.forEach((member, aLong) -> {
            if (System.currentTimeMillis() - aLong > MAX_TIME) invalidEntries.add(member);
        });
        invalidEntries.forEach(m -> queueMap.remove(m));
    }

    public List<Member> getQueuedMembers(int numMembers) {
        cleanQueue();
        return queueMap.entrySet().stream().map(e -> e.getKey()).collect(Collectors.toList()).subList(0, numMembers > queueMap.size() ? numMembers : queueMap.size() - 1);
    }

    public List<Member> getAllQueuedMembers() {
        cleanQueue();
        return queueMap.entrySet().stream().map(e -> e.getKey()).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        PrettyTime p = new PrettyTime();
        LinkedList<Member> entries = queueMap.entrySet().stream().map(e -> e.getKey()).collect(Collectors.toCollection(LinkedList::new));
        return queueMap.entrySet().stream().map(e -> "**`" + StringUtils.leftPad("" + entries.indexOf(e.getKey()), 3,"0") + "`**" +
                 " | `" + StringUtils.leftPad(formatTime(System.currentTimeMillis() - e.getValue()), 9, " ") + "` | " + e.getKey().getAsMention()).collect(Collectors.joining("\n"));
    }

    public String formatTime(long time) {
        time = 600 - time / 1000;
        return  (time / 60 == 0 ? "" : time / 60 + " m ") + StringUtils.leftPad("" + time % 60, 2, "0") + " s";
    }

    private class QueuedMember {

        public Member member;
        public int priority;
        public long timeJoined;

        public QueuedMember(Member member) {
            this.member = member;
            priority = 0;
            timeJoined = System.currentTimeMillis();
        }
    }

}



