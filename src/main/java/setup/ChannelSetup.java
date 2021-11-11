package setup;

import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import utils.InputVerification;
import utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static main.Goldilocks.eventWaiter;


public class ChannelSetup {

    private Member member;
    private Guild guild;
    private Message controlPanel;
    private List<TextChannel> commandChannels;
    private List<String> changes = new ArrayList<>();
    private String tableName;
    private String visualString;

    public ChannelSetup(Member member, TextChannel textChannel, String tableName) {
        this.member = member;
        this.guild = member.getGuild();
        this.tableName = tableName;

        if (tableName.equalsIgnoreCase("commandChannels")) visualString = "Command";
        else visualString = "Logging";

        commandChannels = SetupConnector.getChannels(guild, tableName);
        controlPanel = textChannel.sendMessage(channelSetupEmbed().build()).complete();
        messageHandler();
    }

    private void messageHandler() {
        String[] commands = {"close", "add", "remove"};

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(member.getUser()) && (Arrays.asList(commands).contains(e.getMessage().getContentRaw().toLowerCase()) || !e.getMessage().getContentRaw().replaceAll("[^0-9]", "").isEmpty());
        }, e -> {
            String content = e.getMessage().getContentRaw();
            e.getMessage().delete().queue();

            if (content.equalsIgnoreCase("close")) {
                controlPanel.editMessage(closeEmbed().build()).queue();
                return;
            } else if (content.equalsIgnoreCase("add")) {
                promptField();
                return;
            } else if (content.contains(" ") && content.split(" ").length > 0 && Utils.isNumeric(content.split(" ")[1])) {
                int choice = Integer.parseInt(content.split(" ")[1]);
                if (choice > 0 && choice <= commandChannels.size()) {
                    TextChannel textChannel = commandChannels.get(choice - 1);
                    SetupConnector.executeUpdate("DELETE FROM " + tableName +" where channelId = " + textChannel.getId());
                    changes.add(textChannel.getName() + ": Disabled " + visualString);
                    commandChannels.remove(textChannel);
                    controlPanel.editMessage(channelSetupEmbed().build()).queue();
                } else Utils.errorMessage("Invalid Option", "Please ensure the value you would like to remove is between `1-" + commandChannels.size() + "`.", controlPanel.getTextChannel(), 5L);
            } else Utils.errorMessage("Invalid Option", "Use the syntax provided in the controls section.", controlPanel.getTextChannel(), 5L);
            messageHandler();

        }, 5L, TimeUnit.MINUTES, () -> {
            //Timeout
        });
    }

    private void promptField() {
        controlPanel.editMessage(promptEmbed().build()).queue();
        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(member.getUser()) && (e.getMessage().getTextChannel().equals(controlPanel.getTextChannel()));
        }, e -> {
            String content = e.getMessage().getContentRaw();

            if (content.equalsIgnoreCase("close")) {
                controlPanel.editMessage(closeEmbed().build()).queue();
                e.getMessage().delete().queue();
                return;
            }

            String id = "";
            TextChannel textChannel = InputVerification.getGuildTextChannel(guild, e.getMessage());
            if (textChannel != null) id = textChannel.getId();
            e.getMessage().delete().queue();

            if (!id.isEmpty()) {
                SetupConnector.executeUpdate("INSERT INTO " + tableName + " (guildId,channelId) VALUES (" + guild.getId() + "," + id + ")");
                changes.add(textChannel.getName() + ": Enabled " + visualString + (visualString.equals("Command") ? "s" : ""));
                commandChannels.add(textChannel);
                controlPanel.editMessage(channelSetupEmbed().build()).queue();
                messageHandler();
            } else {
                Utils.errorMessage("Invalid Value for Text Channel", e.getMessage().getContentRaw() + " is not a valid value. " +
                        "Please make sure that your input is a valid name, mention, or id for this field.", controlPanel.getTextChannel(), 5L);
                promptField();
            }

        }, 5L, TimeUnit.MINUTES, () -> {
            //Timeout
        });
    }

    private EmbedBuilder promptEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(visualString + " Channel Configuration for " + guild.getName())
                .setColor(Goldilocks.BLUE)
                .setDescription("Please enter a valid channel to setup " + visualString + " for. Valid values include names," +
                        " mentions, and ids.")
                .setFooter("Setup Initiated by: " + member.getEffectiveName())
                .setTimestamp(new Date().toInstant());

        return embedBuilder;
    }

    private EmbedBuilder closeEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Completed " + visualString + " Channel Setup for " + guild.getName())
                .setColor(Goldilocks.WHITE)
                .setFooter("Setup Initiated by: " + member.getEffectiveName())
                .setTimestamp(new Date().toInstant())
                .addField("Changes ", "```\n" + (changes.isEmpty() ? "No changes were made." : String.join("\n", (changes))) + "\n```", false);

        return embedBuilder;
    }

    private EmbedBuilder channelSetupEmbed() {
        String fieldString = "";
        int index = 1;
        if (commandChannels.isEmpty()) fieldString += "All channels for this server are " + visualString + " channels. Add channels to enter whitelist mode.";
        else for (TextChannel textChannel : commandChannels) fieldString += "**`" + String.format("%-2d", index++) + "`:**" + textChannel.getAsMention() + "\n";

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(visualString + " Channel Setup for " + guild.getName())
                .setColor(Goldilocks.WHITE)
                .setDescription("Your have entered the setup process for " + visualString.toLowerCase() + " channels.\n\n" +
                        "**Controls**\n" +
                        "To __add__ a channel enter `add`\n" +
                        "To __remove__ a channel enter `remove <#>`\n" +
                        "If you would like to __exit__ this process please type `close`")
                .setFooter("Setup Initiated by: " + member.getEffectiveName())
                .setTimestamp(new Date().toInstant());

        // Fieldify
        String[] values = fieldString.split("\n");
        String curField = "";
        for (String s : values) {
            if (curField.length() + s.length() < 1022) curField += s + "\n";
            else {
                embedBuilder.addField(" ", curField, false);
                curField = s + "\n";
            }
        }
        if (!curField.isEmpty()) embedBuilder.addField(" ", curField, false);

        if (!changes.isEmpty()) {
            embedBuilder.addField("Changes ", "```\n" + String.join("\n", changes) + "\n```", false);
        }

        return embedBuilder;
    }

}
