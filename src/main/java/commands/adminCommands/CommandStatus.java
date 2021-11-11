package commands.adminCommands;

import commands.Command;
import commands.CommandHub;
import main.Config;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.Date;

public class CommandStatus extends Command {
    public CommandStatus() {
        setAliases(new String[] {"status"});
        setEligibleRoles(new String[] {"developer"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.SETUP);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Goldilocks Status")
                .setDescription("This bot instance is owned by " + Goldilocks.jda.getUserById(Config.get("INSTANCE_OWNER").toString()).getAsMention() + ". " +
                        "It has `" + Goldilocks.commands.size() + "` commands, `" + Goldilocks.jda.getUsers().size() + "` users, and " +
                        "`" + Goldilocks.jda.getGuilds().stream().mapToLong(g -> g.getMembers().size()).sum() + "` members. This shard has an uptime since `" + Goldilocks.getUptime(Goldilocks.timeStarted) + "` " +
                        "and a current ping of `" + Goldilocks.jda.getGatewayPing() + "` ms.")
                .setColor(Goldilocks.WHITE)
                .setTimestamp(new Date().toInstant());
        msg.getTextChannel().sendMessage(embedBuilder.build()).queue();
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Raid Setup");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Owner\n";
        commandDescription += "Syntax: ;alias <command alias>\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nSets up raiding categories for a guild" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
