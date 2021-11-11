package verification;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerCheck {

    private double mainPercentage;
    private double altPercentage;

    private String charCount;
    private String exaltCount;
    private String aliveFame;
    private String rank;
    private String accFame;
    private String firstSeen;
    private String lastSeen;
    private String guildRank;
    private String deadChars;
    private String dead400;
    private String nameChanges;
    private String guildMemberCount;
    private String formerMemberCount;
    private String guildChangeCount;
    private String petLevel;


    public PlayerCheck() {
        mainPercentage = 0.0;
        altPercentage = 0.0;

        charCount = "Undefined";
        exaltCount = "Undefined";
        aliveFame = "Undefined";
        rank = "Undefined";
        accFame = "Undefined";
        firstSeen = "Undefined";
        lastSeen = "Undefined";
        guildRank = "Undefined";
        deadChars = "Undefined";
        dead400 = "Undefined";
        nameChanges = "Undefined";
        guildMemberCount = "Undefined";
        formerMemberCount = "Undefined";
        guildChangeCount = "Undefined";
        petLevel = "Undefined";
    }

}
