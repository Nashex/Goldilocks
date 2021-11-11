package utils;

import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.Button;
import org.ocpsoft.prettytime.PrettyTime;
import verification.BackgroundCheck;
import verification.CompactPlayerProfile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RealmeyeRequestsIssuer {

    private static ExecutorService REQUEST_ISSUER = Executors.newFixedThreadPool(64);

    public static void ProfileQuickScrape(List<String> players, TextChannel textChannel, String type) {
        List<CompactPlayerProfile> characterList = new ArrayList<>();
        for (String s : players) {
            if (s.length() <= 12) {
                REQUEST_ISSUER.execute(() -> {
                    String ign = s.replaceAll("[^A-Za-z]", "");
                    //System.out.println(s.replaceAll("[^A-Za-z]", ""));
                    switch (type) {
                        case "mini":
                            sendMiniProfile(ign, textChannel);
                            break;
                        case "compact":
                        default:
                            sendCompactProfile(ign, textChannel);
                    }
                });
            }
        }
    }

    public static void sendCompactProfile(String ign, TextChannel textChannel) {
        try {
            List<String> nameChanges = BackgroundCheck.nameChangeScrape(ign);
            CompactPlayerProfile cp = new CompactPlayerProfile(ign);
            String nameScrape = "None Found";
            nameScrape = String.join(" ⇒ ", nameChanges);
            String guildScrape = String.join(" ⇒ ", BackgroundCheck.guildScrape(ign));
            //textChannel.sendMessage(cp.guild + "\n" + nameScrape + "\n" + guildScrape).queue();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd zzz");
            String lastSeen = (cp.hiddenLocation ? "Hidden" : "~" + new PrettyTime().format(LocalDate.parse(cp.lastSeen.split(" ")[0] + " UTC", formatter)));

            String starEmote = "";
            if (cp.stars < 17) starEmote = "<:lightbluestar:798257702920650772>";
            else if (cp.stars < 34) starEmote = "<:darkbluestar:798257702827982848>";
            else if (cp.stars < 51) starEmote = "<:redstar:798257702894960680>";
            else if (cp.stars < 67) starEmote = "<:orangestar:798257705268674560>";
            else if (cp.stars < 84) starEmote = "<:yellowstar:798257702886834186>";
            else if (cp.stars == 85) starEmote = "<:whitestar:798257702677118977>";

            EmbedBuilder profileEmbed = new EmbedBuilder()
                    .setColor(Goldilocks.WHITE)
                    .setTitle(ign.toUpperCase(), "https://realmeye.com/player/" + ign)
                    .addField("Rank", cp.stars + " " + starEmote, true)
                    .addField("Guild", Optional.ofNullable(cp.guild).orElse("No Guild"), true)
                    .addField("Guild Rank", cp.guildRankString + " " + cp.guildRankEmote, true)
                    .addField("Alive Fame", cp.aliveFame + " <:fame:826360464865755136>", true)
                    .addField("Death Fame", cp.accountFame + " <:fame:826360464865755136>", true)
                    .addField("Skins", cp.skins == -1 ? "Hidden" : String.valueOf(cp.skins), true)
                    .addField("Characters", cp.characters == -1 ? "Hidden" : String.valueOf(cp.characters), true)
                    .addField("First Seen", String.valueOf(cp.firstSeen), true)
                    .addField("Last Seen", lastSeen, true)
                    .addField("Name Changes", "```" + (nameScrape.length() > 1000 ? nameScrape.substring(0, 1000) : nameScrape) + "```", false)
                    .addField("Guild Changes", "```\n" + (guildScrape.length() > 1000 ? guildScrape.substring(0, 1000) : guildScrape) + "\n```", false);

            textChannel.sendMessage(profileEmbed.build()).setActionRow(Button.link("https://realmeye.com/player/" + ign, "Characters"),
                    Button.link("https://realmeye.com/graveyard-of-player/" + ign, "Graveyard"),
                    Button.link("https://realmeye.com/fame-history-of-player/" + ign, "Fame History")).queue();

        } catch (Exception e) {
            privateProfileEmbed(ign, textChannel);
        }
    }

    public static void sendMiniProfile(String ign, TextChannel textChannel) {
        try {
            List<String> nameChanges = BackgroundCheck.nameChangeScrape(ign);
            CompactPlayerProfile cp = new CompactPlayerProfile(ign);
            String nameScrape = "None Found";
            nameScrape = String.join(" ⇒ ", nameChanges);
            String guildScrape = String.join(" ⇒ ", BackgroundCheck.guildScrape(ign));

            String starEmote = "";
            if (cp.stars < 17) starEmote = "<:lightbluestar:798257702920650772>";
            else if (cp.stars < 34) starEmote = "<:darkbluestar:798257702827982848>";
            else if (cp.stars < 51) starEmote = "<:redstar:798257702894960680>";
            else if (cp.stars < 67) starEmote = "<:orangestar:798257705268674560>";
            else if (cp.stars < 84) starEmote = "<:yellowstar:798257702886834186>";
            else if (cp.stars == 85) starEmote = "<:whitestar:798257702677118977>";

            EmbedBuilder profileEmbed = new EmbedBuilder()
                    .setColor(Goldilocks.WHITE)
                    .setTitle(ign.toUpperCase(), "https://realmeye.com/player/" + ign)
                    .addField("Rank", cp.stars + " " + starEmote, true)
                    .addField("Guild", Optional.ofNullable(cp.guild).orElse("No Guild"), true)
                    .addField("Guild Rank", cp.guildRankString + " " + cp.guildRankEmote, true)
                    .addField("Name Changes", "```" + (nameScrape.length() > 1000 ? nameScrape.substring(0, 1000) : nameScrape) + "```", false)
                    .addField("Guild Changes", "```\n" + (guildScrape.length() > 1000 ? guildScrape.substring(0, 1000) : guildScrape) + "\n```", false);

            textChannel.sendMessage(profileEmbed.build()).setActionRow(Button.link("https://realmeye.com/player/" + ign, "Characters"),
                    Button.link("https://realmeye.com/graveyard-of-player/" + ign, "Graveyard"),
                    Button.link("https://realmeye.com/fame-history-of-player/" + ign, "Fame History")).queue();

        } catch (Exception e) {
            privateProfileEmbed(ign, textChannel);
        }
    }

    public static void privateProfileEmbed(String ign, TextChannel textChannel) {
        EmbedBuilder profileEmbed = new EmbedBuilder()
                .setColor(Goldilocks.RED)
                .setTitle(ign.toUpperCase(), "https://realmeye.com/player/" + ign)
                .setDescription("```\n This user's realmeye is private. \n```");
        textChannel.sendMessage(profileEmbed.build()).setActionRow(Button.link("https://realmeye.com/player/" + ign, "RealmEye")).queue();
    }

}
