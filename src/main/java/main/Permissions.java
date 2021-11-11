package main;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.List;

public class Permissions {

    public static boolean hasPermission (Member member, String[] eligibleRoles) {

        String guildId = member.getGuild().getId();

        if (eligibleRoles[0].equals("developer"))
            return member.getId().equals(Config.get("INSTANCE_OWNER"));

        if (member.hasPermission(Permission.ADMINISTRATOR) || member.getId().equals(Config.get("INSTANCE_OWNER")))
            return true;

        List<Role> roleList = Database.getEligibleRoles(guildId, eligibleRoles);
        for (Role role : member.getRoles()) if (roleList.contains(role)) return true;
        return false;
    }

}
