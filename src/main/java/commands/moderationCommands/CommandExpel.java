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

public class CommandExpel extends Command {
    public CommandExpel() {
        setAliases(new String[] {"expel", "expelled"});
        setEligibleRoles(new String[] {"hrl", "security"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        List<String> controls = new ArrayList<>(Arrays.asList("add", "remove", "a", "r"));

        Member mod = msg.getMember();
        TextChannel textChannel = msg.getTextChannel();
        Guild guild = mod.getGuild();

        String control = "add";

        if (args.length < 1) {
            textChannel.sendMessage("Please use the command in the following format: `.expel <add/remove> <id/name> [reason]`").queue();
            return;
        }

        String string = args[0];
        if (controls.contains(args[0].toLowerCase())) {
            control = args[0].toLowerCase();
            string = args[1].toLowerCase();
        }

        if (Arrays.asList("add", "a").contains(control)) {
            String expulsion = Database.isExpelled(string, guild);
            if (expulsion.isEmpty()) {
                String reason = (args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)).replace(string + " ", "") : "None provided");
                String name = string.replaceAll("[^A-Za-z]", "").toLowerCase();
                String id = string.replaceAll("[^0-9]", "");
                boolean success = Database.expelAdd(id, name, mod, reason);
                if (success) textChannel.sendMessage(new EmbedBuilder().setColor(Goldilocks.GREEN).setDescription("Successfully expelled " + string + " for \n> " + reason).build()).queue();
                else textChannel.sendMessage("There was an error adding the expulsion for " + string + ".").queue();
            } else {
                textChannel.sendMessage(new EmbedBuilder().setColor(Goldilocks.RED).setDescription(expulsion).build()).queue();
            }
        }

        if (Arrays.asList("remove", "r").contains(control)) {
            for (int i = 1; i < args.length; i++) {
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
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Expel");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Security / Head Raid Leader\n";
        commandDescription += "Syntax: ;expel <add/remove> <id/name> [reason]\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nExpels or Unexpels a user from verifying with the server." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
