package parsing;

import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import setup.SetupConnector;
import utils.ProxyHelper;
import utils.SSLhelper;
import verification.Item;
import verification.VerificationHub;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static main.Goldilocks.jda;

public class CharacterParse {

    ExecutorService REQUEST_ISSUER = Executors.newFixedThreadPool(64);

    int requestsRecieved = 0;
    int totalRequests;
    long timeStarted = System.currentTimeMillis();
    String charString = "";

    Member member;
    Message parseMessage;

    boolean showAll;
    int weaponReq = 0;
    int abilityReq = 0;
    int armorReq = 0;
    int ringReq = 0;
    List<String> bannedItems;

    EmbedBuilder currentEmbed;
    String currentField = "";
    List<String> doesntMeetReqs = new ArrayList<>();

    List<String> peopleInWebApp = null;
    boolean isOSanc = false;

    public void CharParse(List<String> players, VoiceChannel voiceChannel, TextChannel textChannel, Member member, boolean showAll) {

        isOSanc = Database.isOSanc(member.getGuild());
        if (isOSanc) peopleInWebApp = getWebApp(voiceChannel.getId());

        this.showAll = showAll;
        String weaponReqS = SetupConnector.getFieldValue(member.getGuild(), "guildInfo", "ParseWeaponRequirement");
        String abilityReqS = SetupConnector.getFieldValue(member.getGuild(), "guildInfo", "ParseAbilityRequirement");
        String armorReqS = SetupConnector.getFieldValue(member.getGuild(), "guildInfo", "ParseArmorRequirement");
        String ringReqS = SetupConnector.getFieldValue(member.getGuild(), "guildInfo", "ParseRingRequirement");
        weaponReq = weaponReqS.isEmpty() ? 0 : Integer.parseInt(weaponReqS);
        abilityReq = abilityReqS.isEmpty() ? 0 : Integer.parseInt(abilityReqS);
        armorReq = armorReqS.isEmpty() ? 0 : Integer.parseInt(armorReqS);
        ringReq = ringReqS.isEmpty() ? 0 : Integer.parseInt(ringReqS);
        bannedItems = Database.getBannedItems(member.getGuild());

        this.member = member;
        parseMessage = textChannel.sendMessage(inProgressEmbed().build()).complete();
        currentEmbed = charEmbed();
        totalRequests = players.size() - 1;
        List<stats.Character> characterList = new ArrayList<>();
        for (String s : players) {
            if (s.length() <= 12) {
                REQUEST_ISSUER.execute(() -> {
                    //System.out.println(s.replaceAll("[^A-Za-z]", ""));
                    stats.Character character = getCharacter(s.replaceAll("[^A-Za-z]", ""), 1, textChannel);
                    if (character != null) characterList.add(character);
                });
            }
        }
    }

