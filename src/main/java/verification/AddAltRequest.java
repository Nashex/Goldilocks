package verification;

import com.cloudinary.utils.ObjectUtils;
import lombok.Getter;
import lombok.Setter;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import parsing.ImageOcr;
import setup.SetupConnector;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static main.Goldilocks.cloudinary;
import static main.Goldilocks.eventWaiter;

@Getter
@Setter
public class AddAltRequest {
    private Member member;
    private Guild guild;
    private Message message;
    private TextChannel verificationChannel;
    private String userName;
    private Message logMessage;
    private String imageURL;
    HashMap<String, Boolean> fields;

    private Message addAltControlPanel;

    public AddAltRequest(Member member) {
        this.addAltControlPanel = null;
        this.member = member;
        this.verificationChannel = Goldilocks.jda.getTextChannelById(Database.getVerificationChannel(member.getGuild().getId()));
        guild = member.getGuild();

        requestPlayerName();
    }

    public AddAltRequest(Member member, String userName, Message verificationControlPanel) {
        this.member = member;
        this.guild = member.getGuild();
        this.addAltControlPanel = verificationControlPanel;
        this.verificationChannel = verificationControlPanel.getTextChannel();
        this.userName = userName;
        this.imageURL = verificationControlPanel.getEmbeds().get(0).getImage().getUrl();
    }

    public void successfulAlt(Member verifier) {
        String priorUserName = member.getEffectiveName();

        member.getUser().getMutualGuilds()
                .stream().filter(guild1 -> Database.getGuildInfo(guild1, "rank").equals("3") && !Database.isPub(guild1))
                .collect(Collectors.toList()).forEach(guild1 -> {
            try {
                guild1.modifyNickname(guild1.getMember(member.getUser()), priorUserName + " | " + userName).queue();
                if (!guild1.equals(verifier.getGuild())) logAddAlt(guild1, true);
            } catch (Exception e) {}
        });
        System.out.println("Adding Alt to " + member.getEffectiveName());

        member.getUser().openPrivateChannel().complete().sendMessage("Your alt: `" + userName + "` was successfully added.").queue();
        addAltControlPanel.editMessage(messageLog(logAddAlt(verifier.getGuild(), true).getJumpUrl(), verifier, true).build()).queue();
        addAltControlPanel.clearReactions().queue();

    }

    public void failedVerification(Member verifier) {

        member.getUser().openPrivateChannel().complete().sendMessage("Unfortunately your alt account request for `" + userName + "` was denied. If you believe this to be an error, please contact a Security+.").queue();
        try {
            addAltControlPanel.editMessage(messageLog(logAddAlt(verifier.getGuild(), false).getJumpUrl(), verifier, false).build()).queue();
        } catch (Exception e) {}
        addAltControlPanel.clearReactions().queue();

    }

    public void requestPlayerName() {

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Alt Account Addition Request")
                .setThumbnail(guild.getIconUrl())
                .setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("You have requested to get an alt added for the following servers: " + String.join(", ", member.getUser().getMutualGuilds()
                .stream().filter(guild1 -> Database.getGuildInfo(guild1, "rank").equals("3")).map(g -> g.getName()).collect(Collectors.toList())) + ". " +
                "For the next step please type your **alt's** in-game name from __Realm of The Mad God__" +
                " in this chat.");
        embedBuilder.addField("📝 Example", "```\nNashex\n```",false);
        try {
            message = member.getUser().openPrivateChannel().complete().sendMessage(embedBuilder.build()).complete();
        } catch (Exception e) {}

        eventWaiter.waitForEvent(MessageReceivedEvent.class, e -> {
            return !e.getChannelType().isGuild() && e.getAuthor().equals(member.getUser()) && !e.getMessage().getContentRaw().toLowerCase().contains("alt");
        }, e -> {

            userName = e.getMessage().getContentRaw().replaceAll("[^A-z]", "");

            if (!member.getGuild().getMembersByEffectiveName(userName, true).isEmpty()) {
                member.getUser().openPrivateChannel().complete().sendMessage("It looks like there is already an account verified under this name, if you believe this to be an error" +
                        " contact a Security+").queue();
                VerificationHub.addAltRequests.remove(this);
                return;
            }

            message.delete().queue();
            requestPlayerScreenShot();

        }, 10L, TimeUnit.MINUTES, () -> message.delete().queue());

    }

