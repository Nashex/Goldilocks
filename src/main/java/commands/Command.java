package commands;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.awt.*;

@Getter @Setter
public abstract class Command {
    private String[] aliases;
    private String[] eligibleRoles;
    private int guildRank;
    private boolean guildOnly = false;
    private CommandHub.CommandNameSpace nameSpace;

    public abstract void execute(Message msg, String alias, String[] args);

    public EmbedBuilder getInfo() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.RED);
        embedBuilder.setTitle("Error");
        embedBuilder.setDescription("This command has no information.");
        return embedBuilder;
    }
}
