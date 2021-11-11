package slashCommands;

import java.util.ArrayList;

public class SlashCommandHub extends ArrayList<SlashCommand> {

    public SlashCommand getCommand(String alias) {
        return stream().filter(cmd -> cmd.getName().equalsIgnoreCase(alias)).findAny().orElse(null);
    }

    @Override
    public boolean add(SlashCommand slashCommand) {
        return super.add(slashCommand);
    }
}