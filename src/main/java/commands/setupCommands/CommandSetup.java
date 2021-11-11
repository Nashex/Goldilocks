package commands.setupCommands;

import commands.Command;
import commands.CommandHub;
import main.Config;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import setup.ChannelSetup;
import setup.DungeonSetup;
import setup.QuotaSetup;
import setup.Setup;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommandSetup extends Command {
    public CommandSetup() {
        setAliases(new String[] {"setup"});
        setEligibleRoles(new String[] {"mod", "hrl"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.SETUP);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        if (args.length == 0) {
            Message message = msg.getTextChannel().sendMessage(promptEmbed(msg.getGuild()).build())
                    .setActionRows(
                            ActionRow.of(
                                    Button.primary("1", "Guild General"),
                                    Button.primary("2", "Logs"),
                                    Button.primary("3", "Commands").withDisabled(!msg.getAuthor().getId().equals(Config.get("INSTANCE_OWNER"))),
                                    Button.primary("4", "Command Channels")
                            ),
                            ActionRow.of(
                                    Button.primary("5", "Logging Channels"),
                                    Button.primary("6", "Quota"),
                                    Button.primary("7", "Dungeons"),
                                    Button.danger("exit", "Exit")
                            )
                    ).complete();

            List<String> controls = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "exit");

            Goldilocks.eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
                return e.getMessageId().equals(message.getId()) && e.getUser().equals(msg.getAuthor()) &&
                        controls.contains(e.getComponentId());
            }, e -> {

                String control = e.getComponentId();
                e.deferEdit().queue();

                if (control.equals("1")) {
                    new Setup(msg, Setup.SETUP.GUILD);
                }
                if (control.equals("2")) {
                    new Setup(msg, Setup.SETUP.LOGS);
                }
                if (control.equals("3") && msg.getAuthor().getId().equals(Config.get("INSTANCE_OWNER"))) {
                    new Setup(msg, Setup.SETUP.COMMANDS);
                }
                if (control.equals("4")) {
                    new ChannelSetup(msg.getMember(), msg.getTextChannel(), "commandChannels");
                }
                if (control.equals("5")) {
                    new ChannelSetup(msg.getMember(), msg.getTextChannel(), "ignoredChannels");
                }
                if (control.equals("6")) {
                    new QuotaSetup(msg, false);
                }
                if (control.equals("7")) {
                    new DungeonSetup(msg.getMember(), msg.getTextChannel());
                }
                msg.delete().queue();
                message.delete().queue();

            }, 2L, TimeUnit.MINUTES, () -> {
                message.delete().queue();
            });
        }

        // Direct Setup
        if (args.length > 0 && args[0].equalsIgnoreCase("guild")) {
            new Setup(msg, Setup.SETUP.GUILD);
            msg.delete().queue();
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("commandChannels")) {
            new ChannelSetup(msg.getMember(), msg.getTextChannel(), "commandChannels");
            msg.delete().queue();
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("logging")) {
            new ChannelSetup(msg.getMember(), msg.getTextChannel(), "ignoredChannels");
            msg.delete().queue();
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("logs")) {
            new Setup(msg, Setup.SETUP.LOGS);
            msg.delete().queue();
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("commands") && msg.getAuthor().getId().equals(Config.get("INSTANCE_OWNER"))) {
            new Setup(msg, Setup.SETUP.COMMANDS);
            msg.delete().queue();
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("quota")) {
            new QuotaSetup(msg, false);
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("dungeons")) {
            new DungeonSetup(msg.getMember(), msg.getTextChannel());
        }
    }

    private EmbedBuilder promptEmbed(Guild guild) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Setup Options for " + guild.getName())
                .setDescription("You have entered setup for Goldilocks! Please select the section that you would like " +
                        "to setup by pressing the corresponding button.")
                .setColor(Goldilocks.WHITE)
                .setFooter("Press exit to leave setup")
                .setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Setup");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Mod or HRL\n";
        commandDescription += "Syntax: ;setup\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nDisplays the setup menu for a server!" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

}
