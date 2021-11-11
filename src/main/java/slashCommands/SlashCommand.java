package slashCommands;

import commands.CommandHub;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.awt.*;

@Getter @Setter
public abstract class SlashCommand {

    private String name;
    private String[] eligibleRoles;
    private int guildRank;
    private boolean guildOnly = false;
    private CommandHub.CommandNameSpace nameSpace;

    public CommandData commandData;

    public abstract void execute(SlashCommandEvent event);

    public EmbedBuilder getInfo() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.RED);
        embedBuilder.setTitle("Error");
        embedBuilder.setDescription("This slash command has no information.");
        return embedBuilder;
    }

    public SlashCommand enable(Guild guild) {
        guild.upsertCommand(commandData).queue();
        return this;
    }
}
