package setup;

import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import utils.Utils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static main.Goldilocks.eventWaiter;

public class ServerSetup {

    public static void controlPanel(Message msg) {

        TextChannel textChannel = msg.getTextChannel();
        Message controlPanel = textChannel.sendMessage(controlPanelEmbed(msg).build()).complete();

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getMessage().getAuthor().equals(msg.getAuthor()) && (Utils.isNumeric(e.getMessage().getContentRaw()) || e.getMessage().getContentRaw().toLowerCase().equals("close"));
        }, e -> {
            String messageContent = e.getMessage().getContentRaw().toLowerCase();
            if (messageContent.equals("close")) {
                controlPanel.delete().queue();
                msg.delete().queue();
                e.getMessage().delete().queue();
                return;
            }

            int messageChoice = Integer.parseInt(messageContent);
            if (messageChoice <= 15 && messageChoice >= 1) {
                updateField(messageContent, controlPanel, msg);
                e.getMessage().delete().queue();
                return;
            } else {
                Utils.errorMessage("Configuration Update Failed", messageContent + " is not a valid option", msg.getTextChannel(), 10L);
                controlPanel(msg, controlPanel);
                e.getMessage().delete().queue();
            }

        }, 2L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });

    }

    public static void controlPanel(Message msg, Message controlPanel) {


        TextChannel textChannel = msg.getTextChannel();
        controlPanel.editMessage(controlPanelEmbed(msg).build()).queue();

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getMessage().getAuthor().equals(msg.getAuthor()) && (Utils.isNumeric(e.getMessage().getContentRaw()) || e.getMessage().getContentRaw().toLowerCase().equals("close"));
        }, e -> {
            String messageContent = e.getMessage().getContentRaw().toLowerCase();
            if (messageContent.equals("close")) {
                controlPanel.delete().queue();
                msg.delete().queue();
                e.getMessage().delete().queue();
                return;
            }

            int messageChoice = Integer.parseInt(messageContent);
            if (messageChoice <= 16 && messageChoice >= 1) {
                updateField(messageContent, controlPanel, msg);
                e.getMessage().delete().queue();
            } else {
                Utils.errorMessage("Configuration Update Failed", messageContent + " is not a valid option", msg.getTextChannel(), 10L);
                controlPanel(msg, controlPanel);
                e.getMessage().delete().queue();
            }

        }, 2L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });

    }

    public static EmbedBuilder controlPanelEmbed(Message msg) {
        Guild guild = msg.getGuild();
        List<String> guildInfo = Database.getGuildInfo(msg.getGuild().getId());
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Setup Control Panel for " + msg.getGuild().getName());
        embedBuilder.setDescription("This is the current configuration for your guild, please enter the corresponding" +
                " number in the chat below to change the values for the given fields. Filling out all of these field will ensure" +
                " that all of the bots commands will run in your server; however, this is not needed for all servers.");
        embedBuilder.setColor(Goldilocks.GOLD);

        embedBuilder.addField("**1.** Command Prefix: ", "`" + guildInfo.get(0) + "`", false);

        embedBuilder.addField("Categories: ",
                "\n**2.** Match Making Category: " + (guildInfo.get(3).equals("0") ? "None" : guild.getCategoryById(guildInfo.get(3)).getName()), false);

        //Get command channels
        String commandChannels = "**3.** ";
        if (guildInfo.get(4).equals("0")) {
            commandChannels += "All Text Channels";
        } else {
            String[] channelIds = guildInfo.get(4).split(" ");
            for (String channelId : channelIds) {
                try {
                    commandChannels += guild.getTextChannelById(channelId).getAsMention() + " ";
                } catch (Exception e) {
                    break;
                }
            }
        }
        embedBuilder.addField("Command Channels: ", commandChannels, false);

        //Roles
        embedBuilder.addField("Member Roles:", "**4.** Verified: " + (guildInfo.get(5).equals("0") ? "None"  :guild.getRoleById(guildInfo.get(5)).getAsMention()) +
                        "\n**Raid Leading Roles**:" +
                        "\n**5.** Head Raid Leader: " + (guildInfo.get(7).equals("0") ? "None"  :guild.getRoleById(guildInfo.get(7)).getAsMention()) +
                        "\n**6.** Veteran Raid Leader: " + (guildInfo.get(8).equals("0") ? "None"  :guild.getRoleById(guildInfo.get(8)).getAsMention()) +
                        "\n**7.** Raid Leader: " + (guildInfo.get(9).equals("0") ? "None"  :guild.getRoleById(guildInfo.get(9)).getAsMention()) +
                        "\n**8.** Almost Raid Leader: " + (guildInfo.get(10).equals("0") ? "None"  :guild.getRoleById(guildInfo.get(10)).getAsMention()) +
                        "\n**9.** Trial Raid Leader: " + (guildInfo.get(11).equals("0") ? "None"  :guild.getRoleById(guildInfo.get(11)).getAsMention()) +
                        "\n" +
                        "\n**Moderation Roles**:" +
                        "\n**10.** Moderator: " +(guildInfo.get(12).equals("0") ? "None"  :guild.getRoleById(guildInfo.get(12)).getAsMention()) +
                        "\n**11.** Officer: " + (guildInfo.get(13).equals("0") ? "None"  :guild.getRoleById(guildInfo.get(13)).getAsMention()) +
                        "\n**12.** Security: " + (guildInfo.get(14).equals("0") ? "None"  :guild.getRoleById(guildInfo.get(14)).getAsMention()) +
                        "\n**13.** Trial Security: " + (guildInfo.get(15).equals("0") ? "None"  :guild.getRoleById(guildInfo.get(15)).getAsMention()) +
                        "\n" +
                        "\n**Event Roles**:" +
                        "\n**14.** Head Event Organizer: " +(guildInfo.get(16).equals("0") ? "None"  :guild.getRoleById(guildInfo.get(16)).getAsMention()) +
                        "\n**15.** Veteran Event Organizer: " + (guildInfo.get(17).equals("0") ? "None"  :guild.getRoleById(guildInfo.get(17)).getAsMention()) +
                        "\n**16.** Event Organizer: " + (guildInfo.get(18).equals("0") ? "None"  :guild.getRoleById(guildInfo.get(18)).getAsMention())
                , false);

        //embedBuilder.addField("Verification Channels: ", "**17.** Verification Channel: " + (guildInfo.get(16).equals("0") ? "None" : guild.getTextChannelById(guildInfo.get(16)).getAsMention()), false);
        embedBuilder.setFooter("Respond with close at any point to exit server setup");
        embedBuilder.setTimestamp(new Date().toInstant());

        return embedBuilder;
    }

    public static void updateField(String field, Message controlPanel, Message msg) {
        int fieldNum = Integer.valueOf(field) - 1;
        String[][] setupArray = {{"commandPrefix","mmCategory","commandChannels","verifiedRole","headRlRole","vetRlRole","rlRole","arlRole","trlRole","modRole","officerRole"
                ,"securityRole","tsecRole","headEoRole","vetEoRole","eoRole"},
                //Visible Name to the users
                {"Command Prefix","Match Making Category","Command Channels","Verified Role","Head Raid Leader Role","Veteran Raid Leader Role"
                        ,"Raid Leader Role","Almost Raid Leader Role","Trial Raid Leader Role","Moderator Role", "Officer Role", "Security Role", "Trial Security Role","Head Event Organizer","Veteran Event Organizer","Event Organizer"},
                //Custom control panel description per setup item
                {"Please enter the new prefix you have in mind into the chat.\n\n📝 Example: `!`",
                        "Please enter the id of the category you would like to set as your Match Making Category.\n\n📝 Example: `" + controlPanel.getGuild().getCategories().get(0).getId()
                                + "` for the category named `" + controlPanel.getGuild().getCategories().get(0).getName() + "`",
                        "Please tag a channel that you would like to add to the valid command channels.\n\n📝 Example: " + controlPanel.getGuild().getTextChannels().get(1).getAsMention(),
                        "Please enter the id of the role you would like to set as your Verified Role.\n\n📝 Example: `" + controlPanel.getGuild().getRoles().get(1).getId() +
                                "` for the role: " + controlPanel.getGuild().getRoles().get(1).getAsMention(),
                        "Please enter the id of the role you would like to set as your Head Raid Leader Role.\n\n📝 Example: `" + controlPanel.getGuild().getRoles().get(1).getId() +
                                "` for the role: " + controlPanel.getGuild().getRoles().get(1).getAsMention(),
                        "Please enter the id of the role you would like to set as your Veteran Raid Leader Role.\n\n📝 Example: `" + controlPanel.getGuild().getRoles().get(1).getId() +
                                "` for the role: " + controlPanel.getGuild().getRoles().get(1).getAsMention(),
                        "Please enter the id of the role you would like to set as your Raid Leader Role.\n\n📝 Example: `" + controlPanel.getGuild().getRoles().get(1).getId() +
                                "` for the role: " + controlPanel.getGuild().getRoles().get(1).getAsMention(),
                        "Please enter the id of the role you would like to set as your Almost Raid Leader Role.\n\n📝 Example: `" + controlPanel.getGuild().getRoles().get(1).getId() +
                                "` for the role: " + controlPanel.getGuild().getRoles().get(1).getAsMention(),
                        "Please enter the id of the role you would like to set as your Trial Raid Leader Role.\n\n📝 Example: `" + controlPanel.getGuild().getRoles().get(1).getId() +
                                "` for the role: " + controlPanel.getGuild().getRoles().get(1).getAsMention(),
                        "Please enter the id of the role you would like to set as your Moderator Role.\n\n📝 Example: `" + controlPanel.getGuild().getRoles().get(1).getId() +
                                "` for the role: " + controlPanel.getGuild().getRoles().get(1).getAsMention(),
                        "Please enter the id of the role you would like to set as your Officer Role.\n\n📝 Example: `" + controlPanel.getGuild().getRoles().get(1).getId() +
                                "` for the role: " + controlPanel.getGuild().getRoles().get(1).getAsMention(),
                        "Please enter the id of the role you would like to set as your Security Role.\n\n📝 Example: `" + controlPanel.getGuild().getRoles().get(1).getId() +
                                "` for the role: " + controlPanel.getGuild().getRoles().get(1).getAsMention(),
                        "Please enter the id of the role you would like to set as your Trial Security Role.\n\n📝 Example: `" + controlPanel.getGuild().getRoles().get(1).getId() +
                                "` for the role: " + controlPanel.getGuild().getRoles().get(1).getAsMention(),
                        "Please enter the id of the role you would like to set as your Head Event Organizer Role.\n\n📝 Example: `" + controlPanel.getGuild().getRoles().get(1).getId() +
                                "` for the role: " + controlPanel.getGuild().getRoles().get(1).getAsMention(),
                        "Please enter the id of the role you would like to set as your Veteran Event Organizer Role.\n\n📝 Example: `" + controlPanel.getGuild().getRoles().get(1).getId() +
                                "` for the role: " + controlPanel.getGuild().getRoles().get(1).getAsMention(),
                        "Please enter the id of the role you would like to set as your Event Organizer Role.\n\n📝 Example: `" + controlPanel.getGuild().getRoles().get(1).getId() +
                                "` for the role: " + controlPanel.getGuild().getRoles().get(1).getAsMention()
                }};

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Setup Control Panel for " + msg.getGuild().getName());
        embedBuilder.setColor(Goldilocks.GOLD);
        embedBuilder.setDescription(setupArray[2][fieldNum]);
        embedBuilder.setFooter("Respond with close at any point to exit server setup");

        controlPanel.editMessage(embedBuilder.build()).queue();
        retrieveUserInput(setupArray[0][fieldNum], setupArray[1][fieldNum], msg, controlPanel);

    }

    public static void retrieveUserInput(String fieldName, String fieldVisibleName, Message message, Message controlPanel) {
        final String[] updatedValue = new String[1];

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {return e.getAuthor().equals(message.getAuthor());}, e -> {
            if (e.getMessage().getContentRaw().toLowerCase().equalsIgnoreCase("close")) {
                e.getMessage().delete().queue();
                controlPanel.delete().queue();
                message.delete().queue();
                return;
            }

            if (fieldName.equals("mmCategory")) {
                if (message.getGuild().getCategoryById(e.getMessage().getContentRaw()) == null) {
                    message.getGuild().getCategoryById(e.getMessage().getContentRaw());
                    updatedValue[0] = e.getMessage().getContentRaw();
                    Utils.errorMessage("Configuration Update Failed","`" + updatedValue[0] + "` is not a valid value for `" + fieldVisibleName, message.getTextChannel(), 10L);
                    e.getMessage().delete().queue();
                    controlPanel(message, controlPanel);
                    return;
                } else {
                    updatedValue[0] = e.getMessage().getContentRaw();
                    Database.updateGuildInfo(message.getGuild().getId(), fieldName, updatedValue[0],false);
                    e.getMessage().delete().queue();
                    controlPanel(message, controlPanel);
                    return;
                }
            }

            if (fieldName.equals("commandChannels")) {
                if (e.getMessage().getMentionedChannels().size() == 0) {
                    updatedValue[0] = e.getMessage().getContentRaw();
                    Utils.errorMessage("Configuration Update Failed","`" + updatedValue[0] + "` is not a valid value for `" + fieldVisibleName, message.getTextChannel(), 10L);
                    e.getMessage().delete().queue();
                    controlPanel(message, controlPanel);
                    return;
                } else {
                    updatedValue[0] = "";
                    String channelMentions = "";
                    for (TextChannel channel : e.getMessage().getMentionedChannels()) {
                        updatedValue[0] += channel.getId() + " ";
                        channelMentions += channel.getAsMention() + " ";
                    }
                    Database.updateGuildInfo(message.getGuild().getId(), fieldName, updatedValue[0],true);
                    e.getMessage().delete().queue();
                    controlPanel(message, controlPanel);
                    return;

                }
            }

            if (fieldName.contains("Role")) {
                if (e.getGuild().getRoleById(e.getMessage().getContentRaw()) == null) {
                    updatedValue[0] = e.getMessage().getContentRaw();
                    Utils.errorMessage("Configuration Update Failed","`" + updatedValue[0] + "` is not a valid value for `" + fieldVisibleName, message.getTextChannel(), 10L);
                    e.getMessage().delete().queue();
                    controlPanel(message, controlPanel);
                    return;
                } else {
                    updatedValue[0] = e.getMessage().getContentRaw();
                    Database.updateGuildInfo(message.getGuild().getId(), fieldName, updatedValue[0],false);
                    e.getMessage().delete().queue();
                    controlPanel(message, controlPanel);
                    return;
                }
            }

            updatedValue[0] = e.getMessage().getContentRaw();
            Database.updateGuildInfo(message.getGuild().getId(), fieldName, updatedValue[0],false);
            e.getMessage().delete().queue();
            controlPanel(message, controlPanel);

        }, 2L, TimeUnit.MINUTES, () -> {
            //Failed
        });
    }

}
