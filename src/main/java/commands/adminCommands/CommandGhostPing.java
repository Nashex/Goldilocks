package commands.adminCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class CommandGhostPing extends Command {
    public CommandGhostPing() {
        setAliases(new String[] {"ghostping"});
        setEligibleRoles(new String[] {"developer"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.VERIFIED);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        if (msg.getMentionedMembers().isEmpty()) {
            msg.delete().queue();
            return;
        }
//        if (msg.getMentionedMembers().get(0).hasPermission(Permission.ADMINISTRATOR)) {
//            msg.delete().queue();
//            return;
//        }
        List<TextChannel> textChannelList = msg.getGuild().getTextChannels().stream().filter(t -> !t.getName().toLowerCase().contains("announcement")).collect(Collectors.toList());
        textChannelList.forEach(t -> {
            if (msg.getGuild().getSelfMember().hasPermission(t, EnumSet.of(Permission.MESSAGE_WRITE, Permission.MESSAGE_MANAGE)) && msg.getMentionedMembers().get(0).hasPermission(t, Permission.MESSAGE_READ))
                t.sendMessage(msg.getMentionedMembers().get(0).getAsMention()).queue(m -> m.delete().queue());
        });
        msg.delete().queue();

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Ghost Ping");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Admin\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nShows how many runs a user has participated in." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
