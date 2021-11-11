package utils;

import net.dv8tion.jda.api.entities.*;

import java.util.List;

public class InputVerification {

    public static TextChannel getGuildTextChannel(Guild guild, String string) {
        TextChannel textChannel = null;
        List<TextChannel> textChannels = guild.getTextChannels();

        String content = string.replace(" ", "-");
        for (TextChannel textChannel1 : textChannels) {
            if (textChannel1.getName().equalsIgnoreCase(content)) {
                textChannel = textChannel1;
                break;
            }
            if (textChannel1.getId().equalsIgnoreCase(content)) {
                textChannel = textChannel1;
                break;
            }
        }

        return textChannel;
    }

    public static TextChannel getGuildTextChannel(Guild guild, Message message) {
        TextChannel textChannel = null;
        List<TextChannel> textChannels = guild.getTextChannels();

        if (message.getMentionedChannels().isEmpty()) {
            textChannel = getGuildTextChannel(guild, message.getContentRaw());
        } else {
            textChannel = message.getMentionedChannels().get(0);
        }

        return textChannel;
    }

    public static Role getGuildRole(Guild guild, String content) {
        Role role = null;
        List<Role> roles = guild.getRoles();

        for (Role role1 : roles) {
            if (role1.getName().equalsIgnoreCase(content)) {
                role = role1;
                break;
            }
            if (role1.getId().equalsIgnoreCase(content.replaceAll("[^0-9]", ""))) {
                role = role1;
                break;
            }
        }

        return role;
    }

    public static Role getGuildRole(Guild guild, Message message) {
        Role role = null;
        List<Role> roles = guild.getRoles();

        String content = message.getContentRaw();

        if (message.getMentionedRoles().isEmpty()) {
            for (Role role1 : roles) {
                if (role1.getName().equalsIgnoreCase(content)) {
                    role = role1;
                    break;
                }
                if (role1.getId().equalsIgnoreCase(content)) {
                    role = role1;
                    break;
                }
            }
        } else {
            role = message.getMentionedRoles().get(0);
        }

        return role;
    }

    public static Category getGuildCategory(Guild guild, Message message) {
        Category category = null;
        List<Category> categories = guild.getCategories();

        String content = message.getContentRaw();

        for (Category category1 : categories) {
            if (category1.getName().equalsIgnoreCase(content)) {
                category = category1;
                break;
            }
            if (category1.getId().equalsIgnoreCase(content)) {
                category = category1;
                break;
            }
        }
        return category;
    }


}
