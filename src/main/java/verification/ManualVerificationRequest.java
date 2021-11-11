package verification;

import com.cloudinary.utils.ObjectUtils;
import lombok.Getter;
import lombok.Setter;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.apache.commons.lang3.StringUtils;
import parsing.ImageOcr;
import setup.SetupConnector;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static main.Goldilocks.cloudinary;
import static main.Goldilocks.eventWaiter;

@Getter
@Setter
public class ManualVerificationRequest extends VerificationRequest {

    private Member member;
    private Guild guild;
    private Message message;
    private TextChannel verificationChannel;
    private String userName;
    private Message logMessage;
    private String imageURL;
    HashMap<String, Boolean> fields = null;

    private Message verificationControlPanel;

    public ManualVerificationRequest(Member member) {
        this.verificationControlPanel = null;
        this.member = member;
        this.verificationChannel = Goldilocks.jda.getTextChannelById(Database.getVerificationChannel(member.getGuild().getId()));
        guild = member.getGuild();

        requestPlayerName();
    }

    public ManualVerificationRequest(String userName, Member member, Member verifier, String imageURL, TextChannel verificationChannel) {
        this.member = member;
        this.userName = userName;
        guild = member.getGuild();
        this.imageURL = imageURL;
        this.verificationChannel = verificationChannel;

        verificationControlPanel = verificationChannel.sendMessage("Manual Verification Request for: " + userName).complete();
        verificationControlPanel.editMessage(manualVerificationEmbed().build()).queue();
        verificationControlPanel.addReaction("✅").queue();
        verificationControlPanel.addReaction("❌").queue();

        eventWaiter.waitForEvent(MessageReactionAddEvent.class, e -> {
            return e.getReactionEmote().isEmoji() && ("✅❌").contains(e.getReactionEmote().getEmoji()) && e.getUser().equals(verifier.getUser());
        }, e -> {
            String emote = e.getReactionEmote().getEmoji();

            if (emote.equals("✅")) {
                successfulVerification(verifier);
            }

            if (emote.equals("❌")) {
                failedVerification(verifier);
            }

        }, 2L, TimeUnit.MINUTES, () -> {
            verificationControlPanel.delete().queue();
            VerificationHub.manualVerificationRequests.remove(this);
        });

    }

    public ManualVerificationRequest(Member member, String userName, Message verificationControlPanel) {
        this.member = member;
        this.guild = member.getGuild();
        this.verificationControlPanel = verificationControlPanel;
        this.verificationChannel = verificationControlPanel.getTextChannel();
        this.userName = userName;
        this.imageURL = verificationControlPanel.getEmbeds().get(0).getImage().getUrl();
    }

    public void successfulVerification(Member verifier) {

        String verifiedRoleId = SetupConnector.getFieldValue(member.getGuild(), "guildInfo","verifiedRole");
        Role verifiedRole = guild.getRoleById(verifiedRoleId);

        try {
            if (!userName.equals(member.getEffectiveName())) {
                member.modifyNickname(userName).queue(null, new ErrorHandler().ignore(ErrorResponse.MISSING_PERMISSIONS));
            } else if (!userName.toLowerCase().equals(member.getEffectiveName())){
                member.modifyNickname(member.getEffectiveName().toLowerCase()).queue();
            } else {
                member.modifyNickname(StringUtils.capitalize(member.getEffectiveName().toLowerCase())).queue();
            }
            System.out.println("Adding role to " + member.getEffectiveName());
            guild.addRoleToMember(member, verifiedRole).queue(confirm -> System.out.println("Verified " + member.getEffectiveName()));
            logVerification(verifier, true);
        } catch (Exception e) {
            //verificationError(e.getLocalizedMessage());
        }

        member.getUser().openPrivateChannel().complete().sendMessage("You were successfully verified for " + guild.getName() + " please make sure to read the server rules and happy raiding!").queue();
        verificationControlPanel.editMessage(messageLog(logMessage != null ? logMessage.getJumpUrl() : null, verifier, true).build()).queue();
        verificationControlPanel.clearReactions().queue();
        
    }

