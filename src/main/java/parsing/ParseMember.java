package parsing;

import stats.BarConstructor;
import stats.Character;
import stats.RealmPlayer;

public class ParseMember {

    private static boolean parseList[] = new boolean[6];

    public static boolean[] parseMember(String userName, int charNumber){

        parseList[0] = false;
        parseList[1] = false;
        parseList[2] = false;
        parseList[3] = true;
        parseList[4] = false;
        parseList[5] = true;

        Character character = RealmPlayer.getCharacter(userName, charNumber);
        int[] baseStats = null;
        try {
            baseStats = BarConstructor.getBaseStats(character.getName());
        } catch (NullPointerException e) {
            return null;
        }
        int[] charStats = character.getStatsArray();

        //Check attack to max
        if ((baseStats[2] - charStats[2]) == 0)  parseList[0] = true;
        //System.out.println((baseStats[2] - charStats[2]));

        //Check dex to max
        if ((baseStats[5] - charStats[7])  == 0)  parseList[1] = true;
        //System.out.println((baseStats[5] - charStats[7]));

        for (int i = 15; i >= 10; i--) {
            String weaponName = character.getWeapon();
            String armorName = character.getArmor();
            if (weaponName.contains("T" + i) || weaponName.contains(" UT") || weaponName.contains(" ST")) {
                parseList[2] = true;
            }
            if (armorName.contains("T" + i) || armorName.contains(" UT") || armorName.contains(" ST")) {
                parseList[4] = true;
            }
        }

        for (int i = 0; i <= 3; i++) {
            String abilityName = character.getAbility();
            String ringName = character.getRing();
            if (!character.getName().equals("Trickster") && !character.getName().equals("Mystic") && !character.getName().equals("Archer")) {
                if (abilityName.contains("T" + i)) {
                    parseList[3] = false;
                }
            }
            if (ringName.contains("T" + i)) {
                parseList[5] = false;
            }
        }
        return parseList;
    }
}
