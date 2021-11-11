package commands.adminCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommandVerifyEmbed extends Command {
    public CommandVerifyEmbed() {
        setAliases(new String[] {"verifyembed"});
        setEligibleRoles(new String[] {"developer"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.SETUP);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        Guild guild = msg.getGuild();
        List<Integer> verificationReqs = new ArrayList<>();

//        if (verificationReqs.isEmpty() && args.length == 0) {
//            Utils.errorMessage("Failed to Create Verification Embed", "This is not a valid verification channel.", msg.getTextChannel(), 10L);
//            return;
//        }

        TextChannel verificationChannel = msg.getTextChannel();

        //Todo add guild description

        if (args.length > 0 && args[0].equals("main")) {
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("Verify with " + guild.getName())
                    .setColor(Goldilocks.BLUE)
                    .setDescription("Welcome to " + msg.getGuild().getName() + "! In order to get verified please make sure you meet the following requirements:" +
                            "```excel\n" +
                            "↳ You are not verifying an Alt Account\n" +
                            "↳ Public RealmEye Profile\n" +
                            "↳ All Settings set to Public\n" +
                            "↳ Private Last Seen Location\n" +
                            "```\nOnce you are sure that you meet these requirements, react with ✅ to start the verification process. If you do not receive a message please make sure to" +
                            " public your dms from this server.");
            verificationChannel.sendMessage(embedBuilder.build()).queue(m -> m.addReaction("✅").queue());
            return;
        }

        if (args.length > 0 && args[0].equals("fungal")) {
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("Verify with " + guild.getName())
                    .setColor(Goldilocks.BLUE)
                    .setDescription("Welcome to " + msg.getGuild().getName() + "! In order to get verified please make sure you meet the following requirements:" +
                            "```excel\n" +
                            "↳ You are not verifying an Alt Account\n" +
                            "↳ 20+ Stars\n" +
                            "↳ 500 Total Alive Fame\n" +
                            "↳ 2+ Months old Account\n" +
                            "↳ Public RealmEye Profile\n" +
                            "↳ All Settings set to Public\n" +
                            "↳ Private Last Seen Location\n" +
                            "```\nOnce you are sure that you meet these requirements, react with ✅ to start the verification process. If you do not receive a message please make sure to" +
                            " public your dms from this server.");
            verificationChannel.sendMessage(embedBuilder.build()).queue(m -> m.addReaction("✅").queue());
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Verify with " + guild.getName());
        embedBuilder.setColor(new Color(158, 119, 0));
        embedBuilder.setDescription("Welcome to " + guild.getName() + "! In order to get verified please make sure your RealmEye page meets the following requirements:");
        embedBuilder.addField("Requirements:", "```fix\n◽ " + verificationReqs.get(0) + " Stars" + (verificationReqs.get(1) != 0 ? "\n◽ " + verificationReqs.get(1) + " Alive Fame" : "")
                + (verificationReqs.get(2) != 0 ? "\n◽ 1x " + verificationReqs.get(2) + "/8 Any Class" : "") + "\n◽ Public RealmEye Page\n◽ Private Location\n```", false);
        embedBuilder.addField("\nInstructions", "If you meet these requirements, react with ✅ to begin the verification process."
                + "\nAfterwards please follow the instructions " + guild.getSelfMember().getAsMention() + " messages you!\n", false);
        //embedBuilder.addField("📝 Example Verification:", "```fix\n" + guildPrefix + "verify\n```", false);

        String verificationChannelId = Database.getVerificationChannel(guild.getId());

        if (!verificationChannel.equals("0")) {
            embedBuilder.addField("Manual Verification", "If you would like to be manually verified, please react with <:ManualVerification:793326541454704650> and check the messages " +
                    Goldilocks.jda.getSelfUser().getAsMention() + " sends you!", false);
        }

        try {
            verificationChannel.purgeMessages(verificationChannel.getHistory().retrievePast(99).complete());
        } catch (Exception e) {
            verificationChannel.deleteMessageById(verificationChannel.getLatestMessageId()).queue();
        }
        Message verificationMessage = verificationChannel.sendMessage(embedBuilder.build()).complete();
        verificationMessage.addReaction("✅").queue();
        verificationMessage.addReaction(Goldilocks.jda.getEmoteById(793326541454704650L)).queue();
        verificationMessage.pin().queue();
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Verify Embed");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Admin\n";
        commandDescription += "Syntax: ;alias <command alias>\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nCreates a general verification embed" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

}
