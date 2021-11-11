package commands.miscCommands;

import commands.Command;
import commands.CommandHub;
import giveaways.Giveaway;
import giveaways.GiveawayHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.Date;

public class CommandGiveaway extends Command {
    public CommandGiveaway() {
        setAliases(new String[] {"giveaway", "gw", "gcreate"});
        setEligibleRoles(new String[] {"officer", "hrl", "headEo"});
        setGuildRank(1);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        if (args.length > 0) {
            Giveaway giveaway;
            switch (args[0].toLowerCase()) {
                case "end":
                    if (args.length < 2) {
                        msg.getTextChannel().sendMessage("Please specify the message id of an existing giveaway.").queue();
                        return;
                    }
                    giveaway = GiveawayHub.getGiveaway(args[1]);
                    if (giveaway != null)
                        giveaway.endGiveaway();
                    else msg.getTextChannel().sendMessage("I am unable to find the giveaway with the given message id.").queue();
                    break;
                default:
                    new Giveaway(msg.getMember(), msg.getTextChannel(), msg);
            }
        } else {
            new Giveaway(msg.getMember(), msg.getTextChannel(), msg);
        }
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Giveaway");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Officer\n";
        commandDescription += "Syntax: ;giveaway [create/end]\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nStarts a giveaway with the given parameters.\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
