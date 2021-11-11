package commands.adminCommands;

import commands.Command;
import commands.CommandHub;
import io.quickchart.QuickChart;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import quota.DataCollector;
import quota.LogField;
import utils.Charts;
import utils.Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CommandChart extends Command {
    public CommandChart() {
        setAliases(new String[] {"chart","plot"});
        setEligibleRoles(new String[] {"officer", "hrl"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.DEVELOPER);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        //msg.getTextChannel().sendMessage("Data: ```json\n [" + field1.stream().map(f -> "{x: " + f.time + ", y: " + f.value + "}").collect(Collectors.joining(", ")) + "]\n```").queue();
        //msg.getTextChannel().sendMessage("Data: ```json\n [" + field2.stream().map(f -> "{x: " + f.time + ", y: " + f.value + "}").collect(Collectors.joining(", ")) + "]\n```").queue();

        if (args.length == 0) {
            msg.getTextChannel().sendMessage("**Usage:**\n" + Database.getGuildPrefix(msg.getGuild().getId())
                    + "chart [# of time] [s/mi/h/d/mo] <field(s)>\n **Field List:** ```csv\n" + Database.getDistinctFields(msg.getGuild()).stream().collect(Collectors.joining(", ")) + "\n```").queue();
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
            // Output wrong format
        }

        if (args.length > 2 && (args[2].equalsIgnoreCase("prediction") || args[2].equalsIgnoreCase("analyze"))) {
            if (args[2].equalsIgnoreCase("analyze")) {
                msg.getTextChannel().sendMessage(analyzePredictionThresholds(System.currentTimeMillis() - time * 1000)).queue();
            } else {
                if (args.length > 3 && args[3].replaceAll("[^0-9.]", "").equals(args[3])) {
                    float decimal;
                    try {
                        decimal = Float.parseFloat(args[3]);
                        long timeRange = System.currentTimeMillis() - time * 1000;
                        int[] data = getPredictionData(timeRange, decimal);
                        if (data.length == 6) {
                            msg.getTextChannel().sendMessage(Charts.createPieChart(new String[]{"" + data[0], "" + data[1]}, new String[]{"'Correct'", "'Wrong'"}, "Accepted Errors with Threshold " + args[3])).queue();
                            msg.getTextChannel().sendMessage(Charts.createPieChart(new String[]{"" + data[2], "" + data[3]}, new String[]{"'Correct'", "'Wrong'"}, "Denied Errors with Threshold " + args[3])).queue();
                            msg.getTextChannel().sendMessage(Charts.createPieChart(new String[]{"" + data[4], "" + data[5]}, new String[]{"'Predicted'", "'Error Gathering Data'"}, "Prediction Errors")).queue();
                        } else {
                            msg.getTextChannel().sendMessage("Unable to create chart, please try a larger time range.").queue();
                        }
                    } catch (Exception e) {
                        msg.getTextChannel().sendMessage("Invalid decimal number.").queue();
                    }
                } else {
                    msg.getTextChannel().sendMessage("Please use the command as follows: .chart <# of time> <s/mi/h/d/mo> prediction <decimal threshold>").queue();
                }
            }
            return;
        }

        boolean stacked = false;

        List<List<LogField>> fields = new ArrayList<>();
        for (String s : args) {

            if (s.contains("+")) stacked = true;
            List<LogField> field = DataCollector.getData(msg.getGuild(), s, time == 0 ? 0L : System.currentTimeMillis() - (time * 1000));
            if (field.size() > 0) fields.add(field);
        }

        if (fields.isEmpty()) {
            msg.getTextChannel().sendMessage("**Usage:**\n" + Database.getGuildPrefix(msg.getGuild().getId()) + "csv [# of time] [s/mi/h/d/mo] <field(s)>\n **Field List:** ```csv\n" + Database.getDistinctFields(msg.getGuild()).stream().collect(Collectors.joining(", ")) + "\n```").queue();
            return;
        }

        msg.getTextChannel().sendMessage(fields.size() == 2 && !stacked ? utils.Charts.createDualChart(fields) : utils.Charts.createChart(fields, stacked, false)).queue();

    }

    private static String analyzePredictionThresholds(long time) {
        List<Float> thresholds = new ArrayList<>();
        DecimalFormat df = new DecimalFormat("##.##");
        for (float i = 0; i <= 100; i += .1) {
            thresholds.add(i);
        }
        List<Float> errorRate = new ArrayList<>();
        for (Float f : thresholds) {
            int[] data = getPredictionData(time, f);
            errorRate.add((((float) data[0] + data[2]) / (data[4])) * 100.0f);
        }

        String chartUrl;
        QuickChart chart = new QuickChart();
        chart.setWidth(500);
        chart.setHeight(300);
        chart.setBackgroundColor("#202225");
        chart.setConfig("{" +
                "  type: 'line'," +
                "  data: {" +
                "    labels: [" + thresholds.stream().map(f -> "'" + df.format(f) + "'").collect(Collectors.joining(",")) + "]," +
                "    datasets: [" +
                "      {" +
                "        label: 'Alt Prediction'," +
                "        backgroundColor: 'rgb(48, 219, 60)'," +
                "        borderColor: getGradientFillHelper('vertical', ['#00FF00', '#FF0000'],)," +
                "        data: [" + errorRate.stream().map(f -> "'" + f + "'").collect(Collectors.joining(",")) + "]," +
                "        fill: false," +
                "        yAxisID: 'A'," +
                "      }," +
                "    ]," +
                "  }," +
                "  options: {" +
                "     elements: {" +
                "          point: {" +
                "             radius: 1" +
                "          }" +
                "     }," +
                "    legend: {" +
                "          display: true," +
                "          labels: {" +
                "            fontColor: 'white'," +
                "        }," +
                "    }," +
                "    scales: {" +
                "      yAxes: [{" +
                "         scaleLabel: {" +
                "            labelString: 'Accuracy Rate'," +
                "            beginAtZero: true," +
                "            display: true," +
                "            fontColor: 'white'," +
                "         },  " +
                "        id: 'A'," +
                "        ticks: {" +
                "          min: 0," +
                "          max: 100," +
                "          stepSize: 10," +
                "          fontColor: 'white'," +
                "          callback: (val) => {" +
                "            return val + '%' " +
                "          }," +
                "        }" +
                "      }]," +
                "      xAxes: [{" +
                "         scaleLabel: {" +
                "            labelString: 'Threshold'," +
                "            beginAtZero: true," +
                "            display: true," +
                "            fontColor: 'white'," +
                "         },  " +
                "         ticks: {" +
                "            beginAtZero: false," +
                "            fontColor: 'white'," +
                "            display: true" +
                "         }," +
                "         gridLines: {" +
                "            display: false" +
                "         }," +
                "      }]" +
                "    }," +
                "    title: {" +
                "      display: true," +
                "      fontColor: 'white'," +
                "      fontSize: '16'," +
                "      text: 'Veri Pending Alt Prediction Analysis'," +
                "    }," +
                "  }," +
                "}");
        chartUrl = chart.getShortUrl();
        return chartUrl;
    }

    private static int[] getPredictionData(long time, float decimal) {
        int[] data = new int[6];
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:database.db");
             Statement stmt = conn.createStatement()){
            //Accepted
            ResultSet resultSet = stmt.executeQuery("SELECT Count(userId) FROM viVeriPending WHERE (prob >= " + decimal + " AND accepted = '1') AND time > " + time);
            int correct = resultSet.getInt(1);
            data[0] = correct;
            resultSet = stmt.executeQuery("SELECT Count(userId) FROM viVeriPending WHERE (prob >= " + decimal + " AND accepted = '0') AND time > " + time);
            int wrong = resultSet.getInt(1);
            data[1] = wrong;

            //Denied
            resultSet = stmt.executeQuery("SELECT Count(userId) FROM viVeriPending WHERE (prob < " + decimal + " AND accepted = '0') AND time > " + time);
            correct = resultSet.getInt(1);
            data[2] = correct;
            resultSet = stmt.executeQuery("SELECT Count(userId) FROM viVeriPending WHERE (prob < " + decimal + " AND accepted = '1') AND time > " + time);
            //System.out.println("SELECT Count(userId) FROM viVeriPending WHERE (prob < " + decimal + " AND accepted = '1') AND time > " + time);
            wrong = resultSet.getInt(1);
            data[3] = wrong;

            resultSet = stmt.executeQuery("SELECT Count(userId) FROM viVeriPending WHERE time > " + time);
            int total = resultSet.getInt(1);
            data[4] = total;
            resultSet = stmt.executeQuery("SELECT Count(userId) FROM viVeriPending WHERE prob ISNULL AND time > " + time);
            data[5] = resultSet.getInt(1);
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Database connection error.");
        }
        return new int[0];
    }


    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Chart");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Officer or Developer\n";
        commandDescription += "Syntax: .chart <# of time> <s/mi/h/d/w/mo> [fields...]\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nRetrieves a chart of data collected for the server." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
