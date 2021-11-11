package commands.miscCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import utils.Utils;

import java.util.Date;

public class CommandTopRuns extends Command {
    public CommandTopRuns() {
        setAliases(new String[] {"topruns"});
        setEligibleRoles(new String[] {"verified"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.VERIFIED);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        Guild guild = msg.getGuild();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Top Run Completes in " + guild.getName() + ":")
                .setColor(Goldilocks.GOLD)
                .setThumbnail(guild.getIconUrl());

        String topruns[][] = Database.getTopRuns(guild.getId());
        boolean containsTop = false;

        for (int i = 0; i < 10; i++) {
            try {
                if (guild.getMemberById(topruns[i][0]).equals(msg.getMember().getId())) {
                    containsTop = true;
                }
                embedBuilder.addField(i+1 + ". " + guild.getMemberById(topruns[i][0]).getEffectiveName(), "with " + topruns[i][1] + " runs!", false);
            } catch (Exception e) {
                embedBuilder.addField(i+1 + ". " + topruns[i][0] + " is no longer in the server", "with " + topruns[i][1] + " runs!", false);
            }
        }

        embedBuilder.addBlankField(false);

        if (!containsTop) {
            String runsPlace[][] = Database.getRunsPlace(msg.getMember().getId(), guild.getId());
            for (int i = 0; i < 3; i++) {
                try {
                    embedBuilder.addField(runsPlace[i][0] + ". " + guild.getMemberById(runsPlace[i][1]).getEffectiveName(), "with " + runsPlace[i][2] + " runs!", false);
                } catch (Exception e) {
                    embedBuilder.addField(runsPlace[i][0] + ". " + runsPlace[i][1] + " is no longer in the server", "with " + runsPlace[i][2] + " runs!", false);
                    //e.printStackTrace();
                }
            }
        }

        embedBuilder.setFooter("© Pest Control Administration", "https://cdn.discordapp.com/icons/514788290809954305/b5fd5a35617a751d9860116c7a433a58.webp?size=1024");
        embedBuilder.setTimestamp(new Date().toInstant());
        Utils.sendEmbed(msg.getTextChannel(), embedBuilder);

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Top Runs");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Verified\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nShows the ranking list of most runs done in pest control!" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
