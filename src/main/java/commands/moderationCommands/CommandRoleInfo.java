package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import utils.Utils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CommandRoleInfo extends Command {
    public CommandRoleInfo() {
        setAliases(new String[] {"roleinfo", "ri", "rinfo", "list"});
        setEligibleRoles(new String[] {"vetRl","officer"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        if (args.length < 1) {
            Utils.errorMessage("Failed to Retrieve Members in Role", "No arguments were provided.", msg.getTextChannel(), 10L);
            return;
        }

        Guild guild = msg.getGuild();
        String roleName = String.join(" ", args);
        Role role = null;

        List<Role> guildRoles = guild.getRoles();
        for (Role r : guildRoles) {
            if (r.getName().equalsIgnoreCase(roleName)) role = r;
            if (Arrays.stream(r.getName().split(" ")).map(s -> s.substring(0, 1)).collect(Collectors.joining("")).equalsIgnoreCase(args[0])) role = r;
            if (r.getName().length() > 3 && r.getName().substring(0, 3).equalsIgnoreCase(args[0])) role = r;
            if (r.getId().equalsIgnoreCase(args[0])) role = r;
            if (role != null) break;
        }

        if (args[0].equalsIgnoreCase("roles") && alias.equalsIgnoreCase("list")) {
            String roleString = guildRoles.stream().map(Role::getAsMention).collect(Collectors.joining(", "));
            if (roleString.length() > 2000) roleString = roleString.substring(0, 2000);
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("List of Roles for " + guild.getName())
                    .setColor(Goldilocks.LIGHTBLUE)
                    .setDescription(roleString)
                    .setFooter("There are " + guildRoles.size() + " roles");
            msg.getTextChannel().sendMessage(embedBuilder.build()).queue();
            return;
        }

        if (role == null) {
            Utils.errorMessage("Failed to Retrieve Members in Role", "No role was found with that name/id", msg.getTextChannel(), 10L);
            return;
        }

        List<Member> memberList = guild.getMembersWithRoles(role);
        int rolePosition = role.getPosition();
        List<Member> highestMembers = memberList.stream().filter(m -> Utils.getUnHoistedHighestRole(m).getPosition() == rolePosition).collect(Collectors.toList());
        List<Member> higherMembers = memberList.stream().filter(m -> Utils.getUnHoistedHighestRole(m).getPosition() != rolePosition).collect(Collectors.toList());

        StringBuilder highest = new StringBuilder();
        for (Member member : highestMembers) {
            if (highest.length() + member.getAsMention().length() < 980) {
                highest.append(member.getAsMention()).append(" ");
            } else {
                highest.append("and ").append(highestMembers.size() - highestMembers.indexOf(member)).append(" others...");
                break;
            }
        }
        String highestString = highest.toString();

        StringBuilder higher = new StringBuilder();
        for (Member member : higherMembers) {
            if (higher.length() + member.getAsMention().length() < 980) {
                higher.append(member.getAsMention()).append(" ");
            } else {
                higher.append("and ").append(higherMembers.size() - higherMembers.indexOf(member)).append(" others...");
                break;
            }
        }
        String higherString = higher.toString();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Goldilocks.WHITE)
                .setTitle("Role info for: " + role.getName())
                .setDescription("**Role tag: **" + role.getAsMention() + " | Role Color: `" + (role.getColor() == null ? "None Set" : "#" + Integer.toHexString(role.getColor().getRGB())) + "`")
                .addField(higherMembers.size() + " Members with a higher role than `" + role.getName() + "` ", (higherString.isEmpty() ? "None" : higherString), false)
                .addField( highestMembers.size() + " Members with `" + role.getName() + "` as their highest role", (highestString.isEmpty() ? "None" : highestString), false)
                .setFooter("There are " + memberList.size() + " members in the " + role.getName() + " role")
                .setTimestamp(new Date().toInstant());

        msg.getTextChannel().sendMessage(embedBuilder.build()).queue();

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Role Info");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Veteran Raid Leader / Mod\n";
        commandDescription += "Syntax: ;alias <role name>\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nGives the info for a given role." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

}
