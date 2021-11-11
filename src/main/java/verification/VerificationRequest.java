package verification;

import lombok.Getter;
import lombok.Setter;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import org.apache.commons.lang3.StringUtils;
import setup.SetupConnector;
import utils.Utils;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static main.Goldilocks.eventWaiter;

@Getter
@Setter
public class VerificationRequest {

    private Member member;
    public User user;
    private Guild guild;
    private String veriCode;
    private TextChannel verificationChannel;
    private String playerName = "";
    private Message initialMessage = null;
    private Message nameRequestMessage = null;
    private Boolean confirmedCode = false;

    private TextChannel activeVeriChannel = null;

    public long timeEnding;

    public VerificationRequest() {
    }

    public VerificationRequest(Member member, String veriCode, TextChannel verificationChannel) {

        timeEnding = System.currentTimeMillis() + VerificationHub.MAX_MILLIS;

        this.member = member;
        this.user = member.getUser();
        this.guild = member.getGuild();
        this.veriCode = veriCode;
        this.verificationChannel = verificationChannel;

        createActiveVeriChannel();
        initializeGeneralVerification();

    }

    public VerificationRequest(Member member, String veriCode, TextChannel verificationChannel, Message initialMessage, Message nameRequestMessage, TextChannel activeVeriChannel) {

        timeEnding = System.currentTimeMillis() + VerificationHub.MAX_MILLIS;

        this.member = member;
        this.user = member.getUser();
        this.veriCode = veriCode;
        this.verificationChannel = verificationChannel;
        this.guild = member.getGuild();
        this.initialMessage = initialMessage;
        this.nameRequestMessage = nameRequestMessage;
        this.activeVeriChannel = activeVeriChannel;

        member.getUser().openPrivateChannel().complete().sendMessage("Unfortunately the bot was restarted during your verification. Your verification process has now restarted.").queue();
        initializeGeneralVerification();
        if (activeVeriChannel != null) activeVeriChannel.sendMessage("The bot was restarted during this verification. It was successfully retrieved from cache").queue();

    }

    public void initializeGeneralVerification() {
        try {
            initialMessage = user.openPrivateChannel().complete().sendMessage(initialMessageEmbed().build()).complete();
            if (activeVeriChannel != null) activeVeriChannel.sendMessage(initialMessageEmbed().build()).complete();
        } catch (Exception e) {
            logVerification(false, e.getLocalizedMessage());
            return;
        }
        initialMessage.addReaction("✅").queue();
    }

    public void sendTyping() {
        if (activeVeriChannel != null) activeVeriChannel.sendTyping().queue();
    }

    public void updateMessageTime() {
        try {
            if (initialMessage != null) {
                initialMessage.editMessage(initialMessageEmbed().build()).complete();
            }
            if (nameRequestMessage != null) {
                if (nameRequestMessage.getEmbeds().get(0).getFields().size() == 1) {
                    nameRequestMessage.editMessage(nameMessageEmbed().build()).complete();
                } else if (nameRequestMessage.getEmbeds().get(0).getFields().size() == 3) {
                    nameRequestMessage.editMessage(finalStepEmbed().build()).complete();
                }
            }
        } catch (Exception e) {}

    }

