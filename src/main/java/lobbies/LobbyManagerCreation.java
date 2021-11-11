package lobbies;

import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import utils.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static main.Goldilocks.eventWaiter;


public class LobbyManagerCreation {
    public static ScheduledExecutorService TIMER = new ScheduledThreadPoolExecutor(1);

    public static void lobbyManagerCreation(Message msg) {
        TextChannel textChannel = msg.getTextChannel();
        Message controlPanel = textChannel.sendMessage(dungeonSelectionScreen().build()).complete();

        String generalChannel = "";
        if (msg.getMentionedChannels().isEmpty()) {
            generalChannel = msg.getTextChannel().getId();
        } else {
            generalChannel = msg.getMentionedChannels().get(0).getId();
        }

        String[][] dungeonInfo;
        if (msg.getGuild().getId().equals("762883845925109781")) {
            String oryxChannel = Goldilocks.jda.getTextChannelById("768187081171271700").getId();
            String hallsChannel = Goldilocks.jda.getTextChannelById("767654430203445259").getId();
            String shattersChannel = Goldilocks.jda.getTextChannelById("767822161573314571").getId();
            String nestChannel = Goldilocks.jda.getTextChannelById("767823210346840114").getId();
            String courtChannel = Goldilocks.jda.getTextChannelById("768167597727547453").getId();
            String fungalChannel = Goldilocks.jda.getTextChannelById("768169848592138251").getId();
            String amongUsChannel = Goldilocks.jda.getTextChannelById("768191833703448626").getId();
            dungeonInfo = dungeonInfo(oryxChannel, hallsChannel,shattersChannel,nestChannel, fungalChannel, courtChannel, amongUsChannel);
        } else {
            dungeonInfo = dungeonInfo(generalChannel,generalChannel,generalChannel, generalChannel, generalChannel, generalChannel, generalChannel);
        }

        eventWaiter.waitForEvent(MessageReceivedEvent.class, e -> {return e.getAuthor().equals(msg.getAuthor()) && (Utils.isNumeric(e.getMessage().getContentRaw())
                || e.getMessage().getContentRaw().toLowerCase().equals("close") || e.getMessage().getContentRaw().toLowerCase().equals("all"));}, e -> {

            if (e.getMessage().getContentRaw().toLowerCase().equals("close")) {
                controlPanel.delete().queue();
                e.getMessage().delete().queue();
                return;
            }

            if (e.getMessage().getContentRaw().toLowerCase().equals("all")) {
                for (int i = 0; i < 44; i++) {
                    String dungeonName = dungeonInfo[i][2];
                    Emote joinEmote = Goldilocks.jda.getEmoteById(dungeonInfo[i][0]);
                    List<Emote> createEmote = new ArrayList<>();
                    String[] createEmotes = dungeonInfo[i][1].split(" ");
                    for (String string : createEmotes) {
                        createEmote.add(Goldilocks.jda.getEmoteById(string.trim()));
                    }
                    TextChannel lobbyManagerChannel = Goldilocks.jda.getTextChannelById(dungeonInfo[i][3]);
                    Color color = new Color(Integer.valueOf(dungeonInfo[i][4]));
                    int dungeonLimit = Integer.valueOf(dungeonInfo[i][5]);
                    LobbyManagerHub.createLobbyManager(dungeonName, joinEmote, createEmote, lobbyManagerChannel, color, dungeonLimit);
                }
                controlPanel.editMessage(dungeonSelectionScreen().clearFields().setDescription("```\nA new lobby manager for all lobby types was successfully created by " + msg.getMember().getEffectiveName() + "```")
                        .setTitle("Lobby Manager Creation")
                        .setTimestamp(new Date().toInstant()).build()).queue();
                e.getMessage().delete().queue();
                return;
            }

            int choice = Integer.parseInt(e.getMessage().getContentRaw()) - 1;

            if (!(choice <= dungeonInfo.length - 1)) {
                e.getMessage().delete().queue();
                Utils.errorMessage("Failed to Create Lobby Manager", "Lobby type is invalid, please try again.", controlPanel, 5L);
                return;
            }

            String dungeonName = dungeonInfo[choice][2];
            Emote joinEmote = Goldilocks.jda.getEmoteById(dungeonInfo[choice][0]);
            List<Emote> createEmote = new ArrayList<>();
            String[] createEmotes = dungeonInfo[choice][1].split(" ");
            for (String string : createEmotes) {
                createEmote.add(Goldilocks.jda.getEmoteById(string.trim()));
            }
            TextChannel lobbyManagerChannel = Goldilocks.jda.getTextChannelById(dungeonInfo[choice][3]);
            Color color = new Color(Integer.valueOf(dungeonInfo[choice][4]));
            int dungeonLimit = Integer.valueOf(dungeonInfo[choice][5]);
            LobbyManagerHub.createLobbyManager(dungeonName, joinEmote, createEmote, lobbyManagerChannel, color, dungeonLimit);
            controlPanel.editMessage(dungeonSelectionScreen().clearFields().setDescription("```\nA new lobby manager for " + dungeonName +
                    " was successfully created by " + msg.getMember().getEffectiveName() + "```")
                    .setTitle("Lobby Manager Creation")
                    .setTimestamp(new Date().toInstant()).build()).queue();
            e.getMessage().delete().queue();
        }, 3L, TimeUnit.MINUTES, () -> {
            controlPanel.editMessage(dungeonSelectionScreen().clearFields().setDescription("```\n" +
                    "This Lobby Manager Creation Control Panel has been closed due to inactivity. \n```")
                    .setFooter("This message will delete in 10 seconds.").build());
            controlPanel.delete().submitAfter(10L, TimeUnit.SECONDS);
        });
        msg.delete().queue();

    }

