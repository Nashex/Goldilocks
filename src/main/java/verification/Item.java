package verification;

import main.Goldilocks;
import net.dv8tion.jda.api.entities.Emote;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class Item {
    public String slot;
    public String name;
    public String tier;

    public Item() { }

    public Item(String slot) {
        this.slot = slot;
    }

    public Item(String slot, String name, String tier) {
        this.slot = slot;
        this.name = name;
        this.tier = tier;
    }

    public int getTier() {
        int nTier = 20;
        if (!tier.replaceAll("[^0-9]", "").isEmpty())
            nTier = Integer.parseInt(tier.replaceAll("[^0-9]", ""));
        return nTier;
    }

    public String getEmote() {
        String name;
        if (!this.name.equals("Empty")) name = StringUtils.removeEnd(this.name, tier).replaceAll("[^A-Za-z]", "");
        else return getSlotEmote();
        List<Emote> emotes = Goldilocks.jda.getEmotesByName(name, true);
        return (emotes.isEmpty() ? getSlotEmote() : emotes.get(0).getAsMention());
    }

    public String getSlotEmote() {
        String emote = "⭕";
        switch (slot.toLowerCase()) {
            case "weapon":
                emote = "<:weapon:832452315944058940>";
                break;
            case "ability":
                emote = "<:ability:832452361532211200>";
                break;
            case "armor":
                emote = "<:armor:832452390015467580>";
                break;
            case "ring":
                emote = "<:ring:832452304954589225>";
                break;
        }
        return emote;
    }

    public static String getTier(String item) {
        return item.replaceAll("\\b(?!UT|ST|(T[0-9]))\\b\\w+\\W+", "");
    }
}
