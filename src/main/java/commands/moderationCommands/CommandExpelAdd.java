package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CommandExpelAdd extends Command {
    public CommandExpelAdd() {
        setAliases(new String[] {"gdfggdf"});
        setEligibleRoles(new String[] {"hrl", "security"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        if (alias.equalsIgnoreCase("expelled")) {
            if (args.length == 0) return;
            String command = args[0];
            args = Arrays.copyOfRange(args, 1, args.length);
            if (command.equalsIgnoreCase("remove")) {
                Goldilocks.commands.getCommand("rexpel").execute(msg, alias, args);
                return;
            }
            else if (!command.equalsIgnoreCase("add")) return;
        }

        //Group the args in the format <name> <id> <name> <id> <name>
        Member member = msg.getMember();
        TextChannel textChannel = msg.getTextChannel();
        String currentName = "";
        String currentId = "";
        for (int i = 0; i < args.length; i++) {
            if (args[i].replaceAll("[^0-9]","").length() > 10) {
                if (!currentId.isEmpty()) promptExpel(currentId, currentName, member, textChannel);
                currentId = args[i].replaceAll("[^0-9]","");
            } else {
                if (!currentName.isEmpty()) promptExpel(currentId, currentName, member, textChannel);
                    currentName = args[i];
            }
            if (!currentId.isEmpty() && !currentName.isEmpty()) {
                promptExpel(currentId, currentName, member, textChannel);
                currentId = "";
                currentName = "";
            }
        }
        if (!currentId.isEmpty() || !currentName.isEmpty()) promptExpel(currentId, currentName, member, textChannel);
    }

    private void promptExpel(String id, String name, Member issuer, TextChannel textChannel) {

        if (!Database.isExpelled(id, issuer.getGuild()).isEmpty() || !Database.isExpelled(name, issuer.getGuild()).isEmpty()) {
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setColor(Goldilocks.RED)
                    .setTitle("User is currently expelled");
            textChannel.sendMessage(embedBuilder.build()).queue();
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Goldilocks.WHITE)
                .setTitle("Are you sure you would like to expel this user?")
                .setDescription((name.isEmpty() ? "" : "**`Name `: **" + name + "\n") +
                        (id.isEmpty() ? "" : "**`User `: **" + (textChannel.getGuild().getMemberById(id) != null ? textChannel.getGuild().getMemberById(id).getAsMention() : "Not in Server") + " | " + id + "\n"));

        textChannel.sendMessage(embedBuilder.build()).queue(m -> {
            m.addReaction("✅").queue();
            m.addReaction("❌").queue();

            Goldilocks.eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
                return e.getUser().equals(issuer.getUser()) && e.getReactionEmote().isEmoji() && ("✅❌").contains(e.getReactionEmote().getEmoji())
                        && e.getMessageId().equals(m.getId());
            }, e -> {
                String emote = e.getReactionEmote().getEmoji();
                if (("✅").equals(emote)) {
                    embedBuilder.setColor(Goldilocks.GREEN).setTitle("Successfully Expelled User");
                    //Database.expelAdd(id, name, issuer);
                } else {
                    embedBuilder.setColor(Goldilocks.RED).setTitle("User Expulsion Cancelled");
                }
                m.editMessage(embedBuilder.build()).queue(message -> message.clearReactions().queue());
            }, 2L, TimeUnit.MINUTES, () -> {
                m.delete().queue();
            });
        });
    }



    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Expel User");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Security / Head Raid Leader\n";
        commandDescription += "Syntax: ;expel <id> <name> [<id2> <name2> <name3>]\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nExpels a user(s) from verifying with the server." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
