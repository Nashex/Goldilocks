package pointSystem;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PointProfile {

    private int totalPoints;
    private int dailyRunStreak;
    private boolean runStreakMet;
    private int questRunStreak;
    private boolean questStreakMet;
    private int keyPopStreak;
    private boolean keyStreakMet;
    private double dailyRunMultiplier;
    private double questRunMultiplier;
    private double keyRunMultiplier;

    public PointProfile () {

        totalPoints = 100;
        dailyRunStreak = 1;
        runStreakMet = true;
        questRunStreak = 1;
        questStreakMet = true;
        keyPopStreak = 0;
        keyStreakMet = false;
        dailyRunMultiplier = 1.0;
        questRunMultiplier = 1.0;
        keyRunMultiplier = 1.0;

    }

}
