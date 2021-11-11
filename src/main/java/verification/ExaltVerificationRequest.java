package verification;

import com.cloudinary.utils.ObjectUtils;
import lombok.Getter;
import lombok.Setter;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import parsing.ImageOcr;
import setup.SetupConnector;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static main.Goldilocks.cloudinary;
import static main.Goldilocks.eventWaiter;

@Getter
@Setter
public class ExaltVerificationRequest {
    private Member member;
    private Guild guild;
    private Message message;
    private TextChannel verificationChannel;
    private String userName;
    private Message logMessage;
    private String imageURL;
    private String problems;
    HashMap<String, Integer> fields;

    private Message vetVeriControlPanel;

    public ExaltVerificationRequest(Member member) {
        this.vetVeriControlPanel = null;
        this.member = member;
        this.verificationChannel = Goldilocks.jda.getTextChannelById(Database.getVerificationChannel(member.getGuild().getId()));
        guild = member.getGuild();

        this.userName = member.getEffectiveName().split(" ")[0].replaceAll("[^A-Za-z]", "");
        requestPlayerScreenShot();
    }

    public ExaltVerificationRequest(Member member, String userName, Message verificationControlPanel) {
        this.member = member;
        this.guild = member.getGuild();
        this.vetVeriControlPanel = verificationControlPanel;
        this.verificationChannel = verificationControlPanel.getTextChannel();
        this.userName = userName;
        this.imageURL = verificationControlPanel.getEmbeds().get(0).getImage().getUrl();
    }

    public void successfulExaltVerification(Member verifier) {
        Role veteranRole = Goldilocks.jda.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","veteranRole"));

        guild.addRoleToMember(member, veteranRole).queue();
        System.out.println("Veteran Verifying " + member.getEffectiveName());

        member.getUser().openPrivateChannel().complete().sendMessage("You were successfully veteran verified for " + guild.getName() + ". Please read the raiding rules and happy raiding!").queue();
        vetVeriControlPanel.editMessage(messageLog(logVetVerify(verifier.getGuild(), true).getJumpUrl(), verifier, true).build()).queue();
        vetVeriControlPanel.clearReactions().queue();

    }

    public void failedExaltVerification(Member verifier) {

        member.getUser().openPrivateChannel().complete().sendMessage("Unfortunately your veteran verification request for `" + userName + "` was denied. If you believe this to be an error, please contact a Security+.").queue();
        try {
            vetVeriControlPanel.editMessage(messageLog(logVetVerify(verifier.getGuild(), false).getJumpUrl(), verifier, false).build()).queue();
        } catch (Exception e) {}
        vetVeriControlPanel.clearReactions().queue();

    }

    public void requestPlayerScreenShot() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Veteran Verification Request for " + member.getGuild().getName())
                .setThumbnail(guild.getIconUrl())
                .setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("You have requested to get veteran verified for: " + member.getGuild().getName() + ". " +
                "For the next step please send a picture of your main account, in its vault hovering over your exaltations. **It is important that you DO NOT crop the image**, or you will be asked to send another. " +
                "\n**If the image does not match the following format you will be denied.**" +
                "\n\n📝 Example");
        embedBuilder.setImage("https://res.cloudinary.com/nashex/image/upload/v1615519532/vaultPictures/unknown_zhae4z.png");

        message = member.getUser().openPrivateChannel().complete().sendMessage(embedBuilder.build()).complete();

        eventWaiter.waitForEvent(MessageReceivedEvent.class, e -> {
            return !e.getChannelType().isGuild() && e.getAuthor().equals(member.getUser()) && e.getMessage().getAttachments().size() > 0;
        }, e -> {

            Message.Attachment attachment = e.getMessage().getAttachments().get(0);
            imageURL = attachment.getUrl();

            attachment.downloadToFile("vaultPictures/" + attachment.getFileName()).thenAccept(file -> {
                member.getUser().openPrivateChannel().complete().sendMessage("Thank you for your veteran verification request, this process may take up to 24 hours so please be patient.").queue();
                try {
                    fields = ImageOcr.getPlayerExalts(file.getAbsolutePath(), userName);
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }).thenRun(() -> {
                File inputImage = new File("vaultPictures/" + attachment.getFileName());
                String newImageUrl = "";

                try {
                    fields = ImageOcr.getPlayerExalts(inputImage.getAbsolutePath(), userName);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }

                String imageUrl = "";

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
                vetVeriControlPanel = verificationChannel.sendMessage("Veteran Verify Request for: " + member.getEffectiveName()).complete();
                vetVeriControlPanel.editMessage(exaltVerifyRequestEmbed().build()).queue();
                vetVeriControlPanel.addReaction("✅").queue();
                vetVeriControlPanel.addReaction("❌").queue();
            });


        }, 10L, TimeUnit.MINUTES, () -> message.delete().queue());
    }