    public void failedVerification(Member verifier) {

        member.getUser().openPrivateChannel().complete().sendMessage("Unfortunately your verification for " + guild.getName() + " was denied. If you believe this to be an error, please contact a Security+.").queue();
        logVerification(verifier, false);
        verificationControlPanel.editMessage(messageLog(logMessage != null ? logMessage.getJumpUrl() : null, verifier, false).build()).queue();
        verificationControlPanel.clearReactions().queue();

    }

    @Override
    public void requestPlayerName() {

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Manual Verification Request for " + guild.getName())
                .setThumbnail(guild.getIconUrl())
                .setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("You have requested to obtain a manual verification for " + guild.getName() + ". " +
                "For the next step please type your in-game name from Realm of The Mad God" +
                " in this chat.");
        embedBuilder.addField("📝 Example", "```\nNashex\n```",false);
        try {
            message = member.getUser().openPrivateChannel().complete().sendMessage(embedBuilder.build()).complete();
        } catch (Exception e) {}

        eventWaiter.waitForEvent(MessageReceivedEvent.class, e -> {
            return !e.getChannelType().isGuild() && e.getAuthor().equals(member.getUser());
        }, e -> {

            userName = e.getMessage().getContentRaw().replaceAll("[^A-z]", "");
            message.delete().queue();
            requestPlayerScreenShot();

        }, 10L, TimeUnit.MINUTES, () -> message.delete().queue());

    }

    public void requestPlayerScreenShot() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Manual Verification Request for " + guild.getName())
                .setThumbnail(guild.getIconUrl())
                .setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("You have requested to obtain a manual verification for " + guild.getName() + ". " +
                "For the next step please send a picture of you, in your vault saying your discord tag. **It is important that you DO NOT crop the image**, or you will be asked to send another." +
                        (guild.getId().equals("799161165871316992") ? " Please make sure you have 4/5 in both life and mana and that you are standing on the forge with GARGLE in the chat bubble." : "") +
                "\n\n📝 Example");
        embedBuilder.setImage("https://res.cloudinary.com/nashex/image/upload/v1623304511/assets/samurai_exalted_verification_u0njda.png");

        message = member.getUser().openPrivateChannel().complete().sendMessage(embedBuilder.build()).complete();

