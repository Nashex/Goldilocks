package commands.raidCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import raids.Raid;
import raids.RaidHub;

import java.util.Date;

public class CommandEndRaid extends Command {
    public CommandEndRaid() {
        setAliases(new String[] {"abort", "end", "fail"});
        setEligibleRoles(new String[] {"arl","eo"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.RAID);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        if (RaidHub.getRaid(msg.getMember()) != null) {
            Raid raid = RaidHub.getRaid(msg.getMember());
            raid.abortRaid(msg.getMember());
            RaidHub.activeRaids.remove(raid);
        }

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Abort Raid");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: RL\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nAborts your raid and if the afk check has ended, logs it as a fail." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
