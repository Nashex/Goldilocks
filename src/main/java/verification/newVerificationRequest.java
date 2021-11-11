package verification;

import com.cloudinary.utils.ObjectUtils;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.apache.commons.lang3.StringUtils;
import org.ocpsoft.prettytime.PrettyTime;
import quota.LogField;
import setup.SetupConnector;
import shatters.SqlConnector;
import utils.Charts;
import utils.EmoteCache;
import utils.MemberSearch;

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

import static main.Goldilocks.eventWaiter;
import static main.Goldilocks.proxyHelper;

public class newVerificationRequest {

    private String username;
    public Member member;
    private User user;
    private Guild guild;
    private String veriCode;
    private PrivateChannel privateChannel;

    private CompactPlayerProfile playerProfile = null;
    private String pet = "";
    private GraveyardSummary gs = null;
    private List<List<LogField>> history = null;
    private List<String> graveyard;
    private String description[];

    private int stage;
    private boolean verify = false;

    private Message message;
    private Message activeMessage = null;
    private TextChannel veriAttempts = null;
    public long timeEnding;
    private String problems = "";

    public newVerificationRequest(Member member, String veriCode, String name) {
        this.member = member;
        this.user = member.getUser();
        this.guild = member.getGuild();
        this.veriCode = veriCode;

        privateChannel = member.getUser().openPrivateChannel().complete();
        timeEnding = System.currentTimeMillis() + VerificationHub.MAX_MILLIS_NAME;
        proxyHelper.refreshProxyList();
        if (!veriCode.isEmpty()) getPlayerName();
        else {
            username = name;
            createActiveVerification();
            startVerification();
        }
    }

    public void getPlayerName() {
        try {
            message = privateChannel.sendMessage(initialMessageEmbed().build()).complete();
        } catch (Exception e) {VerificationHub.logVerificationDenial(member, guild.getSelfMember(), "Null", "DMs are disabled for this server."); return;}
        createActiveVerification();

        stage = 1;

        Goldilocks.eventWaiter.waitForEvent(MessageReceivedEvent.class, e -> {
            return !e.isFromGuild() && e.getAuthor().equals(user) && e.getMessage().getContentRaw().split(" ").length <= 1;
        }, e -> {

            username = e.getMessage().getContentRaw();
            startVerification();

        }, 2L, TimeUnit.MINUTES, () -> {
            endActiveVeri();
            message.delete().queue();
        });
    }

    public void startVerification() {
        if (MemberSearch.memberSearch(username, guild) != null) {
            privateChannel.sendMessage("There is already a user verified under this ign. If you believe this to be an error please contact any security+.").queue();
            VerificationHub.logVerificationDenial(member, guild.getSelfMember(), username, "There is already a user verified under this ign.");
            endActiveVeri();
            return;
        }
        String memberExpelled = Database.isExpelled(member);
        String nameExpelled = Database.isExpelled(username, guild);

        if (!memberExpelled.isEmpty()) {
            privateChannel.sendMessage("You have been verification blacklisted for being a suspected alt. Please message "
                    + (guild.getMemberById(memberExpelled) == null ? " any Security+" : guild.getMemberById(memberExpelled).getAsMention()) + " if you would like to appeal this.").queue();
            VerificationHub.logVerificationDenial(member, guild.getSelfMember(), username, "User was blacklisted from verification.");
            endActiveVeri();
            return;
        }

        if (!nameExpelled.isEmpty()) {
            privateChannel.sendMessage("This name has been verification blacklisted for being a suspected alt. Please message "
                    + (guild.getMemberById(nameExpelled) == null ? " any Security+" : guild.getMemberById(nameExpelled).getAsMention()) + " if you would like to appeal this.").queue();
            VerificationHub.logVerificationDenial(member, guild.getSelfMember(), username, "Name was blacklisted from verification.");
            endActiveVeri();
            return;
        }
        if (Database.isShatters(guild)) {
            String shattersExpelledId = SqlConnector.isExpelled(member.getId());
            String shattersExpelledName = SqlConnector.isExpelled(username);
            if ((!shattersExpelledId.isEmpty() || !shattersExpelledName.isEmpty())) {
                privateChannel.sendMessage("This name has been verification blacklisted for being a suspected alt. Please message  any Security+ if you would like to appeal this.").queue();
                VerificationHub.logVerificationDenial(member, guild.getSelfMember(), username, "Name was blacklisted from verification.");
                endActiveVeri();
                return;
            }
        }

        Goldilocks.TIMER.schedule(() -> {
            try {
                stage = 0;
                if (message != null) message.editMessage(retrievingEmbed().build()).queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
                else message = privateChannel.sendMessage(retrievingEmbed().build()).complete();
                playerProfile = new CompactPlayerProfile(username);
                pet = BackgroundCheck.petScrape(username);
                gs = new GraveyardSummary(username);
                history = BackgroundCheck.fameHistoryScrape(username);
            } catch (Exception e1) {
                playerProfile = null;
            }
            confirmPlayerName();
        }, 0L, TimeUnit.SECONDS);
    }

