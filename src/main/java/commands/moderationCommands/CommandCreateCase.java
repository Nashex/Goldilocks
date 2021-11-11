package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import moderation.CaseFileCpInstance;
import moderation.PunishmentConnector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import utils.Utils;

import java.util.Date;

public class CommandCreateCase extends Command {
    public CommandCreateCase() {
        setAliases(new String[] {"ccase", "createcase", "newcase", "ncase", "cc"});
        setEligibleRoles(new String[] {"security"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        TextChannel textChannel = msg.getTextChannel();

        if (args.length == 0) {
            Utils.errorMessage("Failed to Create Case File", "Invalid command usage, please use newcase <@/id/name>", textChannel, 10L);
            return;
        }

        if (PunishmentConnector.caseExists(args[0], msg.getGuild().getId())) {
            Utils.errorMessage("Failed to Create Case File", "A case already exists for the following case id: " + args[0], textChannel, 10L);
            return;
        }

        String caseId = args[0];
        PunishmentConnector.addPunishment(msg.getMember(), System.currentTimeMillis() / 1000, caseId);

        msg.delete().queue();
        new CaseFileCpInstance(caseId, textChannel, msg.getMember());

    }


    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Case");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Security or Raid Leader\n";
        commandDescription += "Syntax: ;alias <caseId>\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nRetrieves the case file for a given case" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
