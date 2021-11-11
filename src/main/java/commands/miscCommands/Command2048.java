package commands.miscCommands;

import commands.Command;
import commands.CommandHub;
import games.GameOf2048;
import main.Config;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Command2048 extends Command {
    public Command2048() {
        setAliases(new String[] {"2048","twentyfourtyeight"});
        setEligibleRoles(new String[] {"trl","tSec","game"});
        setGuildRank(1);
        setNameSpace(CommandHub.CommandNameSpace.GAME);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        if (Goldilocks.activeGames.containsKey(msg.getGuild())) {
            msg.getTextChannel().sendMessage("Someone is already playing a game.").complete().delete().queueAfter(10L, TimeUnit.SECONDS);
            msg.delete().queue();
            return;
        }

        String boardInfo = Database.get2048board(msg.getAuthor());
        if (!boardInfo.isEmpty()) {
            Message message = msg.getTextChannel().sendMessage("Would you like to resume from save?").complete();
            message.addReaction("✅").queue();
            message.addReaction("❌").queue();

            Goldilocks.eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
                return e.getMember().equals(msg.getMember()) && e.getMessageId().equals(message.getId())
                        && e.getReactionEmote().isEmoji() && ("✅❌").contains(e.getReactionEmote().getEmoji());
            }, e -> {
                String emote = e.getReactionEmote().getEmoji();

                if (("✅").equals(emote)) {
                    Goldilocks.TIMER.schedule(() -> new GameOf2048(msg.getTextChannel(), msg.getMember(), 4, true, boardInfo), 0L, TimeUnit.SECONDS);
                    message.delete().queue();
                }
                if (("❌").equals(emote)) {
                    try {
                        if (args.length > 0 && msg.getAuthor().getId().equals(Config.get("INSTANCE_OWNER"))) {
                            Goldilocks.TIMER.schedule(() -> new GameOf2048(msg.getTextChannel(), msg.getMember(), Integer.parseInt(args[0]), false, ""), 0L, TimeUnit.SECONDS);
                        }
                        else {
                            Goldilocks.TIMER.schedule(() -> new GameOf2048(msg.getTextChannel(), msg.getMember(), 4, false, ""), 0L, TimeUnit.SECONDS);
                        }
                    } catch (Exception e1) {
                        Goldilocks.TIMER.schedule(() -> new GameOf2048(msg.getTextChannel(), msg.getMember(), 4, false, ""), 0L, TimeUnit.SECONDS);
                    }
                    message.delete().queue();
                }
            }, 5L, TimeUnit.MINUTES, () -> {
                message.delete().queue();
            });
        } else {
            try {
                if (args.length > 0 && msg.getAuthor().getId().equals(Config.get("INSTANCE_OWNER"))) {
                    Goldilocks.TIMER.schedule(() -> new GameOf2048(msg.getTextChannel(), msg.getMember(), Integer.parseInt(args[0]), false, ""), 0L, TimeUnit.SECONDS);
                }
                else {
                    Goldilocks.TIMER.schedule(() -> new GameOf2048(msg.getTextChannel(), msg.getMember(), 4, false, ""), 0L, TimeUnit.SECONDS);
                }
            } catch (Exception e1) {
                Goldilocks.TIMER.schedule(() -> new GameOf2048(msg.getTextChannel(), msg.getMember(), 4, false, ""), 0L, TimeUnit.SECONDS);
            }
        }
        msg.delete().queue();
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: 2048");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Trial Security\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nPaly some 2048 with Goldilocks!" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
