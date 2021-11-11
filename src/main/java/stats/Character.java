package stats;

import lombok.Getter;

@Getter
public class Character {

    private final String username;
    private final String name;
    private final boolean backpack;
    private final long fame;
    private final long level;
    private final long stats_maxed;
    private final String weapon;
    private final String ability;
    private final String armor;
    private final String ring;
    private final int[] statsArray;

    public Character(String username, String name, boolean backpack, long fame, long level, long stats_maxed, String weapon, String ability, String armor, String ring, int[] statArray) {
        this.username = username;
        this.name = name;
        this.backpack = backpack;
        this.fame = fame;
        this.level = level;
        this.stats_maxed = stats_maxed;
        this.weapon = weapon;
        this.ability = ability;
        this.armor = armor;
        this.ring = ring;
        this.statsArray = statArray;
    }
}
