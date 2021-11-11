package setup;

import lombok.Getter;
import lombok.Setter;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import utils.Utils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static main.Goldilocks.eventWaiter;

public class VerificationSetup {

    private static Message currentMessage;

    public static void createVerificationSetup(Message message) {
        TextChannel textChannel = message.getTextChannel();

        Message verificationSetupMessage = textChannel.sendMessage(verificationSetupEmbed(message).build()).complete();
        verificationSetupMessage.addReaction("📤").queue();
        verificationSetupMessage.addReaction("🗳").queue();
        verificationSetupMessage.addReaction("❌").queue();

        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(verificationSetupMessage.getId()) && e.getUser().equals(message.getAuthor()) && (e.getReactionEmote().getEmoji().equals("📤") ||
                    e.getReactionEmote().getEmoji().equals("🗳") || e.getReactionEmote().getEmoji().equals("❌"));
        }, e -> {
            if (e.getReactionEmote().getEmoji().equals("📤")) {
                verificationSetupMessage.delete().queue();
                VerificationSection verificationSection = new VerificationSection(null,null,-1,-1,-1);
                Message controlPanel = textChannel.sendMessage(verificationSetupEmbed(message).build()).complete();
                promptForSectionCreation(message, controlPanel, verificationSection);
            }
            if (e.getReactionEmote().getEmoji().equals("🗳")) {
                verificationSetupMessage.delete().queue();
                DungeonVerificationSection dungeonVerificationSection = new DungeonVerificationSection(null,null,-1);
                Message controlPanel = textChannel.sendMessage(verificationSetupEmbed(message).build()).complete();
                promptForDungeonSectionCreation(message, controlPanel, dungeonVerificationSection);
            }
            if (e.getReactionEmote().getEmoji().equals("❌")) {
                verificationSetupMessage.delete().queue();
                message.delete().queue();
            }
        }, 2L, TimeUnit.MINUTES, () -> {verificationSetupMessage.delete().queue();});

    }

    public static EmbedBuilder verificationSetupEmbed(Message message) {
        Guild guild = message.getGuild();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Verification Configuration Control Panel");
        embedBuilder.setDescription("Below is a brief description of the verification sections for " + guild.getName());
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setFooter("React with ❌ at anytime to close this menu");

        List<String[]> verificationSectionInfo = Database.getGuildVerificationSections(message.getGuild().getId());

        if (verificationSectionInfo.isEmpty()) {
            embedBuilder.addField("You currently have no verification sections", "Please create a verification section by reacting to 📤", false);
            return embedBuilder;
        }

        for (String[] verificationSection : verificationSectionInfo) {

            String verificationSectionSettings = "Verification Channel: " + guild.getTextChannelById(verificationSection[0]).getAsMention() +
                    "\nSection Verified Role: " + guild.getRoleById(verificationSection[1]).getAsMention() +
                    "\nStar Requirement: `" + verificationSection[2] + " Stars`" +
                    "\nFame Requirement: `" + verificationSection[3] + " Alive Fame`" +
                    "\nStats Requirement: `One " + verificationSection[4] + "/8 Character`";

            embedBuilder.addField("Section Name: " + guild.getRoleById(verificationSection[1]).getName() + " Verification", verificationSectionSettings ,false);
        }

        return embedBuilder;
    }

    public static void promptForSectionCreation(Message message, Message controlPanel, VerificationSection verificationSection) {
        controlPanel.editMessage(verificationSectionEmbed(verificationSection).build()).queue();
        controlPanel.addReaction("1️⃣").queue();
        controlPanel.addReaction("2️⃣").queue();
        controlPanel.addReaction("3️⃣").queue();
        controlPanel.addReaction("4️⃣").queue();
        controlPanel.addReaction("5️⃣").queue();
        controlPanel.addReaction("❌").queue();

        if (verificationSection.getVerificationChannel() != null && verificationSection.getVerifiedRole() != null && verificationSection.getStarRequirement() != -1
                && verificationSection.getFameRequirement() != -1 && verificationSection.getStatsRequirement() != -1) {
            controlPanel.editMessage(verificationSectionEmbed(verificationSection).setFooter("React with ❌ at anytime to cancel this process or 📥 to save this Section" ) .build()).queue();
            controlPanel.addReaction("📥").queue();
        }

        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(controlPanel.getId()) && e.getUser().equals(message.getAuthor()) && (e.getReactionEmote().getEmoji().equals("1️⃣") ||
                    e.getReactionEmote().getEmoji().equals("2️⃣") || e.getReactionEmote().getEmoji().equals("3️⃣") || e.getReactionEmote().getEmoji().equals("4️⃣") ||
                    e.getReactionEmote().getEmoji().equals("5️⃣") || e.getReactionEmote().getEmoji().equals("❌") || e.getReactionEmote().getEmoji().equals("📥"));
        }, e -> {
            if (e.getReactionEmote().getEmoji().equals("1️⃣")) {
                getVerificationChannel(message,controlPanel,verificationSection);
            }
            if (e.getReactionEmote().getEmoji().equals("2️⃣")) {
                getVerifiedRole(message, controlPanel,verificationSection);
            }
            if (e.getReactionEmote().getEmoji().equals("3️⃣")) {
                getVerificationRequirement(message,controlPanel,verificationSection, "Star Requirement");
            }
            if (e.getReactionEmote().getEmoji().equals("4️⃣")) {
                getVerificationRequirement(message,controlPanel,verificationSection, "Fame Requirement");
            }
            if (e.getReactionEmote().getEmoji().equals("5️⃣")) {
                getVerificationRequirement(message,controlPanel,verificationSection, "Stats Requirement");
            }
            if (e.getReactionEmote().getEmoji().equals("❌")) {
                controlPanel.delete().queue();
                message.delete().queue();
            }
            if (e.getReactionEmote().getEmoji().equals("📥")) {
                controlPanel.editMessage(verificationSectionEmbed(verificationSection).setTitle("Successfully Created Verification Section")
                        .setFooter("This verification section was created by " + message.getMember().getEffectiveName()).build()).queue();
                controlPanel.clearReactions().queue();
                Database.addVerificationSection(verificationSection.getVerifiedRole().getGuild().getId(), verificationSection.getVerificationChannel().getId(),
                        verificationSection.getVerifiedRole().getId(), verificationSection.getStarRequirement(), verificationSection.getFameRequirement(),
                        verificationSection.getStatsRequirement(), 0);
                message.delete().queue();
            }

        }, 2L, TimeUnit.MINUTES, () -> {controlPanel.delete().queue();});
    }

    public static void promptForDungeonSectionCreation(Message message, Message controlPanel, DungeonVerificationSection dungeonVerificationSection) {
        controlPanel.editMessage(dungeonVerificationSectionEmbed(dungeonVerificationSection).build()).queue();
        controlPanel.addReaction("1️⃣").queue();
        controlPanel.addReaction("2️⃣").queue();
        controlPanel.addReaction("3️⃣").queue();
        controlPanel.addReaction("❌").queue();

        if (dungeonVerificationSection.getVerificationChannel() != null && dungeonVerificationSection.getVerifiedRole() != null && dungeonVerificationSection.getRunsRequirement() != -1) {
            controlPanel.editMessage(dungeonVerificationSectionEmbed(dungeonVerificationSection).setFooter("React with ❌ at anytime to cancel this process or 📥 to save this Section" ) .build()).queue();
            controlPanel.addReaction("📥").queue();
        }

        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(controlPanel.getId()) && e.getUser().equals(message.getAuthor()) && (e.getReactionEmote().getEmoji().equals("1️⃣") ||
                    e.getReactionEmote().getEmoji().equals("2️⃣") || e.getReactionEmote().getEmoji().equals("3️⃣") || e.getReactionEmote().getEmoji().equals("4️⃣") ||
                    e.getReactionEmote().getEmoji().equals("5️⃣") || e.getReactionEmote().getEmoji().equals("❌") || e.getReactionEmote().getEmoji().equals("📥"));
        }, e -> {
            if (e.getReactionEmote().getEmoji().equals("1️⃣")) {
                getVerificationChannel(message,controlPanel, dungeonVerificationSection);
            }
            if (e.getReactionEmote().getEmoji().equals("2️⃣")) {
                getVerifiedRole(message, controlPanel,dungeonVerificationSection);
            }
            if (e.getReactionEmote().getEmoji().equals("3️⃣")) {
                getVerificationRunsRequirement(message,controlPanel,dungeonVerificationSection);
            }
            if (e.getReactionEmote().getEmoji().equals("❌")) {
                controlPanel.delete().queue();
                message.delete().queue();
            }
            if (e.getReactionEmote().getEmoji().equals("📥")) {
                controlPanel.editMessage(dungeonVerificationSectionEmbed(dungeonVerificationSection).setTitle("Successfully Created Verification Section")
                        .setFooter("This verification section was created by " + message.getMember().getEffectiveName()).build()).queue();
                controlPanel.clearReactions().queue();
                Database.addVerificationSection(dungeonVerificationSection.getVerifiedRole().getGuild().getId(), dungeonVerificationSection.getVerificationChannel().getId(),
                        dungeonVerificationSection.getVerifiedRole().getId(), -1, -1,
                        -1, dungeonVerificationSection.getRunsRequirement());
                message.delete().queue();
            }

        }, 2L, TimeUnit.MINUTES, () -> {controlPanel.delete().queue();});
    }

    public static EmbedBuilder verificationSectionEmbed(VerificationSection verificationSection) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Verification Section Creation");
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("Below is a brief description of the current verification section that you are creating" +
                "\n1️⃣ Verification Channel: " + (verificationSection.getVerificationChannel() != null ? verificationSection.getVerificationChannel().getAsMention() : "`None set`") +
                "\n2️⃣ Verified Role: " + (verificationSection.getVerifiedRole() != null ? verificationSection.getVerifiedRole().getAsMention() : "`None set`") +
                "\n3️⃣ Star Requirement: " + (verificationSection.getStarRequirement() != -1 ? "`" + verificationSection.getStarRequirement() + " Stars`" : "`None set`") +
                "\n4️⃣ Fame Requirement: " + (verificationSection.getFameRequirement() != -1 ? "`" + verificationSection.getFameRequirement() + " Alive Fame`" : "`None set`") +
                "\n5️⃣ Stats Requirement: " + (verificationSection.getStatsRequirement() != -1 ? "`One " + verificationSection.getStatsRequirement() + "/8 Character`" : "`None set`"));
        embedBuilder.setFooter("React with ❌ at anytime to cancel this process");
        return embedBuilder;
    }

    public static EmbedBuilder dungeonVerificationSectionEmbed(DungeonVerificationSection dungeonVerificationSection) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Verification Section Creation");
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("Below is a brief description of the current verification section that you are creating" +
                "\n1️⃣ Verification Channel: " + (dungeonVerificationSection.getVerificationChannel() != null ? dungeonVerificationSection.getVerificationChannel().getAsMention() : "`None set`") +
                "\n2️⃣ Verified Role: " + (dungeonVerificationSection.getVerifiedRole() != null ? dungeonVerificationSection.getVerifiedRole().getAsMention() : "`None set`") +
                "\n3️⃣ Run Requirement: " + (dungeonVerificationSection.getRunsRequirement() != -1 ? "`" + dungeonVerificationSection.getRunsRequirement() + " Runs`" : "`None set`"));
        embedBuilder.setFooter("React with ❌ at anytime to cancel this process");
        return embedBuilder;
    }

    public static void getVerifiedRole(Message message, Message controlPanel, VerificationSection verificationSection) {
        TextChannel textChannel = message.getTextChannel();
        currentMessage = textChannel.sendMessage(promptForItem("Verified Role").build()).complete();
        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getMessage().getAuthor().equals(message.getAuthor()) && !message.getGuild().getRoleById(e.getMessage().getContentRaw()).equals(null);
        }, e -> {
            try {
                message.getGuild().getRoleById(e.getMessage().getContentRaw());
                currentMessage.delete().queue();
                verificationSection.setVerifiedRole(message.getGuild().getRoleById(e.getMessage().getContentRaw()));
                e.getMessage().delete().queue();
                promptForSectionCreation(message, controlPanel,verificationSection);

            } catch (Exception error) {
                e.getMessage().delete().queue();
                Utils.errorMessage("Failed to Assign Verified Role","Invalid Role Id", currentMessage, 5L);
                getVerifiedRole(message, controlPanel, verificationSection);
            }

        }, 2L, TimeUnit.MINUTES,() -> {currentMessage.delete().queue();});
    }

    public static void getVerifiedRole(Message message, Message controlPanel, DungeonVerificationSection dungeonVerificationSection) {
        TextChannel textChannel = message.getTextChannel();
        currentMessage = textChannel.sendMessage(promptForItem("Verified Role").build()).complete();
        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getMessage().getAuthor().equals(message.getAuthor()) && !message.getGuild().getRoleById(e.getMessage().getContentRaw()).equals(null);
        }, e -> {
            try {
                message.getGuild().getRoleById(e.getMessage().getContentRaw());
                currentMessage.delete().queue();
                dungeonVerificationSection.setVerifiedRole(message.getGuild().getRoleById(e.getMessage().getContentRaw()));
                e.getMessage().delete().queue();
                promptForDungeonSectionCreation(message, controlPanel,dungeonVerificationSection);

            } catch (Exception error) {
                e.getMessage().delete().queue();
                Utils.errorMessage("Failed to Assign Verified Role","Invalid Role Id", currentMessage, 5L);
                getVerifiedRole(message, controlPanel, dungeonVerificationSection);
            }

        }, 2L, TimeUnit.MINUTES,() -> {currentMessage.delete().queue();});
    }


    public static void getVerificationChannel(Message message, Message controlPanel, VerificationSection verificationSection) {
        TextChannel textChannel = message.getTextChannel();
        currentMessage = textChannel.sendMessage(promptForItem("Verification Channel").build()).complete();
        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getMessage().getAuthor().equals(message.getAuthor()) && !message.getGuild().getTextChannelById(e.getMessage().getContentRaw()).equals(null);
        }, e -> {
            try {
                message.getGuild().getTextChannelById(e.getMessage().getContentRaw());
                currentMessage.delete().queue();
                verificationSection.setVerificationChannel(message.getGuild().getTextChannelById(e.getMessage().getContentRaw()));
                e.getMessage().delete().queue();
                promptForSectionCreation(message, controlPanel, verificationSection);

            } catch (Exception error) {
                e.getMessage().delete().queue();
                Utils.errorMessage("Failed to Assign Verification Channel","Invalid Channel Id", currentMessage, 5L);
                getVerificationChannel(message, controlPanel, verificationSection);
            }
        }, 2L, TimeUnit.MINUTES,() -> {currentMessage.delete().queue();});
    }

    public static void getVerificationChannel(Message message, Message controlPanel, DungeonVerificationSection dungeonVerificationSection) {
        TextChannel textChannel = message.getTextChannel();
        currentMessage = textChannel.sendMessage(promptForItem("Verification Channel").build()).complete();
        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getMessage().getAuthor().equals(message.getAuthor()) && !message.getGuild().getTextChannelById(e.getMessage().getContentRaw()).equals(null);
        }, e -> {
            try {
                message.getGuild().getTextChannelById(e.getMessage().getContentRaw());
                currentMessage.delete().queue();
                dungeonVerificationSection.setVerificationChannel(message.getGuild().getTextChannelById(e.getMessage().getContentRaw()));
                e.getMessage().delete().queue();
                promptForDungeonSectionCreation(message, controlPanel, dungeonVerificationSection);

            } catch (Exception error) {
                e.getMessage().delete().queue();
                Utils.errorMessage("Failed to Assign Verification Channel","Invalid Channel Id", currentMessage, 5L);
                getVerificationChannel(message, controlPanel, dungeonVerificationSection);
            }
        }, 2L, TimeUnit.MINUTES,() -> {currentMessage.delete().queue();});
    }

    public static void getVerificationRequirement(Message message, Message controlPanel, VerificationSection verificationSection, String verificationReqName) {
        TextChannel textChannel = message.getTextChannel();
        currentMessage = textChannel.sendMessage(promptForItem(verificationReqName).build()).complete();
        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getMessage().getAuthor().equals(message.getAuthor()) && Utils.isNumeric(e.getMessage().getContentRaw());
        }, e -> {

            int reqValue = Integer.parseInt(e.getMessage().getContentRaw());

            if (verificationReqName.equals("Star Requirement")) {
                if (reqValue >= 0 && reqValue <= 80) {
                    verificationSection.setStarRequirement(reqValue);
                    e.getMessage().delete().queue();
                    currentMessage.delete().queue();
                    promptForSectionCreation(message, controlPanel, verificationSection);
                    return;
                }
            }
            if (verificationReqName.equals("Fame Requirement")) {
                if (reqValue >= 0) {
                    verificationSection.setFameRequirement(reqValue);
                    e.getMessage().delete().queue();
                    currentMessage.delete().queue();
                    promptForSectionCreation(message, controlPanel, verificationSection);
                    return;
                }
            }
            if (verificationReqName.equals("Stats Requirement")) {
                if (reqValue >= 0 && reqValue <= 8) {
                    verificationSection.setStatsRequirement(reqValue);
                    e.getMessage().delete().queue();
                    currentMessage.delete().queue();
                    promptForSectionCreation(message, controlPanel, verificationSection);
                    return;
                }
            }

            e.getMessage().delete().queue();
            Utils.errorMessage("Failed to Assign Verification Requirement","Invalid Value for " + verificationReqName, currentMessage, 5L);
            getVerificationRequirement(message, controlPanel, verificationSection, verificationReqName);

        }, 2L, TimeUnit.MINUTES,() -> {currentMessage.delete().queue();});
    }

    public static void getVerificationRunsRequirement(Message message, Message controlPanel, DungeonVerificationSection dungeonVerificationSection) {
        TextChannel textChannel = message.getTextChannel();
        currentMessage = textChannel.sendMessage(promptForItem("Runs Requirement").build()).complete();
        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getMessage().getAuthor().equals(message.getAuthor()) && Utils.isNumeric(e.getMessage().getContentRaw());
        }, e -> {

            int reqValue = Integer.parseInt(e.getMessage().getContentRaw());

            if (reqValue >= 0 && reqValue <= 250) {
                dungeonVerificationSection.setRunsRequirement(reqValue);
                e.getMessage().delete().queue();
                currentMessage.delete().queue();
                promptForDungeonSectionCreation(message, controlPanel, dungeonVerificationSection);
                return;
            }

            e.getMessage().delete().queue();
            Utils.errorMessage("Failed to Assign Verification Requirement","Invalid Value for Runs Requirement", currentMessage, 5L);
            getVerificationRunsRequirement(message, controlPanel, dungeonVerificationSection);

        }, 2L, TimeUnit.MINUTES,() -> {currentMessage.delete().queue();});
    }


    public static EmbedBuilder promptForItem(String item) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Please Select a " + item);
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("Please enter the id or value for your " + item.toLowerCase() + "");
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    @Getter
    @Setter
    private static class VerificationSection {

        private Role verifiedRole;
        private TextChannel verificationChannel;
        private int starRequirement;
        private int fameRequirement;
        private int statsRequirement;

        public VerificationSection(Role verifiedRole, TextChannel verificationChannel, int starRequirement, int fameRequirement, int statsRequirement) {
            this.verifiedRole = verifiedRole;
            this.verificationChannel = verificationChannel;
            this.starRequirement = starRequirement;
            this.fameRequirement = fameRequirement;
            this.statsRequirement = statsRequirement;
        }
    }

    @Getter
    @Setter
    private static class DungeonVerificationSection {

        private Role verifiedRole;
        private TextChannel verificationChannel;
        private int runsRequirement;

        public DungeonVerificationSection(Role verifiedRole, TextChannel verificationChannel, int runsRequirement) {
            this.verifiedRole = verifiedRole;
            this.verificationChannel = verificationChannel;
            this.runsRequirement = runsRequirement;
        }
    }

}
