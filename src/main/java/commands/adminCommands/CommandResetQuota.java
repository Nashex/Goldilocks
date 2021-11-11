package commands.adminCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import quota.QuotaManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import utils.Utils;

import java.util.Date;

public class CommandResetQuota extends Command {
    public CommandResetQuota() {
        setAliases(new String[] {"resetquota"});
        setEligibleRoles(new String[] {"admin"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        Utils.confirmAction(msg.getTextChannel(), msg.getAuthor(), "Are you sure you would like to reset quota? This is irreversible.", () -> {
            QuotaManager.resetQuota(msg.getGuild());
        });
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Reset Quota");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Admin\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nResets Quota." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
