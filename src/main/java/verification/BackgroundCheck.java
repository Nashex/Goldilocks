package verification;

import main.Goldilocks;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import quota.LogField;
import stats.CharacterExaltation;
import utils.ProxyHelper;
import utils.SSLhelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BackgroundCheck {

    public static String petScrape(String playerName) {

        String petAbility = "";
        try {
            Document doc = Jsoup.connect("https://www.realmeye.com/pets-of/" + playerName)
                    .proxy(Goldilocks.proxyHelper.currentProxy.getKey(), Goldilocks.proxyHelper.currentProxy.getValue())
                    .headers(VerificationHub.headers)
                    .sslSocketFactory(SSLhelper.socketFactory())
                    .get();

            if (doc.body().getElementsContainingText("Pets are hidden.").size() != 0 || doc.body().getElementsByTag("table").size() == 1) {
                return "Hidden";
            }

            if (doc.body().getElementsContainingText("has no pets").size() != 0) {
                return "No Pets";
            }

            Elements profile = doc.body().getElementsByTag("table");
            Element petTable = profile.get(1);

            Element firstPet = petTable.children().get(1).getElementsByTag("tr").get(0);

            petAbility = firstPet.child(6).text()
                    + " " + firstPet.child(5).text().replaceAll("[^A-Z]", "")
                    + "/" + firstPet.child(8).text()
                    + " " + firstPet.child(7).text().replaceAll("[^A-Z]", "");

            petAbility += ":" + firstPet.child(0).child(0).attributes().get("data-item");

        } catch (Exception e) {
            Goldilocks.proxyHelper.nextProxy();
            e.printStackTrace();
        }

        return petAbility;

    }

    public static List<String> graveScrape(String playerName) {

        List<String> playerGraveStats = new ArrayList<>();

        try {
            Document doc = Jsoup.connect("https://www.realmeye.com/graveyard-of-player/" + playerName + "?bf=400")
                    .proxy(Goldilocks.proxyHelper.currentProxy.getKey(), Goldilocks.proxyHelper.currentProxy.getValue())
                    .headers(VerificationHub.headers)
                    .sslSocketFactory(SSLhelper.socketFactory())
                    .get();

            if (doc.body().getElementsContainingText("is hidden.").size() != 0 || doc.body().getElementsContainingText("No data available").size() != 0) {
                return playerGraveStats;
            }

            //System.out.println(doc.body());
            Elements profile = doc.body().getElementsByTag("p");

            String[] graveyardStats = profile.text().replaceAll("[^0-9- ]", "").replaceAll("[ ]{2,}", " ").replaceFirst(" ", "").split(" ");

            String[] graveYard = profile.text().split("[A-Za-z][.]");
            for (String s : graveYard) {
                playerGraveStats.add(s.replace("up-to-dat", "100%").replace("one", "1")
                        .replace("two", "2")
                        .replace("three", "3")
                        .replace("four", "4")
                        .replace("five", "5")
                        .replace("six", "6")
                        .replace("seven", "7")
                        .replace("eight", "8")
                        .replace("nine", "9")
                        .replaceAll("[^0-9.%.~..]", ""));
            }

            //System.out.println(playerGraveStats.stream().collect(Collectors.joining(" ")));

        } catch (Exception e) {
            Goldilocks.proxyHelper.nextProxy();
            e.printStackTrace();
        }

        return playerGraveStats;
    }

    public static List<List<LogField>> fameHistoryScrape(String playerName) {
        String[] names = {"Daily Fame History", "Weekly Fame History", "All Time Fame History"};
        List<List<LogField>> historyFields = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            List<LogField> field = new ArrayList<>();
            field.add(new LogField(names[i], 0, System.currentTimeMillis()));
            historyFields.add(field);
        }

        try {
            Document doc = Jsoup.connect("https://www.realmeye.com/fame-history-of-player/" + playerName)
                    .proxy(Goldilocks.proxyHelper.currentProxy.getKey(), Goldilocks.proxyHelper.currentProxy.getValue())
                    .headers(VerificationHub.headers)
                    .sslSocketFactory(SSLhelper.socketFactory())
                    .get();

            if (doc.body().getElementsContainingText("is hidden").size() != 0 || doc.body().getElementsContainingText("No data available").size() != 0) {
                return null;
            }

            Elements history = doc.getElementsByTag("script");

            for (Element element : history) {
                if (element.html().contains("initializeGraphs(")) {
                    String allHistory = element.html().split("initializeGraphs[(]")[1].split(",\"")[0];
                    int index = 0;
                    if (allHistory.length() > 0) {
                        historyFields = new ArrayList<>();
                        for (String s : allHistory.replace('[', ':').split("::")) {
                            if (!s.isEmpty()) {
                                List<LogField> field = new ArrayList<>();
                                String arr = s.replaceAll("[]]{2,}(,)?","").replaceAll("[]][,]",":").replaceAll("[:]{2,}", ":");
                                //System.out.println(arr);
                                for (String lf : arr.split(":")) {
                                    String[] vals = lf.split(",");
                                    if (vals.length > 1) field.add(new LogField(names[index], Integer.parseInt(vals[1]), System.currentTimeMillis() - Long.parseLong(vals[0]) * 1000));
                                }
                                if (index >= 1) field.add(historyFields.get(0).get(0));
                                if (field.size() == 1) field.add(new LogField(field.get(0).name, field.get(0).value, field.get(0).time + 86400000));
                                historyFields.add(field);
                                index++;
                            }
                        }
                    }
                    if (historyFields.size() == 2) {
                        List<LogField> field = new ArrayList<>();
                        field.add(new LogField(historyFields.get(0).get(0).name, historyFields.get(0).get(0).value, historyFields.get(0).get(0).time));
                        field.add(new LogField(historyFields.get(0).get(0).name, historyFields.get(0).get(0).value, historyFields.get(0).get(0).time + 86400000));
                        historyFields.add(field);
                    }
                    //System.out.println(historyFields.get(0).stream().map(logField -> "Name: " + logField.name + " | Value: " + logField.value + " | Time: " + logField.time).collect(Collectors.joining("\n")));
                }
            }
        } catch (Exception e) {
            Goldilocks.proxyHelper.nextProxy();
            e.printStackTrace();
        }

        return historyFields;
    }

    public static String rankScrape(String playerName) {

        String rankHistory = "";

        try {
            Document doc = Jsoup.connect("https://www.realmeye.com/rank-history-of-player/" + playerName)
                    .proxy(Goldilocks.proxyHelper.currentProxy.getKey(), Goldilocks.proxyHelper.currentProxy.getValue())
                    .headers(VerificationHub.headers)
                    .sslSocketFactory(SSLhelper.socketFactory())
                    .get();

            Elements profile = doc.body().getElementsByTag("table");

            int rank = Integer.parseInt(profile.get(0).child(0).children().stream()
                    .filter(element -> element.text().contains("Rank")).collect(Collectors.toList()).get(0).text().split(" ")[1]);
            if (rank < 16) {
                rankHistory += Goldilocks.jda.getEmoteById("798257702920650772").getAsMention();
            } else if (rank < 32) {
                rankHistory += Goldilocks.jda.getEmoteById("798257702827982848").getAsMention();
            } else if (rank < 48) {
                rankHistory += Goldilocks.jda.getEmoteById("798257702894960680").getAsMention();
            } else if (rank < 64) {
                rankHistory += Goldilocks.jda.getEmoteById("798257705268674560").getAsMention();
            } else if (rank < 80) {
                rankHistory += Goldilocks.jda.getEmoteById("798257702886834186").getAsMention();
            } else if (rank == 80) {
                rankHistory += Goldilocks.jda.getEmoteById("798257702677118977").getAsMention();
            }

            rankHistory += "," + rank;

            if (doc.body().getElementsContainingText("Rank history is hidden").size() != 0) {
                return rankHistory;
            }

            Element mostRecentRank = profile.get(1).child(1).child(0);

            String[] historyData = mostRecentRank.text().split(" ");
            String when = historyData[1].trim();

            rankHistory += " stars achieved on " + when;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return rankHistory;

    }

    public static List<String> nameChangeScrape(String playerName) {

        List<String> nameHistory = new ArrayList<>();

        try {
            Map.Entry<String, Integer> proxy = ProxyHelper.randomProxy(ProxyHelper.proxyList);
            Document doc = Jsoup.connect("https://www.realmeye.com/name-history-of-player/" + playerName)
                    .proxy(proxy.getKey(), proxy.getValue())
                    .headers(VerificationHub.headers)
                    .sslSocketFactory(SSLhelper.socketFactory())
                    .get();

            if (doc.body().getElementsContainingText("Name history is hidden").size() != 0)
                return Collections.singletonList("Name history is hidden");

            if (doc.body().getElementsContainingText("No name changes detected.").size() != 0)
                return Collections.singletonList("No name changes detected.");

            Elements profile = doc.body().getElementsByTag("table");

            if (profile.size() < 1) return Collections.singletonList("No name changes detected.");

            Element nameChangeTable = profile.get(1);
            for (Element nameChange : nameChangeTable.children().get(1).children()) {
                String name = nameChange.children().get(0).text();
                String from = String.format("%-12s", nameChange.children().get(1).text().replaceAll("[^0-9-:]", " ").split(" ")[0].trim());
                String to = nameChange.children().get(2).text().replaceAll("[^0-9-:]", " ").split(" ")[0].trim();

                //if (!from.trim().isEmpty()) nameHistory.put(name, "From: " + from + " To: " + (to.trim().isEmpty() ? "Now" : to) + "\n");
                if (!from.trim().isEmpty()) nameHistory.add(name);
            }
        } catch (Exception e) {
            return Collections.singletonList("Error retrieving name changes");
        }

        Collections.reverse(nameHistory);
        return nameHistory;

    }

    public static List<CharacterExaltation> exaltationScrape(String playerName) {
        List<CharacterExaltation> exaltations = new ArrayList<>();
        try {
            Document doc = Jsoup.connect("https://www.realmeye.com/exaltations-of/" + playerName)
                    .proxy(Goldilocks.proxyHelper.currentProxy.getKey(), Goldilocks.proxyHelper.currentProxy.getValue())
                    .headers(VerificationHub.headers)
                    .sslSocketFactory(SSLhelper.socketFactory())
                    .get();

            if (doc.body().getElementsContainingText("Exaltations are hidden").size() != 0)
                return exaltations;

            if (doc.body().getElementsContainingText("No exaltations").size() != 0 || doc.body().getElementsByTag("table").size() < 2)
                return exaltations;

            Elements profile = doc.body().getElementsByTag("table");



            Element exaltationTable = profile.get(1);
            Elements charExalts = exaltationTable.getElementsByTag("tr");
            for (Element exaltation : charExalts.subList(1, charExalts.size() - 1)) {
                Elements data = exaltation.children();
                String characterName = data.get(1).text();
                String numExaltationsString = data.get(2).text().replaceAll("[^0-9]", "");
                int numExaltations = numExaltationsString.isEmpty() ? 0 : Integer.parseInt(numExaltationsString);
                String[] exaltationData = new String[8];
                for (int i = 0; i < 8; i++) {
                    exaltationData[i] = data.get(3 + i).text();
                }
                exaltations.add(new CharacterExaltation(characterName, exaltationData, numExaltations));
            }

        } catch (Exception e) {
            Goldilocks.proxyHelper.nextProxy();
            e.printStackTrace();
        }
        return exaltations;
    }

    public static List<String> guildScrape(String playerName) {

        List<String> guildHistory = new ArrayList<>();

        try {
            Map.Entry<String, Integer> proxy = ProxyHelper.randomProxy(ProxyHelper.proxyList);
            Document doc = Jsoup.connect("https://www.realmeye.com/guild-history-of-player/" + playerName)
                    .proxy(proxy.getKey(), proxy.getValue())
                    .headers(VerificationHub.headers)
                    .sslSocketFactory(SSLhelper.socketFactory())
                    .get();

            if (doc.body().getElementsContainingText("Guild history is hidden").size() != 0)
                return Collections.singletonList("Guild history is hidden.");

            if (doc.body().getElementsContainingText("No guild changes detected.").size() != 0)
                return Collections.singletonList("No guild changes detected.");

            Elements profile = doc.body().getElementsByTag("table");

            if (profile.size() < 1) return Collections.singletonList("No guild changes detected.");

            Element guildChangeTable = profile.get(1);
            String lastGuildName = "";

            for (Element nameChange : guildChangeTable.children().get(1).children()) {
                String name = String.format("%-15s", nameChange.children().get(0).text());
                String rank = String.format("%-10s", nameChange.children().get(1).text().trim());
                String from = String.format("%-12s", nameChange.children().get(2).text().replaceAll("[^0-9-:]", " ").split(" ")[0].trim());
                String to = nameChange.children().get(3).text().replaceAll("[^0-9-:]", " ").split(" ")[0].trim();

                if (!lastGuildName.equals(name)) {

                    if (rank.replaceAll(" ", "").isEmpty()) {
                        //guildHistory += "No Guild                         From: " + from + " To: " + to + "\n";
                    } else if (!from.replaceAll(" ", "").isEmpty()) {
                        //guildHistory += name.trim() + ":\n↳ Rank: " + rank + " From: " + from + " To: " + (to.trim().isEmpty() ? "Now" : to) + "\n\n";
                        guildHistory.add(name.trim());
                    }

                }
                lastGuildName = name;
            }

        } catch (Exception e) {
            Goldilocks.proxyHelper.nextProxy();
            return Collections.singletonList("Error retrieving guild history");
        }
        Collections.reverse(guildHistory);
        return guildHistory;

    }

    public static List<String> getGuildMembers(String guildName) {

        List<String> memberNames = new ArrayList<>();

        try {
            Document doc = Jsoup.connect("https://www.realmeye.com/guild/" + guildName)
                    .proxy(Goldilocks.proxyHelper.currentProxy.getKey(), Goldilocks.proxyHelper.currentProxy.getValue())
                    .headers(VerificationHub.headers)
                    .sslSocketFactory(SSLhelper.socketFactory())
                    .get();

            Elements profile = doc.body().getElementsByTag("table");

            Element nameChangeTable;
            try {
                nameChangeTable = profile.get(1);
            } catch (Exception e) {
                return memberNames;
            }

            for (Element nameChange : nameChangeTable.children().get(1).children()) {
                memberNames.add(nameChange.text().split(" ")[0]);
            }

        } catch (Exception e) {
            Goldilocks.proxyHelper.nextProxy();
            e.printStackTrace();
        }

        return memberNames;

    }

    public static List<String> getGuildFormerMembers(String guildName) {

        List<String> memberNames = new ArrayList<>();

        try {
            Document doc = Jsoup.connect("https://www.realmeye.com/former-members-of-guild/" + guildName)
                    .proxy(Goldilocks.proxyHelper.currentProxy.getKey(), Goldilocks.proxyHelper.currentProxy.getValue())
                    .headers(VerificationHub.headers)
                    .sslSocketFactory(SSLhelper.socketFactory())
                    .get();

            Elements profile = doc.body().getElementsByTag("table");

            Element nameChangeTable;
            try {
                nameChangeTable = profile.get(1);
            } catch (Exception e) {
                return memberNames;
            }

            for (Element nameChange : nameChangeTable.children().get(1).children()) {
                if (!nameChange.text().split(" ")[0].equalsIgnoreCase("private")) memberNames.add(nameChange.text().split(" ")[0]);
            }

        } catch (Exception e) {
            Goldilocks.proxyHelper.nextProxy();
        }

        return memberNames;

    }

}