    public void requestPlayerScreenShot() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Alt Account Addition Request")
                .setThumbnail(guild.getIconUrl())
                .setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("You have requested to get an alt added for the following servers: " + String.join(", ", member.getUser().getMutualGuilds()
                .stream().filter(guild1 -> Database.getGuildInfo(guild1, "rank").equals("3")).map(g -> g.getName()).collect(Collectors.toList())) + ". " +
                "For the next step please send a picture of your alt account, in its vault saying your discord tag: `" + member.getUser().getAsTag() + "`. **It is important that you DO NOT crop the image**, or you will be asked to send another." +
                "\n\n📝 Example");
        embedBuilder.setImage("https://res.cloudinary.com/nashex/image/upload/v1609224288/assets/exampleverify_kzb1ic.png");

        message = member.getUser().openPrivateChannel().complete().sendMessage(embedBuilder.build()).complete();

        eventWaiter.waitForEvent(MessageReceivedEvent.class, e -> {
            return !e.getChannelType().isGuild() && e.getAuthor().equals(member.getUser()) && e.getMessage().getAttachments().size() > 0;
        }, e -> {

            Message.Attachment attachment = e.getMessage().getAttachments().get(0);
            imageURL = attachment.getUrl();

            attachment.downloadToFile("vaultPictures/" + attachment.getFileName()).thenAccept(file -> {
                member.getUser().openPrivateChannel().complete().sendMessage("Thank you for your alt account request, this process may take up to 24 hours so please be patient.").queue();
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
                addAltControlPanel = verificationChannel.sendMessage("Alt Account Addition Request for: " + member.getEffectiveName()).complete();
                addAltControlPanel.editMessage(addAltRequestEmbed().build()).queue();
                addAltControlPanel.addReaction("✅").queue();
                addAltControlPanel.addReaction("❌").queue();
            });


        }, 10L, TimeUnit.MINUTES, () -> message.delete().queue());
    }

    public EmbedBuilder addAltRequestEmbed() {
        String problems = "";
        for (Map.Entry entrySet : fields.entrySet()) {
            problems += String.format("%-20s", entrySet.getKey()) + " | " + ((Boolean) entrySet.getValue() ? "✅" : "❌") + "\n";
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Alt Account Addition Request for: " + member.getEffectiveName());
        embedBuilder.setAuthor("Alt Account Addition Control Panel", member.getUser().getAvatarUrl(), member.getUser().getAvatarUrl());
        embedBuilder.setColor(Goldilocks.GOLD);
        embedBuilder.setDescription("Please make sure that you are confident that you want to add this alt to this user before going through with it.");
        embedBuilder.addField("Realmeye Link:", "**[Link](https://www.realmeye.com/player/" + userName + ")**",true);
        embedBuilder.addField("Alt Account Name:", userName, true);
        embedBuilder.addField("User Tag:", member.getAsMention(), true);
        embedBuilder.addField("Image Analysis", "```\n" + problems + "\n```", false);
        embedBuilder.addField("Would you like to add this alt to this user?", "Please react with ✅ if you would, otherwise react with ❌ to cancel the process." +
                "\n\n**Alt Account Image**", false);
        embedBuilder.setImage(imageURL);
        embedBuilder.setFooter(member.getId() + " : " + userName, guild.getIconUrl())
                .setTimestamp(new Date().toInstant());

        return embedBuilder;

    }

    private EmbedBuilder messageLog(String URL, Member verifier, boolean success) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(verifier.getEffectiveName() + (success ? " Successfully Added an Alt to " : " Denied an Alt Account To ")  + member.getUser().getName());
        embedBuilder.setDescription("**[Alt Account Message Link](" + URL + ")**");
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setFooter(guild.getName());
        embedBuilder.setTimestamp(new Date().toInstant());


        return embedBuilder;
    }

    private Message logAddAlt(Guild guild, boolean success) {

        TextChannel logChannel;
        String logChannelId = SetupConnector.getFieldValue(guild, "guildLogs","modLogChannelId");
        if (logChannelId.equals("0")) return null;
        try {
            logChannel = Goldilocks.jda.getTextChannelById(logChannelId);
        } catch (Exception e) {return null;}

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Alt Account Addition " + (success ? "Success" : "Denied") + " - " + member.getEffectiveName())
                .setColor(success ? Color.GREEN : Color.RED)
                .setThumbnail(member.getUser().getAvatarUrl())
                .addField("Realmeye Link:", "**[Link](https://www.realmeye.com/player/" + userName + ")**",true)
                .addField("Alt Account Name:", userName, true)
                .addField("User Tag:", member.getAsMention(), true)
                .setFooter("Date: " + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()))
                .setImage(imageURL);

        return logChannel.sendMessage(embedBuilder.build()).complete();
    }
}