    public stats.Character getCharacter(String player, int charNumber, TextChannel textChannel) {
        try {

            try {
                Map.Entry<String, Integer> proxy = ProxyHelper.randomProxy(ProxyHelper.proxyList);
                Document doc = Jsoup.connect("https://www.realmeye.com/player/" + player)
                        .proxy(proxy.getKey(), proxy.getValue())
                        .headers(VerificationHub.headers)
                        .timeout(5000)
                        .sslSocketFactory(SSLhelper.socketFactory())
                        .ignoreHttpErrors(true)
                        .get();

                //System.out.println("Realmeye pinged for:" + player);
                if (doc.select("table").size() < 2) {
                    requestsRecieved++;
                    if (requestsRecieved == totalRequests - 1) {
                        EmbedBuilder embedBuilder = currentEmbed
                                .setDescription("```\n" + "Done in " + String.format("%.2f", (System.currentTimeMillis() - timeStarted) / 1000.0) + " seconds" + "```");
                        if (!currentField.isEmpty()) currentEmbed.addField(" ", currentField, false);
                        if (!doesntMeetReqs.isEmpty()) embedBuilder.addField("Additional Kick Commands", "```\n/kick " + doesntMeetReqs.stream().collect(Collectors.joining(" ")) + "\n```", false);
                        parseMessage.editMessage(embedBuilder.build()).queue();
                    }
                    return null;
                }
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

                int weaponint = 99;
                int abilityint = 99;
                int armorint = 99;
                int ringint = 99;

                Item items[] = {new Item("Weapon"), new Item("Ability"), new Item("Armor"), new Item("Ring")};
                String itemNames[] = {"Weapon", "Ability", "Armor", "Ring"};

                for (int i = 0; i < 4; i++) {
                    String name = (charInfo.select("[class=\"item-wrapper\"]").get(i).child(0).childrenSize() >= 1)
                            ? charInfo.select("[class=\"item-wrapper\"]").get(i).child(0).child(0).attributes().get("title") : "Empty";
                    items[i].name = name;
                    items[i].tier = Item.getTier(name);
                    //System.out.println(name);
                }

                Boolean backpack = charInfo.select("[class=\"item-wrapper\"]").size() > 4;

//                String dataStats = charInfo.select("[class=\"player-stats\"]").get(0).attr("data-stats");
//                String dataBonuses = charInfo.select("[class=\"player-stats\"]").get(0).attr("data-bonuses");
//                String maxedStats = charInfo.select("[class=\"player-stats\"]").get(0).text();

//                int[] stats = new int[8];
//                int[] bonuses = new int[8];
//                int[] resultant = new int[8];
//                for (int i = 0; i < 8; i++) {
//                    stats[i] = Integer.parseInt(dataStats.replace("[", "").replace("]", "").split(",")[i]);
//                    bonuses[i] = Integer.parseInt(dataBonuses.replace("[", "").replace("]", "").split(",")[i]);
//                    resultant[i] = stats[i] - bonuses[i];
//                }

//                System.out.println("Class: " + className);
//                System.out.println("Level: " + charLevel);
//                System.out.println("Base Fame: " + baseFame);
//                System.out.println("Weapon: " + weapon);
//                System.out.println("Ability: " + ability);
//                System.out.println("Armor: " + armor);
//                System.out.println("Ring: " + ring);
//                System.out.println("Backpack: " + (backpack ? "Yes" : "No"));
//                System.out.println("Maxed Stats: " + maxedStats);
//                System.out.println("Data Stats: " + dataStats);
//                System.out.println("Data Bonuses: " + dataBonuses);
//                System.out.println("Base Stats: " + Arrays.toString(resultant));


                String problems = "";

                int[] reqs = {weaponReq, abilityReq, armorReq, ringReq};

                for (int i = 0; i < 4; i++) {
                    if (items[i].getTier() < reqs[i] && !(i == 1 && (className.equalsIgnoreCase("trickster") || className.equalsIgnoreCase("mystic")))) problems += items[i].getEmote() + " " + items[i].tier + " " + items[i].slot.toLowerCase() + " is below T" + reqs[i] + "\n";
                    if (items[i].name.equals("Empty")) problems += items[i].getSlotEmote() + items[i].slot + " is Empty\n";
                    if (bannedItems.contains(items[i].name.toLowerCase())) problems += items[i].name + "(" + items[i].getEmote() + ") is banned\n";
                }

                if (!problems.isEmpty() || showAll) {
                    //System.out.println(className);
                    String temp = "\n" + jda.getEmotesByName(className, true).stream().filter(e -> Arrays.asList(new String[]{"767811138905178112", "793325895825883146"})
                            .contains(e.getGuild().getId())).collect(Collectors.toList()).get(0).getAsMention() + " **[" + StringUtils.capitalize(player) + "](https://realmeye.com/player/" + player + ")**" +
                            (isOSanc && peopleInWebApp != null ? " | " + (peopleInWebApp.contains(player) ? "✅" : "❌") + " WebApp" : "") + "\n" +
                            "**Lvl:** `" +  charLevel + "` | " +
                            baseFame + "<:fame:826360464865755136>" +
                            " | **Stats:** `?/8`" +
                            " | `" + items[0].tier + "` | " +
                            "`" + items[1].tier + "` | " +
                            "`" + items[2].tier + "` | " +
                            "`" + items[3].tier + "` | " +
                            items[0].getEmote() + items[1].getEmote() + items[2].getEmote() + items[3].getEmote() +
                            "\n" + problems +
                            " `/lock " + player + "`\n";

                    if (currentField.length() + temp.length() < 1024) currentField += temp;
                    else {
                        if ((currentEmbed.length() + doesntMeetReqs.size() * 12) < 4750) currentEmbed.addField(" ", currentField, false);
                        else {
                            textChannel.sendMessage(currentEmbed.build()).complete();
                            //parseMessage.delete().complete();
                            //parseMessage = textChannel.sendMessage(inProgressEmbed().build()).complete();
                            currentEmbed = charEmbed();
                            currentEmbed.addField(" ", currentField, false);
                        }
                        currentField = temp;
                    }
                    //System.out.println(temp);
                    doesntMeetReqs.add(player);
                }

                requestsRecieved++;
                //System.out.println(requestsRecieved + "/" + totalRequests);
                if (requestsRecieved == totalRequests - 1) {
                    EmbedBuilder embedBuilder = currentEmbed
                            .setDescription("```\n" + "Done in " + String.format("%.2f", (System.currentTimeMillis() - timeStarted) / 1000.0) + " seconds" + (isOSanc && peopleInWebApp == null ? "\n\nThere was an issue finding this VC in the WebApp" : "") + "```");
                    if (!currentField.isEmpty()) currentEmbed.addField(" ", currentField, false);
                    if (!doesntMeetReqs.isEmpty()) embedBuilder.addField("Additional Kick Commands", "```\n/kick " + doesntMeetReqs.stream().collect(Collectors.joining(" ")) + "\n```", false);
                    parseMessage.editMessage(embedBuilder.build()).queue();
                    //textChannel.sendMessage(charString + "\n**Time Taken:** " + (String.format("%.2f", (System.currentTimeMillis() - timeStarted) / 1000.0)) + " seconds").queue();
                }
                try {
                    return new stats.Character(player, className, backpack, Long.parseLong(baseFame), Long.parseLong(charLevel.replace("/5", "")),0L , items[0].name, items[1].name, items[2].name, items[3].name, null);
                } catch (Exception e) {return null;}

            } catch (IndexOutOfBoundsException e) {
                //e.printStackTrace();
            }

        } catch (Exception e) {
            //e.printStackTrace();
        }
        requestsRecieved++;
        //System.out.println(requestsRecieved + "/" + totalRequests);
        if (requestsRecieved == totalRequests - 1) {
            EmbedBuilder embedBuilder = currentEmbed
                    .setDescription("```\n" + "Done in " + String.format("%.2f", (System.currentTimeMillis() - timeStarted) / 1000.0) + " seconds" + "```");
            if (!currentField.isEmpty()) currentEmbed.addField(" ", currentField, false);
            if (!doesntMeetReqs.isEmpty()) embedBuilder.addField("Additional Kick Commands", "```\n/kick " + doesntMeetReqs.stream().collect(Collectors.joining(" ")) + "\n```", false);
            parseMessage.editMessage(embedBuilder.build()).queue();
        }
        return null;
    }

