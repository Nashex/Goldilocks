package stats;

import lombok.Getter;
import main.Config;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utils.SSLhelper;
import verification.VerificationHub;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for the API
 */
@Getter
public class RealmPlayer {

    private final String username;
    private final long fame;
    private final long rank;
    private final List<Character> characters = new ArrayList<>();
    private final String description;
    private final String location;
    private final long nestCompletes;

    public RealmPlayer(String username, JSONObject jsonObj) throws PrivateProfileException {

        if (jsonObj.keySet().contains("error")){
            throw new PrivateProfileException("Profile is set to private");
        }

        this.username = username;
        this.fame = jsonObj.getLong("fame");
        this.rank = jsonObj.getLong("rank");
        this.description = jsonObj.getString("desc1") + "\n" + jsonObj.getString("desc2") + "\n" + jsonObj.getString("desc3");
        this.location = jsonObj.getString("player_last_seen");
        this.nestCompletes = parseNestCompletes(username);

        for (Object characterObj : jsonObj.getJSONArray("characters")){

            if (characterObj instanceof JSONObject) {
                JSONObject equips = ((JSONObject) characterObj).getJSONObject("equips");
                String name = ((JSONObject) characterObj).getString("class");
                boolean backpack = ((JSONObject) characterObj).getBoolean("backpack");
                long exp = ((JSONObject) characterObj).getLong("exp");
                long fame = ((JSONObject) characterObj).getLong("fame");
                long level = ((JSONObject) characterObj).getLong("level");
                String pet = ((JSONObject) characterObj).getString("pet");
                long stats_maxed = ((JSONObject) characterObj).getLong("stats_maxed");
                String weapon = ((JSONObject) equips).getString("weapon");
                String ability = ((JSONObject) equips).getString("ability");
                String armor = ((JSONObject) equips).getString("armor");
                String ring = ((JSONObject) equips).getString("ring");

                //Character character = new Character(username, name, backpack, exp, fame, level, pet, stats_maxed, weapon, ability, armor, ring);
                //characters.add(character);
            }

        }
    }

    public static class PrivateProfileException extends Exception {
        public PrivateProfileException(String message) {
            super(message);
        }
    }

    public static long parseNestCompletes(String player) {

        try {
            Document doc = Jsoup.connect("https://www.realmeye.com/graveyard-summary-of-player/" + player)
                    .sslSocketFactory(SSLhelper.socketFactory())
                    .headers(VerificationHub.headers)
                    .data("query", "Java")
                    .proxy(Config.get("PROXY_URL").toString(), Integer.parseInt(Config.get("PROXY_PORT").toString()))
                    .userAgent("Mozilla")
                    .get();

            Element tempContainer = doc.select("[class='table table-striped main-achievements'], .tbody, td:contains(Nests Completed)").first();

            for (Element completes : tempContainer.getAllElements()) {
                if (completes.tagName("td").text().equals("Nests completed2")) {
                    long nestCompletes = Long.parseLong(completes.siblingElements().get(1).text());
                    return nestCompletes;
                }

            }

        } catch (Exception e) {
            System.out.println("Profile is set to private");
            return -1L;
        }
        return -1L;
    }

    public static int[] parseCharStats(String player, int charNumber) {
        int[] dsArray = new int[8];

        try {
            Document doc = Jsoup.connect("https://www.realmeye.com/player/" + player)
                    .proxy(Config.get("PROXY_URL").toString(), Integer.parseInt(Config.get("PROXY_PORT").toString()))
                    .headers(VerificationHub.headers)
                    .sslSocketFactory(SSLhelper.socketFactory())
                    .get();
            Element tempContainer = doc.select("[class='table table-striped tablesorter'], .tbody").first();

            String dataBonusesString = "";
            String dataStatsString = "";
            int pos = 0;

            for (Element stats : tempContainer.getAllElements()) {
                if (stats.attributes().hasDeclaredValueForKey("data-stats")) {
                    dataStatsString = StringUtils.replace(stats.attributes().get("data-stats"), "[", "");
                    dataStatsString = StringUtils.replace(dataStatsString, "]", "");
                    dataBonusesString = StringUtils.replace(stats.attributes().get("data-bonuses"), "[", "");
                    dataBonusesString = StringUtils.replace(dataBonusesString, "]", "");
                    if (pos == charNumber) {
                        break;
                    }
                }

            }
            String[] dsStringArray = dataStatsString.split(",");
            String[] dbStringArray = dataBonusesString.split(",");
            for (int i = 0; i < dsStringArray.length; i++) {
                dsArray[i] = Integer.parseInt(dsStringArray[i]) - Integer.parseInt(dbStringArray[i]);;
            }

            return dsArray;

        } catch (Exception e) {
            e.printStackTrace();
            return dsArray;
        }
    }

