package commands.lobbyCommands;

import commands.Command;
import commands.CommandHub;
import lobbies.LobbyManager;
import lobbies.LobbyManagerHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import utils.Utils;

import java.util.Date;

public class CommandDeleteLobbyManager extends Command {
    public CommandDeleteLobbyManager() {
        setAliases(new String[] {"deletelobbymanager","dlm"});
        setEligibleRoles(new String[] {""});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.LOBBY);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        if (args.length == 0) {
            LobbyManagerHub.createControlPanel(msg.getTextChannel());
            return;
        }

        TextChannel textChannel = msg.getTextChannel();
        msg.delete().queue();
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Goldilocks.GOLD);

        Member executor = msg.getMember();

        if (args[0].equals("all")) {
            String description = "```\nSuccessfully deleted:\n";
            for (LobbyManager lobbyManager : LobbyManagerHub.activeLobbyManagers) {
                description += lobbyManager.getLobbyName() + " | Id: " + lobbyManager.getLobbyMessageId() + "\n\n";
                LobbyManagerHub.deleteLobbyManager(lobbyManager.getLobbyMessageId(), executor);
            }
            embedBuilder.setTitle("All Lobby Managers Successfully Deleted");
            embedBuilder.setDescription(description + "```");
            embedBuilder.setTimestamp(new Date().toInstant());
            msg.getTextChannel().sendMessage(embedBuilder.build()).queue();
            return;
        } else {
            try {
                LobbyManager lobbyManager = LobbyManagerHub.getLobbyManager(args[0]);
                embedBuilder.setTitle("Lobby Manager for " + lobbyManager.getLobbyName() + " Successfully Deleted");
                embedBuilder.setDescription("```\nSuccessfully deleted " + lobbyManager.getLobbyName() + " with an Id of " + args[0] + "\n```");
                embedBuilder.setTimestamp(new Date().toInstant());
                LobbyManagerHub.deleteLobbyManager(args[0], executor);
                msg.getTextChannel().sendMessage(embedBuilder.build()).queue();
            } catch (NullPointerException e) {
                Utils.errorMessage("Failed to Delete Lobby Manager", "Lobby Manager with an Id of " + args[0] + " was not found.",
                        textChannel, 10L);
            }
        }

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Delete Lobby Manager");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Lobby Master\n";
        commandDescription += "Syntax: ;alias <lobby manager id>\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nDeletes a lobby manager." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