        eventWaiter.waitForEvent(MessageReceivedEvent.class, e -> {
            return !e.getChannelType().isGuild() && e.getAuthor().equals(member.getUser()) && e.getMessage().getAttachments().size() > 0;
        }, e -> {

            Message.Attachment attachment = e.getMessage().getAttachments().get(0);
            imageURL = attachment.getUrl();

            attachment.downloadToFile("vaultPictures/" + attachment.getFileName()).thenAccept(file -> {
                member.getUser().openPrivateChannel().complete().sendMessage("Thank you for your manual verification request, verification may take up to 24 hours so please be patient.").queue();
                        try {
                            fields = ImageOcr.detectVaultPicture(file, userName, member.getUser().getAsTag());
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }).thenRun(() -> {
                File inputImage = new File("vaultPictures/" + attachment.getFileName());
                String newImageUrl = "";
                try {
                    Map params = ObjectUtils.asMap(
                            "public_id", "vaultPictures/" + member.getUser().getId(),
                            "overwrite", true,
                            "resource_type", "image"
                    );
                    Map imageDataMap =  cloudinary.uploader().upload(inputImage, params);
                    newImageUrl = (String) imageDataMap.get("url");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

                if (!newImageUrl.isEmpty()) this.imageURL = newImageUrl;


            }).thenRun(() -> {
                verificationControlPanel = verificationChannel.sendMessage("Manual Verification Request for: " + userName).complete();
                verificationControlPanel.editMessage(manualVerificationEmbed().build()).queue();
                verificationControlPanel.addReaction("✅").queue();
                verificationControlPanel.addReaction("❌").queue();
            });


        }, 10L, TimeUnit.MINUTES, () -> message.delete().queue());
    }

    public EmbedBuilder manualVerificationEmbed() {
        boolean publicProfile = false, stars = false, fame = false, graveyard = false, location = false, classReq = false;

        List<Integer> guildReqs = Database.getVerificationReqs(guild);
        int starRequirement = guildReqs.get(0);
        int fameRequirement = guildReqs.get(1);
        int statsRequirement = guildReqs.get(2);

        PlayerProfile playerProfile = null;

        try {
            playerProfile = new PlayerProfile(userName);
            publicProfile = true;
        } catch ( PlayerProfile.PrivateProfileException e ) {
        }

        if (publicProfile) {
            //Check fame
            if (playerProfile.getAliveFame() >= fameRequirement) {
                fame = true;
            }

            //Check stars
            if (playerProfile.getStars() >= starRequirement) {
                stars = true;
            }

            location = playerProfile.isHiddenLocation();
        }

        int requirements = (stars ? 1 : 0) + (publicProfile ? 1 : 0) + (fame ? 1 : 0) + (location ? 1 : 0);

        String problems = "";
        if (fields != null) {
            for (Map.Entry entrySet : fields.entrySet()) {
                problems += String.format("%-20s", entrySet.getKey()) + " | " + ((Boolean) entrySet.getValue() ? "✅" : "❌") + "\n";
            }
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Manual Verification for User: " + userName);
        embedBuilder.setAuthor("Manual Verification Control Panel", member.getUser().getAvatarUrl(), member.getUser().getAvatarUrl());
        embedBuilder.setColor(Goldilocks.GOLD);
        embedBuilder.setDescription("Please make sure that you are confident that you want to verify this user before going through with it.");
        embedBuilder.addField("Realmeye Link:", "**[Link](https://www.realmeye.com/player/" + userName + ")**",false);
        embedBuilder.addField("In-game Name:", userName, false);
        embedBuilder.addField("User Tag:", member.getAsMention(), false);
        embedBuilder.addField("Verification Requirements:", "```" +
                "\n" + String.format("%-20s", "Public RealmEye Page") + " | " + (publicProfile ? "✅" : "❌") +
                "\n" + String.format("%-20s", starRequirement + " Stars") + " | " + (stars ? "✅" : "❌") +
                (fameRequirement > 0 ? "\n" + String.format("%-20s", fameRequirement + " Alive Fame") + " | " + (fame ? "✅" : "❌") : "") +
                "\n" + String.format("%-20s", "Private Location") + " | " + (location ? "✅" : "❌") +
                "\n" +
                "\n" + String.format("%-20s", "Total") + " | " + requirements + "/4 Requirements" +
                "\n```" ,true);
        if (fields != null) embedBuilder.addField("Image Analysis", "```\n" + problems + "\n```", false);
        embedBuilder.addField("Would you like to verify this user?", "Please react with ✅ if you would, otherwise react with ❌ to cancel the verification." +
                "\n\n**Verification Image**", false);
        embedBuilder.setImage(imageURL);
        embedBuilder.setFooter(member.getId() + " - " + userName, guild.getIconUrl())
                .setTimestamp(new Date().toInstant());

        return embedBuilder;

    }

    private EmbedBuilder messageLog(String URL, Member verifier, boolean success) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(verifier.getEffectiveName() + (success ? " Successfully Verified " : " Denied Verification To ")  + member.getUser().getName());
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("**[Verification Message Link](" + URL + ")**");
        embedBuilder.setFooter(guild.getName());
        embedBuilder.setTimestamp(new Date().toInstant());


        return embedBuilder;
    }

    private void logVerification(Member verifier, boolean success) {

        TextChannel logChannel;
        try {
            logChannel = guild.getTextChannelsByName("verification-logs", true).get(0);
        } catch (Exception e) {
            try {
                logChannel = guild.getTextChannelsByName("verify-logs", true).get(0);
            } catch (Exception e2) {return;}
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Manual Verification " + (success ? "Success" : "Denied") + " - " + member.getEffectiveName());
        embedBuilder.setColor(success ? Color.GREEN : Color.RED);
        embedBuilder.setThumbnail(member.getUser().getAvatarUrl());
        embedBuilder.setDescription("**Player Name: **\n" + userName +
                "\n\n**Discord Tag:** \n" + member.getAsMention() +
                "\n\n**Verified by:** \n" + verifier.getAsMention() +
                "\n\n**Realmeye:** **[Link](" + "https://www.realmeye.com/player/" + userName + ")**" +
                "\n\n**Verification Image:**");
        embedBuilder.setFooter("Date: " + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
        embedBuilder.setImage(imageURL);

        logMessage = logChannel.sendMessage(embedBuilder.build()).complete();
    }

}
