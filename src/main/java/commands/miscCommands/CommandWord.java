package commands.miscCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Date;
import java.util.List;
import java.util.Random;

public class CommandWord extends Command {
    public CommandWord() {
        setAliases(new String[] {"word"});
        setEligibleRoles(new String[] {"arl","security","eo"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.GAME);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        Random random = new Random();
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Goldilocks.WHITE);
        if (args.length > 0) {
            try {
                Document doc = Jsoup.connect("https://www.urbandictionary.com/define.php?term=" + String.join("%20", args)).get();

                List<Element> elements = doc.getElementsByClass("def-panel ");
                Element e = elements.get(0);
                embedBuilder.setTitle(e.getElementsByClass("def-header").first().text())
                        .addField("Meaning", e.getElementsByClass("meaning").html().replace("<br>", "\n")
                                .replaceAll("<.*?\\>", "").replaceAll("[\n]{2,}", "\n"), false)
                        .addField("Example", e.getElementsByClass("example").first().html().replace("<br>", "\n")
                                .replaceAll("<.*?\\>", "").replaceAll("[\n]{2,}", "\n")
                                .replaceAll("(Example [0-9]:)", "__**$1**__"), false);

                msg.getTextChannel().sendMessage(embedBuilder.build()).queue();

            } catch (Exception e) {
                msg.getTextChannel().sendMessage("I am unable to find a definition for this word.").queue();
            }
        } else {
            try {
                Document doc = Jsoup.connect("https://www.urbandictionary.com/?page=" + (1 + random.nextInt(100))).get();

                List<Element> elements = doc.getElementsByClass("def-panel ");
                Element e = elements.get(random.nextInt(elements.size() - 1));
                embedBuilder.setTitle(e.getElementsByClass("def-header").first().text())
                        .addField("Meaning", e.getElementsByClass("meaning").text().replaceAll("([0-9][.] )", "\n"), false)
                        .addField("Example", e.getElementsByClass("example").first().text(), false);

                msg.getTextChannel().sendMessage(embedBuilder.build()).queue();

            } catch (Exception e) { }
        }
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Word");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Security, Almost Raid Leader, Event Organizer\n";
        commandDescription += "Syntax: ;word\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nGets a random word from Urban Dictionary's word of the day." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
