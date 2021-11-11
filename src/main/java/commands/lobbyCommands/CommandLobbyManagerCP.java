package commands.lobbyCommands;

import commands.Command;
import commands.CommandHub;
import lobbies.LobbyManagerHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.Date;

public class CommandLobbyManagerCP extends Command {
    public CommandLobbyManagerCP() {
        setAliases(new String[] {"createcontrolpanel","cp","controlpanel"});
        setEligibleRoles(new String[] {""});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.LOBBY);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        LobbyManagerHub.createControlPanel(msg.getTextChannel());
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Lobby Manager Control Panel");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Lobby Master\n";
        commandDescription += "Syntax: ;alias <command alias>\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nShows all of the currently active lobby managers." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