    private EmbedBuilder charEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setColor(Goldilocks.LIGHTBLUE)
                .setTitle("Character Parse for " + member.getEffectiveName())
                .setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    private EmbedBuilder inProgressEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setColor(Goldilocks.LIGHTBLUE)
                .setTitle("Character Parse for " + member.getEffectiveName())
                .setDescription("```\n" + "Parsing characters..." + "\n```")
                .setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    /*
    Classes
     */

    private class PlayerProfile {

        String name;
        LinkedList<Character> characters;

        private PlayerProfile(String name, LinkedList<Character> characters) {
            this.name = name;
            this.characters = characters;
        }

    }

    private static List<String> getWebApp(String voiceId) {
        List<String> peopleInWebApp = new ArrayList<>();

        try {
            HttpClient httpclient = HttpClients.createDefault();
            HttpPost httppost = new HttpPost("https://api.osanc.net/getActiveRaids/");

            // Request parameters and other properties.

            //Execute and get the response.
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    entity.writeTo(baos);
                    String res = new String(baos.toByteArray());
                    JSONObject json = new JSONObject(res);

                    JSONArray activeRaids = json.getJSONArray("list");
                    JSONArray members = null;

                    for (int i = 0; i < activeRaids.length(); i++) {
                        if (activeRaids.getJSONObject(i).getJSONObject("voice_channel").getString("id").equals(voiceId)) {
                            members = activeRaids.getJSONObject(i).getJSONArray("members");
                        }
                    }

                    if (members == null) return null;

                    members.forEach(o -> {
                        String memberName = ((JSONObject) o).getString("server_nickname");
                        String[] memberNames = memberName.replaceAll("[^A-Za-z|]", "").toLowerCase().split("\\|");
                        if (!((JSONObject) o).getBoolean("in_waiting_list")) peopleInWebApp.addAll(Arrays.asList(memberNames));
                    });

                    return peopleInWebApp;

                } catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }
        return null;
    }

}
