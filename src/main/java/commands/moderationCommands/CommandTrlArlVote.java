package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import utils.Utils;

import java.util.Date;
import java.util.List;

public class CommandTrlArlVote extends Command {
    public CommandTrlArlVote() {
        setAliases(new String[] {"arlvote"});
        setEligibleRoles(new String[] {"hrl","mod"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.DEBUG);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        if (msg.getMentionedMembers().isEmpty()) {
            Utils.errorMessage("Unable to Create Trl to Arl Poll", "Please mention an arl", msg.getTextChannel(), 5L);
            return;
        }

        Member member = msg.getMentionedMembers().get(0);
        Emote shatters = Goldilocks.jda.getEmoteById("723001214865899532");

        int numRuns = 0;
        int vetRuns = 0;
        float percentage = (numRuns + vetRuns) / 5.0f;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Goldilocks.BLUE)
                .setFooter("ARL-VOTE: " + member.getId())
                .setDescription("**Vote for: **" + member.getAsMention() + "\n" + Utils.renderPercentage(percentage, 10) + " **" + String.format("%.2f", percentage * 100) + "%**");

        Message message = msg.getTextChannel().sendMessage(embedBuilder.build()).complete();
        msg.delete().queue();
        message.addReaction("✅").queue();
        message.addReaction("❌").queue();
        message.addReaction("\uD83C\uDDFB").queue();
        message.addReaction(shatters).queue();

    }

    public static EmbedBuilder renderEmbedBuilder(Message message) {
        Emote shatters = Goldilocks.jda.getEmoteById("723001214865899532");

        Member member = message.getGuild().getMemberById(message.getEmbeds().get(0).getFooter().getText().split("ARL-VOTE: ")[1]);

        int numRuns = 0;
        int vetRuns = 0;
        try {
            numRuns = message.retrieveReactionUsers(shatters).complete().size() - 1;
            List<User> vetUsers = message.retrieveReactionUsers("\uD83C\uDDFB").complete();
            vetRuns = vetUsers.size() - 1;
        } catch (Exception e) { }

        if (numRuns > 4) numRuns = 4;
        if (vetRuns > 1) vetRuns = 1;

        float percentage = (numRuns + vetRuns) / 5.0f;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Goldilocks.BLUE)
                .setFooter("ARL-VOTE: " + member.getId())
                .setDescription("**Vote for: **" + member.getAsMention() + "\n" + Utils.renderPercentage(percentage, 10) + " **" + String.format("%.2f", percentage * 100) + "%**");

        return embedBuilder;
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Trl to Arl Vote");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Head Raid Leader\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nPuts up a vote for a TRL" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

}
