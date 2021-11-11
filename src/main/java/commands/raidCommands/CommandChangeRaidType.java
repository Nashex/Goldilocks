package commands.raidCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import raids.Raid;
import raids.RaidHub;
import utils.Utils;

import java.util.Date;

public class CommandChangeRaidType extends Command {
    public CommandChangeRaidType() {
        setAliases(new String[] {"change", "type"});
        setEligibleRoles(new String[] {"arl","eo"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.RAID);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        Raid raid = RaidHub.getRaid(msg.getMember());
        if (raid != null) {
            RaidHub.changeRaidType(msg.getMember(), msg.getTextChannel(), raid);
        } else {
            Utils.errorMessage("Failed to Change Raid Type", "Unable to find your raid. If you think this is wrong, please ping Nashex#6969", msg.getTextChannel(), 10L);
        }
        msg.delete().queue();
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Change Raid Type");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Almost Raid Leader / Event Organizer\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nChanges the raid type." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
