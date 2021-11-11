package raids;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import setup.SetupConnector;
import shatters.SqlConnector;
import utils.MemberSearch;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static raids.RaidHub.activeLoggingPanels;

public class LogPanel {

    @Expose PopperList poppers = new PopperList();
    @Expose Popper selected;
    @Expose List<Member> operators = new ArrayList<>();
    @Expose Message controlPanel;
    Raid raid = null;

    public LogPanel(TextChannel textChannel, Member... members) {
        //Create Embed
        this.operators = new ArrayList<>(Arrays.asList(members));
        activeLoggingPanels.add(this);
        createControlPanel(textChannel);
    }

    public LogPanel(Raid raid) {
        this.raid = raid;
        operators = raid.getAssistReactions();
        operators.add(raid.getRaidLeader());
        activeLoggingPanels.add(this);
        createControlPanel(raid.getRaidCommandsChannel());
    }

    public LogPanel link(Raid raid) {
        this.raid = raid;
        operators = raid.getAssistReactions();
        operators.add(raid.getRaidLeader());
        controlPanel.editMessage(panelEmbed().build()).queue();
        return this;
    }

    public void createControlPanel(TextChannel textChannel) {
        //Create Embed
        controlPanel = textChannel.sendMessage(panelEmbed().build()).complete();
        controlPanel.addReaction("🔼").queue();
        controlPanel.addReaction("🔽").queue();
        controlPanel.addReaction("🔍").queue();
        reactionHandler();
        cache();
    }

    public void deleteControlPanel() {
        deleteCache();
        controlPanel.delete().queue();
        //controlPanel = null;
    }

    public LogPanel(String json, Raid raid) {
        this.raid = raid;

        JSONObject jsonObject = new JSONObject(json);
        TextChannel textChannel = Goldilocks.jda.getTextChannelById((jsonObject.getJSONObject("controlPanel")).getString("channelId"));
        String messageId = (jsonObject.getJSONObject("controlPanel")).getString("id");

        if (textChannel == null) {
            Database.executeUpdate("DELETE FROM activeLogPanels WHERE messageId = " + messageId);
            return;
        }

        Guild guild = textChannel.getGuild();

        try {
            controlPanel = textChannel.retrieveMessageById(messageId).complete();
        } catch (Exception e) {
            System.out.println("Log Panel with Message Id: " + messageId + " in Guild " + guild + " not found.");
            Database.executeUpdate("DELETE FROM activeLogPanels WHERE messageId = " + messageId);
            return;
        }

        JSONArray popperArray = jsonObject.getJSONArray("poppers");
        if (!popperArray.isEmpty()) {
            for (int i = 0; i < popperArray.length(); i++) {
                JSONObject popperObject = popperArray.getJSONObject(i);
                if (popperObject.has("member")) {
                    Member member = guild.getMemberById((popperObject.getJSONObject("member")).getString("id"));
                    int numPopped = popperObject.getInt("numPopped");
                    String itemPopped = popperObject.getString("itemPopped");
                    poppers.add(new Popper(member, itemPopped, numPopped));
                }
            }
        }

        if (jsonObject.has("selected")) {
            JSONObject selectedObject = jsonObject.getJSONObject("selected");
            if (selectedObject.has("member")) {
                selected = poppers.get(guild.getMemberById(selectedObject.getJSONObject("member").getString("id")));
            }
        }

        JSONArray operatorArray = jsonObject.getJSONArray("operators");
        for (int i = 0; i < operatorArray.length(); i++) {
            operators.add(guild.getMemberById(((operatorArray.getJSONObject(i).getString("id")))));
        }
        controlPanel.editMessage(panelEmbed().build()).queue();
        reactionHandler();

        activeLoggingPanels.add(this);

        System.out.println("Successfully retrieved Log Panel for " + guild);
    }

