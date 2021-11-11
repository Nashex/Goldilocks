package setup;

import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.requests.ErrorResponse;
import raids.Dungeon;
import raids.DungeonInfo;
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

public class DungeonSetup {

    Member member;
    TextChannel textChannel;
    Guild guild;
    List<Dungeon> dungeonInfo;

    Message controlPanel;
    Dungeon curDungeon = null;

    private final ActionRow mainActionRow = ActionRow.of(
            Button.success("addungeon", "Add a Dungeon"),
            Button.primary("editdungeon", "Edit a Dungeon"),
            Button.danger("removedungeon", "Remove a Dungeon"),
            Button.danger("maincancel", "Exit"));

    public DungeonSetup(Member member, TextChannel textChannel) {
        this.member = member;
        this.textChannel = textChannel;
        this.guild = member.getGuild();

        dungeonInfo = DungeonInfo.nDungeonInfo(guild);
        controlPanel = textChannel.sendMessage(generalDungeonEmbed().build())
                .setActionRows(mainActionRow).complete();
        mainListener();
    }

    public void mainListener() {
        Goldilocks.eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return Objects.equals(member.getUser(), e.getUser()) && Objects.equals(controlPanel, e.getMessage())
                    && Arrays.asList("addungeon", "editdungeon", "removedungeon", "maincancel").contains(e.getComponentId());
        }, e -> {

            String control = e.getComponentId();
            e.deferEdit().queue();

            if (control.equals("maincancel")) {
                controlPanel.delete().queue();
                return;
            }

            if (control.equals("addungeon")) editDungeon(-1);


            if (control.equals("editdungeon")) selectDungeon(0);

            if (control.equals("removedungeon")) selectDungeon(1);

        }, 5L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    public void selectDungeon(int type) {
        controlPanel.editMessage(generalDungeonEmbed().addField("Select a Dungeon", "Please select the corresponding dungeon you would like to " + (type == 0 ? "edit" : "delete") + " by entering" +
                " the corresponding number in.", false).build()).setActionRows().queue();

        eventWaiter.waitForEvent(MessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(member.getUser()) && (!e.getMessage().getContentRaw().replaceAll("[^0-9]", "").isEmpty()
                || e.getMessage().getContentRaw().toLowerCase().equals("close"));
        }, e -> {

            if (e.getMessage().getContentRaw().toLowerCase().equals("close")) {
                controlPanel.editMessage(generalDungeonEmbed().build()).setActionRows(mainActionRow).queue();
                mainListener();
                e.getMessage().delete().queue();
                return;
            }

            int choice = Integer.parseInt(e.getMessage().getContentRaw().replaceAll("[^0-9]", "")) - 1;
            e.getMessage().delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));

