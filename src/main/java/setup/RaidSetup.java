package setup;

import lombok.Getter;
import lombok.Setter;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import raids.Dungeon;
import raids.DungeonInfo;
import utils.Utils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static main.Goldilocks.eventWaiter;

public class RaidSetup {

    public static void createRaidSetup(Message message) {
        TextChannel textChannel = message.getTextChannel();

        Message raidSetupMessage = textChannel.sendMessage(raidSetupEmbed(message).build()).complete();
        raidSetupMessage.addReaction("📤").queue();
        raidSetupMessage.addReaction("❌").queue();

        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(raidSetupMessage.getId()) && e.getUser().equals(message.getAuthor()) && (e.getReactionEmote().getEmoji().equals("📤") ||
                    e.getReactionEmote().getEmoji().equals("❌"));
        }, e -> {
            if (e.getReactionEmote().getEmoji().equals("📤")) {
                raidSetupMessage.delete().queue();
                RaidCategory raidCategory = new RaidCategory(null,null,null,null,-1);
                Message controlPanel = textChannel.sendMessage(raidCategoryEmbed(raidCategory).build()).complete();
                promptForCategoryCreation(message, controlPanel, raidCategory);
            }
            if (e.getReactionEmote().getEmoji().equals("❌")) {
                raidSetupMessage.delete().queue();
                message.delete().queue();
            }
        }, 2L, TimeUnit.MINUTES, () -> {raidSetupMessage.delete().queue();});

    }

    public static EmbedBuilder raidSetupEmbed(Message message) {
        Guild guild = message.getGuild();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Raid Configuration Control Panel");
        embedBuilder.setDescription("Below is a brief description of the raiding sections for " + guild.getName());
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setFooter("React with ❌ at anytime to close this menu");

        List<String[]> raidCategoryInfo = Database.getGuildRaidCategories(message.getGuild().getId());

        if (raidCategoryInfo.isEmpty()) {
            embedBuilder.addField("You currently have no raiding sections", "Please create a raiding section by reacting to 📤", false);
            return embedBuilder;
        }

        for (String[] raidCategory : raidCategoryInfo) {

            String raidCategorySettings = "Section Raiding Role: " + guild.getRoleById(raidCategory[1]).getAsMention() +
                    "\nRaid Command Channel: " + guild.getTextChannelById(raidCategory[2]).getAsMention() +
                    "\nRaid Status Channel : " + guild.getTextChannelById(raidCategory[3]).getAsMention() +
                    "\nDefault Raid Type: " + (Integer.parseInt(raidCategory[4]) != -1 ? "`" + DungeonInfo.dungeonInfo(guild, Integer.parseInt(raidCategory[4])).dungeonInfo[3] + "`" : "`None`");

            embedBuilder.addField("Section Name: " + guild.getCategoryById(raidCategory[0]).getName(), raidCategorySettings ,false);
        }

        return embedBuilder;
    }

    public static void promptForCategoryCreation(Message message, Message controlPanel, RaidCategory raidCategory) {
        controlPanel.editMessage(raidCategoryEmbed(raidCategory).build()).queue();
        controlPanel.addReaction("1️⃣").queue();
        controlPanel.addReaction("2️⃣").queue();
        controlPanel.addReaction("3️⃣").queue();
        controlPanel.addReaction("4️⃣").queue();
        controlPanel.addReaction("5️⃣").queue();
        controlPanel.addReaction("❌").queue();

        if (raidCategory.getRaidCategory() != null && raidCategory.getRaiderRole() != null && raidCategory.getRaidCommands() != null
        && raidCategory.getRaidStatus() != null) {
            controlPanel.editMessage(raidCategoryEmbed(raidCategory).setFooter("React with ❌ at anytime to cancel this process or 📥 to save this Section" ) .build()).queue();
            controlPanel.addReaction("📥").queue();
        }

        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(controlPanel.getId()) && e.getUser().equals(message.getAuthor()) && (e.getReactionEmote().getEmoji().equals("1️⃣") ||
                    e.getReactionEmote().getEmoji().equals("2️⃣") || e.getReactionEmote().getEmoji().equals("3️⃣") || e.getReactionEmote().getEmoji().equals("4️⃣") ||
                    e.getReactionEmote().getEmoji().equals("5️⃣") || e.getReactionEmote().getEmoji().equals("❌") || e.getReactionEmote().getEmoji().equals("📥"));
        }, e -> {
            if (e.getReactionEmote().getEmoji().equals("1️⃣")) {
                getRaidCategory(message, controlPanel,raidCategory);
            }
            if (e.getReactionEmote().getEmoji().equals("2️⃣")) {
                getRaiderRole(message,controlPanel,raidCategory);
            }
            if (e.getReactionEmote().getEmoji().equals("3️⃣")) {
                getTextChannel(message,controlPanel,raidCategory, "Raid Commands Channel");
            }
            if (e.getReactionEmote().getEmoji().equals("4️⃣")) {
                getTextChannel(message,controlPanel,raidCategory, "Raid Status Channel");
            }
            if (e.getReactionEmote().getEmoji().equals("5️⃣")) {
                dungeonSelection(message, controlPanel, raidCategory);
            }
            if (e.getReactionEmote().getEmoji().equals("❌")) {
                controlPanel.delete().queue();
                message.delete().queue();
            }
            if (e.getReactionEmote().getEmoji().equals("📥")) {
                controlPanel.editMessage(raidCategoryEmbed(raidCategory).setTitle("Successfully Created Raiding Section")
                        .setFooter("This raiding section was created by " + message.getMember().getEffectiveName()).build()).queue();
                controlPanel.clearReactions().queue();
                Database.addRaidCategory(raidCategory.getRaidCategory().getGuild().getId(), raidCategory.getRaidCategory().getId(), raidCategory.getRaiderRole().getId(),
                        raidCategory.getRaidCommands().getId(), raidCategory.getRaidStatus().getId(), String.valueOf(raidCategory.getDefaultRaid()));
                message.delete().queue();
            }

        }, 2L, TimeUnit.MINUTES, () -> {controlPanel.delete().queue();});
    }

    public static EmbedBuilder raidCategoryEmbed(RaidCategory raidCategory) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Raiding Section Creation");
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("Below is a brief description of the current raiding category that you are creating" +
                "\n1️⃣ Raiding Category Name: " + (raidCategory.getRaidCategory() != null ? "`" + raidCategory.getRaidCategory().getName() + "`" : "`None set`") +
                "\n2️⃣ Category Raiding Role: " + (raidCategory.getRaiderRole() != null ? raidCategory.getRaiderRole().getAsMention() : "`None set`") +
                "\n3️⃣ Raid Commands Channel: " + (raidCategory.getRaidCommands() != null ? raidCategory.getRaidCommands().getAsMention() : "`None set`") +
                "\n4️⃣ Raid Status Channel : " + (raidCategory.getRaidStatus() != null ? raidCategory.getRaidStatus().getAsMention() : "`None set`") +
                "\n5️⃣ Default Raid Type: " + (raidCategory.getDefaultRaid() != -1 ? "`" + DungeonInfo.oldDungeonInfo()[raidCategory.getDefaultRaid()][3] + "`": "`None set`"));
        embedBuilder.setFooter("React with ❌ at anytime to cancel this process");
        return embedBuilder;
    }

    public static void getRaidCategory(Message message, Message controlPanel, RaidCategory raidCategory) {
        TextChannel textChannel = message.getTextChannel();
        Message currentMessage = textChannel.sendMessage(promptForItem("Raiding Category").build()).complete();
        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getMessage().getAuthor().equals(message.getAuthor());
        }, e -> {

            try {
                message.getGuild().getCategoryById(e.getMessage().getContentRaw());
                currentMessage.delete().queue();
                raidCategory.setRaidCategory(message.getGuild().getCategoryById(e.getMessage().getContentRaw()));
                e.getMessage().delete().queue();
                promptForCategoryCreation(message, controlPanel,raidCategory);
            } catch (Exception error) {
                e.getMessage().delete().queue();
                Utils.errorMessage("Failed to Assign Raiding Category","Invalid Category Id", currentMessage, 5L);
                getRaidCategory(message, controlPanel, raidCategory);
            }
        }, 2L, TimeUnit.MINUTES,() -> {currentMessage.delete().queue();});

    }

    public static void getRaiderRole(Message message, Message controlPanel, RaidCategory raidCategory) {
        TextChannel textChannel = message.getTextChannel();
        Message currentMessage = textChannel.sendMessage(promptForItem("Category Raiding Role").build()).complete();
        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getMessage().getAuthor().equals(message.getAuthor()) && !message.getGuild().getRoleById(e.getMessage().getContentRaw()).equals(null);
        }, e -> {
            try {
                message.getGuild().getRoleById(e.getMessage().getContentRaw());
                currentMessage.delete().queue();
                raidCategory.setRaiderRole(message.getGuild().getRoleById(e.getMessage().getContentRaw()));
                e.getMessage().delete().queue();
                promptForCategoryCreation(message, controlPanel,raidCategory);

            } catch (Exception error) {
                e.getMessage().delete().queue();
                Utils.errorMessage("Failed to Assign Raiding Role","Invalid Role Id", currentMessage, 5L);
                getRaiderRole(message, controlPanel, raidCategory);
            }

        }, 2L, TimeUnit.MINUTES,() -> {currentMessage.delete().queue();});
    }


    public static void getTextChannel(Message message, Message controlPanel, RaidCategory raidCategory, String channelName) {
        TextChannel textChannel = message.getTextChannel();
        Message currentMessage = textChannel.sendMessage(promptForItem(channelName).build()).complete();
        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getMessage().getAuthor().equals(message.getAuthor()) && !message.getGuild().getTextChannelById(e.getMessage().getContentRaw()).equals(null);
        }, e -> {
            try {
                message.getGuild().getTextChannelById(e.getMessage().getContentRaw());
                currentMessage.delete().queue();
                if (channelName.equals("Raid Commands Channel")) {
                    raidCategory.setRaidCommands(message.getGuild().getTextChannelById(e.getMessage().getContentRaw()));
                } else {
                    raidCategory.setRaidStatus(message.getGuild().getTextChannelById(e.getMessage().getContentRaw()));
                }
                e.getMessage().delete().queue();
                promptForCategoryCreation(message, controlPanel, raidCategory);

            } catch (Exception error) {
                e.getMessage().delete().queue();
                Utils.errorMessage("Failed to Assign " + channelName,"Invalid Channel Id", currentMessage, 5L);
                getTextChannel(message, controlPanel, raidCategory, channelName);
            }

        }, 2L, TimeUnit.MINUTES,() -> {currentMessage.delete().queue();});
    }

    public static EmbedBuilder promptForItem(String item) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Please Select a " + item);
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("Please enter the id for your " + item.toLowerCase() + "");
        embedBuilder.setTimestamp(new Date().toInstant());

        return embedBuilder;
    }

    public static void dungeonSelection(Message message, Message mainControlPanel, RaidCategory raidCategory) {
        List<Dungeon> dungeonInfo = DungeonInfo.nDungeonInfo(message.getGuild());
        TextChannel textChannel = message.getTextChannel();
        Message controlPanel = textChannel.sendMessage(dungeonSelectionScreen().build()).complete();

        eventWaiter.waitForEvent(MessageReceivedEvent.class, e -> {return e.getAuthor().equals(message.getAuthor()) && (Utils.isNumeric(e.getMessage().getContentRaw())
                || e.getMessage().getContentRaw().toLowerCase().equals("close") || e.getMessage().getContentRaw().toLowerCase().equals("all"));}, e -> {

            if (e.getMessage().getContentRaw().toLowerCase().equals("close")) {
                controlPanel.delete().queue();
                e.getMessage().delete().queue();
                return;
            }

            int choice = Integer.parseInt(e.getMessage().getContentRaw()) - 1;

            if (!(choice <= dungeonInfo.size() - 1)) {
                e.getMessage().delete().queue();
                Utils.errorMessage("Failed to Select Dungeon", "Dungeon type is invalid, please try again.", controlPanel, 5L);
                return;
            }
            e.getMessage().delete().queue();
            raidCategory.setDefaultRaid(choice);
            promptForCategoryCreation(message, mainControlPanel,raidCategory);
            controlPanel.delete().queue();

        }, 3L, TimeUnit.MINUTES, () -> {
            controlPanel.editMessage(dungeonSelectionScreen().clearFields().setDescription("```\n" +
                    "This Raid Selection Screen has been closed due to inactivity. \n```")
                    .setFooter("This message will delete in 10 seconds.").build()).queue();
            controlPanel.delete().submitAfter(10L, TimeUnit.SECONDS);
        });
    }

    public static EmbedBuilder dungeonSelectionScreen() {
        int i = 0;

        String[][] dungeonInfo = DungeonInfo.oldDungeonInfo();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Raid Type Selection");
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("Please select a type for your lobby manager by typing the number of the corresponding type below.");

        String legendaryDungeons = "";
        String courtDungeons = "";
        String epicDungeons = "";
        String highlandsDungeons = "";
        String randomDungeons = "";
        String randomLobbyTypes = "";
        for (i = i; i < 7; i++) {
            legendaryDungeons += "\n**" + (i + 1) + ".** " + Goldilocks.jda.getEmoteById(dungeonInfo[i][0]).getAsMention() + " " + dungeonInfo[i][3];
        }
        for (i = i; i < 12; i++) {
            courtDungeons += "\n**" + (i + 1) + ".** " + Goldilocks.jda.getEmoteById(dungeonInfo[i][0]).getAsMention() + " " + dungeonInfo[i][3];
        }
        for (i = i; i < 21; i++) {
            epicDungeons += "\n**" + (i + 1) + ".** " + Goldilocks.jda.getEmoteById(dungeonInfo[i][0]).getAsMention() + " " + dungeonInfo[i][3];
        }
        for (i = i; i < 31; i++) {
            highlandsDungeons += "\n**" + (i + 1) + ".** " + Goldilocks.jda.getEmoteById(dungeonInfo[i][0]).getAsMention() + " " + dungeonInfo[i][3];
        }
        for (i = i; i < 42; i++) {
            randomDungeons += "\n**" + (i + 1) + ".** " + Goldilocks.jda.getEmoteById(dungeonInfo[i][0]).getAsMention() + " " + dungeonInfo[i][3];
        }
        for (i = i; i < 47; i++) {
            randomLobbyTypes += "\n**" + (i + 1) + ".** " + Goldilocks.jda.getEmoteById(dungeonInfo[i][0]).getAsMention() + " " + dungeonInfo[i][3];
        }
        embedBuilder.addField("Legendary Dungeons", legendaryDungeons, true);
        embedBuilder.addField("Court Dungeons", courtDungeons, true);
        embedBuilder.addField("Epic Dungeons", epicDungeons, true);
        embedBuilder.addField("Highlands Dungeons", highlandsDungeons, true);
        embedBuilder.addField("Random Dungeons", randomDungeons, true);
        embedBuilder.addField("Random Lobbies", randomLobbyTypes, true);
        embedBuilder.setFooter("To exit the raid selection gui type: close");
        return embedBuilder;
    }

    @Getter
    @Setter
    private static class RaidCategory {

        private Category raidCategory;
        private Role raiderRole;
        private TextChannel raidCommands;
        private TextChannel raidStatus;
        private int defaultRaid;

        public RaidCategory(Category raidCategoryOne, Role raiderRoleOne, TextChannel raidCommandsOne, TextChannel raidStatusOne, int defaultRaidOne) {
            raidCategory = raidCategoryOne;
            raiderRole = raiderRoleOne;
            raidCommands = raidCommandsOne;
            raidStatus = raidStatusOne;
            defaultRaid = defaultRaidOne;
        }
    }

}
