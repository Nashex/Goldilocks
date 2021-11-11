package verification;

import main.Goldilocks;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utils.ProxyHelper;
import utils.SSLhelper;

import java.util.LinkedHashMap;
import java.util.Map;

public class CompactPlayerProfile {
    public String username;
    public int characters = -1;
    public int exaltations;
    public long aliveFame;
    public int stars;
    public long accountFame;
    public String guild;
    public int guildRank;
    public String guildRankString = "Not Found";
    public String guildRankEmote = "";
    public String firstSeen = "Hidden";
    public String description = "";
    public boolean hiddenLocation;
    public String lastSeen;
    public int skins = -1;
    public boolean hiddenNameHistory = false;
    public LinkedHashMap<String, String> nameHistory = new LinkedHashMap<>();

    public CompactPlayerProfile(String username) throws verification.PlayerProfile.PrivateProfileException {
        this.username = username;

        try {
            Map.Entry<String, Integer> proxy = ProxyHelper.randomProxy(ProxyHelper.proxyList);
            Document doc = Jsoup.connect("https://www.realmeye.com/name-history-of-player/" + username)
                    .proxy(proxy.getKey(), proxy.getValue())
                    .headers(VerificationHub.headers)
                    .userAgent("Mozilla")
                    .sslSocketFactory(SSLhelper.socketFactory())
                    .get();

            if (doc.body().getElementsContainingText("haven't seen \"" + username + "\" yet,").size() != 0) {
                throw new verification.PlayerProfile.PrivateProfileException("Profile is set to private");
            }

            Elements profile = doc.body().getElementsByTag("table").select("tr");
            for (Element element : profile) {
                Elements children = element.children();
                switch (element.children().get(0).text()) {
                    case "Characters":
                        characters = Integer.parseInt(children.get(1).text());
                        break;
                    case "Exaltations":
                        exaltations = Integer.parseInt(children.get(1).text().split(" ")[0]);
                        break;
                    case "Fame":
                        aliveFame = Long.parseLong(children.get(1).text().split(" ")[0]);
                        break;
                    case "Rank":
                        stars = Integer.parseInt(children.get(1).text());
                        break;
                    case "Account fame":
                        accountFame = Long.parseLong(children.get(1).text().split(" ")[0]);
                        break;
                    case "Guild":
                        guild = (children.get(1).text().isEmpty() ? "None" : children.get(1).text());
                        break;
                    case "Guild Rank":
                        String rank = (children.get(1).text().isEmpty() ? "None" : children.get(1).text());
                        guildRankString = rank;
                        if (rank.equalsIgnoreCase("Founder")) {
                            guildRank = 5;
                            guildRankEmote = "<:Founder:826357844550221824>";
                        } else if (rank.equalsIgnoreCase("Leader")) {
                            guildRank = 4;
                            guildRankEmote = "<:Leader:826357854989451295>";
                        } else if (rank.equalsIgnoreCase("Officer")) {
                            guildRank = 3;
                            guildRankEmote = "<:Officer:826357888695533639>";
                        } else if (rank.equalsIgnoreCase("Member")) {
                            guildRank = 2;
                            guildRankEmote = "<:Member:826357917577379870>";
                        } else if (rank.equalsIgnoreCase("Initiate")) {
                            guildRank = 1;
                            guildRankEmote = "<:Initiate:826357950586814474>";
                        }
                        break;
                    case "Skins":
                        skins = Integer.parseInt(children.get(1).text().split(" ")[0]);
                    case "Created":
                    case "First seen":
                        firstSeen = children.get(1).text().trim();
                        if (firstSeen.equals("hidden")) firstSeen = "Hidden";
                        break;
                    case "Last seen":
                        lastSeen = children.get(1).text();
                        if (!children.get(1).text().equalsIgnoreCase("hidden")) hiddenLocation = false;
                        else hiddenLocation = true;
                        break;

                }
            }

            try {
                for (Element element : doc.getElementsByClass("description").get(0).children()) {
                    if (element.text().equals("If this is your character, then you can add some description here, when you are logged in to RealmEye.")) {
                        description = "If this is your character\n then you can add some description here\n when you are logged in to RealmEye.";
                        break;
                    }
                    description += (element.text().isEmpty() ? " " : element.text()) + "\n";
                }
            } catch (Exception e) {}

            if (doc.body().getElementsContainingText("Name history is hidden").size() != 0) {
                hiddenNameHistory = true;
                return;
            }

            if (doc.body().getElementsContainingText("No name changes detected.").size() != 0)
                return;

            profile = doc.body().getElementsByTag("table");

            if (profile.size() < 1) return;

            Element nameChangeTable = profile.get(1);
            for (Element nameChange : nameChangeTable.children().get(1).children()) {
                String name = nameChange.children().get(0).text();
                String from = String.format("%-12s", nameChange.children().get(1).text().replaceAll("[^0-9-:]", " ").split(" ")[0].trim());
                String to = nameChange.children().get(2).text().replaceAll("[^0-9-:]", " ").split(" ")[0].trim();

                nameHistory.put(name, to);
            }
            //System.out.println(table.html());
        } catch (Exception e) {
            if (e.getLocalizedMessage().equals("Profile is set to private")) {
                throw new verification.PlayerProfile.PrivateProfileException("Profile is set to private");
            }
            Goldilocks.proxyHelper.nextProxy();
            //e.printStackTrace();
        }
    }
}
