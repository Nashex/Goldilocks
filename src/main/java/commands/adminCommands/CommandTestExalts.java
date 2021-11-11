package commands.adminCommands;

import com.cloudinary.utils.ObjectUtils;
import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import parsing.ImageOcr;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static main.Goldilocks.cloudinary;

public class CommandTestExalts extends Command {
    public CommandTestExalts() {
        setAliases(new String[] {"exalts"});
        setEligibleRoles(new String[] {"developer"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.DEVELOPER);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        Message message = msg;

        if (message.getAttachments().isEmpty()) return;
        if (args.length < 1) return;

        Message.Attachment attachment =  message.getAttachments().get(0);

        Long timeStarted = System.currentTimeMillis();

        Message result = message.getTextChannel().sendMessage("Processing Image...").complete();

        attachment.downloadToFile("parseImages/" + attachment.getFileName())
                .exceptionally(t ->
                { // handle failure
                    t.printStackTrace();
                    return null;
                }).thenRun(() -> {
            File inputImage = new File("parseImages/" + attachment.getFileName());
            try {
                result.editMessage("Reading image...").queue();

                HashMap<String, Integer> fields = ImageOcr.getPlayerExalts(inputImage.getAbsolutePath(), args[0]);
                String problems = "";

                String parseResult = "";
                for (Map.Entry entrySet : fields.entrySet()) {
                    if (!Arrays.asList(new String[]{"Username", "ExaltPointers", "VaultPointers"}).contains(entrySet.getKey()))
                        parseResult += String.format("%-20s", entrySet.getKey()) + "| " + entrySet.getValue() + "/5\n";
                }
                parseResult += String.format("%-20s", "Username") + "| " + (fields.containsKey("Username") ? "✅" : "❌") + "\n";
                parseResult += String.format("%-20s", "Valid Exalt Page") + "| " + (fields.containsKey("ExaltPointers") ? "✅" : "❌") + "\n";
                parseResult += String.format("%-20s", "Vault Detected") + "| " + (fields.containsKey("VaultPointers") ? "✅" : "❌") + "\n";

                String imageUrl = "";

                try {
                    Map params = ObjectUtils.asMap(
                            "public_id", "vaultPictures/" + result.getId(),
                            "overwrite", true,
                            "resource_type", "image"
                    );
                    Map imageDataMap =  cloudinary.uploader().upload(inputImage, params);
                    imageUrl = (String) imageDataMap.get("url");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setAuthor(message.getMember().getEffectiveName() + "'s Vault Analysis Result", attachment.getUrl(), message.getAuthor().getAvatarUrl())
                        .setColor(Goldilocks.BLUE)
                        .addField("Field Results", "```\n" + parseResult + "\n```",false)
                        .setFooter("Time Taken: " + (System.currentTimeMillis() - timeStarted) / 1000 + " seconds")
                        .setTimestamp(new Date().toInstant());

                embedBuilder.setImage(imageUrl);

                result.editMessage(message.getMember().getEffectiveName() + "'s OCR Test Result").queue();
                result.editMessage(embedBuilder.build()).queue();
                //result.getTextChannel().sendFile(inputImage).queue();

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
        embedBuilder.setTitle("Command: Test Vault");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Developer\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nCAnalyzes a Vault Picture." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
