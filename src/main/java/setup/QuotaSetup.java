package setup;

import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import quota.QuotaRole;
import raids.Dungeon;
import raids.DungeonInfo;
import utils.InputVerification;
import utils.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static main.Goldilocks.eventWaiter;

public class QuotaSetup {

    private Member member;
    private Guild guild;
    private TextChannel textChannel;

    private Message controlPanel;
    private List<Dungeon> dungeonInfo;
    private List<String> dungeonIds;
    List<QuotaRole> quotaRoles;

    public QuotaSetup(Message message, boolean roles) {
        member = message.getMember();
        guild = message.getGuild();
        textChannel = message.getTextChannel();

        dungeonInfo = DungeonInfo.unOrderedDungeonInfo(guild);

        if (roles) {
            controlPanel = textChannel.sendMessage(rolePanelEmbed().build()).complete();
            roleCommandListener();
        } else  {
            controlPanel = textChannel.sendMessage(dungeonPanelEmbed().build()).complete();
            dungeonCommandListener();
        }
    }

    public void dungeonCommandListener() {
        String[] commands = {"add", "rm", "roles", "close"};

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(member.getUser()) && Arrays.asList(commands).contains(e.getMessage().getContentRaw().toLowerCase().split(" ")[0]);
        }, e -> {
            String content = e.getMessage().getContentRaw();
            e.getMessage().delete().queue();

            int choice = -1;
            if (!content.replaceAll("[^0-9]", "").isEmpty()) choice = Integer.parseInt(content.replaceAll("[^0-9]", ""));

            if (content.equalsIgnoreCase("close")) {
                controlPanel.delete().queue();
                return;
            } else if (content.equalsIgnoreCase("roles")) {
                controlPanel.editMessage(rolePanelEmbed().build()).queue();
                roleCommandListener();
                return;
            } else if (content.split(" ")[0].equalsIgnoreCase("add")
            && choice >= 0 && choice <= dungeonInfo.size()) {
                if (!dungeonIds.contains(dungeonInfo.get(choice).dungeonIndex + "")) {
                    SetupConnector.executeUpdate("UPDATE guildInfo set quotaString = '" +
                            String.join(" ", dungeonIds) + (dungeonIds.get(0).isEmpty() ? "" : " ") + dungeonInfo.get(choice).dungeonIndex + "' WHERE guildId = " + guild.getId());
                    controlPanel.editMessage(dungeonPanelEmbed().build()).queue();
                } else Utils.errorMessage("Invalid Option", "Your quota already includes this dungeon.", textChannel, 5L);
                //Refresh panel
            } else if (content.split(" ")[0].equalsIgnoreCase("rm") &&
                    choice >= 0 && choice <= dungeonInfo.size()) {
                if (dungeonIds.contains(dungeonInfo.get(choice).dungeonIndex + "")) {
                    int finalChoice = choice;
                    SetupConnector.executeUpdate("UPDATE guildInfo set quotaString = '" + dungeonIds.stream().filter(s ->
                            !s.equals(dungeonInfo.get(finalChoice).dungeonIndex + "")).collect(Collectors.joining(" ")) + "' WHERE guildId = " + guild.getId());
                    controlPanel.editMessage(dungeonPanelEmbed().build()).queue();
                } else Utils.errorMessage("Invalid Option", "Your quota does not include this dungeon.", textChannel, 5L);
            } else Utils.errorMessage("Invalid Option", "Use the syntax provided in the controls section.", textChannel, 5L);
            dungeonCommandListener();

        }, 5L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    public void roleCommandListener() {
        String[] commands = {"addrole", "rmrole", "edit", "close"};

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> {
            return e.getAuthor().equals(member.getUser())
                    && Arrays.asList(commands).contains(e.getMessage().getContentRaw().toLowerCase().split(" ")[0]);
        }, e -> {
            String content = e.getMessage().getContentRaw();
            e.getMessage().delete().queue();

            int choice = -1;
            if (!content.replaceAll("[^0-9]", "").isEmpty() && content.replaceAll("[^0-9]", "").length() <= 5)
                choice = Integer.parseInt(content.replaceAll("[^0-9]", ""));

            if (content.equalsIgnoreCase("close")) {
                controlPanel.delete().queue();
                return;
            } else if (content.split(" ")[0].equalsIgnoreCase("addrole") && content.split(" ").length > 1) {
                controlPanel.editMessage(rolePanelEmbed().build()).queue();
                Role role = InputVerification.getGuildRole(guild, content.replace("addrole ", ""));
                if (role != null) {
                    rolePrompt(new QuotaRole(role));
                    return;
                } else Utils.errorMessage("Invalid Option", "I was unable to find a role with this name, id, or @.", textChannel, 5L);
            } else if (content.split(" ")[0].equalsIgnoreCase("rmrole")
                    && choice >= 0 && choice <= quotaRoles.size()) {
                SetupConnector.executeUpdate("DELETE FROM quotaRoles WHERE roleId = " + quotaRoles.get(choice).role.getId());
                controlPanel.editMessage(rolePanelEmbed().build()).queue();

            } else if (content.split(" ")[0].equalsIgnoreCase("edit")
                    && choice >= 0 && choice <= quotaRoles.size()) {
                rolePrompt(quotaRoles.get(choice));
                return;
            }  else Utils.errorMessage("Invalid Option", "Use the syntax provided in the controls section.", textChannel, 5L);
            roleCommandListener();

        }, 5L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    public void reactionHandler(QuotaRole quotaRole) {
        Goldilocks.eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(controlPanel.getId()) && e.getUser().equals(member.getUser()) && e.getReactionEmote().isEmoji() &&
                    ("1️⃣2️⃣3️⃣4️⃣↩").contains(e.getReactionEmote().getEmoji());
        }, e -> {
            String emote = e.getReactionEmote().getEmoji();
            if (emote.equals("↩")) {
                controlPanel.clearReactions().queue(aVoid -> {
                    SetupConnector.executeUpdate("DELETE FROM quotaRoles WHERE roleId = " + quotaRole.role.getId());
                    SetupConnector.executeUpdate("INSERT INTO quotaRoles (guildId, roleId, runReq, minRuns, assistReq, parseReq) VALUES " +
                            "(" + guild.getId() + ", " + quotaRole.role.getId() + ", " + quotaRole.runs + ", " + quotaRole.minRunsForAssists + ", " +
                            quotaRole.assists + ", " + quotaRole.parses + ")");
                    controlPanel.editMessage(rolePanelEmbed().build()).queue();
                    roleCommandListener();

                });
                return;
            }

            Message message = textChannel.sendMessage("What would you like to set for " + emote + "?").complete();
            eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e2 -> {
                return e2.getAuthor().equals(member.getUser());
            }, e2 -> {
                String content = e2.getMessage().getContentRaw();
                message.delete().queue();
                e2.getMessage().delete().queue();

                int amount = -1;
                if (!content.replaceAll("[^0-9]", "").isEmpty())
                    amount = Integer.parseInt(content.replaceAll("[^0-9]", ""));

                if (amount >= 0) {
                    switch (emote) {
                        case "1️⃣":
                            quotaRole.runs = amount;
                            break;
                        case "2️⃣":
                            quotaRole.assists = amount;
                            break;
                        case "3️⃣":
                            quotaRole.parses = amount;
                            break;
                        case "4️⃣":
                            quotaRole.minRunsForAssists = amount;
                            break;
                    }
                    controlPanel.editMessage(rolePromptEmbed(quotaRole).build()).queue();
                } else Utils.errorMessage("Invalid Option", "Please enter an amount >0.", textChannel, 5L);
                reactionHandler(quotaRole);

            }, 2L, TimeUnit.MINUTES, () -> {
                controlPanel.delete().queue();
            });

        }, 2L, TimeUnit.MINUTES, () -> {
            controlPanel.delete().queue();
        });
    }

    public void rolePrompt(QuotaRole quotaRole) {
        controlPanel.editMessage(rolePromptEmbed(quotaRole).build()).queue();
        controlPanel.addReaction("1️⃣").queue();
        controlPanel.addReaction("2️⃣").queue();
        controlPanel.addReaction("3️⃣").queue();
        controlPanel.addReaction("4️⃣").queue();
        controlPanel.addReaction("↩").queue();
        reactionHandler(quotaRole);
    }

    public EmbedBuilder rolePromptEmbed(QuotaRole quotaRole) {
        String content = "**`Run Requirement      `**: " + (quotaRole.runs == 0 ? "None" : String.valueOf(quotaRole.runs)) +
                "\n**`Assist Requirement   `**: " + (quotaRole.assists == 0 ? "None" : String.valueOf(quotaRole.assists)) +
                "\n**`Parse Requirement    `**: " + (quotaRole.parses == 0 ? "None" : String.valueOf(quotaRole.parses)) +
                "\n**`Min Runs for Assists `**: " + (quotaRole.minRunsForAssists == 0 ? "None" : String.valueOf(quotaRole.minRunsForAssists));
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setColor(quotaRole.role.getColor())
                .setTitle("Quota Role Setup: " + quotaRole.role.getName())
                .setDescription("Use the reactions to control each portion of the quota for this role.\n\n" +
                        "**" + quotaRole.role.getName() + "**\n" + content)
                .setFooter("To go back react with ↩");
        return embedBuilder;
    }

    public EmbedBuilder rolePanelEmbed() {
        quotaRoles = SetupConnector.getQuotaRoles(guild);
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("Quota Role Setup for " + guild.getName())
                .setColor(Goldilocks.WHITE)
                .setDescription("**Controls**\n" +
                        "`addrole <name/id/@>  ` To add a role\n" +
                        "`rmrole <#>           ` To remove a role\n" +
                        "`edit <#>             ` To edit an existing role")
                .setFooter("To exit type close");

        for (QuotaRole q : quotaRoles) {
            String content = "\n**`Run Requirement      `**: " + (q.runs == 0 ? "None" : String.valueOf(q.runs)) +
                    "\n**`Assist Requirement   `**: " + (q.assists == 0 ? "None" : String.valueOf(q.assists)) +
                    "\n**`Parse Requirement    `**: " + (q.parses == 0 ? "None" : String.valueOf(q.parses)) +
                    "\n**`Min Runs for Assists `**: " + (q.minRunsForAssists == 0 ? "None" : String.valueOf(q.minRunsForAssists));
            embedBuilder.addField("**" + quotaRoles.indexOf(q) + ".** " + q.role.getName(), content, false);
        }

        if (quotaRoles.isEmpty()) embedBuilder.addField(" ", "You currently do not have any quota roles, if you would like to add one" +
                " please use the controls listed above.", false);

        return embedBuilder;
    }

    public EmbedBuilder dungeonPanelEmbed() {
        //Use 3 for the name and 0 for the dungeon emote
        dungeonIds = Arrays.asList(SetupConnector.getFieldValue(guild, "guildInfo", "quotaString").split(" "));
        EmbedBuilder embedBuilder = new EmbedBuilder();

        String summary = "";
        String dungeonString = !dungeonIds.get(0).isEmpty() ? dungeonIds.stream().map(s -> dungeonInfo.stream().filter(d -> d.dungeonIndex == Integer.parseInt(s)).findFirst().orElse(new Dungeon(dungeonInfo.size())).dungeonInfo[3] + " ("
                + Goldilocks.jda.getEmoteById(dungeonInfo.stream().filter(d -> d.dungeonIndex == Integer.parseInt(s)).findFirst().orElse(new Dungeon(dungeonInfo.size())).dungeonInfo[0]).getAsMention() + ")").collect(Collectors.joining(", ")) : "None";
        summary += "**Current Dungeons:** " + dungeonString;

        embedBuilder.setColor(Goldilocks.WHITE)
                .setTitle("Quota Setup for " + guild.getName())
                .setDescription("Below is a summary of the quota for you server. To add or remove " +
                        "dungeons from quota please utilize the controls listed in the controls section." +
                        "\n\n" + summary + "\n" +
                        " \n**Controls**\n" +
                        "`add <#>   ` To add a dungeon to quota.\n" +
                        "`rm  <#>   ` To remove a dungeon from quota.\n" +
                        "`roles     ` To setup quota for roles.\n")
                .setFooter("To exit type close");
        int maxDungeons = 15;
        int index = 0;
        for (int i = 0; i <= dungeonInfo.size() / maxDungeons; i++) {
            String currentField = "";
            for (int j = 0; j < maxDungeons && index < dungeonInfo.size(); j++) {
                currentField += Goldilocks.jda.getEmoteById(dungeonInfo.get(index).dungeonInfo[0]).getAsMention() + " | " + String.format("`%1$3s`", index) + " | " + dungeonInfo.get(index).dungeonInfo[3] + "\n";
                index++;
            }
            embedBuilder.addField(" ", currentField, true);
            if (embedBuilder.getFields().size() % 3 == 2) embedBuilder.addBlankField(true);
        }
        return embedBuilder;
    }

}