    public void createActiveVeriChannel() {
        Category veriCategory = Database.getActiveVerificationCategory(guild);
        if (veriCategory != null) {
            activeVeriChannel = veriCategory.createTextChannel(member.getUser().getAsTag().replace("#","-"))
                    .setTopic("Do not edit this message. | U: " + member.getId() + " | V: " + veriCode + " | P: "
                            + playerName + " | 1: " + (initialMessage == null ? "N/A" : initialMessage.getId()) + " | 2: " +
                            (nameRequestMessage == null ? "N/A" : nameRequestMessage.getId()) + " | T: " + verificationChannel.getId())
                    .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL)).complete();
            activeVeriChannel.getManager().sync().queue();
            activeVeriChannel.sendMessage(member.getAsMention() + " has begun the verification process.").queue();
        } else {
            activeVeriChannel = null;
        }
    }

    public void requestPlayerName() {
        if (!confirmedCode) {
            try {
                initialMessage.delete().queue();
            } catch (Exception e) {}
            initialMessage = null;
            if (activeVeriChannel != null) {
                activeVeriChannel.sendMessage(member.getAsMention() + " reacted to ✅ with " + formatTime() + " left.").queue();
                updateTopic();
            }
        }
        confirmedCode = true;

        if (activeVeriChannel != null) {
            activeVeriChannel.sendMessage(nameMessageEmbed().build()).complete();
            updateTopic();
        }
        nameRequestMessage = user.openPrivateChannel().complete().sendMessage(nameMessageEmbed().build()).complete();
    }

    public void confirmPlayerName(String userName) {

        playerName = userName;
        if (activeVeriChannel != null) {
            activeVeriChannel.sendMessage(member.getAsMention() + " requested their name to be set as `" + playerName + "` with " + formatTime() + " left.").queue();
            updateTopic();
        }

        try {
            nameRequestMessage.delete().queue();
            nameRequestMessage = null;
        } catch (NullPointerException e) {nameRequestMessage = null;}

        nameRequestMessage = user.openPrivateChannel().complete().sendMessage(finalStepEmbed().build()).complete();
        nameRequestMessage.addReaction("✅").queue();
        nameRequestMessage.addReaction("❌").queue();
        if (activeVeriChannel != null) activeVeriChannel.sendMessage(finalStepEmbed().build()).complete();

        VerificationHub.VerificationThread.schedule(() -> {
            eventWaiter.waitForEvent(PrivateMessageReactionAddEvent.class, e -> {
                return e.getUser().equals(user) && (e.getReactionEmote().getEmoji().equals("✅") || e.getReactionEmote().getEmoji().equals("❌"));
            }, e -> {

                if (e.getReactionEmote().getEmoji().equals("✅")) {
                    try {
                        nameRequestMessage.delete().queue();
                    } catch (Exception e1) {}
                    nameRequestMessage = user.openPrivateChannel().complete().sendMessage(finalStepEmbed().clearFields()
                            .setDescription("```\nCurrently retrieving data from your Realmeye\n```").setFooter("This can take up to 10 seconds to complete").build()).complete();
                    if (activeVeriChannel != null) {
                        activeVeriChannel.sendMessage(member.getAsMention() + " reacted to ✅ with " + formatTime() + " left.").queue();
                        try { updateTopic();} catch (Exception e2) {}
                    }
                    verifyUser();
                }

                if (e.getReactionEmote().getEmoji().equals("❌")) {
                    if (activeVeriChannel != null) {
                        activeVeriChannel.sendMessage(member.getAsMention() + " requested to re-enter their name with " + formatTime() + " left.").queue();
                        updateTopic();
                    }
                    nameRequestMessage.delete();
                    nameRequestMessage = null;
                    requestPlayerName();
                }

            }, 15, TimeUnit.MINUTES, () -> {
                Utils.errorMessage("Verification failed for " + userName, "User took too long to submit response", nameRequestMessage, 10L);
            });
        }, 0L, TimeUnit.SECONDS);

    }

    public void verifyUser()  {

        String error = "";

        List<Integer> guildReqs = Database.getVerificationReqs(verificationChannel.getId());
        int starRequirement = guildReqs.get(0);
        int fameRequirement = guildReqs.get(1);
        int statsRequirement = guildReqs.get(2);

        //RealmPlayer realmPlayer = null;
        PlayerProfile playerProfile;

        try {
            //realmPlayer = VerificationHub.getRealmPlayer(playerName);
            playerProfile = new PlayerProfile(playerName);
        } catch ( PlayerProfile.PrivateProfileException e ) {
            error = "Player's RealmEye is not public.";
            verificationError(error);
            return;
        }

        if (playerProfile == null) {
            error = "Could not get realmeye information.\n" +
                    "Please make sure your realmeye name is correct. If issues persist contact the " + guild.getName() + " mod team.";
            verificationError(error);
            return;
        }
        //Check fame
        if (playerProfile.getAliveFame() < fameRequirement) {
            error = "Player does not meet the required " + fameRequirement + " alive fame.";
            verificationError(error);
            return;
        }

        //Check stars
        if (playerProfile.getStars() < starRequirement) {
            error = "Player does not meet the required " + starRequirement + " class stars.";
            verificationError(error);
            return;
        }

        //Check player chars
        /*
        List<Character> characters = realmPlayer.getCharacters();
        boolean meetsStatsReq = false;
        for (Character character : characters) {
            if (character.getStats_maxed() >= statsRequirement) {
                meetsStatsReq = true;
                break;
            }

        }

        if (!meetsStatsReq) {
            error = "Player does not have the classes required to verify. " + "One " + statsRequirement + "/8";
            verificationError(error);
            return;
        }
         */


        if (!playerProfile.isHiddenLocation()) {
            error = "Player's location is not set to private.";
            verificationError(error);
            return;
        }

        String verifiedRoleId = SetupConnector.getFieldValue(member.getGuild(), "guildInfo","verifiedRole");
        Role verifiedRole = guild.getRoleById(verifiedRoleId);

        String description = playerProfile.getDescription();
        if (StringUtils.containsIgnoreCase(description, veriCode)) {
            try {
                if (!playerName.equals(member.getEffectiveName())) {
                    member.modifyNickname(playerName).queue();
                } else if (!playerName.toLowerCase().equals(member.getEffectiveName())){
                    member.modifyNickname(member.getEffectiveName().toLowerCase()).queue();
                } else {
                    member.modifyNickname(StringUtils.capitalize(member.getEffectiveName().toLowerCase())).queue();
                }
                System.out.println("Adding role to " + member.getEffectiveName());
                guild.addRoleToMember(member, verifiedRole).queue(confirm -> System.out.println("Verified " + member.getEffectiveName()));
                VerificationHub.generalVerifications.remove(member);

                verificationSuccess();
            } catch (Exception e) {
                verificationError(e.getLocalizedMessage());
            }
            return;
        } else {
            error = "Player's description does not have the vericode.";
            verificationError(error);
            return;
        }

    }

    public void verificationSuccess() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Congratulations you Have Been Successfully Verified for " + guild.getName() + "!");
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("Please make sure to read the server rules and happy raiding!");
        embedBuilder.setTimestamp(new Date().toInstant());

        VerificationHub.generalVerifications.remove(user);
        nameRequestMessage.editMessage(embedBuilder.build()).queue();
        if (activeVeriChannel != null) activeVeriChannel.sendMessage(member.getAsMention() + " was successfully verified with " + formatTime() + " left!").queue();
        if (activeVeriChannel != null) activeVeriChannel.delete().queueAfter(5L, TimeUnit.SECONDS);
        logVerification(true, "");

    }

    public void verificationError(String error) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Verification Failed for " + guild.getName())
                .setThumbnail(guild.getIconUrl())
                .setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("```\nError: " + error + "\n```");
        embedBuilder.setFooter("This message will delete in 30 seconds.");

        try {
            nameRequestMessage.delete().queue();
            nameRequestMessage = null;
        } catch (Exception e) {
            nameRequestMessage = null;
        }

        try {
            user.openPrivateChannel().complete().sendMessage(embedBuilder.build()).complete().delete().submitAfter(30L, TimeUnit.SECONDS);
        } catch (Exception e) { }
        if (activeVeriChannel != null) activeVeriChannel.sendMessage(embedBuilder.setFooter("This channel will delete in 30 seconds").build()).queue();
        if (activeVeriChannel != null) activeVeriChannel.delete().queueAfter(30L, TimeUnit.SECONDS);
        if (!error.equals("Your verification has timed out.")) logVerification(false, error);

        VerificationHub.generalVerifications.remove(user);

    }

    public void logVerification(boolean success, String error) {

        TextChannel textChannel;
        try {
            textChannel = guild.getTextChannelsByName("verification-logs", true).get(0);
        } catch (Exception e) {
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Verification " + (success ? "Success" : "Failed") + " for " + guild.getName())
                .setThumbnail(user.getAvatarUrl());
        if (!success) embedBuilder.setColor(Color.RED); else embedBuilder.setColor(Color.GREEN);
        embedBuilder.addField("Player Name:", playerName, false)
                .addField("Discord Tag", member.getAsMention(), false)
                .addField("RealmEye Link:", "https://www.realmeye.com/player/" + playerName, false);

        try {
            PlayerProfile playerProfile = new PlayerProfile(playerName);
            DecimalFormat df = new DecimalFormat("0.00");
            double mainPercentage = 0.0;
            double altPercentage = 0.0;
            try {
                //mainPercentage = Math.abs(UserDataConnector.getPlayerPercentage(playerProfile, false)) * 100 - .01;
                //altPercentage = Math.abs(UserDataConnector.getPlayerPercentage(playerProfile, true)) * 100 + .01;
            } catch (Exception e) {}
            //embedBuilder.addField("Alt Prediction", "\n**Main Percentage Chance: **" + df.format(mainPercentage) + "%\n**Alt Chance: **" + df.format(altPercentage) + "%", false);
        } catch (Exception e) {
        }

        if (!success) {
            embedBuilder.addField("Error: ", error, false);
        }
        embedBuilder.setFooter("Date: " + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));

        textChannel.sendMessage(embedBuilder.build()).complete().editMessage(embedBuilder.build()).queueAfter(10L, TimeUnit.SECONDS);

    }

    private EmbedBuilder initialMessageEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Verification Request for " + guild.getName())
                .setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("Thank you for verifying with " + guild.getName() + "! You are just have a couple more steps to do before " +
                "you are fully verified.");
        embedBuilder.addField("__1. Your verification code: __", "```\n" + veriCode + "\n``` Please place this on any line of your RealmEye description bars." +
                "\n**After** you have done so please react with ✅ to continue the verification process.", false);
        embedBuilder.setFooter("Your verification will timeout in: " + formatTime());

        return embedBuilder;
    }

    private EmbedBuilder nameMessageEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Verification Request for " + guild.getName())
                .setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("You have confirmed that you put your Verification code: `" + veriCode + "` in" +
                " your RealmEye description. For the next step please type your in-game name from Realm of The Mad God" +
                " in this chat.");
        embedBuilder.addField("📝 Example", "```\nNashex\n```",false)
                .setFooter("Your verification will timeout in: " + formatTime());
        return embedBuilder;
    }

    private EmbedBuilder finalStepEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Verification Request for " + guild.getName())
                .setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("You have stated that your in-game name is: `" + playerName + "`" +
                ", if this is incorrect please react with ❌." +
                "\nNow we need to make sure that you have the correct privacy settings! For this step you can access" +
                " your RealmEye page privacy settings  [here](https://www.realmeye.com/settings-of/" + playerName + ").");
        embedBuilder.addField("__Public Profile__", "Please make sure that the field titled *Who can see my profile?* is set to **Everyone**", false);
        embedBuilder.addField("__Private Location__", "Please make sure that the field titled *Who can see my last known location?* is set to **Nobody**", false);
        embedBuilder.addField("__Final Step__", "Once you have completed everything above please react to this message with ✅", false);
        embedBuilder.setFooter("Your verification will timeout in: " + formatTime());

        return embedBuilder;
    }

    private String formatTime() {
        long totalSeconds = (timeEnding- System.currentTimeMillis()) / 1000;

        long mins = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        seconds = seconds % 10 != 0 ? (10 - (seconds % 10) + seconds) : seconds;
        if (seconds == 60L) {mins++; seconds = 0;}

        return (mins == 1 ? mins + " minute and " : mins + " minutes and ") + seconds + " seconds";

    }

    private void updateTopic() {
        try {
            activeVeriChannel.getManager().setTopic("Do not edit this message. | U: " + member.getId() + " | V: " + veriCode + " | P: "
                    + playerName + " | 1: " + (initialMessage == null ? "N/A" : initialMessage.getId()) + " | 2: " +
                    (nameRequestMessage == null ? "N/A" : nameRequestMessage.getId()) + " | T: " + verificationChannel.getId()).submit().exceptionally(throwable -> {
                System.out.println("Unable to Update Topic");
                return null;
            });
        } catch (Exception e) {}
    }

    public Boolean getConfirmedCode() {
        return confirmedCode;
    }

    public String getPlayerName() {
        return playerName;
    }
}