    public void confirmPlayerName() {
        timeEnding = System.currentTimeMillis() + VerificationHub.MAX_MILLIS;
        message.delete().queue();
        message = privateChannel.sendMessage(finalStepEmbed().build()).complete();
        if (veriAttempts != null) veriAttempts.sendMessage(veriInProgessEmbed(problems).build()).queue();
        message.addReaction("❌").queue();
        message.addReaction("✅").queue();
        stage = 2;
        settingReactionListener();
    }

    public void settingReactionListener() {
        VerificationHub.VerificationThread.schedule(() -> {
            eventWaiter.waitForEvent(PrivateMessageReactionAddEvent.class, e -> {
                return e.getUser().equals(user) && e.getReactionEmote().isEmoji() && ("❌✅").contains(e.getReactionEmote().getEmoji());
            }, e -> {
                String emote = e.getReactionEmote().getEmoji();
                if (stage == 3) return;
                if (("✅").equals(emote)) {
                    if (stage != -1) {
                        Goldilocks.TIMER.schedule(() -> {
                            stage = -1;
                            message.editMessage(finalStepEmbed().setDescription("```\nRefreshing data...\n```\n" + finalStepEmbed().getDescriptionBuilder().toString()).build()).queue();
                            try {
                                playerProfile = new CompactPlayerProfile(username);
                            } catch (PlayerProfile.PrivateProfileException privateProfileException) {playerProfile = null;}
                            pet = BackgroundCheck.petScrape(username);
                            gs = new GraveyardSummary(username);
                            history = BackgroundCheck.fameHistoryScrape(username);
                            MessageEmbed finalStepEmbed = finalStepEmbed().build();
                            if (veriAttempts != null) veriAttempts.sendMessage(veriInProgessEmbed(problems).build()).queue();
                            if (verify) {
                                stage = 3;
                                executeVerification();
                            } else message.editMessage(finalStepEmbed).queue(msg -> stage = 2);
                        }, 0L, TimeUnit.SECONDS);
                    }
                    settingReactionListener();
                } else {
                    stage = 3;
                    endActiveVeri();
                    message.editMessage(verificationEndEmbed("User has chosen to exit verification.").build()).complete();
                }

            }, timeEnding - System.currentTimeMillis(), TimeUnit.MILLISECONDS, () -> {
                stage = 3;
                message.editMessage(verificationEndEmbed("Verification has timed out.").build()).complete();
                endActiveVeri();
            });
        }, 0L, TimeUnit.SECONDS);
    }

    /*
    Active verifications
     */

    public void createActiveVerification() {
        String activeVeriId = SetupConnector.getFieldValue(guild,"guildLogs", "activeVerificationChannelId");
        String veriAttemptsId = SetupConnector.getFieldValue(guild,"guildLogs", "verificationAttemptsChannelId");
        if (!activeVeriId.equals("0")) {
            activeMessage = Goldilocks.jda.getTextChannelById(activeVeriId).sendMessage(veriStartEmbed().build()).complete();
        }
        if (!veriAttemptsId.equals("0")) {
            veriAttempts = Goldilocks.jda.getTextChannelById(veriAttemptsId);
            if (veriAttempts != null) veriAttempts.sendMessage(veriStartEmbed().build()).queue();
        }
    }

    public void updateActiveVeri(String problems) {
        if (activeMessage != null) activeMessage.editMessage(veriInProgessEmbed(problems).build()).queue();
    }