    public void reactionHandler() {
        cache();
        if (controlPanel == null) return;
        Goldilocks.eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
             return !e.getUser().isBot() && (operators.contains(e.getMember()) || (raid != null && (raid.getAssistReactions().contains(e.getMember()) || raid.getRaidLeader().equals(e.getMember()))))
                     && e.getReactionEmote().isEmoji() && ("🔼🔽🔍" + (poppers.size() > 0 ? String.join("", Arrays.copyOfRange(Goldilocks.numEmotes, 1, poppers.size() + 1)) : "") + "❌")
                     .contains(e.getReactionEmote().getEmoji()) && e.getMessageId().equals(controlPanel.getId());
        }, e -> {

            String emoji = e.getReactionEmote().getEmoji();

            if (("❌").equals(emoji)) {
                deleteCache();
                controlPanel.editMessage(panelEmbed().setFooter("This panel has timed out.").build()).queue(message -> message.clearReactions().queue());
                return;
            }

            e.getReaction().removeReaction(e.getUser()).queue();

            if (("🔍").equals(emoji)) {
                searchMember(e.getMember());
                return;
            }

            if (selected != null) {
                if (("🔼").equals(emoji)) {
                    if (raid != null) {
                        selected.increment(e.getMember(), raid, getItemEmote(selected.itemPopped));
                    } else {
                        selected.increment();
                    }
                } else if (("🔽").equals(emoji)) {
                    selected.decrement();
                } else {
                    selected = poppers.get(Arrays.asList(Goldilocks.numEmotes).indexOf(emoji) - 1);
                }
            }

            controlPanel.editMessage(panelEmbed().build()).queue();
            reactionHandler();

        }, 30L, TimeUnit.MINUTES, () -> {
            deleteCache();
            if (controlPanel != null) controlPanel.editMessage(panelEmbed().setFooter("This panel has timed out.").build()).queue(message -> message.clearReactions().queue(null,
                    new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.UNKNOWN_CHANNEL)), new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.UNKNOWN_CHANNEL));

        });
    }

    public void searchMember(Member searcher) {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setColor(Goldilocks.WHITE)
                .setTitle("Member Search")
                .setDescription("Please enter the name, id, tag, or mention of the user to add them to the key logging panel. " +
                        "Once you do, select the member by entering the name from the result you would like. Feel free to type a new name " +
                        "if the one you wanted did not show up.")
                .setFooter("Type cancel to exit");

        Message message = controlPanel.getTextChannel().sendMessage(embedBuilder.build()).complete();
        searchHandler(message, searcher);
    }

    public void searchHandler(Message message, Member searcher) {
        Goldilocks.eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(searcher.getUser()) && e.getMessage().getContentRaw().split(" ").length == 1;
        }, e -> {
            String string = e.getMessage().getContentRaw().split(" ")[0];

            if (string.equalsIgnoreCase("cancel")) {
                message.delete().queue();
                e.getMessage().delete().queue();
                reactionHandler();
                return;
            }

            Member member = MemberSearch.memberSearch(string, searcher.getGuild());

            if (member == null) {
                List<Member> members = MemberSearch.potentialMemberSearch(string, searcher.getGuild(), false);
                message.editMessage(searchResultEmbed(members, string).build()).queue();
                e.getMessage().delete().queue();
                searchHandler(message, searcher);
            } else {
                addPopper(member, message, searcher.getUser());
                // message.delete().queue();
                e.getMessage().delete().queue();
                reactionHandler();
            }

        }, 2L, TimeUnit.MINUTES, () -> {
            message.delete().queue();
        });
    }

    public EmbedBuilder searchResultEmbed(@NotNull List<Member> members, String name) {
        int overTen = members.size() - 10;
        if (members.size() > 10) members = members.subList(0, 10);
        int maxLength = members.stream().map(member -> member.getEffectiveName().length()).max(Integer::compareTo).orElse(0);
        String result = members.stream().map(member -> "`" +
                String.format("%-" + maxLength + "s", member.getEffectiveName()) + "`" + member.getAsMention()).collect(Collectors.joining("\n"));
        result += overTen > 0 ? "\nAnd " + overTen + " others..." : "";

        return new EmbedBuilder()
                .setColor(Goldilocks.WHITE)
                .setTitle("Member Search")
                .setDescription("Please enter the name, id, tag, or mention of the user to add them to the key logging panel. " +
                        "Once you do, select the member by entering their corresponding numbers. Feel free to type a new name " +
                        "if the one you wanted did not show up." )
                .addField("Result for: `" + name + "`", (result.isEmpty() ? "No members were found, please enter a new name or cancel" : result), false)
                .setFooter("Type cancel to exit");

    }

    public void addPopper(Member member, Message message, User author) {
        if (raid != null) {
            List<Emote> options = raid.getEarlyLocEmotes().stream().filter(emote -> emote.getName().toLowerCase().contains("key")
                    || emote.getName().toLowerCase().contains("rune")).collect(Collectors.toList());
            options.add(raid.getKeyEmote());

            if (options.size() > 1) {
                getOption(options, message, member, author);
                return;
            } else {
                poppers.add(member, options.get(0).getName().toLowerCase().contains("key") ? (raid.isDefaultRaid() ? "key" : "eventKey") : options.get(0).getName());
            }
        } else {
            List<Emote> options = new ArrayList<>(getQuotaEmotes(member.getGuild()));
            options.addAll(Arrays.asList(getItemEmote("eventKey"), getItemEmote("Vial"), getItemEmote("SwordRune"), getItemEmote("ShieldRune"), getItemEmote("HelmetRune")));
            getOption(options, message, member, author);
            return;
        }

        message.delete().queue();
        if (poppers.size() == 1) selected = poppers.get(0);
        controlPanel.addReaction(Goldilocks.numEmotes[poppers.size()]).queue();
        controlPanel.editMessage(panelEmbed().build()).queue();
        cache();
    }

    public void getOption(List<Emote> options, Message message, Member member, User author) {
        String result = "";
        for (int i = 1; i <= options.size(); i++) result += "`" + String.format("%1$2d", i) + "`: " + options.get(i - 1).getAsMention() + " `" + options.get(i - 1).getName() + "`\n";
        EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("Which Item are you Logging for " + member.getEffectiveName())
                .setColor(Goldilocks.WHITE)
                .setDescription("**Options**\n" + result)
                .setFooter("Type cancel to exit");

        message.editMessage(embedBuilder.build()).queue();

        Goldilocks.eventWaiter.waitForEvent(GuildMessageReceivedEvent.class,
            e -> e.getAuthor().equals(author) && (e.getMessage().getContentRaw().replaceAll("[^0-9]", "").length() > 0 || e.getMessage().getContentRaw().equalsIgnoreCase("cancel")), e -> {
                String content = e.getMessage().getContentRaw();

                if (content.equalsIgnoreCase("cancel")) {
                    e.getMessage().delete().queue();
                    message.delete().queue();
                    return;
                }

                int option = Integer.parseInt(content.replaceAll("[^0-9]", ""));
                if (option > 0 && option <= options.size()) {
                    String itemName = options.get(option - 1).getName();
                    if (raid == null) itemName = itemName.toLowerCase().contains("key") ? (itemName.equalsIgnoreCase("eventkey") ? "eventKey" : "key") : itemName;
                    else itemName = itemName.toLowerCase().contains("key") ? (raid.isDefaultRaid() ? "key" : "eventKey") : itemName;
                    poppers.add(member, itemName);
                    if (poppers.size() == 1) selected = poppers.get(0);
                    controlPanel.addReaction(Goldilocks.numEmotes[poppers.size()]).queue();
                    controlPanel.editMessage(panelEmbed().build()).queue();
                    cache();
                }
                else {
                    getOption(options, message, member, author);
                    return;
                }
                e.getMessage().delete().queue();
                message.delete().queue();
            }, 2L, TimeUnit.MINUTES, () -> {
            message.delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
        });
    }

    public boolean addPopper(Member member, Raid raid, String itemName) {
        boolean added = poppers.add(member,  itemName.toLowerCase().contains("key") ? (raid.isDefaultRaid() ? "key" : "eventKey") : itemName);
        if (poppers.size() == 1) selected = poppers.get(0);
        controlPanel.addReaction(Goldilocks.numEmotes[poppers.size()]).queue();
        controlPanel.editMessage(panelEmbed().build()).queue();
        cache();
        return added;
    }

    private @NotNull EmbedBuilder panelEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        int maxLength = poppers.stream().map(popper -> popper.member.getEffectiveName().replaceAll("[^A-Za-z |]", "").length()).max(Integer::compareTo).orElse(0);

        String popperString = poppers.stream().map(p -> getItemEmote(p.itemPopped).getAsMention() + ": `" +
                String.format("%-" + maxLength + "s", p.member.getEffectiveName().replaceAll("[^A-Za-z |]", ""))
                 + "` | " + (p.equals(selected) ? "**" : "") + "Num Added: `" + p.numPopped + "`" + (p.equals(selected) ? " ⇐ Selected**" : ""))
                .collect(Collectors.joining("\n"));

        embedBuilder.setTitle((raid == null ? "Generic" : raid.getDungeonName()) + " Logging Panel for " + operators.get(0).getEffectiveName())
                .setColor(raid == null ? Goldilocks.LIGHTBLUE : raid.getRaidColor())
                .setDescription("**Controls**\n" +
                                "**`Select `** 🔢 React with the corresponding number of the user, if they have multiple items on them they may have more than one number\n" +
                                "**`Add    `** 🔼 Use this to log an item for the selected member\n" +
                                "**`Remove `** 🔽 Use this to remove an item from the selected member\n" +
                                "**`Search `** 🔍 Use this to add a member to the logging panel, you can search their name, id, tag, or mention\n")
                .addField("Poppers", popperString.isEmpty() ? "You currently have no poppers add some with 🔍" : popperString, false);

        return embedBuilder;
    }

    public String serialize() {
        JsonSerializer<Member> memberJsonSerializer = new JsonSerializer<Member>() {
            @Override
            public JsonElement serialize(Member src, Type typeOfSrc, JsonSerializationContext context) {
                JsonObject jsonMember = new JsonObject();
                jsonMember.addProperty("id", src.getId());
                jsonMember.addProperty("name", src.getEffectiveName());
                jsonMember.addProperty("avatarUrl", src.getUser().getAvatarUrl());
                return jsonMember;
            }
        };

        JsonSerializer<Popper> popperJsonSerializer = new JsonSerializer<Popper>() {
            @Override
            public JsonElement serialize(Popper src, Type typeOfSrc, JsonSerializationContext context) {
                JsonObject jsonPopper = new JsonObject();
                jsonPopper.addProperty("numPopped", src.numPopped);
                jsonPopper.addProperty("itemPopped", src.itemPopped);
                jsonPopper.add("member", memberJsonSerializer.serialize(src.member, typeOfSrc, context));
                return jsonPopper;
            }
        };

        JsonSerializer<Message> messageJsonSerializer = new JsonSerializer<Message>() {
            @Override
            public JsonElement serialize(Message src, Type typeOfSrc, JsonSerializationContext context) {
                JsonObject jsonMember = new JsonObject();
                if (src != null) {
                    jsonMember.addProperty("id", src.getId());
                    jsonMember.addProperty("channelId", src.getTextChannel().getId());
                } else {
                    jsonMember.addProperty("id", "null");
                    jsonMember.addProperty("channelId", "null");
                }
                return jsonMember;
            }
        };

        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Member.class, memberJsonSerializer)
                .registerTypeAdapter(Message.class, messageJsonSerializer)
                .registerTypeAdapter(Popper.class, popperJsonSerializer)
                .setPrettyPrinting()
                .create();
        return gson.toJson(this);
    }

    public LogPanel cache() {
        String serialData = serialize();
        String sql = "";
        if (raid == null) {
            sql = "INSERT INTO activeLogPanels (guildId, messageId, json) " +
                    "VALUES (" + controlPanel.getGuild().getId() + ", " + controlPanel.getId() + ", '" + serialData + "')" +
                    "ON CONFLICT(messageID) DO UPDATE SET json = '" + serialData + "'";

        } else {
            sql = "UPDATE activeRaids SET logPanel = '" + serialData + "' WHERE " +
                    "raidMessageId = " + raid.getRaidMessageId();
        }
        Database.executeUpdate(sql);
        return this;
    }

    public void deleteCache() {
        String sql = "DELETE FROM activeLogPanels WHERE messageId = " + controlPanel.getId();
        activeLoggingPanels.remove(this);
        Database.executeUpdate(sql);
    }

    public Emote getItemEmote(String name) {
        Emote emote = Database.getEmote(name);
        if (raid != null && raid.isDefaultRaid() && name.equals("key")) return raid.getKeyEmote();
        if (emote == null) return Goldilocks.jda.getEmoteById("841558297869549578");
        return emote;
    }

    public List<Emote> getQuotaEmotes(Guild guild) {
        String[] quotaString = SetupConnector.getFieldValue(guild, "guildInfo", "quotaString").split(" ");
        List<Emote> emotes = new ArrayList<>();
        if (quotaString.length == 0) return emotes;
        for (String s : quotaString) {
            Emote emote = Goldilocks.jda.getEmoteById(DungeonInfo.dungeonInfo(guild, Integer.parseInt(s)).dungeonInfo[1]);
            if (!emotes.contains(emote)) emotes.add(emote);
        }
        return emotes;
    }

    private static class PopperList extends ArrayList<Popper> {
        public PopperList() {
            super();
        }

        public boolean add(Member member, String itemPopped) {
            return add(new Popper(member, itemPopped, 0));
        }

        public boolean add(Member member, String itemPopped, int numPopped) {
            if (this.size() >= 10) return false;
            return super.add(new Popper(member, itemPopped, numPopped));
        }

        @Nullable
        public Popper get(Member member) {
            return stream().filter(p -> p.member.equals(member)).findAny().orElse(null);
        }

        @Nullable
        public String get(String id) {
            return stream().map(popper -> popper.member.getId()).filter(s -> s.equals(id)).findAny().orElse(null);
        }
    }

    private static class Popper {
        Member member;
        int numPopped;
        String itemPopped;

        public Popper(Member member, String itemPopped) {
            this(member, itemPopped, 0);
        }

        public Popper(Member member, String itemPopped, int numPopped) {
            this.member = member;
            this.itemPopped = itemPopped;
            this.numPopped = numPopped;
        }

        public int increment(Member adder, Raid raid, Emote emote) {
            log(1, raid, adder, emote);
            return ++numPopped;
        }

        public int increment() {
            log(1);
            return ++numPopped;
        }

        public int decrement() {
            log(-1);
            return --numPopped;
        }

        public void log(int inc, Raid raid, Member adder, Emote emote) {
            String keyLogChannelId = Database.query("keyLogChannelId", "raidSections", "raidStatusChannel = " + raid.getRaidStatusChannel().getId());
            TextChannel keyLogChannel = raid.getRaidGuild().getTextChannelById(keyLogChannelId);
            if (keyLogChannel != null) {
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.clear().setTitle("Items Added by " + adder.getEffectiveName())
                        .setColor(raid.getRaidColor());
                embedBuilder.setDescription("`" + inc + "` " + emote.getAsMention() + " has been added to " + member.getAsMention() + ".");
                keyLogChannel.sendMessage(embedBuilder.build()).queue();
            }
            log(inc);
        }

        public void log(int inc) {
            String sql = "INSERT INTO popperLogs (guildId, userId, itemPopped, numPopped) VALUES (" + member.getGuild().getId() + ", "
                    + member.getId() + ", '" + itemPopped + "', " + inc + ")";
            Database.executeUpdate(sql);
            // TODO Remove this
            if (itemPopped.toLowerCase().contains("key")) Database.addKeys(member, inc);
            if (Database.isShatters(member.getGuild())) {
                switch (itemPopped) {
                    case "key":
                        SqlConnector.logFieldForMember(member, Collections.singletonList("shatterspops"), 1);
                        break;
                    case "eventKey":
                        SqlConnector.logFieldForMember(member, Collections.singletonList("eventpops"), 1);
                        break;
                    default:
                        break;
                }
            }

        }
    }

}
