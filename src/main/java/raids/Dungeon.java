package raids;

import java.awt.*;

public class Dungeon {
    public int dungeonIndex;
    public String dungeonName;
    public String dungeonCategory;
    public String[] dungeonInfo;
    public boolean enabled = true;

    public Dungeon(int dungeonIndex) {
        this.dungeonIndex = dungeonIndex;
        dungeonName = "Event Dungeon";
        dungeonCategory = "randomLobby";
        dungeonInfo = new String[]{"771201102464942101", "771201091840901122", "", "Event Dungeon", "", String.valueOf((new Color(255, 70, 70)).getRGB()), "50", "", ""};
    }

    public Dungeon(int dungeonIndex, String dungeonName, String dungeonCategory, String[] dungeonInfo) {
        this.dungeonIndex = dungeonIndex;
        this.dungeonName = dungeonName;
        this.dungeonCategory = dungeonCategory;
        this.dungeonInfo = dungeonInfo;
    }

    public Dungeon(int dungeonIndex, String dungeonName, String dungeonCategory, String[] dungeonInfo, boolean enabled) {
        this(dungeonIndex, dungeonName, dungeonCategory, dungeonInfo);
        this.enabled = enabled;
    }

    public boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    public void setEnabled(boolean bool) {
        enabled = bool;
    }

}