    public static Character getCharacter(String player, int charNumber) {
        try {

            try {
                Document doc = Jsoup.connect("https://www.realmeye.com/player/" + player)
                        .proxy(Config.get("PROXY_URL").toString(), Integer.parseInt(Config.get("PROXY_PORT").toString()))
                        .headers(VerificationHub.headers)
                        .sslSocketFactory(SSLhelper.socketFactory())
                        .data("query", "Java")
                        .userAgent("Mozilla")
                        .get();

                //System.out.println("Realmeye pinged for:" + player);
                Element table = doc.select("table").get(1);
                Element currentChar = table.select("tr").get(charNumber);
                Elements charInfo = currentChar.select("td").first().siblingElements();

                String className = charInfo.get(1).text();
                String charLevel = charInfo.get(2).text();
                String baseFame = charInfo.get(4).text();

                if (Integer.parseInt(baseFame) >= 100000) {
                    charLevel = charInfo.get(1).text();
                    baseFame = charInfo.get(3).text();
                }

                //System.out.println(currentChar.html());

                String weapon = (charInfo.select("[class=\"item-wrapper\"]").get(0).child(0).childrenSize() >= 1)
                        ? charInfo.select("[class=\"item-wrapper\"]").get(0).child(0).child(0).attributes().get("title") : "Empty";
                String ability = (charInfo.select("[class=\"item-wrapper\"]").get(1).child(0).childrenSize() >= 1)
                        ? charInfo.select("[class=\"item-wrapper\"]").get(1).child(0).child(0).attributes().get("title") : "Empty";
                String armor = (charInfo.select("[class=\"item-wrapper\"]").get(2).child(0).childrenSize() >= 1)
                        ? charInfo.select("[class=\"item-wrapper\"]").get(2).child(0).child(0).attributes().get("title") : "Empty";
                String ring = (charInfo.select("[class=\"item-wrapper\"]").get(3).child(0).childrenSize() >= 1)
                        ? charInfo.select("[class=\"item-wrapper\"]").get(3).child(0).child(0).attributes().get("title") : "Empty";
                Boolean backpack;
                String dataStats = charInfo.select("[class=\"player-stats\"]").get(0).attr("data-stats");
                String dataBonuses = charInfo.select("[class=\"player-stats\"]").get(0).attr("data-bonuses");
                String maxedStats = charInfo.select("[class=\"player-stats\"]").get(0).text();

                try {
                    charInfo.select("[class=\"item-wrapper\"]").get(4);
                    backpack = true;
                } catch (IndexOutOfBoundsException e) {
                    backpack = false;
                }

                int[] stats = new int[8];
                int[] bonuses = new int[8];
                int[] resultant = new int[8];
                for (int i = 0; i < 8; i++) {
                    stats[i] = Integer.parseInt(dataStats.replace("[", "").replace("]", "").split(",")[i]);
                    bonuses[i] = Integer.parseInt(dataBonuses.replace("[", "").replace("]", "").split(",")[i]);
                    resultant[i] = stats[i] - bonuses[i];
                }

                return new Character(player, className, backpack, Long.parseLong(baseFame), Long.parseLong(charLevel.replace("/5", "")), Long.parseLong(maxedStats.replace("/8", "")), weapon, ability, armor, ring, resultant);

                /*
                System.out.println("Class: " + className);
                System.out.println("Level: " + charLevel);
                System.out.println("Base Fame: " + baseFame);
                System.out.println("Weapon: " + weapon);
                System.out.println("Ability: " + ability);
                System.out.println("Armor: " + armor);
                System.out.println("Ring: " + ring);
                System.out.println("Backpack: " + (backpack ? "Yes" : "No"));
                System.out.println("Maxed Stats: " + maxedStats);
                System.out.println("Data Stats: " + dataStats);
                System.out.println("Data Bonuses: " + dataBonuses);
                System.out.println("Base Stats: " + Arrays.toString(resultant));
                 */

            } catch (IndexOutOfBoundsException e) {
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