    public static EmbedBuilder dungeonSelectionScreen() {
        int i = 0;

        String[][] dungeonInfo = dungeonInfo("","","", "", "","", "");

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Lobby Manager Selection");
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("Please select a type for your lobby manager by typing the number of the corresponding type below.");

        String legendaryDungeons = "";
        String courtDungeons = "";
        String epicDungeons = "";
        String highlandsDungeons = "";
        String randomDungeons = "";
        String randomLobbyTypes = "";
        for (i = i; i < 7; i++) {
            legendaryDungeons += "\n**" + (i + 1) + ".** " + Goldilocks.jda.getEmoteById(dungeonInfo[i][0]).getAsMention() + " " + dungeonInfo[i][2];
        }
        for (i = i; i < 12; i++) {
            courtDungeons += "\n**" + (i + 1) + ".** " + Goldilocks.jda.getEmoteById(dungeonInfo[i][0]).getAsMention() + " " + dungeonInfo[i][2];
        }
        for (i = i; i < 21; i++) {
            epicDungeons += "\n**" + (i + 1) + ".** " + Goldilocks.jda.getEmoteById(dungeonInfo[i][0]).getAsMention() + " " + dungeonInfo[i][2];
        }
        for (i = i; i < 31; i++) {
            highlandsDungeons += "\n**" + (i + 1) + ".** " + Goldilocks.jda.getEmoteById(dungeonInfo[i][0]).getAsMention() + " " + dungeonInfo[i][2];
        }
        for (i = i; i < 42; i++) {
            randomDungeons += "\n**" + (i + 1) + ".** " + Goldilocks.jda.getEmoteById(dungeonInfo[i][0]).getAsMention() + " " + dungeonInfo[i][2];
        }
        for (i = i; i < 44; i++) {
            randomLobbyTypes += "\n**" + (i + 1) + ".** " + Goldilocks.jda.getEmoteById(dungeonInfo[i][0]).getAsMention() + " " + dungeonInfo[i][2];
        }
        embedBuilder.addField("Legendary Dungeons", legendaryDungeons, true);
        embedBuilder.addField("Court Dungeons", courtDungeons, true);
        //embedBuilder.addBlankField(true);
        embedBuilder.addField("Epic Dungeons", epicDungeons, true);
        embedBuilder.addField("Highlands Dungeons", highlandsDungeons, true);
        embedBuilder.addField("Random Dungeons", randomDungeons, true);
        embedBuilder.addField("Random Lobbies", randomLobbyTypes, true);
        embedBuilder.setFooter("To exit the lobby manager gui type: close");
        return embedBuilder;
    }

