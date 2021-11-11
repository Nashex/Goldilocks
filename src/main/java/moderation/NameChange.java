package moderation;

import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import setup.SetupConnector;
import verification.CompactPlayerProfile;
import verification.PlayerProfile;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NameChange {

    public static void modNameChange(Member mod, Member member, String name, Message message) {
        TextChannel textChannel = message.getTextChannel();
        String nickName = member.getEffectiveName();
        String oldName = nickName.split(" ")[0].replaceAll("[^A-Za-z]", "");

        if (message.getAttachments().isEmpty()) {
            try {
                CompactPlayerProfile cp = new CompactPlayerProfile(name);

                if (name.equals(oldName)) {
                    if (textChannel != null) textChannel.sendMessage("Unable to change name as it is the same as their current name.").queue();
                    return;
                }

                List<Map.Entry> currentName = cp.nameHistory.entrySet().stream().filter(e -> e.getKey().toLowerCase().equals(name.toLowerCase()) && !e.getValue().contains("-")).collect(Collectors.toList());
                if (!currentName.isEmpty() && cp.nameHistory.entrySet().stream().anyMatch(e -> e.getKey().toLowerCase().equals(oldName.toLowerCase()))) {
                    String newNickName = nickName.replaceAll("(?i)" + oldName, (String) currentName.get(0).getKey());
                    String newName = (String) currentName.get(0).getKey();
                    if (member.getUser().getName().equals(newNickName)) newNickName = (java.lang.Character.isUpperCase(newNickName.charAt(0)) ? newNickName.substring(0, 1).toLowerCase() : newNickName.substring(0,1).toUpperCase()) + newNickName.substring(1);
                    if (!currentName.isEmpty()) {
                        String finalNewNickName = newNickName;
                        try {
                            member.modifyNickname(newNickName).complete();
                        } catch (Exception e) { }
                        Database.incrementField(mod, "quotaNameChanges", "totalNameChanges");
                        Database.logEvent(mod, Database.EventType.NAMECHANGE, System.currentTimeMillis() / 1000, message.getTextChannel(), message.getContentRaw());
                        textChannel.sendMessage("Successfully changed name from `" + nickName + "` to `" + finalNewNickName + "`!").queue();
                        TextChannel logChannel;
                        String logChannelId = SetupConnector.getFieldValue(member.getGuild(), "guildLogs","modLogChannelId");
                        if (logChannelId.equals("0")) logChannel = null;
                        try {
                            logChannel = Goldilocks.jda.getTextChannelById(logChannelId);
                        } catch (Exception e) {logChannel = null;}
                        if (logChannel != null) {
                            try {
                                logChannel.sendMessage(logMessage(mod, member, oldName, newName, nickName, finalNewNickName, message.getAttachments().isEmpty() ? "" : message.getAttachments().get(0).getProxyUrl()).build()).complete();
                            } catch (Exception e) {}
                        }
                        return;
                    }
                } else {
                    if (message.getAttachments().isEmpty()) {
                        textChannel.sendMessage("Unable to find name in name history, please re-use the command with proof of the name change.").queue();
                        return;
                    }

                }
            } catch (PlayerProfile.PrivateProfileException e) {
                if (message.getAttachments().isEmpty()) {
                    textChannel.sendMessage("Unable to find name in name history, please re-use the command with proof of the name change.").queue();
                    return;
                }
            }
        }

        if (!message.getAttachments().isEmpty()) {
            String newNickName = nickName.replaceAll("(?i)" + oldName, name);
            if (member.getUser().getName().equals(newNickName)) newNickName = (java.lang.Character.isUpperCase(newNickName.charAt(0)) ? newNickName.substring(0, 1).toLowerCase() : newNickName.substring(0,1).toUpperCase()) + newNickName.substring(1);
            String finalNewNickName = newNickName;

            try {
                member.modifyNickname(newNickName).complete();
            } catch (Exception ignored) { }

            textChannel.sendMessage("Successfully changed name from `" + nickName + "` to `" + finalNewNickName + "`").queue();
            TextChannel logChannel;
            String logChannelId = SetupConnector.getFieldValue(member.getGuild(), "guildLogs","modLogChannelId");

            if (logChannelId.equals("0")) logChannel = null;
            else logChannel = Goldilocks.jda.getTextChannelById(logChannelId);
            if (logChannel != null) logChannel.sendMessage(logMessage(mod, member, oldName, name, nickName, finalNewNickName, message.getAttachments().get(0).getProxyUrl()).build()).complete();
        }
    }

    public static void modAddAlt(Member mod, Member member, String altName, Message message) {
        TextChannel textChannel = message.getTextChannel();
        String curName = member.getEffectiveName();
        String newName = curName + " | " + altName;

        if (curName.equals(newName)) {
            textChannel.sendMessage("Unable to add alt as the resulting name is the same as their current name.").queue();
            return;
        }

        try {
            member.modifyNickname(newName).complete();
        } catch (Exception e) {
            textChannel.sendMessage("Unable to add alt due to the following error: `" + e.getLocalizedMessage() + "`").queue();
            return;
        }
        Database.incrementField(mod, "quotaAlts", "totalAlts");
        Database.logEvent(mod, Database.EventType.ADDALT, System.currentTimeMillis() / 1000, message.getTextChannel(), message.getContentRaw());
        textChannel.sendMessage("Successfully added alt (`" + altName + "`) to " + member.getAsMention() + "!").queue();
        TextChannel logChannel;
        String logChannelId = SetupConnector.getFieldValue(member.getGuild(), "guildLogs","modLogChannelId");
        if (logChannelId.equals("0")) logChannel = null;
        logChannel = Goldilocks.jda.getTextChannelById(logChannelId);
        if (logChannel != null) {
            try {
                logChannel.sendMessage(logAltMessage(mod, member, altName, message.getAttachments().isEmpty() ? "" : message.getAttachments().get(0).getProxyUrl()).build()).complete();
            } catch (Exception ignored) {}
        }
    }

    private static EmbedBuilder logAltMessage(Member mod, Member member, String altName, String proxyUrl) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Alt Added for " + member.getEffectiveName())
                .setColor(Goldilocks.GREEN)
                .setTimestamp(new Date().toInstant())
                .addField("Moderator Information", (mod == null ? member.getGuild().getSelfMember().getAsMention() + " | `" + Goldilocks.jda.getSelfUser().getId() : mod.getAsMention() + " | `" + mod.getId()) + "`", true)
                .addField("Recipient Information", member.getAsMention() + " | `" + member.getId() + "`", true)
                .addField("Alt Name", "```\n" + altName + "\n```", false);
        if (!proxyUrl.isEmpty()) embedBuilder.setImage(proxyUrl);
        return embedBuilder;
    }

    public static void changeName(Member member, String name, TextChannel textChannel) {
        try {
            String nickName = member.getEffectiveName();
            String oldName = nickName.split(" ")[0].replaceAll("[^A-Za-z]", "");
            CompactPlayerProfile cp = new CompactPlayerProfile(name);

            if (name.equals(oldName)) {
                if (textChannel != null) textChannel.sendMessage("Unable to change your name as it is the same as your current name.").queue();
                return;
            }

            List<Map.Entry> currentName = cp.nameHistory.entrySet().stream().filter(e -> e.getKey().toLowerCase().equals(name.toLowerCase()) && !e.getValue().contains("-")).collect(Collectors.toList());
            if (!currentName.isEmpty() && cp.nameHistory.entrySet().stream().filter(e -> e.getKey().toLowerCase().equals(oldName.toLowerCase())).count() != 0) {
                String newNickName = nickName.replaceAll("(?i)" + oldName, (String) currentName.get(0).getKey());
                String newName = (String) currentName.get(0).getKey();
                if (member.getUser().getName().equals(newNickName)) newNickName = (java.lang.Character.isUpperCase(newNickName.charAt(0)) ? newNickName.substring(0, 1).toLowerCase() : newNickName.substring(0,1).toUpperCase()) + newNickName.substring(1);
                try {
                    if (!currentName.isEmpty()) {
                        String finalNewNickName = newNickName;
                        try {
                            member.modifyNickname(newNickName).complete();
                        } catch (Exception e) { }
                        textChannel.sendMessage("Successfully changed name from `" + nickName + "` to `" + finalNewNickName + "`!").queue();
                        TextChannel logChannel;
                        String logChannelId = SetupConnector.getFieldValue(member.getGuild(), "guildLogs","modLogChannelId");
                        if (logChannelId.equals("0")) logChannel = null;
                        try {
                            logChannel = Goldilocks.jda.getTextChannelById(logChannelId);
                        } catch (Exception e) {logChannel = null;}
                        if (logChannel != null) {
                            try {
                                logChannel.sendMessage(logMessage(member, oldName, newName, nickName, finalNewNickName).build()).complete();
                            } catch (Exception e) {}
                        }
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    textChannel.sendMessage("Unable to change name, please contact a security.").queue();
                }
            } else {
                textChannel.sendMessage("Unable to find name in name history, please contact a security.").queue();
            }
        } catch (PlayerProfile.PrivateProfileException e) {
            textChannel.sendMessage("Unable to change name, please un-private your profile or contact a security.").queue();
        }
    }

    public static String changeName(Member member, String name, CompactPlayerProfile cp) {
        try {
            String nickName = member.getEffectiveName();
            String oldName = nickName.split(" ")[0].replaceAll("[^A-Za-z]", "");

            if (name.equalsIgnoreCase(oldName)) {
                return "";
            }

            List<Map.Entry> currentName = cp.nameHistory.entrySet().stream().filter(e -> e.getKey().toLowerCase().equals(name.toLowerCase()) && !e.getValue().contains("-")).collect(Collectors.toList());
            if (!currentName.isEmpty() && cp.nameHistory.entrySet().stream().filter(e -> e.getKey().toLowerCase().equals(oldName.toLowerCase())).count() != 0) {
                String newNickName = nickName.replaceAll("(?i)" + oldName, (String) currentName.get(0).getKey());
                String newName = (String) currentName.get(0).getKey();
                if (member.getUser().getName().equals(newNickName)) newNickName = (java.lang.Character.isUpperCase(newNickName.charAt(0)) ? newNickName.substring(0, 1).toLowerCase() : newNickName.substring(0,1).toUpperCase()) + newNickName.substring(1);
                try {
                    if (!currentName.isEmpty()) {
                        String finalNewNickName = newNickName;
                        try {
                            member.modifyNickname(newNickName).complete();
                        } catch (Exception e) { }
                        TextChannel logChannel;
                        String logChannelId = SetupConnector.getFieldValue(member.getGuild(), "guildLogs","modLogChannelId");
                        if (logChannelId.equals("0")) logChannel = null;
                        try {
                            logChannel = Goldilocks.jda.getTextChannelById(logChannelId);
                        } catch (Exception e) {logChannel = null;}
                        if (logChannel != null) logChannel.sendMessage(logMessage(member, oldName, newName, nickName, finalNewNickName).build()).queue();
                        System.out.println("Name Change: " + nickName + " to " + finalNewNickName + " for " + member);
                        return nickName + " ⇒ " + finalNewNickName;
                    }
                } catch (Exception e) { }
            } else {
            }
        } catch (Exception e) { }
        return "";
    }

    public static String globalNameChange(User user, String name, CompactPlayerProfile cp) {
        String newName = "";
        List<Guild> guilds = user.getMutualGuilds().stream()
                .filter(g -> (SetupConnector.getFieldValue(g, "guildInfo","rank").equals("3") || Database.isPub(g))
                        && g.getSelfMember().hasPermission(Permission.NICKNAME_MANAGE)).collect(Collectors.toList());
        for (Guild guild : guilds) newName = changeName(guild.getMember(user), name, cp);
        return newName;
    }

    private static EmbedBuilder logMessage(Member member, String oldName, String newName, String oldNick, String newNick) {
        return logMessage(null, member, oldName, newName, oldNick, newNick);
    }

    private static EmbedBuilder logMessage(Member mod, Member member, String oldName, String newName, String oldNick, String newNick) {
        return logMessage(mod, member, oldName, newName, oldNick, newNick, null);
    }

    private static EmbedBuilder logMessage(Member mod, Member member, String oldName, String newName, String oldNick, String newNick, String imageURL) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Name Changed for " + member.getEffectiveName())
                .setColor(Goldilocks.GREEN)
                .setTimestamp(new Date().toInstant())
                .addField("Moderator Information", (mod == null ? member.getGuild().getSelfMember().getAsMention() + " | `" + Goldilocks.jda.getSelfUser().getId() : mod.getAsMention() + " | `" + mod.getId()) + "`", true)
                .addField("Recipient Information", member.getAsMention() + " | `" + member.getId() + "`", true)
                .addField("Name Change", "```\n" + oldNick + " ⇒ " + newNick + "\n```", false)
                .addField("Found in Name History", "```\n" + oldName + " ⇒ " + newName + "\n```\n" + (imageURL.isEmpty() ? "" : "**Evidence**"), false);
        if (!imageURL.isEmpty()) embedBuilder.setImage(imageURL);
        return embedBuilder;
    }

}
