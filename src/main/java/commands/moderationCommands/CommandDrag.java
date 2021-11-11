package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import utils.MemberSearch;
import utils.Utils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CommandDrag extends Command {

    public CommandDrag() {
        setAliases(new String[] {"drag", "d"});
        setEligibleRoles(new String[] {"arl", "tSec", "eo"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        Guild guild = msg.getGuild();
        Member member = msg.getMember();
        TextChannel textChannel = msg.getTextChannel();

        VoiceChannel voiceChannel = member.getVoiceState().getChannel();
        List<Member> members = new CopyOnWriteArrayList<>();

        for (String string : args) {
            List<Member> tempMembers = MemberSearch.memberSearch(msg, new String[] {string});
            if (!tempMembers.isEmpty()) {
                members.add(tempMembers.get(0));
            }
        }

        msg.delete().queue();

        if (members.isEmpty()) {
            Utils.errorMessage("Failed to Drag Raider", "Could not find the raider(s) you were looking for. Please use the command in the following format: drag <name/@/id>." +
                    " For multiple raiders: drag <name/@/id> <name/@/id>...", msg.getTextChannel(), 15L);
            return;
        }

        if (!member.getVoiceState().inVoiceChannel()) {
            Utils.errorMessage("Failed to Drag Raider", "Please make sure you are in a voice channel prior to dragging raiders.", msg.getTextChannel(), 10L);
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Goldilocks.BLUE)
                .setTimestamp(new Date().toInstant());

        String dragDescription = "";
        int successfulDrags = 0;

        for (Member target : members) {
            dragDescription += "\n" + target.getAsMention() + " | Dragged: ";
            try {
                if (target.getVoiceState().inVoiceChannel()) {
                    guild.moveVoiceMember(target, voiceChannel).queue();
                    dragDescription += "✅";
                    successfulDrags++;
                } else {
                    dragDescription += "❌";
                }
            } catch (Exception e) {
                dragDescription += "❌";
            }
        }

        embedBuilder.setTitle(member.getEffectiveName() + " Successfully Dragged " + successfulDrags + " User" + (successfulDrags == 1 ? "" : "s"))
                .setDescription("Raiders dragged to `" + voiceChannel.getName() + "`\n" + dragDescription);

        textChannel.sendMessage(embedBuilder.build()).queue();

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Drag");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Trial Security / Almost Raid Leader\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nDrags a user." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

}
