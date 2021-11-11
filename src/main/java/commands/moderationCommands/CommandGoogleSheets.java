package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Config;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import sheets.GoogleSheets;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class CommandGoogleSheets extends Command {
    public CommandGoogleSheets() {
        setAliases(new String[] {"sheets", "gsheets", "googlesheets"});
        setEligibleRoles(new String[] {"headRl", "officer"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    public static void listener(Member member, Message message) {
        Goldilocks.eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return e.getUser().equals(member.getUser()) && Objects.equals(e.getMessage(), message);
        }, e -> {

            String control = e.getComponentId();


            if (control.equals("addemail")) {
                e.reply("Please enter the email address you would like to add.").queue();
                getEmail(member, message.getTextChannel());
            } else {

            }

        }, 10L, TimeUnit.MINUTES, () -> message.delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)));
    }

    public static void getEmail(Member member, TextChannel textChannel) {
        Goldilocks.eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(member.getUser()) && e.getMessage().getContentRaw().contains("@");
        }, e -> {
            GoogleSheets.addEmail(textChannel, e.getMessage().getContentRaw());
        }, 10L, TimeUnit.MINUTES, () -> { });
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        Guild guild = msg.getGuild();

        //Goldilocks sheets uses a dataloss preventative storage schema. Thus all of the "items" are stored in events. To view totals and activity it is hig

        GoogleSheets.updateGuildStaff(guild);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Google Sheets Interface for " + guild.getName())
                .setDescription("This is Goldilock's Google Sheets Interface! Use the buttons below to interact with the permissions of the sheet. Otherwise head on over to the sheet via the button below." +
                        " If at any point you have questions on what this is please contact <@" + Config.get("INSTANCE_OWNER") + "> via DMs and he will be sure to give you a run down. For more detailed info " +
                        "on how to use these please visit the sheet and read the sheet marked README.")
                .setColor(Goldilocks.DARKGREEN)
                .setThumbnail("https://res.cloudinary.com/nashex/image/upload/v1622976497/assets/sheetsicon_gcmbfz.png");

        msg.getTextChannel().sendMessage(embedBuilder.build())
                .setActionRow(
                        Button.link("https://docs.google.com/spreadsheets/d/" + GoogleSheets.getSpreadsheetId(guild.getId()), "Google Sheet").withEmoji(Emoji.fromEmote(Goldilocks.jda.getEmoteById(851050452954382366L))),
                        Button.secondary("addemail", "Add Email to Sheet")
                        //Button.secondary("emailremove", "Remove Email")
                ).queue(m -> listener(msg.getMember(), m));

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Google Sheets");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Officer or Head Raid Leader\n";
        commandDescription += "Syntax: ;sheets\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nOpens the Google Sheets UI for Goldilocks!" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
