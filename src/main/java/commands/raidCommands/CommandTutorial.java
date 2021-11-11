package commands.raidCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.Date;

public class CommandTutorial extends Command {
    public CommandTutorial() {
        setAliases(new String[] {"tutorial","faq"});
        setEligibleRoles(new String[] {"arl","eo"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.DEBUG);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        msg.getTextChannel().sendMessage(tutorialEmbed().build()).queue();
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Tutorial");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Almost Raid Leade or Event Organizer\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nShows a tutorial for raiding!" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    private EmbedBuilder tutorialEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        String description = "**General Commands**\n" +
                "To start a raid use `.raid`\n" +
                "To start a headcount use `.hc`\n" +
                "To set a custom color to your afk checks use `.rlprefs`\n" +
                "\n" +
                "**Afk Check Controls**\n" +
                "To end your afk check react with ▶\n" +
                "To change location react with 🗺\n" +
                "To start a new AFK check either do `.raid` or react with 🆕\n" +
                "To **log your raid** and purge vc react with ❌\n" +
                "\n" +
                "**Logging**\n" +
                "__Runs__ -> All logging is automatic, no need for commands. React with ❌ to log and purge vc, react with 🆕 to log and open a new AFK.\n" +
                "__Assists__ -> All parses automatically count as assists, if you took over the run use the command `.log <@/name/id/mention>` to log an assist.\n" +
                "__Keys__ -> Use the key control panel to log keys, select the person you would like to control keys for then use the up and down arrows to add and remove keys.\n" +
                "\n**All logging is synced with ViBot** No need to use ViBot to log if you raided with Goldi.\n" +
                "\n**Failed Runs and Aborting**\n" +
                "Use `.abort` to abort an AFK check and `.fail` to both abort the AFK check and log the run as a fail.\n";

        embedBuilder.setTitle("Tutorial and FaQ for Raiding with Goldilocks")
                .setColor(Goldilocks.GOLD)
                .setDescription(description)
                .setTimestamp(new Date().toInstant());

        return embedBuilder;
    }

}


