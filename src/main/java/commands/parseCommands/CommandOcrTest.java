package commands.parseCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import parsing.ImageOcr;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class CommandOcrTest extends Command {
    public CommandOcrTest() {
        setAliases(new String[] {"ocrtest"});
        setEligibleRoles(new String[] {"admin"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.PARSE);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        Message.Attachment attachment =  msg.getAttachments().get(0);

        attachment.downloadToFile("parseImages/" + attachment.getFileName())
                .exceptionally(t ->
                { // handle failure
                    t.printStackTrace();
                    return null;
                }).thenRun(() -> {
                    File inputImage = new File("parseImages/" + attachment.getFileName());
            try {
                msg.getTextChannel().sendMessage("Parsing image. 🤡").complete().editMessage(ImageOcr.detectText(inputImage.getAbsolutePath())).queue();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

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
