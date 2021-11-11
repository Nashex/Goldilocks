package stats;

public class BarConstructor {
    public static String[] constructBar(int[] statValues, String classType) {

        int temp = statValues[5];
        statValues[5] = statValues[7];
        statValues[7] = statValues[6];
        statValues[6] = temp;

        String[] barArray = new String[8];

        int[] baseStats = getBaseStats(classType);

        int[] newStats = new int[8];
        for (int i = 0; i < 5; i++) {
            newStats[i] = statValues[i];
        }
        newStats[5] = statValues[6];
        newStats[6] = statValues[7];
        newStats[7] = statValues[5];

        double[] percentArray = new double[8];
        for (int i = 0; i < 8; i++) {
            double stat = newStats[i];
            double base = baseStats[i];
            percentArray[i] = stat / base;
        }

        for (int i = 0; i < 8; i++) {
            barArray[i] = createBar(percentArray[i]);
        }

        return barArray;

    }

    public static String getBarEmoji(double percentValue) {
        if (percentValue > .85) {
            return "🟩";
        }
        if (percentValue > .4) {
            return "🟨";
        }
        return "🟥";
    }

    public static String createBar(double percentValue) {
        String bar = "";
        double barValues = percentValue / .1;
        for (int i = 0; i < barValues; i++) {
            bar += getBarEmoji(percentValue);
        }
        double barFiller = 10 - barValues;
        for (int i = 0; i < barFiller; i++) {
            bar += "⬛";
        }
        return bar;
    }

    public static String[] getRatio(int[] statValues, String classType) {

        int temp = statValues[5];
        statValues[5] = statValues[7];
        statValues[7] = statValues[6];
        statValues[6] = temp;

        String[] ratioArray = new String[8];

        int[] baseStats = getBaseStats(classType);

        for (int i = 0; i < 8; i++) {
            ratioArray[i] = statValues[i] + "/" + baseStats[i];
        }

        return ratioArray;
    }

    public static int[] getToMax(int[] statValues, String classType) {
        int[] toMax = new int[8];

        int[] newStats = new int[8];
        for (int i = 0; i < 5; i++) {
            newStats[i] = statValues[i];
        }
        newStats[5] = statValues[6];
        newStats[6] = statValues[7];
        newStats[7] = statValues[5];

        int[] baseStats = getBaseStats(classType);

        toMax[0] = (baseStats[0] - newStats[0])/5;
        toMax[1] = (baseStats[1] - newStats[1])/5;
        for (int i = 2; i < 8; i++) {
            toMax[i] = baseStats[i] - newStats[i];
        }
        return toMax;
    }

                /*
                Life
                Mann
                Attack
                Defense
                Speed
                Dexterity
                Vitality
                Wisdom
                 */

    public static int[] getBaseStats(String classType) {
        int[] baseStats = new int[8];
        switch (classType) {
            case "Rogue":
                baseStats[0] = 720;
                baseStats[1] = 252;
                baseStats[2] = 50;
                baseStats[3] = 25;
                baseStats[4] = 75;
                baseStats[5] = 75;
                baseStats[6] = 40;
                baseStats[7] = 50;
                break;

            case "Archer":
                baseStats[0] = 700;
                baseStats[1] = 252;
                baseStats[2] = 75;
                baseStats[3] = 25;
                baseStats[4] = 50;
                baseStats[5] = 50;
                baseStats[6] = 40;
                baseStats[7] = 50;
                break;

            case "Wizard":
                baseStats[0] = 670;
                baseStats[1] = 385;
                baseStats[2] = 75;
                baseStats[3] = 25;
                baseStats[4] = 50;
                baseStats[5] = 75;
                baseStats[6] = 40;
                baseStats[7] = 60;
                break;

            case "Priest":
                baseStats[0] = 670;
                baseStats[1] = 385;
                baseStats[2] = 50;
                baseStats[3] = 25;
                baseStats[4] = 55;
                baseStats[5] = 55;
                baseStats[6] = 40;
                baseStats[7] = 75;
                break;

            case "Warrior":
                baseStats[0] = 770;
                baseStats[1] = 252;
                baseStats[2] = 75;
                baseStats[3] = 25;
                baseStats[4] = 50;
                baseStats[5] = 50;
                baseStats[6] = 75;
                baseStats[7] = 50;
                break;

            case "Knight":
                baseStats[0] = 770;
                baseStats[1] = 252;
                baseStats[2] = 50;
                baseStats[3] = 40;
                baseStats[4] = 50;
                baseStats[5] = 50;
                baseStats[6] = 75;
                baseStats[7] = 50;
                break;

            case "Paladin":
                baseStats[0] = 770;
                baseStats[1] = 252;
                baseStats[2] = 55;
                baseStats[3] = 30;
                baseStats[4] = 55;
                baseStats[5] = 55;
                baseStats[6] = 60;
                baseStats[7] = 75;
                break;

            case "Assassin":
                baseStats[0] = 720;
                baseStats[1] = 252;
                baseStats[2] = 60;
                baseStats[3] = 25;
                baseStats[4] = 75;
                baseStats[5] = 75;
                baseStats[6] = 40;
                baseStats[7] = 60;
                break;

            case "Necromancer":
                baseStats[0] = 670;
                baseStats[1] = 385;
                baseStats[2] = 75;
                baseStats[3] = 25;
                baseStats[4] = 50;
                baseStats[5] = 60;
                baseStats[6] = 40;
                baseStats[7] = 75;
                break;

            case "Huntress":
                baseStats[0] = 700;
                baseStats[1] = 252;
                baseStats[2] = 75;
                baseStats[3] = 25;
                baseStats[4] = 50;
                baseStats[5] = 50;
                baseStats[6] = 40;
                baseStats[7] = 50;
                break;

            case "Mystic":
                baseStats[0] = 670;
                baseStats[1] = 385;
                baseStats[2] = 65;
                baseStats[3] = 25;
                baseStats[4] = 60;
                baseStats[5] = 65;
                baseStats[6] = 40;
                baseStats[7] = 75;
                break;

            case "Trickster":
                baseStats[0] = 720;
                baseStats[1] = 252;
                baseStats[2] = 65;
                baseStats[3] = 25;
                baseStats[4] = 75;
                baseStats[5] = 75;
                baseStats[6] = 40;
                baseStats[7] = 60;
                break;

            case "Sorcerer":
                baseStats[0] = 670;
                baseStats[1] = 385;
                baseStats[2] = 70;
                baseStats[3] = 25;
                baseStats[4] = 60;
                baseStats[5] = 60;
                baseStats[6] = 75;
                baseStats[7] = 60;
                break;

            case "Ninja":
                baseStats[0] = 720;
                baseStats[1] = 252;
                baseStats[2] = 70;
                baseStats[3] = 25;
                baseStats[4] = 60;
                baseStats[5] = 70;
                baseStats[6] = 75;
                baseStats[7] = 60;
                break;

            case "Samurai":
                baseStats[0] = 720;
                baseStats[1] = 252;
                baseStats[2] = 75;
                baseStats[3] = 30;
                baseStats[4] = 55;
                baseStats[5] = 50;
                baseStats[6] = 60;
                baseStats[7] = 60;
                break;

            case "Bard":
                baseStats[0] = 670;
                baseStats[1] = 385;
                baseStats[2] = 55;
                baseStats[3] = 25;
                baseStats[4] = 55;
                baseStats[5] = 70;
                baseStats[6] = 45;
                baseStats[7] = 75;
                break;
        }
        return baseStats;
    }
}
