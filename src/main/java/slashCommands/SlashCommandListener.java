package slashCommands;

import main.Database;
import main.Goldilocks;
import main.Permissions;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import setup.SetupConnector;
import sheets.GoogleSheets;

import javax.annotation.Nonnull;

public class SlashCommandListener extends ListenerAdapter {

    @Override
    public void onSlashCommand(@Nonnull SlashCommandEvent event) {

        if (event.getGuild() == null || event.getMember() == null) return;

        Guild guild = event.getGuild();
        String guildId;
        int guildRank;
        String guildInfo = SetupConnector.getFieldValue(event.getGuild(), "guildInfo","rank");
        guildRank = guildInfo.isEmpty() ? 1 : Integer.parseInt(guildInfo);

        String alias = event.getName();

        SlashCommand slashCommand;
        if ((slashCommand = Goldilocks.slashCommands.getCommand(alias)) != null) {

            // Check if they are a raiding server
            if (guildRank < slashCommand.getGuildRank()) {
                event.reply("Unfortunately this command is not available in this server, if you believe this to be an error join the support server.")
                        .addActionRow(Button.link("https://discord.gg/jq7WuZRWqe", "Click Here to Join the Support Server!")).setEphemeral(true).queue();
                return;
            }

            // Check if the member has permission to execute the command
            if (!Permissions.hasPermission(event.getMember(), slashCommand.getEligibleRoles())) {
                event.reply("You do not have permission to use `" + alias + "`.").setEphemeral(true).queue();
                return;
            }

            // Execute the command
            try {
                if (guildRank >= 3) {
                    if (SetupConnector.commandEnabled(guild, slashCommand.getClass())) slashCommand.execute(event);
                    else event.reply("Unfortunately this command is not available in this server, if you believe this to be an error join the support server.")
                            .addActionRow(Button.link("https://discord.gg/jq7WuZRWqe", "Click Here to Join the Support Server!")).setEphemeral(true).queue();
                } else {
                    slashCommand.execute(event);
                }
            } catch (Exception ignored) { }

            Database.logEvent(event.getMember(), Database.EventType.COMMAND, System.currentTimeMillis() / 1000, (TextChannel) event.getChannel(), alias);
            GoogleSheets.logEvent(guild, GoogleSheets.SheetsLogType.COMMANDS, event.getMember().getEffectiveName(), event.getChannel().getId(),
                    event.getMember().getId(), alias, event.getCommandPath());

        }

    }
}
