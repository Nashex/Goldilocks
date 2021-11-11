package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.Date;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

public class CommandLockdown extends Command {
    public CommandLockdown() {
        setAliases(new String[] {"lockdown"});
        setEligibleRoles(new String[] {"mod", "hrl"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        TextChannel textChannel = msg.getTextChannel();

        Member self = textChannel.getGuild().getSelfMember();
        if (!self.hasPermission(textChannel, Permission.MANAGE_PERMISSIONS)) {
            textChannel.sendMessage("`❌` I do not have the proper permissions to lockdown this channel.").queue();
            return;
        }

        String lockdown = Database.getLockdown(textChannel);
        if (lockdown.isEmpty()) {
            lockdownChannel(textChannel, msg.getMember());
        } else {
            unlockChannel(textChannel, lockdown);
        }

    }

    public static void unlockChannel(TextChannel textChannel, String permString) {
        Guild guild = textChannel.getGuild();
        Message lockdownMessage = textChannel.sendMessage(Goldilocks.jda.getEmoteById("830993855808536616").getAsMention() + " Unlocking channel...").complete();
        Goldilocks.TIMER.schedule(() -> {
            String[] roleOverrides = permString.split(" ");
            for (String o : roleOverrides) {
                String[] overrides = o.split(":");
                Role role = guild.getRoleById(overrides[0]);
                EnumSet<Permission> perms = textChannel.getPermissionOverride(role).getAllowed();
                if (role != null) {
                    if (Boolean.parseBoolean(overrides[1])) {
                        if (!perms.contains(Permission.MESSAGE_WRITE) && !role.hasPermission(Permission.MESSAGE_WRITE)) perms.add(Permission.MESSAGE_WRITE);
                    }
                    if (Boolean.parseBoolean(overrides[2])) {
                        if (!perms.contains(Permission.MESSAGE_ADD_REACTION) && !role.hasPermission(Permission.MESSAGE_ADD_REACTION)) perms.add(Permission.MESSAGE_ADD_REACTION);
                    }
                }
                textChannel.upsertPermissionOverride(role).setAllow(perms).complete();
            }
            Database.removeLockdown(textChannel);
            lockdownMessage.editMessage("`🔓` **Channel Unlocked**").queue();
        }, 0L, TimeUnit.SECONDS);
    }

    public static void lockdownChannel(TextChannel textChannel, Member member) {
        Guild guild = textChannel.getGuild();
        Role officerRole = guild.getRoleById(Database.getGuildInfo(guild, "officerRole"));
        Message lockdownMessage = textChannel.sendMessage( Goldilocks.jda.getEmoteById("830993855808536616").getAsMention() + " Locking down channel...").complete();
        Goldilocks.TIMER.schedule(() -> {
            String permString = "";
            for (PermissionOverride p : textChannel.getRolePermissionOverrides()) {
                Role role = p.getRole();
                try {
                    if (role.getPosition() < officerRole.getPosition()) {
                        boolean canMessage = false, canReact = false;
                        EnumSet<Permission> denied = p.getDenied();
                        if (role.hasPermission(textChannel, Permission.MESSAGE_WRITE) || role.hasPermission(Permission.MESSAGE_WRITE)) {
                            denied.add(Permission.MESSAGE_WRITE);
                            canMessage = true;
                        }
                        if (role.hasPermission(textChannel, Permission.MESSAGE_ADD_REACTION) || role.hasPermission(Permission.MESSAGE_ADD_REACTION)) {
                            denied.add(Permission.MESSAGE_ADD_REACTION);
                            canReact = true;
                        }
                        if (!denied.isEmpty()) {
                            textChannel.upsertPermissionOverride(role).setAllow(p.getAllowed()).setDeny(denied).complete();
                            permString += role.getId() + ":" + canMessage + ":" + canReact + " ";
                        }
                    }
                } catch (Exception e) {
                    member.getUser().openPrivateChannel().complete().sendMessage("I cannot lock this channel for the role: " + role.getName()).queue();
                }
            }
            Database.logLockdown(textChannel, permString.trim());
            lockdownMessage.editMessage("`🔒` **Channel Locked Down**").queue();
        }, 0L, TimeUnit.SECONDS);
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Lockdown");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Moderator or Head Raid Leader\n";
        commandDescription += "Syntax: ;lockdown\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nLocks down a channel." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
