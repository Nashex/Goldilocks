package commands.adminCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import quota.DataCollector;
import quota.LogField;
import utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CommandCSV extends Command {
    public CommandCSV() {
        setAliases(new String[] {"csv"});
        setEligibleRoles(new String[] {"officer", "hrl"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.DEVELOPER);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        //msg.getTextChannel().sendMessage("Data: ```json\n [" + field1.stream().map(f -> "{x: " + f.time + ", y: " + f.value + "}").collect(Collectors.joining(", ")) + "]\n```").queue();
        //msg.getTextChannel().sendMessage("Data: ```json\n [" + field2.stream().map(f -> "{x: " + f.time + ", y: " + f.value + "}").collect(Collectors.joining(", ")) + "]\n```").queue();

        if (args.length == 0) {
            msg.getTextChannel().sendMessage("**Usage:**\n" + Database.getGuildPrefix(msg.getGuild().getId()) + "csv [# of time] [s/mi/h/d/mo] <field(s)>\n **Field List:** ```csv\n" + Database.getDistinctFields(msg.getGuild()).stream().collect(Collectors.joining(", ")) + "\n```").queue();
            return;
        }

        long time = 0;
        String[] times = {"w", "d", "mo", "h", "mi", "s"};
        if (Utils.isNumeric(args[0]) && Arrays.asList(times).stream().anyMatch(s -> args[1].startsWith(s))) {
            int multiplier = Integer.parseInt(args[0]);
            String prefix = Arrays.asList(times).stream().filter(s -> args[1].startsWith(s)).collect(Collectors.toList()).get(0);
            switch (prefix) {
                case "w":
                    time = multiplier * 604800;
                    break;
                case "mo":
                    time = multiplier * 2628000;
                    break;
                case "h":
                    time = multiplier * 3600;
                    break;
                case "d":
                    time = multiplier * 86400;
                    break;
                case "mi":
                    time = multiplier * 60;
                    break;
                default: //Seconds
                    time = multiplier;
                    break;
            }
        } else {
            //Output wrong format
        }

        List<List<LogField>> fields = new ArrayList<>();
        for (String s : args) {
            List<LogField> field = DataCollector.getData(msg.getGuild(), s, time == 0 ? 0L : System.currentTimeMillis() - (time * 1000));
            if (field.size() > 0) fields.add(field);
        }

        if (fields.isEmpty()) {
            msg.getTextChannel().sendMessage("**Usage:**\n" + Database.getGuildPrefix(msg.getGuild().getId()) + "csv [# of time] [s/mi/h/d/mo] <field(s)>\n **Field List:** ```csv\n" + Database.getDistinctFields(msg.getGuild()).stream().collect(Collectors.joining(", ")) + "\n```").queue();
            return;
        }

        try {
            FileWriter csvWriter = new FileWriter("csvfiles/data" + msg.getId() + ".csv");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            String csvTitle ="Time," + fields.stream().map(fields1 -> fields1.get(0).name).collect(Collectors.joining(",")) + "\n";
            String csvData = "";
            for (int i = 0; i < fields.get(0).size(); i++) {
                csvData += sdf.format((fields.get(0).get(i).time + 7200 * 1000)) + ",";
                int finalI = i;
                csvData += fields.stream().map(fields1 -> (finalI >= fields1.size() ? "0" : String.valueOf(fields1.get(finalI).value))).collect(Collectors.joining(","));
                csvData += "\n";
            }
            csvWriter.append(csvTitle);
            csvWriter.append(csvData);

            csvWriter.flush();
            csvWriter.close();

            File csv = new File("csvfiles/data" + msg.getId() + ".csv");
            msg.getTextChannel().sendMessage("Data for " + String.join(" ", args)).addFile(csv).queue();

        } catch (Exception e) {
            msg.getTextChannel().sendMessage("Error collecting data for " + String.join(" ", args)).queue();
        }

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: CSV");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Officer or Developer\n";
        commandDescription += "Syntax: .csv <# of time> <s/mi/h/d/w/mo> [fields...]\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nRetrieves a CSV of data collected for the server." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
