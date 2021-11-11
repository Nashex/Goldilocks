package commands.miscCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.lang3.StringUtils;
import verification.Character;
import verification.GraveyardScraper;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CommandGraveyard extends Command {
    public CommandGraveyard() {
        setAliases(new String[] {"graveyard", "gs", "gy", "graves"});
        setEligibleRoles(new String[] {"verified"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.VERIFIED);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        List<Character> characters = GraveyardScraper.graveyardScrape(args[0]);

        if (args.length < 1) {
            msg.getTextChannel().sendMessage("Unable to generate graveyard breakdown. Please use the command in the following format: `gs <ign>`").queue();
            return;
        }

        if (characters.isEmpty()) {
            msg.getTextChannel().sendMessage("Unable to generate graveyard breakdown. This may be due to a private graveyard or less than 10 deaths recorded.").queue();
            return;
        }

        //Decas Died with
        long decas = characters.stream().filter(character -> character.getRing().name.equals("Ring of Decades UT")).count();

        long o3deaths = characters.stream().filter(character -> character.getKilledBy().split(" ")[0].equals("O3")).count() +
                characters.stream().filter(character -> character.getKilledBy().equalsIgnoreCase("Oryx the Mad God 3")).count() +
                characters.stream().filter(character -> character.getKilledBy().equalsIgnoreCase("Chaos Ray")).count();

        long miniBossDeaths = characters.stream().filter(character -> character.getKilledBy().equalsIgnoreCase("Archbishop Leucoryx")).count() +
                characters.stream().filter(character -> character.getKilledBy().equalsIgnoreCase("Treasurer Gemsbok")).count() +
                characters.stream().filter(character -> character.getKilledBy().split(" ")[0].equals("O3B")).count() +
                characters.stream().filter(character -> character.getKilledBy().equalsIgnoreCase("Chief Beisa")).count() +
                characters.stream().filter(character -> character.getKilledBy().split(" ")[0].equals("O3C")).count() +
                characters.stream().filter(character -> character.getKilledBy().equalsIgnoreCase("Chancellor Dammah")).count();

        long zero, one, two, three, four, five, six, seven, eight;
        long total = characters.size();

        String deathsBreakdown =
                "**`  0/8  `** **` 1/8 `** **` 2/8 `** **` 3/8 `** **` 4/8 `** **` 5/8 `** **` 6/8 `** **` 7/8 `** **` 8/8 `**\n" +
                        "`" + String.format("%1$5s", zero = characters.stream().filter(character -> character.getStats().equals("0/8")).count()) + "  ` " +
                        "`" + String.format("%1$4s", one =characters.stream().filter(character -> character.getStats().equals("1/8")).count()) + " ` " +
                        "`" + String.format("%1$4s", two = characters.stream().filter(character -> character.getStats().equals("2/8")).count()) + " ` " +
                        "`" + String.format("%1$4s", three = characters.stream().filter(character -> character.getStats().equals("3/8")).count()) + " ` " +
                        "`" + String.format("%1$4s", four = characters.stream().filter(character -> character.getStats().equals("4/8")).count()) + " ` " +
                        "`" + String.format("%1$4s", five = characters.stream().filter(character -> character.getStats().equals("5/8")).count()) + " ` " +
                        "`" + String.format("%1$4s", six = characters.stream().filter(character -> character.getStats().equals("6/8")).count()) + " ` " +
                        "`" + String.format("%1$4s", seven = characters.stream().filter(character -> character.getStats().equals("7/8")).count()) + " ` " +
                        "`" + String.format("%1$4s", eight = characters.stream().filter(character -> character.getStats().equals("8/8")).count()) + " `";
//                "\n" +
//                "`" + String.format("%1$5s", String.format("%.1f", (double) zero / total * 100)) + "% ` " +
//                "`" + String.format("%1$4s", String.format("%.1f", (double) one / total * 100)) + "%` " +
//                "`" + String.format("%1$4s", String.format("%.1f", (double) two / total * 100)) + "%` " +
//                "`" + String.format("%1$4s", String.format("%.1f", (double) three / total * 100)) + "%` " +
//                "`" + String.format("%1$4s", String.format("%.1f", (double) four / total * 100)) + "%` " +
//                "`" + String.format("%1$4s", String.format("%.1f", (double) five / total * 100)) + "%` " +
//                "`" + String.format("%1$4s", String.format("%.1f", (double) six / total * 100)) + "%` " +
//                "`" + String.format("%1$4s", String.format("%.1f", (double) seven / total * 100)) + "%` " +
//                "`" + String.format("%1$4s", String.format("%.1f", (double) eight / total * 100)) + "%` ";

        List<String> lhWeapons = Arrays.asList("Sword of the Colossus UT", "Bow of the Void UT", "Staff of Unholy Sacrifice UT");
        List<String> lhAbilities = Arrays.asList("Marble Seal UT", "Quiver of the Shadows UT", "Skull of Corrupted Souls UT");
        List<String> lhArmors = Arrays.asList("Breastplate of New Life UT", "Armor of Nil", "Ritual Robe UT");
        List<String> lhRings = Arrays.asList("Magical Lodestone UT", "Sourcestone UT", "Omnipotence Ring UT", "Bloodshed Ring UT");

        long lostHallsWhites = characters.stream().map(character -> {
            boolean weapon = lhWeapons.contains(character.getWeapon().name);
            boolean armor = lhAbilities.contains(character.getAbility().name);
            boolean ability = lhArmors.contains(character.getArmor().name);
            boolean rings = lhRings.contains(character.getRing().name);
            return (weapon ? 1 : 0) + (armor ? 1 : 0) + (ability ? 1 : 0) + (rings ? 1 : 0);
        }).mapToInt(Integer::intValue).sum();

        List<String> shattersAbilities = Arrays.asList("The Twilight Gemstone UT");
        List<String> shattersRings = Arrays.asList("Bracer of the Guardian UT", "The Forgotten Crown UT", "Ice Crown UT");

        long shattersWhites = characters.stream().map(character -> {
            boolean armor = shattersAbilities.contains(character.getAbility().name);
            boolean rings = shattersRings.contains(character.getRing().name);
            return (armor ? 1 : 0) + (rings ? 1 : 0);
        }).mapToInt(Integer::intValue).sum();

        List<String> o3Weapons = Arrays.asList("Superior UT", "Avarice UT", "Lumiaire UT", "Enforcer UT", "Divinity UT");
        List<String> o3Abilities = Arrays.asList("Genesis Spell UT", "Gambler's Fate UT", "Chaotic Scripture UT", "Ballistic Star UT", "Oryx's Escutcheon UT");
        List<String> o3Armors = Arrays.asList("Diplomatic Robe UT", "Turncoat Cape UT", "Vesture of Duality UT", "Centaur's Shielding UT", "Gladiator Guard UT");
        List<String> o3Rings = Arrays.asList("Chancellor's Cranium UT", "Collector's Monocle UT", "Divine Coronation UT", "Battalion Banner UT", "Exalted God's Horn UT");

        long o3Whites = characters.stream().map(character -> {
            boolean weapon = o3Weapons.contains(character.getWeapon().name);
            boolean armor = o3Abilities.contains(character.getAbility().name);
            boolean ability = o3Armors.contains(character.getArmor().name);
            boolean rings = o3Rings.contains(character.getRing().name);
            return (weapon ? 1 : 0) + (armor ? 1 : 0) + (ability ? 1 : 0) + (rings ? 1 : 0);
        }).mapToInt(Integer::intValue).sum();

        List<String> eventWeapons = Arrays.asList("Dirk of Cronus UT", "Ray Katana UT", "Crystal Sword UT", "Crystal Wand UT");
        List<String> eventAbilities = Arrays.asList("Orb of Conflict UT", "Tablet of the King's Avatar UT", "Shield of Ogmur UT", "Seal of Blasphemous Prayer UT", "Helm of the Juggernaut UT", "Quiver of Thunder UT", "Crystallised Fangs Venom UT");

        long eventWhites = characters.stream().map(character -> {
            boolean weapon = eventWeapons.contains(character.getWeapon().name);
            boolean armor = eventAbilities.contains(character.getAbility().name);

            return (weapon ? 1 : 0) + (armor ? 1 : 0) ;
        }).mapToInt(Integer::intValue).sum();

        long noobDeaths = characters.stream().filter(character -> {
            boolean weapon = character.getWeapon().getTier() == 0;
            boolean ability = character.getAbility().getTier() == 0;
            return weapon && ability;
        }).count();

        String description = "```" +
                "General Breakdown: " + "\n" +
                "Number of Deaths    | " + characters.size() + "\n" +
                "Most Played Class   | " + StringUtils.capitalize(characters.stream().map(character -> character.getClassName().toLowerCase()).distinct().sorted(((o1, o2) ->
                Math.toIntExact(characters.stream().filter(character -> character.getClassName().equalsIgnoreCase(o2)).count() - characters.stream().filter(character -> character.getClassName().equalsIgnoreCase(o1)).count()))).findFirst().get()) + "\n" +
                "Least Played Class  | " + StringUtils.capitalize(characters.stream().map(character -> character.getClassName().toLowerCase()).distinct().sorted(((o1, o2) ->
                Math.toIntExact(characters.stream().filter(character -> character.getClassName().equalsIgnoreCase(o1)).count() - characters.stream().filter(character -> character.getClassName().equalsIgnoreCase(o2)).count()))).findFirst().get()) + "\n" +
                "Highest Base Fame   | " + characters.stream().map(Character::getFame).max(Long::compareTo).get() + "\n" +
                "Highest Total Fame  | " + characters.stream().map(Character::getTotalFame).max(Long::compareTo).get() + "\n" +
                "Noob Deaths (T0/T0) | " + noobDeaths + "\n" +
                "Backpacks           | " + characters.stream().filter(Character::isBackpack).count() + "\n" +
                "Decas               | " + decas + "\n\n" +
                "Tiered Items: " + "\n" +
                "Tier 13 Weapons     | " + characters.stream().filter(character -> character.getWeapon().getTier() == 13).count() + "\n" +
                "Tier 14 Weapons     | " + characters.stream().filter(character -> character.getWeapon().getTier() == 14).count() + "\n" +
//                "Tier 6 Abilities    | " + characters.stream().filter(character -> character.getAbility().getTier() == 6).count() + "\n" +
                "Tier 7 Abilities    | " + characters.stream().filter(character -> character.getAbility().getTier() == 7).count() + "\n" +
                "Tier 14 Armors      | " + characters.stream().filter(character -> character.getArmor().getTier() == 14).count() + "\n" +
                "Tier 15 Armors      | " + characters.stream().filter(character -> character.getArmor().getTier() == 15).count() + "\n\n" +
                "Deaths Breakdown: " + "\n" +
                "Lost Halls Deaths   | " + characters.stream().filter(character -> character.getKilledBy().split(" ")[0].equals("LH")).count() + "\n" +
                "Oryx 3 Deaths       | " + o3deaths + "\n" +
                "Mini-Boss Deaths    | " + miniBossDeaths + "\n\n" +
                "Whites Breakdown: " + "\n" +
                "Lost Halls Whites   | " + lostHallsWhites + "\n" +
                "Shatters Whites     | " + shattersWhites + "\n" +
                "Oryx 3 Whites       | " + o3Whites + "\n" +
                "Event Whites        | " + eventWhites + "\n" +
                "```";


        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Graveyard Summary: " + StringUtils.capitalize(args[0]) )
                .setColor(Goldilocks.LIGHTBLUE)
                .setDescription(description + "\n" + deathsBreakdown);

        msg.getTextChannel().sendMessage(embedBuilder.build()).queue();
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Graveyard");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Verified\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nGives a nice stats-breakdown of your Realmeye graveyard." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
