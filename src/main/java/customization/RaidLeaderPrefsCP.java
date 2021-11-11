package customization;

import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import utils.Utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static main.Goldilocks.eventWaiter;

public class RaidLeaderPrefsCP {

    Member member;
    TextChannel textChannel;
    Message controlPanel = null;
    boolean keyCpPref;

    public RaidLeaderPrefsCP(Member member, TextChannel textChannel) {

        this.member = member;
        this.textChannel = textChannel;

    }

    public void createControlPanel() {

        keyCpPref = RaidLeaderPrefsConnector.getRlKeyCp(member);

        if (controlPanel == null) {
            controlPanel = textChannel.sendMessage(controlPanelEmbed().build()).complete();
        } else {
            controlPanel.editMessage(controlPanelEmbed().build()).queue();
        }

        try {
            controlPanel.addReaction("🎨").queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            controlPanel.addReaction("1️⃣").queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            controlPanel.addReaction("2️⃣").queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            controlPanel.addReaction("3️⃣").queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            controlPanel.addReaction("💠").queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            controlPanel.addReaction("🔄").queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            controlPanel.addReaction("❌").queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
        } catch (Exception e) {
        }
        //1️⃣2️⃣3️⃣
        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(controlPanel.getId()) && e.getMember().equals(member) && e.getReactionEmote().isEmoji() && ("🎨1️⃣2️⃣3️⃣💠🔄❌").contains(e.getReactionEmote().getEmoji());
        }, e -> {

            if (e.getReactionEmote().getEmoji().equals("❌")) {
                controlPanel.delete().queue();
                return;
            }

            String emote = e.getReactionEmote().getEmoji();

            if (emote.equals("🎨")) {
                promptForColor();
            }
            if (emote.equals("1️⃣")) {
                promptForEmote("emoteOneId");
            }
            if (emote.equals("2️⃣")) {
                promptForEmote("emoteTwoId");
            }
            if (emote.equals("3️⃣")) {
                promptForEmote("emoteThreeId");
            }
            if (emote.equals("🔄")) {
                RaidLeaderPrefsConnector.resetPreferences(member.getGuild().getId(), member.getId());
                e.getReaction().removeReaction().queue();
                createControlPanel();
            }

            if (("💠").contains(emote)) {
                controlPanel.removeReaction(emote, member.getUser()).queue();
                RaidLeaderPrefsConnector.toggleKeyControlPanel(member);
                createControlPanel();
            }

        }, 2L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });

    }

    private void promptForColor() {
        controlPanel.clearReactions().queue();
        List<Emote> colorEmotes = Goldilocks.jda.getGuildById("771984493484965888").getEmotes();
        Emote[] orderedColorEmotes = new Emote[36];
        int i = 1;
        String optionString = "";
        for (Emote emote : colorEmotes) {
            orderedColorEmotes[Integer.parseInt(emote.getName().replaceAll("[^0-9]", "")) - 1] = emote;
        }
        for (int j = 1; j <= 36; j++) {
            optionString += "**`" + String.format("%-2s", j) + ".`**" + orderedColorEmotes[j - 1].getAsMention() + " ";
            if (i % 6 == 0) {
                optionString += "\n";
            }
            i++;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setTitle("Color Selection for " + member.getEffectiveName());
        embedBuilder.setDescription("Here the following color options \nPlease enter the corresponding number to the color you want\n\n**__Options:__**\n" + optionString);
        embedBuilder.setFooter("To exit this menu type close at any time");
        embedBuilder.setTimestamp(new Date().toInstant());

        controlPanel.editMessage(embedBuilder.build()).queue();

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(member.getUser()) && (e.getMessage().getContentRaw().equalsIgnoreCase("close") || Utils.isNumeric(e.getMessage().getContentRaw()));
        }, e -> {
            Emote selectedEmote;
            if (e.getMessage().getContentRaw().equalsIgnoreCase("close")) {
                controlPanel.delete().queue();
                e.getMessage().delete().queue();
                return;
            }

            int choice = Integer.parseInt(e.getMessage().getContentRaw()) - 1;
            if (choice >= 0 && choice < orderedColorEmotes.length) {
                e.getMessage().delete().queue();
                selectedEmote = orderedColorEmotes[choice];
                //Todo add emote setter
                getColor(selectedEmote);
                createControlPanel();
            } else {
                e.getMessage().delete().queue();
                Utils.errorMessage("Could Not Select Emote", "The choice indicated was not a valid option", textChannel, 10L);
                promptForColor();
            }

        }, 2L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });

    }

    private void promptForEmote(String emoteId) {
        controlPanel.clearReactions().queue();
        controlPanel.editMessage(emoteEmbed().build()).queue();

        try {
            eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
                return e.getAuthor().equals(member.getUser());
            }, e -> {
                if (e.getMessage().getContentRaw().toLowerCase().equals("close")) {
                    controlPanel.delete().queue();
                    e.getMessage().delete().queue();
                    return;
                }

                findEmote(e.getMessage().getContentRaw().toLowerCase(), emoteId);
                e.getMessage().delete().queue();

            }, 2L, TimeUnit.MINUTES, () -> {
                controlPanel.delete().queue();
            });
        } catch (Exception e) {}
    }

    private void findEmote(String emoteName, String emoteId) {

        List<Emote> earlyLocEmotes = Goldilocks.jda.getGuildById("794814328082530364").getEmotes();

        HashMap<String, Emote> emoteNameMap = new HashMap<>();
        Goldilocks.jda.getEmotes().stream().filter(emote -> !emote.getGuild().getId().equals("767819019960188980")
                && !emote.getGuild().getId().equals("722999001460244491") && !emote.getGuild().getId().equals("733482900841824318")
                && !emote.getGuild().getId().equals("514788290809954305") && !emote.getGuild().getId().equals("762883845925109781")
                && !emote.getName().toLowerCase().contains("key"))
                .collect(Collectors.toList()).forEach(emote -> {
            emoteNameMap.put(emote.getName() + (earlyLocEmotes.contains(emote) ? "** | Early Location**" : ""), emote);
        });

        List<Emote> possibleEmotes = new ArrayList<>();
        final String[] emoteString = {"\n**__Options: __**\n"};
        final int[] i = {1};
        emoteNameMap.forEach((s, emote) -> {
            if (s.toLowerCase().contains(emoteName)) {
                if (possibleEmotes.size() <= 20) {
                    possibleEmotes.add(emote);
                    final String[] displayName = {""};
                    s = s.replace("of", "Of").replace("the", "The");
                    Arrays.stream(s.split("(?=\\p{Upper})")).forEach(s1 -> displayName[0] += s1 + " ");
                    emoteString[0] += "**" + i[0] + ".** " + emote.getAsMention() + "⇒ " + displayName[0] + "\n";
                    i[0]++;
                }
            }
        });

        EmbedBuilder embedBuilder = emoteEmbed();
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("Here are the following emotes found for your entry of `" + emoteName + "`\nPlease enter the number your corresponding emote is linked to." +
                "\n\nIf *none of these emotes match the one you wanted feel free to **type a new name*** in chat\n" + (possibleEmotes.size() == 0 ?  "\n**__Options:__**\nNo emotes match `" + emoteName + "`" : emoteString[0]));
        embedBuilder.setFooter("To exit this menu type close at any time");

        controlPanel.editMessage(embedBuilder.build()).queue();

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(member.getUser());
        }, e -> {
            Emote selectedEmote;
            if (e.getMessage().getContentRaw().equalsIgnoreCase("close")) {
                controlPanel.delete().queue();
                e.getMessage().delete().queue();
                return;
            }

            if (!Utils.isNumeric(e.getMessage().getContentRaw())) {
                findEmote(e.getMessage().getContentRaw().toLowerCase(), emoteId);
                e.getMessage().delete().queue();
                return;
            }

            int choice = Integer.parseInt(e.getMessage().getContentRaw());
            if (choice > 0 && choice <= possibleEmotes.size()) {
                e.getMessage().delete().queue();
                selectedEmote = possibleEmotes.get(choice - 1);
                RaidLeaderPrefsConnector.setRlEmote(member.getGuild().getId(), member.getId(), selectedEmote.getId(), emoteId);
                createControlPanel();
            } else {
                e.getMessage().delete().queue();
                Utils.errorMessage("Could Not Select Emote", "The choice indicated was not a valid option", textChannel, 10L);
                promptForEmote(emoteId);
            }

        }, 2L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
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

    private EmbedBuilder controlPanelEmbed() {
        List<Emote> rlEmotes = RaidLeaderPrefsConnector.getRlEmotes(member.getId(), member.getGuild().getId());
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Raid Preferences for " + member.getEffectiveName());
        embedBuilder.setThumbnail(member.getUser().getAvatarUrl());

        Color rlColor = RaidLeaderPrefsConnector.getRlColor(member.getId(), member.getGuild().getId());

        if (rlColor != null) {
            embedBuilder.setColor(rlColor);
            embedBuilder.addField("Raid Color", "⟸ You current color is displayed to the left, if you'd like to change it react with 🎨", false);
        } else {
            embedBuilder.addField("Raid Color", "You currently have no color set, to set one please react with 🎨", false);
        }

        String emoteString = "\n";
        int i = 1;
        for (Emote emote : rlEmotes) {
            if (i == 1) emoteString += "1️⃣⤇";
            if (i == 2) emoteString += "\n2️⃣⤇";
            if (i == 3) emoteString += "\n3️⃣⤇";
            emoteString += "**Emote #" + i + ":** " + emote.getAsMention();
            i++;
        }
        for (i = i; i <= 3; i++) {
            if (i == 1) emoteString += "1️⃣⤇";
            if (i == 2) emoteString += "\n2️⃣⤇";
            if (i == 3) emoteString += "\n3️⃣⤇";
            emoteString += "**Emote #" + i + ":** `None Set` ";
        }
        embedBuilder.setDescription("This is your afk check preferences control panel, to change any of your preferences make sure to react with the emoji next to them. Once you react you will be prompted with a selection screen." +
                " All of the options set here will appear on all your AFK checks.");

        embedBuilder.addField("Custom Emotes", "Here are your custom raid emotes, these emotes appear on all of your AFK checks!" + emoteString, false);
        embedBuilder.addField("Key Control Panel", "💠 Your Key Control Panels are currently set to be: " + (keyCpPref ? "**MAXIMIZED**" : "**MINIMIZED**"), false);
        embedBuilder.addField("Reset to Default", "If you would like to reset all your settings to the default setting please react with 🔄", false);
        embedBuilder.setTimestamp(new Date().toInstant());
        embedBuilder.setFooter("To exit this menu react with ❌ at any time");

        return embedBuilder;
    }

    private Color getColor (Emote emote) {
        Color color;

        BufferedImage image = null;
        try {
            image = ImageIO.read(new File ("colors/" + emote.getName() + ".png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        assert image != null;
        color = new Color(image.getRGB(image.getWidth() / 2, image.getHeight() / 2));
        RaidLeaderPrefsConnector.setRlColor(member.getId(), member.getGuild().getId(), color);
        return color;
    }

}
