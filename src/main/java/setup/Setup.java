package setup;

import lombok.AllArgsConstructor;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.text.CaseUtils;
import utils.InputVerification;
import utils.Utils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static main.Goldilocks.eventWaiter;

public class Setup {

    protected Member member;
    protected Guild guild;

    protected Message controlPanel;
    protected SETUP setup;
    protected String errors = "";

    protected Map<String, Object> fields;
    protected Map<String, String> values;
    protected Map<Integer, String> fieldMap = new HashMap<>();
    protected List<String> changes = new ArrayList<>();

    public Setup (Message message, SETUP setupType) {
        member = message.getMember();
        guild = member.getGuild();
        fields = SetupConnector.getFields(guild, setupType.dbTable);
        values = SetupConnector.getValues(guild, setupType.dbTable);
        setup = setupType;

        controlPanel = message.getTextChannel().sendMessage(setupEmbed().build()).complete();
        messageHandler();
    }

    protected void messageHandler() {
        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(member.getUser()) && (e.getMessage().getContentRaw().equalsIgnoreCase("close") || Utils.isNumeric(e.getMessage().getContentRaw()));
        }, e -> {
            String content = e.getMessage().getContentRaw();
            e.getMessage().delete().queue();

            if (content.equalsIgnoreCase("close")) {
                controlPanel.editMessage(closeEmbed().build()).queue();
                return;
            }

            int choice = Integer.parseInt(content);
            if (choice > 0 && choice <= fields.size()) {
                promptField(fieldMap.get(choice));
            }
            else Utils.errorMessage("Invalid Option", "Enter a value between 1 and " + fields.size() + ".", controlPanel.getTextChannel(), 10L);


        }, 5L, TimeUnit.MINUTES, () -> {
            //Timeout
        });
    }

    protected void promptField(String field) {
        controlPanel.editMessage(promptEmbed(field).build()).queue();
        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(member.getUser()) && (e.getMessage().getTextChannel().equals(controlPanel.getTextChannel()));
        }, e -> {
            String content = e.getMessage().getContentRaw();

            if (content.equalsIgnoreCase("close")) {
                controlPanel.editMessage(closeEmbed().build()).queue();
                e.getMessage().delete().queue();
                return;
            }

            Object type = fields.get(field);
            String id = "";
            if (type.equals(TextChannel.class)) {
                TextChannel textChannel = InputVerification.getGuildTextChannel(guild, e.getMessage());
                if (textChannel != null) id = textChannel.getId();
            } else if (type.equals(Category.class)){
                Category category = InputVerification.getGuildCategory(guild, e.getMessage());
                if (category != null) id = category.getId();
            } else if (type.equals(Role.class)){
                Role role = InputVerification.getGuildRole(guild, e.getMessage());
                if (role != null) id = role.getId();
            } else if (type.equals(Boolean.class)){
                try {
                    Boolean aBoolean = Boolean.parseBoolean(e.getMessage().getContentRaw());
                    if (aBoolean != null) id = String.valueOf(aBoolean);
                } catch (Exception e1) {}
            } else if (type.equals(Integer.class)){
                try {
                    Integer integer = Integer.parseInt(e.getMessage().getContentRaw());
                    if (integer != null) id = String.valueOf(integer);
                } catch (Exception e1) {}
            }
            e.getMessage().delete().queue();

            if (!id.isEmpty()) {
                SetupConnector.updateField(guild, setup.dbTable, CaseUtils.toCamelCase(field, false) + (type.equals(Boolean.class) || type.equals(Integer.class) || type.equals(Role.class) ? "" : "Id"), id);
                if (field.replace(" ", "").contains("SlashCommand")) Goldilocks.slashCommands.getCommand(field.replace(" ", "").replace("SlashCommand", "").toLowerCase()).enable(guild);
                changes.add(field + ": " + values.get(field) + " → " + id);
                values.replace(field, id);
                controlPanel.editMessage(setupEmbed().build()).queue();
                messageHandler();
            } else {
                Utils.errorMessage("Invalid Value for " + field, e.getMessage().getContentRaw() + " is not a valid value. " +
                        "Please make sure that your input is a valid name, mention, or id for this field.", controlPanel.getTextChannel(), 5L);
                promptField(field);
            }

        }, 5L, TimeUnit.MINUTES, () -> {
            //Timeout
        });
    }

    protected EmbedBuilder promptEmbed(String fieldName) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(setup.visibleName + " for " + guild.getName())
                .setColor(Goldilocks.BLUE)
                .setDescription("Please enter a valid value for `" + fieldName + "`. Valid values include names," +
                        " mentions, and ids. If it is a boolean field, please use either `true` or `false`.")
                .setFooter("Setup Initiated by: " + member.getEffectiveName())
                .setTimestamp(new Date().toInstant());

        return embedBuilder;
    }

    protected int getRank(String string) {
        if (string.startsWith("Command") || string.startsWith("Slash")) return -1 * string.split(" ")[1].charAt(0);
        int v = -1 * string.charAt(0);
        String s = string.toLowerCase();
        if (s.contains("prefix")) v += 10000;
        else if (s.contains("role")) v += 5000;
        else if (s.toLowerCase().contains("category")) v += 2000;
        else if (s.toLowerCase().contains("channel")) v += 1000;
        else if (s.toLowerCase().contains("requirement")) v += 500;
        else if (s.contains("bool")) v += 100;
        return v;
    }

    private EmbedBuilder setupEmbed() {
        String fieldsString = "";
        int idx = 1;
         List<Map.Entry> fieldStrings = fields.entrySet().stream().sorted(new Comparator<Map.Entry<String, Object>>() {
            @Override
            public int compare(Map.Entry<String, Object> o1, Map.Entry<String, Object> o2) {
                return getRank(o2.getKey()) - getRank(o1.getKey());
            }
        }).collect(Collectors.toList());
        for (Map.Entry entrySet : fieldStrings) {
            fieldsString += "**`" + String.format("%1$2d", idx) + ": " + String.format("%-30s", entrySet.getKey().toString().replace("Command ", "")) + "`**| " + getMentionString((String) entrySet.getKey()) + "\n";
            fieldMap.put(idx++, (String) entrySet.getKey());
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(setup.visibleName + " for " + guild.getName())
                .setColor(Goldilocks.WHITE)
                .setDescription("Your have entered the setup process for " + setup.simpleName + ". To change a value for a field please enter the" +
                        " corresponding number. If you would like to exit this process please type `close`.")
                .setFooter("Setup Initiated by: " + member.getEffectiveName())
                .setTimestamp(new Date().toInstant());
        String fieldDesc = "";
        String fieldName = "Fields";
        for (String string : fieldsString.split("\n")) {
            if (fieldDesc.length() + string.length() > 1000) {
                embedBuilder.addField(fieldName, fieldDesc, false);
                fieldName = " ";
                fieldDesc = "";
            }
            fieldDesc += string + "\n";
        }
        if (!fieldDesc.isEmpty()) embedBuilder.addField(fieldName, fieldDesc, false);

        if (!errors.isEmpty()) {
            embedBuilder.addField("Errors ", "```\n" + errors + "\n```", false);
            errors = "";
        }

        if (!changes.isEmpty()) {
            embedBuilder.addField("Changes ", "```\n" + String.join("\n", changes) + "\n```", false);
        }

        return embedBuilder;
    }

    protected EmbedBuilder closeEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Completed " + setup.visibleName + " for " + guild.getName())
                .setColor(Goldilocks.WHITE)
                .setFooter("Setup Initiated by: " + member.getEffectiveName())
                .setTimestamp(new Date().toInstant())
                .addField("Changes ", "```\n" + (changes.isEmpty() ? "No changes were made." : String.join("\n", (changes))) + "\n```", false);

        return embedBuilder;
    }

    protected String getMentionString(String string) {
        String value = values.get(string);
        String mention = value;
        if ((value == null || value.equals("0")) && !string.toLowerCase().contains("bool")) return "Not set";
        try {
            if (fields.get(string).equals(TextChannel.class)) mention = guild.getTextChannelById(value).getAsMention();
            if (fields.get(string).equals(Category.class)) mention = guild.getCategoryById(value).getName();
            if (fields.get(string).equals(Role.class)) mention = guild.getRoleById(value).getAsMention();
            if (fields.get(string).equals(Boolean.class)) mention = String.valueOf(Boolean.parseBoolean(value.replace("1", "true")) ? "`✅`" : "`❌`");

        } catch (Exception e) {
            errors += "Failed to Retrieve " + string + " with ID " + value + ". Resetting field\n\n";
            //Todo reset field
            return "Error. ID: " + value;
        }

        return mention;
    }

    @AllArgsConstructor
    public enum SETUP {
        LOGS("Log Configuration", "guildLogs", "logs"),
        COMMANDS("Command Configuration", "commandConfig", "commands"),
        GUILD("Guild Configuration", "guildInfo", "guild info");

        public String visibleName;
        public String dbTable;
        public String simpleName;
    }

}


