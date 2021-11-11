package verification;

import com.cloudinary.Transformation;
import com.cloudinary.transformation.Layer;
import com.cloudinary.utils.ObjectUtils;
import main.Config;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import quota.LogField;
import setup.SetupConnector;
import sheets.GoogleSheets;
import utils.Charts;
import utils.SSLhelper;
import utils.Utils;

import javax.annotation.Nonnull;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class VerificationHub extends ListenerAdapter {

    public static Map <String, String> headers = new HashMap<>();

    public static ScheduledExecutorService VerificationThread = new ScheduledThreadPoolExecutor(2);
    public static ConcurrentHashMap<User, VerificationRequest> generalVerifications = new ConcurrentHashMap<>();
    public static List<newVerificationRequest> newGeneralVerifications = new ArrayList<>();
    public static List<ManualVerificationRequest> manualVerificationRequests = new ArrayList<>();
    public static List<newManualVerificationRequest> newManualVerificationRequests = new ArrayList<>();
    public static List<ExaltVerificationRequest> vetVerificationRequests = new ArrayList<>();
    public static List<AddAltRequest> addAltRequests = new ArrayList<>();
    public static List<Member> dungeonVerifications = new CopyOnWriteArrayList<>();
    public static int VerificationCode = 0;

    public static Long MVERIFY_EMOTEID = 852426562682093598L;
    public static Long VETVERIFICATION_EMOTEID = 819758336060686396L;
    public static long MAX_MILLIS = 900000L;
    public static long MAX_MILLIS_NAME = 120000L;


    public VerificationHub() {

        headers.put("Connection", "keep-alive");
        headers.put("Cache Control", "max-age=0");
        headers.put("Upgrade Insecure Requests", "1");
        headers.put("Sec Fetch Site", "none");
        headers.put("Sec Fetch Mode", "navigate");
        headers.put("Sec Fetch User", "1");
        headers.put("Sec Fetch Dest", "document");
        headers.put("Accept Encoding", "gzip, deflate, br");
        headers.put("Accept Language", "en-US,en;q=0.9");
        headers.put("Cookie", "n=1");

        VerificationThread.scheduleWithFixedDelay(() -> {
            generalVerifications.forEach((user, verificationRequest) -> verificationRequest.updateMessageTime());
            newGeneralVerifications.forEach(newVerificationRequest::updateMessageTime);
            for (int i = 0; i < newGeneralVerifications.size(); i++) {
                newVerificationRequest req = newGeneralVerifications.get(i);
                if (req.timeEnding < System.currentTimeMillis()) {
                    req.timeout();
                }
            }
            generalVerifications.forEach((user, verificationRequest) -> {
                try {
                    if (verificationRequest.timeEnding - System.currentTimeMillis() < 0) {
                        verificationRequest.verificationError("Your verification has timed out.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Failed to remove verification from array.");
                }
            });
        }, 10L, 10L, TimeUnit.SECONDS);
    }

    public static boolean hasOpenVerification(Member member) {
        for (newVerificationRequest vr : newGeneralVerifications) {
            if (vr.member.equals(member)) return true;
        }
        for (newManualVerificationRequest vr : newManualVerificationRequests) {
            if (vr.member.equals(member)) return true;
        }
        return false;
    }


    public static void requestVerificationUser(Member member, TextChannel textChannel) {
        List<Guild> mutualGuilds = member.getUser().getMutualGuilds().stream().filter(g ->
                Database.getGuildInfo(g, "rank").equals("3") &&
                        g.getMember(member.getUser()).getNickname() != null)
                .collect(Collectors.toList());
        String AlphaNumericString = "ChezapizzaIsHotULOVE2CIT";
        String vericode = textChannel.getGuild().getName().replaceAll("[^A-Z]", "") + "_";
        for (int i = 0; i < 6; i++) vericode += AlphaNumericString.charAt(new Random().nextInt(AlphaNumericString.length() - 1));
        if (mutualGuilds.isEmpty()) {
            newGeneralVerifications.add(new newVerificationRequest(member, vericode, ""));
        } else {
            String name = mutualGuilds.get(0).getMember(member.getUser()).getEffectiveName().split("\\|")[0].replaceAll("[^A-Za-z]", "");
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("You have already been verified with me!")
                    .setDescription("Would you like to use the name `" + name + "`?\n" +
                            "If so please react with ✅ otherwise react with ❌ to continue normally.")
                    .setColor(Goldilocks.GREEN)
                    .setTimestamp(new Date().toInstant());

            Message message;
            PrivateChannel privateChannel = member.getUser().openPrivateChannel().complete();
            try {
                message = privateChannel.sendMessage(embedBuilder.build()).complete();
                message.addReaction("✅").queue();
                message.addReaction("❌").queue();
            } catch (Exception e) {
                textChannel.sendMessage(member.getAsMention() + " please un-private your dms and then re-react with ✅").queue(m -> m.delete().queueAfter(30L, TimeUnit.SECONDS));
                return;
            }

            String finalVericode = vericode;
            Goldilocks.eventWaiter.waitForEvent(MessageReactionAddEvent.class, e -> {
                return !e.getChannelType().isGuild() && e.getUser().equals(member.getUser()) && e.getChannel().equals(privateChannel) && e.getReactionEmote().isEmoji()
                        && ("✅❌").contains(e.getReactionEmote().getEmoji());
            }, e -> {

                if (e.getReactionEmote().getEmoji().equals("✅")) {
                    newGeneralVerifications.add(new newVerificationRequest(member, "", name));
                } else {
                    newGeneralVerifications.add(new newVerificationRequest(member, finalVericode, ""));
                }

                message.delete().queue();

            }, 2L, TimeUnit.MINUTES, () -> {
                message.delete().queue();
            });
        }

        //generalVerifications.put(member.getUser(), new VerificationRequest(member, vericode, textChannel));
        //VerificationCode++;

    }

    public static void retrieveVerifications() {

//        List<Guild> guilds = Goldilocks.jda.getGuilds().stream()
//                .filter(guild -> !Database.getVerificationChannel(guild.getId()).equals("0"))
//                .collect(Collectors.toList());

        List<Guild> guilds = Goldilocks.jda.getGuilds().stream().filter(guild -> SetupConnector.getFieldValue(guild, "guildInfo", "rank").equals("3")
                && !SetupConnector.getFieldValue(guild, "guildLogs", "activeverificationchannelid").equals("0")).collect(Collectors.toList());

        for (Guild g : guilds) {
            try {
                TextChannel textChannel = Goldilocks.jda.getTextChannelById(Database.getVerificationChannel(g.getId()));

                List<Message> verificationRequests = textChannel.getHistory().retrievePast(99).complete().stream()
                        .filter(message -> message.getEmbeds().size() > 0 && message.getEmbeds().get(0).getFooter() != null
                        && message.getEmbeds().get(0).getFooter().getText().contains(" | ") && message.getEmbeds().get(0).getFooter().getText().split(" ").length == 3).collect(Collectors.toList());

                verificationRequests.stream().forEach(message -> {
                    String[] footer = message.getEmbeds().get(0).getFooter().getText().split("( [|] )");
                    Member member = g.getMemberById(footer[0]);
                    String username = footer[1];
                    if (member != null) {
                        newManualVerificationRequests.add(new newManualVerificationRequest(message, username, member));
                        System.out.println("Manual verification retrieved for " + member);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Failed to retrieve verifications for | " + g);
            }
        }
    }

    public static void requestVerificationDungeon(Member member, String dungeon) {

        dungeonVerifications.add(member);

        if (dungeon.equals("amongus")) {
            Role amongUsRole = Goldilocks.jda.getRoleById("767109126446252042");
            if (member.getRoles().contains(amongUsRole)) {
                dungeonVerifications.remove(member);
                return;
            }
            member.getGuild().addRoleToMember(member, amongUsRole).queue();
            dungeonVerifications.remove(member);
            return;
        }

        Guild guild = member.getGuild();
        Role role = null;
        int totalDungeonCompletes = 0;
        switch (dungeon) {
            case "voids":
                totalDungeonCompletes += parseDungeonCompletes(member.getEffectiveName().split(" | ")[0].replaceAll("[^A-Za-z0-9]", ""), "voids");
                totalDungeonCompletes += parseDungeonCompletes(member.getEffectiveName().split(" | ")[0].replaceAll("[^A-Za-z0-9]", ""), "halls");
                role = guild.getRoleById("767108831465046048");
                break;
            case "cults":
                totalDungeonCompletes += parseDungeonCompletes(member.getEffectiveName().split(" | ")[0].replaceAll("[^A-Za-z0-9]", ""), "voids");
                totalDungeonCompletes += parseDungeonCompletes(member.getEffectiveName().split(" | ")[0].replaceAll("[^A-Za-z0-9]", ""), "halls");
                totalDungeonCompletes += parseDungeonCompletes(member.getEffectiveName().split(" | ")[0].replaceAll("[^A-Za-z0-9]", ""), "cults");
                role = guild.getRoleById("768273113057067020");
                break;
            case "fungals":
                totalDungeonCompletes += parseDungeonCompletes(member.getEffectiveName().split(" | ")[0].replaceAll("[^A-Za-z0-9]", ""), "fungals");
                totalDungeonCompletes += parseDungeonCompletes(member.getEffectiveName().split(" | ")[0].replaceAll("[^A-Za-z0-9]", ""), "crystals");
                role = guild.getRoleById("768273269258190848");
                break;
            case "nests":
                totalDungeonCompletes += parseDungeonCompletes(member.getEffectiveName().split(" | ")[0].replaceAll("[^A-Za-z0-9]", ""), dungeon);
                role = guild.getRoleById("768273382801670154");
                break;
            case "shatters":
                totalDungeonCompletes += parseDungeonCompletes(member.getEffectiveName().split(" | ")[0].replaceAll("[^A-Za-z0-9]", ""), dungeon);
                role = guild.getRoleById("768275421409968169");
                break;
        }

        if (member.getRoles().contains(role)) {
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Dungeon Verification Request for " + member.getGuild().getName())
                .setThumbnail(member.getGuild().getIconUrl())
                .setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("```\nRetrieving your user data from realmeye\n```");
        Message verificationMessage = member.getUser().openPrivateChannel().complete().sendMessage(embedBuilder.build()).complete();

        if (totalDungeonCompletes < 10) {
            Utils.errorMessage("Dungeon Verification failed", "You do not have enough " + dungeon + " dungeon completes to verify.", verificationMessage, 15L);
            return;
        }
        guild.addRoleToMember(member, role).queue();
        embedBuilder.clearFields().setDescription("```\nYou have successfully been verified for " + role.getName() + "!\n```");
        verificationMessage.editMessage(embedBuilder.build()).complete().delete().submitAfter(30L, TimeUnit.SECONDS);
        dungeonVerifications.remove(member);
    }

    public ManualVerificationRequest getManualVerificationRequest(String messageId) {
        ManualVerificationRequest manualVerificationRequest = null;

        for (ManualVerificationRequest manualVerificationRequest1 : manualVerificationRequests) {
            if (manualVerificationRequest1.getVerificationControlPanel() != null && manualVerificationRequest1.getVerificationControlPanel().getId().equals(messageId)) {
                manualVerificationRequest = manualVerificationRequest1;
            }
        }

        return manualVerificationRequest;
    }

    public newManualVerificationRequest getNewManualVerificationRequest(String messageId) {
        newManualVerificationRequest manualVerificationRequest = null;

        for (newManualVerificationRequest req : newManualVerificationRequests) {
            if (req.message.getId().equals(messageId)) {
                manualVerificationRequest = req;
            }
        }



        return manualVerificationRequest;
    }



    public AddAltRequest getAddAltRequest(String messageId) {
        AddAltRequest aar = null;
        for (AddAltRequest addAltRequest : addAltRequests) {
            if (addAltRequest.getAddAltControlPanel() != null && addAltRequest.getAddAltControlPanel().getId().equals(messageId)) {
                aar = addAltRequest;
            }
        }
        return aar;
    }

    public ExaltVerificationRequest getVetVerificationRequest(String messageId) {
        ExaltVerificationRequest vvr = null;
        for (ExaltVerificationRequest exaltVerificationRequest : vetVerificationRequests) {
            if (exaltVerificationRequest.getVetVeriControlPanel() != null && exaltVerificationRequest.getVetVeriControlPanel().getId().equals(messageId)) {
                vvr = exaltVerificationRequest;
            }
        }

        return vvr;
    }

    @Override
    public void onButtonClick(@Nonnull ButtonClickEvent event) {
        newManualVerificationRequest manVerify = getNewManualVerificationRequest(event.getMessageId());
        if (manVerify != null) {
            event.deferEdit().queue();
            if (event.getComponentId().equals("unlock")) manVerify.unlockVerification(event.getMember());
        }

        if (manVerify == null) {
            if (event.getGuild() != null && event.getChannel().getId().equals(Database.getGuildInfo(event.getGuild(), "verificationChannelId"))) {
                if (!Objects.requireNonNull(event.getMessage()).getEmbeds().isEmpty() && event.getMessage().getEmbeds().get(0).getFooter() != null
                        && event.getMessage().getEmbeds().get(0).getFooter().getText() != null && event.getMessage().getEmbeds().get(0).getFooter().getText().contains("|")) {
                    String[] footer = event.getMessage().getEmbeds().get(0).getFooter().getText().split("( [|] )");
                    Member member = event.getGuild().getMemberById(footer[0]);
                    String username = footer[1];
                    if (member != null) {
                        newManualVerificationRequest mv = new newManualVerificationRequest(event.getMessage(), username, member);
                        newManualVerificationRequests.add(mv);
                        mv.unlockVerification(event.getMember());
                        System.out.println("FAILURE | Manual verification retrieved for " + member);
                    }
                }
            }
        }
    }

    @Override
    public void onGuildMessageReactionAdd(@Nonnull GuildMessageReactionAddEvent event) {

        if (event.getUser().isBot()) {
            return;
        }

        if (!SetupConnector.getFieldValue(event.getGuild(), "guildInfo","rank").equals("3")) return;

        TextChannel textChannel = event.getChannel();
        TextChannel verificationChannel = Goldilocks.jda.getTextChannelById(Database.getVerificationChannel(event.getGuild().getId()));

        if (textChannel.equals(verificationChannel)) {
            ExaltVerificationRequest exaltVerificationRequest = getVetVerificationRequest(event.getMessageId());
            ManualVerificationRequest manualVerificationRequest = getManualVerificationRequest(event.getMessageId());
            AddAltRequest addAltRequest = getAddAltRequest(event.getMessageId());
            newManualVerificationRequest manVerify = getNewManualVerificationRequest(event.getMessageId());

            if (manualVerificationRequest != null) {
                if (event.getReactionEmote().isEmoji()) {
                    String emote = event.getReactionEmote().getEmoji();
                    if (emote.equals("✅")) {
                        manualVerificationRequests.remove(manualVerificationRequest);
                        manualVerificationRequest.successfulVerification(event.getMember());
                    }
                    if (emote.equals("❌")) {
                        manualVerificationRequests.remove(manualVerificationRequest);
                        manualVerificationRequest.failedVerification(event.getMember());
                    }
                }
            }
            if (addAltRequest != null) {
                if (event.getReactionEmote().isEmoji()) {
                    String emote = event.getReactionEmote().getEmoji();
                    if (emote.equals("✅")) {
                        addAltRequests.remove(addAltRequest);
                        addAltRequest.successfulAlt(event.getMember());
                    }
                    if (emote.equals("❌")) {
                        addAltRequests.remove(addAltRequest);
                        addAltRequest.failedVerification(event.getMember());
                    }
                }
            }
            if (exaltVerificationRequest != null) {
                if (event.getReactionEmote().isEmoji()) {
                    String emote = event.getReactionEmote().getEmoji();
                    if (emote.equals("✅")) {
                        vetVerificationRequests.remove(exaltVerificationRequest);
                        exaltVerificationRequest.successfulExaltVerification(event.getMember());
                    }
                    if (emote.equals("❌")) {
                        vetVerificationRequests.remove(exaltVerificationRequest);
                        exaltVerificationRequest.failedExaltVerification(event.getMember());
                    }
                }
            }

            if (manVerify != null) {
                if (event.getReactionEmote().isEmoji()) {
                    String emote = event.getReactionEmote().getEmoji();
                    if (emote.equals("🔑")) {
                        manVerify.unlockVerification(event.getMember());
                    }
                }
            }

        }

        if (event.getReactionEmote().isEmote()) {
            if (event.getReactionEmote().getEmote().equals(Goldilocks.jda.getEmoteById(MVERIFY_EMOTEID))) {
                manualVerificationRequests.add(new ManualVerificationRequest(event.getMember()));
            }
            if (event.getReactionEmote().getEmote().equals(Goldilocks.jda.getEmoteById(VETVERIFICATION_EMOTEID))) {
                vetVerificationRequests.add(new ExaltVerificationRequest(event.getMember()));
            }
        }

    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {

        User user = event.getAuthor();
        if (generalVerifications.containsKey(user)) {
            VerificationRequest verificationRequest = generalVerifications.get(user);
            verificationRequest.confirmPlayerName(event.getMessage().getContentRaw().trim());
        }

    }

    @Override
    public void onPrivateMessageReactionAdd(PrivateMessageReactionAddEvent event) {

        User user = event.getUser();
        if (event.getReactionEmote().isEmoji() && event.getReactionEmote().getEmoji().equals("✅")) {
            if (generalVerifications.containsKey(user)) {
                VerificationRequest verificationRequest = generalVerifications.get(user);
                if (!verificationRequest.getConfirmedCode()) {
                    verificationRequest.requestPlayerName();
                }
            }
        }

    }

    public static long parseDungeonCompletes(String playerName, String dungeonName) {
        String dungeonString = "";
        switch (dungeonName) {
            case "halls":
                dungeonString = "Lost Halls completed";
                break;
            case "cults":
                dungeonString = "Cultist Hideouts completed";
                break;
            case "voids":
                dungeonString = "Voids completed";
                break;
            case "shatters":
                dungeonString = "Shatters completed1";
                break;
            case "nests":
                dungeonString = "Nests completed2";
                break;
            case "fungals":
                dungeonString = "Fungal Caverns completed";
                break;
            case "crystals":
                dungeonString = "Crystal Caverns completed";
                break;
        }

        try {
            Document doc = Jsoup.connect("https://www.realmeye.com/graveyard-summary-of-player/" + playerName)
                    .proxy(Config.get("PROXY_URL").toString(), Integer.parseInt(Config.get("PROXY_PORT").toString()))
                    .headers(VerificationHub.headers)
                    .sslSocketFactory(SSLhelper.socketFactory())
                    .get();

            Element tempContainer = doc.select("[class='table table-striped main-achievements'], .tbody, td:contains(Nests Completed)").first();

            for (Element completes : tempContainer.getAllElements()) {
                if (completes.tagName("td").text().equals(dungeonString)) {
                    long nestCompletes = Long.parseLong(completes.siblingElements().get(1).text());
                    return nestCompletes;
                }

            }

        } catch (Exception e) {
            //System.out.println("Profile is set to private");
            Goldilocks.proxyHelper.nextProxy();
            return 0L;
        }
        return 0L;
    }

    public static void veteranVerificationSuccess(Role role, Member member, Guild guild, int runCount, boolean success) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Congratulations you Have Been Successfully Verified for " + role.getName() + "!");
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("Please make sure to read the additional raiding rules and happy raiding!");
        embedBuilder.setTimestamp(new Date().toInstant());

        if (success) {
            member.getUser().openPrivateChannel().complete().sendMessage(embedBuilder.build()).queue();
        } else {
            embedBuilder.setTitle(role.getName() + " Verification failed for " + guild.getName());
            embedBuilder.setDescription("You only have " + runCount + " completed runs.");
            member.getUser().openPrivateChannel().complete().sendMessage(embedBuilder.build()).queue(null, new ErrorHandler().ignore(ErrorResponse.CANNOT_SEND_TO_USER));
        }

        logVerification(success, member, guild, runCount, role);

    }

    public static void verifyUser(Member member, Member moderator, String username, String graphsURL) {
        verifyUser(member, moderator, username, graphsURL, "");
    }

    public static void verifyUser(Member member, Member moderator, String username, String graphsURL, String moduleURL) {
        String userName = member.getUser().getName();
        String nickName = username;

        Guild guild = member.getGuild();

        Goldilocks.TIMER.schedule(() -> GraveyardScraper.graveyardScrape(userName), 0L, TimeUnit.SECONDS);

        String errors = "";
        Role verified = Goldilocks.jda.getRoleById(Database.getGuildInfo(member.getGuild(),"verifiedRole"));

        if (nickName.equals(userName)) {
            if ((java.lang.Character.isUpperCase(userName.charAt(0)))) nickName = nickName.substring(0, 1).toLowerCase() + nickName.substring(1);
            else nickName = nickName.substring(0, 1).toUpperCase() + nickName.substring(1);
        }
        String finalNickName = nickName;
        member.modifyNickname(nickName).queue(aVoid -> System.out.println(finalNickName + " set for " + member));
        member.modifyNickname(nickName).queueAfter(15L, TimeUnit.SECONDS,aVoid -> System.out.println(finalNickName + " re-set for " + member));
        if (verified != null) {
            guild.addRoleToMember(member, verified).queue(aVoid -> System.out.println("Verified " + member));
            guild.addRoleToMember(member, verified).queueAfter(15L, TimeUnit.SECONDS, aVoid -> System.out.println("Readded role to " + member));

        } else errors += "Verified Role is invalid, please update the id via setup.\n";

        GoogleSheets.logEvent(member.getGuild(), GoogleSheets.SheetsLogType.VERIFICATIONS, moderator.getEffectiveName(), moderator.getId(), member.getEffectiveName(), member.getId(), "Accepted");
        if (!moderator.getUser().equals(Goldilocks.jda.getSelfUser())) {
            Database.incrementField(moderator, "quotaVerifications", "totalVerifications");
            Database.logEvent(moderator, Database.EventType.VERIFICATION, System.currentTimeMillis() / 1000, null, "veripending");
        }

        logVerification(member, moderator, username, errors, graphsURL, moduleURL);

    }

    public static void logVerification(Member member, Member moderator, String username, String errors, String graphsURL) {
        logVerification(member, moderator, username, errors, graphsURL, "");
    }

    public static void logVerification(Member member, Member moderator, String username, String errors, String graphsURL, String moduleURL) {
        String logId = SetupConnector.getFieldValue(member.getGuild(), "guildLogs", (moderator.equals(member.getGuild().getSelfMember()) || !moduleURL.isEmpty()) ? "verificationLogChannelId" : "modLogChannelId");
        if (!logId.equals("0")) {
            TextChannel logChannel = Goldilocks.jda.getTextChannelById(logId);
            assert logChannel != null;
            if (errors.isEmpty()) logChannel.sendMessage(successEmbed(member, moderator, username, graphsURL, moduleURL).build()).queue();
            else logChannel.sendMessage(errorEmbed(member, moderator, username, errors).build()).queue();
        }
    }

    public static void logVerificationDenial(Member member, Member moderator, String username, String problems) {
        logVerification(member, moderator, username, problems, "");
    }

    public static void logVerificationDenial(Member member, Member moderator, String username, String problems, String moduleURL) {
        String logId = SetupConnector.getFieldValue(member.getGuild(), "guildLogs", "verificationLogChannelId");
        if (!logId.equals("0")) {
            TextChannel logChannel = Goldilocks.jda.getTextChannelById(logId);
            assert logChannel != null;
            logChannel.sendMessage(denialEmbed(member, moderator, username, problems, moduleURL).build()).queue();
        }
    }

    public static EmbedBuilder successEmbed(Member member, Member moderator, String username, String graphsURL, String moduleURL) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.setAuthor(member.getUser().getAsTag() + " has Successfully Verified Under " + username, "https://realmeye.com/player/" + username, member.getUser().getAvatarUrl())
                .setColor(Goldilocks.GREEN)
                .addField("User Information", member.getAsMention() + " | `" + member.getId() + "`", true)
                .addField("Moderator Information", moderator.getAsMention() + " | `" + moderator.getId() + "`", true)
                .setTimestamp(new Date().toInstant());

        if (!graphsURL.isEmpty()) embedBuilder.setImage(graphsURL);
        if (!moduleURL.isEmpty()) embedBuilder.setDescription("[Module Link](" + moduleURL + ")");
        return embedBuilder;
    }

    public static EmbedBuilder errorEmbed(Member member, Member moderator, String username, String error) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(member.getUser().getAsTag() + " had an Error While Verifying Under " + username, "https://realmeye.com/player/" + username, member.getUser().getAvatarUrl())
                .setColor(Goldilocks.RED)
                .addField("User Information", member.getAsMention() + " | `" + member.getId() + "`", true)
                .addField("Moderator Information", moderator.getAsMention() + " | `" + moderator.getId() + "`", true)
                .addField("Error:", "```\n" + error + "\n```", false)
                .setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    public static EmbedBuilder denialEmbed(Member member, Member moderator, String username, String problems, String moduleURL) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(member.getUser().getAsTag() + " was Denied Verification Under the Name: " + username, "https://realmeye.com/player/" + username, member.getUser().getAvatarUrl())
                .setColor(Goldilocks.RED)

                .addField("User Information", member.getAsMention() + " | `" + member.getId() + "`", true)
                .addField("Moderator Information", moderator.getAsMention() + " | `" + moderator.getId() + "`", true)
                .addField("Problems:", "```\n" + problems + "\n```", false)
                .setTimestamp(new Date().toInstant());

        if (!moduleURL.isEmpty()) embedBuilder.setDescription("[Module Link](" + moduleURL + ")");
        return embedBuilder;
    }

    public static void logVerification(boolean success, Member member, Guild guild, int runCount, Role role) {

        TextChannel textChannel;
        try {
            textChannel = guild.getTextChannelsByName("verification-logs", true).get(0);
        } catch (Exception e) {
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(role.getName() + " Verification " + (success ? "Success" : "Failed") + " for " + guild.getName())
                .setThumbnail(member.getUser().getAvatarUrl());
        if (!success) embedBuilder.setColor(Color.RED); else embedBuilder.setColor(Color.GREEN);
        embedBuilder.addField("Player Name:", member.getEffectiveName().split(" ")[0], false)
                .addField("Runs Completed: ", "`" + runCount + "`", false)
                .addField("Discord Tag:", member.getAsMention(), false)
                .addField("RealmEye Link:", "https://www.realmeye.com/player/" + member.getEffectiveName().split(" ")[0], false);
        embedBuilder.setFooter("Date: " + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));

        textChannel.sendMessage(embedBuilder.build()).complete().editMessage(embedBuilder.build()).queueAfter(10L, TimeUnit.SECONDS);

    }

    public static String getGraphsURL(String username, List<List<LogField>> history, GraveyardSummary gs) {
        String URL = "";
        try {
            String chartDay = Charts.createFameChart(history.get(0)).getShortUrl();
            String chartWeek = Charts.createFameChart(history.get(1)).getShortUrl();
            String chartAlltime = Charts.createFameChart(history.get(2)).getShortUrl();
            String dungeonChart = Charts.createDungeonChart(gs).getShortUrl();

            Map chartDayMap = Goldilocks.cloudinary.uploader().upload(chartDay, ObjectUtils.asMap("public_id", "charts/" + username + "Day"));
            Map chartWeekMap = Goldilocks.cloudinary.uploader().upload(chartWeek, ObjectUtils.asMap("public_id", "charts/" + username + "Week"));
            Map chartAlltimeMap = Goldilocks.cloudinary.uploader().upload(chartAlltime, ObjectUtils.asMap("public_id", "charts/" + username + "Alltime"));
            Map chartDungeonMap = Goldilocks.cloudinary.uploader().upload(dungeonChart, ObjectUtils.asMap("public_id", "charts/" + username + "Dungeon"));

            URL = Goldilocks.cloudinary.url().transformation(new Transformation()
                    .width(400).height(300).crop("fill").chain()
                    .overlay(new Layer().publicId(chartDayMap.get("public_id").toString())).width(400).height(300).x(400).crop("fill").chain()
                    .overlay(new Layer().publicId(chartWeekMap.get("public_id").toString())).width(400).height(300).y(300).x(-200).crop("fill").chain()
                    .overlay(new Layer().publicId(chartAlltimeMap.get("public_id").toString())).width(400).height(300).y(150).x(200).crop("fill")).imageTag(chartDungeonMap.get("public_id").toString()).split("'")[1];
        } catch (Exception e) {}

        return URL;
    }

}
