package commands.setupCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import utils.Utils;

import java.util.Date;

public class CommandCreateStickyRole extends Command {
    public CommandCreateStickyRole() {
        setAliases(new String[] {"stickyrole", "createstickyrole", "rolemessage"});
        setEligibleRoles(new String[] {""});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.SETUP);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        if (args.length < 1) {
            Utils.errorMessage("Failed to Create Sticky Role Message", "Please use the following syntax: stickyrole <roleId> <textchannelId>", msg.getTextChannel(), 10L);
            return;
        }

        TextChannel textChannel;
        Role stickyRole;
        Guild guild = msg.getGuild();

        try {
            stickyRole = Goldilocks.jda.getRoleById(args[0]);
            textChannel = Goldilocks.jda.getTextChannelById(args[1]);

            stickyRole.getId();
            textChannel.getId();

        } catch (Exception e) {
            Utils.errorMessage("Failed to Create Sticky Role Message", "Please use the following syntax: stickyrole <roleId> <textchannelId>", msg.getTextChannel(), 10L);
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();

        if (args.length > 2) {
            if (args[2].equalsIgnoreCase("event")) {
                embedBuilder.setTitle("Event Verify with " + guild.getName());
                embedBuilder.setColor(Goldilocks.BLUE);
                embedBuilder.setDescription("Hello! Please react to this message with ✅ to be given access to event raids!\n" +
                        "If you want your role removed, just un-react!");
                embedBuilder.setFooter("Please note you can only use this feature once every 5 minutes.");
            }
            if (args[2].equalsIgnoreCase("fun")) {
                embedBuilder.setTitle("Fun Verify with " + guild.getName());
                embedBuilder.setColor(Goldilocks.BLUE);
                embedBuilder.setDescription("Hello! Please react to this message with ✅ to be given access to fun raids!\n" +
                        "If you want your role removed, just un-react!");
                embedBuilder.setFooter("Please note you can only use this feature once every 5 minutes.");
            }
        } else {
            return;
        }

        Message stickyMessage = textChannel.sendMessage(embedBuilder.build()).complete();
        stickyMessage.addReaction("✅").queue();

        Database.createStickyRoleMessage(stickyMessage, stickyRole);

        EmbedBuilder confirmationMessage = new EmbedBuilder();
        confirmationMessage.setAuthor("Successfully Created Sticky Role Message", msg.getMember().getUser().getAvatarUrl(), msg.getMember().getUser().getAvatarUrl());
        confirmationMessage.setColor(Goldilocks.BLUE);
        confirmationMessage.setDescription("**Role: **" + stickyRole.getAsMention() +
                "\n**Channel: **" + textChannel.getAsMention() +
                "\n**Message Link: ** [Sticky Role Message](" + stickyMessage.getJumpUrl() + ")");
        confirmationMessage.setFooter("Created By: " + msg.getMember().getEffectiveName() + " on ");
        confirmationMessage.setTimestamp(new Date().toInstant());

        msg.delete().queue();
        msg.getTextChannel().sendMessage(confirmationMessage.build()).queue();

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Sticky Role");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Admin\n";
        commandDescription += "Syntax: ;alias <command alias>\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nCreates a sticky role message." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
