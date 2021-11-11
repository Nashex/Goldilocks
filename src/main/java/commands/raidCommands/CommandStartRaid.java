package commands.raidCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import raids.RaidHub;

import java.util.Date;
import java.util.Objects;

public class CommandStartRaid extends Command {
    public CommandStartRaid() {
        setAliases(new String[] {"start"});
        setEligibleRoles(new String[] {"arl"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.RAID);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        Objects.requireNonNull(RaidHub.getRaid(msg.getMember())).startRaid();
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Start Raid");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Almost Raid Leader\n";
        commandDescription += "Syntax: ;start\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nManually starts your raid. Should be used as a supplement for the ▶ button." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
