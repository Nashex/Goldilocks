package verification;

import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utils.ProxyHelper;
import utils.SSLhelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CharacterSummary {

    String userName;
    List<Character> characters = new ArrayList<>();

    public CharacterSummary(String userName) {
        this.userName = userName;
    }

    public List<String> stringify(int limit) {
        List<String> list = new ArrayList<>();
        String currentString = "";
        for (Character character : characters) {
            String charString = character.getMessageString();
            if (currentString.length() + charString.length() < limit) {
                currentString += charString + "\n";
            } else {
                list.add(currentString);
                currentString = charString + "\n";
            }
        }
        if (list.isEmpty() && currentString.isEmpty()) currentString = "Player either has a private profile or no characters.";
        if (!currentString.isEmpty()) list.add(currentString);
        return list;
    }

    public List<MessageEmbed.Field> fieldify() {
        List<String> strings = stringify(1024);
        return strings.stream().map(s -> new MessageEmbed.Field(strings.indexOf(s) == 0 ? "Characters" : " ", s, false)).collect(Collectors.toList());
    }

    public CharacterSummary getCharacters() throws IOException {
        Map.Entry<String, Integer> proxy = ProxyHelper.randomProxy(ProxyHelper.proxyList);
        Document doc = Jsoup.connect("https://www.realmeye.com/player/" + userName)
                .proxy(proxy.getKey(), proxy.getValue())
                .headers(VerificationHub.headers)
                .timeout(5000)
                .sslSocketFactory(SSLhelper.socketFactory())
                .ignoreHttpErrors(true)
                .get();

        if (doc.body().text().contains("haven't seen") || doc.body().text().contains("private profile") || doc.select("table").size() == 1) {
            return this;
        }

        //System.out.println("Realmeye pinged for:" + player);
        Element table = doc.select("table").get(1);
        Elements characters = table.select("tr");
        for (Element character : characters.subList(1, characters.size())) {
            int offset = 10 - character.select("td").size();
            List<String> columns = character.select("td").eachText();
            //Elements charInfo = currentChar.select("td").first().siblingElements();

            String className = columns.get(0);
            String level = columns.get(1);
            String cqc = columns.get(2);
            String fame = columns.get(3);
            String place = columns.get(5);
            String stats = columns.get(6);

            Item items[] = {new Item("Weapon"), new Item("Ability"), new Item("Armor"), new Item("Ring")};
            String itemNames[] = {"Weapon", "Ability", "Armor", "Ring"};
            for (int i = 0; i < 4; i++) {
                String name = (character.select("td").select("[class=\"item-wrapper\"]").get(i).child(0).childrenSize() >= 1)
                        ? character.select("td").select("[class=\"item-wrapper\"]").get(i).child(0).child(0).attributes().get("title") : "Empty";
                items[i].name = name;
                items[i].tier = Item.getTier(name);
                //System.out.println(name);
            }

            this.characters.add(new Character(className, level, cqc, fame, place, items[0], items[1], items[2], items[3], stats));
        }
        return this;
    }

}