    public void endActiveVeri() {
        VerificationHub.newGeneralVerifications.remove(this);
        if (activeMessage != null) activeMessage.editMessage(veriEndEmbed().build()).complete().delete().queueAfter(15L, TimeUnit.SECONDS, null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
        if (veriAttempts != null) veriAttempts.sendMessage(veriEndEmbed().build()).queue();
    }

    private EmbedBuilder veriEndEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setDescription(member.getAsMention() + " has ended the verification process.")
                .setColor(Goldilocks.RED)
                .setFooter("This message will delete in 15 seconds")
                .setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    private EmbedBuilder veriInProgessEmbed(String problems) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setDescription(member.getAsMention() + " is attempting to verify with the name **[" + username + "](https://realmeye.com/player/" + username + ")** and" +
                " has to fix the following before they can be verified:\n```\n" + (problems.isEmpty() ? "None" : problems) + "\n```")
                .setColor(Goldilocks.LIGHTBLUE)
                .setFooter("Verification will timeout in: " + formatTime())
                .setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    private EmbedBuilder veriStartEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setDescription(member.getAsMention() + " has begun the verification process.")
                .setColor(Goldilocks.LIGHTBLUE)
                .setFooter("Verification will timeout in: " + formatTime())
                .setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    /*
    Verification
     */

    public void executeVerification() {
        int fameReq = Integer.parseInt(Database.getGuildInfo(guild, "aliveFameRequirement"));
        int starReq = Integer.parseInt(Database.getGuildInfo(guild, "starRequirement"));
        int petLevelReq = Integer.parseInt(Database.getGuildInfo(guild, "petLevelRequirement"));
        int minDeaths = Integer.parseInt(Database.getGuildInfo(guild, "deathsRequirement"));
        int glandDungeonReq = Integer.parseInt(Database.getGuildInfo(guild, "godLandDungeonRequirement"));
        int accountAgeReq = Integer.parseInt(Database.getGuildInfo(guild, "accountAgeRequirement"));

        int udlCompletes = gs.getDungeon("Undead Lairs completed") != null ? Integer.parseInt(gs.getDungeon("Undead Lairs completed").total) : 0;
        int spriteCompletes = gs.getDungeon("Sprite Worlds completed") != null ? Integer.parseInt(gs.getDungeon("Sprite Worlds completed").total) : 0;
        int snakePitCompletes = gs.getDungeon("Snake Pits completed") != null ? Integer.parseInt(gs.getDungeon("Snake Pits completed").total) : 0;

        graveyard = BackgroundCheck.graveScrape(username);
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
            problems += "Pet Level: " + petLevel + "/" + petLevelReq + "\n";
        }
        if (deaths < minDeaths) {
            problems += "Deaths: " + deaths + "/" + minDeaths + "\n";
        }
        if (udlCompletes < glandDungeonReq) {
            problems += "UDL Completes: " + udlCompletes + "/" + glandDungeonReq + "\n";
        }
        if (spriteCompletes < glandDungeonReq) {
            problems += "Sprite Completes: " + spriteCompletes + "/" + glandDungeonReq + "\n";
        }
        if (snakePitCompletes < glandDungeonReq) {
            problems += "Snake Pit Completes: " + snakePitCompletes + "/" + glandDungeonReq + "\n";
        }
        if (history.get(2).size() < 10) {
            problems += "All-Time Fame Aggregation Below Threshold\n";
        }
        if (!playerProfile.firstSeen.toLowerCase().contains("year")) {
            if (accountAgeReq > 1) {
                String dayString = playerProfile.firstSeen.replaceAll("[^0-9]","");
                if (!dayString.isEmpty()) {
                    if (accountAgeReq > Integer.parseInt(dayString)) problems += "Realm Account Age: " + playerProfile.firstSeen + "\n";
                } else problems += "Realm Account Age: " + playerProfile.firstSeen + "\n";
            } else {
                problems += "Realm Account Age: " + playerProfile.firstSeen + "\n";
            }
        }

        System.out.println(guild.getName() + " | " + (problems.isEmpty() ? "User automatically verified" : "User sent to manual verification"));

        if (problems.isEmpty()) {
            message.delete().queue();
            privateChannel.sendMessage("You have been successfully verified in " + guild.getName() + "! Please read the rules and happy raiding!").queue();
            Goldilocks.TIMER.schedule(() -> VerificationHub.verifyUser(member, guild.getSelfMember(), username, VerificationHub.getGraphsURL(username, history, gs)), 0L, TimeUnit.SECONDS);
        }
        else sendManualVerification(problems);
        endActiveVeri();

    }

