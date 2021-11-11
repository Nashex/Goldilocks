package commands.miscCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.VoiceChannel;
import utils.ClearVc;

import java.util.Date;

public class CommandClearVc extends Command {
    public CommandClearVc() {
        setAliases(new String[] {"clean","clear"});
        setEligibleRoles(new String[] {"rl","security","eo"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.RAID);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        VoiceChannel voiceChannel;
        if (!msg.getMember().getVoiceState().inVoiceChannel()) {
            msg.getTextChannel().sendMessage("You must be in a voice channel to use this command.").queue();
            return;
        }
        voiceChannel = msg.getMember().getVoiceState().getChannel();

        ClearVc.clearVc(msg.getTextChannel(), voiceChannel);

    }

    private EmbedBuilder queueEmbed(VoiceChannel voiceChannel, String percentageBar, String percent) {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setColor(Goldilocks.YELLOW);
        embedBuilder.setDescription("\n **Moving Members from " + voiceChannel.getName() + ":**\n"
                + percentageBar + " | **" + percent +"%**");
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Ping");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Trial Security\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nChecks if the bot is online." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
