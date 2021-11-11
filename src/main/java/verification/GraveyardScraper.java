package verification;

import com.google.gson.*;
import main.Goldilocks;
import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utils.ProxyHelper;
import utils.SSLhelper;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GraveyardScraper {
    public static List<Character> graveyardScrape(String playerName) {
        List<Character> characters = new ArrayList<>();
        List<String> playerGraveStats = new ArrayList<>();

        long timeStarted = System.currentTimeMillis();

        try {
            Map.Entry<String, Integer> proxy = ProxyHelper.randomProxy(ProxyHelper.proxyList);
            Document doc = Jsoup.connect("https://www.realmeye.com/graveyard-of-player/" + playerName)
                    .proxy(proxy.getKey(), proxy.getValue())
                    .headers(VerificationHub.headers)
                    .sslSocketFactory(SSLhelper.socketFactory())
                    .get();

            if (doc.body().getElementsContainingText("is hidden.").size() != 0 || doc.body().getElementsContainingText("No data available").size() != 0 || doc.body().getElementsByTag("table").size() <= 1) {
                return characters;
            }

            Elements profile = doc.body().getElementsByTag("p");
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

            Elements graveElements = doc.body().getElementsByTag("table").get(1).getElementsByTag("tr");
            for (Element graveElement : graveElements) {
                String time = graveElement.child(0).text();
                String className = graveElement.child(2).text();
                String level = graveElement.child(3).text();
                String baseFame = graveElement.child(4).text();
                String totalFame = graveElement.child(5).text();
                String exp = graveElement.child(6).text();

                Elements itemElements = graveElement.child(7).getElementsByClass("item-wrapper");
                //itemElements.forEach(element -> System.out.println(element.html()));

                Item items[] = {new Item("Weapon"), new Item("Ability"), new Item("Armor"), new Item("Ring")};
                if (!itemElements.isEmpty()) {
                    for (int i = 0; i < 4; i++) {
                        String itemName = (itemElements.get(i).child(0).childrenSize() >= 1)
                                ? itemElements.get(i).child(0).child(0).attributes().get("title") : "Empty";
                        items[i].name = itemName;
                        items[i].tier = Item.getTier(itemName);
                    }
                }

                String stats = graveElement.child(8).text();
                String killedBy = graveElement.child(9).text();

                if (!itemElements.isEmpty()) characters.add(new Character(time, className, level, baseFame, totalFame, items[0], items[1], items[2], items[3], itemElements.size() == 5, stats, killedBy));

            }

        } catch (Exception e) {
            Goldilocks.proxyHelper.nextProxy();
            e.printStackTrace();
        }


        if (playerGraveStats.get(0).replaceAll("[^0-9]","").isEmpty()) return characters;

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 101; i <= Integer.parseInt(playerGraveStats.get(0)); i+= 100) {
            int finalI = i;
            futures.add(CompletableFuture.runAsync(() -> characters.addAll(graveyardScrape(playerName, finalI))));
        }
        futures.forEach(CompletableFuture::join);

        JsonSerializer<Item> itemJsonSerializer = new JsonSerializer<Item>() {
            @Override
            public JsonElement serialize(Item src, Type typeOfSrc, JsonSerializationContext context) {
                JsonObject jsonMember = new JsonObject();
                jsonMember.addProperty("name", src.name);
                jsonMember.addProperty("tier", src.tier);
                return jsonMember;
            }
        };
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Item.class, itemJsonSerializer)
                .create();

        JSONArray characterObject = new JSONArray(gson.toJson(characters));

        JSONObject parent = new JSONObject();
        parent.put("playerName", playerName);
        parent.put("deaths", playerGraveStats.get(0));
        parent.put("processed", playerGraveStats.get(1));
        parent.put("characters", characterObject);

        String sql = "INSERT INTO graveyards (ign, deaths, content) VALUES ('" + playerName + "', " + playerGraveStats.get(0) + ", '" + StringEscapeUtils.escapeSql(parent.toString(2)) + "')";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:graveyards.db");
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return characters;
    }

    public static List<Character> graveyardScrape(String playerName, int num) {
        List<Character> characters = new ArrayList<>();

        try {
            Map.Entry<String, Integer> proxy = ProxyHelper.randomProxy(ProxyHelper.proxyList);
            Document doc = Jsoup.connect("https://www.realmeye.com/graveyard-of-player/" + playerName + "/" + num)
                    .proxy(proxy.getKey(), proxy.getValue())
                    .headers(VerificationHub.headers)
                    .sslSocketFactory(SSLhelper.socketFactory())
                    .get();

            if (doc.body().getElementsContainingText("is hidden.").size() != 0 || doc.body().getElementsContainingText("No data available").size() != 0 || doc.body().getElementsByTag("table").size() == 1) {
                return characters;
            }

            Elements graveElements = doc.body().getElementsByTag("table").get(1).getElementsByTag("tr");
            for (Element graveElement : graveElements) {
                String time = graveElement.child(0).text();
                String className = graveElement.child(2).text();
                String level = graveElement.child(3).text();
                String baseFame = graveElement.child(4).text();
                String totalFame = graveElement.child(5).text();
                String exp = graveElement.child(6).text();

                Elements itemElements = graveElement.child(7).getElementsByClass("item-wrapper");
                //itemElements.forEach(element -> System.out.println(element.html()));

                Item items[] = {new Item("Weapon"), new Item("Ability"), new Item("Armor"), new Item("Ring")};
                if (!itemElements.isEmpty()) {
                    for (int i = 0; i < 4; i++) {
                        String itemName = (itemElements.get(i).child(0).childrenSize() >= 1)
                                ? itemElements.get(i).child(0).child(0).attributes().get("title") : "Empty";
                        items[i].name = itemName;
                        items[i].tier = Item.getTier(itemName);
                    }
                }

                String stats = graveElement.child(8).text();
                String killedBy = graveElement.child(9).text();

                if (!itemElements.isEmpty()) characters.add(new Character(time, className, level, baseFame, totalFame, items[0], items[1], items[2], items[3], itemElements.size() == 5, stats, killedBy));

            }

        } catch (Exception e) {
            Goldilocks.proxyHelper.nextProxy();
            e.printStackTrace();
        }

        return characters;
    }
}
