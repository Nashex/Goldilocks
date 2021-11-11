package setup;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

@Getter
@Setter
public class KeySection {
    private Guild guild;
    private Role keyRole;
    private int keyAmount;
    private boolean leaderboardRole;
    private String uniqueMessage;
    private boolean earlyLocation;

    public KeySection() {
        guild = null;
        keyRole = null;
        keyAmount = -1;
        leaderboardRole = false;
        uniqueMessage = "";
        earlyLocation = false;
    }

    public KeySection(Guild guild, Role keyRole, int keyAmount, boolean leaderboardRole, String uniqueMessage) {
        this.guild = guild;
        this.keyRole = keyRole;
        this.keyAmount = keyAmount;
        this.leaderboardRole = leaderboardRole;
        this.uniqueMessage = uniqueMessage;
    }
}