    public void sendManualVerification(String problems) {
        message.delete().queue(aVoid -> privateChannel.sendMessage(manualVerificationEmbed().build()).queue());
        TextChannel verificationChannel = Goldilocks.jda.getTextChannelById(Database.getGuildInfo(guild, "verificationChannelId"));
        VerificationHub.VerificationThread.schedule(() -> {
            try {
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

                EmbedBuilder profileEmbed = new EmbedBuilder()
                        .setAuthor(user.getAsTag() + " is attempting to verify as: " + playerProfile.username, user.getAvatarUrl())
                        .setDescription("**" + member.getAsMention() + ": [RealmEye](https://realmeye.com/player/" + username + ") **")
                        .addField("Rank", playerProfile.stars + " " + starEmote, true)
                        .addField("Guild", Optional.ofNullable(playerProfile.guild).orElse("No Guild"), true)
                        .addField("Guild Rank", playerProfile.guildRankString + " " + playerProfile.guildRankEmote, true)
                        .addField("Alive Fame", playerProfile.aliveFame + " <:fame:826360464865755136>", true)
                        .addField("Death Fame", playerProfile.accountFame + " <:fame:826360464865755136>", true)
                        .addField("Deaths (%)", graveyard.isEmpty() ? "Private" : graveyard.get(0) + " (" + graveyard.get(1) + ")", true)
                        .addField("Skins", String.valueOf(playerProfile.skins), true)
                        .addField("Characters", String.valueOf(playerProfile.characters), true)
                        .addField("Pet " + (pet.contains(":") ? EmoteCache.tempCacheEmote(pet.split(":")[1]) : ""), (pet.contains(":") ? pet.split(":")[0] : pet), true)
                        .addField("First Seen", String.valueOf(playerProfile.firstSeen), true)
                        .addField("Last Seen", lastSeen, true)
                        .addField("Active For", (gs.getAchievement("Active for") == null ? "Not found" : gs.getAchievement("Active for").total), true)
                        .addField("Discord Created", new PrettyTime().format(user.getTimeCreated()), false);
                try {
                    List<MessageEmbed.Field> fields = new CharacterSummary(username).getCharacters().fieldify();
                    for (MessageEmbed.Field f : fields) profileEmbed.addField(f);
                } catch (IOException exception) { }

                profileEmbed.addField("Name Changes", "```\n" + (nameHistory.isEmpty() ? "None detected" : nameHistory) + "\n```", false)
                        .addField("Problems", "```\n" + problems + "\n```", false)
                        .setFooter(user.getId() + " | " + username);
                if (!outputURL.isEmpty()) profileEmbed.setImage(outputURL);

//                Message verificationMessage;
//                if (errorURL.isEmpty()) verificationMessage = verificationChannel.sendFile(file,"dungeons.png").embed(profileEmbed.build()).complete();
//                else verificationMessage = verificationChannel.sendMessage(profileEmbed.build()).complete();
                Message verificationMessage = verificationChannel.sendMessage(profileEmbed.build())
                        .setActionRow(Button.success("unlock", "Unlock this Verification Panel")).complete();

                VerificationHub.newManualVerificationRequests.add(new newManualVerificationRequest(verificationMessage, username, member));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0L, TimeUnit.SECONDS);
    }

    /*
    Embeds
     */

    private EmbedBuilder initialMessageEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Verification Request for " + guild.getName())
                .setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("Thank you for verifying with " + guild.getName() + "! You are just have a couple more steps to do before " +
                "you are fully verified.\n\nFirst, please type your __in-game name for Realm of the Mad God__ in the chat below.");
        embedBuilder.setFooter("Your verification will timeout in: " + formatTime());

        return embedBuilder;
    }

    private EmbedBuilder retrievingEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Verification Request for " + guild.getName())
                .setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("```\nRetrieving user data from RealmEye\n```");
        embedBuilder.setFooter("Your verification will timeout in: " + formatTime());

        return embedBuilder;
    }

