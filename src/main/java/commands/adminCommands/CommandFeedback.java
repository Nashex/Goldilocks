package commands.adminCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.util.Arrays;
import java.util.Date;

public class CommandFeedback extends Command {
    public CommandFeedback() {
        setAliases(new String[] {"embed","feedback"});
        setEligibleRoles(new String[] {"mod"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        TextChannel textChannel = msg.getTextChannel();
        if (args.length < 2 || msg.getMentionedChannels().isEmpty()) {
            textChannel.sendMessage("Please use the command as follows: " + Database.getGuildPrefix(msg.getGuild().getId()) + "feedback <channel tag> <message>").queue();
            return;
        }

        TextChannel sendingChannel = msg.getMentionedChannels().get(0);
//        if (msg.getGuild().getSelfMember().hasPermission(sendingChannel, EnumSet.of(Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION))) {
//            textChannel.sendMessage("I do not have the correct perms to send a message to this channel.").queue();
//            return;
//        }

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setColor(Goldilocks.DARKGREEN)
                .setDescription(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        sendingChannel.sendMessage(embedBuilder.build()).queue(message -> {
            message.addReaction("👍").queue();
            message.addReaction("👎").queue();
        }, new ErrorHandler().handle(ErrorResponse.MISSING_PERMISSIONS, aVoid -> textChannel.sendMessage("I do not have the correct permissions to send a message to this channel").queue()));


    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Embed Message");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Admin\n";
        commandDescription += "Syntax: ;embed <Channel> <content>\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nEmbeds a message" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
