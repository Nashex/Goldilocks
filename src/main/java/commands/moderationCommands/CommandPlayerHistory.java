package commands.moderationCommands;

import com.cloudinary.utils.ObjectUtils;
import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import main.Permissions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.ocpsoft.prettytime.PrettyTime;
import quota.LogField;
import stats.CharacterExaltation;
import utils.Charts;
import utils.EmoteCache;
import utils.MemberSearch;
import utils.Utils;
import verification.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CommandPlayerHistory extends Command {
    public CommandPlayerHistory() {
        setAliases(new String[] {"history", "playerhistory", "pinfo", "playerinfo"});
        setEligibleRoles(new String[] {"tSec", "arl", "headEo"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        TextChannel textChannel = msg.getTextChannel();
        Guild guild = msg.getGuild();

        if (args.length == 0) {
            Utils.errorMessage("Failed to Check User Player History", "Please use the command in the following format: history <name/@/id>", textChannel, 10L);
            return;
        }

        Message result = textChannel.sendMessage("<a:loading:830993855808536616> Retrieving RealmEye information for " + args[0]).complete();


        String rankHistory = "";
        //String nameHistory = "";
        String guildHistory = "";
        String fameHistory = "";
        String playersInServer = "";
        String formerPlayersInServer = "";
        String guildiesInVc = "";
        String firstSeen = "";

        String username = args[0];
        Goldilocks.TIMER.schedule(() -> {
            try {
                int fameReq = Integer.parseInt(Database.getGuildInfo(guild, "aliveFameRequirement"));
                int starReq = Integer.parseInt(Database.getGuildInfo(guild, "starRequirement"));
                int petLevelReq = Integer.parseInt(Database.getGuildInfo(guild, "petLevelRequirement"));
                int minDeaths = Integer.parseInt(Database.getGuildInfo(guild, "deathsRequirement"));

                Member member = MemberSearch.memberSearch(username, guild);
                CompactPlayerProfile playerProfile = new CompactPlayerProfile(username);
                String pet = BackgroundCheck.petScrape(username);
                GraveyardSummary gs = new GraveyardSummary(username);
                List<List<LogField>> history = BackgroundCheck.fameHistoryScrape(username);
                //String[] playerIgns = (member != null ? member.getEffectiveName().replaceAll("[^A-z- ]", "").split(" ") : playerNames);

                String errorURL = "";
                File file = new File("data/playerCharts/allCharts" + username + ".png");
                if (history != null) {
                    try {
                        BufferedImage bufferedImage = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2d = bufferedImage.createGraphics();
                        g2d.drawImage(ImageIO.read(Charts.getFameChartFile(history.get(0), username + "_daychart")), null, 400, 0);
                        g2d.drawImage(ImageIO.read(Charts.getFameChartFile(history.get(1), username + "_weekChart")), null, 0, 300);
                        g2d.drawImage(ImageIO.read(Charts.getFameChartFile(history.get(2), username + "_allTimeChart")), null, 400, 300);
                        g2d.drawImage(ImageIO.read(Charts.getDungeonChartFile(gs)), null, 0, 0);
                        g2d.dispose();
                        ImageIO.write(bufferedImage, "png", file);
                    } catch (Exception e) {errorURL = "https://res.cloudinary.com/nashex/image/upload/v1616976297/assets/dungeons_luxexn_jzz8qs.png";}
                } else errorURL = "https://res.cloudinary.com/nashex/image/upload/v1618513990/assets/Famehistoryprivate_urt9q9.png";


                String outputURL = "";
                try {
                    outputURL = errorURL.isEmpty() ? (String) Goldilocks.cloudinary.uploader().upload(file, ObjectUtils.asMap("public_id", "charts/" + username + "_History")).get("url") : errorURL;
                } catch (Exception e) {
                    outputURL = "https://res.cloudinary.com/nashex/image/upload/v1618513990/assets/Famehistoryprivate_urt9q9.png";
                }
                List<String> graveyard = BackgroundCheck.graveScrape(username);
                int petLevel = pet.equals("No Pets") || pet.equals("Hidden") ? 0 : pet.split(" ")[0].replaceAll("[^0-9]", "").isEmpty() ? 0 : Integer.parseInt(pet.split(" ")[0].replaceAll("[^0-9]", ""));
                int deaths = graveyard.isEmpty() ? 0 : graveyard.get(0).isEmpty() ? 0 : graveyard.get(0).replaceAll("[^0-9]", "").isEmpty() ? 0 : Integer.parseInt(graveyard.get(0).replaceAll("[^0-9]", ""));
                String problems = "";

                if (playerProfile.aliveFame < fameReq) {
                    problems += "Alive fame: " + playerProfile.aliveFame + "/" + fameReq + "\n";
                }
                if (playerProfile.stars < starReq) {
                    problems += "Stars: " + playerProfile.stars + "/" + starReq + "\n";
                }
                if (petLevel < petLevelReq) {
                    problems += "Pet Level: " + (pet.equals("Hidden") ? "Hidden" : petLevel + "/" + petLevelReq) + "\n";
                }
                if (deaths < minDeaths) {
                    problems += "Deaths: " + deaths + "/" + minDeaths + "\n";
                }
                if (history != null && history.size() > 2 && history.get(2).size() < 10) {
                    problems += "All-Time Fame Aggregation Below Threshold\n";
                }
                if (!playerProfile.firstSeen.toLowerCase().contains("year")) {
                    problems += "Realm Account Age: " + playerProfile.firstSeen + "\n";
                }
//                Map chartDayMap, chartWeekMap, chartAlltimeMap, chartDungeonMap;
//                if (!chartDay.isEmpty() && !chartWeek.isEmpty() && !chartAlltime.isEmpty() && !dungeonChart.isEmpty()) {
//                    try {
//                        chartDayMap = Goldilocks.cloudinary.uploader().upload(chartDay, ObjectUtils.asMap("public_id", "charts/" + username + "Day"));
//                        chartWeekMap = Goldilocks.cloudinary.uploader().upload(chartWeek, ObjectUtils.asMap("public_id", "charts/" + username + "Week"));
//                        chartAlltimeMap = Goldilocks.cloudinary.uploader().upload(chartAlltime, ObjectUtils.asMap("public_id", "charts/" + username + "Alltime"));
//                        chartDungeonMap = Goldilocks.cloudinary.uploader().upload(dungeonChart, ObjectUtils.asMap("public_id", "charts/" + username + "Dungeon"));
//                        outputUrl = Goldilocks.cloudinary.url().transformation(new Transformation()
//                                .width(400).height(300).crop("fill").chain()
//                                .overlay(new Layer().publicId(chartDayMap.get("public_id").toString())).width(400).height(300).x(400).crop("fill").chain()
//                                .overlay(new Layer().publicId(chartWeekMap.get("public_id").toString())).width(400).height(300).y(300).x(-200).crop("fill").chain()
//                                .overlay(new Layer().publicId(chartAlltimeMap.get("public_id").toString())).width(400).height(300).y(150).x(200).crop("fill")).imageTag(chartDungeonMap.get("public_id").toString()).split("'")[1];
//                    } catch (Exception e) { }
//                }


                List<String> nameChanges = new ArrayList<>(playerProfile.nameHistory.keySet());
                Collections.reverse(nameChanges);
                String nameHistory = nameChanges.stream().collect(Collectors.joining(" ⇒ "));
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd zzz");
                String lastSeen = (playerProfile.hiddenLocation ? "Hidden" : "~" + new PrettyTime().format(LocalDate.parse(playerProfile.lastSeen.split(" ")[0] + " UTC", formatter)));

                String starEmote = "";
                if (playerProfile.stars < 17) starEmote = "<:lightbluestar:798257702920650772>";
                else if (playerProfile.stars < 34) starEmote = "<:darkbluestar:798257702827982848>";
                else if (playerProfile.stars < 51) starEmote = "<:redstar:798257702894960680>";
                else if (playerProfile.stars < 67) starEmote = "<:orangestar:798257705268674560>";
                else if (playerProfile.stars < 84) starEmote = "<:yellowstar:798257702886834186>";
                else if (playerProfile.stars == 85) starEmote = "<:whitestar:798257702677118977>";

                List<CharacterExaltation> exaltations = BackgroundCheck.exaltationScrape(username);
                List<String> exaltStrings = new ArrayList<>();
                if (!exaltations.isEmpty()) {
                    String exalts = "<:empty:749206900176060536> ";
                    for (int i = 0; i < 8; i++) exalts += "` " + String.format("%-3s", exaltations.get(0).HEADERS[i]) + (i < 2 ? "" : " ") + "` ";
                    exalts += "\n";
                    for (CharacterExaltation exaltation : exaltations) {
                        String temp = exaltation.characterEmote.getAsMention() + " ";
                        for (int i = 0; i < 8; i++) temp += "` " + String.format("%1$2d", exaltation.exaltationData[i]) + (i < 2 ? "" : " ") + " ` ";
                        if (exalts.length() + temp.length() < 1024) exalts += temp + "\n";
                        else {
                            exaltStrings.add(exalts);
                            exalts = "";
                        }
                    }
                    if (!exalts.isEmpty()) exaltStrings.add(exalts);
                }

                EmbedBuilder profileEmbed = new EmbedBuilder()
                        .setColor(Goldilocks.WHITE)
                        .setTitle(username + "'s RealmEye History")
                        .setDescription("**Tag:** " + (member == null ? "Not found" : member.getAsMention()) +  " |  **[RealmEye](https://realmeye.com/player/" + username + ") **")
                        .addField("Rank", playerProfile.stars + " " + starEmote, true)
                        .addField("Guild", Optional.ofNullable(playerProfile.guild).orElse("No Guild"), true)
                        .addField("Guild Rank", playerProfile.guildRankString + " " + playerProfile.guildRankEmote, true)
                        .addField("Alive Fame", playerProfile.aliveFame + " <:fame:826360464865755136>", true)
                        .addField("Death Fame", playerProfile.accountFame + " <:fame:826360464865755136>", true)
                        .addField("Deaths (%)", graveyard.isEmpty() ? "Private" : graveyard.get(0) + " (" + graveyard.get(1) + ")", true)
                        .addField("Skins", playerProfile.skins == -1 ? "Hidden" : String.valueOf(playerProfile.skins), true)
                        .addField("Characters", playerProfile.characters == -1 ? "Hidden" : String.valueOf(playerProfile.characters), true)
                        .addField("Pet " + (pet.contains(":") ? EmoteCache.tempCacheEmote(pet.split(":")[1]) : ""), (pet.contains(":") ? pet.split(":")[0] : pet), true)
                        .addField("First Seen", String.valueOf(playerProfile.firstSeen), true)
                        .addField("Last Seen", lastSeen, true)
                        .addField("Active For", (gs.getAchievement("Active for") == null ? "Not found" : gs.getAchievement("Active for").total), true);

                        //.addField("Discord Created", new PrettyTime().format(user.getTimeCreated()), true)
                try {
                    List<MessageEmbed.Field> fields = new CharacterSummary(username).getCharacters().fieldify();
                    if (!fields.isEmpty()) for (MessageEmbed.Field f : fields) profileEmbed.addField(f);
                    if (!exaltStrings.isEmpty()) for (String s : exaltStrings) profileEmbed.addField(exaltStrings.indexOf(s) == 0 ? "Exaltations" : " ", s, false);
                } catch (IOException exception) { }
                profileEmbed.addField("Name Changes", "```\n" + (nameHistory.isEmpty() ? "None detected" : nameHistory) + "\n```", false);
                if (Permissions.hasPermission(msg.getMember(), new String[] {"tSec"})) {
                    if ((Database.isPub(msg.getGuild()) && Arrays.asList(new String[]{"706670131115196588", "464829705728819220"}).contains(textChannel.getId())) || !Database.isPub(msg.getGuild())) {
                        profileEmbed.addField("Problems", "```\n" + (problems.isEmpty() ? "None" : problems) + "\n```", false);
                    }
                }
                if (!outputURL.isEmpty()) profileEmbed.setImage(outputURL);
                result.delete().queue();
                textChannel.sendMessage(profileEmbed.build()).queue();
//            System.out.println(playerIgns[0]);
//            for (String ign : playerIgns) {
//                PlayerProfile playerProfile = new PlayerProfile(ign);
//
//                rankHistory = BackgroundCheck.rankScrape(ign);
//                firstSeen = playerProfile.getFirstSeen();
//                nameHistory = String.join("⇒", BackgroundCheck.nameChangeScrape(ign).keySet().stream().collect(Collectors.toList()));
//                guildHistory = BackgroundCheck.guildScrape(ign);
//                List<String> fameHistoryArr = BackgroundCheck.graveScrape(ign);
//                if (fameHistoryArr.isEmpty()) fameHistory = "Graveyard is set to Private";
//                else fameHistory = "Total Amount of Dead Characters: " + fameHistoryArr.get(0) + "\nTotal Dead >400 Base Fame Chars: " + fameHistoryArr.get(1);
//
//                List<String> playersInGuild = BackgroundCheck.getGuildMembers(playerProfile.getGuild());
//                List<String> formerPlayersInGuild = BackgroundCheck.getGuildFormerMembers(playerProfile.getGuild());
//
//                Map<Member, String> guildMemberNames = msg.getGuild().getMembers().stream().filter(member1 -> !member1.getRoles().isEmpty()).collect(Collectors.toList())
//                        .stream().collect(Collectors.toMap(member1 -> member1, member1 -> member1.getEffectiveName().replaceAll("[^A-Za-z- ]", "").split(" ")[0]));
//                for (String player : playersInGuild) {
//                    if (guildMemberNames.containsValue(player) && !player.equals(ign)) {
//                        Member guildie = guildMemberNames.entrySet().stream().filter(memberStringEntry -> player.equalsIgnoreCase(memberStringEntry.getValue())).collect(Collectors.toList()).get(0).getKey();
//                        if (playersInServer.length() < 950) playersInServer += guildie.getAsMention() + "(" + guildie.getEffectiveName() + ")" + " ";
//                        if (guildie.getVoiceState().inVoiceChannel() && guildiesInVc.length() < 950) {
//                            guildiesInVc += guildie.getAsMention() + "[" + guildie.getVoiceState().getChannel().getName() + "]";
//                        }
//                    }
//
//                }
//
//                for (String player : formerPlayersInGuild) {
//                    if (guildMemberNames.containsValue(player) && !player.equals(ign)) {
//                        Member formerGuildie = guildMemberNames.entrySet().stream().filter(memberStringEntry -> player.equalsIgnoreCase(memberStringEntry.getValue())).collect(Collectors.toList()).get(0).getKey();
//                        if (formerGuildie.getVoiceState().inVoiceChannel() && guildiesInVc.length() < 950) {
//                            formerPlayersInServer += formerGuildie.getAsMention() + "[" + formerGuildie.getVoiceState().getChannel().getName() + "]";
//                        }
//                    }
//
//                }
//
//                EmbedBuilder resultEmbed = new EmbedBuilder();
//                resultEmbed.setTitle("Player History for: " + ign)
//                        .setColor(Goldilocks.WHITE)
//                        .setTimestamp(new Date().toInstant())
//                        .addField("Rank: " + rankHistory.split(",")[0], "```\n" + rankHistory.split(",")[1] + "\n```", true)
//                        .addField("Alive Fame", "```\n" + playerProfile.getAliveFame() + "\n```", true)
//                        .addField("Guild", "```\n" + Optional.ofNullable(playerProfile.getGuild()).orElse("None") + "\n```", true)
//                        .addField("First Seen", "```\n" + firstSeen + "\n```", false)
//                        .addField("Graveyard History", "```\n" + fameHistory + "\n```", false)
//                        .addField("Name Change History", "```\n" + nameHistory + "\n```", false)
//                        .addField("Guild Change History", "```\n" + guildHistory + "\n```", false)
//                        .addField("Guildies in Voicechannel", (guildiesInVc.isEmpty() ? "None" : guildiesInVc), false)
//                        .addField("Former Guildies in Voicechannel", (formerPlayersInServer.isEmpty() ? "None" : formerPlayersInServer), false)
//                        .addField("Guildies in Server", (playersInServer.isEmpty() ? "None" : playersInServer), false)
//                        .setFooter("Searched by " + msg.getMember().getEffectiveName(), msg.getAuthor().getAvatarUrl());
//                if (member != null) embedBuilder.setThumbnail(member.getUser().getAvatarUrl());
//                msg.delete().queue();
//                result.editMessage(resultEmbed.build()).queue();
//
//            }
            } catch (PlayerProfile.PrivateProfileException e) {
                result.delete().queue();
                textChannel.sendMessage("RealmEye is set to private.").queue();
            }
        }, 0L, TimeUnit.SECONDS);

    }


    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Player Info");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Trial Security or Almost Raid Leader\n";
        commandDescription += "Syntax: ;pinfo <caseId>\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nRetrieves player info for a given ign." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

}
