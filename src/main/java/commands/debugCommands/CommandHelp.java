package commands.debugCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import main.Permissions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import setup.SetupConnector;
import utils.Utils;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CommandHelp extends Command {
    public CommandHelp() {
        setAliases(new String[] {"help", "commands"});
        setEligibleRoles(new String[] {"verified"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.VERIFIED);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        if (args.length == 1) {
            if (Goldilocks.commands.getCommand(args[0]) == null) {
                Utils.sendPM(msg.getAuthor(), "Could not find that command!");
                return;
            }

            if (!msg.isFromGuild()) {
                Utils.sendPMEmbed(msg.getAuthor(), Goldilocks.commands.getCommand(args[0]).getInfo());
                return;
            } else {
                msg.delete().queue();
                msg.getTextChannel().sendMessage(Goldilocks.commands.getCommand(args[0]).getInfo().setFooter("This message will delete in 30 seconds").build()).complete().delete().submitAfter(30L, TimeUnit.SECONDS);
                return;
            }
        }

        Member member = null;
        String guildPrefix = "";
        try {
            guildPrefix = Database.getGuildPrefix(msg.getGuild().getId());
            member = msg.getMember();
        } catch (Exception e) {
            guildPrefix = Database.getGuildPrefix(msg.getAuthor().getMutualGuilds().get(0).getId());
            member = msg.getAuthor().getMutualGuilds().get(0).getMember(msg.getAuthor());
        }

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setColor(Goldilocks.BLUE)
                .setThumbnail(Goldilocks.jda.getSelfUser().getAvatarUrl())
                .setTitle("Commands for Goldilocks")
                .setFooter("Use " + guildPrefix + "help <command name> to get more information about a command.");

        for (CommandHub.CommandNameSpace ns : CommandHub.CommandNameSpace.values()) {
            String fieldDescription = "";
            Member finalMember = member;
            fieldDescription = String.join(" ", Goldilocks.commands.stream()
                    .filter(command -> command.getNameSpace().equals(ns) && SetupConnector.commandEnabled(finalMember.getGuild(), command.getClass()))
                    .map(command -> command.getAliases()[0] + (!Permissions.hasPermission(finalMember, command.getEligibleRoles()) ? "¹" : ""))
                    .collect(Collectors.toList()));
            if (!fieldDescription.isEmpty()) embedBuilder.addField(ns.name, "```\n" + fieldDescription + "\n```", false);
        }

        embedBuilder.setDescription("Here is a list of all of Goldilocks' available commands. You do not have permission for any command marked with ¹. Commands marked with ² are not available in this server.");

        msg.getAuthor().openPrivateChannel().complete().sendMessage(embedBuilder.build()).queue(message -> msg.addReaction("✅").queue());
        //if (msg.isFromGuild()) msg.delete().queue();

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Command information");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Verified\n";
        commandDescription += "Syntax: ;alias <command alias>\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nShows information for all Goldilocks' commands" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
}
}
