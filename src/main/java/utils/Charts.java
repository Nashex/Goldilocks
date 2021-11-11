package utils;

import io.quickchart.QuickChart;
import main.Database;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.apache.commons.lang3.StringUtils;
import quota.DataCollector;
import quota.LogField;
import raids.DungeonInfo;
import verification.GraveyardSummary;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class Charts {

    public static String createChart(Database.EventType eventType, Guild guild) {
        String chartUrl = "";

        String labelString = "['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']";
        String dataString = Database.getData(eventType, guild);

        QuickChart chart = new QuickChart();
        chart.setWidth(500);
        chart.setHeight(300);
        chart.setConfig("{"
                + "    type: 'line',"
                + "    data: {"
                + "        labels: " + labelString + ","
                + "        datasets: [{"
                + "            label: 'Runs',"
                + "            data: " + dataString + ","
                + "            fill: false,"
                + "            backgroundColor: 'rgba(255, 99, 132, 0.5)',"
                + "            borderColor: 'rgb(255, 99, 132)',"
                + "        }]"
                + "    }"
                + "}"
        );

        System.out.println(Database.getData(eventType, guild));
        chartUrl = chart.getShortUrl();

        return chartUrl;
    }

    public static String createQuotaChart(Guild guild) {
        String chartUrl;
        String labels = "[";
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd");

        for (int i = 6; i >= 0; i--) {
            Date date = new Date(System.currentTimeMillis() - (86400000 * i));
            labels += "'" + formatter.format(date) + "',";
        }
        labels += "]";

        QuickChart chart = new QuickChart();
        chart.setWidth(500);
        chart.setHeight(300);
        chart.setBackgroundColor("#202225");
        chart.setConfig("{"
                + "    type: 'line',"
                + "    data: {"
                + "        labels: " + labels + ","
                + "        datasets: [{"
                + "            label: 'Runs',"
                + "            data: " + Database.getData(Database.EventType.RAID, guild) + ","
                + "            fill: false,"
                + "            backgroundColor: 'rgba(255, 99, 132, 0.5)',"
                + "            borderColor: 'rgb(255, 99, 132)',"
                + "        },"
                + "        {"
                + "            label: 'Parses',"
                + "            data: " + Database.getData(Database.EventType.PARSE, guild) + ","
                + "            fill: false,"
                + "            backgroundColor: 'rgba(99, 255, 132, 0.5)',"
                + "            borderColor: 'rgb(99, 255, 132)',"
                + "        },"
                + "        {"
                + "            label: 'Assists',"
                + "            data: " + Database.getData(Database.EventType.ASSIST, guild) + ","
                + "            fill: false,"
                + "            backgroundColor: 'rgba(99, 132, 255, 0.5)',"
                + "            borderColor: 'rgb(99, 132, 255)',"
                + "        }]"
                + "    },"
                + "    options: { "
                + "      title: { "
                + "        display: true,"
                + "        fontColor: 'white',"
                + "        fontSize: '16',"
                + "        text: '" + guild.getName() + " Run History" + "',"
                + "     }, legend: {"
                + "          display: true,"
                + "          labels: {"
                + "            fontColor: 'white',"
                + "        },"
                + "     }, scales: {"
                + "            yAxes: [{"
                + "              scaleLabel: {"
                + "                beginAtZero: true,"
                + "                display: true,"
                + "                fontColor: 'white',"
                + "              },  "
                + "              gridLines: {"
                + "                color: 'rgba(0, 0, 0, 0)',"
                + "              },"
                + "              id: 'A',"
                + "              ticks: {"
                + "                fontColor: 'white',"
                + "                display: true,"
                + "                precision: '0',"
                + "              }"
                + "            }],"
                + "            xAxes: [{"
                + "                ticks: {"
                + "                    beginAtZero: true,"
                + "                    fontColor: 'white',"
                + "                    display: true"
                + "                },"
                + "                gridLines: {"
                + "                    display: false"
                + "                },"
                + "            }]"
                + "          },"
                + "     },"
                + "}"
        );

        chartUrl = chart.getShortUrl();

        return chartUrl;
    }

    public static String createParseChart(Guild guild) {
        String chartUrl;
        String labels = "[";
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd");

        for (int i = 6; i >= 0; i--) {
            Date date = new Date(System.currentTimeMillis() - (86400000 * i));
            labels += "'" + formatter.format(date) + "',";
        }
        labels += "]";

        QuickChart chart = new QuickChart();
        chart.setWidth(500);
        chart.setHeight(300);
        chart.setBackgroundColor("#202225");
        chart.setConfig("{"
                + "    type: 'line',"
                + "    data: {"
                + "        labels: " + labels + ","
                + "        datasets: [{"
                + "            label: 'Parses',"
                + "            data: " + Database.getData(Database.EventType.PARSE, guild) + ","
                + "            fill: false,"
                + "            backgroundColor: 'rgba(99, 255, 132, 0.5)',"
                + "            borderColor: 'rgb(99, 255, 132)',"
                + "        }]"
                + "    },"
                + "    options: { "
                + "      title: { "
                + "        display: true,"
                + "        fontColor: 'white',"
                + "        fontSize: '16',"
                + "        text: '" + guild.getName() + " Parsing History" + "',"
                + "     }, legend: {"
                + "          display: true,"
                + "          labels: {"
                + "            fontColor: 'white',"
                + "        },"
                + "     }, scales: {"
                + "            yAxes: [{"
                + "              scaleLabel: {"
                + "                beginAtZero: true,"
                + "                display: true,"
                + "                fontColor: 'white',"
                + "              },  "
                + "              gridLines: {"
                + "                color: 'rgba(0, 0, 0, 0)',"
                + "              },"
                + "              id: 'A',"
                + "              ticks: {"
                + "                fontColor: 'white',"
                + "                display: true,"
                + "                precision: '0',"
                + "              }"
                + "            }],"
                + "            xAxes: [{"
                + "                ticks: {"
                + "                    beginAtZero: true,"
                + "                    fontColor: 'white',"
                + "                    display: true"
                + "                },"
                + "                gridLines: {"
                + "                    display: false"
                + "                },"
                + "            }]"
                + "          },"
                + "     },"
                + "}"
        );

        chartUrl = chart.getShortUrl();

        return chartUrl;
    }

    public static String createParseAssistChart(Guild guild) {
        String chartUrl;
        String labels = "[";
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd");

        for (int i = 6; i >= 0; i--) {
            Date date = new Date(System.currentTimeMillis() - (86400000 * i));
            labels += "'" + formatter.format(date) + "',";
        }
        labels += "]";

        QuickChart chart = new QuickChart();
        chart.setWidth(500);
        chart.setHeight(300);
        chart.setBackgroundColor("#202225");
        chart.setConfig("{"
                + "    type: 'line',"
                + "    data: {"
                + "        labels: " + labels + ","
                + "        datasets: [{"
                + "            label: 'Parses',"
                + "            data: " + Database.getData(Database.EventType.PARSE, guild) + ","
                + "            fill: false,"
                + "            backgroundColor: 'rgba(99, 255, 132, 0.5)',"
                + "            borderColor: 'rgb(99, 255, 132)',"
                + "        },"
                + "        {"
                + "            label: 'Assists',"
                + "            data: " + Database.getData(Database.EventType.ASSIST, guild) + ","
                + "            fill: false,"
                + "            backgroundColor: 'rgba(99, 132, 255, 0.5)',"
                + "            borderColor: 'rgb(99, 132, 255)',"
                + "        }]"
                + "    },"
                + "    options: { "
                + "      title: { "
                + "        display: true,"
                + "        fontColor: 'white',"
                + "        fontSize: '16',"
                + "        text: '" + guild.getName() + " Parse and Assist History" + "',"
                + "     }, legend: {"
                + "          display: true,"
                + "          labels: {"
                + "            fontColor: 'white',"
                + "        },"
                + "     }, scales: {"
                + "            yAxes: [{"
                + "              scaleLabel: {"
                + "                beginAtZero: true,"
                + "                display: true,"
                + "                fontColor: 'white',"
                + "              },  "
                + "              gridLines: {"
                + "                color: 'rgba(0, 0, 0, 0)',"
                + "              },"
                + "              id: 'A',"
                + "              ticks: {"
                + "                fontColor: 'white',"
                + "                display: true,"
                + "                precision: '0',"
                + "              }"
                + "            }],"
                + "            xAxes: [{"
                + "                ticks: {"
                + "                    beginAtZero: true,"
                + "                    fontColor: 'white',"
                + "                    display: true"
                + "                },"
                + "                gridLines: {"
                + "                    display: false"
                + "                },"
                + "            }]"
                + "          },"
                + "     },"
                + "}"
        );


        chartUrl = chart.getShortUrl();

        return chartUrl;
    }

    public static String createModerationChart(Guild guild) {
        String chartUrl;
        String labels = "[";
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd");

        for (int i = 6; i >= 0; i--) {
            Date date = new Date(System.currentTimeMillis() - (86400000 * i));
            labels += "'" + formatter.format(date) + "',";
        }
        labels += "]";

        QuickChart chart = new QuickChart();
        chart.setWidth(500);
        chart.setHeight(300);
        chart.setBackgroundColor("#202225");
        chart.setConfig("{"
                + "    type: 'line',"
                + "    data: {"
                + "        labels: " + labels + ","
                + "        datasets: [{"
                + "            label: 'Parses',"
                + "            data: " + Database.getData(Database.EventType.PARSE, guild) + ","
                + "            fill: false,"
                + "            backgroundColor: 'rgba(99, 255, 132, 0.5)',"
                + "            borderColor: 'rgb(99, 255, 132)',"
                + "        },"
                + "        {"
                + "            label: 'Veris',"
                + "            data: " + Database.getData(Database.EventType.VERIFICATION, guild) + ","
                + "            fill: false,"
                + "            backgroundColor: 'rgba(255, 212, 105, 0.5)',"
                + "            borderColor: 'rgb(255, 212, 105)',"
                + "        },"
                + "        {"
                + "            label: 'Alts',"
                + "            data: " + Database.getData(Database.EventType.ADDALT, guild) + ","
                + "            fill: false,"
                + "            backgroundColor: 'rgba(255, 105, 217, 0.5)',"
                + "            borderColor: 'rgb(255, 105, 217)',"
                + "        },"
                + "        {"
                + "            label: 'Name Changes',"
                + "            data: " + Database.getData(Database.EventType.VERIFICATION, guild) + ","
                + "            fill: false,"
                + "            backgroundColor: 'rgba(255, 105, 105, 0.5)',"
                + "            borderColor: 'rgb(255, 105, 105)',"
                + "        },"
                + "        {"
                + "            label: 'Modmails',"
                + "            data: " + Database.getData(Database.EventType.MODMAIL, guild) + ","
                + "            fill: false,"
                + "            backgroundColor: 'rgba(99, 132, 255, 0.5)',"
                + "            borderColor: 'rgb(99, 132, 255)',"
                + "        }]"
                + "    },"
                + "    options: { "
                + "      title: { "
                + "        display: true,"
                + "        fontColor: 'white',"
                + "        fontSize: '16',"
                + "        text: '" + guild.getName() + " Moderation History" + "',"
                + "     }, legend: {"
                + "          display: true,"
                + "          labels: {"
                + "            fontColor: 'white',"
                + "        },"
                + "     }, scales: {"
                + "            yAxes: [{"
                + "              scaleLabel: {"
                + "                beginAtZero: true,"
                + "                display: true,"
                + "                fontColor: 'white',"
                + "              },  "
                + "              gridLines: {"
                + "                color: 'rgba(0, 0, 0, 0)',"
                + "              },"
                + "              id: 'A',"
                + "              ticks: {"
                + "                fontColor: 'white',"
                + "                display: true,"
                + "                precision: '0',"
                + "              }"
                + "            }],"
                + "            xAxes: [{"
                + "                ticks: {"
                + "                    beginAtZero: true,"
                + "                    fontColor: 'white',"
                + "                    display: true"
                + "                },"
                + "                gridLines: {"
                + "                    display: false"
                + "                },"
                + "            }]"
                + "          },"
                + "     },"
                + "}"
        );


        chartUrl = chart.getShortUrl();

        return chartUrl;
    }

    public static List<LogField> compressFields(List<LogField> fields) {
        List<LogField> newFields = new ArrayList<>();
        for (int i = 1; i < fields.size(); i++) {
            if (fields.get(i).value != fields.get(i - 1).value) newFields.add(fields.get(i));
        }
        return newFields;
    }

    public static List<LogField> averageFields(List<LogField> fields) {
        List<LogField> newFields = new ArrayList<>();
        for (int i = 1; i < fields.size(); i+=2) {
            newFields.add(new LogField(fields.get(i).name, (fields.get(i - 1).value + fields.get(i).value) / 2, fields.get(i).time));
        }
        if (newFields.size() > 1000) return averageFields(newFields);
        return newFields;
    }

    public static String createChart(List<List<LogField>> fields, boolean isStacked, boolean isStepped) {
        String chartUrl;

        fields = fields.stream().map(fields1 -> {
            if (fields1.size() > 1000) return averageFields(fields1);
            return fields1;
        }).collect(Collectors.toList());

        String dataSet = fields.stream().map(l -> {
            return    "        {"
                    + "            label: '" + l.get(0).name + " ',"
                    + "            data: " + "[" + l.stream().map(f -> "{x: " + (f.time - 14400 * 1000) + ", y: " + f.value + "}").collect(Collectors.joining(", ")) + "]" + ","
                    + (isStepped ? "            steppedLine: true," : "")
                    + "            fill: false,"
                    + "        }";
        }).collect(Collectors.joining(", "));

        QuickChart chart = new QuickChart();
        chart.setWidth(500);
        chart.setHeight(300);
        chart.setBackgroundColor("#202225");
        chart.setConfig("{"
                + "    type: 'line',"
                + "    data: {"
                + "        datasets: [" + dataSet + "]"
                + "    },"
                + "    options: { "
                + "      title: { "
                + "        display: true,"
                + "        fontColor: 'white',"
                + "        fontSize: '16',"
                + "        text: '" + (isStacked ? "Stacked " : "") + "Plot for: " + fields.stream().map(l -> StringUtils.capitalize(l.get(0).name)).collect(Collectors.joining(" ")) + "',"
                + "     }, legend: {"
                + "          display: true,"
                + "          labels: {"
                + "            fontColor: 'white',"
                + "          },"
                + "     }, elements: {"
                + "          point: {"
                + "             radius: 1"
                + "          }"
                + "     },"
                + "     scales: {"
                + "            yAxes: [{"
                + "              scaleLabel: {"
                + "                labelString: 'Amount',"
                + "                beginAtZero: true,"
                + "                display: true,"
                + "                fontColor: 'white',"
                + "              },  "
                + "             " + (isStacked ? "stacked: 'true'," : "")
                + "              gridLines: {"
                + "                color: 'rgba(0, 0, 0, 0)',"
                + "              },"
                + "              id: 'A',"
                + "              ticks: {"
                + "                beginAtZero: false,"
                + "                fontColor: 'white',"
                + "                display: true,"
                + "                precision: '0',"
                + "              }"
                + "            }],"
                + "            xAxes: [{"
                + "              scaleLabel: {"
                + "                labelString: 'Time (EST)',"
                + "                beginAtZero: true,"
                + "                display: true,"
                + "                fontColor: 'white',"
                + "              },  "
                + "                type: 'time',"
                + "                ticks: {"
                + "                    beginAtZero: false,"
                + "                    fontColor: 'white',"
                + "                    display: true"
                + "                },"
                + "                gridLines: {"
                + "                    display: false"
                + "                },"
                + "            }]"
                + "          },"
                + "     },"
                + "}"
        );

        chartUrl = chart.getShortUrl();

        return chartUrl;
    }

    public static String createBarChart(List<List<LogField>> fields, boolean isStacked) {
        String chartUrl;

        String[][] dungeons = DungeonInfo.oldDungeonInfo();

        String dataSet = fields.stream().map(l -> {
            String[] dungeon = dungeons[Integer.parseInt(l.get(0).name)];
            Color color = new Color(Integer.valueOf(dungeon[5]));
            return    "        {"
                    + "            label: '" + dungeon[3] + " ',"
                    + "            data: " + "[" + l.stream().map(f -> "{x: " + (f.time - 14400 * 1000) + ", y: " + f.value + "}").collect(Collectors.joining(", ")) + "]" + ","
                    + "            fill: true,"
                    + "            borderColor: 'rgba(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ", 1)',"
                    + "            backgroundColor: 'rgba(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ", 0.5)',"
                    + "            borderWidth: 2,"
                    + "        }";
        }).collect(Collectors.joining(", "));

        QuickChart chart = new QuickChart();
        chart.setWidth(500);
        chart.setHeight(300);
        chart.setBackgroundColor("#202225");
        chart.setConfig("{"
                + "    type: 'bar',"
                + "    data: {"
                + "        datasets: [" + dataSet + "]"
                + "    },"
                + "    options: { "
                + "      title: { "
                + "        display: true,"
                + "        fontColor: 'white',"
                + "        fontSize: '16',"
                + "        text: 'This Weeks Run Activity',"
                + "     }, legend: {"
                + "          display: true,"
                + "          labels: {"
                + "            fontColor: 'white',"
                + "          },"
                + "     }, "
                + "     scales: {"
                + "            yAxes: [{"
                + "              scaleLabel: {"
                + "                labelString: 'Amount',"
                + "                display: true,"
                + "                fontColor: 'white',"
                + "              },  "
                + "             " + (isStacked ? "stacked: 'true'," : "")
                + "              gridLines: {"
                + "                color: 'rgba(0, 0, 0, 0)',"
                + "              },"
                + "              id: 'A',"
                + "              ticks: {"
                + "                beginAtZero: true,"
                + "                fontColor: 'white',"
                + "                display: true,"
                + "              }"
                + "            }],"
                + "            xAxes: [{"
                + "              offset: true,"
                + "              scaleLabel: {"
                + "                labelString: 'Date (EST)',"
                + "                beginAtZero: true,"
                + "                display: true,"
                + "                fontColor: 'white',"
                + "              },  "
                + "             " + (isStacked ? "stacked: 'true'," : "")
                + "                type: 'time',"
                + "                time: {"
                + "                   unit: 'day'"
                + "                },"
                + "                ticks: {"
                + "                    fontColor: 'white',"
                + "                    display: true"
                + "                },"
                + "                gridLines: {"
                + "                    display: false"
                + "                },"
                + "            }]"
                + "          },"
                + "          plugins: {"
                + "             datalabels: {"
                + "             anchor: 'center',"
                + "             align: 'center',"
                + "             color: '#fff',"
                + "             font: {"
                + "                weight: 'bold',"
                + "             },"
                + "             formatter: (value) => {"
                + "                return value['y'];"
                + "             },"
                + "         },"
                + "       },"
                + "     },"
                + "}"
        );

        chartUrl = chart.getShortUrl();

        return chartUrl;
    }

    public static File getFameChartFile(List<LogField> field, String fileName) {
        if (field != null && !field.isEmpty()) {
            try {
                createFameChart(field).toFile("data/playerCharts/" + fileName + ".png");
                return new File("data/playerCharts/" + fileName + ".png");
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return new File("data/playerCharts/famehistoryprivate.png");
    }

    public static QuickChart createFameChart(List<LogField> field) {
        String chartUrl;

        String dataSet = "{"
                + "            label: '" + field.get(0).name + " ',"
                + "            data: " + "[" + field.stream().map(f -> "{x: " + (f.time - 14400 * 1000) + ", y: " + f.value + "}").collect(Collectors.joining(", ")) + "]" + ","
                + "            borderColor: 'rgba(255, 140, 0, .8)',"
                + "            backgroundColor: 'rgba(255, 140, 0, 0.1)',"
                + "            steppedLine: true,"
                + "            fill: true,"
                + "        }";

        QuickChart chart = new QuickChart();
        chart.setWidth(400);
        chart.setHeight(300);
        chart.setBackgroundColor("#202225");
        chart.setConfig("{"
                + "    type: 'line',"
                + "    data: {"
                + "        datasets: [" + dataSet + "]"
                + "    },"
                + "    options: { "
                + "      title: { "
                + "        display: true,"
                + "        fontColor: 'white',"
                + "        fontSize: '16',"
                + "        text: 'Plot for: " + field.get(0).name + "',"
                + "     }, legend: {"
                + "          display: true,"
                + "          labels: {"
                + "            fontColor: 'white',"
                + "          },"
                + "     }, elements: {"
                + "          point: {"
                + "             radius: 1"
                + "          }"
                + "     },"
                + "     scales: {"
                + "            yAxes: [{"
                + "              scaleLabel: {"
                + "                labelString: 'Amount',"
                + "                beginAtZero: true,"
                + "                display: true,"
                + "                fontColor: 'white',"
                + "              },  "
                + "              gridLines: {"
                + "                color: 'rgba(0, 0, 0, 0)',"
                + "              },"
                + "              id: 'A',"
                + "              ticks: {"
                + "                beginAtZero: false,"
                + "                fontColor: 'white',"
                + "                display: true,"
                + "                precision: '0',"
                + "              }"
                + "            }],"
                + "            xAxes: [{"
                + "              scaleLabel: {"
                + "                labelString: 'Time (EST)',"
                + "                beginAtZero: true,"
                + "                display: true,"
                + "                fontColor: 'white',"
                + "              },  "
                + "                type: 'time',"
                + "                ticks: {"
                + "                    beginAtZero: false,"
                + "                    fontColor: 'white',"
                + "                    display: true"
                + "                },"
                + "                gridLines: {"
                + "                    display: false"
                + "                },"
                + "            }]"
                + "          },"
                + "     },"
                + "}"
        );

        return chart;
    }

    public static File getDungeonChartFile(GraveyardSummary gs) {
        QuickChart dungeonQuickChart = Charts.createDungeonChart(gs);
        if (dungeonQuickChart != null) {
            try {
                dungeonQuickChart.toFile("data/playerCharts/" + gs.username + "_dungeonChart.png");
                return new File("data/playerCharts/" + gs.username + "_dungeonChart.png");
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return new File("data/playerCharts/default_gs.png");
    }

    public static QuickChart createDungeonChart(GraveyardSummary gs) {
        String chartUrl;

        String dataSet = "";
        try {
            dataSet += gs.getDungeon("Undead Lairs completed").toChartString("UDL", "#6B749988", "#6B7499") + "," +
                    gs.getDungeon("Snake Pits completed").toChartString("S-Pits", "#5CBB5488", "#5CBB54") + "," +
                    gs.getDungeon("Sprite Worlds completed").toChartString("Sprites", "#ffffff88", "#ffffff") + "," +
                    gs.getDungeon("Cultist Hideouts completed").toChartString("Cults", "#C4100088", "#C41000") + "," +
                    gs.getDungeon("Voids completed").toChartString("Voids", "#351E9188", "#351E91") + "," +
                    gs.getDungeon("Nests completed2").toChartString("Nests", "#FF9B1888", "#FF9B18") + "," +
                    gs.getDungeon("Shatters completed1").toChartString("Shatters", "#06540F88", "#borderWidth");
        } catch (NullPointerException e) {
            return null;
            //return "https://res.cloudinary.com/nashex/image/upload/v1616976297/assets/dungeons_luxexn_jzz8qs.png";
        }

        QuickChart chart = new QuickChart();
        chart.setWidth(400);
        chart.setHeight(300);
        chart.setBackgroundColor("#202225");
        chart.setConfig("{" +
                "        type: 'horizontalBar'," +
                "        data: {" +
                "            datasets: [" + dataSet + "]," +
                "        }," +
                "        options: {" +
                "          title: {" +
                "            display: true," +
                "            text: 'Dungeon Completes'," +
                "            fontColor: 'white'," +
                "            fontSize: '16'" +
                "          }," +
                "          gridLines: {" +
                "            display: false" +
                "          }," +
                "          legend: {" +
                "            display: true," +
                "            labels: {" +
                "                fontColor: 'rgba(0, 0, 0, 0)'," +
                "            }" +
                "          }," +
                "          scales: {" +
                "            yAxes: [{" +
                "              scaleLabel: {" +
                "                beginAtZero: false," +
                "                display: true," +
                "                labelString: 'Completes'," +
                "                fontColor: 'white'," +
                "              },  " +
                "              gridLines: {" +
                "                color: 'rgba(0, 0, 0, 0)'," +
                "              }," +
                "              id: 'A'," +
                "              ticks: {" +
                "                fontColor: 'white'," +
                "                display: false" +
                "              }" +
                "            }]," +
                "            xAxes: [{" +
                "                ticks: {" +
                "                    beginAtZero: true," +
                "                    display: true" +
                "                }," +
                "                gridLines: {" +
                "                    display: false" +
                "                }," +
                "            }]" +
                "          }," +
                "          plugins: {" +
                "            backgroundImageUrl: 'https://res.cloudinary.com/nashex/image/upload/v1612256022/assets/dungeons_luxexn.png'," +
                "            datalabels: {" +
                "               anchor: 'center'," +
                "               align: 'center'," +
                "               color: '#fff'," +
                "               font: {" +
                "                  weight: 'bold'," +
                "               }," +
                "            }," +
                "        }," +
                "      }" +
                "}");

        return chart;
    }

    public static String createDualChart(List<List<LogField>> fields) {
        String chartUrl;

        final int[] index = {0};
        String dataSet = fields.stream().map(l -> {
            return    "        {"
                    + "            label: '" + l.get(0).name + " ',"
                    + "            data: " + "[" + l.stream().map(f -> "{x: " + (f.time - 14400 * 1000) + ", y: " + f.value + "}").collect(Collectors.joining(", ")) + "]" + ","
                    + "            fill: false,"
                    + "            yAxisID: 'y" + index[0]++ + "'"
                    + "        }";
        }).collect(Collectors.joining(", "));

        QuickChart chart = new QuickChart();
        chart.setWidth(500);
        chart.setHeight(300);
        chart.setBackgroundColor("#202225");
        chart.setConfig("{"
                + "    type: 'line',"
                + "    data: {"
                + "        datasets: [" + dataSet + "]"
                + "    },"
                + "    options: { "
                + "      title: { "
                + "        display: true,"
                + "        fontColor: 'white',"
                + "        fontSize: '16',"
                + "        text: 'Plot for: " + fields.stream().map(l -> StringUtils.capitalize(l.get(0).name)).collect(Collectors.joining(" ")) + "',"
                + "     }, legend: {"
                + "          display: true,"
                + "          labels: {"
                + "            fontColor: 'white',"
                + "          },"
                + "     }, elements: {"
                + "          point: {"
                + "             radius: 1"
                + "          }"
                + "     },"
                + "     scales: {"
                + "            yAxes: ["
                + "            {"
                + "               id: 'y0',"
                + "               type: 'linear',"
                + "               display: 'true',"
                + "               position: 'left',"
                + "               gridLines: {"
                + "                color: 'rgba(0, 0, 0, 0)',"
                + "               },"
                + "               ticks: {"
                + "                 beginAtZero: false,"
                + "                 fontColor: 'white',"
                + "                 display: true,"
                + "                 precision: '0',"
                + "               },"
                + "               scaleLabel: {"
                + "                 labelString: '" + fields.get(0).get(0).name + "',"
                + "                 beginAtZero: true,"
                + "                 display: true,"
                + "                 fontColor: 'white',"
                + "              },  "
                + "            },"
                + "            {"
                + "               id: 'y1',"
                + "               type: 'linear',"
                + "               display: 'true',"
                + "               position: 'right',"
                + "               gridLines: {"
                + "                 color: 'rgba(0, 0, 0, 0)',"
                + "               },"
                + "               ticks: {"
                + "                 beginAtZero: false,"
                + "                 fontColor: 'white',"
                + "                 display: true,"
                + "                 precision: '0',"
                + "               },"
                + "               scaleLabel: {"
                + "                 labelString: '" + fields.get(1).get(0).name +  "',"
                + "                 beginAtZero: true,"
                + "                 display: true,"
                + "                 fontColor: 'white',"
                + "              },  "
                + "            }"
                + "            ],"
                + "            xAxes: [{"
                + "              scaleLabel: {"
                + "                labelString: 'Time (EST)',"
                + "                beginAtZero: true,"
                + "                display: true,"
                + "                fontColor: 'white',"
                + "              },  "
                + "                type: 'time',"
                + "                ticks: {"
                + "                    beginAtZero: false,"
                + "                    fontColor: 'white',"
                + "                    display: true"
                + "                },"
                + "                gridLines: {"
                + "                    display: false"
                + "                },"
                + "            }]"
                + "          },"
                + "     },"
                + "}"
        );

        chartUrl = chart.getShortUrl();

        return chartUrl;
    }

    public static String createUserChart(Member member) {
        String chartUrl;
        String labels = "[";
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd");

        for (int i = 6; i >= 0; i--) {
            Date date = new Date(System.currentTimeMillis() - (86400000 * i));
            labels += "'" + formatter.format(date) + "',";
        }
        labels += "]";

        QuickChart chart = new QuickChart();
        chart.setWidth(500);
        chart.setHeight(300);
        chart.setBackgroundColor("#202225");
        chart.setConfig("{"
                + "    type: 'line',"
                + "    data: {"
                + "        labels: " + labels + ","
                + "        datasets: [{"
                + "            label: 'Runs',"
                + "            data: " + Database.getData(Database.EventType.RAID, member) + ","
                + "            fill: false,"
                + "            backgroundColor: 'rgba(255, 99, 132, 0.5)',"
                + "            borderColor: 'rgb(255, 99, 132)',"
                + "        },"
                + "        {"
                + "            label: 'Parses',"
                + "            data: " + Database.getData(Database.EventType.PARSE, member) + ","
                + "            fill: false,"
                + "            backgroundColor: 'rgba(99, 255, 132, 0.5)',"
                + "            borderColor: 'rgb(99, 255, 132)',"
                + "        },"
                + "        {"
                + "            label: 'Assists',"
                + "            data: " + Database.getData(Database.EventType.ASSIST, member) + ","
                + "            fill: false,"
                + "            backgroundColor: 'rgba(99, 132, 255, 0.5)',"
                + "            borderColor: 'rgb(99, 132, 255)',"
                + "        }]"
                + "    },"
                + "    options: { "
                + "      title: { "
                + "        display: true,"
                + "        fontColor: 'white',"
                + "        fontSize: '16',"
                + "        text: '" + member.getEffectiveName() + " Activity for the Last 7 Days" + "',"
                + "     }, legend: {"
                + "          display: true,"
                + "          labels: {"
                + "            fontColor: 'white',"
                + "        },"
                + "     }, scales: {"
                + "            yAxes: [{"
                + "              scaleLabel: {"
                + "                beginAtZero: true,"
                + "                display: true,"
                + "                fontColor: 'white',"
                + "              },  "
                + "              gridLines: {"
                + "                color: 'rgba(0, 0, 0, 0)',"
                + "              },"
                + "              id: 'A',"
                + "              ticks: {"
                + "                fontColor: 'white',"
                + "                display: true,"
                + "                precision: '0',"
                + "              }"
                + "            }],"
                + "            xAxes: [{"
                + "                ticks: {"
                + "                    beginAtZero: true,"
                + "                    fontColor: 'white',"
                + "                    display: true"
                + "                },"
                + "                gridLines: {"
                + "                    display: false"
                + "                },"
                + "            }]"
                + "          },"
                + "     },"
                + "}"
        );
        chartUrl = chart.getShortUrl();

        return chartUrl;
    }

    public static String createPieChart(String[] data, String[] labels, String title) {
        String chartUrl;
        QuickChart chart = new QuickChart();
        chart.setWidth(500);
        chart.setHeight(300);
        chart.setBackgroundColor("transparent");
        chart.setConfig("{" +
                "  type: 'doughnut'," +
                "  data: {" +
                "    datasets: [" +
                "      {" +
                "        data: [" + String.join(",", data) + "]," +
                "        backgroundColor: [" +
                "          'rgb(48, 219, 60)'," +
                "          'rgb(227, 98, 98)'," +
                "        ]," +
                "        borderColor: 'rgba(0, 0, 0, 0)'," +
                "        label: 'Dataset 1'," +
                "      }," +
                "    ]," +
                "    labels: [" + String.join(",", labels) + "]," +
                "  }," +
                "  options: {" +
                "      title: { " +
                "        display: true," +
                "        fontColor: 'white'," +
                "        fontSize: '16'," +
                "        text: '" + title + "'," +
                "     }, legend: {" +
                "          display: true," +
                "          labels: {" +
                "            fontColor: 'white'," +
                "        }," +
                "     }," +
                "    plugins: {" +
                "      datalabels: {" +
                "        display: true," +
                "        color: '#fff', " +
                "        backgroundColor: '#202225'," +
                "        borderRadius: 3," +
                "        font: {" +
                "          color: 'red'," +
                "          weight: 'bold'," +
                "        }" +
                "      }," +
                "      doughnutlabel: {" +
                "        labels: [{" +
                "          color: '#fff', " +
                "          text: '" + (Integer.parseInt(data[0]) + Integer.parseInt(data[1])) + "'," +
                "          font: {" +
                "            size: 20," +
                "            weight: 'bold'" +
                "          }" +
                "        }, {" +
                "          color: '#fff', " +
                "          text: 'total'" +
                "        }]" +
                "      }" +
                "    }" +
                "  }" +
                "}");
        chartUrl = chart.getShortUrl();

        return chartUrl;
    }

    public static String createPingChart() {
        String chartUrl;

        List<LogField> pingData = DataCollector.getPingData();

        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        QuickChart chart = new QuickChart();
        chart.setWidth(1000);
        chart.setHeight(300);
        chart.setBackgroundColor("#202225");
        chart.setConfig("{"
                + "    type: 'line',"
                + "    data: {"
                + "        datasets: [{"
                + "            label: 'Ping',"
                + "            data: " + "[" + pingData.stream().map(f -> "{x: " + (f.time - 28800000) + ", y: " + f.value + "}").collect(Collectors.joining(", ")) + "]" + ","
                + "            fill: false,"
                + "            backgroundColor: 'rgba(14, 144, 255, 0.5)',"
                + "            borderColor: 'rgba(14, 144, 255, 0.7)',"
                + "        }]"
                + "    },"
                + "    options: { "
                + "      title: { "
                + "        display: true,"
                + "        fontColor: 'white',"
                + "        fontSize: '22',"
                + "        text: 'Goldilocks Latency (24hr)',"
                + "     }, legend: {"
                + "          display: false,"
                + "          labels: {"
                + "            fontColor: 'white',"
                + "          },"
                + "     }, elements: {"
                + "          point: {"
                + "             radius: 1"
                + "          }"
                + "     },"
                + "     scales: {"
                + "            yAxes: [{"
                + "              scaleLabel: {"
                + "                labelString: 'Ping (ms)',"
                + "                beginAtZero: true,"
                + "                display: true,"
                + "                fontColor: 'white',"
                + "              },  "
                + "              gridLines: {"
                + "                color: 'rgba(0, 0, 0, 0)',"
                + "              },"
                + "              id: 'A',"
                + "              ticks: {"
                + "                beginAtZero: false,"
                + "                fontColor: 'white',"
                + "                display: true,"
                + "                precision: '0',"
                + "              }"
                + "            }],"
                + "            xAxes: [{"
                + "              scaleLabel: {"
                + "                labelString: 'Time (EST)',"
                + "                beginAtZero: true,"
                + "                display: true,"
                + "                fontColor: 'white',"
                + "              },  "
                + "                type: 'time',"
                + "                ticks: {"
                + "                    parser: 'HH:mm:ss',"
                + "                    beginAtZero: false,"
                + "                    fontColor: 'white',"
                + "                    display: true"
                + "                },"
                + "                gridLines: {"
                + "                    display: false"
                + "                },"
                + "            }]"
                + "          },"
                + "     },"
                + "}"
        );

        chartUrl = chart.getShortUrl();

        return chartUrl;
    }

}
