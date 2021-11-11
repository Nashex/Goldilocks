package commands.adminCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Date;

public class CommandDungeonVerifyEmbed extends Command {
    public CommandDungeonVerifyEmbed() {
        setAliases(new String[] {"dungeonverifyembed","dve"});
        setEligibleRoles(new String[] {""});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.SETUP);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        Guild guild = msg.getGuild();

        if (!guild.getId().equals("762883845925109781")) {
            return;
        }

        TextChannel verificationChannel = guild.getTextChannelById("767942353784012840");

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Dungeon Verify with G.O.L.D!");
        embedBuilder.setColor(Goldilocks.GOLD);
        embedBuilder.setDescription("In order to get verified please make sure your RealmEye page meets the following requirements:");
        embedBuilder.addField("Requirements:", "```fix\n◽ 10 Dungeon Completes of the Dungeon you are verifying for\n◽ Public RealmEye \n```", false);
        embedBuilder.addField("\nInstructions", "If you meet these requirements, react with the dungeon you want to verify for to begin the verification process.", false);

        try {
            verificationChannel.deleteMessages(verificationChannel.getHistory().retrievePast(99).complete()).queue();
        } catch (Exception e) {
            verificationChannel.deleteMessageById(verificationChannel.getLatestMessageId()).queue();
        }

        Emote voids = Goldilocks.jda.getEmoteById(767811845947654204L);
        Emote cults = Goldilocks.jda.getEmoteById(766907072607682560L);
        Emote shatters = Goldilocks.jda.getEmoteById(723001214865899532L);
        Emote nest = Goldilocks.jda.getEmoteById(723001215407095899L);
        Emote fungal = Goldilocks.jda.getEmoteById(723001215696240660L);
        Emote amongUs = Goldilocks.jda.getEmoteById(767656425740304385L);

        Message verifyMessage = verificationChannel.sendMessage(embedBuilder.build()).complete();
        verifyMessage.addReaction(voids).queue();
        verifyMessage.addReaction(cults).queue();
        verifyMessage.addReaction(shatters).queue();
        verifyMessage.addReaction(nest).queue();
        verifyMessage.addReaction(fungal).queue();
        verifyMessage.addReaction(amongUs).queue();

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
