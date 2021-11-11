package utils;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import setup.SetupConnector;

import javax.annotation.Nonnull;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Logging {

    public static HashMap<Guild, WebhookClient> webHookClients = new HashMap<>();

    public static void logMessages(Member member, List<Message> messages) {
        TextChannel logChannel = Goldilocks.jda.getTextChannelById(SetupConnector.getFieldValue(member.getGuild(), "guildLogs", "logChannelId"));
        if (logChannel == null) return;
        int idx = 0;
        String currentField = "";
        boolean title = true;

        SimpleDateFormat timeFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm");
        //timeFormat.setTimeZone(TimeZone.getTimeZone("EST"));

        List<Message> filteredMessages = messages.stream().filter(m -> !m.getAuthor().isBot()).collect(Collectors.toList());
        filteredMessages.sort(new Comparator<Message>() {
            @Override
            public int compare(Message o1, Message o2) {
                return (int) (o1.getTimeCreated().toEpochSecond() - o2.getTimeCreated().toEpochSecond());
            }
        });
        List<EmbedBuilder> embedBuilders = new ArrayList<>();
        embedBuilders.add(new EmbedBuilder().setColor(Goldilocks.WHITE).setTitle("Message Purge by " + member.getEffectiveName() + " in " + messages.get(0).getTextChannel().getName())
                .addField("Mod Information", member.getAsMention() + " | " + member.getId(), true)
                .addField("Summary", "Channel: " + messages.get(0).getTextChannel().getAsMention() + "\nTotal Messages: `" + filteredMessages.size() + "`", true)
                .addField("Authors", filteredMessages.stream().map(m -> m.getAuthor().getAsMention()).distinct().collect(Collectors.joining(" ")), true)
                .setTimestamp(new Date().toInstant()));
        for (Message m : filteredMessages) {
            String messageContent = m.getContentRaw();
            String fieldContent = "\n\n💬 Sent by " + m.getAuthor().getAsMention() + " at `" + timeFormat.format(new Date(m.getTimeCreated().toEpochSecond() * 1000 + 7200000)) + " EST`"
                    + (m.isEdited() ? "\n✏ Edited `" + timeFormat.format(new Date(m.getTimeEdited().toEpochSecond() * 1000 + 7200000)) + " EST`" : "") + "\n" + messageContent
                    + (!m.getAttachments().isEmpty() ? "\n" + m.getAttachments().stream().map(a -> "[Attachment](" + a.getProxyUrl() + ")").collect(Collectors.joining(", ")) : "");
            if (currentField.length() + fieldContent.length() > 1024) {
                if (fieldContent.length() <= 1024) {
                    if (embedBuilders.get(idx).length() + 1024 < 5900 && embedBuilders.get(idx).getFields().size() < 25) {
                        embedBuilders.get(idx).addField((title ? "Messages" : ""), currentField, false);
                        title = false;
                    } else {
                        embedBuilders.add(new EmbedBuilder().setColor(Goldilocks.LIGHTBLUE));
                        embedBuilders.get(++idx).addField(" ", currentField, false);
                    }
                    currentField = fieldContent;
                } else {
                    if (embedBuilders.get(idx).length() + fieldContent.length() > 5900 && embedBuilders.get(idx).getFields().size() < 25) {
                        embedBuilders.add(new EmbedBuilder().setColor(Goldilocks.LIGHTBLUE).setTimestamp(new Date().toInstant()));
                        idx++;
                    }
                    int startingIdx = 1024 - currentField.length();
                    embedBuilders.get(idx).addField((title ? "Messages" : ""), currentField + fieldContent.substring(0, startingIdx), false);
                    title = false;
                    if (fieldContent.length() >= startingIdx + 1024) {
                        embedBuilders.get(idx).addField(" ", fieldContent.substring(startingIdx, startingIdx + 1024), false);
                        currentField = fieldContent.substring(startingIdx + 1024);
                    } else currentField = fieldContent.substring(startingIdx);
                }
            }
            else currentField += fieldContent;
        }
        if (!currentField.isEmpty()) {
            if (embedBuilders.get(idx).length() + 1024 < 5900 && embedBuilders.get(idx).getFields().size() < 25) embedBuilders.get(idx).addField((title ? "Messages" : ""), currentField, false);
            else {
                embedBuilders.add(new EmbedBuilder().setColor(Goldilocks.LIGHTBLUE));
                embedBuilders.get(++idx).addField(" ", currentField, false);
            }
        }
        embedBuilders.forEach(e -> {
            if (e.isValidLength()) logChannel.sendMessage(e.build()).queue();
        });
    }

    public static String createWebHook(Guild guild) {
        TextChannel logChannel = Goldilocks.jda.getTextChannelById(SetupConnector.getFieldValue(guild, "guildLogs", "logChannelId"));
        if (logChannel == null) return "";
        if (!guild.getSelfMember().hasPermission(logChannel, Permission.MANAGE_WEBHOOKS)) return "";
        try {
            Webhook webhook = logChannel.createWebhook(guild.getSelfMember().getEffectiveName()).setAvatar(Icon.from(new File("goldilockspfp.png"))).complete();
            return webhook.getUrl();
        } catch (Exception exception) {
            return "";
        }
    }

    public static boolean createWebhookClient(Guild guild) {
        TextChannel logChannel = Goldilocks.jda.getTextChannelById(SetupConnector.getFieldValue(guild, "guildLogs", "logChannelId"));
        if (logChannel == null) return false;
        String URL = "";
        List<Webhook> webhooks = logChannel.retrieveWebhooks().complete().stream()
                .filter(webhook -> webhook.getName().equals(guild.getSelfMember().getEffectiveName())).collect(Collectors.toList());
        if (webhooks.size() < 1) URL = createWebHook(logChannel.getGuild());
        else URL = webhooks.get(0).getUrl();

        if (URL.isEmpty()) {
            webHookClients.put(logChannel.getGuild(), null);
            return false;
        }

        WebhookClientBuilder builder = new WebhookClientBuilder(URL); // or id, token
        builder.setThreadFactory((job) -> {
            Thread thread = new Thread(job);
            thread.setName(logChannel.getGuild().getName());
            thread.setDaemon(true);
            return thread;
        });

        builder.setWait(true);
        WebhookClient client = builder.build();
        webHookClients.put(logChannel.getGuild(), client);
        return true;
    }

    public static void logMessageEdit(Message message) {
        if (SetupConnector.getFieldValue(message.getGuild(), "guildLogs", "logChannelId").isEmpty()) return;
        TextChannel logChannel = Goldilocks.jda.getTextChannelById(SetupConnector.getFieldValue(message.getGuild(), "guildLogs", "logChannelId"));
        if (logChannel == null) return;
        if (webHookClients.containsKey(message.getGuild()) && webHookClients.get(message.getGuild()) == null) return;
        if (!webHookClients.containsKey(message.getGuild())) {
            if (!createWebhookClient(message.getGuild())) return;
        }
        String content = Database.getMessageContent(message.getId());
        if (!content.isEmpty()) webHookClients.get(message.getGuild()).send(messageEditEmbed(message, content).build());
        Database.updateMessage(message);
    }

    public static void logMessageDelete(Guild guild, String messageId) {
        String[] messageInfo = Database.getMessageInfo(messageId);
        if (SetupConnector.getFieldValue(guild, "guildLogs", "logChannelId").isEmpty()) return;
        TextChannel logChannel = Goldilocks.jda.getTextChannelById(SetupConnector.getFieldValue(guild, "guildLogs", "logChannelId"));
        if (logChannel == null) return;
        if (webHookClients.containsKey(guild) && webHookClients.get(guild) == null) return;
        if (!webHookClients.containsKey(guild)) if (!createWebhookClient(guild)) return;
        if (messageInfo.length == 0) return;
        User author = Goldilocks.jda.getUserById(messageInfo[0]);
        String content = messageInfo[1];
        TextChannel textChannel = guild.getTextChannelById(messageInfo[2]);
        if (textChannel != null && author != null && !content.isEmpty()) {
            webHookClients.get(guild).send(messageDeleteEmbed(author, textChannel, content).build());
        }
    }

    public static WebhookEmbedBuilder messageDeleteEmbed(User author, TextChannel textChannel, String content) {
        WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
        embed.setColor(Goldilocks.RED.getRGB())
                .setDescription("❌** Message by " + author.getAsMention() + " deleted in " + textChannel.getAsMention() + "**")
                .addField(new WebhookEmbed.EmbedField(false, "Content", content))
                .setFooter(new WebhookEmbed.EmbedFooter("User ID: " + author.getId(), author.getAvatarUrl()))
                .setTimestamp(new Date().toInstant());
        return embed;
    }

    public static WebhookEmbedBuilder messageEditEmbed(@Nonnull Message message, String oldContent) {
        WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
        embed.setColor(Goldilocks.LIGHTBLUE.getRGB())
                .setDescription("✏** " + (message.getMember() != null ? message.getMember().getEffectiveName() : message.getAuthor().getAsTag()) + " edited a [message](" + message.getJumpUrl() + ")**")
                .addField(new WebhookEmbed.EmbedField(true, "User Info", message.getAuthor().getAsMention() + " | " + message.getAuthor().getId()))
                .addField(new WebhookEmbed.EmbedField(true, "Text Channel", message.getTextChannel().getAsMention()))
                .addField(new WebhookEmbed.EmbedField(false, "Content Before", oldContent))
                .addField(new WebhookEmbed.EmbedField(false, "Content After", message.getContentRaw()))
                .setFooter(new WebhookEmbed.EmbedFooter("User ID: " + message.getAuthor().getId(), message.getAuthor().getAvatarUrl()))
                .setTimestamp(new Date().toInstant());
        return embed;
    }



}
