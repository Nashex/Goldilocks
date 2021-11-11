package verification;

import lombok.Getter;

import java.util.Arrays;
import java.util.stream.Collectors;

import static main.Goldilocks.jda;

@Getter
public class Character {

    String className;
    int level;
    String cqc;
    long fame;
    String place;
    String stats;

    String timeKilled;
    long totalFame;
    boolean backpack;
    String killedBy;

    Item weapon;
    Item ability;
    Item armor;
    Item ring;

    public Character(String className, String level, String cqc, String fame, String place, Item weapon, Item ability, Item armor, Item ring, String stats) {
        this.className = className;
        this.level = level.replaceAll("[^0-9]", "").isEmpty() ? 0 : Integer.parseInt(level.replaceAll("[^0-9]", ""));
        this.cqc = cqc;
        this.fame = fame.replaceAll("[^0-9]", "").isEmpty() ? 0L : Long.parseLong(fame.replaceAll("[^0-9]", ""));
        this.place = place;
        this.weapon = weapon;
        this.ability = ability;
        this.armor = armor;
        this.ring = ring;
        this.stats = stats;
    }

    public Character(String timeKilled, String className, String level, String baseFame,String totalFame, Item weapon, Item ability, Item armor, Item ring, boolean backpack, String stats, String killedBy) {
        this.timeKilled = timeKilled;
        this.className = className;
        this.level = level.replaceAll("[^0-9]", "").isEmpty() ? 0 : Integer.parseInt(level.replaceAll("[^0-9]", ""));
        this.fame = baseFame.replaceAll("[^0-9]", "").isEmpty() ? 0L : Long.parseLong(baseFame.replaceAll("[^0-9]", ""));
        this.totalFame = totalFame.replaceAll("[^0-9]", "").isEmpty() ? 0L : Long.parseLong(totalFame.replaceAll("[^0-9]", ""));
        this.weapon = weapon;
        this.ability = ability;
        this.armor = armor;
        this.ring = ring;
        this.backpack = backpack;
        this.stats = stats;
        this.killedBy = killedBy;
    }

    public String getMessageString() {
        String classEmote = jda.getEmotesByName(className, true).stream().filter(e -> Arrays.asList(new String[]{"767811138905178112", "793325895825883146"})
                .contains(e.getGuild().getId())).collect(Collectors.toList()).get(0).getAsMention();
        String fameString = fame + "<:fame:826360464865755136>";
        return classEmote + " | **`Lvl:`**`" + String.format("%1$2d", level) + "`|**`Fame:`**`" + String.format("%1$5d", fame) + "`|**`Stats:`**`" + stats + "` | " + weapon.getEmote() + ability.getEmote() + armor.getEmote() + ring.getEmote();   }
}