    public EmbedBuilder exaltVerifyRequestEmbed() {
        problems = "";
        HashMap<String, Integer> reqs = Database.getVetVerificationReqs(guild);
        try {
            for (Map.Entry entrySet : fields.entrySet()) {
                if (!Arrays.asList(new String[]{"Username", "ExaltPointers", "VaultPointers"}).contains(entrySet.getKey())) {
                    problems += String.format("%-4s", entrySet.getKey()) + String.format("%-16s", " ("+ entrySet.getValue() + "/5)") + "| " + (reqs.get(entrySet.getKey()) <= (Integer) entrySet.getValue() ? "✅" : "❌") + "\n";
                    if (reqs.containsKey(entrySet.getKey())) reqs.remove(entrySet.getKey());
                }
            }
        } catch (Exception e) {e.printStackTrace();}
        problems += String.format("%-20s", "Username") + "| " + (fields.containsKey("Username") ? "✅" : "❌") + "\n";
        problems += String.format("%-20s", "Valid Exalt Page") + "| " + (fields.containsKey("ExaltPointers") ? "✅" : "❌") + "\n";
        problems += String.format("%-20s", "Vault Detected") + "| " + (fields.containsKey("VaultPointers") ? "✅" : "❌") + "\n";

        problems += "\nThe bot was unable to fully verify the following fields:\n";

        for (Map.Entry entrySet : reqs.entrySet()) {
            problems += String.format("%-4s", entrySet.getKey()) + String.format("%-16s", " (Not Found)") + "| ❔\n";
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Veteran Verification Request for: " + member.getEffectiveName());
        embedBuilder.setAuthor("Veteran Verification Control Panel", member.getUser().getAvatarUrl(), member.getUser().getAvatarUrl());
        embedBuilder.setColor(Goldilocks.GOLD);
        embedBuilder.setDescription("Please make sure that you are confident that you want to veteran verify this user before going through with it.");
        embedBuilder.addField("Realmeye Link:", "**[Link](https://www.realmeye.com/player/" + userName + ")**",true);
        embedBuilder.addField("User Name:", userName, true);
        embedBuilder.addField("User Tag:", member.getAsMention(), true);
        embedBuilder.addField("Image Analysis", "```\n" + problems + "\n```", false);
        embedBuilder.addField("Would you like to veteran verify this user?", "Please react with ✅ if you would, otherwise react with ❌ to cancel the process." +
                "\n\n**Exaltation Image**", false);
        embedBuilder.setImage(imageURL);
        embedBuilder.setFooter(member.getId() + " ~ " + userName, guild.getIconUrl())
                .setTimestamp(new Date().toInstant());

        return embedBuilder;

    }

    private EmbedBuilder messageLog(String URL, Member verifier, boolean success) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(verifier.getEffectiveName() + (success ? " Successfully Veteran Verified " : " Denied Veteran Verification to ")  + member.getUser().getName());
        embedBuilder.setDescription("**[Veteran Verification Message Link](" + URL + ")**");
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setFooter(guild.getName());
        embedBuilder.setTimestamp(new Date().toInstant());


        return embedBuilder;
    }

    private Message logVetVerify(Guild guild, boolean success) {

        TextChannel logChannel;
        String logChannelId = SetupConnector.getFieldValue(member.getGuild(), "guildLogs","verificationLogChannelId");
        if (logChannelId.equals("0")) return null;
        try {
            logChannel = Goldilocks.jda.getTextChannelById(logChannelId);
        } catch (Exception e) {return null;}

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Veteran Verification " + (success ? "Success" : "Denied") + " - " + member.getEffectiveName())
                .setColor(success ? Color.GREEN : Color.RED)
                .setThumbnail(member.getUser().getAvatarUrl())
                .addField("Realmeye Link:", "**[Link](https://www.realmeye.com/player/" + userName + ")**",true)
                .addField("User Name:", userName, true)
                .addField("User Tag:", member.getAsMention(), true)
                .addField("Image Analysis", "```\n" + problems + "\n```", false)
                .setFooter("Date: " + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()))
                .setImage(imageURL);

        return logChannel.sendMessage(embedBuilder.build()).complete();
    }
}
