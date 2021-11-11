package commands.lobbyCommands;

import commands.Command;
import commands.CommandHub;
import lobbies.LobbyManagerCreation;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.Date;

public class CommandCreateLobbyManager extends Command {
    public CommandCreateLobbyManager() {
        setAliases(new String[] {"createlobbymanager","clm"});
        setEligibleRoles(new String[] {""});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.LOBBY);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        LobbyManagerCreation.lobbyManagerCreation(msg);
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Create Lobby Manager");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Lobby Master\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nOpens up the lobby creation menu." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
