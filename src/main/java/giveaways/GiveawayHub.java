package giveaways;

import main.Database;
import main.Goldilocks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GiveawayHub {

    public static List<Giveaway> giveaways = new ArrayList<>();

    public GiveawayHub() {
        giveaways = Database.retrieveGiveaways();
        Goldilocks.TIMER.scheduleWithFixedDelay(() -> {
            for (Giveaway giveaway : giveaways) {
                giveaway.updateGiveaway();
            }
        }, 1L, 5L, TimeUnit.MINUTES);
    }

    public static Giveaway getGiveaway(String messageId) {
        for (Giveaway giveaway : giveaways) if (giveaway.giveawayMessage.getId().equals(messageId)) return giveaway;
        return null;
    }

}
