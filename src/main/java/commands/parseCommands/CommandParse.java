package commands.parseCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import parsing.Parse;
import utils.Utils;

import java.io.File;
import java.util.Date;

public class CommandParse extends Command {
    public CommandParse() {
        setAliases(new String[] {"fullparse"});
        setEligibleRoles(new String[] {"developer"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.PARSE);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        if (msg.getAttachments().size() == 0) {
            Utils.errorMessage("Cannot Parse VC", "User did not attach an image to the `parsevc` command", msg.getTextChannel(), 10L);
            return;
        }
        if (!msg.getMember().getVoiceState().inVoiceChannel()) {
            Utils.errorMessage("Cannot Parse VC", "User is not currently in voice channel", msg.getTextChannel(), 10L);
            return;
        }

        VoiceChannel voiceChannel = msg.getMember().getVoiceState().getChannel();
        TextChannel textChannel = msg.getTextChannel();
        Member member = msg.getMember();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Processing " + member.getEffectiveName() + "'s Parse Image");
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("```\nProcessing players in the image...\n```");
        embedBuilder.setTimestamp(new Date().toInstant());

        Message.Attachment attachment =  msg.getAttachments().get(0);
        Message message = textChannel.sendMessage(embedBuilder.build()).complete();
        attachment.downloadToFile("parseImages/" + attachment.getFileName())
                .exceptionally(t ->
                { // handle failure
                    t.printStackTrace();
                    return null;
                }).thenAccept(parse -> {

            //Imageocr
            File inputImage = new File("parseImages/" + attachment.getFileName());
            Parse.parse(voiceChannel, textChannel, member, inputImage, message);

        });

        Database.addParse(msg.getMember());

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Parse");
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
