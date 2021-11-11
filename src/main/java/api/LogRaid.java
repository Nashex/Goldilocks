package api;

import javax.persistence.Entity;
import java.util.Objects;

@Entity
public class LogRaid {
    //GoogleSheets.logEvent(raidGuild, GoogleSheets.SheetsLogType.RAIDS, raidLeader.getEffectiveName(), raidLeader.getId(), dungeonName, raidType + "");
    private String guildId;
    private String raidLeaderName;
    private String raidLeaderId;
    private String dungeonName;

    LogRaid() { }

    LogRaid(String guildId, String raidLeaderName, String raidLeaderId, String dungeonName) {
        this.guildId = guildId;
        this.raidLeaderName = raidLeaderName;
        this.raidLeaderId = raidLeaderId;
        this.dungeonName = dungeonName;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getRaidLeaderName() {
        return raidLeaderName;
    }

    public String getRaidLeaderId() {
        return raidLeaderId;
    }

    public String getDungeonName() {
        return dungeonName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.guildId, this.raidLeaderName, this.raidLeaderId, this.dungeonName);
    }

    @Override
    public String toString() {
        return "LogRaid{" + "guildId=" + this.guildId + ", raidLeaderName='" + this.raidLeaderName + '\'' + ", raidLeaderId='" + this.raidLeaderId + '\''+ ", dungeonName='" + this.dungeonName + '}';
    }

}
