package quota;

import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import setup.SetupConnector;
import shatters.SqlConnector;
import utils.Charts;
import utils.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class QuotaManager {

    public static HashMap<Guild, Message> quotaMessages = new HashMap<>();
    public static HashMap<Guild, Message> raidQuotaMessages = new HashMap<>();

    public static void updateQuotaMessage(Guild guild) {
        if (!quotaMessages.containsKey(guild)) {
            String quotaChannelId = SetupConnector.getFieldValue(guild, "guildLogs", "quotaChannelId");
            TextChannel quotaChannel = null;
            if (!quotaChannelId.isEmpty()) quotaChannel = guild.getTextChannelById(quotaChannelId);
            if (quotaChannel != null) {
                try {
                    if (quotaChannel.hasLatestMessage()) {
                        Message lastMessage = quotaChannel.getHistory().retrievePast(1).complete().get(0);
                        if (lastMessage.getAuthor().equals(Goldilocks.jda.getSelfUser())) lastMessage.delete().queue();
                    }
                } catch (Exception ignored) { }
                if (guild.getSelfMember().getPermissions(quotaChannel).contains(Permission.MESSAGE_READ)) {
                    Message quotaMessage = quotaChannel.sendMessage(getQuotaStyle(guild).build()).complete();
                    quotaMessages.put(guild, quotaMessage);
                }
            }
        } else {
            Goldilocks.TIMER.execute(() -> {
                try {
                    quotaMessages.get(guild).editMessage(getQuotaStyle(guild).build()).queue();
                } catch (Exception e) { e.printStackTrace(); }
            });
        }

        //Raid Quota
        if (!raidQuotaMessages.containsKey(guild)) {
            String quotaChannelId = SetupConnector.getFieldValue(guild, "guildLogs", "raidQuotaChannelId");
            TextChannel quotaChannel = null;
            if (!quotaChannelId.isEmpty()) quotaChannel = guild.getTextChannelById(quotaChannelId);
            if (quotaChannel != null) {
                try {
                    if (quotaChannel.hasLatestMessage()) {
                        Message lastMessage = quotaChannel.getHistory().retrievePast(1).complete().get(0);
                        if (lastMessage.getAuthor().equals(Goldilocks.jda.getSelfUser())) lastMessage.delete().queue();
                    }
                } catch (Exception ignored) { }
                if (guild.getSelfMember().getPermissions(quotaChannel).contains(Permission.MESSAGE_READ)) {
                    Message quotaMessage = quotaChannel.sendMessage(getSecondaryQuotaStyle(guild).build()).complete();
                    raidQuotaMessages.put(guild, quotaMessage);
                }
            }
        } else {
            Goldilocks.TIMER.execute(() -> {
                try {
                    raidQuotaMessages.get(guild).editMessage(getSecondaryQuotaStyle(guild).build()).queue();
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
    }

    public static EmbedBuilder getQuotaStyle(Guild guild) {
        String quotaStyle = SetupConnector.getFieldValue(guild, "guildInfo", "quotaStyle");

        switch (quotaStyle) {
            case "0":
                return quotaEmbed(guild);
            case "1":
                return shattersQuotaEmbed(guild);
            case "2":
                return parseAssistEmbed(guild);
            case "3":
                return parseEmbed(guild);
            case "4":
                return fullQuotaEmbed(guild);
        }

        return quotaEmbed(guild);
    }

    public static EmbedBuilder getSecondaryQuotaStyle(Guild guild) {
        String quotaStyle = SetupConnector.getFieldValue(guild, "guildInfo", "secQuotaStyle");

        switch (quotaStyle) {
            case "0":
                return runsAssistEmbed(guild);
        }
        return quotaEmbed(guild);
    }

    public static void updateQuotaTimes(String timeString) {
        HashMap<Guild, Message> updatedMessages = new HashMap<>();
        HashMap<Guild, Message> finalUpdatedMessages = updatedMessages;
        quotaMessages.forEach(((guild, message) -> {
            try {
                message.editMessage(getQuotaStyle(guild)
                        .setDescription("```yaml\nQuota Reset in:   " + timeString + "\n```")
                        .build()).queue();
            } catch (ErrorResponseException e) {
                e.printStackTrace();
                TextChannel quotasChannel = guild.getTextChannelsByName("staff-quotas", true).get(0);
                Message newMessage = quotasChannel.sendMessage(getQuotaStyle(guild).build()).complete();
                finalUpdatedMessages.put(guild, newMessage);
            }
        }));

        for (Map.Entry<Guild, Message> e : updatedMessages.entrySet()) quotaMessages.replace(e.getKey(), e.getValue());

        //Raid messages
        updatedMessages = new HashMap<>();
        HashMap<Guild, Message> finalUpdatedMessages1 = updatedMessages;
        raidQuotaMessages.forEach(((guild, message) -> {
            try {
                message.editMessage(getSecondaryQuotaStyle(guild)
                        .setDescription("```ini\n[Quota Reset in]:   " + timeString + "\n```")
                        .build()).queue();
            } catch (ErrorResponseException e) {
                e.printStackTrace();
                TextChannel quotasChannel = guild.getTextChannelsByName("staff-quotas", true).get(0);
                Message newMessage = quotasChannel.sendMessage(getSecondaryQuotaStyle(guild).build()).complete();
                finalUpdatedMessages1.put(guild, newMessage);
            }
        }));

        for (Map.Entry<Guild, Message> e : updatedMessages.entrySet()) raidQuotaMessages.replace(e.getKey(), e.getValue());
    }

    public static void resetQuota() {
        List<Guild> guilds = Goldilocks.jda.getGuilds().stream().filter(g -> !SetupConnector.getFieldValue(g, "guildLogs", "quotaChannelId").equals("0"))
                .collect(Collectors.toList());

        for (Guild guild : guilds) Goldilocks.TIMER.execute(() -> resetQuota(guild));
    }

    public static void resetQuota(Guild guild) {
        try {
            logUnmetQuota(guild);

            String quotaHistoryChannelId = SetupConnector.getFieldValue(guild, "guildLogs", "quotaHistoryChannelId");
            if (!quotaHistoryChannelId.isEmpty()) {
                TextChannel quotaHistoryChannel = Goldilocks.jda.getTextChannelById(quotaHistoryChannelId);
                if (quotaHistoryChannel != null) quotaHistoryChannel.sendMessage(getQuotaStyle(guild).build()).complete();
            }
            String raidQuotaHistoryChannelId = SetupConnector.getFieldValue(guild, "guildLogs", "raidQuotaHistoryChannelId");
            if (!quotaHistoryChannelId.isEmpty()) {
                TextChannel raidQuotaHistoryChannel = Goldilocks.jda.getTextChannelById(raidQuotaHistoryChannelId);
                if (raidQuotaHistoryChannel != null) raidQuotaHistoryChannel.sendMessage(getSecondaryQuotaStyle(guild).build()).complete();
            }
            Database.resetQuota(guild.getId());
            updateQuotaMessage(guild);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void logUnmetQuota(Guild guild) {

        // Get leaders on leave
        List<String> leaveKeyWords = new ArrayList<>(Arrays.asList("leave", "lol", "triforcej"));
        List<Member> leadersOnLeave = new ArrayList<>();
        List<Member> updatedMembers = new ArrayList<>();
        String staffUpdatesChannelId = SetupConnector.getFieldValue(guild, "guildLogs", "staffUpdatesChannelId");
        if (!staffUpdatesChannelId.isEmpty()) {
            TextChannel staffUpdates = Goldilocks.jda.getTextChannelById(staffUpdatesChannelId);
            if (staffUpdates != null) {
                staffUpdates.getHistory().retrievePast(100).complete().stream()
                        .filter(m -> m.getTimeCreated().toEpochSecond() > ((System.currentTimeMillis() - 604800000) / 1000) )
                        .forEach(m -> {
                            if (!m.getMentionedMembers().isEmpty()) {
                                if (Arrays.stream(m.getContentRaw().toLowerCase().replaceAll("[^A-Za-z ]", "").split(" ")).anyMatch(leaveKeyWords::contains)) {
                                    // Add all of the mentioned members to the list
                                    leadersOnLeave.addAll(m.getMentionedMembers());
                                } else {
                                    updatedMembers.addAll(m.getMentionedMembers());
                                }
                            }
                        });
            }
        }

        List<QuotaRole> quotaRoles = SetupConnector.getQuotaRoles(guild);
        if (quotaRoles.isEmpty()) return;

        String unmetModChannelId = SetupConnector.getFieldValue(guild, "guildLogs", "unmetModQuotaChannelId");
        String unmetRlChannelId = SetupConnector.getFieldValue(guild, "guildLogs", "unmetRlQuotaChannelId");
        //String unmetEoChannelId = SetupConnector.getFieldValue(guild, "guildLogs", "unmetEoQuotaChannelId");

        TextChannel unmetModChannel = null, unmetRlChannel = null, unmetEoChannel = null;
        if (!unmetModChannelId.isEmpty()) unmetModChannel = guild.getTextChannelById(unmetModChannelId);
        if (!unmetRlChannelId.isEmpty()) unmetRlChannel = guild.getTextChannelById(unmetRlChannelId);
        //if (!unmetEoChannelId.isEmpty()) unmetEoChannel = guild.getTextChannelById(unmetEoChannelId);

        // Send the quota to the channels
        if (unmetModChannel != null) sendUnmetQuota(unmetModChannel, quotaRoles, leadersOnLeave, updatedMembers, "tSecRole", "securityRole", "officerRole", "modRole");
        if (unmetRlChannel != null) sendUnmetQuota(unmetRlChannel, quotaRoles, leadersOnLeave, updatedMembers, "arlRole", "rlRole", "vetRlRole");
        //if (unmetEoChannel != null) sendUnmetQuota(unmetEoChannel, quotaRoles, "eoRole");
    }

    public static void sendUnmetQuota(TextChannel textChannel, List<QuotaRole> quotaRoles, List<Member> leadersOnLeave, List<Member> staffUpdates, String... roleColumns) {
        List<Role> roles = getRoleList(textChannel.getGuild(), roleColumns);

        quotaRoles = quotaRoles.stream().filter(q -> roles.contains(q.role)).collect(Collectors.toList());
        if (quotaRoles.isEmpty()) return;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Unmet Quota Summary for " + textChannel.getGuild().getName())
                .setColor(Goldilocks.RED)
                .setDescription("Any roles marked with `LEAVE` have had a staff update for being on leave within the quota period. Any marked with `UPDATE` have had another, uncategorized staff update within the quota period." +
                        "\n\n **The following roles have a quota:\n**" +
                        quotaRoles.stream().map(q -> "`" + String.format("%-18s", q.role.getName()) + String.format("| Runs: %-3d Assists: %-3d Parses: %-3d", q.runs, q.assists, q.parses) + "`")
                .collect(Collectors.joining("\n")));

        boolean isShatters = Database.isShatters(textChannel.getGuild());
        boolean isFungal = Database.isFungal(textChannel.getGuild());

        for (QuotaRole quotaRole : quotaRoles) {
            String roleResult = "";
            List<Member> members = textChannel.getGuild().getMembersWithRoles(quotaRole.role).stream()
                    .filter(m -> Utils.getHighestRole(m).equals(quotaRole.role)).collect(Collectors.toList());

            for (Member m : members) {
                String[] memberQuotaNums = Database.getQuota(textChannel.getGuild().getId(), m.getId());
                // Run calculation
                int runs = Integer.parseInt(memberQuotaNums[0]);
                double assists = Integer.parseInt(memberQuotaNums[1]); // Todo add per guild assist multiplier
                int parses = Integer.parseInt(memberQuotaNums[2]);
                if (quotaRole.minRunsForAssists > 0 && runs < quotaRole.minRunsForAssists) assists = 0;
                double totalRuns = runs + .5 * assists;

                if (isShatters) {
                    memberQuotaNums = SqlConnector.shattersStats(m.getUser());
                    runs = Integer.parseInt(memberQuotaNums[6]);
                    assists = Integer.parseInt(memberQuotaNums[7]);
                    parses = Integer.parseInt(Database.getStaffData(m, "quotaParses"));
                    if (quotaRole.minRunsForAssists > 0 && runs < quotaRole.minRunsForAssists) assists = 0;
                    totalRuns = runs + .5 * assists;
                }

                if (quotaRole.minRunsForAssists < 0) {

                    int[] memberQuotaPoints = Database.getAllQuota(m.getId(), m.getId());
                    if (memberQuotaPoints[0] < (-1 * quotaRole.minRunsForAssists)) {
                        String memberName = m.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                        String modString = String.format("%-12s [Total Points]: %-3s\n;\t[P]: %-2s [V]: %-2s [A]: %-2s [N]: %-2s [M]: %-2s",
                                memberName, memberQuotaPoints[0] + "", memberQuotaPoints[3] + "", memberQuotaPoints[4] + "", memberQuotaPoints[6] + "" ,memberQuotaPoints[5] + "", memberQuotaPoints[7] + "") + "\n";
                        if (leadersOnLeave.contains(m)) roleResult += "<LEAVE>";
                        if (staffUpdates.contains(m)) roleResult += "<UPDATE>";
                    }

                } else if (totalRuns < quotaRole.runs || assists < quotaRole.assists || parses < quotaRole.parses) {
                    roleResult += String.format("%-12s Runs: %-3s Assists: %-3s Parses: %-3s", m.getEffectiveName().split(" ")[0].replaceAll("[^A-Za-z]", "")
                            , memberQuotaNums[0] + "", memberQuotaNums[1], memberQuotaNums[2]);
                    if (leadersOnLeave.contains(m)) roleResult += "<LEAVE>";
                    if (staffUpdates.contains(m)) roleResult += "<UPDATE>";
                    roleResult += "\n";
                }
            }
            if (roleResult.length() > 1000) {
                embedBuilder.addField(quotaRole.role.getName(), "```xml\n" + (roleResult.isEmpty() ? "Everyone met quota! 🎊" : roleResult.substring(0 , 1000)) + "\n```", false);
                embedBuilder.addField(quotaRole.role.getName() + "(Cont)", "```xml\n" + (roleResult.isEmpty() ? "Everyone met quota! 🎊" : roleResult.substring(1000)) + "\n```", false);
            } else {
                embedBuilder.addField(quotaRole.role.getName(), "```xml\n" + (roleResult.isEmpty() ? "Everyone met quota! 🎊" : roleResult) + "\n```", false);
            }

        }

        embedBuilder.setTimestamp(new Date().toInstant());
        textChannel.sendMessage(embedBuilder.build()).queue();
    }

    public static List<Role> getRoleList(Guild guild, String... columns) {
        List<Role> roleList = new ArrayList<>();
        for (String s : columns) {
            String roleId;
            Role role = null;
            if (!(roleId = SetupConnector.getFieldValue(guild, "guildInfo", s)).equals("0")) {
                role = guild.getRoleById(roleId);
                if (role != null) roleList.add(role);
            }
        }
        return roleList;
    }

    public static EmbedBuilder parseEmbed(Guild guild) {
        EmbedBuilder quotaEmbed = new EmbedBuilder();
        quotaEmbed.setColor(Goldilocks.GOLD);
        quotaEmbed.setTitle(guild.getName() + "'s Parses");
        String topAssistsString = "```yaml\n";
        String[][] topParses = Database.getTopQuota("quotaParses", guild.getId());
        if (topParses != null) {

            // Parses leaderboard
            try {
                for (int i = 0; i < 3; i++) {
                    String memberName = guild.getMemberById(topParses[i][0]).getEffectiveName().split(" ")[0].replaceAll("[^A-Za-z]","");
                    String memberAssists  = "" + String.format("%-12s Parses: ",memberName);
                    switch (i) {
                        case 0:
                            topAssistsString += "\n🥇 First Place:   "  + memberAssists + topParses[0][1];
                            break;
                        case 1:
                            topAssistsString += "\n🥈 Second Place:  "  + memberAssists + topParses[1][1];
                            break;
                        case 2:
                            topAssistsString += "\n🥉 Third Place:   "  + memberAssists + topParses[2][1];
                            break;
                    }
                }
                quotaEmbed.addField("**Top Parses**", topAssistsString + "\n```", false);
            } catch (Exception e) {}
        }

        //Moderator Field
        if (!SetupConnector.getFieldValue(guild, "guildInfo","modRole").equals("0")) {
            Role moderator = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","modRole"));
            String moderatorString = "```apache\n";
            List<Member> moderators = guild.getMembersWithRoles(moderator);
            moderators.sort(new Comparator<Member>() {
                @Override
                public int compare(Member m1, Member m2) {
                    return (int) (Integer.parseInt(Database.getAssistsParses(guild.getId(), m2.getId())[1]) - Integer.parseInt(Database.getAssistsParses(guild.getId(), m1.getId())[1]));
                }
            });
            for (Member member : moderators) {
                if (!member.getPermissions().contains(Permission.ADMINISTRATOR)) {
                    String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                    String[] memberQuotaNums = Database.getAssistsParses(guild.getId(), member.getId());
                    moderatorString += String.format("%-12s Parses: %-3s", memberName, memberQuotaNums[1]) + "\n";
                }
            }
            quotaEmbed.addField(moderator.getName() + "s", moderatorString + "\n```", false);
        }
        //Officer Field
        if (!SetupConnector.getFieldValue(guild, "guildInfo","officerRole").equals("0")) {
            Role moderator = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","modRole"));
            Role officer = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","officerRole"));
            String officerString = "```apache\n";
            List<Member> officers = guild.getMembersWithRoles(officer);
            officers.sort(new Comparator<Member>() {
                @Override
                public int compare(Member m1, Member m2) {
                    return (int) (Integer.parseInt(Database.getAssistsParses(guild.getId(), m2.getId())[1]) - Integer.parseInt(Database.getAssistsParses(guild.getId(), m1.getId())[1]));
                }
            });
            for (Member member : officers) {
                if (!member.getRoles().contains(moderator) && !member.getPermissions().contains(Permission.ADMINISTRATOR)) {
                    String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                    String[] memberQuotaNums = Database.getAssistsParses(guild.getId(), member.getId());
                    officerString += String.format("%-12s Parses: %-3s", memberName, memberQuotaNums[1]) + "\n";
                }
            }
            quotaEmbed.addField(officer.getName() + "s", officerString + "\n```", false);
        }

        //Security Field
        if (!SetupConnector.getFieldValue(guild, "guildInfo","securityRole").equals("0")) {
            Role officer = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","officerRole"));
            Role security = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","securityRole"));
            String securityString = "```apache\n";
            List<Member> securities = guild.getMembersWithRoles(security);
            securities.sort(new Comparator<Member>() {
                @Override
                public int compare(Member m1, Member m2) {
                    return (int) (Integer.parseInt(Database.getAssistsParses(guild.getId(), m2.getId())[1]) - Integer.parseInt(Database.getAssistsParses(guild.getId(), m1.getId())[1]));
                }
            });
            for (Member member : securities) {
                if (!member.getRoles().contains(officer) && !member.getPermissions().contains(Permission.ADMINISTRATOR)) {
                    String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                    String[] memberQuotaNums = Database.getAssistsParses(guild.getId(), member.getId());
                    securityString += String.format("%-12s Parses: %-3s", memberName, memberQuotaNums[1]) + "\n";
                }
            }
            quotaEmbed.addField((security.getName().endsWith("y") ? security.getName().replace("y", "ies") : security.getName() + "s"), securityString + "\n```", false);
        }
        //Trial Security Field
        if (!SetupConnector.getFieldValue(guild, "guildInfo","tSecRole").equals("0")) {
            Role trialSecurity = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","tSecRole"));
            String trialSecurityString = "```apache\n";
            List<Member> helpers = guild.getMembersWithRoles(trialSecurity);
            helpers.sort(new Comparator<Member>() {
                @Override
                public int compare(Member m1, Member m2) {
                    return (int) (Integer.parseInt(Database.getAssistsParses(guild.getId(), m2.getId())[1]) - Integer.parseInt(Database.getAssistsParses(guild.getId(), m1.getId())[1]));
                }
            });
            for (Member member : helpers) {
                String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                String[] memberQuotaNums = Database.getAssistsParses(guild.getId(), member.getId());
                trialSecurityString += String.format("%-12s Parses: %-3s", memberName, memberQuotaNums[1]) + "\n";
            }
            quotaEmbed.addField(trialSecurity.getName() + "s", trialSecurityString + "\n```", false);
        }
        quotaEmbed.setImage(Charts.createParseChart(guild));
        quotaEmbed.setFooter(guild.getName() + "'s Parses");
        quotaEmbed.setTimestamp(new Date().toInstant());
        return quotaEmbed;
    }

    public static EmbedBuilder parseAssistEmbed(Guild guild) {
        EmbedBuilder quotaEmbed = new EmbedBuilder();
        quotaEmbed.setColor(Goldilocks.GOLD);
        quotaEmbed.setTitle(guild.getName() + "'s Assists and Parses");
        String topParsesString = "```yaml\n";
        String topAssistsString = "```yaml\n";
        String[][] topAssists = Database.getTopQuota("quotaAssists", guild.getId());
        String[][] topParses = Database.getTopQuota("quotaParses", guild.getId());
        if (topAssists != null) {
            // Assists leaderboard
            try {
                for (int i = 0; i < 3; i++) {
                    String memberName = guild.getMemberById(topAssists[i][0]).getEffectiveName().split(" ")[0].replaceAll("[^A-Za-z]","");
                    String memberAssists  = "" + String.format("%-12s Assists: ",memberName);
                    switch (i) {
                        case 0:
                            topAssistsString += "\n🥇 First Place:   "  + memberAssists + topAssists[0][1];
                            break;
                        case 1:
                            topAssistsString += "\n🥈 Second Place:  "  + memberAssists + topAssists[1][1];
                            break;
                        case 2:
                            topAssistsString += "\n🥉 Third Place:   "  + memberAssists + topAssists[2][1];
                            break;
                    }
                }
                quotaEmbed.addField("**Top Assists**", topAssistsString + "\n```", false);
            } catch (Exception ignored) { }
        }

        if (topParses != null) {
            // Parses leaderboard
            try {
                for (int i = 0; i < 3; i++) {
                    String memberName = guild.getMemberById(topParses[i][0]).getEffectiveName().split(" ")[0].replaceAll("[^A-Za-z]","");
                    String memberAssists  = "" + String.format("%-12s Parses: ",memberName);
                    switch (i) {
                        case 0:
                            topParsesString += "\n🥇 First Place:   "  + memberAssists + topParses[0][1];
                            break;
                        case 1:
                            topParsesString += "\n🥈 Second Place:  "  + memberAssists + topParses[1][1];
                            break;
                        case 2:
                            topParsesString += "\n🥉 Third Place:   "  + memberAssists + topParses[2][1];
                            break;
                    }
                }
                quotaEmbed.addField("**Top Parses**", topParsesString + "\n```", false);
            } catch (Exception ignored) { }
        }

        // Rl Fields
        MessageEmbed.Field hrlField = quotaAssistField(guild, "headRlRole", "adminRole");
        MessageEmbed.Field vrlField = quotaAssistField(guild, "vetRlRole", "headRlRole");
        MessageEmbed.Field rlField = quotaAssistField(guild, "rlRole", "vetRlRole");
        MessageEmbed.Field arlField = quotaAssistField(guild, "arlRole", "rlRole");

        if (hrlField != null) quotaEmbed.addField(hrlField);
        if (vrlField != null) quotaEmbed.addField(vrlField);
        if (rlField != null) quotaEmbed.addField(rlField);
        if (arlField != null) quotaEmbed.addField(arlField);

        // Moderation Fields
        MessageEmbed.Field modField = quotaAssistField(guild, "modRole", "adminRole");
        MessageEmbed.Field officerField = quotaAssistField(guild, "officerRole", "modRole");
        MessageEmbed.Field securityField = quotaAssistField(guild, "securityRole", "officerRole");
        MessageEmbed.Field helperField = quotaAssistField(guild, "tSecRole", "securityRole");

        if (modField != null) quotaEmbed.addField(modField);
        if (officerField != null) quotaEmbed.addField(officerField);
        if (securityField != null) quotaEmbed.addField(securityField);
        if (helperField != null) quotaEmbed.addField(helperField);

        quotaEmbed.setImage(Charts.createParseAssistChart(guild));
        quotaEmbed.setFooter(guild.getName() + "'s Parses and Assists");
        quotaEmbed.setTimestamp(new Date().toInstant());
        return quotaEmbed;
    }

    public static MessageEmbed.Field quotaAssistField(Guild guild, String roleName, String higherRole) {
        if (!SetupConnector.getFieldValue(guild, "guildInfo",roleName).equals("0")) {

            String higherRoleId = SetupConnector.getFieldValue(guild, "guildInfo", higherRole);
            String roleId = SetupConnector.getFieldValue(guild, "guildInfo", roleName);
            if (roleId.isEmpty() || higherRoleId.isEmpty()) return null;

            Role higher = guild.getRoleById(higherRoleId);
            Role role = guild.getRoleById(roleId);
            String securityString = "```apache\n";
            List<Member> roleMembers = guild.getMembersWithRoles(role).stream().filter(m -> !m.getRoles().contains(higher) && !m.getPermissions().contains(Permission.ADMINISTRATOR)).collect(Collectors.toList());
            if (roleMembers.isEmpty()) return null;
            roleMembers.sort(new Comparator<Member>() {
                @Override
                public int compare(Member m1, Member m2) {
                    return (int) (Integer.parseInt(Database.getAssistsParses(guild.getId(), m2.getId())[0]) - Integer.parseInt(Database.getAssistsParses(guild.getId(), m1.getId())[0]));
                }
            });
            for (Member member : roleMembers) {
                String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                String[] memberQuotaNums = Database.getAssistsParses(guild.getId(), member.getId());
                securityString += String.format("%-12s Assists: %-3s Parses: %s", memberName, memberQuotaNums[0], memberQuotaNums[1]) + "\n";
            }
            return new MessageEmbed.Field((role.getName().endsWith("y") ? role.getName().replace("y", "ies") : role.getName() + "s"), securityString + "\n```", false);
        }
        return null;
    }

    public static EmbedBuilder runsAssistEmbed(Guild guild) {
        EmbedBuilder quotaEmbed = new EmbedBuilder();
        quotaEmbed.setColor(Goldilocks.BLUE);
        quotaEmbed.setTitle(guild.getName() + "'s Runs and Assists");
        String topRunsString = "```ini\n";
        String topAssistsString = "```ini\n";
        String[][] topRuns = Database.getTopQuota("quotaRuns", guild.getId());
        String[][] topAssists = Database.getTopQuota("quotaAssists", guild.getId());
        if (topRuns != null) {
            //Top Runs leaderboard
            try {
                for (int i = 0; i < 3; i++) {
                    String memberName = guild.getMemberById(topRuns[i][0]).getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                    String memberRuns  = "" + String.format("%-12s [Runs]: ",memberName);
                    switch (i) {
                        case 0:
                            topRunsString += "\n🥇 First Place:   "  + memberRuns + topRuns[0][1];
                            break;
                        case 1:
                            topRunsString += "\n🥈 Second Place:  "  + memberRuns + topRuns[1][1];
                            break;
                        case 2:
                            topRunsString += "\n🥉 Third Place:   "  + memberRuns + topRuns[2][1];
                            break;
                    }
                }
                quotaEmbed.addField("**Top Runs**", topRunsString + "\n```", false);
            } catch (Exception e) {}

            //Assists leaderboard
            try {
                for (int i = 0; i < 3; i++) {
                    String memberName = guild.getMemberById(topAssists[i][0]).getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                    String memberAssists  = "" + String.format("%-12s [Assists]: ",memberName);
                    switch (i) {
                        case 0:
                            topAssistsString += "\n🥇 First Place:   "  + memberAssists + topAssists[0][1];
                            break;
                        case 1:
                            topAssistsString += "\n🥈 Second Place:  "  + memberAssists + topAssists[1][1];
                            break;
                        case 2:
                            topAssistsString += "\n🥉 Third Place:   "  + memberAssists + topAssists[2][1];
                            break;
                    }
                }
                quotaEmbed.addField("**Top Assists**", topAssistsString + "\n```", false);
            } catch (Exception e) {}
        }

        // Rl Fields
        MessageEmbed.Field hrlField = quotaRunsAssistField(guild, "headRlRole", "adminRole");
        MessageEmbed.Field vrlField = quotaRunsAssistField(guild, "vetRlRole", "headRlRole");
        MessageEmbed.Field rlField = quotaRunsAssistField(guild, "rlRole", "vetRlRole");
        MessageEmbed.Field arlField = quotaRunsAssistField(guild, "arlRole", "rlRole");

        if (hrlField != null) quotaEmbed.addField(hrlField);
        if (vrlField != null) quotaEmbed.addField(vrlField);
        if (rlField != null) quotaEmbed.addField(rlField);
        if (arlField != null) quotaEmbed.addField(arlField);

        quotaEmbed.setImage(Charts.createQuotaChart(guild));
        quotaEmbed.setFooter(guild.getName() + "'s Runs and Assists");
        quotaEmbed.setTimestamp(new Date().toInstant());
        return quotaEmbed;
    }

    public static MessageEmbed.Field quotaRunsAssistField(Guild guild, String roleName, String higherRole) {
        if (!SetupConnector.getFieldValue(guild, "guildInfo",roleName).equals("0")) {

            String higherRoleId = SetupConnector.getFieldValue(guild, "guildInfo", higherRole);
            String roleId = SetupConnector.getFieldValue(guild, "guildInfo", roleName);
            if (roleId.isEmpty() || higherRoleId.isEmpty()) return null;

            Role higher = guild.getRoleById(higherRoleId);
            Role role = guild.getRoleById(roleId);
            String securityString = "```ini\n";
            List<Member> roleMembers = guild.getMembersWithRoles(role).stream().filter(m -> !m.getRoles().contains(higher) && !m.getPermissions().contains(Permission.ADMINISTRATOR)).collect(Collectors.toList());
            if (roleMembers.isEmpty()) return null;
            roleMembers.sort(new Comparator<Member>() {
                @Override
                public int compare(Member m1, Member m2) {
                    return (int) (Integer.parseInt(Database.getAssistsParses(guild.getId(), m2.getId())[0]) - Integer.parseInt(Database.getAssistsParses(guild.getId(), m1.getId())[0]));
                }
            });
            for (Member member : roleMembers) {
                String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                String[] memberQuotaNums = Database.getRunsAssists(guild.getId(), member.getId());
                securityString += String.format("%-12s [Runs]: %-3s [Assists]: %s", memberName, memberQuotaNums[0], memberQuotaNums[1]) + "\n";
            }
            return new MessageEmbed.Field((role.getName().endsWith("y") ? role.getName().replace("y", "ies") : role.getName() + "s"), securityString + "\n```", false);
        }
        return null;
    }

    public static EmbedBuilder fullQuotaEmbed(Guild guild) {
        EmbedBuilder quotaEmbed = new EmbedBuilder();
        quotaEmbed.setColor(Goldilocks.BLUE);
        quotaEmbed.setTitle(guild.getName() + "'s Points");
        String topPointsString = "```ini\n";
        HashMap<String, Integer> topPointsMap = Database.getTopPoints(guild.getId());
        List<String> topPoints = topPointsMap.entrySet().stream().sorted((e1, e2) -> e2.getValue() - e1.getValue()).map(e -> e.getKey()).collect(Collectors.toList());
        if (topPointsMap != null) {
            // Assists leaderboard
            try {
                for (int i = 0; i < 3; i++) {
                    String memberName = guild.getMemberById(topPoints.get(i)).getEffectiveName().split(" ")[0].replaceAll("[^A-Za-z]","");
                    String memberAssists  = "" + String.format("%-12s [Total Points]: ",memberName);
                    switch (i) {
                        case 0:
                            topPointsString += "\n🥇 First Place:   "  + memberAssists + topPointsMap.get(topPoints.get(0));
                            break;
                        case 1:
                            topPointsString += "\n🥈 Second Place:  "  + memberAssists + topPointsMap.get(topPoints.get(1));
                            break;
                        case 2:
                            topPointsString += "\n🥉 Third Place:   "  + memberAssists + topPointsMap.get(topPoints.get(2));
                            break;
                    }
                }
                quotaEmbed.addField("**Top Points**", topPointsString + "\n```", false);
            } catch (Exception ignored) { }
        }

        // Rl Fields
//        MessageEmbed.Field hrlField = quotaAssistField(guild, "headRlRole", "adminRole");
//        MessageEmbed.Field vrlField = quotaAssistField(guild, "vetRlRole", "headRlRole");
//        MessageEmbed.Field rlField = quotaAssistField(guild, "rlRole", "vetRlRole");
//        MessageEmbed.Field arlField = quotaAssistField(guild, "arlRole", "rlRole");
//
//        if (hrlField != null) quotaEmbed.addField(hrlField);
//        if (vrlField != null) quotaEmbed.addField(vrlField);
//        if (rlField != null) quotaEmbed.addField(rlField);
//        if (arlField != null) quotaEmbed.addField(arlField);

        quotaEmbed.addField("Key", "```\n[ Parses | Veris | Alts | NameChanges | ModMails ]\n```", false);

        // Moderation Fields
        List<MessageEmbed.Field> modFields = quotaFullField(guild, "modRole", "adminRole");
        List<MessageEmbed.Field> officerFields = quotaFullField(guild, "officerRole", "modRole");
        List<MessageEmbed.Field> securityFields = quotaFullField(guild, "securityRole", "officerRole");
        List<MessageEmbed.Field> helperFields = quotaFullField(guild, "tSecRole", "securityRole");

        if (modFields != null) modFields.forEach(quotaEmbed::addField);
        if (officerFields != null) officerFields.forEach(quotaEmbed::addField);
        if (securityFields != null) securityFields.forEach(quotaEmbed::addField);
        if (helperFields != null) helperFields.forEach(quotaEmbed::addField);

        quotaEmbed.setImage(Charts.createModerationChart(guild));
        quotaEmbed.setFooter(guild.getName() + "'s Total Points");
        quotaEmbed.setTimestamp(new Date().toInstant());
        return quotaEmbed;
    }

    public static List<MessageEmbed.Field> quotaFullField(Guild guild, String roleName, String higherRole) {
        List<MessageEmbed.Field> fields = new ArrayList<>();
        String currentField = "";
        if (!SetupConnector.getFieldValue(guild, "guildInfo",roleName).equals("0")) {

            String higherRoleId = SetupConnector.getFieldValue(guild, "guildInfo", higherRole);
            String roleId = SetupConnector.getFieldValue(guild, "guildInfo", roleName);
            if (roleId.isEmpty() || higherRoleId.isEmpty()) return null;

            Role higher = guild.getRoleById(higherRoleId);
            Role role = guild.getRoleById(roleId);
            List<Member> roleMembers = guild.getMembersWithRoles(role).stream().filter(m -> !m.getRoles().contains(higher) && !m.getPermissions().contains(Permission.ADMINISTRATOR)).collect(Collectors.toList());
            if (roleMembers.isEmpty()) return null;
            roleMembers.sort(new Comparator<Member>() {
                @Override
                public int compare(Member m1, Member m2) {
                    return (int) (Database.getAllQuota(guild.getId(), m2.getId())[0] - Database.getAllQuota(guild.getId(), m1.getId())[0]);
                }
            });
            for (Member member : roleMembers) {
                String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                int[] memberQuotaNums = Database.getAllQuota(guild.getId(), member.getId());
                String modString = String.format("%-12s [Total Points]: %-3s\n;\t[P]: %-2s [V]: %-2s [A]: %-2s [N]: %-2s [M]: %-2s",
                       memberName, memberQuotaNums[0] + "", memberQuotaNums[3] + "", memberQuotaNums[4] + "", memberQuotaNums[6] + "" ,memberQuotaNums[5] + "", memberQuotaNums[7] + "") + "\n";
                if (currentField.length() + modString.length() < 1000) {
                    currentField += modString;
                } else {
                    fields.add(new MessageEmbed.Field(fields.isEmpty() ? (role.getName().endsWith("y") ? role.getName().replace("y", "ies") : role.getName() + "s") : " ", "```apache\n" + currentField + "\n```", false));
                    currentField = modString;
                }
            }
            if (!currentField.isEmpty()) fields.add(new MessageEmbed.Field(fields.isEmpty() ? (role.getName().endsWith("y") ? role.getName().replace("y", "ies") : role.getName() + "s") : " ", "```ini\n" + currentField + "\n```", false));
            return fields;
        }
        return fields;
    }

    public static EmbedBuilder quotaEmbed(Guild guild) {
        EmbedBuilder quotaEmbed = new EmbedBuilder();
        quotaEmbed.setColor(Goldilocks.GOLD);
        quotaEmbed.setTitle(guild.getName() + "'s Staff Quotas");
        String topRunsString = "```yaml\n";
        String topAssistsString = "```yaml\n";
        String[][] topRuns = Database.getTopQuota("quotaRuns", guild.getId());
        String[][] topAssists = Database.getTopQuota("quotaAssists", guild.getId());
        if (topRuns != null) {
            //Top Runs leaderboard
            try {
                for (int i = 0; i < 3; i++) {
                    String memberName = guild.getMemberById(topRuns[i][0]).getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                    String memberRuns  = "" + String.format("%-12s Runs: ",memberName);
                    switch (i) {
                        case 0:
                            topRunsString += "\n🥇 First Place:   "  + memberRuns + topRuns[0][1];
                            break;
                        case 1:
                            topRunsString += "\n🥈 Second Place:  "  + memberRuns + topRuns[1][1];
                            break;
                        case 2:
                            topRunsString += "\n🥉 Third Place:   "  + memberRuns + topRuns[2][1];
                            break;
                    }
                }
                quotaEmbed.addField("**Top Runs**", topRunsString + "\n```", false);
            } catch (Exception e) {}

            //Assists leaderboard
            try {
                for (int i = 0; i < 3; i++) {
                    String memberName = guild.getMemberById(topAssists[i][0]).getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                    String memberAssists  = "" + String.format("%-12s Assists: ",memberName);
                    switch (i) {
                        case 0:
                            topAssistsString += "\n🥇 First Place:   "  + memberAssists + topAssists[0][1];
                            break;
                        case 1:
                            topAssistsString += "\n🥈 Second Place:  "  + memberAssists + topAssists[1][1];
                            break;
                        case 2:
                            topAssistsString += "\n🥉 Third Place:   "  + memberAssists + topAssists[2][1];
                            break;
                    }
                }
                quotaEmbed.addField("**Top Assists**", topAssistsString + "\n```", false);
            } catch (Exception e) {}
        }

        //Vet RL Field
        if (!SetupConnector.getFieldValue(guild, "guildInfo","vetRlRole").equals("0")) {
            Role hrlRole = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","headRlRole"));
            Role vetRlrole = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","vetRlRole"));
            String vetRlString = "```apache\n";
            List<Member> vetRaidLeaders = guild.getMembersWithRoles(vetRlrole);
            vetRaidLeaders.sort(new Comparator<Member>() {
                @Override
                public int compare(Member m1, Member m2) {
                    return (int) (Integer.parseInt(Database.getRunsAssists(guild.getId(), m2.getId())[0]) - Integer.parseInt(Database.getRunsAssists(guild.getId(), m1.getId())[0]));
                }
            });
            for (Member member : vetRaidLeaders) {
                //!member.getRoles().contains(hrlRole) &&
                if (!member.getPermissions().contains(Permission.ADMINISTRATOR)) {
                    String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                    String[] memberQuotaNums = Database.getRunsAssists(guild.getId(), member.getId());
                    vetRlString += String.format("%-12s Runs: %-5s Assists: %s",memberName, memberQuotaNums[0], memberQuotaNums[1]) + "\n";
                }
            }
            quotaEmbed.addField(vetRlrole.getName() + "s", vetRlString + "\n```", false);
        }

        //Raid Leader field
        if (!SetupConnector.getFieldValue(guild, "guildInfo","rlRole").equals("0")) {
            boolean cont = false;
            Role vetRlRole = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","vetRlRole"));
            Role raidLeaderRole = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","rlRole"));
            String raidLeaderString = "```apache\n";
            List<Member> raidLeaders = guild.getMembersWithRoles(raidLeaderRole);
            raidLeaders.sort(new Comparator<Member>() {
                @Override
                public int compare(Member m1, Member m2) {
                    return (int) (Integer.parseInt(Database.getRunsAssists(guild.getId(), m2.getId())[0]) - Integer.parseInt(Database.getRunsAssists(guild.getId(), m1.getId())[0]));
                }
            });
            for (Member member : raidLeaders) {
                if (!member.getRoles().contains(vetRlRole) && !member.getPermissions().contains(Permission.ADMINISTRATOR)) {
                    String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                    String[] memberQuotaNums = Database.getRunsAssists(guild.getId(), member.getId());
                    if (raidLeaderString.length() < 980) {
                        raidLeaderString += String.format("%-12s Runs: %-5s Assists: %s",memberName, memberQuotaNums[0], memberQuotaNums[1]) + "\n";
                    } else {
                        cont = true;
                        quotaEmbed.addField(raidLeaderRole.getName() + "s", raidLeaderString + "\n```", false);
                        raidLeaderString = "```apache\n";
                        raidLeaderString += String.format("%-12s Runs: %-5s Assists: %s",memberName, memberQuotaNums[0], memberQuotaNums[1]) + "\n";
                    }
                }
            }
            quotaEmbed.addField(raidLeaderRole.getName() + "s" + (cont ? " (Continued)" : ""), raidLeaderString + "\n```", false);
        }
        //Almost Leader field
        if (!SetupConnector.getFieldValue(guild, "guildInfo","arlRole").equals("0")) {
            Role raidLeaderRole = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","rlRole"));
            Role arlRole = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","arlRole"));
            String arlString = "```apache\n";
            List<Member> almostRaidLeaders = guild.getMembersWithRoles(arlRole);
            almostRaidLeaders.sort(new Comparator<Member>() {
                @Override
                public int compare(Member m1, Member m2) {
                    return (int) (Integer.parseInt(Database.getRunsAssists(guild.getId(), m2.getId())[0]) - Integer.parseInt(Database.getRunsAssists(guild.getId(), m1.getId())[0]));
                }
            });
            for (Member member : almostRaidLeaders) {
                if (!member.getRoles().contains(raidLeaderRole) && !member.getPermissions().contains(Permission.ADMINISTRATOR)) {
                    String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                    String[] memberQuotaNums = Database.getRunsAssists(guild.getId(), member.getId());
                    arlString += String.format("%-12s Runs: %-5s Assists: %s",memberName, memberQuotaNums[0], memberQuotaNums[1]) + "\n";
                }
            }
            quotaEmbed.addField(arlRole.getName() + "s", arlString + "\n```", false);
        }
        //Moderator Field
        if (!SetupConnector.getFieldValue(guild, "guildInfo","modRole").equals("0")) {
            Role moderator = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","modRole"));
            String moderatorString = "```apache\n";
            for (Member member : guild.getMembersWithRoles(moderator)) {
                if (!member.getPermissions().contains(Permission.ADMINISTRATOR)) {
                    String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                    String[] memberQuotaNums = Database.getAssistsParses(guild.getId(), member.getId());
                    moderatorString += String.format("%-12s Parses: %-3s Assists: %s",memberName, memberQuotaNums[1], memberQuotaNums[0]) + "\n";
                }
            }
            //quotaEmbed.addField(moderator.getName() + "s", moderatorString + "\n```", false);
        }
        //Officer Field
        if (!SetupConnector.getFieldValue(guild, "guildInfo","officerRole").equals("0")) {
            Role moderator = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","modRole"));
            Role officer = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","officerRole"));
            String officerString = "```apache\n";
            List<Member> officers = guild.getMembersWithRoles(officer);
            officers.sort(new Comparator<Member>() {
                @Override
                public int compare(Member m1, Member m2) {
                    return (int) (Integer.parseInt(Database.getAssistsParses(guild.getId(), m2.getId())[0]) - Integer.parseInt(Database.getAssistsParses(guild.getId(), m1.getId())[0]));
                }
            });
            for (Member member : officers) {
                if (!member.getRoles().contains(moderator) && !member.getPermissions().contains(Permission.ADMINISTRATOR)) {
                    String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                    String[] memberQuotaNums = Database.getAssistsParses(guild.getId(), member.getId());
                    officerString += String.format("%-12s Parses: %-3s Assists: %s",memberName, memberQuotaNums[1], memberQuotaNums[0]) + "\n";
                }
            }
            quotaEmbed.addField(officer.getName() + "s", officerString + "\n```", false);
        }

        //Security Field
        if (!SetupConnector.getFieldValue(guild, "guildInfo","securityRole").equals("0")) {
            Role officer = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","officerRole"));
            Role security = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","securityRole"));
            String securityString = "```apache\n";
            List<Member> securities = guild.getMembersWithRoles(security);
            securities.sort(new Comparator<Member>() {
                @Override
                public int compare(Member m1, Member m2) {
                    return (int) (Integer.parseInt(Database.getAssistsParses(guild.getId(), m2.getId())[0]) - Integer.parseInt(Database.getAssistsParses(guild.getId(), m1.getId())[0]));
                }
            });
            for (Member member : securities) {
                if (!member.getRoles().contains(officer) && !member.getPermissions().contains(Permission.ADMINISTRATOR)) {
                    String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                    String[] memberQuotaNums = Database.getAssistsParses(guild.getId(), member.getId());
                    securityString += String.format("%-12s Parses: %-3s Assists: %s",memberName, memberQuotaNums[1], memberQuotaNums[0]) + "\n";
                }
            }
            quotaEmbed.addField((security.getName().endsWith("y") ? security.getName().replace("y", "ies") : security.getName() + "s"), securityString + "\n```", false);
        }
        //Trial Security Field
        if (!SetupConnector.getFieldValue(guild, "guildInfo","tSecRole").equals("0")) {
            Role trialSecurity = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","tSecRole"));
            String trialSecurityString = "```apache\n";
            for (Member member : guild.getMembersWithRoles(trialSecurity)) {
                String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                String[] memberQuotaNums = Database.getAssistsParses(guild.getId(), member.getId());
                trialSecurityString += String.format("%-12s Parses: %-3s Assists: %s",memberName, memberQuotaNums[1], memberQuotaNums[0]) + "\n";
            }
            quotaEmbed.addField(trialSecurity.getName() + "s", trialSecurityString + "\n```", false);
        }
        //Event Organizer Field
        if (!SetupConnector.getFieldValue(guild, "guildInfo","eoRole").equals("0")) {
            Role eventOrganizer = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","eoRole"));
            Role headEventOrganizer = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","headEoRole"));
            String trialSecurityString = "```apache\n";
            List<Member> eventOrganizers = guild.getMembersWithRoles(eventOrganizer);
            eventOrganizers.sort(new Comparator<Member>() {
                @Override
                public int compare(Member m1, Member m2) {
                    return (int) (Integer.parseInt(Database.getEventAssists(guild.getId(), m2.getId())[1]) - Integer.parseInt(Database.getEventAssists(guild.getId(), m1.getId())[1]));
                }
            });
            for (Member member : eventOrganizers) {
                if (!member.getPermissions().contains(Permission.ADMINISTRATOR) && !member.getRoles().contains(headEventOrganizer)) {
                    String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                    String[] memberQuotaNums = Database.getEventAssists(guild.getId(), member.getId());
                    Long testTime = Long.parseLong(memberQuotaNums[1]);
                    String timeString = (testTime > 86399 ? testTime / 86399 + "d " : "");
                    testTime = testTime - (86400 * (testTime / 86400));
                    timeString += (testTime > 3559 ? testTime / 3600 + "h " : "");
                    testTime = testTime - (3600 * (testTime / 3600));
                    timeString += testTime / 60 + "m";
                    trialSecurityString += String.format("%-12s Event Time: %s",memberName, timeString) + "\n";
                }
            }
            quotaEmbed.addField(eventOrganizer.getName() + "s", trialSecurityString + "\n```", false);
        }

        quotaEmbed.setImage(Charts.createQuotaChart(guild));
        quotaEmbed.setFooter(guild.getName() + "'s Staff Quotas");
        quotaEmbed.setTimestamp(new Date().toInstant());

        return quotaEmbed;
    }

    public static EmbedBuilder shattersQuotaEmbed(Guild guild) {
        EmbedBuilder quotaEmbed = new EmbedBuilder();
        quotaEmbed.setColor(Goldilocks.GOLD);
        quotaEmbed.setTitle(guild.getName() + "'s Staff Quotas");
        String topRunsString = "```apache\n";
        String topAssistsString = "```apache\n";

        //Retrieve top quota from database (Sorted)
        //Retrieve top assists from the db (Sorted)

        LinkedHashMap <String, String[]> quotaRuns = SqlConnector.getStatsSortedField("currentweek", "currentWeekassists", ".5");
        Map.Entry<String, String[]>[] quotaRunsArr = quotaRuns.entrySet().toArray(new Map.Entry[0]);
        LinkedHashMap <String, String[]> quotaAssists = SqlConnector.getStatsSortedField("currentWeekassists", "currentweek", "0");
        Map.Entry<String, String[]>[] quotaAssistsArr = quotaAssists.entrySet().toArray(new Map.Entry[0]);

        //quotaEmbed.addField("Top Runs:", quotaRuns.entrySet().stream().map(e -> e.getKey() + " | Runs: " + e.getValue()[0]).collect(Collectors.joining("\n")).substring(0, 1000), false);

        try {
            if (quotaRuns.size() >= 3) {
                topRunsString += "\n🥇 1st:   " + String.format("%-12s Runs: %-3s Assists:" , guild.getMemberById(quotaRunsArr[0].getKey()).getEffectiveName()
                        .split("[.|]")[0].replaceAll("[^A-Za-z]", ""), quotaRunsArr[0].getValue()[0]) + quotaRunsArr[0].getValue()[1];
                topRunsString += "\n🥈 2nd:   " + String.format("%-12s Runs: %-3s Assists:" , guild.getMemberById(quotaRunsArr[1].getKey()).getEffectiveName()
                        .split("[.|]")[0].replaceAll("[^A-Za-z]", ""), quotaRunsArr[1].getValue()[0]) + quotaRunsArr[1].getValue()[1];
                topRunsString += "\n🥉 3rd:   " + String.format("%-12s Runs: %-3s Assists:" , guild.getMemberById(quotaRunsArr[2].getKey()).getEffectiveName()
                        .split("[.|]")[0].replaceAll("[^A-Za-z]", ""), quotaRunsArr[2].getValue()[0]) + quotaRunsArr[2].getValue()[1];
                quotaEmbed.addField("**Top Runs**", topRunsString + "\n```", false);
            }
            if (quotaAssists.size() >= 3) {
                topAssistsString += "\n🥇 1st:   " + String.format("%-12s Assists: ", guild.getMemberById(quotaAssistsArr[0].getKey()).getEffectiveName()
                        .split("[.|]")[0].replaceAll("[^A-Za-z]", "")) + quotaAssistsArr[0].getValue()[1];
                topAssistsString += "\n🥈 2nd:   " + String.format("%-12s Assists: ", guild.getMemberById(quotaAssistsArr[1].getKey()).getEffectiveName()
                        .split("[.|]")[0].replaceAll("[^A-Za-z]", "")) + quotaAssistsArr[1].getValue()[1];
                topAssistsString += "\n🥉 3rd:   " + String.format("%-12s Assists: ", guild.getMemberById(quotaAssistsArr[2].getKey()).getEffectiveName()
                        .split("[.|]")[0].replaceAll("[^A-Za-z]", "")) + quotaAssistsArr[2].getValue()[1];
                quotaEmbed.addField("**Top Assists**", topAssistsString + "\n```", false);
            }
        } catch (Exception ignored) { }

        //Raid leaders
        String headRaidLeaderId = SetupConnector.getFieldValue(guild, "guildInfo","headRlRole");
        String vetRaidLeaderId = SetupConnector.getFieldValue(guild, "guildInfo","vetRlRole");
        String raidLeaderId = SetupConnector.getFieldValue(guild, "guildInfo","rlRole");
        String almostRaidLeaderId = SetupConnector.getFieldValue(guild, "guildInfo","arlRole");

        Role headRl = (headRaidLeaderId.equals("0") ? null : guild.getRoleById(headRaidLeaderId));
        Role vetRl = (vetRaidLeaderId.equals("0") ? null : guild.getRoleById(vetRaidLeaderId));
        Role rl = (raidLeaderId.equals("0") ? null : guild.getRoleById(raidLeaderId));
        Role arl = (almostRaidLeaderId.equals("0") ? null : guild.getRoleById(almostRaidLeaderId));

        //Mod Team
        String modRoleId = SetupConnector.getFieldValue(guild, "guildInfo","modRole");
        String officerRoleId = SetupConnector.getFieldValue(guild, "guildInfo","officerRole");
        String securityRoleId = SetupConnector.getFieldValue(guild, "guildInfo","securityRole");

        Role modRole = (modRoleId.equals("0") ? null : guild.getRoleById(modRoleId));
        Role officerRole = (officerRoleId.equals("0") ? null : guild.getRoleById(officerRoleId));
        Role securityRole = (securityRoleId.equals("0") ? null : guild.getRoleById(securityRoleId));

        //Hrl Role
        if (headRl != null) {
            List<String> hrlStrings = getRoleString(guild, headRl, null, quotaRuns);
            for (int i = 0; i < hrlStrings.size(); i++) quotaEmbed.addField((i == 0 ? headRl.getName() + "s" : " "), hrlStrings.get(i) , false);
        }

        //Vrl Role
        if (vetRl != null) {
            List<String> vrlStrings = getRoleString(guild, vetRl, headRl, quotaRuns);
            for (int i = 0; i < vrlStrings.size(); i++) quotaEmbed.addField((i == 0 ? vetRl.getName() + "s" : " "), vrlStrings.get(i) , false);
        }

        //Raid leader
        if (rl != null) {
            List<String> rlStrings = getRoleString(guild, rl, vetRl, quotaRuns);
            for (int i = 0; i < rlStrings.size(); i++) quotaEmbed.addField((i == 0 ? rl.getName() + "s" : " "), rlStrings.get(i) , false);
        }

        //Almost Raid leader
        if (arl != null) {
            List<String> arlStrings = getRoleString(guild, arl, rl, quotaRuns);
            for (int i = 0; i < arlStrings.size(); i++) quotaEmbed.addField((i == 0 ? arl.getName() + "s" : " "), arlStrings.get(i) , false);
        }

        //Officer
        if (officerRole != null) {
            List<String> officerStrings = getRoleString(guild, officerRole, modRole, quotaAssists);
            for (int i = 0; i < officerStrings.size(); i++) quotaEmbed.addField((i == 0 ? officerRole.getName() + "s" : " "), officerStrings.get(i) , false);
        }

        if (securityRole != null) {
            List<String> securityStrings = getRoleString(guild, securityRole, officerRole, quotaAssists);
            for (int i = 0; i < securityStrings.size(); i++) quotaEmbed.addField((i == 0 ? (securityRole.getName().endsWith("y") ? securityRole.getName().replace("y", "ies") : securityRole.getName() + "s") : " "), securityStrings.get(i) , false);
        }

        List<List<LogField>> fields = Arrays.asList(DataCollector.getData("451171819672698920", "currentweek", System.currentTimeMillis() - 604800000),
                DataCollector.getData("451171819672698920", "currentweekassists", System.currentTimeMillis() - 604800000));

        quotaEmbed.setImage(Charts.createDualChart(fields));
        quotaEmbed.setFooter(guild.getName() + "'s Staff Quotas");
        quotaEmbed.setTimestamp(new Date().toInstant());

        return quotaEmbed;
    }

    public static List<String> getRoleString(Guild guild, Role role, Role higherRole, LinkedHashMap <String, String[]> quotaRuns) {
        List<String> roleStrings = new ArrayList<>();
        String roleString = "```apache\n";
        List<String> hrls = guild.getMembersWithRoles(role).stream().filter(member -> higherRole != null ? !member.getRoles().contains(higherRole) : true).map(member -> member.getId()).collect(Collectors.toList());
        List<Map.Entry<String, String[]>> sortedHrls = quotaRuns.entrySet().stream().filter(e -> hrls.contains(e.getKey())).collect(Collectors.toList());
        for (Map.Entry<String, String[]> s : sortedHrls) {
            if (roleString.length() > 950) {
                roleString += "\n```";
                roleStrings.add(roleString);
                roleString = "```apache\n";
            }
            if (!guild.getMemberById(s.getKey()).getEffectiveName()
                    .split("[.|]")[0].replaceAll("[^A-Za-z]", "").isEmpty()) {
                roleString += String.format("%-12s Runs: %2$-4s Fails: %3$-4s Assists: %4$-4s", guild.getMemberById(s.getKey()).getEffectiveName()
                        .split("[.|]")[0].replaceAll("[^A-Za-z]", "") ,s.getValue()[0], s.getValue()[2], s.getValue()[1]) + "\n";
            }
        }
        if (roleString.split("\n").length == 1) roleString += "None";
        roleString += "\n```";
        roleStrings.add(roleString);
        return roleStrings;
    }

    public static EmbedBuilder quotaResetEmbed(Guild guild) {
        EmbedBuilder quotaResetEmbed = new EmbedBuilder();
        quotaResetEmbed.setColor(Goldilocks.GOLD);
        quotaResetEmbed.setTitle(guild.getName() + "'s Staff Quotas");
        String topRunsString = "```yaml\n";
        String topAssistsString = "```yaml\n";
        String[][] topRuns = Database.getTopQuota("quotaRuns", guild.getId());
        String[][] topAssists = Database.getTopQuota("quotaAssists", guild.getId());
        if (topRuns != null) {
            //Top Runs leaderboard
            for (int i = 0; i < 3; i++) {
                String memberName = guild.getMemberById(topRuns[i][0]).getEffectiveName().replaceAll("[^A-Za-z]","");
                String memberRuns  = "" + String.format("%-12s Runs: ",memberName);
                switch (i) {
                    case 0:
                        topRunsString += "\n🥇 First Place:   "  + memberRuns + topRuns[0][1];
                        break;
                    case 1:
                        topRunsString += "\n🥈 Second Place:  "  + memberRuns + topRuns[1][1];
                        break;
                    case 2:
                        topRunsString += "\n🥉 Third Place:   "  + memberRuns + topRuns[2][1];
                        break;
                }
            }
            quotaResetEmbed.addField("**Top Runs**", topRunsString + "\n```", false);

            //Assists leaderboard
            for (int i = 0; i < 3; i++) {
                String memberName = guild.getMemberById(topAssists[i][0]).getEffectiveName().replaceAll("[^A-Za-z]","");
                String memberAssists  = "" + String.format("%-12s Assists: ",memberName);
                switch (i) {
                    case 0:
                        topAssistsString += "\n🥇 First Place:   "  + memberAssists + topAssists[0][1];
                        break;
                    case 1:
                        topAssistsString += "\n🥈 Second Place:  "  + memberAssists + topAssists[1][1];
                        break;
                    case 2:
                        topAssistsString += "\n🥉 Third Place:   "  + memberAssists + topAssists[2][1];
                        break;
                }
            }
            quotaResetEmbed.addField("**Top Assists**", topAssistsString + "\n```", false);

            //Vet RL Field
            if (!SetupConnector.getFieldValue(guild, "guildInfo","vetRlRole").equals("0")) {
                Role hrlRole = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","headRlRole"));
                Role vetRlrole = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","vetRlRole"));
                String vetRlString = "```css\n";
                for (Member member : guild.getMembersWithRoles(vetRlrole)) {
                    if (!member.getRoles().contains(hrlRole) && !member.getPermissions().contains(Permission.ADMINISTRATOR)) {
                        String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                        String[] memberQuotaNums = Database.getRunsAssists(guild.getId(), member.getId());
                        if (Integer.parseInt(memberQuotaNums[0]) < 7) {
                            vetRlString += String.format("%-12s Runs: %-3s Assists: %-3s",memberName, memberQuotaNums[0], memberQuotaNums[1]) + "\n";
                        }
                    }
                }
                quotaResetEmbed.addField(vetRlrole.getName() + "s", vetRlString + "\n```", false);
            }

        }

        //Raid Leader field
        if (!SetupConnector.getFieldValue(guild, "guildInfo","rlRole").equals("0")) {
            Role vetRlRole = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","vetRlRole"));
            Role raidLeaderRole = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","rlRole"));
            String raidLeaderString = "```css\n";
            for (Member member : guild.getMembersWithRoles(raidLeaderRole)) {
                if (!member.getRoles().contains(vetRlRole) && !member.getPermissions().contains(Permission.ADMINISTRATOR)) {
                    String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                    String[] memberQuotaNums = Database.getRunsAssists(guild.getId(), member.getId());
                    if (Integer.parseInt(memberQuotaNums[0]) < 7) {
                        raidLeaderString += String.format("%-17s Total Runs: %-3s Assists: %-3s",memberName, memberQuotaNums[0], memberQuotaNums[1]) + "\n";
                    }
                }
            }
            String currentString = "";
            for (String s : raidLeaderString.split("\n")) {
                if (currentString.length() + s.length() < 1000) currentString += s;
                else quotaResetEmbed.addField(raidLeaderRole.getName() + "s", currentString + "\n```", false);
            }
            if (!currentString.isEmpty()) quotaResetEmbed.addField(raidLeaderRole.getName() + "s", currentString + "\n```", false);

        }
        //Almost Leader field
        if (!SetupConnector.getFieldValue(guild, "guildInfo","arlRole").equals("0")) {
            Role raidLeaderRole = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","rlRole"));
            Role arlRole = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","arlRole"));
            String arlString = "```css\n";
            for (Member member : guild.getMembersWithRoles(arlRole)) {
                if (!member.getRoles().contains(raidLeaderRole) && !member.getPermissions().contains(Permission.ADMINISTRATOR)) {
                    String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                    String[] memberQuotaNums = Database.getRunsAssists(guild.getId(), member.getId());
                    if (Integer.parseInt(memberQuotaNums[0]) < 7) {
                        arlString += String.format("%-17s Total Runs: %-3s Assists: %-3s",memberName, memberQuotaNums[0], memberQuotaNums[1]) + "\n";
                    }
                }
            }
            quotaResetEmbed.addField(arlRole.getName() + "s", arlString + "\n```", false);
        }

        //Officer Field
        if (!SetupConnector.getFieldValue(guild, "guildInfo","officerRole").equals("0")) {
            Role moderator = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","modRole"));
            Role officer = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","officerRole"));
            String officerString = "```css\n";
            for (Member member : guild.getMembersWithRoles(officer)) {
                if (!member.getRoles().contains(moderator) && !member.getPermissions().contains(Permission.ADMINISTRATOR)) {
                    String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                    String[] memberQuotaNums = Database.getAssistsParses(guild.getId(), member.getId());
                    if (Integer.parseInt(memberQuotaNums[0]) < 6) {
                        officerString += String.format("%-15s Total Parses: %-3s Assists: %-3s",memberName, memberQuotaNums[1], memberQuotaNums[0]) + "\n";
                    }
                }
            }
            quotaResetEmbed.addField(officer.getName() + "s", officerString + "\n```", false);
        }

        //Security Field
        if (!SetupConnector.getFieldValue(guild, "guildInfo","securityRole").equals("0")) {
            Role officer = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","officerRole"));
            Role security = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","securityRole"));
            String securityString = "```css\n";
            for (Member member : guild.getMembersWithRoles(security)) {
                if (!member.getRoles().contains(officer) && !member.getPermissions().contains(Permission.ADMINISTRATOR)) {
                    String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                    String[] memberQuotaNums = Database.getAssistsParses(guild.getId(), member.getId());
                    if (Integer.parseInt(memberQuotaNums[0]) < 6) {
                        securityString += String.format("%-15s Total Parses: %-3s Assists: %-3s",memberName, memberQuotaNums[1], memberQuotaNums[0]) + "\n";
                    }
                }
            }
            quotaResetEmbed.addField(security.getName() + "s", securityString + "\n```", false);
        }
        //Event Organizer Field
        if (!SetupConnector.getFieldValue(guild, "guildInfo","eoRole").equals("0")) {
            Role eventOrganizer = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","eoRole"));
            Role headEventOrganizer = guild.getRoleById(SetupConnector.getFieldValue(guild, "guildInfo","headEoRole"));
            String trialSecurityString = "```css\n";
            for (Member member : guild.getMembersWithRoles(eventOrganizer)) {
                if (!member.getPermissions().contains(Permission.ADMINISTRATOR) && !member.getRoles().contains(headEventOrganizer)) {
                    String memberName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                    String[] memberQuotaNums = Database.getEventAssists(guild.getId(), member.getId());
                    Long testTime = Long.parseLong(memberQuotaNums[1]);
                    String timeString = (testTime > 86399 ? testTime / 86399 + "d " : "");
                    testTime = testTime - (86400 * (testTime / 86400));
                    timeString += (testTime > 3559 ? testTime / 3600 + "h " : "");
                    testTime = testTime - (3600 * (testTime / 3600));
                    timeString += testTime / 60 + "m";
                    trialSecurityString += String.format("%-12s Event Time: %s",memberName, timeString) + "\n";
                }
            }
            quotaResetEmbed.addField(eventOrganizer.getName() + "s", trialSecurityString + "\n```", false);
        }

        quotaResetEmbed.setFooter(guild.getName() + "'s Staff Quotas");
        quotaResetEmbed.setTimestamp(new Date().toInstant());

        return quotaResetEmbed;
    }

}
