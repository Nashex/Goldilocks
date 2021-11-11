package commands;

import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;

public class CommandHub extends ArrayList<Command> {

    public Command getCommand(String alias) {
        return stream().filter(cmd -> Arrays.stream(cmd.getAliases()).anyMatch(s -> s.equalsIgnoreCase(alias))).findAny().orElse(null);
    }

    @Override
    public boolean add(Command command) {
        return super.add(command);
    }

    @AllArgsConstructor
    public enum CommandNameSpace {
        MOD("Moderation Commands"),
        PARSE("Parse Commands"),
        VERIFIED("Verified Commands"),
        RAID("Raid Commands"),
        LOBBY("Lobby Commands"),
        SETUP("Setup Commands"),
        CUSTOMIZATION("Customization Commands"),
        GAME("Game Commands"),
        DEBUG("Debug Commands"),
        DEVELOPER("Developer Commands");

        public String name;
    }

}
