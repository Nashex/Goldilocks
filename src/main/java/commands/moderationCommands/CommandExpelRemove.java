package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.*;

public class CommandExpelRemove extends Command {
    public CommandExpelRemove() {
        setAliases(new String[] {"rexpel"});
        setEligibleRoles(new String[] {"hrl", "security"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        Member mod = msg.getMember();
        TextChannel textChannel = msg.getTextChannel();
        Guild guild = mod.getGuild();

        if (args.length < 1 ) {
            textChannel.sendMessage("Please use the command in the following format: `.rexpel <id/name> [additional names/ids]`").queue();
            return;
        }

        for (int i = 0; i < args.length; i++) {
            String expulsion = Database.isExpelled(args[i], guild);
            if (!expulsion.isEmpty()) {
                boolean success = Database.expelRemove(args[i], guild);
                if (success) textChannel.sendMessage("Successfully removed expulsion for " + args[i] + "!").queue();
                else textChannel.sendMessage("There was an error removing the expulsion for " + args[i] + ".").queue();
            } else {
                textChannel.sendMessage(args[i] + " is not currently expelled.").allowedMentions(EnumSet.of(Message.MentionType.EMOTE)).queue();
            }
        }
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Remove Expel");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Security / Head Raid Leader\n";
        commandDescription += "Syntax: .rexpel <id/name> [additional names/ids]\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nRemoves the expulsion for the user/user(s)" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
