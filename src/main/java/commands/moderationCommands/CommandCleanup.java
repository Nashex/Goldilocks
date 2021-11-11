package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import listeners.GeneralListener;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import utils.Logging;
import utils.Utils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CommandCleanup extends Command {
    public CommandCleanup() {
        setAliases(new String[] {"cleanup","purge"});
        setEligibleRoles(new String[] {"tSec"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        TextChannel textchannel = msg.getTextChannel();
        if (args.length == 1 && Utils.isNumeric(args[0])) {
            int numToDelete = Integer.parseInt(args[0]) + 2;
            if (numToDelete > 0 && numToDelete < 100) {
                Message embedMessage = textchannel.sendMessage(cleanupEmbed("Cleaning up " + (numToDelete - 2) + " messages", msg.getMember()).build()).complete();
                List<Message> messages = textchannel.getHistory().retrievePast(numToDelete).complete();
                Logging.logMessages(msg.getMember(), messages);
                messages = messages.stream().filter(message -> !message.getAuthor().isBot() || message.equals(embedMessage))
                        .collect(Collectors.toList());
                GeneralListener.lastPurge = messages.stream().map(message -> message.getId()).collect(Collectors.toList());
                textchannel.purgeMessages(messages);
            }
        }

    }

    public EmbedBuilder cleanupEmbed(String cleanupMessage, Member executer) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Cleaning up Messages");
        embedBuilder.setColor(Goldilocks.GOLD);
        embedBuilder.setDescription("```\n" + cleanupMessage + "\n```");
        embedBuilder.setFooter(executer.getEffectiveName() + " is cleaning up messages");
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Cleanup");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Trial Security\n";
        commandDescription += "Syntax: cleanup <#>\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nCleans up the desired amount of messages." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
