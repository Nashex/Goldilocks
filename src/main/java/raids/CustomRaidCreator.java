package raids;

import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static main.Goldilocks.eventWaiter;

public class CustomRaidCreator {

    Member member;
    TextChannel textChannel;
    Message controlPanel;
    Message raidPreview;
    String dungeonName = "";
    Emote portalEmote = getEmote("723001215184535553");
    Emote keyEmote = getEmote("771201091840901122");
    List<Emote> earlyLocationEmotes = new ArrayList<>();
    List<Emote> additionalEmotes = new ArrayList<>();
    private Emote[] defaultEmotes = {getEmote("771681007086075964"), getEmote("823935665514610738"), getEmote("771680219382677514")};
    int vcCap = 50;
    boolean isOpen;


    public CustomRaidCreator(Member member, TextChannel textChannel, int raidTypeNum) {
        this.member = member;
        this.textChannel = textChannel;
        raidPreview = textChannel.sendMessage(raidMessage().build()).complete();
        controlPanel = textChannel.sendMessage(controlPanelEmbed().build()).complete();
        addReactions();
        reactionHandler();
    }

    public void recreateControlPanel() {
        controlPanel.editMessage(controlPanelEmbed().build()).queue();
        addReactions();
        reactionHandler();
    }

    public void reactionHandler() {

        controlPanel.editMessage(controlPanelEmbed().build()).queue();
        addReactions();

        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMember().equals(member) && e.getReactionEmote().isEmoji() && ("🏷🚪🔑🗺🔢🔊").contains(e.getReactionEmote().getEmoji()) && isOpen;
        }, e -> {

            String emoji = e.getReactionEmote().getEmoji();
            if (emoji.equals("🏷")) setDungeonName();
            if (emoji.equals("🚪")) {
                //Bring to portal emote selection
            }
            if (emoji.equals("🔑")) {
                //keyEmote = promptForEmote(Arrays.asList("767819019960188980"));
            }
            if (emoji.equals("🗺")) {
                //Bring to early location emote selection. maximum of 3
            }
            if (emoji.equals("🔢")) {
                //Bring to additional emote selection. maximum of 6
            }
            if (emoji.equals("🔊")) {
                //Bring to vc cap selection
            }

        }, 2L, TimeUnit.MINUTES, () -> {
            if (isOpen) {
                controlPanel.delete().queue();
                raidPreview.delete().queue();
            }
        });

        List<String> commands = Arrays.asList("close", "save");

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getMember().equals(member) && commands.contains(e.getMessage().getContentRaw().toLowerCase()) && isOpen;
        }, e -> {

            String command = e.getMessage().getContentRaw().toLowerCase();
            if (("close").equals(command)) {
                isOpen = false;
                controlPanel.delete().queue();
                raidPreview.delete().queue();
            }


        }, 2L, TimeUnit.MINUTES, () -> {
        });
    }

    private EmbedBuilder controlPanelEmbed() {
        EmbedBuilder controlPanel = new EmbedBuilder();

        controlPanel.setAuthor(member.getEffectiveName() + "'s Custom Raid", member.getUser().getAvatarUrl(), member.getUser().getAvatarUrl())
                .setTitle(dungeonName.isEmpty() ? member.getEffectiveName() + "'s Raid" : member.getEffectiveName() + "'s " + dungeonName)
                .setColor(Goldilocks.WHITE)
                .setFooter("React with 📥 when you are finished to save your raid.")
                .setTimestamp(new Date().toInstant());

        String earlyLocEmotesString = "";
        for (Emote e : earlyLocationEmotes) earlyLocEmotesString += e.getAsMention();
        String additionalEmotesString = "";
        for (Emote e : additionalEmotes) additionalEmotesString += e.getAsMention();


        String description = "This is your custom raid creation control panel! " +
                "Below you will see a few fields that you can set for your raid. " +
                "In order to change the value of each of the fields please make sure " +
                "to react with the emote listed beside it.\n" +
                "\n🏷 **| Dungeon Name: ⇒** " + (dungeonName.isEmpty() ? "`None Set`" : dungeonName) +
                "\n↳ This is the name of your raid, this is what will be sent out as " +
                "a ping when you start the raid." +
                "\n\n🚪 | **Portal Emote:  ⇒** " + (portalEmote == null ? "`None Set`" : portalEmote.getAsMention()) +
                "\n↳ This is what you tell your raiders to react to make sure that they get their run logged." +
                "\n\n🔑 | **Key Emote:  ⇒** " + (keyEmote == null ? "`None Set`" : keyEmote.getAsMention()) +
                "\n↳ This is the key reaction for your afk check." +
                "\n\n🗺 | **Early Location Emotes  ⇒** " + (earlyLocationEmotes.isEmpty() ? "`None Set`" : earlyLocEmotesString) +
                "\n↳ Any raiders that react to the emotes you set here will be given early location." +
                "\n\n🔢 | **Additional Emotes  ⇒** " + (additionalEmotes.isEmpty() ? "`None Set`" : additionalEmotesString) +
                "\n↳ These are additional emotes that you would like added to your afk check." +
                "\n\n🔊 | **Default VC Cap ⇒** `" + vcCap + "`";

        controlPanel.setDescription(description);

        return controlPanel;
    }

    private void setDungeonName() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Please enter the name for your AFK check type.")
                .setDescription("This is the name of your raid, this is what will be sent out " +
                        "a ping when you start the raid.")
                .setColor(customization.RaidLeaderPrefsConnector.getRlColor(member.getId(), member.getGuild().getId()) == null ?
                        Goldilocks.GOLD : customization.RaidLeaderPrefsConnector.getRlColor(member.getId(), member.getGuild().getId()))
                .setFooter("Type close at anytime to exit name creation.")
                .setTimestamp(new Date().toInstant());

        controlPanel.clearReactions().queue();
        controlPanel.editMessage(embedBuilder.build()).queue();

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getMember().equals(member);
        }, e -> {
            String entry = e.getMessage().getContentRaw();
            e.getMessage().delete().queue();

            if (("close").equalsIgnoreCase(entry)) {
                recreateControlPanel();
            }

            dungeonName = entry;
            recreateControlPanel();
            raidPreview.editMessage(raidMessage().build()).queue();

        }, 2L, TimeUnit.MINUTES, () -> {
            if (isOpen) controlPanel.delete().queue();
        });
    }

    public void addReactions() {
        controlPanel.addReaction("🏷").queue();
        controlPanel.addReaction("🚪").queue();
        controlPanel.addReaction("🔑").queue();
        controlPanel.addReaction("🗺").queue();
        controlPanel.addReaction("🔢").queue();
        controlPanel.addReaction("🔊").queue();
        if (!dungeonName.isEmpty() && portalEmote != null && keyEmote != null) controlPanel.addReaction("📥").queue();
    }

    private void promptForEmote(List<String> guildIds, String emote) {
        controlPanel.clearReactions().queue();
        controlPanel.editMessage(emoteEmbed().build()).queue();

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getMember().equals(member) && e.getChannel().equals(textChannel);
        }, e -> {
            if (e.getMessage().getContentRaw().toLowerCase().equals("close")) {
                controlPanel.delete().queue();
                e.getMessage().delete().queue();
                return;
            }

            findEmote(guildIds, e.getMessage().getContentRaw().toLowerCase(), emote);
            e.getMessage().delete().queue();

        }, 2L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });

    }

    public void findEmote(List<String> guildIds, String emoteName, String type) {
        //Get emotes from the specified guilds
        List<Guild> guilds = guildIds.stream().map(guildId -> Goldilocks.jda.getGuildById(guildId)).collect(Collectors.toList());
        List<Emote> emoteList = new ArrayList<>();
        guilds.forEach(guild -> emoteList.addAll(guild.getEmotes()));

        //Get all the matching emotes and put them in a list and visible string
        String emoteString = "\n**__Options: __**\n";
        int index = 1;
        List<Emote> possibleEmotes = emoteList.stream().filter(emote -> emote.getName().toLowerCase().contains(emoteName.toLowerCase())).collect(Collectors.toList());
        for (Emote emote : possibleEmotes) {
            if (index > 20) break;
            final String[] displayName = {""};
            String emoteTempName = emote.getName();
            displayName[0] = emoteTempName.replace("of", "Of").replace("the", "The");
            Arrays.stream(emoteTempName.split("(?=\\p{Upper})")).forEach(s1 -> displayName[0] += s1 + " ");
            emoteString += "**" + index + ".** " + emote.getAsMention() + "⇒ " + displayName[0] + "\n";
            index++;
        }

        EmbedBuilder embedBuilder = emoteEmbed();
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("Here are the following emotes found for your entry of `" + emoteName + "`\nPlease enter the number your corresponding emote is linked to." +
                "\n\nIf *none of these emotes match the one you wanted feel free to **type a new name*** in chat\n" + (possibleEmotes.size() == 0 ?  "\n**__Options:__**\nNo emotes match `" + emoteName + "`" : emoteString));
        embedBuilder.setFooter("To exit this menu type close at any time");

        controlPanel.editMessage(embedBuilder.build()).queue();

        final Emote[] selectedEmote = {null};
        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getMember().equals(member);
        }, e -> {

            if (e.getMessage().getContentRaw().equalsIgnoreCase("close")) {
                controlPanel.delete().queue();
                e.getMessage().delete().queue();
                return;
            }

            if (!Utils.isNumeric(e.getMessage().getContentRaw())) {
                //findEmote(guildIds, emoteName);
                e.getMessage().delete().queue();
                return;
            }

            int choice = Integer.parseInt(e.getMessage().getContentRaw());
            if (choice > 0 && choice <= possibleEmotes.size()) {
                e.getMessage().delete().queue();
                selectedEmote[0] = possibleEmotes.get(choice);
                reactionHandler();
            } else {
                e.getMessage().delete().queue();
                Utils.errorMessage("Could Not Select Emote", "The choice indicated was not a valid option", textChannel, 10L);

            }

        }, 2L, TimeUnit.MINUTES, () -> {

        });

        //return selectedEmote[0];

    }

    private EmbedBuilder emoteEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.setTitle("Emote Selection for " + member.getEffectiveName());
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("Please enter the key words of the emote you would like to select.\nOne word entries like `tome` and `wand` work best.");
        embedBuilder.setFooter("Please type close at any time to exit this menu");
        embedBuilder.setTimestamp(new Date().toInstant());

        return embedBuilder;
    }

    public EmbedBuilder raidMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        List<Emote> raidLeaderEmotes = customization.RaidLeaderPrefsConnector.getRlEmotes(member.getId(), member.getId());
        List<Emote> debuffEmotes = Arrays.asList(getEmote("768995607905566740"), getEmote("771659502839398411"),
                getEmote("771659502780153877"), getEmote("771659503099445248"), getEmote("771659503094464522"));
        Emote nitro = Goldilocks.jda.getEmoteById("771683299106095105");

        embedBuilder.setAuthor(member.getEffectiveName().replaceAll("[^A-Za-z0-9- ]", "") + "'s " + (dungeonName.isEmpty() ? "Custom" : dungeonName) + " Raid", null, member.getUser().getAvatarUrl())
                .setColor(Goldilocks.WHITE);
        embedBuilder.setThumbnail(portalEmote.getImageUrl());
        String debuffString = "\n\nPlease **select the debuffs your character has** by reacting with the following emotes » ";
        String raidDescription = "";
        raidDescription += "**Click on** the channel titled **`" + member.getEffectiveName() + "'s " + (dungeonName.isEmpty() ? "Custom" : dungeonName) + "`** to join this raid." +
                "\nAfter you do so please **react with " + portalEmote.getAsMention() + "** so you do not get moved out." +
                "\n\nIf you have a " + (dungeonName.isEmpty() ? "raid" : dungeonName) + " key, please react with " + keyEmote.getAsMention() + " and check your dms." +
                "\n\nPlease **indicate your class and gear choices** by reacting with the following emotes » ";

        for (Emote emote : defaultEmotes) {
            if (!additionalEmotes.contains(emote) && !earlyLocationEmotes.contains(emote)) {
                raidDescription += emote.getAsMention();
            }
        }

        for (Emote emote : debuffEmotes) {
            if (!additionalEmotes.isEmpty()) {
                if (additionalEmotes.contains(emote)) {
                    debuffString += emote.getAsMention();
                }
            } else {
                debuffString += emote.getAsMention();
            }
        }

        for (Emote emote : additionalEmotes) {
            if (!debuffString.contains(emote.getAsMention()) && !raidLeaderEmotes.contains(emote)) {
                raidDescription += emote.getAsMention();
            }
        }

        for (Emote emote : raidLeaderEmotes) {
            if (!debuffString.contains(emote.getAsMention()) && !earlyLocationEmotes.contains(emote)) {
                raidDescription += emote.getAsMention();
            }
        }

        raidDescription += debuffString + "\n";
        String emoteString = "";
        for (Emote emote : earlyLocationEmotes) {
            switch (emote.getId()) {
                case "768782625783283743":
                    raidDescription += "\nIf you intend to rush, please react with " + emote.getAsMention() + " and check your dms.";
                    break;
                default:
                    if (!emote.getName().toLowerCase().contains("key")) {
                        emoteString += emote.getAsMention();
                    }
                    break;
            }

        }
        if (!emoteString.isEmpty()) {
            raidDescription += "\n\nIf you are bringing any of the following " + emoteString + " and have 85+ mheal, please react with the corresponding emote and check your dms.\n";
        }
        raidDescription += "\nIf you are boosting this server, react with " + nitro.getAsMention() + " to receive early location.";
        embedBuilder.setDescription(raidDescription);
        embedBuilder.setFooter("This raid will begin automatically or when the raid leader is ready");
        embedBuilder.setTimestamp(new Date().toInstant());

        return  embedBuilder;
    }

    public Emote getEmote(String emoteId) {
        return Goldilocks.jda.getEmoteById(emoteId);
    }

}