    public static String[][] dungeonInfo(String oryxChannel, String hallsChannel, String shattersChannel, String nestChannel, String fungalCavern, String randomChannel, String amongUsChannel) {

        // {portalEmoteId, keyEmoteId, dungeonName, textChannel, color, lobbyLimit}
        String[][] dungeonInfo = {
                //Legendary Dungeons
                //Oryx 3 768186834127290408 768186895930359888 768186846059954227
                {"768186960622649375", "768186911650611300", "Oryx 3", oryxChannel, String.valueOf((new Color(28, 18, 33)).getRGB()), "30"},
                //Void
                {"767811845947654204", "767819092756267068 767898941433708544", "The Void", hallsChannel, String.valueOf((new Color(42, 0, 125)).getRGB()), "15"},
                //Mbc
                {"767811815522435072", "767819092756267068", "Marble Collosus", hallsChannel, String.valueOf((new Color(111, 111, 111)).getRGB()), "15"},
                //Cult
                {"766907072607682560", "767819092756267068", "Cultist Hideout", hallsChannel, String.valueOf((new Color(97, 11, 11)).getRGB()), "15"},
                //Shatters
                {"723001214865899532", "767819092672905256", "The Shatters", shattersChannel, String.valueOf((new Color(9, 59, 9)).getRGB()), "15"},
                //The Nest
                {"723001215407095899", "767819092467253289", "The Nest", nestChannel, String.valueOf((new Color(227, 144, 0)).getRGB()), "15"},
                //Fungal Cavern
                {"723001215696240660", "767819091540836433", "Fungal Cavern", fungalCavern, String.valueOf((new Color(56, 78, 101)).getRGB()), "15"},

                //Court dungeons
                //High Tech Terror
                {"768163469034782721", "768163697830658088", "High Tech Terror", randomChannel, String.valueOf((new Color(58, 160, 0)).getRGB()), "15"},
                //Thicket
                {"767812271334096937", "767819092383498304", "Secluded Thicket", randomChannel, String.valueOf((new Color(129, 255, 79)).getRGB()), "15"},
                //Layer of Shaitan
                {"723001215100911700", "767819092601602118", "Layer of Shaitan", randomChannel, String.valueOf((new Color(255, 103, 20)).getRGB()), "15"},
                //Cnidarian Reef
                {"767811936028196915", "767819091531792414", "Cnidarian Reef", randomChannel, String.valueOf((new Color(255, 215, 113)).getRGB()), "15"},
                //Puppet Master's Encore
                {"723001215419547648", "767819092588494848", "Puppet Master's Encore", randomChannel, String.valueOf((new Color(101, 23, 23)).getRGB()), "15"},

                //Epic dungeons
                //Davy Jones' Locker
                {"767955903001395200", "767819091624722512", "Davy Jones' Locker", randomChannel, String.valueOf((new Color(46, 28, 56)).getRGB()), "15"},
                //Mountain Temple
                {"723001215616548884", "767819092207861772", "Mountain Temple", randomChannel, String.valueOf((new Color(52, 13, 13)).getRGB()), "15"},
                //Deadwater Docks
                {"723001215444844564", "767819091406749727", "Deadwater Docks", randomChannel, String.valueOf((new Color(0, 194, 126)).getRGB()), "15"},
                //Woodland Labyrinth
                {"723001214974951475", "767819092673298462", "Woodland Labyrinth", randomChannel, String.valueOf((new Color(33, 94, 0)).getRGB()), "15"},
                //The Crawling Depths
                {"723001215436193792", "767819095479025726", "The Crawling Depths", randomChannel, String.valueOf((new Color(104, 45, 161)).getRGB()), "15"},
                //Ocean Trench
                {"723001215226478633", "767819092593737768", "Ocean Trench", randomChannel, String.valueOf((new Color(249, 35, 255)).getRGB()), "15"},
                //Ice Cave
                {"723001215709085717", "767819091611222026", "Ice Cave", randomChannel, String.valueOf((new Color(94, 239, 255)).getRGB()), "15"},
                //Tomb of the Ancients
                {"767812113187471401", "767819092773568572", "Tomb of the Ancients", randomChannel, String.valueOf((new Color(255, 212, 79)).getRGB()), "15"},
                //Lair of Draconis
                {"723001215214026835", "767819091293372447", "Lair of Draconis", randomChannel, String.valueOf((new Color(189, 161, 0)).getRGB()), "15"},

                //Highlands Dungeons
                //Parasite Chambers
                {"767952088256479262", "767819092224376844", "Parasite Chambers", randomChannel, String.valueOf((new Color(158, 34, 34)).getRGB()), "15"},
                //Mad Lab
                {"767951828766687275", "767819092576174160", "Mad Lab", randomChannel, String.valueOf((new Color(125, 31, 255)).getRGB()), "15"},
                //Haunted Cemetery
                {"767952240987865098", "767819091540705340", "Haunted Cemetery", randomChannel, String.valueOf((new Color(58, 160, 0)).getRGB()), "15"},
                //Cursed Library
                {"767951597804060702", "767819091226394646", "Cursed Library", randomChannel, String.valueOf((new Color(41, 48, 62)).getRGB()), "15"},
                //Manor of the Immortals
                {"723001215549440081", "767819092596883456", "Manor of the Immortals", randomChannel, String.valueOf((new Color(111, 80, 120)).getRGB()), "15"},
                //Sprite World
                {"723001214828281858", "767819092677361705", "Sprite World", randomChannel, String.valueOf((new Color(147, 147, 147)).getRGB()), "15"},
                //Undead Lair
                {"767950706577113130", "767819092635156501", "Undead Lair", randomChannel, String.valueOf((new Color(215, 151, 142)).getRGB()), "15"},
                //Abyss of Demons
                {"767951332073144390", "767819091519864842", "Abyss of Demons", randomChannel, String.valueOf((new Color(156, 43, 43)).getRGB()), "15"},
                //Puppet Master’s Theatre
                {"723001215264227329", "767819092638957629", "Puppet Master’s Theatre", randomChannel, String.valueOf((new Color(193, 38, 38)).getRGB()), "15"},
                //Toxic Sewers
                {"723001215268421702", "767819092698071102", "Toxic Sewers", randomChannel, String.valueOf((new Color(76, 83, 74)).getRGB()), "15"},

                //Random Dungeons
                //Pirate Cave
                {"723001215184535553", "767819092371308606", "Pirate Cave", randomChannel, String.valueOf((new Color(123, 56, 29)).getRGB()), "15"},
                //Forest Maze
                {"723001215276810371", "767819091557613669", "Forest Maze", randomChannel, String.valueOf((new Color(53, 121, 13)).getRGB()), "15"},
                //Spider Den
                {"723001215478399088", "767819092719042571", "Spider Den", randomChannel, String.valueOf((new Color(84, 160, 41)).getRGB()), "15"},
                //Snake Pit
                {"767953167362687018", "767819092316651541", "Snake Pit", randomChannel, String.valueOf((new Color(87, 168, 43)).getRGB()), "15"},
                //Forbidden Jungle
                {"723001215214157916", "768175442661998592", "Forbidden Jungle", randomChannel, String.valueOf((new Color(82, 113, 111)).getRGB()), "15"},
                //The Hive
                {"723001215503302696", "767819092626374728", "The Hive", randomChannel, String.valueOf((new Color(255, 217, 68)).getRGB()), "15"},
                //Magic Woods
                {"723001215268683796", "767819092576043098", "Magic Woods", randomChannel, String.valueOf((new Color(96, 177, 48)).getRGB()), "15"},
                //Candyland
                {"723001215449038868", "767819091531530260", "Candyland", randomChannel, String.valueOf((new Color(255, 111, 128)).getRGB()), "15"},
                //Ancient Ruins
                {"753021093685755955", "767819091146571778", "Ancient Ruins", randomChannel, String.valueOf((new Color(132, 103, 59)).getRGB()), "15"},
                //The Machine
                {"767953802909188116", "768176461554516069", "The Machine", randomChannel, String.valueOf((new Color(27, 88, 43)).getRGB()), "15"},
                //Realm Clearing
                {"768183740387295253", "767656775796391947", "Realm Clearing", randomChannel, String.valueOf((new Color(65, 237, 255)).getRGB()), "15"},

                //Random Lobbies
                //Boost Boys
                {"768185025749057586", "767656775796391947", "Boost Boys", randomChannel, String.valueOf((new Color(228, 72, 255)).getRGB()), "15"},
                //Among Us
                {"767656425740304385", "767656775796391947", "Among Us", amongUsChannel, String.valueOf((new Color(255, 65, 65)).getRGB()), "10"},
        };


        //MBC
        //Cultist
        //Shatters
        //Thicket
        //Tomb
        //Wlab
        return dungeonInfo;
    }

}
