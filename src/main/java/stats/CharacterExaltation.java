package stats;

import main.Goldilocks;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;

import java.util.List;

public class CharacterExaltation {

    public final String[] HEADERS = {"HP", "MP", "ATK", "DEF", "SPD", "DEX", "VIT", "WIS"};

    public String characterName;
    public Emote characterEmote;
    public int numExaltations;
    public int[] exaltationData = new int[8];
    public Emote[] exaltationEmotes = new Emote[8];

    public CharacterExaltation(String characterName, String[] data, int numExaltations) {
        this.characterName = characterName;
        this.numExaltations = numExaltations;
        Guild CHAR_EMOTE_SERVER = Goldilocks.jda.getGuildById("767811138905178112");
        List<Emote> charEmotes = CHAR_EMOTE_SERVER.getEmotesByName(characterName, true);
        characterEmote = charEmotes.isEmpty() ? null : charEmotes.get(0);
        //String[] EMOTE_IDS = {"835112169607266304", "835112092628549682", "835112092524740618", "835112092419489802", "835112092394717245", "835112092662628352"};
        for (int i = 0; i < 8; i++) {
            if (data[i].isEmpty()) exaltationData[i] = 0;
            else exaltationData[i] = Integer.parseInt(data[i].replaceAll("[^0-9]", ""));
            //System.out.println(HEADERS[i] + (i < 2 ? exaltationData[i] / 5 : exaltationData[i]));
            exaltationEmotes[i] = Goldilocks.jda.getEmotesByName(HEADERS[i] + (i < 2 ? exaltationData[i] / 5 : exaltationData[i]), true).get(0);
        }
    }
}
