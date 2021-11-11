package commands.adminCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Date;

public class CommandRunsVerificationEmbed extends Command {
    public CommandRunsVerificationEmbed() {
        setAliases(new String[] {"runsembed"});
        setEligibleRoles(new String[] {""});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.SETUP);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        Guild guild = msg.getGuild();
        TextChannel textChannel = msg.getTextChannel();

        if (!Database.getGuildVerificationChannels(guild.getId()).contains(textChannel.getId())) {
            return;
        }


        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Verify as Indiana Jones with Passage of Life!");
        embedBuilder.setColor(Goldilocks.GOLD);
        embedBuilder.setDescription("In order to get verified please make sure your RealmEye page meets the following requirements:");
        embedBuilder.addField("Requirements:", "```fix\n◽ 50 Dungeon Completes with Passage of Life\n◽ Public RealmEye Page\n◽ Private Location\n```", false);
        //embedBuilder.addField("Optional Requirements:", "If you meet the following reqs, you do not have to meet the reqs listed above.\n```fix\n◽ 25 Nest Completes```", false);
        //embedBuilder.addField("REQUIREMENTS", "20 Stars\n2000 Alive Fame\n1 - 6/8 Class", true);
        embedBuilder.addField("\nInstructions", "If you meet these requirements, feel free to react with ✅ in order to verify.", false);
        //embedBuilder.addField("📝 Example Verification:", "```fix\n-exverify\n```", false);



        Message verifyMessage = textChannel.sendMessage(embedBuilder.build()).complete();;
        verifyMessage.addReaction("✅").queue();


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
