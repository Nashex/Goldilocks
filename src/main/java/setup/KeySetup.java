package setup;

import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static main.Goldilocks.eventWaiter;

public class KeySetup {

    private Guild guild;
    private Message message;
    private Message controlPanel;
    private TextChannel textChannel;
    private List<EmbedBuilder> keySectionPages = new ArrayList<>();
    private int pageIndex = 1;
    private KeySection keySection = new KeySection();

    public KeySetup(Message message) {
        guild = message.getGuild();
        this.message = message;
        this.textChannel = message.getTextChannel();
        createKeySetup();
    }

    public void createKeySetup() {

        guild = message.getGuild();

        TextChannel textChannel = message.getTextChannel();

        Message raidSetupMessage = textChannel.sendMessage(keySetupEmbed().build()).complete();
        raidSetupMessage.addReaction("🛠").queue();
        raidSetupMessage.addReaction("❌").queue();

        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(raidSetupMessage.getId()) && e.getUser().equals(message.getAuthor()) && (e.getReactionEmote().getEmoji().equals("🛠") ||
                    e.getReactionEmote().getEmoji().equals("❌"));
        }, e -> {
            if (e.getReactionEmote().getEmoji().equals("🛠")) {
                raidSetupMessage.delete().queue();
                createKeySectionSetupMenu();
            }
            if (e.getReactionEmote().getEmoji().equals("❌")) {
                raidSetupMessage.delete().queue();
                message.delete().queue();
            }
        }, 2L, TimeUnit.MINUTES, () -> {raidSetupMessage.delete().queue();});

    }

    public EmbedBuilder keySetupEmbed() {

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Key Role Configuration Control Panel");
        embedBuilder.setDescription("Below is a brief description of the key popper roles for " + guild.getName());
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setFooter("React with ❌ at anytime to close this menu");

        List<String[]> keySectionInfo = Database.getGuildKeySections(guild.getId());

        if (keySectionInfo.isEmpty()) {
            embedBuilder.addField("You currently have no key popper roles", "If you would like to create a key popper role, react with 🛠", false);
            return embedBuilder;
        }

        for (String[] keySection : keySectionInfo) {

            String keySectionSettings = "Key Popper Role: " + guild.getRoleById(keySection[1]).getAsMention() +
                    "\nKey Requirement: `" + keySection[2] + "`" +
                    "\nLeaderboard Role : `" + (keySection[3].equals("1") ? "Yes`" : "No`") +
                    "\nCustom Message: " + "`" + (keySection[4].isEmpty() ? "None Set" : keySection[4]) + "`";

            embedBuilder.addField(guild.getRoleById(keySection[0]).getName(), keySectionSettings ,false);
        }

        return embedBuilder;
    }

    public void createKeySectionSetupMenu() {

        List<String[]> keySectionInfo = Database.getGuildKeySections(guild.getId());
        if (keySectionInfo.isEmpty()) {
            keySectionPages.add(keySectionEmbed(new String[0]));
        } else {
            for (String[] keySection : keySectionInfo) {
                keySectionPages.add(keySectionEmbed(keySection));
            }
            keySectionPages.add(keySectionEmbed(new String[1]));
        }

        controlPanel = textChannel.sendMessage(keySectionPages.get(0).build()).complete();

        if (keySectionPages.size() > 1) {
            controlPanel.addReaction("◀").queue();
            controlPanel.addReaction("▶").queue();
            controlPanel.addReaction("🛠").queue();
        }
        controlPanel.addReaction("🆕").queue();
        controlPanel.addReaction("❌").queue();

        menuControls();
    }

    private void menuControls() {
        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(controlPanel.getId()) && e.getUser().equals(message.getAuthor())
                    && ("◀▶🛠🆕❌").contains(e.getReactionEmote().getEmoji());
        }, e -> {

            String emote = e.getReactionEmote().getEmoji();

            if (emote.equals("🛠")) {
                if (pageIndex != keySectionPages.size()) {
                    List<String[]> keySectionInfo = Database.getGuildKeySections(guild.getId());
                    keySection = Database.getKeySection(keySectionInfo.get(pageIndex - 1)[1]);
                    keySectionCreation();
                } else {
                    e.getReaction().removeReaction(e.getUser()).queue();
                    menuControls();
                    return;
                }
            }

            if (emote.equals("🆕")) {
                keySectionCreation();
                return;
            }

            if (emote.equals("◀")) {
                if (pageIndex == 1) {
                    e.getReaction().removeReaction(e.getUser()).queue();
                    menuControls();
                    return;
                }
                pageIndex--;
                controlPanel.editMessage(keySectionPages.get(pageIndex - 1).build()).queue();
                e.getReaction().removeReaction(e.getUser()).queue();
                menuControls();
            }
            if (emote.equals("▶")) {
                if (pageIndex == keySectionPages.size()) {
                    e.getReaction().removeReaction(e.getUser()).queue();
                    menuControls();
                    return;
                }
                pageIndex++;
                controlPanel.editMessage(keySectionPages.get(pageIndex - 1).build()).queue();
                e.getReaction().removeReaction(e.getUser()).queue();
                menuControls();
            }
            if (emote.equals("❌")) {
                controlPanel.delete().queue();
                message.delete().queue();
            }
        }, 2L, TimeUnit.MINUTES, () -> {controlPanel.delete().queue();});
    }

    public void keySectionCreation() {
        controlPanel.clearReactions().complete();
        controlPanel.editMessage(keySectionEmbed(keySection).build()).queue();
        controlPanel.addReaction("1️⃣").queue();
        controlPanel.addReaction("2️⃣").queue();
        controlPanel.addReaction("3️⃣").queue();
        controlPanel.addReaction("4️⃣").queue();
        controlPanel.addReaction("❌").queue();

        sectionMenuControls();

    }

    private void sectionMenuControls() {
        eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(controlPanel.getId()) && e.getUser().equals(message.getAuthor())
                    && ("1️⃣2️⃣3️⃣4️⃣📥❌").contains(e.getReactionEmote().getEmoji());
        }, e -> {

            String emote = e.getReactionEmote().getEmoji();

            if (emote.equals("1️⃣")) {
                getPopperRole();
                return;
            }

            if (emote.equals("2️⃣")) {
                getKeyAmount();
                return;
            }

            if (emote.equals("3️⃣")) {
                getCustomMessage();
                return;
            }

            if (emote.equals("4️⃣")) {
                getCustomMessage();
                return;
            }

            if (emote.equals("📥") &&
                    (keySection.getKeyRole() != null && keySection.getKeyAmount() != -1)) {
                keySection.setGuild(guild);
                Database.addKeySection(keySection);
                message.delete().queue();
                controlPanel.delete().queue();
                return;
            }

            if (emote.equals("❌")) {
                controlPanel.delete().queue();
                message.delete().queue();
            }
        }, 2L, TimeUnit.MINUTES, () -> {controlPanel.delete().queue();});
    }

    private EmbedBuilder keySectionEmbed(String[] keySection) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setTitle("Key Popper Role Configuration");
        embedBuilder.setDescription("**Controls:**" +
                (keySectionPages.size() > 1 ? "\nTo edit an existing key popper role react with 🛠" : "") +
                "\nTo create a key popper role react with 🆕" +
                (keySectionPages.size() > 1 ? "\nPlease use ◀ and ▶ to scroll through roles." : ""));
        if (keySection.length == 0) {
            embedBuilder.addField("New Key Popper Role:","If you would like to add a key popper role please react with 🆕, otherwise exit this menu by reacting with ❌", false);
        } else if (keySection.length == 1) {
            embedBuilder.addField("Assign New Key Popper Role", "If you would like to create a key popper role, react with 🛠", false);
            return embedBuilder;
        } else {
            String keySectionSettings = "Key Popper Role: " + guild.getRoleById(keySection[1]).getAsMention() +
                    "\nKey Requirement: `" + keySection[2] + "`" +
                    "\nLeaderboard Role : `" + (keySection[3].equals("1") ? "Yes`" : "No`") +
                    "\nCustom Message: " + "`" + (keySection[4].isEmpty() ? "None Set" : keySection[4]) + "`" +
                    "\n__To edit this role react with:__ 🛠";
            embedBuilder.addField(guild.getRoleById(keySection[0]).getName(), keySectionSettings ,false);
        }

        embedBuilder.setFooter((keySectionPages.size() > 1 ? "Role: " + pageIndex + " of " + keySectionPages.size() + " | " : "")
                + "React with ❌ at anytime to close this menu");

        return embedBuilder;
    }

    private EmbedBuilder keySectionEmbed(KeySection keySection) {

        boolean save = keySection.getKeyRole() != null && keySection.getKeyAmount() != -1;
        if (save) controlPanel.addReaction("📥").queue();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Goldilocks.GOLD);
        embedBuilder.setTitle("Key Popper Role Configuration");
        embedBuilder.setDescription("**Controls:**" +
                "\nReact with the number emotes to edit the corresponding attributes of the role." +
                (save ? "\nReact with 📥 to store this key popping section" : ""));
        String keySectionSettings = "1️⃣ Key Popper Role: " + (keySection.getKeyRole() == null ? "`None Set`" : keySection.getKeyRole().getAsMention()) +
                "\n2️⃣ Key Requirement: " + (keySection.getKeyAmount() == -1 ? "`None Set`" : "`" + keySection.getKeyAmount() + "`") +
                "\n3️⃣ Custom Message: " + (keySection.getUniqueMessage().isEmpty() ? "`None Set`" : "`" + keySection.getUniqueMessage() + "`");
        embedBuilder.addField((keySection.getKeyRole() == null ? "New Key Popper Role" : keySection.getKeyRole().getName()), keySectionSettings ,false);
        embedBuilder.setFooter("React with ❌ at anytime to close this menu");

        return embedBuilder;
    }

    public void getPopperRole() {
        controlPanel.editMessage(promptForItem("Key Popper Role").build()).queue();
        controlPanel.clearReactions().queue();

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getMessage().getAuthor().equals(message.getAuthor()) && (("close").equals(e.getMessage().getContentRaw()) || !message.getGuild().getRoleById(e.getMessage().getContentRaw()).equals(null));
        }, e -> {

            if (("close").equals(e.getMessage().getContentRaw())) {
                controlPanel.delete().queue();
                message.delete().queue();
                e.getMessage().delete().queue();
                return;
            }

            try {
                guild.getRoleById(e.getMessage().getContentRaw());
                keySection.setKeyRole(guild.getRoleById(e.getMessage().getContentRaw()));
                e.getMessage().delete().queue();
                keySectionCreation();

            } catch (Exception error) {
                e.getMessage().delete().queue();
                Utils.errorMessage("Failed to Assign Raiding Role","Invalid Role Id", textChannel, 5L);
                getPopperRole();
            }

        }, 2L, TimeUnit.MINUTES,() -> {controlPanel.delete().queue(); message.delete().queue();});
    }

    public void getKeyAmount() {
        controlPanel.editMessage(promptForItem("Required Key Amount").build()).queue();
        controlPanel.clearReactions().queue();

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getMessage().getAuthor().equals(message.getAuthor()) && (("close").equals(e.getMessage().getContentRaw()) || Utils.isNumeric(e.getMessage().getContentRaw()));
        }, e -> {

            if (("close").equals(e.getMessage().getContentRaw())) {
                controlPanel.delete().queue();
                message.delete().queue();
                e.getMessage().delete().queue();
                return;
            }

            int requiredKeys = Integer.parseInt(e.getMessage().getContentRaw());

            if (requiredKeys > 0 && requiredKeys <= 1000) {

                keySection.setKeyAmount(requiredKeys);
                e.getMessage().delete().queue();
                keySectionCreation();

            } else if (requiredKeys < 0) {
                Utils.errorMessage("Failed to Assign Required Key Amount","The amount specified must be larger than 0.", textChannel, 5L);
                e.getMessage().delete().queue();
                getKeyAmount();
            } else {
                Utils.errorMessage("Failed to Assign Required Key Amount","The amount specified must be less than 1001.", textChannel, 5L);
                e.getMessage().delete().queue();
                getKeyAmount();
            }

        }, 2L, TimeUnit.MINUTES,() -> {controlPanel.delete().queue(); message.delete().queue();});
    }

    public void getCustomMessage() {
        controlPanel.editMessage(promptForItem("Custom Message").build()).queue();
        controlPanel.clearReactions().queue();

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getMessage().getAuthor().equals(message.getAuthor());
        }, e -> {

            if (("close").equals(e.getMessage().getContentRaw())) {
                message.delete().queue();
                controlPanel.delete().queue();
                e.getMessage().delete().queue();
                return;
            }

            keySection.setUniqueMessage(e.getMessage().getContentRaw());
            e.getMessage().delete().queue();
            keySectionCreation();

        }, 2L, TimeUnit.MINUTES,() -> {controlPanel.delete().queue(); message.delete().queue();});
    }

    public void getEarlyLoc() {
        controlPanel.editMessage(promptForItem("Early Location").build()).queue();
        controlPanel.clearReactions().queue();

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getMessage().getAuthor().equals(message.getAuthor()) && (e.getMessage().getContentRaw().equalsIgnoreCase("false")
            || e.getMessage().getContentRaw().equalsIgnoreCase("true"));
        }, e -> {
            String content = e.getMessage().getContentRaw().toLowerCase();
            if (("close").equals(content)) {
                message.delete().queue();
                controlPanel.delete().queue();
                e.getMessage().delete().queue();
                return;
            }

            if (("true").equals(content)) {

            }

            keySection.setUniqueMessage(e.getMessage().getContentRaw());
            e.getMessage().delete().queue();
            keySectionCreation();

        }, 2L, TimeUnit.MINUTES,() -> {controlPanel.delete().queue(); message.delete().queue();});
    }

    private EmbedBuilder promptForItem(String item) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Please Select a " + item);
        embedBuilder.setColor(Goldilocks.BLUE);
        if (item.equals("Custom Message")) {
            embedBuilder.setDescription("Please enter a custom message you would like sent to the key popper once they receive this role.");
        } else if (item.equals("Required Key Amount")) {
            embedBuilder.setDescription("Please enter the minimum number of keys to obtain this role.");
        } else {
            embedBuilder.setDescription("Please enter the id for your " + item.toLowerCase() + ".");
        }

        embedBuilder.setFooter("Please type close at any time to exit the setup.");
        embedBuilder.setTimestamp(new Date().toInstant());

        return embedBuilder;
    }

}