    private EmbedBuilder finalStepEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Verification Instructions for " + guild.getName())
                .setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("You have stated that your in-game name is: `" + username + "`" +
                ", if this is incorrect please react with ❌ to cancel this verification." +
                "\n\nNow we need to make sure that you have the correct privacy settings! For this step you can access" +
                " your RealmEye page privacy settings  [here](https://www.realmeye.com/settings-of/" + username + ")." +
                " Please ensure your settings look like [this](https://res.cloudinary.com/nashex/image/upload/v1617138296/descriptions/Realmeye_settings_ipvyne.png)" +
                " or you will not be able to be verified. __Everything besides last location must be set to **everyone**.__" +
                "\n\nIf you do not have access to your realm eye password type `/tell mreyeball password` in-game to get it.\n" +
                (veriCode.isEmpty() ? "" : "\n**Verification Code**\nYour verification code is below, please put this in any line of your Realm Eye description.```\n" + veriCode + "\n```\n") +
                "***React with ✅ when you have finished these steps. Please note it may take up to 30 seconds for your RealmEye to update.***");

        embedBuilder.setFooter("Your verification will timeout in: " + formatTime());

        String problems = "";
        boolean vericodeInDesc = false;
        if (playerProfile == null) problems = "Your profile is set to private.";
        else {
            vericodeInDesc = playerProfile.description.contains(veriCode) || veriCode.isEmpty();
            problems += (!vericodeInDesc? "Vericode not in description\n" : "") +
                    (pet.equals("Hidden") ? "Pet yard is set to Nobody\n" : "") +
                    (gs.dungeons.isEmpty() ? "Graveyard is set to Nobody\n" : "") +
                    (playerProfile.hiddenNameHistory ? "Name history is set to Nobody\n" : "") +
                    (history == null ? "Fame history is set to Nobody\n" : "") +
                    (playerProfile.characters == -1 ? "Characters is set to Nobody\n" : "") +
                    (playerProfile.skins == -1 ? "Skins is set to Nobody\n" : "") +
                    (playerProfile.firstSeen.toLowerCase() == "hidden" ? "Account created is set to Nobody\n" : "") +
                    (!playerProfile.hiddenLocation ? "Account location is set to Everyone\n" : "");
        }
        embedBuilder.addField("Problems", "```\n" + (problems.isEmpty() ? "None" : problems) + "\n```", false);
        if (!vericodeInDesc && playerProfile != null) {
            String description[] = playerProfile.description.replaceAll(" ", "%20").split("\n");
            if (description.length == 3) {
                String desc = "";
                for (int i = 0; i < 3; i++) desc += StringUtils.center(description[i].replaceAll("%20", " "), 60) + "\n";
                embedBuilder.addField("Your Description", "```\n" + desc + "\n```", false);
            }
        }

        this.problems = problems;

        updateActiveVeri(problems);
        verify = problems.isEmpty();

        return embedBuilder;
    }

    private EmbedBuilder verificationEndEmbed(String reason) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Your Verification Request for " + guild.getName() + " was Canceled")
                .setColor(Goldilocks.RED)
                .setDescription("**Reason**\n" + "```\n" + reason + "\n```")
                .setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    private EmbedBuilder manualVerificationEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Your Verification Request for " + guild.getName() + " was Flagged")
                .setColor(Goldilocks.RED)
                .setDescription("You have been flagged for manual verification. Please allow up to 24 hours for this process to" +
                        " finish. If by the end of those 24 hours you do not recieve an update please feel free to re-enter the verification " +
                        "process.")
                .setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    /*
    Time methods
     */

    public void updateMessageTime() {
        try {
            switch (stage) {
                case 1:
                    message.editMessage(initialMessageEmbed().build()).submit().exceptionally(throwable -> null);
                    break;
                case -1:
                case 2:
                    message.editMessage(finalStepEmbed().build()).queue();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {}
    }

    private String formatTime() {
        long totalSeconds = (timeEnding - System.currentTimeMillis()) / 1000;
        long mins = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        seconds = seconds % 10 != 0 ? (10 - (seconds % 10) + seconds) : seconds;
        if (seconds == 60L) {mins++; seconds = 0;}

        return (mins == 1 ? mins + " minute and " : mins + " minutes and ") + seconds + " seconds";
    }

    public void timeout() {
        stage = 3;
        VerificationHub.newGeneralVerifications.remove(this);
        message.editMessage(verificationEndEmbed("Verification has timed out.").build()).complete();
    }

}
