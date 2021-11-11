package verification;

import lombok.Getter;
import lombok.Setter;
import main.Goldilocks;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utils.SSLhelper;

import java.util.Optional;

@Getter
@Setter
public class PlayerProfile {

    private String username;
    private int characters;
    private int exaltations;
    private long aliveFame;
    private int stars;
    private long accountFame;
    private String guild;
    private int guildRank;
    private String guildRankEmote;
    private String firstSeen;
    private String description;
    private boolean hiddenLocation;
    private int skins;

    public PlayerProfile(String username) throws PrivateProfileException {
        this.username = username;

        try {

            Document doc = Jsoup.connect("https://www.realmeye.com/player/" + username)
                    .proxy(Goldilocks.proxyHelper.currentProxy.getKey(), Goldilocks.proxyHelper.currentProxy.getValue())
                    .headers(VerificationHub.headers)
                    .userAgent("Mozilla")
                    .sslSocketFactory(SSLhelper.socketFactory())
                    .get();

            if (doc.body().getElementsContainingText("haven't seen \"" + username + "\" yet,").size() != 0) {
                throw new PrivateProfileException("Profile is set to private");
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
                        break;
                    case "Last seen":
                        if (!children.get(1).text().equalsIgnoreCase("hidden")) hiddenLocation = false;
                        else hiddenLocation = true;
                        break;
                }
            }

            try {
                for (Element element : doc.getElementsByClass("description").get(0).children()) {
                    description += Optional.ofNullable(element.text()).orElse("Empty") + "\n";
                }
            } catch (Exception e) {}

            //System.out.println(table.html());
        } catch (Exception e) {
            if (e.getLocalizedMessage().equals("Profile is set to private")) {
                throw new PrivateProfileException("Profile is set to private");
            }
            Goldilocks.proxyHelper.nextProxy();
            e.printStackTrace();
        }
    }

    public PlayerProfile(String username, int exaltations, long aliveFame, int stars, long accountFame, String guild, String description, boolean hiddenLocation) {

        this.username = username;
        this.exaltations = exaltations;
        this.aliveFame = aliveFame;
        this.stars = stars;
        this.accountFame = accountFame;
        this.guild = guild;
        this.description = description;
        this.hiddenLocation = hiddenLocation;

    }

    public static class PrivateProfileException extends Exception {
        public PrivateProfileException(String message) {
            super(message);
        }
    }

}