            if (!(choice <= dungeonInfo.size() - 1)) {
                selectDungeon(type);
            } else {
                curDungeon = dungeonInfo.get(choice);
                if (type == 0) editDungeon(choice);
                if (type == 1) deleteDungeon();
            }


        }, 3L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    public EmbedBuilder generalDungeonEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Dungeon Editor for " + guild.getName());
        embedBuilder.setColor(Goldilocks.WHITE);

        String[] dungeonTypes = {"legendary", "court", "epic", "highlands", "random", "randomLobby"};
        String[] dungeonVisNames = {"Legendary Dungeons", "Court Dungeons and Aliens", "Epic Dungeons", "Highlands Dungeons", "Random Dungeons", "Random Lobbies"};

        for (int i = 0; i < dungeonTypes.length; i++) {
            int finalI = i;
            embedBuilder.addField(dungeonVisNames[i], dungeonInfo.stream()
                    .filter(n -> n.dungeonCategory.equals(dungeonTypes[finalI]))
                    .map(n ->  Goldilocks.jda.getEmoteById(n.dungeonInfo[0]).getAsMention() + " **|`" + String.format("%-2d", dungeonInfo.indexOf(n) + 1) + "`|** " + " " + n.dungeonName)
                    .collect(Collectors.joining("\n")), true);
        }

        return embedBuilder;
    }

    public void deleteDungeon() {
        DungeonInfo.removeDungeon(guild, curDungeon);
        curDungeon = null;
        dungeonInfo = DungeonInfo.nDungeonInfo(guild);
        controlPanel.editMessage(generalDungeonEmbed().build()).setActionRows(mainActionRow).queue();
        mainListener();
    }

    public void editDungeon(int index) {
        if (curDungeon == null) curDungeon = new Dungeon(index == -1 ? dungeonInfo.size() : index);
        controlPanel.editMessage(dungeonEmbed(curDungeon).build())
                .setActionRows(
                        ActionRow.of(
                                Button.secondary("name", "Dungeon Name"),
                                Button.secondary("portal", "Portal Emote").withEmoji(Emoji.fromEmote(Goldilocks.jda.getEmoteById(curDungeon.dungeonInfo[0]))),
                                Button.secondary("key", "Key Emote").withEmoji(Emoji.fromEmote(Goldilocks.jda.getEmoteById(curDungeon.dungeonInfo[1]))),
                                Button.secondary("color", "AFK Color")

                        ),
                        ActionRow.of(
                                Button.secondary("cap", "Default Cap"),
                                Button.secondary("location", "Early Loc Emotes"),
                                Button.secondary("additional", "Additional Emotes"),
                                Button.secondary("category", "Category")
                        ),
                        ActionRow.of(
                                Button.success("save", "Save"),
                                Button.danger("cancel", "Cancel")
                        )
                ).queue();
        editDungeonListener();
    }

    private void editDungeonListener() {
        List<String> controls = Arrays.asList("name", "portal", "key", "cap", "location", "additional", "color", "category", "save", "cancel");
        Goldilocks.eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return Objects.equals(member.getUser(), e.getUser()) && Objects.equals(controlPanel, e.getMessage())
                    && controls.contains(e.getComponentId());
        }, e -> {

            String control = e.getComponentId();
            e.deferEdit().queue();

            if (control.equals("cancel")) {
                controlPanel.editMessage(generalDungeonEmbed().build()).setActionRows(mainActionRow).queue();
                mainListener();
                curDungeon = null;
                return;
            }

            if (control.equals("name")) editName();
            if (control.equals("portal")) editEmotes(3);
            if (control.equals("key")) editEmotes(2);
            if (control.equals("cap")) editCap();

            if (control.equals("location")) editEmotes(0);
            if (control.equals("additional")) editEmotes(1);

            if (control.equals("color")) editColor();
            if (control.equals("category")) editCategory();

            if (control.equals("save")) {
                DungeonInfo.updateDungeon(guild, curDungeon);
                curDungeon = null;
                dungeonInfo = DungeonInfo.nDungeonInfo(guild);
                controlPanel.editMessage(generalDungeonEmbed().build()).setActionRows(mainActionRow).queue();
                mainListener();
            }


        }, 5L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    public void editName() {
        controlPanel.editMessage(
                new EmbedBuilder().setColor(Goldilocks.WHITE)
                        .setTitle("Currently Editing: Dungeon Name")
                        .setDescription("What would you like to the name of this dungeon to be?")
                .build()
        ).setActionRows().queue();

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(member.getUser());
        }, e -> {
            if (e.getMessage().getContentRaw().toLowerCase().equals("cancel")) {
                e.getMessage().delete().queue();
                editDungeon(curDungeon.dungeonIndex);
                return;
            }

            curDungeon.dungeonName = e.getMessage().getContentRaw();
            curDungeon.dungeonInfo[3] = e.getMessage().getContentRaw();
            e.getMessage().delete().queue();
            editDungeon(curDungeon.dungeonIndex);
        }, 2L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    public void editCategory() {
        controlPanel.editMessage(
                new EmbedBuilder().setColor(Goldilocks.WHITE)
                        .setTitle("Currently Editing: Dungeon Category")
                        .setDescription("What category would you like this dungeon to be in?")
                        .build()
        ).setActionRows(
                ActionRow.of(
                        Button.secondary("legendary", "Legendary"),
                        Button.secondary("court", "Court"),
                        Button.secondary("epic", "Epic"),
                        Button.secondary("highlands", "Highlands")
                ),
                ActionRow.of(
                        Button.secondary("random", "Random"),
                        Button.secondary("randomLobby", "Random Lobby"),
                        Button.danger("cancel", "Cancel")
                )
        ).queue();

        Goldilocks.eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return Objects.equals(member.getUser(), e.getUser()) && Objects.equals(controlPanel, e.getMessage());
        }, e -> {

            String control = e.getComponentId();
            e.deferEdit().queue();

            if (!control.equals("cancel")) curDungeon.dungeonCategory = control;
            editDungeon(curDungeon.dungeonIndex);

        }, 5L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    public void editCap() {
        controlPanel.editMessage(
                new EmbedBuilder().setColor(Goldilocks.WHITE)
                        .setTitle("Currently Editing: Dungeon Voice Channel Cap")
                        .setDescription("What would you like to the cap of this dungeon to be? Please enter a number between 1 and 99.")
                        .build()
        ).setActionRows().queue();

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(member.getUser()) && !e.getMessage().getContentRaw().replaceAll("[^0-9]", "").isEmpty();
        }, e -> {
            curDungeon.dungeonInfo[6] = e.getMessage().getContentRaw().replaceAll("[^0-9]", "");
            e.getMessage().delete().queue();
            editDungeon(curDungeon.dungeonIndex);
        }, 2L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    private void editColor() {
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
        embedBuilder.setTitle("Currently Editing: Dungeon Color");
        embedBuilder.setDescription("Here the following color options \nPlease enter the corresponding number to the color you want\n\n**__Options:__**\n" + optionString);
        embedBuilder.setFooter("To exit this menu type close at any time");
        embedBuilder.setTimestamp(new Date().toInstant());

        controlPanel.editMessage(embedBuilder.build()).setActionRows().queue();

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
                getColor(selectedEmote);
                editDungeon(curDungeon.dungeonIndex);
            } else {
                e.getMessage().delete().queue();
                Utils.errorMessage("Could Not Select Emote", "The choice indicated was not a valid option", textChannel, 10L);
                editColor();
            }

        }, 2L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });

    }

    private Color getColor (Emote emote) {
        Color color;

        BufferedImage image = null;
        try {
            image = ImageIO.read(new File("colors/" + emote.getName() + ".png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        assert image != null;
        color = new Color(image.getRGB(image.getWidth() / 2, image.getHeight() / 2));
        curDungeon.dungeonInfo[5] = color.getRGB() + "";
        return color;
    }

    /*
     DUNGEON ARRAY INFORMATION
         [0] portal emote
         [1] key emote
         [2] early loc emotes
         [3] dungeon name
         [4] additional emotes
         [5] embed color
         [6] dungeon vc cap
         [7] starting image url
         [8] on going image url
     */

    public EmbedBuilder dungeonEmbed(Dungeon dungeon) {
        Color dungeonColor = new Color(Integer.parseInt(dungeon.dungeonInfo[5]));
        String hex = String.format("#%02X%02X%02X", dungeonColor.getRed(), dungeonColor.getGreen(), dungeonColor.getBlue());

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Currently Editing: " + dungeon.dungeonName)
                .setColor(dungeonColor)
                .setDescription("Please use the buttons to edit the corresponding attributes of the dungeon. " +
                        "If you would like your changes to not be saved, please click cancel, otherwise click save.");

        embedBuilder.setThumbnail(dungeon.dungeonInfo[7].isEmpty() ? Goldilocks.jda.getEmoteById(curDungeon.dungeonInfo[0]).getImageUrl() : dungeon.dungeonInfo[7]);

        embedBuilder.addField("Dungeon Name", "`" + dungeon.dungeonName + "`", true)
                .addField("Portal Emote", Goldilocks.jda.getEmoteById(curDungeon.dungeonInfo[0]).getAsMention() , true)
                .addField("Key Emote", Goldilocks.jda.getEmoteById(curDungeon.dungeonInfo[1]).getAsMention() , true)
                .addField("AFK Check Color", "⇐ Hex Code: `" + hex + "`", true)
                .addField("Default VC Cap", "`" + dungeon.dungeonInfo[6] + "`", true)
                .addField("Dungeon Category", "`" + dungeon.dungeonCategory + "`", true);

        embedBuilder.addField("Early Location Emotes", "The following emotes are this dungeon's early location emotes." +
                " The number enclosed to the right of each emote is their current limit.\n\n" +
                Arrays.stream(dungeon.dungeonInfo[2].split(" ")).filter(s -> !s.isEmpty()).map(s -> {
                    String[] emoteInfo = s.split(",");
                    return getEmote(emoteInfo[0]).getAsMention() + " [`" + (emoteInfo.length > 1 ? emoteInfo[1] : 5) + "`]";
                }).collect(Collectors.joining(" **|** "))
                , false);

        embedBuilder.addField("Additional Emotes", "The following emotes are this dungeon's additional emotes. Melee reacts are added to all AFK checks by default." +
                        " If no emotes are added, the debuff emotes will be used as additional emotes.\n\n" +
                Arrays.stream(dungeon.dungeonInfo[4].split(" ")).filter(s -> !s.isEmpty()).map(s -> getEmote(s).getAsMention())
                .collect(Collectors.joining(" **|** "))
                , false);

        return embedBuilder;
    }

    private void editEmotes(int type) {
        if (type == 0) {
            controlPanel.editMessage(earlyLocEmotesEmbed().build())
                    .setActionRows(
                            editEmoteActionRows(getCurDungeonEarlyLocEmotes())
                    ).queue();
            editEmotesListener(type);
        } else if (type == 1) {
            controlPanel.editMessage(additionalEmotesEmbed().build())
                    .setActionRows(
                            editEmoteActionRows(getCurDungeonAdditionalEmotes())
                    ).queue();
            editEmotesListener(type);
        } else {
            promptForEmote(type);
        }

    }

    private void editEmotesListener(int type) {
        List<String> controls = Arrays.asList("add", "back");
        Goldilocks.eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return Objects.equals(member.getUser(), e.getUser()) && Objects.equals(controlPanel, e.getMessage())
                    && (e.getComponentId().contains("emote") || controls.contains(e.getComponentId()));
        }, e -> {

            String control = e.getComponentId();
            e.deferEdit().queue();

            if (control.equals("back")) {
                editDungeon(curDungeon.dungeonIndex);
                return;
            }

            if (control.equals("add")) {
                promptForEmote(type);
            }

            if (control.contains("emote")) {
                int index = Integer.parseInt(control.replaceAll("[^0-9]", ""));
                if (type == 0) editEarlyLocEmote(index); // TODO Additional emotes
                if (type == 1) {
                    String emoteId = curDungeon.dungeonInfo[4].split(" ")[Integer.parseInt(control.replaceAll("[^0-9]", ""))];
                    curDungeon.dungeonInfo[4] = curDungeon.dungeonInfo[4].replace(emoteId, "").replace("  ", " ").trim();
                    editEmotes(type);
                }
            }

        }, 5L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    private void promptForEmote(int type) {
        controlPanel.editMessage(emoteEmbed().build()).setActionRows().queue();

        try {
            eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
                return e.getAuthor().equals(member.getUser());
            }, e -> {
                if (e.getMessage().getContentRaw().toLowerCase().equals("close")) {
                    editEmotes(type);
                    e.getMessage().delete().queue();
                    return;
                }

                findEmote(e.getMessage().getContentRaw().toLowerCase(), type);
                e.getMessage().delete().queue();

            }, 2L, TimeUnit.MINUTES, () -> {
                controlPanel.delete().queue();
            });
        } catch (Exception e) {}
    }

    private void findEmote(String emoteName, int type) {
        HashMap<String, Emote> emoteNameMap = new HashMap<>();
        Goldilocks.jda.getEmotes().forEach(emote -> {
            emoteNameMap.put(emote.getName(), emote);
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
                editEmotes(type);
                e.getMessage().delete().queue();
                return;
            }

            if (!Utils.isNumeric(e.getMessage().getContentRaw())) {
                findEmote(e.getMessage().getContentRaw().toLowerCase(), type);
                e.getMessage().delete().queue();
                return;
            }

            int choice = Integer.parseInt(e.getMessage().getContentRaw());
            if (choice > 0 && choice <= possibleEmotes.size()) {
                e.getMessage().delete().queue();
                selectedEmote = possibleEmotes.get(choice - 1);
                if (type < 2) {
                    curDungeon.dungeonInfo[type == 0 ? 2 : 4] = (curDungeon.dungeonInfo[type == 0 ? 2 : 4] + " " + selectedEmote.getId() + (type == 0 ? ",5" : "")).trim();
                    editEmotes(type);
                } else {
                    curDungeon.dungeonInfo[type == 2 ? 1 : 0] = selectedEmote.getId();
                    editDungeon(curDungeon.dungeonIndex);
                }
            } else {
                e.getMessage().delete().queue();
                Utils.errorMessage("Could Not Select Emote", "The choice indicated was not a valid option", textChannel, 10L);
                promptForEmote(type);
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

    private EmbedBuilder additionalEmotesEmbed() {
        return new EmbedBuilder()
                .setColor(Goldilocks.LIGHTBLUE)
                .setTitle("Currently Editing: Early Location Emotes")
                .setDescription("These are the current additional emotes for `" + curDungeon.dungeonName + "`. If you would like an emote removed, press the corresponding button."
                );
    }

    private EmbedBuilder earlyLocEmotesEmbed() {
        return new EmbedBuilder()
                .setColor(Goldilocks.GREEN)
                .setTitle("Currently Editing: Early Location Emotes")
                .setDescription("These are the current early location emotes for `" + curDungeon.dungeonName + "`. Press the add emote button to add an early location emote to the AFK, " +
                        "press on the emote buttons to edit them."
                );
    }

    private List<ActionRow> editEmoteActionRows(List<Emote> emotes) {
        List<ActionRow> actionRows = new ArrayList<>();
        List<Button> curRow = new ArrayList<>();

        int index = 0;
        for (Emote emote : emotes) {
            Button button = Button.of(ButtonStyle.SECONDARY, "emote" + index, Emoji.fromEmote(emote));
            if (curRow.size() < 5) curRow.add(button);
            else {
                actionRows.add(ActionRow.of(curRow));
                curRow = new ArrayList<>(Collections.singletonList(button));
            }
            index++;
        }
        if (curRow.size() < 4) {
            curRow.add(Button.success("add", "Add Emote"));
            curRow.add(Button.danger("back", "Go Back"));
            actionRows.add(ActionRow.of(curRow));
        }
        else {
            actionRows.add(ActionRow.of(curRow));
            actionRows.add(ActionRow.of(Button.success("add", "Add Emote"), Button.danger("back", "Go Back")));
        }
        return actionRows;
    }

    private void editEarlyLocEmote(int index) {
        String[] earlyLocEmotes = curDungeon.dungeonInfo[2].split(" ");
        String[] emoteInfo = earlyLocEmotes[index].split(",");
        controlPanel.editMessage(earlyEmoteEditEmbed(getEmote(emoteInfo[0]), (emoteInfo.length > 1 ? Integer.parseInt(emoteInfo[1]) : 5)).build())
                .setActionRow(
                        Button.secondary("max", "Change Max"),
                    Button.danger("remove", "Remove Emote"),
                    Button.danger("cancel", "Go Back")
                ).queue();
        editEarlyLocListener(index);
    }

    public void editEarlyLocListener(int index) {
        List<String> controls = Arrays.asList("remove", "max", "cancel");
        Goldilocks.eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return Objects.equals(member.getUser(), e.getUser()) && Objects.equals(controlPanel, e.getMessage())
                    && controls.contains(e.getComponentId());
        }, e -> {

            String control = e.getComponentId();
            e.deferEdit().queue();

            String[] earlyLocEmotes = curDungeon.dungeonInfo[2].split(" ");
            String[] emoteInfo = earlyLocEmotes[index].split(",");

            if (control.equals("max")) {
                editEmoteMax(getEmote(emoteInfo[0]), "" + (emoteInfo.length > 1 ? emoteInfo[1] : 5), index);
                return;
            }

            if (control.equals("remove")) {
                curDungeon.dungeonInfo[2] = curDungeon.dungeonInfo[2].replace(emoteInfo[0] + (emoteInfo.length > 1 ? "," + emoteInfo[1] : ""), "").replace("  ", " ").trim();
            }

            if (control.equals("cancel")) {
                controlPanel.editMessage(earlyLocEmotesEmbed().build()).setActionRows(editEmoteActionRows(getCurDungeonEarlyLocEmotes())).queue();
            }

            controlPanel.editMessage(earlyLocEmotesEmbed().build()).setActionRows(editEmoteActionRows(getCurDungeonEarlyLocEmotes())).queue();
            editEmotesListener(0);

        }, 5L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    public void editEmoteMax(Emote emote, String limit, int index) {
        controlPanel.editMessage(new EmbedBuilder().setTitle("Currently Editing: Emote Max Reacts")
                .setColor(Goldilocks.LIGHTBLUE)
                .setDescription("Please select the max number of reacts you would like to allow for " + emote.getAsMention() + ".").build())
                .setActionRows(
                        ActionRow.of(
                                Button.secondary("1", "One"),
                                Button.secondary("2", "Two"),
                                Button.secondary("3", "Three"),
                                Button.secondary("4", "Four"),
                                Button.secondary("5", "Five")
                        ),
                        ActionRow.of(
                                Button.secondary("6", "Six"),
                                Button.secondary("7", "Seven"),
                                Button.secondary("8", "Eight"),
                                Button.secondary("9", "Nine"),
                                Button.secondary("10", "Ten")
                        ),
                        ActionRow.of(
                                Button.danger("cancel", "Cancel")
                        )
                ).queue();

        List<String> controls = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "cancel");
        Goldilocks.eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return Objects.equals(member.getUser(), e.getUser()) && Objects.equals(controlPanel, e.getMessage())
                    && controls.contains(e.getComponentId());
        }, e -> {

            String control = e.getComponentId();
            e.deferEdit().queue();

            if (!control.equals("cancel")) {
                curDungeon.dungeonInfo[2] = curDungeon.dungeonInfo[2].replace(emote.getId(), emote.getId() + "," + control)
                        .replace("," + control + "," + limit, "," + control); // Override additional cap
            }

            editEarlyLocEmote(index);

        }, 5L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    private EmbedBuilder earlyEmoteEditEmbed(Emote emote, int limit) {
        return new EmbedBuilder()
                .setColor(Goldilocks.LIGHTBLUE)
                .setTitle("Currently Editing: " + emote.getName())
                .setDescription("**Emote: **" + emote.getAsMention()
                + "\n**Emote Limit: ** `" + limit + "`\n\n" +
                        "Please note that the emote limit is the maximum amount of users that can receive location for this emote.");

    }

    private Emote getEmote(String id) {
        return Goldilocks.jda.getEmoteById(id);
    }

    private List<Emote> getCurDungeonEarlyLocEmotes() {
        return Arrays.stream(curDungeon.dungeonInfo[2].split(" ")).filter(s -> !s.isEmpty()).map(s -> getEmote(s.split(",")[0])).collect(Collectors.toList());
    }

    private List<Emote> getCurDungeonAdditionalEmotes() {
        return Arrays.stream(curDungeon.dungeonInfo[4].split(" ")).filter(s -> !s.isEmpty()).map(s -> getEmote(s.split(",")[0])).collect(Collectors.toList());
    }

}
