package verification;

import main.Goldilocks;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utils.SSLhelper;

import java.util.ArrayList;
import java.util.List;

public class GraveyardSummary {

    public String username;
    public List<summaryObject> dungeons = new ArrayList<>();
    public List<summaryObject> achievements = new ArrayList<>();

    public GraveyardSummary(String playerName) {
        username = playerName;
        try {
            Document doc = Jsoup.connect("https://www.realmeye.com/graveyard-summary-of-player/" + playerName)
                    .proxy(Goldilocks.proxyHelper.currentProxy.getKey(), Goldilocks.proxyHelper.currentProxy.getValue())
                    .headers(VerificationHub.headers)
                    .sslSocketFactory(SSLhelper.socketFactory())
                    .get();

            if (doc.body().getElementsContainingText("is hidden.").size() != 0 || doc.body().getElementsContainingText("No data available").size() != 0 || doc.body().getElementsByTag("table").size() == 1) {
                return;
            }

            //System.out.println(doc.body());
            Elements dungeonsElements = doc.body().getElementsByTag("table").get(1).getElementsByTag("tr");
            for (Element dungeonElement : dungeonsElements) {
                String name = dungeonElement.child(1).text();
                if (!name.isEmpty()) {
                    String total = dungeonElement.child(2).text();
                    String max = dungeonElement.child(3).text();
                    String avg = dungeonElement.child(4).text();
                    String min = dungeonElement.child(5).text();
                    dungeons.add(new summaryObject(name, total, max, avg, min));
                }
            }

            Elements achievementElements = doc.body().getElementsByTag("table").get(2).getElementsByTag("tr");
            for (Element achievementElement : achievementElements) {
                String name = achievementElement.child(0).text();
                if (!name.isEmpty()) {
                    String total = achievementElement.child(1).text();
                    String max = achievementElement.child(2).text();
                    String avg = achievementElement.child(3).text();
                    String min = achievementElement.child(4).text();
                    achievements.add(new summaryObject(name, total, max, avg, min));
                }
            }

        } catch (Exception e) {
            Goldilocks.proxyHelper.nextProxy();
            e.printStackTrace();
        }

    }

    public summaryObject getDungeon(String name) {
        for (summaryObject dungeon : dungeons) {
            if (dungeon.name.equals(name)) return dungeon;
        }
        return null;
    }

    public summaryObject getAchievement(String name) {
        for (summaryObject achievement : achievements) {
            if (achievement.name.equals(name)) return achievement;
        }
        return null;
    }



    public class summaryObject {
        public String name;
        public String total;
        public String max;
        public String average;
        public String min;

        public summaryObject(String name, String total, String max, String average, String min) {
            this.name = name;
            this.total = total;
            this.max = max;
            this.average = average;
            this.min = min;
        }

        @Override
        public String toString() {
            return "Summary Object{" +
                    "name='" + name + '\'' +
                    ", total=" + total +
                    ", max=" + max +
                    ", average=" + average +
                    ", min=" + min +
                    '}';
        }

        public String toChartString(String name, String backgroundColor, String borderColor) {
            return "{" +
                    "label: '" + name + "'," +
                    "data: [" + total + "]," +
                    "backgroundColor: '" + backgroundColor + "'," +
                    "borderColor: '" + borderColor + "'," +
                    "borderWidth: '2'" +
                    '}';
        }
    }

}

